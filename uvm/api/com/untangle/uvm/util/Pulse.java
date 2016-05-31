/**
 * $Id$
 */
package com.untangle.uvm.util;

import org.apache.log4j.Logger;

public class Pulse implements Runnable
{
    /* things shouldn't be firing that fast, must be a bug */
    private static final long DELAY_MINIMUM = 500;

    /* wait at most a second for the pulse to execute when force running */
    private static final long FORCE_RUN_MAX_WAIT = 1000;

    private final Logger logger = Logger.getLogger(getClass());

    public enum PulseState 
    {
        UNBORN, /* Pulse thread has never started running */
        STARTING, /* Pulse thread is actively running */
        RUNNING, /* Pulse thread is actively running */
        KILLED, /* Pulse thread has been killed, but for some reason hasn't died yet */
        DEAD /* Thread is totally dead and cannot be resurrected */
    };

    private PulseState state = PulseState.UNBORN;

    /* The thread to run this task */
    private Thread thread = null;

    /* The task to run */
    private Runnable task;

    /* The name of this pulse */
    private String name;

    /* Used for debugging */
    private String logPrefix;
    
    /* amount of time to wait until the next beat */
    private long delay;

    /* an extra delay for the initial run (optional) */
    private long extraInitialDelay;

    /* a flag used when forcing the pulse to run prematurely */
    private boolean forceRun = false;
    
    /* the number of times the pulse has run */
    private long count = 0;

    /* This is the next time that you should wake up */
    private long nextTask = 0;

    /* The thread priority */
    private int threadPriority;

    /**
     * Create a new pulse with the default name and isDaemon setting.
     */
    public Pulse( Runnable task, long delay )
    {
        this( null, task, delay );
    }

    /**
     * Create a new pulse and set its name, task, delay
     */
    public Pulse( String name, Runnable task, long delay )
    {
        this( name, task, delay, 0, Thread.MIN_PRIORITY );
    }

    /**
     * Create a new pulse and set its name, task, delay, extraInitialDelay
     */
    public Pulse( String name, Runnable task, long delay, long extraInitialDelay )
    {
        this( name, task, delay, extraInitialDelay, Thread.MIN_PRIORITY );
    }
    
    /**
     * Create a new pulse, optionally setting the name and isDaemon
     * setting.
     */
    public Pulse( String name, Runnable task, long delay, long extraInitialDelay, int threadPriority )
    {
        this.name = name;
        this.task = task;
        this.delay = delay;
        this.extraInitialDelay = extraInitialDelay;
        this.threadPriority = threadPriority;
        this.logPrefix = "Pulse[" + task.getClass().getSimpleName() + "] ";
    }

    /**
     * Start the thread, you can only start a pulse once and once it
     * is stopped, you can never restart it.
     */
    public void start()
    {
        /* Can't start unless it is in the unborn state */
        if ( PulseState.UNBORN != this.state ) {
            throw new IllegalStateException( "Unable to start a pulse twice" );
        }

        this.delay = delay;
        this.extraInitialDelay = extraInitialDelay;
        
        /* Indicate that the thread is now starting */
        this.state = PulseState.STARTING;

        /* Create and start the thread */
        if ( ( name == null ) || ( name.length() == 0 ) ) {
            this.thread = new Thread( this );
        } else {
            this.thread = new Thread( this, name );
        }
        this.thread.setDaemon( true );
        this.thread.setPriority( threadPriority );

        logger.debug(logPrefix + "launching..." );
        this.thread.start();
    }
    
    /**
     * Stop the thread, you can only start a pulse once and once it
     * is stopped, you can never restart it.
     */
    public void stop()
    {
        switch ( this.state ) {
        case UNBORN:
            logger.warn(logPrefix + "Attempt to stop an unborn pulse." );
            return;

        case DEAD:
            logger.warn(logPrefix + "Attempt to stop a dead pulse." );
            return;

        case STARTING: /* unlikely but possible */
        case KILLED:
        case RUNNING:
            logger.debug(logPrefix + "Stopping the pulse." );
            this.state = PulseState.KILLED;
            /* Interrupt the thread. */
            if ( this.thread != null )
                this.thread.interrupt();
            return;
        }
    }

    /**
     * Return the current state
     */
    public PulseState getState()
    {
        return this.state;
    }
    
    /**
     * Run it now (blocks complete or until maxWait ms)
     */
    public boolean forceRun( long maxWait )
    {
        if ( this.state != PulseState.RUNNING ) {
            logger.warn(logPrefix +"Can not force run pulse that is not running: " + this.state );
            return false;
        } else if ( this.thread == null ) {
            logger.warn(logPrefix +"Can not force run pulse that without thread: " + this.state );
            return false;
        }
        
        long origCount = this.getCount();
        this.forceRun = true;
            
        /* has ticked since beat was called. */
        if ( origCount != this.getCount())
            return true;

        synchronized ( this ) {
            try {
                this.wait( maxWait );
            } catch ( InterruptedException e ) {
                logger.debug(logPrefix + "interrupted while waiting", e );
            }
        }

        /* If the count changed, then this waited for one tick to complete */
        return ( count == this.getCount() );
    }

    /**
     * Beat with the default max wait
     */
    public boolean forceRun()
    {
        return forceRun( FORCE_RUN_MAX_WAIT );
    }

    /**
     * This contains the loop that runs the task over and over
     */
    public void run()
    {
        long now;
        
        if ( this.state != PulseState.STARTING) {
            logger.warn(logPrefix + "Unable to start the thread outside of running state" );
            return;
        }
        this.state = PulseState.RUNNING;
        logger.debug(logPrefix + "starting ..." );

        nextTask = System.currentTimeMillis() + delay + extraInitialDelay;

        while ( this.state == PulseState.RUNNING ) {
            try {
                long sleepTime = nextTask - System.currentTimeMillis(); // sleep until nextTask
                if ( sleepTime <= 0 ) {
                    if ( logger.isDebugEnabled() )
                        logger.debug(logPrefix + "delay(" + delay + ") <= 0, firing immediately." );
                    sleepTime = DELAY_MINIMUM;
                }

                if ( sleepTime < DELAY_MINIMUM ) {
                    if ( logger.isDebugEnabled() )
                        logger.debug(logPrefix + "delay(" + delay + ") < " + DELAY_MINIMUM + " less than minimum.");
                    sleepTime = DELAY_MINIMUM;
                }

                synchronized ( this ) {
                    if ( logger.isDebugEnabled() )
                        logger.debug(logPrefix + "sleeping (" + sleepTime + " ms) ...");
                    wait( sleepTime );
                }
            } catch ( InterruptedException e ) {
                if ( logger.isDebugEnabled() )
                    logger.debug(logPrefix + "interrupted while waiting for task to complete." );
            }

            /* We woke up */
            now = System.currentTimeMillis();

            /* If we need to exit, quit now */
            if ( this.state != PulseState.RUNNING )
                return;

            /* If we woke up prematurely, go back to sleep, unless forceRun is set */
            if ( !forceRun && now < nextTask )
                continue;

            /* Save when the next task should occur */
            nextTask = nextTask + delay;

            /**
             * If we are so far behind that the *next* task has already supposed to be run
             * then just reset to a reasonable time
             */
            if ( nextTask < now ) {
                if ( logger.isDebugEnabled() )
                    logger.debug(logPrefix + "pulse running behind, reset to min delay.");
                nextTask = now + DELAY_MINIMUM;
            }

            /* Run the task */
            try {
                long t0=0, t1=0;
                if ( logger.isDebugEnabled() ) {
                    t0 = System.currentTimeMillis();
                    logger.debug(logPrefix + "running ...");
                }
                task.run();
                if ( logger.isDebugEnabled() ) { 
                    t1 = System.currentTimeMillis();
                    logger.debug(logPrefix + "running ... done (" + (t1-t0) + " ms)");
                }
            } catch ( Exception e ) {
                logger.warn(logPrefix + "exception running task", e );
            }

            /* Increment the number of counts */
            this.count++;
            
            /* Notify anyone waiting */
            synchronized (this) {
                notifyAll();
            }
        }

        logger.debug(logPrefix + "stopping ..." );
        this.state = PulseState.DEAD;
        this.thread = null;
    }

    private long getCount()
    {
        return this.count;
    }
}
