package forum.model;

/**
 * Simple ID + label option for avatar part selection.
 */
public class AvatarOption {

    private final long id;
    private final String code;
    private final String displayName;
    private final int unlockLevel;

    public AvatarOption(long id, String code, String displayName) {
        this(id, code, displayName, 0);
    }

    public AvatarOption(long id, String code, String displayName, int unlockLevel) {
        this.id = id;
        this.code = code;
        this.displayName = displayName;
        this.unlockLevel = Math.max(0, unlockLevel);
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

    public int getUnlockLevel() {
        return unlockLevel;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

