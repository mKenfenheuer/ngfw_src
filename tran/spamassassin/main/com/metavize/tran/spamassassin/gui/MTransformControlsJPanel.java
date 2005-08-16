/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.tran.spamassassin.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;


import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

public class MTransformControlsJPanel extends com.metavize.tran.spam.gui.MTransformControlsJPanel{
        
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }
    
}
