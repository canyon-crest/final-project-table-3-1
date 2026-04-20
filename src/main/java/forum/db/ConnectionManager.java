package forum.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Loads JDBC settings from a properties file and hands out {@link Connection}s.
 */
public final class ConnectionManager {

    private static String jdbcUrl;
    private static String dbUser;
    private static String dbPassword;

    private ConnectionManager() {
    }

    /**
     * Reads db.url, db.user, and db.password from the given file.
     *
     * @param file typically forum.properties next to the app / working directory
     * @throws IOException if the file cannot be read
     */
    public static void loadFromFile(File file) throws IOException {
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            p.load(in);
        }
        jdbcUrl = p.getProperty("db.url", "").trim();
        dbUser = p.getProperty("db.user", "").trim();
        dbPassword = p.getProperty("db.password", "");
        if (jdbcUrl.isEmpty() || dbUser.isEmpty()) {
            throw new IOException("db.url and db.user must be set in " + file.getName());
        }
    }

    /**
     * @return true if {@link #loadFromFile(File)} has been called successfully
     */
    public static boolean isConfigured() {
        return jdbcUrl != null && !jdbcUrl.isEmpty() && dbUser != null && !dbUser.isEmpty();
    }

    /**
     * Opens a new connection. Callers should use try-with-resources.
     *
     * @return a live JDBC connection
     * @throws SQLException if the driver or network fails
     */
    public static Connection getConnection() throws SQLException {
        if (!isConfigured()) {
            throw new SQLException("Database is not configured (missing forum.properties).");
        }
        return DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
    }
}
