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
import java.util.TimeZone;

import forum.model.CommentInfo;

/**
 * Stores comment rows on threads.
 */
public class CommentRepository extends RepositoryBase {

    private static final Calendar UTC_CAL = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    /**
     * Loads comments for one thread, oldest first.
     *
     * @param threadId thread to load
     * @return comments with author usernames
     * @throws SQLException on database errors
     */
    public List<CommentInfo> findByThreadId(long threadId) throws SQLException {
        final String sql = """
                SELECT c.commentID, c.parentCommentID, c.content, c.dateCreated, u.username,
                       u.avatar_headpiece_id, u.avatar_clothing_id, u.avatar_accessory_id
                FROM comments c
                JOIN `user` u ON u.userID = c.authorID
                WHERE c.threadID = ?
                ORDER BY c.dateCreated ASC, c.commentID ASC
                """;
        List<CommentInfo> list = new ArrayList<>();
        try (Connection c = openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, threadId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("commentID");
                    long parentRaw = rs.getLong("parentCommentID");
                    Long parent = rs.wasNull() ? null : parentRaw;
                    String body = rs.getString("content");
                    Timestamp created = rs.getTimestamp("dateCreated", UTC_CAL);
                    String author = rs.getString("username");
                    Long authorHeadpiece = (Long) rs.getObject("avatar_headpiece_id", Long.class);
                    Long authorClothing = (Long) rs.getObject("avatar_clothing_id", Long.class);
                    Long authorAccessory = (Long) rs.getObject("avatar_accessory_id", Long.class);
                    list.add(new CommentInfo(id, parent, body, author, created,
                            authorHeadpiece, authorClothing, authorAccessory));
                }
            }
        }
        return list;
    }

    /**
     * Inserts a comment on a thread.
     *
     * @param threadId        target thread
     * @param authorId        logged-in user id
     * @param parentCommentId parent comment for replies, or {@code null} for top-level
     * @param content         comment body
     * @return new comment id, or -1 if insert failed
     * @throws SQLException on database errors
     */
    public long insertComment(long threadId, long authorId, Long parentCommentId, String content)
            throws SQLException {
        final String sql = "INSERT INTO comments (threadID, authorID, parentCommentID, content) VALUES (?, ?, ?, ?)";
        try (Connection c = openConnection();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, threadId);
            ps.setLong(2, authorId);
            if (parentCommentId == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setLong(3, parentCommentId);
            }
            ps.setString(4, content);
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
     * Updates the body content of an existing comment.
     *
     * @param commentId target comment id
     * @param content new body text
     * @return true if one or more rows were updated
     * @throws SQLException on database errors
     */
    public boolean updateCommentContent(long commentId, String content) throws SQLException {
        final String sql = "UPDATE comments SET content = ? WHERE commentID = ?";
        try (Connection c = openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, content);
            ps.setLong(2, commentId);
            return ps.executeUpdate() > 0;
        }
    }
}
