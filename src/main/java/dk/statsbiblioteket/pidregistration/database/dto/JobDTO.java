package dk.statsbiblioteket.pidregistration.database.dto;

import dk.statsbiblioteket.pidregistration.Collection;

import java.util.Date;

/**
 */
public class JobDTO {
    public enum State {
        PENDING("pending"),
        DONE("done"),
        ERROR("error");

        private String databaseStateName;

        private State(String databaseStateName) {
            this.databaseStateName = databaseStateName;
        }

        public String getDatabaseStateName() {
            return databaseStateName;
        }
    }

    private int id;
    private String uuid;
    private Collection collection;
    private State state;
    private Date lastStateChange;

    public JobDTO() {
    }

    public JobDTO(String uuid, Collection collection, State state) {
        this.uuid = uuid;
        this.collection = collection;
        this.state = state;
        this.lastStateChange = new Date();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Collection getCollection() {
        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
        setLastStateChange(new Date());
    }

    public Date getLastStateChange() {
        return lastStateChange;
    }

    public void setLastStateChange(Date lastStateChange) {
        this.lastStateChange = lastStateChange;
    }
}
