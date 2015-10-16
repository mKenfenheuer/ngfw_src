Ext.define('Webui.untangle-node-spamassassin.settings', {
    extend:'Ung.NodeWin',
    emailPanel: null,
    gridEventLog: null,
    gridDnsblEventLog: null,

    initComponent: function() {
        try {
            this.lastUpdate = this.getRpcNode().getLastUpdate();
            this.lastCheck = this.getRpcNode().getLastUpdateCheck();
            this.signatureVer = this.getRpcNode().getSignatureVersion();
            this.vendor = this.getRpcNode().getVendor();
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }

        this.buildEmail();
        // builds the tab panel with the tabs
        this.buildTabPanel([this.emailPanel]);
        this.callParent(arguments);
    },
    afterRender: function() {
        this.callParent(arguments);
        this.enableSuperSpam();
        var smtpStrengthValue = this.emailPanel.down('numberfield[name=smtpStrengthValue]');
        smtpStrengthValue.setVisible(this.isCustomStrength(this.settings.smtpConfig.strength));
        if(!this.settings.smtpConfig.scan) {
            smtpStrengthValue.disable();
        }
        var resetBayes = this.emailPanel.down('button[name=resetBayesButton]');
        resetBayes.setVisible(rpc.isExpertMode);
    },
    isCustomStrength: function(strength) {
        return !(strength == 50 || strength == 43 || strength == 35 || strength == 33 || strength == 30);
    },
    getStrengthSelectionValue: function(strength) {
        if (this.isCustomStrength(strength)) {
            return 0;
        } else {
            return strength;
        }
    },
    onBeforeCollapse: function( panel, animate ) {
        this.isAdvanced = false;
        return true;
    },
    onBeforeExpand: function( panel, animate ) {
        this.isAdvanced = true;
        return true;
    },
    //enable super spam if quarantine is selected from the drop down
    enableSuperSpam: function(elem) {
        var smtpAction = this.emailPanel.down('combo[name=smtpAction]');
        var dropSuperSpam = this.emailPanel.down('checkbox[name=dropSuperSpam]');
        var smtpStrengthValue = this.emailPanel.down('numberfield[name=smtpSuperStrengthValue]');
        var newValue = smtpAction.getValue();
        if(smtpAction.disabled==true) {
            dropSuperSpam.disable();
            smtpStrengthValue.disable();
        } else if(newValue == 'QUARANTINE' || newValue == 'MARK' || newValue == 'PASS') {
            dropSuperSpam.enable();
            if(dropSuperSpam.getValue()) {
                smtpStrengthValue.enable();
            } else {
                smtpStrengthValue.enable();
            }
        } else {
            dropSuperSpam.setValue(0);
            dropSuperSpam.disable();
            smtpStrengthValue.disable();
        }
        //Ext does not gray out the label of a textfield!
        if(smtpStrengthValue.disabled) {
            smtpStrengthValue.fireEvent('disable');
        } else {
            smtpStrengthValue.fireEvent('enable');
        }
    },
    // Email Config Panel
    buildEmail: function() {
        var smtpData = [['MARK', this.i18n._('Mark')], ['PASS', this.i18n._('Pass')],
                         ['DROP', this.i18n._('Drop')], ['QUARANTINE', this.i18n._('Quarantine')]];
        var strengthsData = [[50, this.i18n._('Low (Threshold: 5.0)')], [43, this.i18n._('Medium (Threshold: 4.3)')], [35, this.i18n._('High (Threshold: 3.5)')],
                              [33, this.i18n._('Very High (Threshold: 3.3)')], [30, this.i18n._('Extreme (Threshold: 3.0)')], [0, this.i18n._('Custom')]];

        this.emailPanel = Ext.create('Ext.panel.Panel',{
            title: this.i18n._('Email'),
            name: 'Email',
            helpSource: 'spam_blocker_lite_email',
            autoScroll: true,
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                title: this.i18n._('SMTP'),
                items: [{
                    xtype: 'checkbox',
                    name: 'Scan SMTP',
                    boxLabel: this.i18n._('Scan SMTP'),
                    hideLabel: true,
                    checked: this.settings.smtpConfig.scan,
                    listeners: {
                        "afterrender":{
                            fn: Ext.bind(function(elem) {
                                this.emailPanel.down('combo[name=smtpStrength]').setDisabled(!elem.getValue());
                                this.emailPanel.down('combo[name=smtpAction]').setDisabled(!elem.getValue());
                                this.enableSuperSpam();
                            }, this)
                        },
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.smtpConfig.scan = newValue;
                                this.emailPanel.down('combo[name=smtpStrength]').setDisabled(!elem.getValue());
                                this.emailPanel.down('combo[name=smtpAction]').setDisabled(!elem.getValue());
                                var smtpStrengthValue = this.emailPanel.down('numberfield[name=smtpStrengthValue]');
                                if(smtpStrengthValue.isVisible()) {
                                    smtpStrengthValue.setDisabled(!elem.getValue());
                                }
                                this.enableSuperSpam();
                            }, this)
                        }
                    }
                }, {
                    border: false,
                    layout: 'column',
                    items: [{
                        border: false,
                        items: [{
                            xtype: 'combo',
                            name: 'smtpStrength',
                            editable: false,
                            store: strengthsData,
                            fieldLabel: this.i18n._('Strength'),
                            width: 300,
                            queryMode: 'local',
                            value: this.getStrengthSelectionValue(this.settings.smtpConfig.strength),
                            listeners: {
                                select: Ext.bind(function(elem, record) {
                                    var smtpStrengthValue = this.emailPanel.down('numberfield[name=smtpStrengthValue]');
                                    if (record.get("field1") == 0) {
                                        smtpStrengthValue.setVisible(true);
                                    } else {
                                        smtpStrengthValue.setVisible(false);
                                        this.settings.smtpConfig.strength = record.get("field1");
                                    }
                                    smtpStrengthValue.setValue(this.settings.smtpConfig.strength / 10.0);
                                }, this)
                            }
                        }]
                    },{
                        border: false,
                        columnWidth:1,
                        items: [{
                            xtype: 'numberfield',
                            fieldLabel: '&nbsp;&nbsp;&nbsp;' + this.i18n._('Strength Value'),
                            name: 'smtpStrengthValue',
                            value: this.settings.smtpConfig.strength / 10.0,
                            toValidate: true,
                            width: 200,
                            allowDecimals: true,
                            allowBlank: false,
                            blankText: this.i18n._('Strength Value must be a number. Smaller value is higher strength.'),
                            minValue: -2147483648,
                            maxValue: 2147483647,
                            hideTrigger:true,
                            listeners: {
                                "change": {
                                    fn: Ext.bind(function(elem, newValue) {
                                        this.settings.smtpConfig.strength = Math.round(newValue * 10);
                                    }, this)
                                }
                            }
                        }]
                    }]
                }, {
                    xtype: 'combo',
                    name: 'smtpAction',
                    editable: false,
                    store: smtpData,
                    valueField: 'key',
                    displayField: 'name',
                    fieldLabel: this.i18n._('Action'),
                    width: 300,
                    queryMode: 'local',
                    value: this.settings.smtpConfig.msgAction,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.smtpConfig.msgAction = newValue;
                            }, this)
                        },
                        "afterrender": {
                            fn: Ext.bind(function(elem) {
                                this.enableSuperSpam();
                            }, this)
                        },
                        "select":{
                            fn: Ext.bind(function(elem) {
                                this.enableSuperSpam();
                            }, this)
                        }
                    }
                }, {
                    xtype: 'checkbox',
                    name: 'dropSuperSpam',
                    boxLabel: this.i18n._('Drop Super Spam'),
                    hideLabel: true,
                    checked: this.settings.smtpConfig.blockSuperSpam,
                    listeners: {
                        "afterrender": {
                            fn: Ext.bind(function(elem) {
                                this.emailPanel.down('numberfield[name=smtpSuperStrengthValue]').setDisabled(!elem.getValue());
                            }, this)
                        },
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.smtpConfig.blockSuperSpam = newValue;
                                this.emailPanel.down('numberfield[name=smtpSuperStrengthValue]').setDisabled(!newValue);
                            }, this)
                        }
                    }
                },
                         {
                             xtype: 'numberfield',
                             labelWidth: 150,
                             fieldLabel: this.i18n._('Super Spam Threshold'),
                             name: 'smtpSuperStrengthValue',
                             value: this.settings.smtpConfig.superSpamStrength / 10.0,
                             toValidate: true,
                             allowDecimals: false,
                             allowBlank: false,
                             minValue: 0,
                             maxValue: 2147483647,
                             hideTrigger:true,
                             listeners: {
                                 "change": {
                                     fn: Ext.bind(function(elem, newValue) {
                                         this.settings.smtpConfig.superSpamStrength = Math.round(newValue * 10);
                                     }, this)
                                 },
                                 "disable":{
                                     fn: function(elem) {
                                         if(this.rendered) {
                                             this.getEl().addCls('x-item-disabled');
                                         }
                                     }
                                 },
                                 "afterrender":{
                                     fn: Ext.bind(function(elem) {
                                         this.enableSuperSpam();
                                     }, this)
                                 },
                                 "enable": {
                                     fn: function(elem) {
                                         if(this.rendered) {
                                             this.getEl().removeCls('x-item-disabled');
                                         }
                                     }
                                 }
                             }
                         },{
                             name:'advanced',
                             xtype:'fieldset',
                             title:this.i18n._("Advanced SMTP Configuration"),
                             collapsible: true,
                             collapsed: !this.isAdvanced,
                             listeners: {
                                 beforecollapse: Ext.bind(this.onBeforeCollapse, this ),
                                 beforeexpand: Ext.bind(this.onBeforeExpand, this )
                             },
                             items: [{
                                 xtype: 'checkbox',
                                 name: 'Enable tarpitting',
                                 boxLabel: this.i18n._('Enable tarpitting'),
                                 hideLabel: true,
                                 checked: this.settings.smtpConfig.tarpit,
                                 listeners: {
                                     "change": {
                                         fn: Ext.bind(function(elem, checked) {
                                             this.settings.smtpConfig.tarpit = checked;
                                         }, this)
                                     }
                                 }
                             },{
                                 xtype: 'checkbox',
                                 name: 'SMTP Add Email Headers',
                                 boxLabel: this.i18n._('Add email headers'),
                                 hideLabel: true,
                                 checked: this.settings.smtpConfig.addSpamHeaders,
                                 listeners: {
                                     "change": {
                                         fn: Ext.bind(function(elem, newValue) {
                                             this.settings.smtpConfig.addSpamHeaders = newValue;
                                         }, this)
                                     }
                                 }
                             },{
                                 xtype: 'checkbox',
                                 name: 'SMTP Fail Closed',
                                 boxLabel: this.i18n._('Close connection on scan failure'),
                                 hideLabel: true,
                                 checked: this.settings.smtpConfig.failClosed,
                                 listeners: {
                                     "change": {
                                         fn: Ext.bind(function(elem, newValue) {
                                             this.settings.smtpConfig.failClosed = newValue;
                                         }, this)
                                     }
                                 }
                             },{
                                 xtype: 'checkbox',
                                 name: 'Scan outbound (WAN) SMTP',
                                 boxLabel: this.i18n._('Scan outbound (WAN) SMTP'),
                                 hideLabel: true,
                                 checked: this.settings.smtpConfig.scanWanMail,
                                 listeners: {
                                     "change": {
                                         fn: Ext.bind(function(elem, newValue) {
                                             this.settings.smtpConfig.scanWanMail = newValue;
                                         }, this)
                                     }
                                 }
                             },{
                                 xtype: 'checkbox',
                                 name: 'Allow and ignore TLS sessions',
                                 boxLabel: this.i18n._('Allow and ignore TLS sessions'),
                                 hideLabel: true,
                                 checked: this.settings.smtpConfig.allowTls,
                                 listeners: {
                                     "change": {
                                         fn: Ext.bind(function(elem, newValue) {
                                             this.settings.smtpConfig.allowTls = newValue;
                                         }, this)
                                     }
                                 }
                             },{
                                 xtype: 'numberfield',
                                 fieldLabel: this.i18n._('CPU Load Limit'),
                                 labelWidth: 150,
                                 name: 'SMTP CPU Load Limit',
                                 value: this.settings.smtpConfig.loadLimit,
                                 toValidate: true,
                                 allowDecimals: true,
                                 allowBlank: false,
                                 blankText: this.i18n._('Value must be a float.'),
                                 minValue: 0,
                                 maxValue: 50,
                                 hideTrigger:true,
                                 listeners: {
                                     "change": {
                                         fn: Ext.bind(function(elem, newValue) {
                                             this.settings.smtpConfig.loadLimit = newValue;
                                         }, this)
                                     }
                                 }
                             },{
                                 xtype: 'numberfield',
                                 fieldLabel: this.i18n._('Concurrent Scan Limit'),
                                 labelWidth: 150,
                                 name: 'SMTP Concurrent Scan Limit',
                                 value: this.settings.smtpConfig.scanLimit,
                                 toValidate: true,
                                 allowDecimals: false,
                                 allowBlank: false,
                                 blankText: this.i18n._('Value must be a integer.'),
                                 minValue: 0,
                                 maxValue: 100,
                                 hideTrigger:true,
                                 listeners: {
                                     "change": {
                                         fn: Ext.bind(function(elem, newValue) {
                                             this.settings.smtpConfig.scanLimit = newValue;
                                         }, this)
                                     }
                                 }
                             },{
                                 xtype: 'numberfield',
                                 fieldLabel: this.i18n._('Message Size Limit'),
                                 labelWidth: 150,
                                 name: 'SMTP Message Size Limit',
                                 value: this.settings.smtpConfig.msgSizeLimit,
                                 toValidate: true,
                                 allowDecimals: false,
                                 allowBlank: false,
                                 blankText: this.i18n._('Value must be a integer.'),
                                 minValue: 0,
                                 maxValue: 2147483647,
                                 hideTrigger:true,
                                 listeners: {
                                     "change": {
                                         fn: Ext.bind(function(elem, newValue) {
                                             this.settings.smtpConfig.msgSizeLimit = newValue;
                                         }, this)
                                     }
                                 }
                             },{
                                 xtype: "button",
                                 name: "resetBayesButton",
                                 margin: '5 0 0 0',
                                 text: this.i18n._("Clear Bayes Data"),
                                 iconCls: "reboot-icon",
                                 handler: Ext.bind(function() {
                                     Ext.MessageBox.confirm(this.i18n._("Warning"),
                                                            Ext.String.format(this.i18n._("This will clear all learned bayesian data!"), rpc.companyName ),
                                                            Ext.bind(function(btn) {
                                                                var script = [
                                                                    '/bin/rm -f /home/spamd/.spamassassin/bayes*;',
                                                                    '/etc/init.d/spamassassin restart;'
                                                                ];
                                                                var command =  "/bin/bash -c " + script.join("");

                                                                if (btn == "yes") {
                                                                    Ext.MessageBox.wait(this.i18n._("Clearing bayes data..."), i18n._("Please wait"));
                                                                    Ung.Main.getExecManager().execOutput(Ext.bind(function (result, exception) {
                                                                        if(exception) {
                                                                            Ext.MessageBox.alert(this.i18n._("Failure"),this.i18n._("Error: Unable to reset bayes data."));
                                                                        } else {
                                                                            Ext.MessageBox.alert(this.i18n._("Success"), i18n._("Bayes data cleared."));
                                                                        }
                                                                    }, this), command);
                                                                }
                                                            }, this));
                                 }, this)
                             }]
                         }]
            }, {
                xtype: 'fieldset',
                title: this.i18n._('Note'),
                html: this.i18n._('Spam Blocker Lite last checked for updates') + ":&nbsp;&nbsp;&nbsp;&nbsp;" +
                    (this.lastCheck != null&& this.lastCheck.time != 0 ? i18n.timestampFormat(this.lastCheck): i18n._("never")) + '<br\>' +
                    this.i18n._('Spam Blocker Lite was last updated') + ":&nbsp;&nbsp;&nbsp;&nbsp;" +
                    (this.lastUpdate != null && this.lastUpdate.time != 0 ? i18n.timestampFormat(this.lastUpdate): i18n._("never"))
            }]
        });
    },
    validate: function() {
        var components = this.query("component[toValidate]");
        return this.validateComponents(components);
    }
});
//# sourceURL=spamassassin-settings.js