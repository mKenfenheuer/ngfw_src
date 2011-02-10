/* $HeadURL$ */
package com.untangle.uvm.node.firewall.ip;

import java.net.InetAddress;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.networking.IPNetwork;

/**
 * An interface to test for an address.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class IPMatcher
{
    private static IPMatcher ANY_MATCHER = new IPMatcher("any");
    private static IPMatcher NIL_MATCHER = new IPMatcher("none");
    private static IPMatcher INTERNAL_MATCHER = new IPMatcher("internal");
    private static IPMatcher EXTERNAL_MATCHER = new IPMatcher("external");

    private final Logger logger = Logger.getLogger(getClass());

    private static LinkedList<IPNetwork> internalNetworkList = null;
    
    public String matcher;

    public enum IPMatcherType { ANY, NONE, SINGLE, RANGE, SUBNET, INTERNAL, EXTERNAL, LIST };
    
    /**
     * The type of this matcher
     */
    private IPMatcherType type = IPMatcherType.NONE;

    /**
     * if this port matcher is a list of port matchers, this list stores the children
     */
    private LinkedList<IPMatcher> children = null;

    /**
     * if its a range these two variable store the min and max
     */
    private long rangeMin = -1;
    private long rangeMax = -1;

    /**
     * if its a subnet matcher 
     */
    private long subnetNetwork = -1;
    private long subnetNetmask = -1;
    
    /**
     * if its just an int matcher this stores the number
     */
    private InetAddress single = null;

    public IPMatcher( String matcher )
    {
        initialize(matcher);
    }

    /**
     * Make a subnet matcher
     */
    public IPMatcher( IPAddress network, IPAddress netmask )
    {
        this.type = IPMatcherType.SUBNET;
        this.subnetNetwork = addrToLong(network.getAddr());
        this.subnetNetmask = addrToLong(netmask.getAddr());
    }
    
    /**
     * Return true if <param>address</param> matches this matcher.
     *
     * @param address The address to test
     * @return True if the <param>address</param> matches.
     */
    public boolean isMatch( InetAddress address )
    {
        long tmp;
        
        switch (this.type) {

        case ANY:
            return true;

        case NONE:
            return false;

        case SINGLE:
            if (this.single.equals(address))
                return true;
            return false;
            
        case RANGE:
            tmp = addrToLong( address );
            return (( this.rangeMin <= tmp ) && ( tmp <= this.rangeMax ));

        case SUBNET:
            tmp = addrToLong( address );
            return (( tmp & this.subnetNetmask ) == this.subnetNetwork );
            
        case INTERNAL:
            tmp = addrToLong( address );
            for ( IPNetwork network : internalNetworkList ) {
                long subNetwork = addrToLong(network.getNetwork().getAddr());
                long subNetmask = addrToLong(network.getNetmask().getAddr());
                if (( tmp & subNetmask ) == subNetwork )
                    return true;
            }
            return false;
            
        case EXTERNAL:
            /* same as internal, but inverted */
            tmp = addrToLong( address );
            for ( IPNetwork network : internalNetworkList ) {
                long subNetwork = addrToLong(network.getNetwork().getAddr());
                long subNetmask = addrToLong(network.getNetmask().getAddr());
                if (( tmp & subNetmask ) == subNetwork )
                    return false;
            }
            return true;

        case LIST:
            for (IPMatcher child : this.children) {
                if (child.isMatch(address))
                    return true;
            }
            return false;

        default:
            logger.warn("Unknown IP matcher type: " + this.type);
            return false;
        }


    }

    /**
     * Retrieve the database representation of this port matcher.
     *
     * @return The database representation of this port matcher.
     */
    public String toDatabaseString()
    {
        return matcher;
    }

    /**
     * return toDatabaseString()
     */
    public String toString()
    {
        return toDatabaseString();
    }

    /**
     * Initialize all the private variables
     */
    private void initialize( String matcher )
    {
        this.matcher = matcher;

        /**
         * If it contains a comma it must be a list of port matchers
         * if so, go ahead and initialize the children
         */
        if (matcher.contains(",")) {
            this.type = IPMatcherType.LIST;

            this.children = new LinkedList<IPMatcher>();

            String[] results = matcher.split(",");
            
            /* check each one */
            for (String childString : results) {
                IPMatcher child = new IPMatcher(childString);
                this.children.add(child);
            }

            return;
        }

        /**
         * Check the common constants
         */
        if ("any".equals(matcher))  {
            this.type = IPMatcherType.ANY;
            return;
        }
        if ("all".equals(matcher)) {
            this.type = IPMatcherType.ANY;
            return;
        }
        if ("none".equals(matcher)) {
            this.type = IPMatcherType.NONE;
            return;
        }
        if ("internal".equals(matcher)) {
            this.type = IPMatcherType.INTERNAL;
            return;
        }
        if ("external".equals(matcher)) {
            this.type = IPMatcherType.EXTERNAL;
            return;
        }
        
        /**
         * If it contains a dash it must be a range
         */
        if (matcher.contains("-")) {
            this.type = IPMatcherType.RANGE;
            
            String[] results = matcher.split("-");

            if (results.length != 2) {
                logger.warn("Invalid IPMatcher: Invalid Range: " + matcher);
                throw new java.lang.IllegalArgumentException("Invalid IPMatcher: Invalid Range: " + matcher);
            }

            try {
                InetAddress addrMin = IPAddress.parse(results[0]).getAddr();
                InetAddress addrMax = IPAddress.parse(results[1]).getAddr();

                this.rangeMin = addrToLong(addrMin);
                this.rangeMax = addrToLong(addrMax);
            } catch (ParseException e) {
                logger.warn("Unknown IPMatcher format: \"" + matcher + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (parse exception)", e);
            } catch (java.net.UnknownHostException e) {
                logger.warn("Unknown IPMatcher format: \"" + matcher + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (unknown host)", e);
            } catch (Exception e) {
                logger.warn("Unknown IPMatcher format: \"" + matcher + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (exception)", e);
            }
 
            return;
        }

        /**
         * If it contains a slash it must be a subnet matcher
         */
        if (matcher.contains("/")) {
            this.type = IPMatcherType.SUBNET;
            
            String[] results = matcher.split("/");

            if (results.length != 2) {
                logger.warn("Invalid IPMatcher: Invalid Subnet: " + matcher);
                throw new java.lang.IllegalArgumentException("Invalid IPMatcher: Invalid Subnet: " + matcher);
            }

            try {
                InetAddress addrNetwork = IPAddress.parse(results[0]).getAddr();
                this.subnetNetwork = addrToLong(addrNetwork);

                /**
                 * The netmask can be a IP (255.255.0.0) or a number (16)
                 */
                this.subnetNetwork = -1;

                /* First try to see if its a number */
                try {
                    this.subnetNetmask = cidrToLong(Integer.parseInt(results[1]));
                }
                catch ( NumberFormatException e ) {
                    /* It must be an IP address*/
                }

                /* If that didnt work it must be a IP */
                if (subnetNetmask == -1) {
                    this.subnetNetmask = addrToLong(IPAddress.parse(results[1]).getAddr());
                }

            } catch (ParseException e) {
                logger.warn("Unknown IPMatcher format: \"" + matcher + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (parse exception)", e);
            } catch (java.net.UnknownHostException e) {
                logger.warn("Unknown IPMatcher format: \"" + matcher + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (unknown host)", e);
            } catch (Exception e) {
                logger.warn("Unknown IPMatcher format: \"" + matcher + "\"", e);
                throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (exception)", e);
            }
 
            return;
        }
        
        /**
         * if it isn't any of these it must be a basic SINGLE matcher
         */
        this.type = IPMatcherType.SINGLE;
        try {
            IPAddress addr = IPAddress.parse(matcher);
            this.single = addr.getAddr();
        } catch (ParseException e) {
            logger.warn("Unknown IPMatcher format: \"" + matcher + "\"", e);
            throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (parse exception)", e);
        } catch (java.net.UnknownHostException e) {
            logger.warn("Unknown IPMatcher format: \"" + matcher + "\"", e);
            throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (unknown host)", e);
        } catch (Exception e) {
            logger.warn("Unknown IPMatcher format: \"" + matcher + "\"", e);
            throw new java.lang.IllegalArgumentException("Unknown IPMatcher format: \"" + matcher + "\" (exception)", e);
        }

        return;
    }

    /* Number of bytes in an IPv4 address */
    static final int INADDRSZ = 4; /* XXX IPv6 */

    /* An array of the CIDR values */
    static final String CIDR_STRINGS[] = 
    {
        "0.0.0.0",         "128.0.0.0",       "192.0.0.0",       "224.0.0.0",
        "240.0.0.0",       "248.0.0.0",       "252.0.0.0",       "254.0.0.0",
        "255.0.0.0",       "255.128.0.0",     "255.192.0.0",     "255.224.0.0",
        "255.240.0.0",     "255.248.0.0",     "255.252.0.0",     "255.254.0.0",
        "255.255.0.0",     "255.255.128.0",   "255.255.192.0",   "255.255.224.0",
        "255.255.240.0",   "255.255.248.0",   "255.255.252.0",   "255.255.254.0",
        "255.255.255.0",   "255.255.255.128", "255.255.255.192", "255.255.255.224",
        "255.255.255.240", "255.255.255.248", "255.255.255.252", "255.255.255.254",
        "255.255.255.255"
    };

    /* Should be an unmodifiable list or vector */
    static final InetAddress CIDR_CONVERTER[] = new InetAddress[CIDR_STRINGS.length];

    private static long addrToLong( InetAddress address )
    {
        long val = 0;
        
        byte valArray[] = address.getAddress();
        
        for ( int c = 0 ; c < INADDRSZ ; c++ ) {
            val += ((long)byteToInt(valArray[c])) << ( 8 * ( INADDRSZ - c - 1 ));
        }
        
        return val;
    }

    /**
     * Convert a CIDR index to an InetAddress.
     *
     * @param cidr CIDR index to convert.
     * @return the InetAddress that corresponds to <param>cidr</param>.
     */
    private InetAddress cidrToInetAddress( int cidr ) throws ParseException
    {
        if ( cidr < 0 || cidr > CIDR_CONVERTER.length ) {
            throw new ParseException( "CIDR notation[" + cidr + "] should end with a number between 0 and " + CIDR_CONVERTER.length );
        }

        return CIDR_CONVERTER[cidr];
    }

    /**
     * Convert a CIDR index to a long.
     *
     * @param cidr CIDR index to convert.
     * @return the long that corresponds to <param>cidr</param>.
     */    
    private long cidrToLong( int cidr ) throws ParseException
    {
        return addrToLong( cidrToInetAddress( cidr ));
    }

    private static int byteToInt( byte val )
    {
        int num = val;
        if ( num < 0 ) num = num & 0x7F + 0x80;
        return num;
    }
    
    public static IPMatcher getAnyMatcher()
    {
        return ANY_MATCHER;
    }

    public static IPMatcher getNilMatcher()
    {
        return NIL_MATCHER;
    }
    
    public static IPMatcher getInternalMatcher()
    {
        return INTERNAL_MATCHER;
    }

    public static IPMatcher getExternalMatcher()
    {
        return EXTERNAL_MATCHER;
    }
    

    public static IPMatcher makeSubnetMatcher( IPAddress network, IPAddress netmask )
    {
        return new IPMatcher( network, netmask );
    }

    /**
     * Update the internal network with a list of networks.
     * 
     * @param networkList The list of networks that are on the internal interface.
     */    
    public static synchronized void setInternalNetworks( LinkedList<IPNetwork> networkList )
    {
        internalNetworkList = networkList;
    }

    public IPMatcherType getType()
    {
        return this.type;
    }

    static
    {
        int c = 0;
        for ( String cidr : CIDR_STRINGS ) {
            try {
                CIDR_CONVERTER[c++] = InetAddress.getByName( cidr );
            } catch ( java.net.UnknownHostException e ) {
                System.err.println( "Invalid CIDR String at index: " + c );
            }
        }
    }

}
