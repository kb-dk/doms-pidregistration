package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidCredentialsException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidResourceException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.MethodFailedException;

import javax.xml.transform.TransformerException;

/**
 */
public class DOMSUpdater {

    private DOMSClient domsClient;

    public DOMSUpdater(DOMSClient domsClient) {
        this.domsClient = domsClient;
    }

    public void update(String objectId, DOMSMetadata metadata) {
        try {
            unpublishModifyPublish(objectId, metadata);
        } catch (Exception e) {
            throw new BackendMethodFailedException(
                    "Backendmethod failed while trying to add handle to '" + objectId + "'",
                    e);
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
