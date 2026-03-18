using expense.tracker from '../db/schema';

/**
 * Catalog Service - Main service for Budget and Transactions
 *
 * This service exposes Budget and Transactions entities with:
 * - Draft support for all CRUD operations
 * - Custom actions and functions
 * - Authorization restrictions (Admin/User roles)
 */
@path: '/catalog'
service CatalogService {

    /**
     * Budget entity - Admin only access
     * Stores the overall budget configuration
     */
    @odata.draft.enabled
    @requires: 'Admin'
    entity Budget as projection on tracker.Budget;

    /**
     * Transactions entity - Full CRUD with draft support
     * Tracks individual expense transactions
     *
     * Authorization:
     * - Admin: Full access (READ, CREATE, UPDATE, DELETE)
     * - User: Limited access (READ, CREATE, UPDATE)
     */
    @odata.draft.enabled
    @requires: 'authenticated-user'
    @restrict: [
        {
            grant: '*',
            to   : 'Admin'
        },
        {
            grant: [
                'READ',
                'CREATE',
                'UPDATE'
            ],
            to   : 'User'
        }
    ]
    entity Transactions as projection on tracker.Transactions actions {
        /**
         * Custom bound action to mark a transaction as reviewed
         * Includes confirmation popup via side effects
         */
        @(
            Common.SideEffects              : {TargetEntities: [_it]},
            Core.OperationAvailable         : true,
            cds.odata.bindingparameter.name : '_it'
        )
        action markAsReviewed();

        /**
         * Custom bound action to flag a transaction for audit
         */
        @(
            Common.SideEffects              : {TargetEntities: [_it]},
            Core.OperationAvailable         : true,
            cds.odata.bindingparameter.name : '_it'
        )
        action flagForAudit();
    };

    /**
     * Budget Summary - Singleton entity for KPI display
     * Provides calculated budget overview data for UI
     * Available to all authenticated users
     */
    @readonly
    @requires: 'authenticated-user'
    entity BudgetSummary {
        key ID                : Integer default 1;
            totalBudget       : Decimal(15, 2);
            currency          : String(3);
            spentAmount       : Decimal(15, 2);
            remainingAmount   : Decimal(15, 2);
            transactionCount  : Integer;
            budgetUtilization : Decimal(5, 2); // Percentage
    };

    /**
     * View for Transaction Types (for value help)
     * Available to all authenticated users
     */
    @readonly
    @requires: 'authenticated-user'
    entity TransactionTypes as projection on tracker.TransactionTypes;

    function getBudgetSummary() returns BudgetSummary;
}

// Auto-expose annotations for value helps
annotate CatalogService with @(cds.autoexpose);
