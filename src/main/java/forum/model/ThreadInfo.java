package forum.model;

import java.sql.Timestamp;

/**
 * One row from {@code threads} with author display name.
 */
public class ThreadInfo extends StorableEntity {

    private final String title;
    private final String content;
    private final String authorUsername;
    private final Timestamp dateCreated;
    private final Long authorAvatarHeadpieceId;
    private final Long authorAvatarClothingId;
    private final Long authorAvatarAccessoryId;
    private final int likeCount;
    private final int dislikeCount;

    /**
     * @param id threadID
     * @param title thread title
     * @param content thread body text
     * @param authorUsername post author's username
     * @param dateCreated post creation time
     * @param authorAvatarHeadpieceId selected headpiece id for author (nullable)
     * @param authorAvatarClothingId selected clothing id for author (nullable)
     * @param authorAvatarAccessoryId selected accessory id for author (nullable)
     */
    public ThreadInfo(long id, String title, String content, String authorUsername, Timestamp dateCreated,
            Long authorAvatarHeadpieceId, Long authorAvatarClothingId, Long authorAvatarAccessoryId) {
        this(id, title, content, authorUsername, dateCreated,
                authorAvatarHeadpieceId, authorAvatarClothingId, authorAvatarAccessoryId, 0, 0);
    }

    public ThreadInfo(long id, String title, String content, String authorUsername, Timestamp dateCreated,
            Long authorAvatarHeadpieceId, Long authorAvatarClothingId, Long authorAvatarAccessoryId,
            int likeCount, int dislikeCount) {
        super(id);
        this.title = title == null ? "" : title;
        this.content = content == null ? "" : content;
        this.authorUsername = authorUsername == null ? "" : authorUsername;
        this.dateCreated = dateCreated;
        this.authorAvatarHeadpieceId = authorAvatarHeadpieceId;
        this.authorAvatarClothingId = authorAvatarClothingId;
        this.authorAvatarAccessoryId = authorAvatarAccessoryId;
        this.likeCount = Math.max(0, likeCount);
        this.dislikeCount = Math.max(0, dislikeCount);
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

    /**
     * @return creation timestamp
     */
    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public Long getAuthorAvatarHeadpieceId() {
        return authorAvatarHeadpieceId;
    }

    public Long getAuthorAvatarClothingId() {
        return authorAvatarClothingId;
    }

    public Long getAuthorAvatarAccessoryId() {
        return authorAvatarAccessoryId;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public int getDislikeCount() {
        return dislikeCount;
    }

    @Override
    public String toString() {
        return title + " - by " + authorUsername;
    }
}

