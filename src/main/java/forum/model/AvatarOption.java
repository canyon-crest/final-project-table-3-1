package forum.model;

/**
 * Simple ID + label option for avatar part selection.
 */
public class AvatarOption {

    private final long id;
    private final String code;
    private final String displayName;

    public AvatarOption(long id, String code, String displayName) {
        this.id = id;
        this.code = code;
        this.displayName = displayName;
    }

    public long getId() {
        return id;
    }

    public String getCode() {
        return code == null ? "" : code;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

