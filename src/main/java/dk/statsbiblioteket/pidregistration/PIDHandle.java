package dk.statsbiblioteket.pidregistration;

/**
 * PID Handle bean
 */
public class PIDHandle {
    private String namespace;
    private String id;

    public PIDHandle(String namespace, String id) {
        this.namespace = namespace;
        this.id = id;
    }

    public String asString() {
        return namespace + "/" + id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PIDHandle pidHandle = (PIDHandle) o;

        if (!id.equals(pidHandle.id)) return false;
        if (!namespace.equals(pidHandle.namespace)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return asString();
    }
}
