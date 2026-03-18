using CatalogService as service from '../srv/catalog-service';

// ============================================================================
// TRANSACTION TYPES - Value Help Entity (Shared)
// ============================================================================

annotate service.TransactionTypes with @(
    Common.Label: 'Transaction Types',
    UI          : {
        Identification : [{Value: code}],
        SelectionFields: [code],
        LineItem       : [
            {
                Value: code,
                Label: 'Type Code'
            },
            {
                Value: description,
                Label: 'Description'
            }
        ]
    }
) {
    code        @(
        title       : 'Type Code',
        Common.Label: 'Code'
    );

    description @(
        title       : 'Description',
        Common.Label: 'Description'
    );
};
