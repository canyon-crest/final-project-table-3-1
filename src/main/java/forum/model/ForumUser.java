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
    private final int xpTotal;
    private final int level;

    /**
     * @param id           userID from {@code user}
     * @param username     login name
     * @param passwordHash bcrypt hash from the database
     */
    public ForumUser(long id, String username, String passwordHash) {
        this(id, username, passwordHash, null, null, null, 0, 1);
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
        this(id, username, passwordHash, avatarHeadpieceId, avatarClothingId, avatarAccessoryId, 0, 1);
    }

    public ForumUser(long id, String username, String passwordHash,
            Long avatarHeadpieceId, Long avatarClothingId, Long avatarAccessoryId,
            int xpTotal, int level) {
        super(id);
        this.username = username;
        this.passwordHash = passwordHash;
        this.avatarHeadpieceId = avatarHeadpieceId;
        this.avatarClothingId = avatarClothingId;
        this.avatarAccessoryId = avatarAccessoryId;
        this.xpTotal = Math.max(0, xpTotal);
        this.level = Math.max(1, level);
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

    public int getXpTotal() {
        return xpTotal;
    }

    public int getLevel() {
        return level;
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
