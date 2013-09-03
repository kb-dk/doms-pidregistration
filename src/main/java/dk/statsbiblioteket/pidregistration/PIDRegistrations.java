package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.doms.DOMSClient;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadata;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadataQueryer;
import dk.statsbiblioteket.pidregistration.doms.DOMSObjectQueryer;
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
    private DOMSObjectQueryer domsObjectQueryer;
    private DOMSUpdater domsUpdater;

    public PIDRegistrations(PropertyBasedRegistrarConfiguration configuration,
                            DOMSClient domsClient,
                            GlobalHandleRegistry handleRegistry,
                            Date fromInclusive) {
        this.configuration = configuration;
        this.handleRegistry = handleRegistry;

        domsMetadataQueryer = new DOMSMetadataQueryer(domsClient);
        domsObjectQueryer = new DOMSObjectQueryer(domsClient, fromInclusive);
        domsUpdater = new DOMSUpdater(domsClient);
    }

    /**
     * 1. For each collection query the DOMS for object IDs after the last modified data in the specific collection
     *
     * 2. For each object ID,
     *   2a. (DOMS) Check if a handle is attached to the object. If not, build and attach it
     *   2b. (Global Handle Registry) Check if a (handle -> URL) pair exists in the global handle registry. If not,
     *       build and attach it.
     */
    public void doRegistrations() {
        for (Collection collection : Collection.values()) {
            switch (collection) {
                case DOMS_RADIO_TV:
                case DOMS_REKLAMEFILM:
                    List<String> objectIds = domsObjectQueryer.findNextIn(collection);
                    while (!objectIds.isEmpty()) {
                        handleObjects(collection, objectIds);
                        objectIds = domsObjectQueryer.findNextIn(collection);
                    }
                    break;
                default:
                    throw new UnknownCollectionException("Unknown collection: " + collection);
            }
        }
        String message = String.format("Done adding handles. #success: %s #failure: %s", success, failure);
        log.info(message);
    }

    private void handleObjects(Collection collection, List<String> objectIds) {
        for (String objectId : objectIds) {
            try {
                log.info(String.format("Handling object ID '%s'", objectId));
                DOMSMetadata metadata = domsMetadataQueryer.getMetadataForObject(objectId);
                PIDHandle handle = buildHandle(objectId);
                boolean domsChanged = false;
                if (!metadata.handleExists(handle)) {
                    log.debug(String.format("Attaching PID handle '%s' to object ID '%s' in DOMS", handle, objectId));
                    metadata.attachHandle(handle);
                    domsUpdater.update(objectId, metadata);
                    domsChanged = true;
                } else {
                    log.info(String.format(
                            "PID handle '%s' already attached to object ID '%s'. Not added to DOMS", handle, objectId
                    ));
                }

                log.info(String.format("Attaching PID handle '%s' in global registry", handle));
                boolean handleRegistryChanged = handleRegistry.registerPid(handle, buildUrl(collection, handle));

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

    private String buildUrl(Collection collection, PIDHandle handle) {
        return String.format("%s/%s/%s", configuration.getPidResolverPrefix(), collection.getId(), handle.asString());
    }
}
