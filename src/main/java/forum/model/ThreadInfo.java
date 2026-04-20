package forum.model;

/**
 * One row from {@code threads} with author display name.
 */
public class ThreadInfo extends StorableEntity {

    private final String title;
    private final String content;
    private final String authorUsername;

    /**
     * @param id threadID
     * @param title thread title
     * @param content thread body text
     * @param authorUsername post author's username
     */
    public ThreadInfo(long id, String title, String content, String authorUsername) {
        super(id);
        this.title = title == null ? "" : title;
        this.content = content == null ? "" : content;
        this.authorUsername = authorUsername == null ? "" : authorUsername;
    }

    /**
     * @return thread title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return thread body content
     */
    public String getContent() {
        return content;
    }

    /**
     * @return author username
     */
    public String getAuthorUsername() {
        return authorUsername;
    }

    @Override
    public String toString() {
        return title + " - by " + authorUsername;
    }
}

