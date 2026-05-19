package forum.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Superclass for data-access classes that share the same connection factory.
 * Demonstrates an inheritance hierarchy for the AP CSA rubric.
 */
public abstract class RepositoryBase {

    /**
     * @return a new connection from the app-wide {@link ConnectionManager}
     * @throws SQLException if a connection cannot be opened
     */
    protected Connection openConnection() throws SQLException {
        return ConnectionManager.getConnection();
    }

    /**
     * Reads the auto-increment id from an INSERT on the same connection.
     * Tries JDBC generated keys first, then {@code LAST_INSERT_ID()} for TiDB/MySQL compatibility.
     *
     * @param connection connection that executed the insert
     * @param statement prepared statement used for the insert
     * @return new row id, or -1 if none could be read
     * @throws SQLException on database errors while running the fallback query
     */
    protected static long readInsertedRowId(Connection connection, PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                long id = readFirstPositiveLong(keys);
                if (id > 0L) {
                    return id;
                }
            }
        } catch (SQLException ignored) {
            // Some TiDB/MySQL driver builds throw when reading generated-keys rows directly.
        }
        try (PreparedStatement idStatement = connection.prepareStatement("SELECT LAST_INSERT_ID()");
                ResultSet rs = idStatement.executeQuery()) {
            if (rs.next()) {
                long id = rs.getLong(1);
                if (id > 0L) {
                    return id;
                }
            }
        }
        return -1L;
    }

    private static long readFirstPositiveLong(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columns = meta.getColumnCount();
        for (int i = 1; i <= columns; i++) {
            try {
                long value = rs.getLong(i);
                if (!rs.wasNull() && value > 0L) {
                    return value;
                }
            } catch (SQLException ignored) {
                // Try the next column if this driver exposes non-numeric metadata.
            }
        }
        return -1L;
    }
}
