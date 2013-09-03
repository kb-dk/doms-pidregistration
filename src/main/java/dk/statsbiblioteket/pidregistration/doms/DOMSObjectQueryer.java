package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.Collection;
import dk.statsbiblioteket.pidregistration.UnknownCollectionException;
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
public class DOMSObjectQueryer {

    private static final int MAX_DOMS_RESULT_SIZE = 10000;

    private Date fromInclusive;
    private DOMSClient domsClient;
    private Map<Collection, Long> collectionTimestamps = new HashMap<Collection, Long>();

    public DOMSObjectQueryer(DOMSClient domsClient, Date fromInclusive) {
        this.domsClient = domsClient;
        this.fromInclusive = fromInclusive;
    }

    public List<String> findNextIn(Collection collection) {
        try {
            List<String> result = new ArrayList<String>();
            if (!collectionTimestamps.containsKey(collection)) {
                collectionTimestamps.put(collection, fromInclusive.getTime() == 0 ? 0 : fromInclusive.getTime() - 1);
            }

            long lastTimestamp = collectionTimestamps.get(collection);
            // 'since' is date in milliseconds exclusive
            List<RecordDescription> recordDescriptions = domsClient.getCentralWebservice().getIDsModified(
                    lastTimestamp, translate(collection), "SummaVisible", "Published", 0, MAX_DOMS_RESULT_SIZE);
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

    private String translate(Collection collection) {
        switch (collection) {
            case DOMS_RADIO_TV:
                return "doms:RadioTV_Collection";
            case DOMS_REKLAMEFILM:
                return "doms:Collection_Reklamefilm";
            default: throw new UnknownCollectionException("unknown collection: " + collection);
        }
    }
}
