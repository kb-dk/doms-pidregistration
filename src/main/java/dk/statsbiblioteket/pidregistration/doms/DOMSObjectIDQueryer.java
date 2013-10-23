package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.Collection;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidCredentialsException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.MethodFailedException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.RecordDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class DOMSObjectIDQueryer {
    private static final Logger log = LoggerFactory.getLogger(DOMSObjectIDQueryer.class);

    private Date fromInclusive;
    private DOMSClient domsClient;
    private Map<Collection, Long> collectionTimestamps = new HashMap<Collection, Long>();

    public DOMSObjectIDQueryer(DOMSClient domsClient, Date fromInclusive) {
        this.domsClient = domsClient;
        this.fromInclusive = fromInclusive;
    }

    public List<String> findNextIn(Collection collection) {
        try {
            log.debug("Querying DOMS for objects in collection {} modified since {}", collection, fromInclusive);
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
            log.debug("Got {} objects in collection {}", result.size(), collection);
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
