package forum.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;

import forum.model.AvatarOption;
import forum.model.ForumUser;

/**
 * Loads and stores rows in the {@code user} table (back-ticked in SQL because {@code user} is reserved).
 */
public class UserRepository extends RepositoryBase {

    /**
     * Seeds avatar part rows when tables exist but are empty (so the profile dropdowns work without a manual SQL run).
     */
    private static final String SEED_HEADPIECES = """
            INSERT INTO avatar_headpiece (code, display_name, asset_key, unlock_level, is_active) VALUES
              ('none', 'None', 'headpiece:none', 0, 1),
              ('cap_red', 'Red Cap', 'headpiece:cap_red', 0, 1),
              ('beanie_gray', 'Gray Beanie', 'headpiece:beanie_gray', 1, 1),
              ('visor_white', 'White Visor', 'headpiece:visor_white', 2, 1),
              ('headband_raven', 'Raven Headband', 'headpiece:headband_raven', 3, 1),
              ('crown_gold', 'Gold Crown', 'headpiece:crown_gold', 6, 1),
              ('halo', 'Halo', 'headpiece:halo', 5, 1)
            ON DUPLICATE KEY UPDATE display_name = VALUES(display_name)
            """;

    private static final String SEED_CLOTHING = """
            INSERT INTO avatar_clothing (code, display_name, asset_key, unlock_level, is_active) VALUES
              ('default_tee', 'Default Tee', 'clothing:default_tee', 0, 1),
              ('polo_navy', 'Navy Polo', 'clothing:polo_navy', 0, 1),
              ('raven_tee', 'Raven Tee', 'clothing:raven_tee', 1, 1),
              ('hoodie_black', 'Black Hoodie', 'clothing:hoodie_black', 2, 1),
              ('sweater_cream', 'Cream Sweater', 'clothing:sweater_cream', 2, 1),
              ('dress_shirt', 'Dress Shirt', 'clothing:dress_shirt', 3, 1),
              ('letterman', 'Letterman Jacket', 'clothing:letterman', 4, 1),
              ('jacket_red', 'Red Jacket', 'clothing:jacket_red', 5, 1),
              ('tracksuit', 'Tracksuit', 'clothing:tracksuit', 3, 1),
              ('formal_gown', 'Formal Gown', 'clothing:formal_gown', 6, 1),
              ('team_jersey', 'Team Jersey', 'clothing:team_jersey', 4, 1)
            ON DUPLICATE KEY UPDATE display_name = VALUES(display_name)
            """;

    private static final String SEED_ACCESSORIES = """
            INSERT INTO avatar_accessory (code, display_name, asset_key, unlock_level, is_active) VALUES
              ('none', 'None', 'accessory:none', 0, 1),
              ('glasses', 'Glasses', 'accessory:glasses', 1, 1),
              ('scarf_striped', 'Striped Scarf', 'accessory:scarf_striped', 2, 1),
              ('watch_silver', 'Silver Watch', 'accessory:watch_silver', 2, 1),
              ('bow_tie', 'Bow Tie', 'accessory:bow_tie', 3, 1),
              ('star_pin', 'Star Pin', 'accessory:star_pin', 3, 1),
              ('backpack', 'Backpack', 'accessory:backpack', 4, 1),
              ('earbuds', 'Earbuds', 'accessory:earbuds', 1, 1)
            ON DUPLICATE KEY UPDATE display_name = VALUES(display_name)
            """;

    private boolean avatarCatalogEnsured;

    /**
     * Looks up a user by login name.
     *
     * @param username case-sensitive match to the {@code username} column
     * @return the user row if found
     * @throws SQLException on database errors
     */
    public Optional<ForumUser> findByUsername(String username) throws SQLException {
        final String sql = "SELECT userID, username, password, avatar_headpiece_id, avatar_clothing_id, avatar_accessory_id "
                + "FROM `user` WHERE username = ?";
        try (Connection c = openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                long id = rs.getLong("userID");
                String name = rs.getString("username");
                String hash = rs.getString("password");
                Long headpiece = (Long) rs.getObject("avatar_headpiece_id", Long.class);
                Long clothing = (Long) rs.getObject("avatar_clothing_id", Long.class);
                Long accessory = (Long) rs.getObject("avatar_accessory_id", Long.class);
                return Optional.of(new ForumUser(id, name, hash, headpiece, clothing, accessory));
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

    /**
     * Updates the username for an existing account.
     *
     * @param userId current user id
     * @param username new username to store
     * @return true if exactly one row was updated
     * @throws SQLException on database errors
     */
    public boolean updateUsername(long userId, String username) throws SQLException {
        final String sql = "UPDATE `user` SET username = ? WHERE userID = ?";
        try (Connection c = openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setLong(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateAvatarSelection(long userId, Long headpieceId, Long clothingId, Long accessoryId) throws SQLException {
        final String sql = "UPDATE `user` "
                + "SET avatar_headpiece_id = ?, avatar_clothing_id = ?, avatar_accessory_id = ? "
                + "WHERE userID = ?";
        try (Connection c = openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (headpieceId == null) {
                ps.setNull(1, java.sql.Types.INTEGER);
            } else {
                ps.setLong(1, headpieceId.longValue());
            }
            if (clothingId == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
            } else {
                ps.setLong(2, clothingId.longValue());
            }
            if (accessoryId == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setLong(3, accessoryId.longValue());
            }
            ps.setLong(4, userId);
            return ps.executeUpdate() == 1;
        }
    }

    public List<AvatarOption> findAllHeadpieces() throws SQLException {
        return findAllAvatarOptions("avatar_headpiece", "headpiece_id");
    }

    public List<AvatarOption> findAllClothing() throws SQLException {
        return findAllAvatarOptions("avatar_clothing", "clothing_id");
    }

    public List<AvatarOption> findAllAccessories() throws SQLException {
        return findAllAvatarOptions("avatar_accessory", "accessory_id");
    }

    private List<AvatarOption> findAllAvatarOptions(String table, String idCol) throws SQLException {
        ensureAvatarCatalogIfNeeded();
        try (Connection c = openConnection()) {
            boolean hasIsActive = hasAnyColumn(c, table, "is_active", "isActive");
            boolean hasUnlockLevel = hasAnyColumn(c, table, "unlock_level", "unlockLevel");
            String resolvedIdCol = resolveFirstExistingColumn(c, table, idCol, "id");
            String labelCol = resolveFirstExistingColumn(c, table,
                    "display_name", "displayName", "name", "label", "code", "asset_key", "assetKey");
            if (resolvedIdCol == null || labelCol == null) {
                return new ArrayList<>();
            }
            boolean hasCode = hasColumn(c, table, "code");
            String codeSelect = hasCode ? ", code" : "";
            StringBuilder sql = new StringBuilder("SELECT " + resolvedIdCol + " AS id" + codeSelect + ", " + labelCol + " AS label FROM " + table);
            if (hasIsActive) {
                String activeCol = resolveFirstExistingColumn(c, table, "is_active", "isActive");
                sql.append(" WHERE ").append(activeCol).append(" = 1");
            }
            if (hasUnlockLevel) {
                String unlockCol = resolveFirstExistingColumn(c, table, "unlock_level", "unlockLevel");
                sql.append(" ORDER BY ").append(unlockCol).append(" ASC, label ASC");
            } else {
                sql.append(" ORDER BY label ASC");
            }
            try (PreparedStatement ps = c.prepareStatement(sql.toString()); ResultSet rs = ps.executeQuery()) {
                List<AvatarOption> out = new ArrayList<>();
                while (rs.next()) {
                    String code = hasCode ? rs.getString("code") : "";
                    out.add(new AvatarOption(rs.getLong("id"), code, rs.getString("label")));
                }
                return out;
            }
        }
    }

    // ========================================================================
    // AP CSA Note: The following methods are helper functions for dynamic database columns.
    // They are completely beyond the AP CSA subset and are only used here
    // to handle database migrations automatically without crashing the app.
    // ========================================================================

    private static boolean hasColumn(Connection c, String table, String column) throws SQLException {
        DatabaseMetaData meta = c.getMetaData();
        try (ResultSet rs = meta.getColumns(c.getCatalog(), null, table, column)) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = meta.getColumns(c.getCatalog(), null, table.toLowerCase(), column.toLowerCase())) {
            return rs.next();
        }
    }

    private static String resolveFirstExistingColumn(Connection c, String table, String... candidates)
            throws SQLException {
        for (String candidate : candidates) {
            if (hasColumn(c, table, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean hasAnyColumn(Connection c, String table, String... candidates) throws SQLException {
        return resolveFirstExistingColumn(c, table, candidates) != null;
    }

    /**
     * If avatar catalog tables are empty, inserts the same seed rows as {@code db_schema.sql} so dropdowns populate.
     */
    private void ensureAvatarCatalogIfNeeded() throws SQLException {
        synchronized (this) {
            if (avatarCatalogEnsured) {
                return;
            }
            try (Connection c = openConnection()) {
                if (hasFullActiveCatalog(c)) {
                    avatarCatalogEnsured = true;
                    return;
                }
                if (canSeedAvatarTable(c, "avatar_headpiece")
                        && canSeedAvatarTable(c, "avatar_clothing")
                        && canSeedAvatarTable(c, "avatar_accessory")) {
                    try (Statement st = c.createStatement()) {
                        st.executeUpdate(SEED_HEADPIECES);
                        st.executeUpdate(SEED_CLOTHING);
                        st.executeUpdate(SEED_ACCESSORIES);
                    }
                }
                avatarCatalogEnsured = true;
            }
        }
    }

    private static boolean canSeedAvatarTable(Connection c, String table) throws SQLException {
        return hasColumn(c, table, "code")
                && hasAnyColumn(c, table, "display_name", "displayName")
                && hasAnyColumn(c, table, "asset_key", "assetKey")
                && hasAnyColumn(c, table, "unlock_level", "unlockLevel")
                && hasAnyColumn(c, table, "is_active", "isActive");
    }

    private static boolean hasFullActiveCatalog(Connection c) throws SQLException {
        return countActiveRows(c, "avatar_headpiece") > 0
                && countActiveRows(c, "avatar_clothing") > 0
                && countActiveRows(c, "avatar_accessory") > 0;
    }

    private static long countActiveRows(Connection c, String table) throws SQLException {
        String activeCol = resolveFirstExistingColumn(c, table, "is_active", "isActive");
        String sql;
        if (activeCol != null) {
            sql = "SELECT COUNT(*) FROM " + table + " WHERE " + activeCol + " = 1";
        } else {
            sql = "SELECT COUNT(*) FROM " + table;
        }
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        }
    }
}
