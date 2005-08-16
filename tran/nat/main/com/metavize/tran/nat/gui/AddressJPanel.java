/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: AddressJPanel.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.tran.nat.gui;


import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.tran.firewall.*;
import com.metavize.tran.nat.*;


import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

public class AddressJPanel extends MEditTableJPanel{

            
    public AddressJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( false );
        super.setInsets(new java.awt.Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        AddressTableModel addressTableModel = new AddressTableModel();
        this.setTableModel( addressTableModel );

    }
    
    



class AddressTableModel extends MSortedTableModel{ 

    
    private static final int  T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int  C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int  C2_MW = 130;  /* MAC address */
    private static final int  C3_MW = 130;  /* current static target IP */
    private static final int  C4_MW = 130;  /* current IP */
    private static final int  C5_MW = 100;  /* hostname */
    private static final int  C6_MW = 100;  /* current lease end */
    private static final int  C7_MW = 120;  /* category */
    
    private final int C8_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW + C7_MW), 120); /* description */


        
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0,  C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  C2_MW, true,  true,  false, false, String.class, "00:01:23:45:67:89", sc.html("MAC<br>address") );
        addTableColumn( tableColumnModel,  3,  C3_MW, true,  true,  false, false, String.class, "1.2.3.4", sc.html("target static<br>IP address") );
        addTableColumn( tableColumnModel,  4,  C4_MW, true,  false, false, false, String.class, "", sc.html("current<br>IP address") );
        addTableColumn( tableColumnModel,  5,  C5_MW, true,  false, false, false, String.class, "", sc.html("current<br>hostname") );
        addTableColumn( tableColumnModel,  6,  C6_MW, true,  false, false, false, String.class, "", sc.html("current<br>lease end") );
        addTableColumn( tableColumnModel,  7,  C7_MW, true,  true,  false, false, String.class, sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY);
        addTableColumn( tableColumnModel,  8,  C8_MW, true,  true,  false, true,  String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  9,  10,    false, false, true,  false, DhcpLeaseRule.class, null, "");
        return tableColumnModel;
    }

    
    public void generateSettings(Object settings, boolean validateOnly) throws Exception {
        NatSettings natSettings = (NatSettings) settings;        
        List leaseRulesList = new ArrayList();
        int rowIndex = 1;
        for( Vector rowVector : (Vector<Vector>) this.getDataVector() ){

            DhcpLeaseRule leaseRule = (DhcpLeaseRule) rowVector.elementAt(9);
            try{ leaseRule.setMacAddress( MACAddress.parse( (String)rowVector.elementAt(2)) ); }
            catch(Exception e){ throw new Exception("Invalid \"MAC address\" in row: " + rowIndex); }
            try{ leaseRule.setStaticAddress( IPNullAddr.parse( (String)rowVector.elementAt(3)) ); }
            catch(Exception e){ throw new Exception("Invalid \"target static IP address\" in row: " + rowIndex); }
            leaseRule.setCategory( (String) rowVector.elementAt(7) );
            leaseRule.setDescription( (String) rowVector.elementAt(8) );
            leaseRulesList.add(leaseRule);
	    rowIndex++;
        }
        
	// SAVE SETTINGS ////////
	if( !validateOnly ){
	    natSettings.setDhcpLeaseList(leaseRulesList);
	}
    }
    

    
    public Vector generateRows(Object settings) {    
        NatSettings natSettings = (NatSettings) settings;
        Vector allRowsVector = new Vector();
        int count = 1;
        for( DhcpLeaseRule leaseRule : (List<DhcpLeaseRule>) natSettings.getDhcpLeaseList() ){

	    Vector rowVector = new Vector();
	    rowVector.add(super.ROW_SAVED);
	    rowVector.add(new Integer(count));
	    rowVector.add(leaseRule.getMacAddress().toString());
	    rowVector.add(leaseRule.getStaticAddress().toString());
	    rowVector.add(leaseRule.getCurrentAddress().toString());
	    rowVector.add(leaseRule.getHostname());
	    rowVector.add(leaseRule.getEndOfLease().toString());
	    rowVector.add(leaseRule.getCategory());
	    rowVector.add(leaseRule.getDescription());
	    rowVector.add(leaseRule);
	    allRowsVector.add(rowVector);
	    count++;
        }
        return allRowsVector;
    }
    
}

}
