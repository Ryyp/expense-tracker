using CatalogService from './catalog-service';

/**
 * ============================================================================
 * AUTHORIZATION CONFIGURATION
 * ============================================================================
 *
 * Defines role-based access control for the CatalogService
 *
 * Roles:
 * - Admin: Full access to Budget and Transactions
 * - User:  Access to Transactions only (no Budget access)
 */

// Budget Entity - Admin Only Access
//
// Only users with the 'Admin' role can:
// - Create new budgets
// - Read budget information
// - Update existing budgets
// - Delete budgets (if no transactions exist)
annotate CatalogService.Budget with @(restrict: [
    {
        grant: ['*'], // All operations (CREATE, READ, UPDATE, DELETE)
        to   : 'Admin'
    }
]);

// Transactions Entity - Admin and User Access
//
// Both 'Admin' and 'User' roles can:
// - Create new transactions
// - Read transaction information
// - Update existing transactions
// - Delete transactions
// - Execute custom actions (markAsReviewed, flagForAudit)
annotate CatalogService.Transactions with @(restrict: [
    {
        grant: ['*'], // All operations
        to   : ['Admin', 'User']
    }
]);

// Transaction Types - Public Read Access
//
// All authenticated users can read transaction types for value help
annotate CatalogService.TransactionTypes with @(restrict: [
    {
        grant: ['READ'],
        to   : ['Admin', 'User']
    }
]);

// Budget Summary - Read-only access for all authenticated users
//
// All authenticated users can view budget summary information
annotate CatalogService.BudgetSummary with @(restrict: [
    {
        grant: ['READ'],
        to   : ['Admin', 'User', 'authenticated-user']
    }
]);

/**
 * Custom Functions Authorization
 *
 * getBudgetSummary - Available to Admin and User
 * This function is used to display budget information in the UI
 */
annotate CatalogService with @(requires: ['Admin', 'User']);
