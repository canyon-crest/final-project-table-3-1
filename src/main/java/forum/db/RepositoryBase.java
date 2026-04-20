package forum.db;

import java.sql.Connection;
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
}
