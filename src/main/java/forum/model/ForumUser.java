package forum.model;

/**
 * Logged-in account; {@link #getPasswordHash()} is only for verifying login, never for display.
 */
public class ForumUser extends StorableEntity {

    private final String username;
    private final String passwordHash;

    /**
     * @param id           userID from {@code user}
     * @param username     login name
     * @param passwordHash bcrypt hash from the database
     */
    public ForumUser(long id, String username, String passwordHash) {
        super(id);
        this.username = username;
        this.passwordHash = passwordHash;
    }

    /**
     * @return login name
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return bcrypt hash; do not show in the UI
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Simple rule used before hitting the database.
     *
     * @param candidate proposed username
     * @return true if non-blank and in range for VARCHAR(50)
     */
    public static boolean isValidUsername(String candidate) {
        if (candidate == null) {
            return false;
        }
        String trimmed = candidate.trim();
        return !trimmed.isEmpty() && trimmed.length() <= 50;
    }
}
