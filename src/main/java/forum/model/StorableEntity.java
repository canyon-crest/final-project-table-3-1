package forum.model;

/**
 * Superclass for rows that have a numeric primary key in TiDB.
 */
public class StorableEntity {

    private long id;

    /**
     * @param id primary key; use {@code 0} before insert if unknown
     */
    public StorableEntity(long id) {
        this.id = id;
    }

    /**
     * @return database primary key
     */
    public long getId() {
        return id;
    }

    /**
     * @param id replaces the identifier (for example after an insert returns generated keys)
     */
    protected void setId(long id) {
        this.id = id;
    }
}
