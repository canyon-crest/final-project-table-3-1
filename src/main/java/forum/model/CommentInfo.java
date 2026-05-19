package forum.model;

import java.sql.Timestamp;

/**
 * One row from {@code comments} with author display name.
 */
public class CommentInfo extends StorableEntity {

    private final Long parentCommentId;
    private final String content;
    private final String authorUsername;
    private final Timestamp dateCreated;
    private final Long authorAvatarHeadpieceId;
    private final Long authorAvatarClothingId;
    private final Long authorAvatarAccessoryId;
    private final int likeCount;
    private final int dislikeCount;

    /**
     * @param id              commentID
     * @param parentCommentId parent comment when this is a reply; {@code null} for top-level
     * @param content         comment body
     * @param authorUsername  author username
     * @param dateCreated     creation time
     * @param authorAvatarHeadpieceId selected headpiece id for the author (nullable)
     * @param authorAvatarClothingId selected clothing id for the author (nullable)
     * @param authorAvatarAccessoryId selected accessory id for the author (nullable)
     */
    public CommentInfo(long id, Long parentCommentId, String content, String authorUsername,
            Timestamp dateCreated, Long authorAvatarHeadpieceId, Long authorAvatarClothingId, Long authorAvatarAccessoryId) {
        this(id, parentCommentId, content, authorUsername, dateCreated,
                authorAvatarHeadpieceId, authorAvatarClothingId, authorAvatarAccessoryId, 0, 0);
    }

    public CommentInfo(long id, Long parentCommentId, String content, String authorUsername,
            Timestamp dateCreated, Long authorAvatarHeadpieceId, Long authorAvatarClothingId,
            Long authorAvatarAccessoryId, int likeCount, int dislikeCount) {
        super(id);
        this.parentCommentId = parentCommentId;
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
     * @return parent comment id for replies, or {@code null} for top-level
     */
    public Long getParentCommentId() {
        return parentCommentId;
    }

    /**
     * @return comment body
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
}
