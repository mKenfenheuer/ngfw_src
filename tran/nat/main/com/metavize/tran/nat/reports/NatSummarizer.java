/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat.reports;

import java.sql.*;

import com.metavize.mvvm.reporting.BaseSummarizer;
import com.metavize.mvvm.reporting.Util;

import org.apache.log4j.Logger;

public class NatSummarizer extends BaseSummarizer {

    private static final Logger logger = Logger.getLogger(NatSummarizer.class);

    public NatSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {

	long totalOutboundSessionsCreated = 0l;
	long totalRedirections = 0l;
	long tcpRedirections = 0l;
	long tcpInboundRedirections = 0l;
	long tcpOutboundRedirections = 0l;
	long udpRedirections = 0l;
	long udpInboundRedirections = 0l;
	long udpOutboundRedirections = 0l;
	long pingRedirections = 0l;
	long pingInboundRedirections = 0l;
	long pingOutboundRedirections = 0l;
	long dmzRedirections = 0l;



        try {
            String sql;
	    PreparedStatement ps;
	    ResultSet rs;
	    
	    sql = " SELECT sum(nat_sessions), sum(dmz_sessions)," +
		" sum(tcp_incoming), sum(tcp_outgoing)," +
		" sum(udp_incoming), sum(udp_outgoing)," +
		" sum(icmp_incoming), sum(icmp_outgoing)" +
		" from tr_nat_statistic_evt evt where time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            totalOutboundSessionsCreated = rs.getLong(1);
	    dmzRedirections = rs.getLong(2);
	    tcpInboundRedirections = rs.getLong(3);
	    tcpOutboundRedirections = rs.getLong(4);
	    udpInboundRedirections = rs.getLong(5);
	    udpOutboundRedirections = rs.getLong(6);
	    pingInboundRedirections = rs.getLong(7);
	    pingOutboundRedirections = rs.getLong(8);
            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

	tcpRedirections = tcpInboundRedirections + tcpOutboundRedirections;
	udpRedirections = udpInboundRedirections + udpOutboundRedirections;
	pingRedirections = pingInboundRedirections + pingOutboundRedirections;
	totalRedirections = tcpRedirections + udpRedirections + pingRedirections;

        addEntry("Total redirections", Util.trimNumber("",totalRedirections) );

        addEntry("&nbsp;","&nbsp;");

        addEntry("TCP redirections", Util.trimNumber("",tcpRedirections));
        addEntry("&nbsp;&nbsp;&nbsp;Inbound", Util.trimNumber("",tcpInboundRedirections) 
		 + "(" + Util.percentNumber(tcpInboundRedirections, totalRedirections) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Outbound", Util.trimNumber("",tcpOutboundRedirections) 
		 + "(" + Util.percentNumber(tcpOutboundRedirections, totalRedirections) + ")");
        addEntry("UDP redirections", Util.trimNumber("",udpRedirections));
        addEntry("&nbsp;&nbsp;&nbsp;Inbound", Util.trimNumber("",udpInboundRedirections) 
		 + "(" + Util.percentNumber(udpInboundRedirections, totalRedirections) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Outbound", Util.trimNumber("",udpOutboundRedirections) 
		 + "(" + Util.percentNumber(udpOutboundRedirections, totalRedirections) + ")");
        addEntry("PING redirections", Util.trimNumber("",pingRedirections));
        addEntry("&nbsp;&nbsp;&nbsp;Inbound", Util.trimNumber("",pingInboundRedirections)  
		 + "(" + Util.percentNumber(pingInboundRedirections, totalRedirections) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Outbound", Util.trimNumber("",pingOutboundRedirections)  
		 + "(" + Util.percentNumber(pingOutboundRedirections, totalRedirections) + ")");
	
	addEntry("&nbsp;","&nbsp;");

        addEntry("NAT outbound sessions created", Util.trimNumber("",totalOutboundSessionsCreated));
        addEntry("DMZ inbound redirections", Util.trimNumber("",dmzRedirections) );




        // XXXX
        String tranName = "Network Sharing";

        return summarizeEntries(tranName);
    }
}

