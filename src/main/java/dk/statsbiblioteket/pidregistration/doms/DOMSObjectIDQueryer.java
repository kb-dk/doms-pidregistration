package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.Collection;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidCredentialsException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.MethodFailedException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.RecordDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 */
public class DOMSObjectIDQueryer {
    private static final Logger log = LoggerFactory.getLogger(DOMSObjectIDQueryer.class);

    private DOMSClient domsClient;

    public DOMSObjectIDQueryer(DOMSClient domsClient) {
        this.domsClient = domsClient;
    }

    public DOMSObjectIDQueryResult findNextIn(Collection collection, Date sinceInclusive) {
        try {
            List<String> result = new ArrayList<String>();
            log.debug("Querying DOMS for objects in collection {} modified since {} ({})", new Object[] {
                      collection,
                      sinceInclusive,
                      sinceInclusive.getTime()});

            Date latestReadSoFar = sinceInclusive;
            List<RecordDescription> recordDescriptions = domsClient.getIDsModified(sinceInclusive, collection);
            for (RecordDescription recordDescription : recordDescriptions) {
                Date recordDescriptionDate = new Date(recordDescription.getDate());
                if (recordDescriptionDate.after(latestReadSoFar)) {
                    latestReadSoFar = recordDescriptionDate;
                }

                result.add(recordDescription.getPid());
            }

            log.debug("Got {} objects in collection {}", result.size(), collection);
            return new DOMSObjectIDQueryResult(result, latestReadSoFar);
        } catch (InvalidCredentialsException e) {
            throw new BackendInvalidCredsException(
                    "Invalid Credentials Supplied", e);
        } catch (MethodFailedException e) {
            throw new BackendMethodFailedException(
                    "Server error: " + e.getMessage(), e);
        }
    }
}
