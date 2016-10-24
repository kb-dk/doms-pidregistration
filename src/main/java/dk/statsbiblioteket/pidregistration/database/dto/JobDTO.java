package dk.statsbiblioteket.pidregistration.database.dto;

import dk.statsbiblioteket.pidregistration.Collection;

import java.util.Date;

/**
 */
public class JobDTO {
    public enum State {
        PENDING("pending"),
        DONE("done"),
        ERROR("error"),
        DELETED("deleted");

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
    private String collectionId;
    private State state;
    private Date created;
    private Date lastStateChange;

    public JobDTO() {
    }

    public JobDTO(String uuid, String collectionId, State state) {
        this.uuid = uuid;
        this.collectionId = collectionId;
        this.state = state;
        this.created = new Date();
        this.created = new Date();
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

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
        setLastStateChange(new Date());
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastStateChange() {
        return lastStateChange;
    }

    public void setLastStateChange(Date lastStateChange) {
        this.lastStateChange = lastStateChange;
    }
}
