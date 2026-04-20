package forum.model;

/**
 * One row from {@code category}.
 */
public class CategoryInfo extends StorableEntity {

    private final String name;
    private final String description;
    private final int sortOrder;

    /**
     * @param id          categoryID
     * @param name        display title
     * @param description optional blurb (may be null from JDBC)
     * @param sortOrder   lower values appear first
     */
    public CategoryInfo(long id, String name, String description, int sortOrder) {
        super(id);
        this.name = name;
        this.description = description == null ? "" : description;
        this.sortOrder = sortOrder;
    }

    /**
     * @return category title
     */
    public String getName() {
        return name;
    }

    /**
     * @return short description for tooltips or subtitles
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return sort key from the database
     */
    public int getSortOrder() {
        return sortOrder;
    }

    @Override
    public String toString() {
        return name;
    }
}
