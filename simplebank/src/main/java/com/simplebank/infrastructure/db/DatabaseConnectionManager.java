package com.simplebank.infrastructure.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// TODO: You may change the package and class name as you see fit. 
// Remember to remove the "TODO" comment after making your changes.
public class DatabaseConnectionManager {

    // Read configuration from environment variables with sensible defaults for local development
    private static final String URL = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/simplebank");
    private static final String USER = System.getenv().getOrDefault("DB_USER", "postgres");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASS", "postgres");

    // Private constructor to prevent instantiation (Utility class)
    private DatabaseConnectionManager() {}

    /**
     * Establishes and returns a connection to the database.
     * The caller is responsible for closing the connection (e.g., using try-with-resources).
     *
     * @return Connection to the PostgreSQL database
     * @throws SQLException if a database access error occurs or the url is null
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
