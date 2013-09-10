package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.Collection;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidCredentialsException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.MethodFailedException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.RecordDescription;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class DOMSObjectIDQueryer {
    private Date fromInclusive;
    private DOMSClient domsClient;
    private Map<Collection, Long> collectionTimestamps = new HashMap<Collection, Long>();

    public DOMSObjectIDQueryer(DOMSClient domsClient, Date fromInclusive) {
        this.domsClient = domsClient;
        this.fromInclusive = fromInclusive;
    }

    public List<String> findNextIn(Collection collection) {
        try {
            List<String> result = new ArrayList<String>();
            if (!collectionTimestamps.containsKey(collection)) {
                collectionTimestamps.put(collection, fromInclusive.getTime() == 0 ? 0 : fromInclusive.getTime() - 1);
            }

            long sinceExclusive = collectionTimestamps.get(collection);
            List<RecordDescription> recordDescriptions = domsClient.getIDsModified(sinceExclusive, collection);
            for (RecordDescription recordDescription : recordDescriptions) {
                collectionTimestamps.put(collection, recordDescription.getDate());
                result.add(recordDescription.getPid());
            }
            return result;
        } catch (InvalidCredentialsException e) {
            throw new BackendInvalidCredsException(
                    "Invalid Credentials Supplied", e);
        } catch (MethodFailedException e) {
            throw new BackendMethodFailedException(
                    "Server error: " + e.getMessage(), e);
        }
    }
}
