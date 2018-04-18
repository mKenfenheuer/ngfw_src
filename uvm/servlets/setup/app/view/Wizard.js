Ext.define('Ung.Setup.Wizard', {
    extend: 'Ext.window.Window',
    alias: 'widget.setupwizard',
    modal: true,

    resizable: false,
    draggable: false,

    width: 800,
    height: 600,
    frame: false,
    frameHeader: false,
    header: false,
    border: false,
    bodyBorder: false,
    bodyPadding: 0,
    layout: 'card',
    padding: 0,

    bodyStyle: {
        background: '#FFF',
        border: 0
    },

    defaults: {
        border: false,
        bodyBorder: false,
        bodyPadding: 0,
        cls: 'step',
        header: false
    },

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        defaults: {
            xtype: 'component'
        },
        items: [{
            html: 'Server Settings'.t()
        }, {
            html: 'Network Cards'.t()
        }]
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        background: '#FFF',
        defaults: {
            scale: 'medium',
            hidden: true,
            focusable: false
        },
        items: [{
            itemId: 'prevBtn',
            iconCls: 'fa fa-chevron-circle-left fa-lg',
            handler: 'onPrev'
        }, '->', {
            itemId: 'nextBtn',
            iconCls: 'fa fa-chevron-circle-right fa-lg',
            iconAlign: 'right',
            handler: 'onNext'
        }]
    }],

    items: [
        { xtype: 'serversettings' },
        { xtype: 'networkcards' },
        { xtype: 'internetconnection' },
        { xtype: 'internalnetwork' },
        { xtype: 'wireless' },
        { xtype: 'autoupgrades' },
        { xtype: 'complete' }
    ],

    listeners: {
        afterrender: 'onAfterRender'
    },

    controller: {
        onAfterRender: function () {
            this.updateNav();
        },

        onPrev: function () {
            var me = this;
            me.getView().getLayout().prev();
            me.updateNav();
        },

        onNext: function () {
            var me = this, layout = me.getView().getLayout();


            layout.getActiveItem().fireEvent('save', function () {
                layout.next();
                me.updateNav();
            });

            // layout.getActiveItem().getController().save(function () {
            //     // update completed step

            //     var stepName = layout.getActiveItem().getXType();

            //     if (!rpc.wizardSettings.wizardComplete && stepName !== 'Welcome') {
            //         rpc.wizardSettings.completedStep = layout.getActiveItem().getXType();
            //         rpc.jsonrpc.UvmContext.setWizardSettings(function (result, ex) {
            //             if (ex) { Util.handleException(ex); return; }
            //         }, rpc.wizardSettings);
            //     }

            //     // move to next step
            //     layout.next();
            // });
        },

        updateNav: function () {
            var me = this, view = me.getView(),
                prevBtn = view.down('#prevBtn'),
                nextBtn = view.down('#nextBtn'),
                layout = me.getView().getLayout(),
                prevStep = layout.getPrev(),
                nextStep = layout.getNext();

            if (prevStep) {
                prevBtn.show().setText(prevStep.getTitle());
            } else {
                prevBtn.hide().setText('');
            }

            if (nextStep) {
                nextBtn.show().setText(nextStep.getTitle());
            } else {
                nextBtn.hide().setText('');
            }
        }
    }

});