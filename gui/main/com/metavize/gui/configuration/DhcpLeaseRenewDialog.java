/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: DhcpLeaseRenewDialog.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.configuration;

import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.util.Util;

import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.NetworkingConfiguration;

import javax.swing.*;


final public class DhcpLeaseRenewDialog extends MOneButtonProgressJDialog {
    
    private NetworkingConfiguration networkingConfiguration = null;
    
    public DhcpLeaseRenewDialog() {
        this.setTitle("Renewing DHCP Lease");
        this.labelJLabel.setText("Renewing DHCP Lease");
        this.messageJLabel.setText("<html><center>You have requested that EdgeGuard contact the network's DHCP<br>server in order to renew its lease on DHCP settings.</center></html>");
        new DhcpLeaseRenewThread();
        this.setVisible(true);
    }
    
    public NetworkingConfiguration getNetworkingConfiguration(){
        return networkingConfiguration;
    }

    public void windowClosing(java.awt.event.WindowEvent windowEvent) {}

    private void proceedJButtonActionPerformed(java.awt.event.ActionEvent evt) {
	setVisible(false);
	dispose();
    }
        
    private class DhcpLeaseRenewThread extends Thread {
        public DhcpLeaseRenewThread(){
            DhcpLeaseRenewDialog.this.jProgressBar.setValue(0);
            DhcpLeaseRenewDialog.this.jProgressBar.setIndeterminate(true);
            DhcpLeaseRenewDialog.this.jProgressBar.setString("Renewing DHCP lease...");
	    DhcpLeaseRenewDialog.this.proceedJButton.setEnabled(false);
            this.start();
        }
        public void run(){
            try{
                DhcpLeaseRenewDialog.this.networkingConfiguration = Util.getNetworkingManager().renewDhcpLease();
                Thread.currentThread().sleep(2000l);
                SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
                    DhcpLeaseRenewDialog.this.jProgressBar.setIndeterminate(false);
                    DhcpLeaseRenewDialog.this.jProgressBar.setString("DHCP lease renewed.  Proceeding.");
                }});
                Thread.currentThread().sleep(2000l);
                DhcpLeaseRenewDialog.this.setVisible(false);
                DhcpLeaseRenewDialog.this.dispose();
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error: unable to renew DHCP lease", e);
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
                    DhcpLeaseRenewDialog.this.jProgressBar.setIndeterminate(false);
                    DhcpLeaseRenewDialog.this.jProgressBar.setString("DHCP lease renewal failure.  Please try again later.");
		    DhcpLeaseRenewDialog.this.proceedJButton.setEnabled(true);
                }});
            }

        }
    }
    
    
}
