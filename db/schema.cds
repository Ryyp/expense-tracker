namespace expense.tracker;

using {
    cuid,
    managed
} from '@sap/cds/common';

/**
 * Budget Entity - Stores the overall budget
 */
entity Budget : cuid, managed {
    amount        : Decimal(15, 2) @mandatory;
    currency      : String(3) default 'AUD';
    description   : String(255);
}

/**
 * Transactions Entity - Stores expense transactions
 */
entity Transactions : cuid, managed {
    description     : String(255) @mandatory;
    type            : String(100) @mandatory;
    amount          : Decimal(15, 2) @mandatory;
    currency        : String(3) default 'AUD';
    transactionDate : Date default $now;
}

/**
 * Transaction Types - Code list for transaction categories
 * This is a static reference data entity
 */
entity TransactionTypes {
    key code        : String(100);
        description : String(255);
}
