package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.doms.DOMSClient;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadata;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadataQueryer;
import dk.statsbiblioteket.pidregistration.doms.DOMSObjectIDQueryer;
import dk.statsbiblioteket.pidregistration.doms.DOMSUpdater;
import dk.statsbiblioteket.pidregistration.handlesystem.GlobalHandleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * Main batch job. Does all the work related to the registration of PIDs in DOMS and in the Global Handle Registry
 */
public class PIDRegistrations {
    private static final Logger log = LoggerFactory.getLogger(PIDRegistrations.class);

    private int success = 0;
    private int failure = 0;

    private PropertyBasedRegistrarConfiguration configuration;
    private GlobalHandleRegistry handleRegistry;

    private DOMSMetadataQueryer domsMetadataQueryer;
    private DOMSObjectIDQueryer domsObjectIdQueryer;
    private DOMSUpdater domsUpdater;

    public PIDRegistrations(PropertyBasedRegistrarConfiguration configuration,
                            DOMSClient domsClient,
                            GlobalHandleRegistry handleRegistry,
                            Date fromInclusive) {
        this.configuration = configuration;
        this.handleRegistry = handleRegistry;

        domsMetadataQueryer = new DOMSMetadataQueryer(domsClient);
        domsObjectIdQueryer = new DOMSObjectIDQueryer(domsClient, fromInclusive);
        domsUpdater = new DOMSUpdater(domsClient);
    }

    /**
     * 1. For each collection query the DOMS for object IDs after the last modified data in the specific collection
     * <p/>
     * 2. For each object ID,
     * 2a. (DOMS) Check if a handle is attached to the object. If not, build and attach it
     * 2b. (Global Handle Registry) Check if a (handle -> URL) pair exists in the global handle registry. If not,
     * build and attach it.
     */
    public void doRegistrations() {
        List<Collection> collections = configuration.getDomsCollections();

        for (Collection collection : collections) {
            List<String> objectIds = domsObjectIdQueryer.findNextIn(collection);
            while (!objectIds.isEmpty()) {
                handleObjects(collection, objectIds);
                objectIds = domsObjectIdQueryer.findNextIn(collection);
            }
        }
        String message = String.format("Done adding handles. #success: %s #failure: %s", success, failure);
        log.info(message);
    }

    private void handleObjects(Collection collection, List<String> objectIds) {
        for (String objectId : objectIds) {
            try {
                log.info(String.format("Handling object ID '%s'", objectId));
                PIDHandle pidHandle = buildHandle(objectId);
                boolean domsChanged = updateDoms(pidHandle);
                boolean handleRegistryChanged = handleRegistry.registerPid(pidHandle, buildUrl(collection, pidHandle));

                if (domsChanged || handleRegistryChanged) {
                    success++;
                }
            } catch (Exception e) {
                failure++;
                log.error(String.format("Error handling object ID '%s'", objectId), e);
            }
        }
    }

    private PIDHandle buildHandle(String objectId) {
        return new PIDHandle(configuration.getHandlePrefix(), objectId);
    }

    private boolean updateDoms(PIDHandle pidHandle) {
        String objectId = pidHandle.getId();
        DOMSMetadata metadata = domsMetadataQueryer.getMetadataForObject(objectId);
        boolean domsChanged = false;
        if (!metadata.handleExists(pidHandle)) {
            log.debug(String.format("Attaching PID handle '%s' to object ID '%s' in DOMS", pidHandle, objectId));
            metadata.attachHandle(pidHandle);
            domsUpdater.update(objectId, metadata);
            domsChanged = true;
        } else {
            log.info(String.format(
                    "PID handle '%s' already attached to object ID '%s'. Not added to DOMS", pidHandle, objectId
            ));
        }
        return domsChanged;
    }

    private String buildUrl(Collection collection, PIDHandle handle) {
        return String.format("%s/%s/%s", configuration.getPidPrefix(), collection.getId(), handle.getId());
    }
}
