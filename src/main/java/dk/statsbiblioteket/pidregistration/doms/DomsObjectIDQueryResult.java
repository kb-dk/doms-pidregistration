package dk.statsbiblioteket.pidregistration.doms;

import java.util.Date;
import java.util.List;

/**
 */
public class DOMSObjectIDQueryResult {
    private List<String> objectIds;
    private Date latestRead;

    public DOMSObjectIDQueryResult(List<String> objectIds, Date latestRead) {
        this.objectIds = objectIds;
        this.latestRead = latestRead;
    }

    public boolean isEmpty() {
        return objectIds.isEmpty();
    }

    public List<String> getObjectIds() {
        return objectIds;
    }

    public Date getLatestRead() {
        return latestRead;
    }
}
