package com.dalrae.expensetracker.handlers;

import com.sap.cds.Result;
import com.sap.cds.ql.Select;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.messages.Messages;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.request.UserInfo;
import cds.gen.catalogservice.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CatalogServiceHandler
 *
 * Tests cover:
 * - Permission checks for Budget access
 * - Transaction amount validation
 * - Budget availability validation
 * - Budget summary calculations
 * - Custom actions (markAsReviewed, flagForAudit)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogServiceHandler Tests")
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogServiceHandlerTest {

    @Mock
    private PersistenceService db;

    @Mock
    private Messages messages;

    @Mock
    private CdsReadEventContext readContext;

    @Mock
    private UserInfo userInfo;

    @Mock
    private Result result;

    @InjectMocks
    private CatalogServiceHandler handler;

    private Budget testBudget;
    private Transactions testTransaction;

    @BeforeEach
    void setUp() {
        // Initialize test budget
        testBudget = Budget.create();
        testBudget.setId("budget-1");
        testBudget.setAmount(new BigDecimal("10000.00"));
        testBudget.setCurrency("AUD");

        // Initialize test transaction
        testTransaction = Transactions.create();
        testTransaction.setId("trans-1");
        testTransaction.setDescription("Test Transaction");
        testTransaction.setType("Food");
        testTransaction.setAmount(new BigDecimal("100.00"));
    }

    // ============================================================================
    // PERMISSION TESTS
    // ============================================================================

    @Test
    @DisplayName("Admin user should be able to read Budget")
    void testBeforeReadBudget_AdminUser_Success() {
        // Given
        when(readContext.getUserInfo()).thenReturn(userInfo);
        when(userInfo.hasRole("Admin")).thenReturn(true);
        when(userInfo.getName()).thenReturn("admin");

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> handler.beforeReadBudget(readContext));

        verify(userInfo).hasRole("Admin");
    }

    @Test
    @DisplayName("Non-admin user should not be able to read Budget")
    void testBeforeReadBudget_NonAdminUser_ThrowsException() {
        // Given
        when(readContext.getUserInfo()).thenReturn(userInfo);
        when(userInfo.hasRole("Admin")).thenReturn(false);
        when(userInfo.getName()).thenReturn("regular_user");

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> handler.beforeReadBudget(readContext));

        assertEquals(ErrorStatuses.FORBIDDEN, exception.getErrorStatus());
        assertTrue(exception.getMessage().contains("You do not have permission to access Budget data"));

        verify(userInfo).hasRole("Admin");
    }

    // ============================================================================
    // TRANSACTION AMOUNT VALIDATION TESTS
    // ============================================================================

    @Test
    @DisplayName("Should create transaction with valid positive amount")
    void testBeforeCreateTransaction_ValidAmount_Success() {
        // Given
        testTransaction.setAmount(new BigDecimal("500.00"));
        List<Transactions> transactions = Collections.singletonList(testTransaction);

        when(db.run(any(Select.class))).thenReturn(result);
        when(result.single(Budget.class)).thenReturn(testBudget);
        when(result.listOf(Transactions.class)).thenReturn(Collections.emptyList());

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> handler.beforeCreateTransaction(transactions));
    }

    @Test
    @DisplayName("Should reject transaction with zero amount")
    void testBeforeCreateTransaction_ZeroAmount_ThrowsException() {
        // Given
        testTransaction.setAmount(BigDecimal.ZERO);
        List<Transactions> transactions = Collections.singletonList(testTransaction);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> handler.beforeCreateTransaction(transactions));

        assertEquals(ErrorStatuses.BAD_REQUEST, exception.getErrorStatus());
        assertTrue(exception.getMessage().contains("must be greater than zero"));
    }

    @Test
    @DisplayName("Should reject transaction with negative amount")
    void testBeforeCreateTransaction_NegativeAmount_ThrowsException() {
        // Given
        testTransaction.setAmount(new BigDecimal("-100.00"));
        List<Transactions> transactions = Collections.singletonList(testTransaction);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> handler.beforeCreateTransaction(transactions));

        assertEquals(ErrorStatuses.BAD_REQUEST, exception.getErrorStatus());
        assertTrue(exception.getMessage().contains("must be greater than zero"));
    }

    @Test
    @DisplayName("Should reject transaction with null amount")
    void testBeforeCreateTransaction_NullAmount_ThrowsException() {
        // Given
        testTransaction.setAmount(null);
        List<Transactions> transactions = Collections.singletonList(testTransaction);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> handler.beforeCreateTransaction(transactions));

        assertEquals(ErrorStatuses.BAD_REQUEST, exception.getErrorStatus());
        assertTrue(exception.getMessage().contains("amount is required"));
    }

    // ============================================================================
    // BUDGET AVAILABILITY VALIDATION TESTS
    // ============================================================================

    @Test
    @DisplayName("Should reject transaction when no budget exists")
    void testBeforeCreateTransaction_NoBudget_ThrowsException() {
        // Given
        testTransaction.setAmount(new BigDecimal("100.00"));
        List<Transactions> transactions = Collections.singletonList(testTransaction);

        when(db.run(any(Select.class))).thenReturn(result);
        when(result.single(Budget.class)).thenReturn(null);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> handler.beforeCreateTransaction(transactions));

        assertEquals(ErrorStatuses.BAD_REQUEST, exception.getErrorStatus());
        assertTrue(exception.getMessage().contains("Please set a budget first"));
    }

    @Test
    @DisplayName("Should reject transaction when it exceeds remaining budget")
    void testBeforeCreateTransaction_ExceedsBudget_ThrowsException() {
        // Given
        testBudget.setAmount(new BigDecimal("1000.00"));

        Transactions existingTransaction = Transactions.create();
        existingTransaction.setAmount(new BigDecimal("800.00"));

        testTransaction.setAmount(new BigDecimal("300.00")); // Would exceed 1000 total

        List<Transactions> newTransactions = Collections.singletonList(testTransaction);

        when(db.run(any(Select.class))).thenReturn(result);
        when(result.single(Budget.class)).thenReturn(testBudget);
        when(result.listOf(Transactions.class)).thenReturn(Collections.singletonList(existingTransaction));

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> handler.beforeCreateTransaction(newTransactions));

        assertEquals(ErrorStatuses.BAD_REQUEST, exception.getErrorStatus());
        assertTrue(exception.getMessage().contains("exceeds remaining budget"));
    }

    @Test
    @DisplayName("Should allow transaction within remaining budget")
    void testBeforeCreateTransaction_WithinBudget_Success() {
        // Given
        testBudget.setAmount(new BigDecimal("1000.00"));

        Transactions existingTransaction = Transactions.create();
        existingTransaction.setAmount(new BigDecimal("500.00"));

        testTransaction.setAmount(new BigDecimal("200.00")); // Total: 700, within 1000

        List<Transactions> newTransactions = Collections.singletonList(testTransaction);

        when(db.run(any(Select.class))).thenReturn(result);
        when(result.single(Budget.class)).thenReturn(testBudget);
        when(result.listOf(Transactions.class)).thenReturn(Collections.singletonList(existingTransaction));

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> handler.beforeCreateTransaction(newTransactions));
    }

    // ============================================================================
    // BUDGET VALIDATION TESTS
    // ============================================================================

    @Test
    @DisplayName("Should create budget with valid positive amount")
    void testBeforeSaveBudget_ValidAmount_Success() {
        // Given
        testBudget.setAmount(new BigDecimal("5000.00"));
        List<Budget> budgets = Collections.singletonList(testBudget);

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> handler.beforeSaveBudget(budgets));
    }

    @Test
    @DisplayName("Should reject budget with zero amount")
    void testBeforeSaveBudget_ZeroAmount_ThrowsException() {
        // Given
        testBudget.setAmount(BigDecimal.ZERO);
        List<Budget> budgets = Collections.singletonList(testBudget);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> handler.beforeSaveBudget(budgets));

        assertEquals(ErrorStatuses.BAD_REQUEST, exception.getErrorStatus());
        assertTrue(exception.getMessage().contains("must be greater than zero"));
    }

    @Test
    @DisplayName("Should reject budget with negative amount")
    void testBeforeSaveBudget_NegativeAmount_ThrowsException() {
        // Given
        testBudget.setAmount(new BigDecimal("-1000.00"));
        List<Budget> budgets = Collections.singletonList(testBudget);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> handler.beforeSaveBudget(budgets));

        assertEquals(ErrorStatuses.BAD_REQUEST, exception.getErrorStatus());
        assertTrue(exception.getMessage().contains("must be greater than zero"));
    }

    @Test
    @DisplayName("Should reject budget with null amount")
    void testBeforeSaveBudget_NullAmount_ThrowsException() {
        // Given
        testBudget.setAmount(null);
        List<Budget> budgets = Collections.singletonList(testBudget);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> handler.beforeSaveBudget(budgets));

        assertEquals(ErrorStatuses.BAD_REQUEST, exception.getErrorStatus());
        assertTrue(exception.getMessage().contains("amount is required"));
    }

    // ============================================================================
    // BUDGET DELETION TESTS
    // ============================================================================

    @Test
    @DisplayName("Should prevent budget deletion when transactions exist")
    void testBeforeDeleteBudget_WithTransactions_ThrowsException() {
        // Given
        List<Transactions> existingTransactions = Collections.singletonList(testTransaction);

        when(db.run(any(Select.class))).thenReturn(result);
        when(result.listOf(Transactions.class)).thenReturn(existingTransactions);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> handler.beforeDeleteBudget());

        assertEquals(ErrorStatuses.BAD_REQUEST, exception.getErrorStatus());
        assertTrue(exception.getMessage().contains("Cannot delete budget while transactions exist"));
    }

    @Test
    @DisplayName("Should allow budget deletion when no transactions exist")
    void testBeforeDeleteBudget_NoTransactions_Success() {
        // Given
        when(db.run(any(Select.class))).thenReturn(result);
        when(result.listOf(Transactions.class)).thenReturn(Collections.emptyList());

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> handler.beforeDeleteBudget());
    }

    // ============================================================================
    // TRANSACTION UPDATE TESTS
    // ============================================================================

    @Test
    @DisplayName("Should allow transaction update when amount is decreased")
    void testBeforeUpdateTransaction_DecreaseAmount_Success() {
        // Given
        Transactions oldTransaction = Transactions.create();
        oldTransaction.setId("trans-1");
        oldTransaction.setAmount(new BigDecimal("500.00"));
        oldTransaction.setIsActiveEntity(true);

        testTransaction.setAmount(new BigDecimal("300.00")); // Decreased
        List<Transactions> transactions = Collections.singletonList(testTransaction);

        when(db.run(any(Select.class))).thenReturn(result);
        when(result.first(Transactions.class)).thenReturn(Optional.of(oldTransaction));

        // When & Then - Should not throw exception (no budget check needed when decreasing)
        assertDoesNotThrow(() -> handler.beforeUpdateTransaction(transactions));
    }

    @Test
    @DisplayName("Should validate budget when transaction amount is increased")
    void testBeforeUpdateTransaction_IncreaseAmount_ValidatesBudget() {
        // Given
        testBudget.setAmount(new BigDecimal("1000.00"));

        Transactions oldTransaction = Transactions.create();
        oldTransaction.setId("trans-1");
        oldTransaction.setAmount(new BigDecimal("300.00"));
        oldTransaction.setIsActiveEntity(true);

        testTransaction.setId("trans-1");
        testTransaction.setAmount(new BigDecimal("500.00")); // Increased by 200

        List<Transactions> transactions = Collections.singletonList(testTransaction);

        when(db.run(any(Select.class))).thenReturn(result);
        when(result.first(Transactions.class)).thenReturn(Optional.of(oldTransaction));
        when(result.single(Budget.class)).thenReturn(testBudget);
        when(result.listOf(Transactions.class)).thenReturn(Collections.singletonList(oldTransaction));

        // When & Then - Should not throw exception (increase of 200 is within budget)
        assertDoesNotThrow(() -> handler.beforeUpdateTransaction(transactions));
    }

    // ============================================================================
    // CUSTOM ACTION TESTS
    // ============================================================================

    @Test
    @DisplayName("Should mark transaction as reviewed")
    void testMarkAsReviewed_Success() {
        // Given
        TransactionsMarkAsReviewedContext context = mock(TransactionsMarkAsReviewedContext.class);

        when(context.getCqn()).thenReturn(mock(com.sap.cds.ql.cqn.CqnSelect.class));
        when(db.run(any(com.sap.cds.ql.cqn.CqnSelect.class))).thenReturn(result);
        when(result.single(Transactions.class)).thenReturn(testTransaction);

        // When
        handler.onMarkAsReviewed(context);

        // Then
        verify(messages).success(contains("marked as reviewed"));
        verify(context).setCompleted();
    }

    @Test
    @DisplayName("Should flag transaction for audit")
    void testFlagForAudit_Success() {
        // Given
        TransactionsFlagForAuditContext context = mock(TransactionsFlagForAuditContext.class);

        when(context.getCqn()).thenReturn(mock(com.sap.cds.ql.cqn.CqnSelect.class));
        when(db.run(any(com.sap.cds.ql.cqn.CqnSelect.class))).thenReturn(result);
        when(result.single(Transactions.class)).thenReturn(testTransaction);

        // When
        handler.onFlagForAudit(context);

        // Then
        verify(messages).warn(contains("flagged for audit"));
        verify(context).setCompleted();
    }

    @Test
    @DisplayName("Should throw exception when marking non-existent transaction as reviewed")
    void testMarkAsReviewed_TransactionNotFound_ThrowsException() {
        // Given
        TransactionsMarkAsReviewedContext context = mock(TransactionsMarkAsReviewedContext.class);

        when(context.getCqn()).thenReturn(mock(com.sap.cds.ql.cqn.CqnSelect.class));
        when(db.run(any(com.sap.cds.ql.cqn.CqnSelect.class))).thenReturn(result);
        when(result.single(Transactions.class)).thenReturn(null);

        // When & Then
        ServiceException exception = assertThrows(ServiceException.class,
                () -> handler.onMarkAsReviewed(context));

        assertEquals(ErrorStatuses.NOT_FOUND, exception.getErrorStatus());
        assertTrue(exception.getMessage().contains("Transaction not found"));
    }

    // ============================================================================
    // BUDGET SUMMARY TESTS
    // ============================================================================

    @Test
    @DisplayName("Should calculate budget summary correctly with transactions")
    void testOnReadBudgetSummary_WithTransactions_CalculatesCorrectly() {
        // Given
        testBudget.setAmount(new BigDecimal("10000.00"));
        testBudget.setCurrency("AUD");

        Transactions trans1 = Transactions.create();
        trans1.setAmount(new BigDecimal("3000.00"));

        Transactions trans2 = Transactions.create();
        trans2.setAmount(new BigDecimal("2000.00"));

        List<Transactions> transactions = Arrays.asList(trans1, trans2);

        when(db.run(any(Select.class))).thenReturn(result);
        when(result.single(Budget.class)).thenReturn(testBudget);
        when(result.listOf(Transactions.class)).thenReturn(transactions);

        CdsReadEventContext context = mock(CdsReadEventContext.class);

        // When
        handler.onReadBudgetSummary(context);

        // Then
        verify(context).setResult(argThat(list -> {
            @SuppressWarnings("unchecked")
            List<java.util.Map<String, Object>> summaryList = (List<java.util.Map<String, Object>>) list;
            java.util.Map<String, Object> summary = summaryList.get(0);

            return summary.get("totalBudget").equals(new BigDecimal("10000.00")) &&
                   summary.get("currency").equals("AUD") &&
                   summary.get("spentAmount").equals(new BigDecimal("5000.00")) &&
                   summary.get("remainingAmount").equals(new BigDecimal("5000.00")) &&
                   summary.get("transactionCount").equals(2) &&
                   summary.get("budgetUtilization").equals(new BigDecimal("50.00"));
        }));
    }

    @Test
    @DisplayName("Should handle budget summary when no budget exists")
    void testOnReadBudgetSummary_NoBudget_ReturnsZeros() {
        // Given
        when(db.run(any(Select.class))).thenReturn(result);
        when(result.single(Budget.class)).thenReturn(null);
        when(result.listOf(Transactions.class)).thenReturn(Collections.emptyList());

        CdsReadEventContext context = mock(CdsReadEventContext.class);

        // When
        handler.onReadBudgetSummary(context);

        // Then
        verify(context).setResult(argThat(list -> {
            @SuppressWarnings("unchecked")
            List<java.util.Map<String, Object>> summaryList = (List<java.util.Map<String, Object>>) list;
            java.util.Map<String, Object> summary = summaryList.get(0);

            return summary.get("totalBudget").equals(BigDecimal.ZERO) &&
                   summary.get("currency").equals("AUD") &&
                   summary.get("spentAmount").equals(BigDecimal.ZERO) &&
                   summary.get("remainingAmount").equals(BigDecimal.ZERO) &&
                   summary.get("transactionCount").equals(0) &&
                   summary.get("budgetUtilization").equals(BigDecimal.ZERO);
        }));
    }
}