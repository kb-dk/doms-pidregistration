package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.dao.JobsDAO;
import dk.statsbiblioteket.pidregistration.database.dto.JobDTO;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadata;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadataQueryer;
import dk.statsbiblioteket.pidregistration.doms.DOMSUpdater;
import dk.statsbiblioteket.pidregistration.handlesystem.GlobalHandleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;


public class HandleCall implements Callable<Boolean> {
    private static final Logger log = LoggerFactory.getLogger(HandleCall.class);
    private JobDTO jobDto;
    private GlobalHandleRegistry handleRegistry;
    private JobsDAO jobsDAO;
    private PropertyBasedRegistrarConfiguration configuration;
    private DOMSMetadataQueryer domsMetadataQueryer;
    private DOMSUpdater domsUpdater;

    public HandleCall(JobDTO jobDto, GlobalHandleRegistry handleRegistry, JobsDAO jobsDAO, PropertyBasedRegistrarConfiguration configuration, DOMSMetadataQueryer domsMetadataQueryer, DOMSUpdater domsUpdater) {
        this.jobDto = jobDto;
        this.handleRegistry = handleRegistry;
        this.jobsDAO = jobsDAO;
        this.configuration = configuration;
        this.domsMetadataQueryer = domsMetadataQueryer;
        this.domsUpdater = domsUpdater;
    }

    public Boolean call() {
        boolean succeeded = false;
        try {
            log.info(String.format("Handling object ID '%s'", jobDto.getUuid()));
            PIDHandle pidHandle = buildHandle(jobDto.getUuid());
            boolean domsChanged = updateDoms(pidHandle);
            boolean handleRegistryChanged = handleRegistry.registerPid(
                    pidHandle,
                    buildUrl(jobDto.getCollection(), pidHandle)
            );

            if (domsChanged || handleRegistryChanged) {
                succeeded = true;
            }

            jobDto.setState(JobDTO.State.DONE);
            jobsDAO.update(jobDto);
        } catch (Exception e) {
            jobDto.setState(JobDTO.State.ERROR);
            jobsDAO.update(jobDto);
            log.error(String.format("Error handling object ID '%s'", jobDto.getUuid()), e);
        }
        return succeeded;
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
