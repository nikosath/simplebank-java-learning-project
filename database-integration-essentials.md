# Database Integration Essentials

This guide bridges the gap between relational database theory and your Java code for Version 2. To keep your focus on software architecture and raw SQL, we are intentionally avoiding massive ORM frameworks (like Hibernate or Spring Data). 

Instead, you will learn universal data access patterns and how to write secure queries.

---

- [Database Integration Essentials](#database-integration-essentials)
  - [1. The Repository Design Pattern](#1-the-repository-design-pattern)
  - [2. Bypassing the Boilerplate (The Scaffold)](#2-bypassing-the-boilerplate-the-scaffold)
  - [3. Defeating SQL Injection (Prepared Statements)](#3-defeating-sql-injection-prepared-statements)
  - [4. Inserting and Updating Data](#4-inserting-and-updating-data)
  - [5. Configuration \& Credentials](#5-configuration--credentials)

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

## 5. Configuration & Credentials
Never hardcode database credentials (URLs, usernames, passwords) directly in your Java code. If you commit them to GitHub, they are compromised.

The provided `DatabaseConnectionManager` is designed to read credentials from environment variables. When running your app locally, configure your environment variables:

```bash
DB_URL=jdbc:postgresql://localhost:5432/simplebank
DB_USER=your_username
DB_PASS=your_password
```

By mastering `PreparedStatement` and the repository pattern, you ensure your application remains secure, and your core domain logic remains blissfully unaware of the underlying SQL engine.