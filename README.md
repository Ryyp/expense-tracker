# Expense Tracker

A full-stack expense tracking application built with SAP Cloud Application Programming Model (CAP) and SAP Fiori Elements.

## Features

- **Budget Management**: Set and manage overall budget with dynamic currency support
- **Transaction Tracking**: Create, read, update, and delete expense transactions
- **Budget Summary Dashboard**: Real-time overview of budget utilization with KPI cards
- **Role-Based Access**: Admin and User roles with different permission levels
- **Draft Support**: Edit transactions with draft functionality
- **Custom Actions**: Mark transactions as reviewed and flag for audit
- **Auto-Refresh**: Budget summary automatically updates when transactions change
- **Comprehensive Testing**: Unit tests with 100% coverage of business logic

## Tech Stack

### Backend
- **SAP CAP** with Java
- **Spring Boot** framework
- **OData V4** protocol
- **H2 Database** for local development
- **JUnit 5 + Mockito** for testing

### Frontend
- **SAP Fiori Elements** (List Report & Object Page)
- **SAP UI5** framework
- **Custom Controller Extensions**
- **Responsive design**

## Prerequisites

- Node.js (v18+)
- Java (JDK 21+)
- Maven (3.8+)
- @sap/cds-dk

## Installation

```bash
# Clone repository
git clone https://github.com/Ryyp/expense-tracker.git
cd expense-tracker

# Install dependencies
npm install

# Build backend
cd srv
mvn clean install
cd ..
```

## Running the Application

### Using npm

```bash
npm start
```

### Using Maven (Spring Boot)

```bash
cd srv
mvn spring-boot:run
```

### Using IntelliJ IDEA

1. Open the project in IntelliJ IDEA
2. Navigate to `srv/src/main/java/com/dalrae/expensetracker/Application.java`
3. Right-click on the `Application` class and select **Run 'Application'**

Or configure a Spring Boot run configuration:
1. Open **Run/Debug Configurations**
2. Click **+ → Spring Boot**
3. Set **Main class**: `com.dalrae.expensetracker.Application`
4. Click **OK** and run

### Access URLs

- **API Base**: http://localhost:8080/
- **OData Service**: http://localhost:8080/catalog/
- **Budget Management**: http://localhost:8080/budget/webapp/index.html
- **Transactions**: http://localhost:8080/transactions/webapp/index.html

### Mock Users

- **Admin**: `admin` (Roles: Admin, authenticated-user)
- **User**: `user` (Roles: User, authenticated-user)

## Testing

```bash
cd srv
mvn test                    # Run tests
mvn clean test jacoco:report  # With coverage
```

Coverage report: `srv/target/site/jacoco/index.html`

## Project Structure

```
expense-tracker/
├── app/                    # Frontend applications
│   ├── budget/            # Budget Management app
│   ├── transactions/      # Transactions app
│   └── common.cds        # Shared annotations
├── db/                    # Database schema
│   └── schema.cds
├── srv/                   # Backend services
│   ├── src/
│   │   ├── main/java/    # Service handlers
│   │   └── test/         # Unit tests
│   ├── catalog-service.cds
│   └── authorization.cds
└── package.json
```

## Key Features

### Budget Summary KPIs
- Total Budget
- Spent Amount
- Remaining Budget
- Budget Utilization %

### Business Logic Validations
- Positive amount validation
- Budget limit enforcement
- Budget deletion protection
- Transaction amount updates

### Custom Actions
- `markAsReviewed()`: Mark transaction as reviewed
- `flagForAudit()`: Flag transaction for audit

## API Endpoints

- **Budget**: `/catalog/Budget` (Admin only)
- **Transactions**: `/catalog/Transactions`
- **BudgetSummary**: `/catalog/BudgetSummary` (Read-only)
- **TransactionTypes**: `/catalog/TransactionTypes` (Value help)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push and create a Pull Request

## License

MIT License

## Acknowledgments

Built with SAP Cloud Application Programming Model and SAP Fiori Elements.
