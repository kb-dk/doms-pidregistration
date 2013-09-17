package dk.statsbiblioteket.pidregistration;

/**
 */
public class Collection {
    private String id;
    private String domsName;

    public Collection(String id, String domsName) {
        this.id = id;
        this.domsName = domsName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Collection that = (Collection) o;

        if (!domsName.equals(that.domsName)) return false;
        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + domsName.hashCode();
        return result;
    }

    public String getId() {
        return id;
    }

    public String getDomsName() {
        return domsName;
    }

    @Override
    public String toString() {
        return String.format("{'%s':'%s'}", id, domsName);
    }
}
