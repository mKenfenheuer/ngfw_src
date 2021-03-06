Ext.define('Ung.apps.intrusionprevention.view.Signatures', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-intrusion-prevention-signatures',
    itemId: 'signatures',
    title: 'Signatures'.t(),
    scrollable: true,

    controller: 'unintrusionsignaturesgrid',

    name: 'signatures',

    region: 'center',

    sortableColumns: true,
    plugins: [
        'gridfilters'
    ],
    features: [{
        ftype: 'grouping',
        groupHeaderTpl: '{columnName}: {name} ({rows.length} signature{[values.rows.length > 1 ? "s" : ""]})',
        startCollapsed: true
     }],

    bind: '{signatures}',

    listeners: {
        reconfigure: 'signaturesReconfigure'
    },

    // tbar: ['@add', '->', '@import', '@export'],
    tbar: ['@add'],
    recordActions: ['edit', 'copy', 'delete'],
    copyId: 'sid',
    copyIdPreserve: true,
    copyAppendField: 'msg',
    copyModify: [{
        key: 'reserved',
        value: false,
    },{
        key: 'default',
        value: false,
    }],

    bbar: [ 'Search'.t(), {
        xtype: 'textfield',
        name: 'searchFilter',
        listeners: {
            change: 'filterSearch'
        }
    },{
        xtype: 'checkbox',
        name: 'searchLog',
        boxLabel: 'Log'.t(),
        listeners: {
            change: 'filterLog'
        }
    }, {
        xtype: 'checkbox',
        name: 'searchBlock',
        boxLabel: 'Block'.t(),
        listeners: {
            change: 'filterBlock'
        }
    },{
        xtype: 'tbtext',
        name: 'searchStatus',
        html: 'Loading...'.t(),
        listeners: {
            afterrender: 'updateSearchStatusBar'
        }
    }],

    restrictedRecords: {
        keyMatch: 'reserved',
        valueMatch: true,
        // editableFields:[
        //     "log",
        //     "block"
        // ]
    },

    recordModel: 'Ung.model.intrusionprevention.signature',
    emptyRow: "alert tcp any any -> any any ( msg:\"new signature\"; classtype:unknown; sid:1999999; gid:1; classtype:unknown; category:app-detect; content:\"matchme\"; nocase;)",

    columns: [{
        header: "Gid".t(),
        dataIndex: 'gid',
        width: Renderer.idWidth,
        renderer: Ung.apps.intrusionprevention.MainController.idRenderer
    },{
        header: "Sid".t(),
        dataIndex: 'sid',
        width: Renderer.idWidth,
        renderer: Ung.apps.intrusionprevention.MainController.idRenderer
    },{
        header: "Classtype".t(),
        dataIndex: 'classtype',
        width: Renderer.messageWidth,
        renderer: Ung.apps.intrusionprevention.MainController.classtypeRenderer
    },{
        header: "Category".t(),
        dataIndex: 'category',
        width: Renderer.messageWidth,
        renderer: Ung.apps.intrusionprevention.MainController.categoryRenderer
    },{
        header: "Msg".t(),
        dataIndex: 'msg',
        width: Renderer.messageWidth,
        flex:3,
    },{
        header: "Reference".t(),
        dataIndex: 'sid',
        width: Renderer.messageWidth,
        renderer: Ung.apps.intrusionprevention.MainController.referenceRenderer
    },{
        header: "Log".t(),
        dataIndex: 'log',
        width: Renderer.booleanWidth,
        // listeners: {
        //     beforecheckchange: 'logBeforeCheckChange'
        // }
    },{
        // xtype:'checkcolumn',
        header: "Block".t(),
        dataIndex: 'block',
        width: Renderer.booleanWidth,
        // listeners: {
        //     beforecheckchange: 'blockBeforeCheckChange'
        // }
    }],

    editorXtype: 'ung.cmp.unintrusionsignaturesrecordeditor',
    editorFields: [{
        xtype:'numberfield',
        bind: '{record.gid}',
        fieldLabel: 'Gid'.t(),
        emptyText: '[enter gid]'.t(),
        allowBlank: false,
        hideTrigger: true,
        listeners:{
            change: 'editorGidChange'
        }
    },{
        xtype:'numberfield',
        bind: '{record.sid}',
        fieldLabel: 'Sid'.t(),
        emptyText: '[enter sid]'.t(),
        allowBlank: false,
        hideTrigger: true,
        listeners:{
            change: 'editorSidChange'
        }
     },{
        fieldLabel: 'Classtype'.t(),
        editable: false,
        xtype: 'combo',
        queryMode: 'local',
        bind:{
            value: '{record.classtype}',
        },
        store: Ung.apps.intrusionprevention.Main.classtypes,
        valueField: 'name',
        displayField: 'name',
        forceSelection: true,
        listeners: {
            change: 'editorClasstypeChange'
        }
    },{
        fieldLabel: 'Category'.t(),
        bind:{
            value: '{record.category}',
        },
        store: Ung.apps.intrusionprevention.Main.categories,
        emptyText: "[enter category]".t(),
        allowBlank: false,
        xtype: 'combo',
        queryMode: 'local',
        valueField: 'name',
        displayField: 'name'
        // !!! what to do on change?
        // change meta field for...
    },{
        xtype:'textfield',
        bind: '{record.msg}',
        fieldLabel: 'Msg'.t(),
        emptyText: "[enter msg]".t(),
        allowBlank: false,
        listeners: {
            change: 'editorMsgChange'
        }
    },{
         xtype:'checkbox',
         bind: '{record.log}',
         fieldLabel: 'Log'.t(),
         listeners: {
             change: 'editorLogChange'
         }
     },{
         xtype:'checkbox',
         bind: '{record.block}',
         fieldLabel: 'Block'.t(),
         listeners: {
             change: 'editorBlockChange'
         }
     },{
        xtype:'textareafield',
        bind: '{record.signature}',
        fieldLabel: 'Signature'.t(),
        emptyText: "[enter signature]".t(),
        allowBlank: false,
        height: 100,
        listeners:{
            change: 'editorSignatureChange'
        }
    }]
});
