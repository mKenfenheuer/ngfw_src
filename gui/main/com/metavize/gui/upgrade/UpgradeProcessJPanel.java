/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.upgrade;

import com.metavize.mvvm.toolbox.*;
import com.metavize.mvvm.policy.Policy;
import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.MEditTableJPanel;
import com.metavize.gui.widgets.editTable.MSortedTableModel;
import com.metavize.gui.util.Util;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.*;
import java.util.*;
import java.awt.Insets;

public class UpgradeProcessJPanel extends JPanel
    implements Refreshable<UpgradeCompoundSettings> {

    private static final int DOWNLOAD_SLEEP_MILLIS = 1000;
    private static final int DOWNLOAD_FINAL_SLEEP_MILLIS = 3000;
    		
    private MConfigJDialog mConfigJDialog;
    private UpgradeTableModel upgradeTableModel;
    private MEditTableJPanel mEditTableJPanel;

    public UpgradeProcessJPanel(MConfigJDialog mConfigJDialog) {
	this.mConfigJDialog = mConfigJDialog;

	// UPGRADE TABLE //
        mEditTableJPanel = new MEditTableJPanel(false, true);
        mEditTableJPanel.setInsets(new Insets(0,0,0,0));
        mEditTableJPanel.setTableTitle("Available Upgrades");
        mEditTableJPanel.setDetailsTitle("Upgrade Details");
        mEditTableJPanel.setAddRemoveEnabled(false);
        upgradeTableModel = new UpgradeTableModel();
        mEditTableJPanel.setTableModel( upgradeTableModel );
        mEditTableJPanel.getJTable().setRowHeight(49);

	initComponents();
    }
    

    public void doRefresh(UpgradeCompoundSettings upgradeCompoundSettings){
	upgradeTableModel.doRefresh(upgradeCompoundSettings);
    }


    class UpgradeTableModel extends MSortedTableModel<UpgradeCompoundSettings> {
	
	public TableColumnModel getTableColumnModel(){	    
	    DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
	    //                                 #  min  rsz    edit   remv   desc   typ            def
	    addTableColumn( tableColumnModel,  0,  30, false, false, false, false, Integer.class, null, "#");
	    addTableColumn( tableColumnModel,  1,  49, false, false, false, false, ImageIcon.class, null, "");
	    addTableColumn( tableColumnModel,  2,  49, false, false, false, false, ImageIcon.class, null, "");
	    addTableColumn( tableColumnModel,  3, 150, true,  false, false, false, String.class, null, "name");
	    addTableColumn( tableColumnModel,  4,  75, false, false, false, false, String.class, null, sc.html("new<br>version"));
	    addTableColumn( tableColumnModel,  5, 125, false, false, false, false, String.class, null, "type");
	    addTableColumn( tableColumnModel,  6,  70, true,  false, false, false, Integer.class, null, sc.html("size<br>(KB)"));
	    addTableColumn( tableColumnModel,  7, 125, false, false, true,  true,  String.class, null, "description");	    
	    return tableColumnModel;
	}
	
	public void generateSettings(UpgradeCompoundSettings upgradeCompoundSettings,
				     Vector<Vector> tableVector, boolean validateOnly) throws Exception { }
	
	public Vector<Vector> generateRows(UpgradeCompoundSettings upgradeCompoundSettings){
	    MackageDesc[] mackageDescs = upgradeCompoundSettings.getUpgradableMackageDescs();
	    Vector<Vector> allRows = new Vector<Vector>(mackageDescs.length);
	    Vector tempRow = null;
	    int rowIndex = 0;
	    
	    for( MackageDesc mackageDesc : mackageDescs ){
		if( mackageDesc.getType() == MackageDesc.CASING_TYPE ||
		    mackageDesc.getType() == MackageDesc.TRANSFORM_BASE_TYPE)
		    continue;
		try{
		    rowIndex++;
		    tempRow = new Vector(7);
		    tempRow.add( rowIndex );
		    
		    byte[] orgIcon = mackageDesc.getOrgIcon();
		    byte[] descIcon = mackageDesc.getDescIcon();
		    if( orgIcon != null)
			tempRow.add( new ImageIcon(orgIcon) );
		    else
			tempRow.add( new ImageIcon(getClass().getResource("/com/metavize/gui/transform/IconOrgUnknown42x42.png"))) ;
		    
		    if( descIcon != null)
			tempRow.add( new ImageIcon(descIcon) );
		    else
			tempRow.add( new ImageIcon(getClass().getResource("/com/metavize/gui/transform/IconDescUnknown42x42.png"))) ;
		    
		    tempRow.add( mackageDesc.getDisplayName() );
		    tempRow.add( mackageDesc.getAvailableVersion() );
		    if( mackageDesc.getType() == MackageDesc.SYSTEM_TYPE )
			tempRow.add( "System Component" );
		    else if( mackageDesc.getType() == MackageDesc.TRANSFORM_TYPE )
			tempRow.add( "Software Appliance" );
		    else
			tempRow.add( "Unknown" );
		    tempRow.add( Integer.toString(mackageDesc.getSize()/1000));
		    tempRow.add( mackageDesc.getLongDescription() );
		    allRows.add( tempRow );
		}
		catch(Exception e){
		    Util.handleExceptionNoRestart("Error adding upgrade row", e);
		}
	    }
	    return allRows;
	}
    }
    
        private void initComponents() {//GEN-BEGIN:initComponents
                java.awt.GridBagConstraints gridBagConstraints;

                contentJPanel = mEditTableJPanel;
                actionJPanel = new javax.swing.JPanel();
                upgradeJButton = new javax.swing.JButton();

                setLayout(new java.awt.GridBagLayout());

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 15, 0, 15);
                add(contentJPanel, gridBagConstraints);

                actionJPanel.setLayout(new java.awt.GridBagLayout());

                upgradeJButton.setFont(new java.awt.Font("Dialog", 0, 12));
                upgradeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/images/Button_Upgrade_EdgeGuard_130x17.png")));
                upgradeJButton.setDoubleBuffered(true);
                upgradeJButton.setFocusPainted(false);
                upgradeJButton.setFocusable(false);
                upgradeJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                upgradeJButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                upgradeJButton.setMaximumSize(new java.awt.Dimension(157, 25));
                upgradeJButton.setMinimumSize(new java.awt.Dimension(157, 25));
                upgradeJButton.setPreferredSize(new java.awt.Dimension(157, 25));
                upgradeJButton.addActionListener(new java.awt.event.ActionListener() {
                        public void actionPerformed(java.awt.event.ActionEvent evt) {
                                upgradeJButtonActionPerformed(evt);
                        }
                });

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = 0;
                gridBagConstraints.weightx = 0.5;
                gridBagConstraints.insets = new java.awt.Insets(0, 0, 15, 0);
                actionJPanel.add(upgradeJButton, gridBagConstraints);

                gridBagConstraints = new java.awt.GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = 1;
                gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(15, 0, 15, 0);
                add(actionJPanel, gridBagConstraints);

        }//GEN-END:initComponents

    private void upgradeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upgradeJButtonActionPerformed
	if( Util.getIsDemo() )
	    return;
	ProceedJDialog proceedJDialog = new ProceedJDialog();
	if( !proceedJDialog.isUpgrading() )
	    return;
	new PerformUpgradeThread();
    }//GEN-LAST:event_upgradeJButtonActionPerformed
		
    private class PerformUpgradeThread extends Thread {
        public PerformUpgradeThread(){
            super("MVCLIENT-PerformUpgradeThread");
	    setDaemon(true);
            this.setContextClassLoader(Util.getClassLoader());
	    mConfigJDialog.infiniteProgressJComponent.setText("Upgrading...");
	    mConfigJDialog.infiniteProgressJComponent.setProgressBarVisible(true);
	    mConfigJDialog.infiniteProgressJComponent.start();
            this.start();
        }
        public void run() {
            try{
                // DISABLE ALL GRAPHS SO NO EXCEPTIONS ARE CAUSED
                Map<Policy,Map<ButtonKey,MTransformJPanel>> policyRackMap = Util.getPolicyStateMachine().getPolicyRackMap();
                for( Policy policy : policyRackMap.keySet() )
                    for( MTransformJPanel mTransformJPanel : policyRackMap.get(policy).values() )
                        mTransformJPanel.mTransformDisplayJPanel().setUpdateGraph(false);
                Util.getStatsCache().kill();

                // DO THE DOWNLOAD AND INSTALL
                long key = Util.getToolboxManager().upgrade();
                com.metavize.gui.util.Visitor visitor =
		    new com.metavize.gui.util.Visitor(UpgradeProcessJPanel.this.mConfigJDialog.infiniteProgressJComponent.getProgressBar());
                while (true) {
                    java.util.List<InstallProgress> lip = Util.getToolboxManager().getProgress(key);
                    for (InstallProgress ip : lip) {
                        ip.accept(visitor);
                        if( visitor.isDone() )
                            break;
                    }
                    if( visitor.isDone() )
                        break;
                    if (0 == lip.size()) {
                        Thread.currentThread().sleep(DOWNLOAD_SLEEP_MILLIS);
                    }
                }

                if( visitor.isSuccessful() ){
                    // LET THE USER KNOW WERE FINISHED NORMALLY
                    SwingUtilities.invokeAndWait( new Runnable(){ public void run(){
			UpgradeProcessJPanel.this.mConfigJDialog.infiniteProgressJComponent.setText("Upgrade Successful!");
                    }});
		    Thread.currentThread().sleep(DOWNLOAD_FINAL_SLEEP_MILLIS);
                }
                else{
                    throw new Exception();
                }
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Termination of upgrade:", e);
                // LET THE USER KNOW WERE FINISHED ABNORMALLY
                SwingUtilities.invokeLater( new Runnable(){ public void run(){
		    UpgradeProcessJPanel.this.mConfigJDialog.infiniteProgressJComponent.setText("Error.  Please contact Metavize.");
                }});
            }
            finally{
                new RestartDialog();
            }
        }
    }
		
        // Variables declaration - do not modify//GEN-BEGIN:variables
        private javax.swing.JPanel actionJPanel;
        private javax.swing.JPanel contentJPanel;
        protected javax.swing.JButton upgradeJButton;
        // End of variables declaration//GEN-END:variables
		
}
