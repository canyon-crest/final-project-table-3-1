package forum.service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import forum.db.CategoryRepository;
import forum.db.ThreadRepository;
import forum.db.UserRepository;
import forum.model.CategoryInfo;
import forum.model.ForumUser;
import forum.model.ThreadInfo;

/**
 * Coordinates repositories and session state (composition: has-a users + categories + session).
 */
public class ForumService {

    private final ForumSession session;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final ThreadRepository threads;

    /**
     * @param session    shared login state
     * @param users      user persistence
     * @param categories category persistence
     * @param threads thread persistence
     */
    public ForumService(ForumSession session, UserRepository users, CategoryRepository categories,
            ThreadRepository threads) {
        this.session = session;
        this.users = users;
        this.categories = categories;
        this.threads = threads;
    }

    /**
     * @return the live session (for UI labels, logout, etc.)
     */
    public ForumSession getSession() {
        return session;
    }

    /**
     * Attempts login and updates {@link ForumSession} on success.
     *
     * @param username plaintext username
     * @param password plaintext password
     * @return true if credentials matched
     * @throws SQLException on database errors
     */
    public boolean attemptLogin(String username, String password) throws SQLException {
        if (username == null || password == null || username.isBlank()) {
            return false;
        }
        Optional<ForumUser> row = users.findByUsername(username.trim());
        if (row.isEmpty()) {
            return false;
        }
        ForumUser u = row.get();
        if (!users.passwordMatches(password, u.getPasswordHash())) {
            return false;
        }
        session.setCurrentUser(u);
        return true;
    }

    /**
     * Creates an account if validation passes and the name is free.
     *
     * @param username plaintext username
     * @param password plaintext password
     * @return {@code true} if a row was inserted
     * @throws SQLException on database errors
     */
    public boolean register(String username, String password) throws SQLException {
        if (!ForumUser.isValidUsername(username) || password == null || password.isEmpty()) {
            return false;
        }
        if (users.findByUsername(username.trim()).isPresent()) {
            return false;
        }
        long id = users.insertUser(username.trim(), password);
        return id > 0;
    }

    /**
     * Loads categories and applies an in-memory ordering pass (selection sort by {@code sortOrder}).
     *
     * @return a new list sorted for display
     * @throws SQLException on database errors
     */
    public List<CategoryInfo> loadCategoriesSorted() throws SQLException {
        List<CategoryInfo> list = new ArrayList<>(categories.findAll());
        selectionSortBySortOrder(list);
        return list;
    }

    /**
     * Loads posts for the chosen category.
     *
     * @param categoryId category to view
     * @return posts in newest-first order
     * @throws SQLException on database errors
     */
    public List<ThreadInfo> loadPostsForCategory(long categoryId) throws SQLException {
        return threads.findByCategoryId(categoryId);
    }

    /**
     * Creates a new thread in the selected category.
     *
     * @param categoryId target category
     * @param title post title
     * @param content post body
     * @return true if inserted
     * @throws SQLException on database errors
     */
    public boolean createPost(long categoryId, String title, String content) throws SQLException {
        if (!session.isLoggedIn()) {
            return false;
        }
        if (title == null || title.isBlank() || title.length() > 150) {
            return false;
        }
        if (content == null || content.isBlank()) {
            return false;
        }
        long authorId = session.getCurrentUser().getId();
        long id = threads.insertThread(categoryId, authorId, title.trim(), content.trim());
        return id > 0;
    }

    /**
     * In-place selection sort — demonstrates explicit algorithmic steps for the project rubric.
     *
     * @param list categories to reorder
     */
    void selectionSortBySortOrder(List<CategoryInfo> list) {
        for (int i = 0; i < list.size(); i++) {
            int min = i;
            for (int j = i + 1; j < list.size(); j++) {
                if (list.get(j).getSortOrder() < list.get(min).getSortOrder()) {
                    min = j;
                }
            }
            if (min != i) {
                CategoryInfo tmp = list.get(i);
                list.set(i, list.get(min));
                list.set(min, tmp);
            }
        }
    }
}
