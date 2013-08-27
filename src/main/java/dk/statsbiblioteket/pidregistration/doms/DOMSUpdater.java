package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.CentralWebservice;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidCredentialsException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidResourceException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.MethodFailedException;

import javax.xml.transform.TransformerException;
import java.util.Arrays;

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
        CentralWebservice doms = domsClient.getCentralWebservice();

        if (!isActive(objectId)) {
            throw new BackendInvalidStateException("object " + objectId + " is not currently Published. Cannot add handle");
        }

        doms.markInProgressObject(Arrays.asList(objectId), "Prepare to add handle PID");

        doms.modifyDatastream(objectId, domsClient.getDatastreamId(), metadata.getMetadata(), "Adding handle PID");

        doms.markPublishedObject(Arrays.asList(objectId), "Done adding handle PID");
    }

    private boolean isActive(String objectId) throws MethodFailedException, InvalidResourceException, InvalidCredentialsException {
        String state = domsClient.getCentralWebservice().getObjectProfile(objectId).getState();
        return "A".equals(state);
    }
}
