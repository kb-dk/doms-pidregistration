package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.doms.DOMS;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadata;
import dk.statsbiblioteket.pidregistration.doms.DOMSQueryBuilder;
import dk.statsbiblioteket.pidregistration.handlesystem.GlobalHandleRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;
import java.util.List;

/**
 * Main batch job. Does all the work related to the registration of PIDs in DOMS and in the Global Handle Registry
 */
public class PIDRegistrations {
    private final Log log = LogFactory.getLog(getClass());

    private static final int DOMS_QUERY_PAGE_SIZE = 100;

    private int success = 0;
    private int noWorkDone = 0;
    private int failure = 0;

    private PropertyBasedRegistrarConfiguration configuration;
    private DOMS doms;
    private GlobalHandleRegistry handleRegistry;
    private Date fromInclusive;

    public PIDRegistrations(PropertyBasedRegistrarConfiguration configuration,
                            DOMS doms,
                            GlobalHandleRegistry handleRegistry,
                            Date fromInclusive) {
        this.configuration = configuration;
        this.doms = doms;
        this.handleRegistry = handleRegistry;
        this.fromInclusive = fromInclusive;
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
                    DOMSQueryBuilder queryBuilder = new DOMSQueryBuilder(collection,
                                                                         fromInclusive,
                                                                         DOMS_QUERY_PAGE_SIZE);
                    boolean done = false;
                    while (!done) {
                        List<String> objectIds = doms.findObjectsFromQuery(queryBuilder.next());
                        if (objectIds.isEmpty()) {
                            done = true;
                        } else {
                            handleObjects(collection, objectIds);
                        }
                    }
                    break;
                default:
                    throw new UnknownCollectionException("Unknown collection: " + collection);
            }
        }
        String message = String.format("Done adding handles. #success: %s #noWorkDone: %s #failure: %s", success, noWorkDone, failure);
        log.info(message);
    }

    private void handleObjects(Collection collection, List<String> objectIds) {
        for (String objectId : objectIds) {
            try {
                log.debug(String.format("Handling object ID '%s'", objectId));
                DOMSMetadata metadata = doms.getMetadataForObject(objectId);
                PIDHandle handle = buildHandle(objectId);
                boolean domsChanged = false;
                if (!metadata.handleExists(handle)) {
                    log.debug(String.format("Attaching PID handle '%s' to object ID '%s'", handle, objectId));
                    metadata.attachHandle(handle);
                    doms.updateMetadataForObject(objectId, metadata);
                    domsChanged = true;
                } else {
                    log.debug(String.format(
                            "PID handle '%s' already attached to object ID '%s'. Not added to DOMS", handle, objectId
                    ));
                }

                log.debug(String.format("Attaching PID handle '%s' in global registry", handle));
                boolean handleRegistryChanged = handleRegistry.registerPid(handle, buildUrl(collection, handle));

                if (domsChanged || handleRegistryChanged) {
                    success++;
                } else {
                    noWorkDone++;
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
