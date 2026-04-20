package forum.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;

import forum.model.ForumUser;

/**
 * Loads and stores rows in the {@code user} table (back-ticked in SQL because {@code user} is reserved).
 */
public class UserRepository extends RepositoryBase {

    /**
     * Looks up a user by login name.
     *
     * @param username case-sensitive match to the {@code username} column
     * @return the user row if found
     * @throws SQLException on database errors
     */
    public Optional<ForumUser> findByUsername(String username) throws SQLException {
        final String sql = "SELECT userID, username, password FROM `user` WHERE username = ?";
        try (Connection c = openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                long id = rs.getLong("userID");
                String name = rs.getString("username");
                String hash = rs.getString("password");
                return Optional.of(new ForumUser(id, name, hash));
            }
        }
    }

    /**
     * Inserts a new account with a bcrypt hash of the password.
     *
     * @param username desired username
     * @param plainPassword password before hashing; never stored verbatim
     * @return the new primary key, or -1 if the insert failed (e.g. duplicate username)
     * @throws SQLException on database errors
     */
    public long insertUser(String username, String plainPassword) throws SQLException {
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        final String sql = "INSERT INTO `user` (username, password) VALUES (?, ?)";
        try (Connection c = openConnection(); PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            int n = ps.executeUpdate();
            if (n == 0) {
                return -1;
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        return -1;
    }

    /**
     * Checks a plaintext password against the stored bcrypt hash.
     *
     * @param plainPassword user input at login
     * @param storedHash    value from the database
     * @return true if the password matches
     */
    public boolean passwordMatches(String plainPassword, String storedHash) {
        return BCrypt.checkpw(plainPassword, storedHash);
    }
}
