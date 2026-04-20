package forum.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import forum.model.ThreadInfo;

/**
 * Stores thread/post rows.
 */
public class ThreadRepository extends RepositoryBase {

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
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        return -1;
    }

    /**
     * Loads threads for one category, newest first.
     *
     * @param categoryId target category id
     * @return thread summaries with title, content, and author username
     * @throws SQLException on database errors
     */
    public List<ThreadInfo> findByCategoryId(long categoryId) throws SQLException {
        final String sql = """
                SELECT t.threadID, t.title, t.content, u.username
                FROM threads t
                JOIN `user` u ON u.userID = t.authorID
                WHERE t.categoryID = ?
                ORDER BY t.dateCreated DESC, t.threadID DESC
                """;
        List<ThreadInfo> list = new ArrayList<>();
        try (Connection c = openConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("threadID");
                    String title = rs.getString("title");
                    String content = rs.getString("content");
                    String author = rs.getString("username");
                    list.add(new ThreadInfo(id, title, content, author));
                }
            }
        }
        return list;
    }
}

