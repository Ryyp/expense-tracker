using CatalogService as service from '../../srv/catalog-service';

// ============================================================================
// TRANSACTIONS APP - List Report and Object Page Annotations
// ============================================================================

annotate service.Transactions with @(
    // Common Annotations
    Common.SemanticKey : [description],
    Common.Label       : 'Transaction',

    // Capabilities
    Capabilities       : {
        DeleteRestrictions: {Deletable: true},
        InsertRestrictions: {Insertable: true},
        UpdateRestrictions: {Updatable: true},
        SearchRestrictions: {Searchable: true}
    },

    // UI Annotations
    UI                 : {
        // Selection Fields (Filter Bar)
        SelectionFields               : [
            type,
            transactionDate,
            description,
            amount
        ],

        // List Report Header
        HeaderInfo                    : {
            TypeName       : 'Transaction',
            TypeNamePlural : 'Transactions',
            Title          : {Value: description},
            Description    : {Value: type},
            TypeImageUrl   : 'sap-icon://expense-report'
        },

        // Header Facets
        HeaderFacets                  : [],

        // List Report Table Columns
        LineItem                      : [
            {
                $Type            : 'UI.DataField',
                Value            : description,
                Label            : 'Description',
                ![@UI.Importance]: #High
            },
            {
                $Type            : 'UI.DataField',
                Value            : type,
                Label            : 'Type',
                ![@UI.Importance]: #High,
                Criticality      : #Information
            },
            {
                $Type            : 'UI.DataField',
                Value            : amount,
                Label            : 'Amount',
                ![@UI.Importance]: #High
            },
            {
                $Type            : 'UI.DataField',
                Value            : transactionDate,
                Label            : 'Date',
                ![@UI.Importance]: #Medium
            },
            {
                $Type : 'UI.DataFieldForAction',
                Label : 'Mark as Reviewed',
                Action: 'CatalogService.markAsReviewed',
                Inline: true
            },
            {
                $Type : 'UI.DataFieldForAction',
                Label : 'Flag for Audit',
                Action: 'CatalogService.flagForAudit',
                Inline: false
            }
        ],

        // Presentation Variant (Sorting and Visualization)
        PresentationVariant           : {
            Text          : 'Default',
            SortOrder     : [{
                Property   : transactionDate,
                Descending : true
            }],
            Visualizations: ['@UI.LineItem']
        },

        // Object Page Sections (Facets)
        Facets                        : [
            {
                $Type : 'UI.ReferenceFacet',
                Target: '@UI.FieldGroup#GeneralInfo',
                Label : 'General Information',
                ID    : 'GeneralInfo'
            },
            {
                $Type : 'UI.ReferenceFacet',
                Target: '@UI.FieldGroup#TransactionDetails',
                Label : 'Transaction Details',
                ID    : 'TransactionDetails'
            },
            {
                $Type : 'UI.ReferenceFacet',
                Target: '@UI.FieldGroup#AdminInfo',
                Label : 'Administrative Information',
                ID    : 'AdminInfo'
            }
        ],

        // Data Point for Amount (Object Page Header)
        DataPoint #Amount             : {
            Value        : amount,
            Title        : 'Transaction Amount',
            MinimumValue : 0,
            Visualization: #Number,
            ValueFormat  : {NumberOfFractionalDigits: 2}
        },

        // Field Group for General Information
        FieldGroup #GeneralInfo       : {Data: [
            {
                $Type: 'UI.DataField',
                Value: description,
                Label: 'Description'
            },
            {
                $Type: 'UI.DataField',
                Value: type,
                Label: 'Transaction Type'
            }
        ]},

        // Field Group for Transaction Details
        FieldGroup #TransactionDetails: {Data: [
            {
                $Type: 'UI.DataField',
                Value: amount,
                Label: 'Amount'
            },
            {
                $Type: 'UI.DataField',
                Value: transactionDate,
                Label: 'Transaction Date'
            }
        ]},

        // Field Group for Admin Info
        FieldGroup #AdminInfo         : {Data: [
            {
                $Type: 'UI.DataField',
                Value: createdAt,
                Label: 'Created At'
            },
            {
                $Type: 'UI.DataField',
                Value: createdBy,
                Label: 'Created By'
            },
            {
                $Type: 'UI.DataField',
                Value: modifiedAt,
                Label: 'Modified At'
            },
            {
                $Type: 'UI.DataField',
                Value: modifiedBy,
                Label: 'Modified By'
            }
        ]}
    }
) {
    // Field Level Annotations
    ID              @(
        title        : 'ID',
        UI.Hidden    : true,
        Core.Computed: true
    );

    description     @(
        title                  : 'Description',
        Common.Label           : 'Description',
        Common.FieldControl    : #Mandatory,
        UI.MultiLineText       : true
    );

    type            @(
        title                           : 'Type',
        Common.Label                    : 'Transaction Type',
        Common.FieldControl             : #Mandatory,
        Common.ValueList                : {
            CollectionPath: 'TransactionTypes',
            Parameters    : [
                {
                    $Type            : 'Common.ValueListParameterInOut',
                    LocalDataProperty: type,
                    ValueListProperty: 'code'
                },
                {
                    $Type            : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty: 'description'
                }
            ]
        },
        Common.ValueListWithFixedValues: true
    );

    amount          @(
        title               : 'Amount',
        Common.Label        : 'Amount',
        Common.FieldControl : #Mandatory,
        Measures.ISOCurrency: currency
    );

    currency        @(
        title       : 'Currency',
        Common.Label: 'Currency',
        UI.Hidden   : false
    );

    transactionDate @(
        title              : 'Transaction Date',
        Common.Label       : 'Date',
        Common.FieldControl: #Mandatory
    );

    createdAt       @(
        title          : 'Created At',
        Core.Computed  : true,
        UI.HiddenFilter: true
    );

    createdBy       @(
        title          : 'Created By',
        Core.Computed  : true,
        UI.HiddenFilter: true
    );

    modifiedAt      @(
        title          : 'Modified At',
        Core.Computed  : true,
        UI.HiddenFilter: true
    );

    modifiedBy      @(
        title          : 'Modified By',
        Core.Computed  : true,
        UI.HiddenFilter: true
    );
};

// ============================================================================
// ACTIONS - Confirmation Popups and Side Effects
// ============================================================================

// Mark as Reviewed Action
annotate service.Transactions actions {
    markAsReviewed @(
        Common.Label                    : 'Mark as Reviewed',
        Common.IsActionCritical         : false,
        Core.OperationAvailable         : true,
        Common.SideEffects              : {TargetEntities: [_it]},
        cds.odata.bindingparameter.name : '_it'
    );

    flagForAudit   @(
        Common.Label                    : 'Flag for Audit',
        Common.IsActionCritical         : true,
        Core.OperationAvailable         : true,
        Common.SideEffects              : {TargetEntities: [_it]},
        cds.odata.bindingparameter.name : '_it'
    );
}

