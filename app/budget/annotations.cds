using CatalogService as service from '../../srv/catalog-service';

// ============================================================================
// BUDGET APP - List Report and Object Page Annotations
// ============================================================================

annotate service.Budget with @(
    // Common Annotations
    Common.SemanticKey: [description],
    Common.Label      : 'Budget',

    // Capabilities
    Capabilities      : {
        DeleteRestrictions: {Deletable: true},
        InsertRestrictions: {Insertable: true},
        UpdateRestrictions: {Updatable: true}
    },

    // UI Annotations
    UI                : {
        // Selection Fields
        SelectionFields: [description],

        // List Report
        LineItem       : [
            {
                $Type            : 'UI.DataField',
                Value            : amount,
                Label            : 'Budget Amount',
                ![@UI.Importance]: #High
            },
            {
                $Type            : 'UI.DataField',
                Value            : description,
                Label            : 'Description',
                ![@UI.Importance]: #High
            },
            {
                $Type            : 'UI.DataField',
                Value            : createdAt,
                Label            : 'Created At',
                ![@UI.Importance]: #Low
            }
        ],

        // Header Info
        HeaderInfo     : {
            TypeName      : 'Budget',
            TypeNamePlural: 'Budgets',
            Title         : {Value: 'Budget'},
            Description   : {Value: description}
        },

        // Object Page Facets
        Facets         : [
            {
                $Type : 'UI.ReferenceFacet',
                Target: '@UI.FieldGroup#BudgetDetails',
                Label : 'Budget Details'
            },
            {
                $Type : 'UI.ReferenceFacet',
                Target: '@UI.FieldGroup#AdminInfo',
                Label : 'Administrative Information'
            }
        ],

        // Field Groups
        FieldGroup #BudgetDetails: {Data: [
            {
                $Type: 'UI.DataField',
                Value: amount,
                Label: 'Budget Amount'
            },
            {
                $Type: 'UI.DataField',
                Value: description,
                Label: 'Description'
            }
        ]},

        FieldGroup #AdminInfo    : {Data: [
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
    amount      @(
        title               : 'Budget Amount',
        Common.Label        : 'Amount',
        Common.FieldControl : #Mandatory,
        Measures.ISOCurrency: 'EUR'
    );

    description @(
        title           : 'Description',
        Common.Label    : 'Description',
        UI.MultiLineText: true
    );
};
