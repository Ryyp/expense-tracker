package com.dalrae.expensetracker.handlers;

import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.messages.Messages;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.runtime.CdsRuntime;
import com.sap.cds.services.request.UserInfo;
import cds.gen.catalogservice.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Service Handler for CatalogService
 *
 * Implements custom business logic and validations for Budget and Transactions.
 * This handler provides:
 * - Transaction amount validation
 * - Budget limit enforcement
 * - Custom actions (markAsReviewed, flagForAudit)
 * - Budget summary calculations
 * - Transaction queries by type
 * - Permission checks and user-friendly messages
 *
 * @author Expense Tracker Team
 * @version 1.0
 */
@Component
@ServiceName(CatalogService_.CDS_NAME)
public class CatalogServiceHandler implements EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(CatalogServiceHandler.class);

    @Autowired
    private PersistenceService db;

    @Autowired
    private Messages messages;

    @Autowired
    private CdsRuntime cdsRuntime;

    // ============================================================================
    // PERMISSION CHECKS
    // ============================================================================

    /**
     * Before READ Budget - Check if user has Admin role
     * If user doesn't have Admin role, throw a friendly exception
     */
    @Before(event = "READ", entity = Budget_.CDS_NAME)
    public void beforeReadBudget(CdsReadEventContext context) {
        UserInfo userInfo = context.getUserInfo();
        logger.debug("User attempting to read Budget: {}, roles: {}",
                     userInfo.getName(), userInfo.getRoles());

        // Check if user has Admin role
        if (!userInfo.hasRole("Admin")) {
            logger.warn("User {} does not have Admin role, denying Budget access", userInfo.getName());

            throw new ServiceException(ErrorStatuses.FORBIDDEN,
                    "You do not have permission to access Budget data. " +
                    "Budget management is restricted to administrators only.\n\n" +
                    "You can view budget summary information, but cannot modify the budget settings.\n\n" +
                    "Please contact your administrator if you need access to budget management features.");
        }

        logger.debug("User {} has Admin role, allowing Budget access", userInfo.getName());
    }

    // ============================================================================
    // TRANSACTION VALIDATIONS
    // ============================================================================

    /**
     * Before CREATE Transaction - Validate amount and budget availability
     *
     * Validations:
     * 1. Amount must be positive (> 0)
     * 2. Budget must exist before creating transactions
     * 3. Transaction amount must not exceed remaining budget
     *
     * @param transactions List of transactions being created
     * @throws ServiceException if validation fails
     */
    @Before(event = "CREATE", entity = Transactions_.CDS_NAME)
    public void beforeCreateTransaction(List<Transactions> transactions) {
        logger.debug("Validating {} transaction(s) before CREATE", transactions.size());

        for (Transactions transaction : transactions) {
            validateTransactionAmount(transaction.getAmount());
            validateBudgetAvailability(transaction.getAmount(), null);
        }

        logger.info("Successfully validated {} transaction(s) for creation", transactions.size());
    }

    /**
     * Before UPDATE Transaction - Validate amount and budget for updates
     *
     * When updating, only validates if the new amount is higher than the old amount
     * to prevent unnecessary budget checks when amount is reduced
     *
     * @param transactions List of transactions being updated
     * @throws ServiceException if validation fails
     */
    @Before(event = "UPDATE", entity = Transactions_.CDS_NAME)
    public void beforeUpdateTransaction(List<Transactions> transactions) {
        logger.debug("Validating {} transaction(s) before UPDATE", transactions.size());

        for (Transactions transaction : transactions) {
            if (transaction.getAmount() == null) {
                continue; // Amount not being updated
            }

            // Validate positive amount
            validateTransactionAmount(transaction.getAmount());

            // Get the old transaction to calculate difference
            // Note: Using where clause instead of byId because draft entities have composite keys
            Transactions oldTransaction = db.run(
                    com.sap.cds.ql.Select.from(Transactions_.class)
                            .where(t -> t.ID().eq(transaction.getId())
                                    .and(t.IsActiveEntity().eq(true)))
            ).first(Transactions.class).orElse(null);

            if (oldTransaction != null && oldTransaction.getAmount() != null) {
                BigDecimal difference = transaction.getAmount().subtract(oldTransaction.getAmount());

                // Only validate budget if amount is increasing
                if (difference.compareTo(BigDecimal.ZERO) > 0) {
                    logger.debug("Transaction amount increasing by {}, validating budget", difference);
                    validateBudgetAvailability(difference, oldTransaction.getId());
                }
            }
        }

        logger.info("Successfully validated {} transaction(s) for update", transactions.size());
    }

    /**
     * Validates that transaction amount is positive
     *
     * @param amount Transaction amount to validate
     * @throws ServiceException if amount is zero or negative
     */
    private void validateTransactionAmount(BigDecimal amount) {
        if (amount == null) {
            throw new ServiceException(ErrorStatuses.BAD_REQUEST,
                    "Transaction amount is required");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(ErrorStatuses.BAD_REQUEST,
                    "Transaction amount must be greater than zero. Provided: " + amount);
        }

        logger.debug("Amount validation passed: {}", amount);
    }

    /**
     * Validates that transaction does not exceed remaining budget
     *
     * @param amount Amount to check against remaining budget
     * @param excludeTransactionId Transaction ID to exclude from spent calculation (for updates)
     * @throws ServiceException if transaction exceeds remaining budget
     */
    private void validateBudgetAvailability(BigDecimal amount, String excludeTransactionId) {
        logger.debug("Validating budget availability for amount: {}", amount);

        // Get the latest budget
        Budget budget = db.run(
                com.sap.cds.ql.Select.from(Budget_.class)
                        .orderBy(b -> b.createdAt().desc())
                        .limit(1)
        ).single(Budget.class);

        if (budget == null) {
            throw new ServiceException(ErrorStatuses.BAD_REQUEST,
                    "Please set a budget first before adding transactions");
        }

        BigDecimal totalBudget = budget.getAmount();
        logger.debug("Current budget: {}", totalBudget);

        // Calculate total spent (excluding current transaction if updating)
        List<Transactions> allTransactions = db.run(
                com.sap.cds.ql.Select.from(Transactions_.class)
        ).listOf(Transactions.class);

        BigDecimal spentAmount = allTransactions.stream()
                .filter(t -> excludeTransactionId == null || !t.getId().equals(excludeTransactionId))
                .map(Transactions::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        logger.debug("Current spent amount: {}", spentAmount);

        // Calculate remaining budget
        BigDecimal remainingBudget = totalBudget.subtract(spentAmount);
        logger.debug("Remaining budget: {}", remainingBudget);

        // Check if new expense exceeds remaining budget
        if (amount.compareTo(remainingBudget) > 0) {
            String errorMessage = String.format(
                    "Transaction amount %.2f exceeds remaining budget %.2f. " +
                            "Total budget: %.2f, Already spent: %.2f",
                    amount, remainingBudget, totalBudget, spentAmount
            );
            logger.error(errorMessage);
            throw new ServiceException(ErrorStatuses.BAD_REQUEST, errorMessage);
        }

        logger.debug("Budget validation passed");
    }

    // ============================================================================
    // BUDGET VALIDATIONS
    // ============================================================================

    /**
     * Before CREATE/UPDATE Budget - Validate positive amount
     *
     * @param budgets List of budgets being saved
     * @throws ServiceException if budget amount is not positive
     */
    @Before(event = {"CREATE", "UPDATE"}, entity = Budget_.CDS_NAME)
    public void beforeSaveBudget(List<Budget> budgets) {
        logger.debug("Validating {} budget(s) before save", budgets.size());

        for (Budget budget : budgets) {
            BigDecimal amount = budget.getAmount();

            if (amount == null) {
                throw new ServiceException(ErrorStatuses.BAD_REQUEST,
                        "Budget amount is required");
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException(ErrorStatuses.BAD_REQUEST,
                        "Budget amount must be greater than zero. Provided: " + amount);
            }
        }

        logger.info("Successfully validated {} budget(s)", budgets.size());
    }

    /**
     * Before DELETE Budget - Prevent deletion if transactions exist
     *
     * @throws ServiceException if transactions exist
     */
    @Before(event = "DELETE", entity = Budget_.CDS_NAME)
    public void beforeDeleteBudget() {
        logger.debug("Checking if budget can be deleted");

        List<Transactions> transactions = db.run(
                com.sap.cds.ql.Select.from(Transactions_.class).limit(1)
        ).listOf(Transactions.class);

        if (!transactions.isEmpty()) {
            throw new ServiceException(ErrorStatuses.BAD_REQUEST,
                    "Cannot delete budget while transactions exist. " +
                            "Please delete all transactions first.");
        }

        logger.info("Budget deletion validation passed");
    }

    // ============================================================================
    // CUSTOM ACTIONS
    // ============================================================================

    /**
     * Custom Action: Mark transaction as reviewed
     *
     * This action marks a transaction as having been reviewed.
     * In a real implementation, you might add a 'reviewed' field to the entity.
     *
     * @param context Action context containing the transaction ID
     */
    @On(event = TransactionsMarkAsReviewedContext.CDS_NAME)
    public void onMarkAsReviewed(TransactionsMarkAsReviewedContext context) {
        logger.info("Marking transaction as reviewed");

        // Get the transaction from the target using CQN
        Transactions transaction = db.run(context.getCqn()).single(Transactions.class);

        if (transaction == null) {
            throw new ServiceException(ErrorStatuses.NOT_FOUND,
                    "Transaction not found");
        }

        // In a real implementation, you would update a 'reviewed' field here
        // For now, we just send a success message

        messages.success(String.format(
                "Transaction '%s' (Amount: %.2f) has been marked as reviewed",
                transaction.getDescription(),
                transaction.getAmount()
        ));

        context.setCompleted();
    }

    /**
     * Custom Action: Flag transaction for audit
     *
     * This action flags a transaction for audit review.
     *
     * @param context Action context containing the transaction ID
     */
    @On(event = TransactionsFlagForAuditContext.CDS_NAME)
    public void onFlagForAudit(TransactionsFlagForAuditContext context) {
        logger.info("Flagging transaction for audit");

        // Get the transaction from the target using CQN
        Transactions transaction = db.run(context.getCqn()).single(Transactions.class);

        if (transaction == null) {
            throw new ServiceException(ErrorStatuses.NOT_FOUND,
                    "Transaction not found");
        }

        messages.warn(String.format(
                "Transaction '%s' has been flagged for audit review",
                transaction.getDescription()
        ));

        context.setCompleted();
    }

    // ============================================================================
    // BUDGET SUMMARY ENTITY - Singleton Read Handler
    // ============================================================================

    /**
     * Handler for reading BudgetSummary entity
     * Returns calculated budget overview as a singleton entity
     *
     * @param context Read context for BudgetSummary
     */
    @On(entity = "CatalogService.BudgetSummary")
    public void onReadBudgetSummary(CdsReadEventContext context) {
        logger.debug("Reading Budget Summary");

        // Get the latest budget
        Budget budget = db.run(
                com.sap.cds.ql.Select.from(Budget_.class)
                        .orderBy(b -> b.createdAt().desc())
                        .limit(1)
        ).single(Budget.class);

        BigDecimal totalBudget = budget != null ? budget.getAmount() : BigDecimal.ZERO;
        String currency = budget != null ? budget.getCurrency() : "AUD";

        // Get all transactions
        List<Transactions> transactions = db.run(
                com.sap.cds.ql.Select.from(Transactions_.class)
        ).listOf(Transactions.class);

        // Calculate spent amount
        BigDecimal spentAmount = transactions.stream()
                .map(Transactions::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate remaining amount
        BigDecimal remainingAmount = totalBudget.subtract(spentAmount);

        // Calculate budget utilization percentage
        BigDecimal budgetUtilization = BigDecimal.ZERO;
        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            budgetUtilization = spentAmount
                    .divide(totalBudget, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Create result map
        Map<String, Object> summary = new HashMap<>();
        summary.put("ID", 1);
        summary.put("totalBudget", totalBudget);
        summary.put("currency", currency);
        summary.put("spentAmount", spentAmount);
        summary.put("remainingAmount", remainingAmount);
        summary.put("transactionCount", transactions.size());
        summary.put("budgetUtilization", budgetUtilization);

        logger.info("Budget summary - Total: {}, Currency: {}, Spent: {}, Remaining: {}, Count: {}, Utilization: {}%",
                totalBudget, currency, spentAmount, remainingAmount, transactions.size(), budgetUtilization);

        // Set result as a single row
        context.setResult(Arrays.asList(summary));
    }

    // ============================================================================
    // CUSTOM FUNCTIONS
    // ============================================================================

    /**
     * Custom Function: Get Budget Summary
     *
     * This function returns a summary of the budget, including total, spent,
     * remaining, transaction count, and utilization percentage.
     *
     * @param context Function context
     */
    @On(event = GetBudgetSummaryContext.CDS_NAME)
    public void onGetBudgetSummary(GetBudgetSummaryContext context) {
        logger.debug("Executing getBudgetSummary function");

        // Get the latest budget
        Budget budget = db.run(
                com.sap.cds.ql.Select.from(Budget_.class)
                        .orderBy(b -> b.createdAt().desc())
                        .limit(1)
        ).single(Budget.class);

        BigDecimal totalBudget = (budget != null && budget.getAmount() != null)
                ? budget.getAmount() : BigDecimal.ZERO;

        // Calculate total spent
        List<Transactions> transactions = db.run(
                com.sap.cds.ql.Select.from(Transactions_.class)
        ).listOf(Transactions.class);

        BigDecimal spentAmount = transactions.stream()
                .map(Transactions::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate remaining budget
        BigDecimal remainingAmount = totalBudget.subtract(spentAmount);

        // Calculate budget utilization
        BigDecimal budgetUtilization = BigDecimal.ZERO;
        if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
            budgetUtilization = spentAmount.divide(totalBudget, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Create the result structure
        BudgetSummary summary = BudgetSummary.create();
        summary.setTotalBudget(totalBudget);
        summary.setSpentAmount(spentAmount);
        summary.setRemainingAmount(remainingAmount);
        summary.setTransactionCount(transactions.size());
        summary.setBudgetUtilization(budgetUtilization);

        logger.info("Budget Summary - Total: {}, Spent: {}, Remaining: {}, Count: {}, Utilization: {}%",
                totalBudget, spentAmount, remainingAmount, transactions.size(), budgetUtilization);

        context.setResult(summary);
    }
}
