package forum.service;

import forum.model.ForumUser;

/**
 * Holds the currently logged-in user for this app instance.
 */
public class ForumSession {

    private ForumUser currentUser;

    /**
     * @param user account after a successful login, or {@code null} to log out
     */
    public void setCurrentUser(ForumUser user) {
        this.currentUser = user;
    }

    /**
     * Clears login state.
     */
    public void logout() {
        this.currentUser = null;
    }

    /**
     * @return the active user, or {@code null} if anonymous
     */
    public ForumUser getCurrentUser() {
        return currentUser;
    }

    /**
     * @return true if {@link #getCurrentUser()} is non-null
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
