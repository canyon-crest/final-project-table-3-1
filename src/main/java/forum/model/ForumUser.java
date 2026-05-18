package forum.model;

/**
 * Logged-in account; {@link #getPasswordHash()} is only for verifying login, never for display.
 */
public class ForumUser extends StorableEntity {

    private final String username;
    private final String passwordHash;
    private final Long avatarHeadpieceId;
    private final Long avatarClothingId;
    private final Long avatarAccessoryId;

    /**
     * @param id           userID from {@code user}
     * @param username     login name
     * @param passwordHash bcrypt hash from the database
     */
    public ForumUser(long id, String username, String passwordHash) {
        this(id, username, passwordHash, null, null, null);
    }

    /**
     * @param id userID from {@code user}
     * @param username login name
     * @param passwordHash bcrypt hash from the database
     * @param avatarHeadpieceId selected headpiece row id (nullable)
     * @param avatarClothingId selected clothing row id (nullable)
     * @param avatarAccessoryId selected accessory row id (nullable)
     */
    public ForumUser(long id, String username, String passwordHash,
            Long avatarHeadpieceId, Long avatarClothingId, Long avatarAccessoryId) {
        super(id);
        this.username = username;
        this.passwordHash = passwordHash;
        this.avatarHeadpieceId = avatarHeadpieceId;
        this.avatarClothingId = avatarClothingId;
        this.avatarAccessoryId = avatarAccessoryId;
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

    public Long getAvatarHeadpieceId() {
        return avatarHeadpieceId;
    }

    public Long getAvatarClothingId() {
        return avatarClothingId;
    }

    public Long getAvatarAccessoryId() {
        return avatarAccessoryId;
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
