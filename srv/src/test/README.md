# Expense Tracker - Unit Tests

## Overview

This directory contains comprehensive unit tests for the Expense Tracker Java application.

## Test Coverage

### CatalogServiceHandlerTest

Tests for the main service handler covering:

#### 1. **Permission Tests**
- ✅ Admin users can read Budget data
- ✅ Non-admin users are denied Budget access with proper error message

#### 2. **Transaction Amount Validation**
- ✅ Valid positive amounts are accepted
- ✅ Zero amounts are rejected
- ✅ Negative amounts are rejected
- ✅ Null amounts are rejected

#### 3. **Budget Availability Validation**
- ✅ Transactions are rejected when no budget exists
- ✅ Transactions exceeding remaining budget are rejected
- ✅ Transactions within remaining budget are accepted

#### 4. **Budget Validation**
- ✅ Valid positive budget amounts are accepted
- ✅ Zero budget amounts are rejected
- ✅ Negative budget amounts are rejected
- ✅ Null budget amounts are rejected

#### 5. **Budget Deletion**
- ✅ Budget deletion is prevented when transactions exist
- ✅ Budget deletion is allowed when no transactions exist

#### 6. **Transaction Updates**
- ✅ Decreasing transaction amount is allowed
- ✅ Increasing transaction amount validates against budget

#### 7. **Custom Actions**
- ✅ Mark transaction as reviewed
- ✅ Flag transaction for audit
- ✅ Proper error handling for non-existent transactions

#### 8. **Budget Summary Calculations**
- ✅ Correct calculation with transactions
- ✅ Proper handling when no budget exists
- ✅ Budget utilization percentage calculation

## Running Tests

### Run all tests
```bash
cd srv
mvn test
```

### Run specific test class
```bash
mvn test -Dtest=CatalogServiceHandlerTest
```

### Run specific test method
```bash
mvn test -Dtest=CatalogServiceHandlerTest#testBeforeReadBudget_AdminUser_Success
```

### Run tests with coverage
```bash
mvn clean test jacoco:report
```

Coverage report will be available at: `srv/target/site/jacoco/index.html`

### Run tests in verbose mode
```bash
mvn test -X
```

## Test Structure

```
srv/src/test/java/
└── com/
    └── dalrae/
        └── expensetracker/
            └── handlers/
                └── CatalogServiceHandlerTest.java
```

## Test Dependencies

- **JUnit 5** (Jupiter): Modern testing framework
- **Mockito**: Mocking framework for isolating tests
- **AssertJ**: Fluent assertions for better readability
- **Spring Boot Test**: Integration with Spring Boot

## Best Practices

1. **Test Naming Convention**: `test<MethodName>_<Scenario>_<ExpectedResult>`
   - Example: `testBeforeCreateTransaction_ValidAmount_Success`

2. **AAA Pattern**: Arrange, Act, Assert
   ```java
   // Given (Arrange)
   testTransaction.setAmount(new BigDecimal("500.00"));

   // When (Act)
   handler.beforeCreateTransaction(transactions);

   // Then (Assert)
   assertDoesNotThrow(() -> ...);
   ```

3. **Isolation**: Each test is independent and uses mocks
   - No database required
   - Fast execution
   - Predictable results

4. **Coverage**: Aim for >80% code coverage on business logic

## Adding New Tests

When adding new functionality to `CatalogServiceHandler`:

1. Add corresponding test methods in `CatalogServiceHandlerTest`
2. Follow the existing naming and structure patterns
3. Use `@DisplayName` for clear test descriptions
4. Mock external dependencies (database, messages, etc.)
5. Test both success and failure scenarios

## Common Test Patterns

### Testing Exceptions
```java
ServiceException exception = assertThrows(ServiceException.class,
    () -> handler.someMethod(params));

assertEquals(ErrorStatuses.BAD_REQUEST, exception.getErrorStatus());
assertTrue(exception.getMessage().contains("expected message"));
```

### Testing with Mocks
```java
when(db.run(any(Select.class))).thenReturn(result);
when(result.single(Budget.class)).thenReturn(testBudget);

// Execute test
handler.someMethod();

// Verify interactions
verify(db, times(1)).run(any(Select.class));
```

### Testing Success Cases
```java
assertDoesNotThrow(() -> handler.someMethod(params));
verify(messages).success(contains("expected message"));
```

## Continuous Integration

Tests are automatically run on:
- Every commit
- Pull request creation
- Before deployment

Failed tests will block the build and deployment.

## Troubleshooting

### Tests fail with "No bean found"
- Make sure `@ExtendWith(MockitoExtension.class)` is present
- Check that all dependencies are properly mocked with `@Mock`

### Tests fail with ClassNotFoundException
- Run `mvn clean install` to regenerate CDS entities
- Ensure `cds-maven-plugin` has run successfully

### Tests are slow
- Check if you're accidentally hitting real database
- Ensure all external dependencies are mocked
- Use `@ExtendWith(MockitoExtension.class)` not `@SpringBootTest`

## Further Reading

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [CAP Java Testing Guide](https://cap.cloud.sap/docs/java/developing-applications/testing)
