/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.jnetcap;

import java.net.InetAddress;

import java.util.EmptyStackException;

public class NetcapUDPSession extends NetcapSession 
{
    protected static final int MERGED_DEAD = 0xDEAD00D;
    
    /** These cannot conflict with the flags inside of NetcapTCPSession and NetcapSession */
    private final static int FLAG_TTL            = 64;
    private final static int FLAG_TOS            = 65;
    private final static int FLAG_ICMP_CLIENT_ID = 66;
    private final static int FLAG_ICMP_SERVER_ID = 67;
    
    private final PacketMailbox clientMailbox;
    private final PacketMailbox serverMailbox;
    
    public NetcapUDPSession( int id ) 
    {
        super( id, Netcap.IPPROTO_UDP );           
        
        clientMailbox = new UDPSessionMailbox( true );
        serverMailbox = new UDPSessionMailbox( false );
    }
    
    public PacketMailbox clientMailbox() { return clientMailbox; }    
    public PacketMailbox serverMailbox() { return serverMailbox; }
    
    public byte ttl()
    { 
        return (byte) getIntValue( FLAG_TTL, pointer.value()); 
    }

    public byte tos()
    { 
        return (byte) getIntValue( FLAG_TOS, pointer.value());
    }

    public boolean isIcmpSession()
    {
        /* Only ICMP sessions have non-zero ICMP client ids */
        return ( clientSide.client().port() == 0 && clientSide.server().port() == 0 );
    }

    public int icmpClientId()
    { 
        return  getIntValue( FLAG_ICMP_CLIENT_ID, pointer.value());
    }
    
    public int icmpServerId()
    {
        return  getIntValue( FLAG_ICMP_SERVER_ID, pointer.value());
    }
    
    protected Endpoints makeEndpoints( boolean ifClient ) 
    {
        return new SessionEndpoints( ifClient );
    }
    
    /**
     * Merge this session with any other UDP sessions started at the same time.</p>
     * @param traffic - Description of the traffic going to the server (dst should refer
     *                  to the server endpoint).
     * @return Returns whether or not the session was merged, or merged out.  True If this session
     *         should continue, false if this session was merged out.
     */
    public boolean merge( IPTraffic traffic )
    {
        int ret  = merge( pointer.value(),
                          Inet4AddressConverter.toLong( traffic.dst().host()), traffic.dst().port(),
                          Inet4AddressConverter.toLong( traffic.src().host()), traffic.src().port());
        
        if ( ret == MERGED_DEAD ) {
            return false;
        } else if ( ret == 0 ) {
            return true;
        } else {
            Netcap.error( "Invalid merge" );
        }
        
        return false;
    }

    /**
     * Merge this session with any other ICMP (treated as UDP for now) sessions started at the same time.</p>
     * @param traffic - Description of the traffic going to the server (dst should refer
     *                  to the server endpoint).
     * @return Returns whether or not the session was merged, or merged out.  True If this session
     *         should continue, false if this session was merged out.
     */
    public boolean icmpMerge( IPTraffic traffic, int id )
    {
        int ret  = icmpMerge( pointer.value(), id,
                              Inet4AddressConverter.toLong( traffic.dst().host()),
                              Inet4AddressConverter.toLong( traffic.src().host()));
        
        if ( ret == MERGED_DEAD ) {
            return false;
        } else if ( ret == 0 ) {
            return true;
        } else {
            Netcap.error( "Invalid merge" );
        }
        
        return false;
    }

    
    private static native long   read( long sessionPointer, boolean ifClient, int timeout );
    private static native byte[] data( long packetPointer );
    private static native int    getData( long packetPointer, byte[] buffer );
    
    /**
     * Merge this session with any other UDP session that may have started in the reverse
     * direction.</p>
     *
     * @param sessionPointer - Pointer to the udp session.
     * @param srcAddr - Source address(server side, server address)
     * @param srcPort - Source port(server side, server port)
     * @param dstAddr - Destination address(server side, client address)
     * @param dstPort - Destination port(server side, client port)
     */
    private static native int    merge( long sessionPointer,
                                        long srcAddr, int srcPort, long dstAddr, int dstPort );

    /**
     * Merge this session with any other ICMP/UDP session that may have started in the reverse
     * direction.</p>
     *
     * @param sessionPointer - Pointer to the udp session.
     * @param id - Session identifier in the ICMP message.
     * @param srcAddr - Source address(server side, server address)
     * @param dstAddr - Destination address(server side, client address)
     */
    private static native int    icmpMerge( long sessionPointer, int id, long srcAddr, long dstAddr );

    private static native int    mailboxPointer( long sessionPointer, boolean ifClient );
    
    /* This is for sending the data associated with a netcap_pkt_t structure */
    private static native int  send( long packetPointer );
    
    class UDPSessionMailbox implements PacketMailbox
    {
        private final boolean ifClient;

        UDPSessionMailbox( boolean ifClient ) {
            this.ifClient = ifClient;
        }

        public Packet read( int timeout )
        {
            CPointer packetPointer = new CPointer( NetcapUDPSession.read( pointer.value(), ifClient, timeout ));
            
            IPTraffic ipTraffic = new IPTraffic( packetPointer );
            
            switch ( ipTraffic.protocol()) {
            case Netcap.IPPROTO_UDP:
                return new PacketMailboxUDPPacket( packetPointer );
            case Netcap.IPPROTO_ICMP:
                return new PacketMailboxICMPPacket( packetPointer );
            default:
                int tmp = ipTraffic.protocol();

                /* Must free the packet */
                ipTraffic.raze();
                throw new IllegalStateException( "Packet is neither ICMP or UDP: " +  tmp );
            }
        }

        public Packet read() 
        {
            return read( 0 );
        }

        public int pointer()
        {
            return NetcapUDPSession.mailboxPointer( pointer.value(), ifClient );
        }

        abstract class PacketMailboxPacket implements Packet
        {
            private final CPointer pointer;
            protected final IPTraffic traffic;
            
            PacketMailboxPacket( CPointer pointer ) 
            {
                this.pointer = pointer;
                this.traffic = makeTraffic( pointer );
            }
            
            public IPTraffic traffic()
            {
                return traffic;
            }
            
            public byte[] data() 
            {
                return NetcapUDPSession.data( pointer.value());
            }

            public int getData( byte[] buffer )
            {
                return NetcapUDPSession.getData( pointer.value(), buffer );
            }

            /**
             * Send out this packet 
             */
            public void send() 
            {
                NetcapUDPSession.send( pointer.value());
            }

            public void raze() 
            {
                traffic.raze();
            }
            
            protected abstract IPTraffic makeTraffic( CPointer pointer );
        }
        
        class PacketMailboxUDPPacket extends PacketMailboxPacket implements UDPPacket
        {
            PacketMailboxUDPPacket( CPointer pointer )
            {
                super( pointer );
            }

            protected IPTraffic makeTraffic( CPointer pointer )
            {
                return new IPTraffic( pointer );
            }
        }
        
        class PacketMailboxICMPPacket extends PacketMailboxPacket implements ICMPPacket
        {
            final byte icmpType;
            final byte icmpCode;
            
            PacketMailboxICMPPacket( CPointer pointer )
            {
                super( pointer );
                
                icmpType = ((ICMPTraffic)traffic).icmpType();
                icmpCode = ((ICMPTraffic)traffic).icmpCode();
            }
            
            public byte icmpType()
            {
                return icmpType;
            }
            
            public byte icmpCode()
            {
                return icmpCode;
            }

            public InetAddress icmpSource( byte data[], int limit )
            {
                return ((ICMPTraffic)traffic).icmpSource( data, limit );
            }
            
            protected IPTraffic makeTraffic( CPointer pointer )
            {
                return new ICMPTraffic( pointer );
            }

        }

    }
}
