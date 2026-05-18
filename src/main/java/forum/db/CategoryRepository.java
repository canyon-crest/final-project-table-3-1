package forum.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import forum.model.CategoryInfo;

/**
 * Reads forum categories for the home screen and navigation.
 */
public class CategoryRepository extends RepositoryBase {

    /**
     * Loads every category row ordered for display.
     *
     * @return categories as defined in the database (caller may sort further)
     * @throws SQLException on database errors
     */
    public List<CategoryInfo> findAll() throws SQLException {
        final String sql = "SELECT categoryID, name, description, sortOrder FROM category ORDER BY sortOrder, name";
        List<CategoryInfo> list = new ArrayList<>();
        try (Connection c = openConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong("categoryID");
                String name = rs.getString("name");
                String desc = rs.getString("description");
                int sort = rs.getInt("sortOrder");
                list.add(new CategoryInfo(id, name, desc, sort));
            }
        }
        return list;
    }

}
