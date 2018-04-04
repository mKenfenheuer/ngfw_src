Ext.define('Ung.store.Widgets', {
    extend: 'Ext.data.Store',
    alias: 'store.widgets',
    storeId: 'widgets',
    model: 'Ung.model.Widget',
    listeners: {
        load: function () {
            console.log('widgets load');
        },
        datachanged: function () {
            console.log('widgets datachanged');
        },
        update: function (store, record, operation, modifiedFieldNames, details) {
            if (operation === 'edit') {
                Ext.fireEvent('updatewidget', store, record, operation, modifiedFieldNames, details);
            }
            // console.log('widgets update', operation, modifiedFieldNames, details);
        },
        remove: function (store, records, index) {
            console.log('widgets remove', index);
        },
        add: function (store, records, index) {
            Ext.fireEvent('addwidget', records);
            console.log('widgets add', index);
        },
        refresh: function (store, eOpts) {
            console.log('widgets refresh', eOpts);
        }

    }
});
