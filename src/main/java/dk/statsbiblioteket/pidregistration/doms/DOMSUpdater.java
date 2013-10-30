package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidCredentialsException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidResourceException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.MethodFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.TransformerException;

/**
 */
public class DOMSUpdater {
    private static final Logger log = LoggerFactory.getLogger(DOMSUpdater.class);

    private DOMSClient domsClient;

    public DOMSUpdater(DOMSClient domsClient) {
        this.domsClient = domsClient;
    }

    public void update(String objectId, DOMSMetadata metadata) {
        try {
            unpublishModifyPublish(objectId, metadata);
        } catch (Exception e) {
            try {
                domsClient.markPublishedObject(objectId);
            } catch (MethodFailedException e1) {
                log.error("Publishing object '{}' after update error failed");
            } catch (InvalidResourceException e1) {
                log.error("Publishing object '{}' after update error failed");
            } catch (InvalidCredentialsException e1) {
                log.error("Publishing object '{}' after update error failed");
            }
            throw new BackendMethodFailedException(
                    "Backendmethod failed while trying to update '" + objectId + "'", e
            );
        }
    }

    private void unpublishModifyPublish(String objectId, DOMSMetadata metadata) throws MethodFailedException, InvalidResourceException, InvalidCredentialsException, TransformerException {
        if (!domsClient.isActive(objectId)) {
            throw new BackendInvalidStateException("object " + objectId + " is not currently Published. Cannot add handle");
        }

        domsClient.markInProgressObject(objectId);

        domsClient.modifyDatastream(objectId, metadata);

        domsClient.markPublishedObject(objectId);
    }
}
