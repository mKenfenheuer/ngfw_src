/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.net.InetAddress;

import com.metavize.mvvm.tapi.Protocol;

import com.metavize.mvvm.NetworkingConfiguration;

import com.metavize.mvvm.MvvmContextFactory;

import com.metavize.mvvm.tapi.AbstractEventHandler;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.UDPNewSessionRequest;
import com.metavize.mvvm.tapi.MPipeException;

import com.metavize.mvvm.tapi.event.TCPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.UDPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.UDPSessionEvent;
import com.metavize.mvvm.tapi.event.TCPSessionEvent;

import com.metavize.mvvm.tapi.IPSession;
import com.metavize.mvvm.tapi.UDPSession;

import com.metavize.mvvm.tran.Transform;
import org.apache.log4j.Logger;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.IntfMatcher;

class NatEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(NatEventHandler.class);
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();

    /* Number of milliseconds to wait in between updating the address and updating the mvvm */
    private static final int SLEEP_TIME = 1000;
    
    private static final int TCP_NAT_PORT_START = 10000;
    private static final int TCP_NAT_PORT_END   = 60000;

    private static final int UDP_NAT_PORT_START = 10000;
    private static final int UDP_NAT_PORT_END   = 60000;

    private static final int ICMP_PID_START     = 1;
    private static final int ICMP_PID_END       = 60000;

    /* match to determine whether a session is natted */
    /* XXX Probably need to initialized this with a value */
    private RedirectMatcher nat;

    /* match to determine  whether a session is directed for the dmz */
    /* XXX Probably need to initialized this with a value */
    private RedirectMatcher dmz;

    /* All of the other rules */
    /* Use an empty list rather than null */
    private List<RedirectMatcher> redirectList = new LinkedList<RedirectMatcher>();

    /* tracks the open TCP ports for NAT */
    private final PortList tcpPortList;
    
    /* Tracks the open UDP ports for NAT */
    private final PortList udpPortList;
    
    /* Tracks the open ICMP identifiers, Not exactly a port, but same kind of thing */
    private final PortList icmpPidList;

    /* The internal address */
    private IPaddr internalAddress;
    private IPaddr internalSubnet;
    
    /* Setup  */
    NatEventHandler()
    {
        tcpPortList = PortList.makePortList( TCP_NAT_PORT_START, TCP_NAT_PORT_END );
        udpPortList = PortList.makePortList( UDP_NAT_PORT_START, UDP_NAT_PORT_END );
        icmpPidList = PortList.makePortList( ICMP_PID_START, ICMP_PID_END );
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequestEvent event )
        throws MPipeException
    {        
        handleNewSessionRequest( event.sessionRequest(), Protocol.TCP );
    }

    public void handleUDPNewSessionRequest( UDPNewSessionRequestEvent event )
        throws MPipeException
    {
        handleNewSessionRequest( event.sessionRequest(), Protocol.UDP );
    }

    private void handleNewSessionRequest( IPNewSessionRequest request, Protocol protocol )
    {
        /* Check for NAT, Redirects or DMZ */
        if ( isNat(  request, protocol ) ||
             isRedirect( request, protocol ) || 
             isDmz(  request,  protocol )) {
            request.release(true);
            return;
        }
        
        /* If nat is on, and this session wasn't natted, redirected or dmzed, it
         * must be rejected */
        if ( nat.isEnabled()) {
            /* Increment the block counter */
            incrementCount( Transform.GENERIC_0_COUNTER ); // BLOCK COUNTER
            
            /* XXX How should the session be rejected */
            request.rejectSilently();
            return;
        } 
        
        /* Otherwise release the session, and don't care about the finalization */
        request.release(false);
    }

    public void handleTCPFinalized(TCPSessionEvent event)
        throws MPipeException
    {
        releasePort( Protocol.TCP, event.ipsession());
    }
    
    public void handleUDPFinalized(UDPSessionEvent event)
        throws MPipeException
    {
        /* XXX Special case ICMP */
        UDPSession udpsession = (UDPSession)event.ipsession();
        
        if ( udpsession.isPing()) {
            Boolean isNatPid = (Boolean)udpsession.attachment();
            int pid = udpsession.icmpId();

            if ( isNatPid == null ) {
                logger.error( "null attachment on Natd session" );
                return;
            }
            if ( isNatPid ) {
                logger.debug( "ICMP: Releasing pid: " + pid );
                
                icmpPidList.releasePort( pid );
            } else {
                logger.debug( "Ignoring non-natted pid: " + pid );
            }
        } else {
            releasePort( Protocol.UDP, udpsession );
        }
    }
    
    void configure( NatSettings settings, NetworkingConfiguration netConfig )
    {
        IPMatcher localHostMatcher = IPMatcher.MATCHER_LOCAL;

        if ( settings.getNatEnabled()) {
            internalAddress = settings.getNatInternalAddress();
            internalSubnet = settings.getNatInternalSubnet();

            /* Create the nat redirect */
            IPMatcher tmp = new IPMatcher( internalAddress, internalSubnet, false );

            /* XXX Update to use local host */
            nat = new RedirectMatcher( true, ProtocolMatcher.MATCHER_ALL,
                                       IntfMatcher.MATCHER_IN, IntfMatcher.MATCHER_ALL,
                                       tmp, IPMatcher.MATCHER_ALL, 
                                       PortMatcher.MATCHER_ALL, PortMatcher.MATCHER_ALL,
                                       false, null, -1 );
            
            enableNat();
        } else {
            nat = RedirectMatcher.MATCHER_DISABLED;
            disableNat();
        }

        /* Configure the DMZ */
        if ( settings.getDmzEnabled()) {
            dmz = new RedirectMatcher( true, ProtocolMatcher.MATCHER_ALL,
                                       IntfMatcher.MATCHER_OUT, IntfMatcher.MATCHER_ALL,
                                       IPMatcher.MATCHER_ALL, localHostMatcher,
                                       PortMatcher.MATCHER_ALL, PortMatcher.MATCHER_ALL,
                                       true, settings.getDmzAddress().getAddr(), -1 );
        } else {
            dmz = RedirectMatcher.MATCHER_DISABLED;
        }
        
        /* Empty out the list */
        redirectList.clear();

        List<RedirectRule> list = (List<RedirectRule>)settings.getRedirectList();

        if ( list == null ) {
            logger.error( "Settings contain null redirect list" );
        } else {
            /* Update all of the rules */
            for ( Iterator<RedirectRule> iter = list.iterator() ; iter.hasNext() ; ) {
                redirectList.add( new RedirectMatcher( iter.next()));
            }
        }        
    }

    void deconfigure()
    {
        /* Bring down NAT */
        disableNat();
    }


    RedirectMatcher getNat()
    {
        return nat;
    }
    

    RedirectMatcher getDmz()
    {
        return dmz;
    }
    
    List <RedirectMatcher> getRedirectList()
    {
        return redirectList;
    }
    
    /**
     * Determine if a session is natted, and if necessary, rewrite its session information.
     */
    private boolean isNat( IPNewSessionRequest request, Protocol protocol )
    {
        int port;
        
        request.attach( Boolean.FALSE );
        
        if ( nat.isMatch( request, protocol )) {
            /* Check to see if this is redirect, check before changing the source address */
            isRedirect(  request, protocol );

            /* Change the source in the request */
            /* All redirecting occurs here */
            InetAddress localAddr = IPMatcher.getLocalAddress();
            // Unfortunately, as of 5/02/05, this can sometimes be null, probably due to
            // initialization order, the sleeping involved, etc.  For now, just make sure
            // not to ever change the client addr to null, which causes awful things to happen.  jdi XXX
            if (localAddr == null) {
                logger.warn("null localAddr in isNat");
                // We return false, which will cause the session to be blocked later.
                return false;
            }

            request.clientAddr(localAddr);
            
            /* Set the client port */
            /* XXX THIS IS A HACK, it really should check if the protocol is ICMP, but
             * for now there are only UDP sessions */
            if ( request.clientPort() == 0 && request.serverPort() == 0 ) {
                port = icmpPidList.getNextPort();
                ((UDPNewSessionRequest)request).icmpId( port );
                request.attach( Boolean.TRUE );
                logger.debug( "Redirect PING session to id: " + port );
            } else {
                port = getNextPort( protocol );
                request.clientPort( port );
                
                /* Attach that the port is Natd */
                request.attach( Boolean.TRUE );
                logger.debug( "Redirecting session to port: " + port );
            }

            /* Increment the NAT counter */
            incrementCount( Transform.GENERIC_1_COUNTER ); // NAT COUNTER
            
            return true;
        }

        return false;
    }

    /**
     * Determine if a session is redirected, and if necessary, rewrite its session information.
     */
    private boolean isRedirect( IPNewSessionRequest request, Protocol protocol )
    {
        for ( Iterator<RedirectMatcher> iter = redirectList.iterator(); iter.hasNext(); ) {
            RedirectMatcher matcher = iter.next();
            
            if ( matcher.isMatch( request, protocol )) {
                /* Redirect the session */
                matcher.redirect( request );

                /* Increment the NAT counter */
                incrementCount( Transform.GENERIC_2_COUNTER ); // REDIR COUNTER

                return true;
            }
        }
        return false;
    }

    /**
     * Determine if a session is for the DMZ, and if necessary, rewrite its session information.
     */
    private boolean isDmz( IPNewSessionRequest request, Protocol protocol )
    {
        if ( dmz.isMatch( request, protocol )) {
            dmz.redirect( request );
            
            /* Increment the DMZ counter */
            incrementCount( Transform.GENERIC_3_COUNTER ); // DMZ COUNTER

            return true;
        }
        return false;
    }


    /**
     * Retrieve the next port from the port list
     */
    private int getNextPort( Protocol protocol )
    {                
        return getPortList( protocol ).getNextPort();
    }

    /**
     * Release a port and place and back onto the port list
     */
    private void releasePort( Protocol protocol, IPSession session )
    {
        int port = session.clientPort();
        
        PortList pList = getPortList( protocol );

        Boolean isNatPort = (Boolean)session.attachment();
        
        if ( isNatPort == null ) {
            logger.error( "null attachment on Natd session" );
            return;
        }

        if ( isNatPort ) {
            logger.debug( "Releasing port: " + port );
            
            getPortList( protocol ).releasePort( port );
        } else {
            logger.debug( "Ignoring non-natted port: " + port );
        }
    }

    private PortList getPortList( Protocol protocol )
    {
        if ( protocol == Protocol.UDP ) {
            return udpPortList;
        } else if ( protocol == Protocol.TCP ) {
            return tcpPortList;
        }

        throw new IllegalArgumentException( "Unknown protocol: " + protocol );
    }

    private void enableNat()
    {
        int code;
        
        /* Wait for this to finish */
        try {
            Process p = Runtime.getRuntime().exec( "/sbin/ifconfig br0:0 " + internalAddress.toString() + 
                                                   " netmask " + internalSubnet.toString());
            
            code = p.waitFor();
        } catch ( Exception e ) {
            logger.error( "Interrupting while reconfiguring interface" );
            /* XXXXXXXXXXX, not nice to throw these, but they don't have to be declared */
            throw new IllegalStateException( "Interrupting while reconfiguring interface", e );
        }

        if ( code != 0 ) {
            throw new IllegalStateException( "Unable to update the internal address" );
        }        
        
        /* Sleep a second while the "dust settles" */
        try {
            Thread.sleep( SLEEP_TIME );
        } catch ( Exception e ) {
            logger.error( "Interrupting while reconfiguring interface" );
            /* XXXXXXXXXXX, not nice to throw these, but they don't have to be declared */
            throw new IllegalStateException( "Interrupting while reconfiguring interface", e );
        }
           
        /* Reconfigure the MVVM */
        MvvmContextFactory.context().argonManager().updateAddress();
        MvvmContextFactory.context().argonManager().disableLocalAntisubscribe();
    }
   
    private void disableNat()
    {
        int code;
        
        /* Wait for this to finish */
        try {
            /* Bring down interface br0:0 */
            Process p = Runtime.getRuntime().exec( "/sbin/ifconfig br0:0 down" );

            code = p.waitFor();
        } catch ( Exception e ) {
            logger.error( "Interrupting while reconfiguring interface" );
            /* XXXXXXXXXXX, not nice to throw these, but they don't have to be declared */
            throw new IllegalStateException( "Interrupting while reconfiguring interface", e );
        }

        if ( code != 0 ) {
            logger.error( "Error bringing down br0:0, ignoring" );
        }        
        
        /* Sleep a second while the "dust settles" */
        try {
            Thread.sleep( SLEEP_TIME );
        } catch ( Exception e ) {
            logger.error( "Interrupting while reconfiguring interface" );
            /* XXXXXXXXXXX, not nice to throw these, but they don't have to be declared */
            throw new IllegalStateException( "Interrupting while reconfiguring interface", e );
        }
           
        /* Reconfigure the MVVM */
        MvvmContextFactory.context().argonManager().updateAddress();

        /* Don't catch traffic to the local host */
        MvvmContextFactory.context().argonManager().enableLocalAntisubscribe();
    }
}
