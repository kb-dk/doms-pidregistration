package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.doms.DOMS;
import dk.statsbiblioteket.pidregistration.doms.DOMSQueryBuilder;
import dk.statsbiblioteket.pidregistration.doms.Metadata;
import dk.statsbiblioteket.pidregistration.handlesystem.GlobalHandleRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;
import java.util.List;

public class HandleRegistrations {
    private final Log log = LogFactory.getLog(getClass());
    private int success = 0;
    private int noWorkDone = 0;
    private int failure = 0;

    private PropertyBasedRegistrarConfiguration configuration;
    private DOMS doms;
    private GlobalHandleRegistry handleRegistry;
    private Date fromInclusive;

    public HandleRegistrations(PropertyBasedRegistrarConfiguration configuration,
                               DOMS doms,
                               GlobalHandleRegistry handleRegistry,
                               Date fromInclusive) {
        this.configuration = configuration;
        this.doms = doms;
        this.handleRegistry = handleRegistry;
        this.fromInclusive = fromInclusive;
    }

    public void doRegistrations() {
        for (Collection collection : Collection.values()) {
            switch (collection) {
                case DOMS_RADIO_TV:
                case DOMS_REKLAMEFILM:
                    handleDomsObjects(collection);
                    break;
                default:
                    throw new UnknownCollectionException("Unknown collection: " + collection);
            }
        }
        String message = String.format("Done adding handles. #success: %d #noWorkDone: %d #failure: %d", success, noWorkDone, failure);
        log.info(message);
    }

    private void handleDomsObjects(Collection collection) {
        DOMSQueryBuilder queryBuilder = new DOMSQueryBuilder(collection, fromInclusive);
        List<String> objectIds = doms.findObjectsFromQuery(queryBuilder.build());
        for (String objectId : objectIds) {
            try {
                log.debug(String.format("Handling object ID '%s'", objectId));
                Metadata metadata = doms.getMetadataForObject(objectId);
                PIDHandle handle = buildHandle(objectId);
                if (!metadata.handleExists(handle)) {
                    log.debug(String.format("Attaching PID handle '%s' to object ID '%s'", handle, objectId));
                    metadata.attachHandle(handle);
                    doms.updateMetadataForObject(objectId, metadata);
                    log.debug(String.format("Attaching PID handle '%s' in global registry", handle));
                    handleRegistry.registerPid(handle, buildUrl(collection, handle));
                    success++;
                } else {
                    log.debug(String.format(
                            "PID handle '%s' already attached to object ID '%s'. No further work done.", handle, objectId
                    ));
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
