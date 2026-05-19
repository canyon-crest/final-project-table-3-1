package forum.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import forum.model.ThreadInfo;

/**
 * Stores thread/post rows.
 */
public class ThreadRepository extends RepositoryBase {

    private static final Calendar UTC_CAL = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    private static final String THREAD_SELECT_BASE = """
            SELECT t.threadID, t.title, t.content, t.dateCreated, u.username,
                   u.avatar_headpiece_id, u.avatar_clothing_id, u.avatar_accessory_id,
                   COALESCE(SUM(CASE WHEN tr.reaction = 1 THEN 1 ELSE 0 END), 0) AS like_count,
                   COALESCE(SUM(CASE WHEN tr.reaction = -1 THEN 1 ELSE 0 END), 0) AS dislike_count
            FROM threads t
            JOIN `user` u ON u.userID = t.authorID
            LEFT JOIN thread_reaction tr ON tr.threadID = t.threadID
            """;

    /**
     * Inserts a new thread in a category.
     *
     * @param categoryId selected category
     * @param authorId logged-in user id
     * @param title short post title
     * @param content post body
     * @return new thread id, or -1 if insert failed
     * @throws SQLException on database errors
     */
    public long insertThread(long categoryId, long authorId, String title, String content) throws SQLException {
        final String sql = "INSERT INTO threads (categoryID, authorID, title, content) VALUES (?, ?, ?, ?)";
        try (Connection c = openConnection();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, categoryId);
            ps.setLong(2, authorId);
            ps.setString(3, title);
            ps.setString(4, content);
            int n = ps.executeUpdate();
            if (n == 0) {
                return -1;
            }
            return readInsertedRowId(c, ps);
        }
    }

    /**
     * Loads threads for one category, newest first.
     *
     * @param categoryId target category id
     * @return thread summaries with title, content, and author username
     * @throws SQLException on database errors
     */
    public List<ThreadInfo> findByCategoryId(long categoryId) throws SQLException {
        final String sql = THREAD_SELECT_BASE + """
                WHERE t.categoryID = ?
                GROUP BY t.threadID, t.title, t.content, t.dateCreated, u.username,
                         u.avatar_headpiece_id, u.avatar_clothing_id, u.avatar_accessory_id
                ORDER BY t.dateCreated DESC, t.threadID DESC
                """;
        List<ThreadInfo> list = new ArrayList<>();
        try (Connection c = openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapThreadInfo(rs));
                }
            }
        }
        return list;
    }

    public Optional<ThreadInfo> findById(long threadId) throws SQLException {
        final String sql = THREAD_SELECT_BASE
                + " WHERE t.threadID = ?"
                + " GROUP BY t.threadID, t.title, t.content, t.dateCreated, u.username,"
                + " u.avatar_headpiece_id, u.avatar_clothing_id, u.avatar_accessory_id";
        try (Connection c = openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapThreadInfo(rs));
            }
        }
    }

    private ThreadInfo mapThreadInfo(ResultSet rs) throws SQLException {
        long id = rs.getLong("threadID");
        String title = rs.getString("title");
        String content = rs.getString("content");
        Timestamp created = rs.getTimestamp("dateCreated", UTC_CAL);
        String author = rs.getString("username");
        Long authorHeadpiece = (Long) rs.getObject("avatar_headpiece_id", Long.class);
        Long authorClothing = (Long) rs.getObject("avatar_clothing_id", Long.class);
        Long authorAccessory = (Long) rs.getObject("avatar_accessory_id", Long.class);
        int likes = rs.getInt("like_count");
        int dislikes = rs.getInt("dislike_count");
        return new ThreadInfo(id, title, content, author, created,
                authorHeadpiece, authorClothing, authorAccessory, likes, dislikes);
    }

    public long findAuthorId(long threadId) throws SQLException {
        final String sql = "SELECT authorID FROM threads WHERE threadID = ?";
        try (Connection c = openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1L;
    }

    /**
     * Toggles/upserts one user's reaction on a thread.
     * Same reaction twice removes the reaction.
     *
     * @param threadId thread id
     * @param userId reacting user id
     * @param reaction 1 for like, -1 for dislike
     * @return resulting reaction state (1 like, -1 dislike, 0 no reaction)
     * @throws SQLException on database errors
     */
    public int setReaction(long threadId, long userId, int reaction) throws SQLException {
        if (reaction != 1 && reaction != -1) {
            return 0;
        }
        final String selectSql = "SELECT reaction FROM thread_reaction WHERE threadID = ? AND userID = ?";
        try (Connection c = openConnection()) {
            Integer existing = null;
            try (PreparedStatement ps = c.prepareStatement(selectSql)) {
                ps.setLong(1, threadId);
                ps.setLong(2, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        existing = Integer.valueOf(rs.getInt(1));
                    }
                }
            }
            if (existing != null && existing.intValue() == reaction) {
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM thread_reaction WHERE threadID = ? AND userID = ?")) {
                    ps.setLong(1, threadId);
                    ps.setLong(2, userId);
                    ps.executeUpdate();
                    return 0;
                }
            }
            if (existing == null) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO thread_reaction (threadID, userID, reaction) VALUES (?, ?, ?)")) {
                    ps.setLong(1, threadId);
                    ps.setLong(2, userId);
                    ps.setInt(3, reaction);
                    ps.executeUpdate();
                    return reaction;
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE thread_reaction SET reaction = ?, dateUpdated = CURRENT_TIMESTAMP WHERE threadID = ? AND userID = ?")) {
                ps.setInt(1, reaction);
                ps.setLong(2, threadId);
                ps.setLong(3, userId);
                ps.executeUpdate();
                return reaction;
            }
        }
    }
}

