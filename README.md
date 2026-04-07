# SimpleBank - Java Learning Project

A guided Java banking project you will build and improve in five versions, with mentor feedback at each stage. Part of the goal is to enjoy the work and create a strong portfolio piece.

**Purpose**: Apply software development skills by creating a simple banking system while following four LinkedIn Learning courses (v1–v4), a few additional learning materials, plus one final AI-assisted phase (v5) where you choose a short course on AI-assisted development and use agentic AI to add a graphical user interface.

This project emphasizes strong fundamentals in software design, testing, and problem solving, while also encouraging you to adopt tools and approaches suited to the AI era. **AI-specific assistance (code-generation tools, copilots, agentic AI) is off-limits during v1–v4.** You are expected to write the core application yourself to build genuine understanding. AI tools are introduced only in v5, after the core application is complete.

**Target audience**: Engineers from junior to mid-level who want to practice building a full application with clear business rules and evolving requirements, including optional stretch goals for more advanced engineering skills.

---

- [SimpleBank - Java Learning Project](#simplebank---java-learning-project)
	- [Project Overview](#project-overview)
	- [User Interface](#user-interface)
		- [Main Menu](#main-menu)
		- [Account Menu (after successful login)](#account-menu-after-successful-login)
	- [Learning Roadmap and Version Goals](#learning-roadmap-and-version-goals)
	- [Business Rules (Functional Requirements)](#business-rules-functional-requirements)
		- [Accounts](#accounts)
		- [Banking Transactions](#banking-transactions)
		- [Security \& Validation](#security--validation)
		- [Transaction History](#transaction-history)
		- [Advanced Business Rules (Introduced in v4)](#advanced-business-rules-introduced-in-v4)
		- [Error Messages (Use These Exact Texts When the Related Rule Applies)](#error-messages-use-these-exact-texts-when-the-related-rule-applies)
	- [Implementation Requirements (Non-Functional Requirements)](#implementation-requirements-non-functional-requirements)
		- [Additional Reading Before Starting v2](#additional-reading-before-starting-v2)
	- [Important Design Goals \& Testing Philosophy](#important-design-goals--testing-philosophy)
	- [Suggested Schedule (Months 1–8)](#suggested-schedule-months-18)
	- [Mentor and Mentee Workflow](#mentor-and-mentee-workflow)
	- [Getting Started](#getting-started)

## Project Overview

You will build a command-line banking application that starts simple and becomes more professional over time.

The system allows customers to:
- Open new accounts
- Deposit and withdraw money
- Transfer money between accounts
- View their transaction history
- Check account balance

---

## User Interface

In v1 through v4, the application runs on the command line and uses a simple menu-driven interface. In v5, you will add a browser-based UI on top of the existing business logic and storage layers. The CLI should remain available, and both the GUI and CLI must be active after the app starts.

### Main Menu

1. Create New Account  
2. Login to Account  
3. Exit  

### Account Menu (after successful login)

1. Check Balance  
2. Deposit Money  
3. Withdraw Money  
4. Transfer Money  
5. View Transaction History  
6. Logout  

---

## Learning Roadmap and Version Goals

| LinkedIn Course | Additional Learning | Version to Build | Core Deliverables | Optional Stretch Goals | Git Tag |
|--------|---------------------|------------------|-------------------|------------------------|---------|
| [Learning Java Collections](https://www.linkedin.com/learning/learning-java-collections) | [Test Sociably, Fake the Boundaries](test-sociably-fake-the-boundaries.md) *(optional)* | **v1** | Fully working app using in-memory storage. Use Maps for `O(1)` account lookups, and Streams with Lambda expressions for formatting/filtering transaction history. Include a solid suite of automated unit tests for your core logic. | Apply concepts from [Test Sociably, Fake the Boundaries](test-sociably-fake-the-boundaries.md). | `v1-in-memory` |
| [Relational Databases Essential Training](https://www.linkedin.com/learning/relational-databases-essential-training) | - [Database Integration Essentials](database-integration-essentials.md)<br> - [Additional Reading Before Starting v2](#additional-reading-before-starting-v2) | **v2** | Add PostgreSQL storage using raw SQL. Use the provided `DatabaseConnectionManager` to handle connection boilerplate. Use `CHECK` constraints to enforce business rules. Design a properly **Normalized** schema. Use `PreparedStatements` to strictly prevent **SQL Injection**. | Ensure your transaction history queries avoid the **N+1 performance problem**. Create an in-memory **Fake** (e.g., `FakeAccountRepository`) for your tests. Use Docker Compose (`docker-compose.yml`) to run PostgreSQL locally and a starter SQL bootstrap script (`simplebank/sql/seed.sql`) to initialize it. | `v2-with-storage` |
| [Java Refactoring Best Practices](https://www.linkedin.com/learning/java-refactoring-best-practices) | — | **v3** | Clean up the code. Refactor primitive obsession by creating a custom `Money` class for currency. *(Hint: Ensure your database repositories from v2 are updated to map SQL decimals directly into your new Money objects).* Handle exceptions properly and remove dead code. Ensure all tests still pass. | Build a robust Custom Exception Hierarchy (e.g., `InsufficientFundsException`, `InvalidPinException`). | `v3-refactored` |
| [Advanced Design Patterns: Design Principles](https://www.linkedin.com/learning/advanced-design-patterns-design-principles) | — | **v4** | Apply SOLID design principles and add Checking/Savings account types. Favor Composition over Inheritance to apply account behaviors rather than creating a rigid class hierarchy. Add tests for the new rules. | Apply the Interface Segregation Principle by breaking large interfaces into smaller ones (e.g., `Transferable`, `InterestBearing`). Implement the daily outgoing limit stretch goal. | `v4-solid` |
| An up-to-date AI-Assisted Development short course | — | **v5** | Add an HTML-based graphical user interface using the existing business logic and storage layers. | Improve styling, responsiveness, or extra UI conveniences after the required flows work. | `v5-gui-ai-assisted` |

Important:
- Unless a rule is explicitly marked as starting in `v4`, it applies from `v1` onward.
- In `v1`, all data exists only while the program is running. Persistent storage starts in `v2`.
- Stretch goals are optional and do not block marking a version complete.
- `v3` is intentionally about refactoring existing behavior, not adding new business rules.
- `v5` is intentionally about leveraging agentic AI for replacing the interface, not redesigning the domain logic. It is more flexible, with fewer strict requirements, and gives the engineer freedom to choose the user interface and implementation approach while keeping the core app stable.

---

## Business Rules (Functional Requirements)
**Mandatory in `v1` and later**

### Accounts
- Every account has a unique account number (format: ACC-000123)
- Account numbers are generated by the system, not typed by the user
- Every account belongs to one person (account holder name)
- Every account has a 4-digit PIN for security
- Account balance can never be negative

### Banking Transactions
- All deposits, withdrawals, and transfers must be recorded
- Every transaction includes date & time, type, amount, balance after the transaction, and description
- A withdrawal or transfer always uses the currently logged-in account as the source account
- A transfer must be made to a different target account
- A transfer must either complete fully or not happen at all
- Every successful transfer creates two linked transaction records: `TRANSFER_OUT` in the source account and `TRANSFER_IN` in the target account
- The two transfer records must use the same timestamp and amount, and the description must include the other account number
- Minimum amount for any transaction is 0.01
- Maximum amount for any single transaction is 10,000.00

### Security & Validation
- PIN must be exactly 4 digits
- Account holder name cannot be empty
- You cannot withdraw or transfer more money than the current balance
- The system must clearly explain any errors to the user

### Transaction History
- Transaction history is shown for the currently logged-in account only
- Transactions must be displayed from newest to oldest
- Each history entry must use this format:

	`YYYY-MM-DD HH:MM:SS | TYPE | AMOUNT | BALANCE_AFTER | DESCRIPTION`

- `TYPE` must be one of: `DEPOSIT`, `WITHDRAWAL`, `TRANSFER_IN`, `TRANSFER_OUT`
- `AMOUNT` and `BALANCE_AFTER` must always use two decimal places
- Example:

	`2026-04-18 14:32:10 | TRANSFER_OUT | 250.00 | 1200.00 | Transfer to ACC-000205`

- If an account has no transactions yet, the system must clearly say so

### Advanced Business Rules (Introduced in v4)
- The rules in this subsection do not apply to `v1`, `v2`, or `v3`

**Mandatory in `v4` and later**
- When creating an account in `v4` and later, the user must choose an account type: `CHECKING` or `SAVINGS`
- `SAVINGS` accounts must keep a minimum balance of `100.00` after any withdrawal or outgoing transfer
- If a mandatory advanced-rule validation fails, the operation must be rejected before any balance or transaction history is changed

**Optional stretch goal in `v4`**
- For any account, the total amount sent out per calendar day (`withdrawals + outgoing transfers`) cannot exceed `5000.00`
- If this stretch goal is implemented, the validation must happen before any balance or transaction history is changed

### Error Messages (Use These Exact Texts When the Related Rule Applies)

- "Account holder name cannot be blank"
- "PIN must be exactly 4 digits"
- "Amount must be at least 0.01"
- "Maximum transaction amount is 10000.00"
- "Insufficient funds. Current balance: X.XX" (where `X.XX` is the actual balance with two decimal places)
- "Invalid account number or PIN"
- "Source and target account must be different"
- "Target account does not exist"
- "Savings accounts must maintain a minimum balance of 100.00"
- "Daily outgoing limit exceeded. Remaining daily limit: X.XX" (only if the optional `v4` stretch goal is implemented; `X.XX` is the actual remaining limit with two decimal places)

---

## Implementation Requirements (Non-Functional Requirements)  

**Mandatory in `v2`:**
- **SQL Injection Prevention**: The application must never concatenate raw user input into SQL query strings. You must use parameter binding (e.g., `PreparedStatements` in Java).
- **Proper Normalization**: The database schema must be in at least 3rd Normal Form (e.g., account details and transaction records should live in separate, properly linked tables using Foreign Keys).
- **Transfer Atomicity with SQL Transactions**: Because a transfer must either complete fully or not happen at all, you must wrap multi-step database operations in an explicit transaction (`BEGIN` / `COMMIT` / `ROLLBACK`, or `Connection.setAutoCommit(false)` in Java). Without this, a crash between the debit and credit SQL statements would leave accounts in an inconsistent state.

**Optional stretch goal in `v2`:**
- **The N+1 Problem**: When viewing transaction history or generating reports, the application must fetch the necessary data efficiently (e.g., using SQL `JOIN`s) rather than executing a new database query for every single row of history.

### Additional Reading Before Starting v2

These readings explain the three concepts referenced in the mandatory and optional v2 requirements above.

**1. SQL Injection Prevention**
SQL Injection is a vulnerability where an attacker alters your database queries using malicious inputs. In Java, the primary, non-negotiable defense is using `PreparedStatement` (parameterized queries) which separates the SQL logic from the data.
* **Read:** [Understanding and Preventing SQL Injection in Java Applications (GeeksforGeeks)](https://www.geeksforgeeks.org/java/understanding-and-preventing-sql-injection-in-java-applications/)

**2. Database Normalization**
Normalization is a database design process that breaks down large tables into smaller, related tables to eliminate data redundancy and improve data integrity.
* **Read:** [What Is Database Normalization? (IBM)](https://www.ibm.com/think/topics/database-normalization)

**Optional stretch goal reading**

**3. The N+1 Query Problem** *(only if pursuing the optional N+1 stretch goal)*
The N+1 problem is a severe performance issue where an application executes one query to fetch a list of entities (the "1"), and then executes "N" additional queries inside a loop to fetch related data for each entity. In your app, this could happen if you fetch an Account, and then run a separate query for every single Transaction history row.
* **Read:** [The N+1 Query Problem (Vlad Mihalcea)](https://vladmihalcea.com/n-plus-1-query-problem/)

---

## Important Design Goals & Testing Philosophy

Build the application so that:
- The core banking logic is separate from how data is stored.
- The user interface is separate from the banking logic.

**Testing is Mandatory**: You must write automated tests (e.g., using JUnit). A basic suite of tests that proves your application works correctly is required for every version.

**Optional stretch goal - Testing Challenge (The Pragmatic Way)**: If you want to push your engineering skills further, read and apply concepts from [test-sociably, fake-the-boundaries](test-sociably-fake-the-boundaries.md). Specifically, try to:
- **Test Sociably**: Test using real collaborators/dependencies.
- **Fake the Boundaries**: Use Fakes (in-memory implementations) for external boundaries like databases. Only use standard mocks for UI-to-Service wiring or error simulations that are hard to fake.

---

## Suggested Schedule (Months 1–8)

This project is self-paced, but we provide a suggested 8-month schedule to help you plan your work. The first seven months focus on strong fundamentals, while the final month is reserved for the AI-assisted interface phase.  
The schedule also includes vacation time so you can balance progress with breaks.

**Total commitment**: 5 hours per week  
**Vacation**: 2 weeks in Month 5 (no work expected)

| Period              | Focus                                              | Expected Deliverable                  |
|---------------------|----------------------------------------------------|---------------------------------------|
| Month 1–2          | v1 + Learning Java Collections course              | v1 complete                           |
| Month 3–4          | v2 + Relational Databases course                  | v2 complete                           |
| Month 5            | Vacation (2 weeks) + light catch-up if needed      | —                                     |
| Month 6            | v3 + Java Refactoring Best Practices course        | v3 complete                           |
| Month 7            | v4 + Advanced Design Patterns: Design Principles course | v4 complete                           |
| Month 8            | v5 + AI-Assisted Development month                 | v5 complete (HTML GUI with AI help; AI use restricted to this phase)   |

---

## Mentor and Mentee Workflow

For each version:
1. As the mentee, work in your own fork only.
2. Create a branch for the version, matching the tag name where possible (for example `v1-in-memory`).
3. Finish the work and commit your changes.
4. Push your branch to your fork, open a pull request from your fork branch to the upstream `main` branch for mentor review only, and send the PR link to your mentor. The mentor will not merge mentee pull requests.
5. After review and approval, merge the branch into your fork's `main`.
6. Create the matching tag in your fork, push it, and inform your mentor of the tag name.
7. Start the next version from your fork's `main`.

---

## Getting Started

1. Set up your fork and follow the Mentor and Mentee Workflow above.
2. Read the business rules carefully.
3. Read the v1 roadmap row and start with the in-memory CLI version first.
4. Note the provided `DatabaseConnectionManager.java` file in the starter code; you will use it in v2 to connect to PostgreSQL without writing JDBC setup boilerplate. You can safely ignore it during v1. When you reach v2, start PostgreSQL with `docker compose up -d` from the repo root. Use the (initially empty) `simplebank/sql/seed.sql` to create the starter schema.
5. Start building.

---

**Happy Coding!**
