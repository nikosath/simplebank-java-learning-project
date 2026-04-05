# Database Integration Essentials

This guide bridges the gap between relational database theory and your Java code for Version 2. To keep your focus on software architecture and raw SQL, we are intentionally avoiding massive ORM frameworks (like Hibernate or Spring Data). 

Instead, you will learn universal data access patterns and how to write secure queries.

---

- [Database Integration Essentials](#database-integration-essentials)
  - [1. The Repository Design Pattern](#1-the-repository-design-pattern)
  - [2. Bypassing the Boilerplate (The Scaffold)](#2-bypassing-the-boilerplate-the-scaffold)
  - [2b. Running PostgreSQL with Docker Compose](#2b-running-postgresql-with-docker-compose)
  - [3. Defeating SQL Injection (Prepared Statements)](#3-defeating-sql-injection-prepared-statements)
  - [4. Inserting and Updating Data](#4-inserting-and-updating-data)
  - [5. Protecting Multi-Step Operations (SQL Transactions)](#5-protecting-multi-step-operations-sql-transactions)
  - [6. Enforcing Business Rules in the Schema (CHECK Constraints)](#6-enforcing-business-rules-in-the-schema-check-constraints)
    - [Two-layer responsibility](#two-layer-responsibility)
  - [7. Configuration \& Credentials](#7-configuration--credentials)

## 1. The Repository Design Pattern
As mentioned in the testing guide, your core banking logic should not know about SQL, databases, or tables. It should only know about Domain Objects (like `Account`) and Interfaces (like `AccountRepository`).

The **Repository Pattern** acts as a bridge. You define an interface in your domain logic, and you write a concrete implementation that talks to the database.

```java
// 1. The Domain Interface (Your service uses this)
public interface AccountRepository {
    void save(Account account);
    Optional<Account> findByAccountNumber(String accountNumber);
}

// 2. The Database Implementation (This contains the SQL)
public class PostgresAccountRepository implements AccountRepository {
    // SQL logic goes here
}
```

Note: In your tests, you will create a `FakeAccountRepository` that implements this exact same interface using an in-memory `Map`.

## 2. Bypassing the Boilerplate (The Scaffold)
Connecting a language to a database usually requires tedious boilerplate (loading drivers, handling connection strings). To eliminate this friction, this project includes a `DatabaseConnectionManager` class.

Whenever your repository needs to talk to the database, ask the manager for a connection. Always use a try-with-resources block so the connection is automatically closed when you are done, preventing resource leaks.

```java
try (Connection conn = DatabaseConnectionManager.getConnection()) {
    // Execute your SQL here
} catch (SQLException e) {
    throw new RuntimeException("Database operation failed", e);
}
```

## 2b. Running PostgreSQL with Docker Compose
If you do not want to install PostgreSQL manually, the starter repo includes a basic `docker-compose.yml` at the repo root. It starts a local PostgreSQL container and mounts the empty starter `simplebank/sql/seed.sql` file into PostgreSQL's init folder, so the mentee has a dedicated place to add schema and seed statements later.

```bash
docker compose up -d
docker compose down
```

If you add content to the seed file later, recreate the container or its volume so the init script runs again.

## 3. Defeating SQL Injection (Prepared Statements)
Mandatory rule: you must never concatenate raw user input into a SQL string.

If you write this:

```java
String sql = "SELECT * FROM accounts WHERE name = '" + userInput + "'";
```

An attacker can input `' OR '1'='1`, changing your query to:

```sql
SELECT * FROM accounts WHERE name = '' OR '1'='1'
```

The solution: prepared statements.
Instead of concatenating strings, use `?` as a placeholder. The database driver treats the injected value strictly as data, never as executable code.

```java
public Optional<Account> findByAccountNumber(String accountNumber) {
    String sql = "SELECT account_number, balance, pin FROM accounts WHERE account_number = ?";
    
    try (Connection conn = DatabaseConnectionManager.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
         
        // Bind the variable to the first '?' placeholder (1-indexed)
        stmt.setString(1, accountNumber);
        
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                // Map the ResultSet row back into a Domain Object
                return Optional.of(new Account(
                    rs.getString("account_number"),
                    rs.getBigDecimal("balance"),
                    rs.getString("pin")
                ));
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException("Failed to fetch account", e);
    }
    return Optional.empty();
}
```

## 4. Inserting and Updating Data
For `INSERT`, `UPDATE`, or `DELETE`, the pattern is identical, but you use `executeUpdate()` instead of `executeQuery()`.

```java
public void save(Account account) {
    String sql = "INSERT INTO accounts (account_number, balance, pin) VALUES (?, ?, ?)";
    
    try (Connection conn = DatabaseConnectionManager.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
         
        stmt.setString(1, account.getAccountNumber());
        stmt.setBigDecimal(2, account.getBalance());
        stmt.setString(3, account.getPin());
        
        // Execute the change
        stmt.executeUpdate();
        
    } catch (SQLException e) {
        throw new RuntimeException("Failed to save account", e);
    }
}
```

## 5. Protecting Multi-Step Operations (SQL Transactions)
A transfer in SimpleBank must debit one account **and** credit another. If the application crashes between those two SQL statements, you will end up with lost money. SQL transactions solve this.

By default, JDBC runs every statement in its own implicit transaction (auto-commit mode). To group multiple statements into one atomic unit of work, turn auto-commit off, and commit or roll back explicitly:

```java
public void transfer(String fromAccount, String toAccount, BigDecimal amount) {
    String debitSql  = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
    String creditSql = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
    
    try (Connection conn = DatabaseConnectionManager.getConnection()) {
        conn.setAutoCommit(false); // Start transaction
        try (PreparedStatement debit = conn.prepareStatement(debitSql);
             PreparedStatement credit = conn.prepareStatement(creditSql)) {
             
            debit.setBigDecimal(1, amount);
            debit.setString(2, fromAccount);
            debit.executeUpdate();
            
            credit.setBigDecimal(1, amount);
            credit.setString(2, toAccount);
            credit.executeUpdate();
            
            conn.commit(); // Both succeeded — make permanent
        } catch (SQLException e) {
            conn.rollback(); // Something failed — undo everything
            throw new RuntimeException("Transfer failed", e);
        }
    } catch (SQLException e) {
        throw new RuntimeException("Database connection failed", e);
    }
}
```

Key points:
- `setAutoCommit(false)` groups subsequent statements into a single transaction.
- `commit()` makes all changes permanent only when every step succeeds.
- `rollback()` undoes everything if any step fails, so the database never ends up in an inconsistent state.
- In your SimpleBank transfer, the transaction should cover the balance updates **and** the two transaction-history inserts (`TRANSFER_OUT` and `TRANSFER_IN`).

## 6. Enforcing Business Rules in the Schema (CHECK Constraints)
A `CHECK` constraint is a rule you add to a table column or the whole table definition. PostgreSQL will **reject any `INSERT` or `UPDATE` that violates the condition** — no matter what code path wrote it.

### Two-layer responsibility
A `CHECK` constraint and your Java validation are **not alternatives** — they serve different purposes:

| Layer | Job |
|---|---|
| **Java service** | Runs first. Produces user-friendly error messages (e.g., `"Insufficient funds. Current balance: 42.00"`). Prevents the unnecessary database round-trip. |
| **DB CHECK constraint** | Safety net. Catches any bug that bypasses the service: a second code path, a future storage backend, or a direct SQL script run by a developer. |

The key distinction: **a violated `CHECK` constraint surfaces as a raw `SQLException`, not a friendly message**. That is why Java must always validate first, and the database constraint is purely a backstop for correctness.

## 7. Configuration & Credentials
Never hardcode database credentials (URLs, usernames, passwords) directly in your Java code. If you commit them to GitHub, they are compromised.

The provided `DatabaseConnectionManager` is designed to read credentials from **OS environment variables** (via `System.getenv()`). Before running your app locally, export them in your terminal:

```bash
# Linux / macOS
export DB_URL=jdbc:postgresql://localhost:5432/simplebank
export DB_USER=your_username
export DB_PASS=your_password

# Then run your application from the same terminal session
```

Alternatively, configure these variables in your IDE's run configuration (IntelliJ: Run → Edit Configurations → Environment variables). Note: placing them in a `.env` file will **not** work automatically—`System.getenv()` reads only real OS environment variables, not `.env` files.

By mastering `PreparedStatement`, transactions, `CHECK` constraints, and the repository pattern, you ensure your application remains secure and consistent, and your core domain logic remains blissfully unaware of the underlying SQL engine.