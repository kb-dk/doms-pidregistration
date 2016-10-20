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
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final JobDTO jobDto;
    private final GlobalHandleRegistry handleRegistry;
    private final JobsDAO jobsDAO;
    private final PropertyBasedRegistrarConfiguration configuration;
    private final DOMSMetadataQueryer domsMetadataQueryer;
    private final DOMSUpdater domsUpdater;

    public HandleCall(JobDTO jobDto, GlobalHandleRegistry handleRegistry, JobsDAO jobsDAO,
                      PropertyBasedRegistrarConfiguration configuration, DOMSMetadataQueryer domsMetadataQueryer,
                      DOMSUpdater domsUpdater) {
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
            log.info("Handling object ID '{}'", jobDto.getUuid());
            PIDHandle pidHandle = new PIDHandle(configuration.getHandlePrefix(), jobDto.getUuid());
            boolean domsChanged = updateDoms(pidHandle);
            String url = String.format(
                    "%s/%s/%s", configuration.getPidPrefix(), jobDto.getCollection().getId(), pidHandle.getId());
            boolean handleRegistryChanged = handleRegistry.registerPid(
                    pidHandle,
                    url
            );

            if (domsChanged || handleRegistryChanged) {
                succeeded = true;
            }

            jobDto.setState(JobDTO.State.DONE);
            jobsDAO.update(jobDto);
        } catch (Exception e) {
            jobDto.setState(JobDTO.State.ERROR);
            jobsDAO.update(jobDto);
            log.error("Error handling object ID '{}'", jobDto.getUuid(), e);
        }
        return succeeded;
    }

    private boolean updateDoms(PIDHandle pidHandle) {
        String objectId = pidHandle.getId();
        DOMSMetadata metadata = domsMetadataQueryer.getMetadataForObject(objectId);
        boolean domsChanged = false;
        if (!metadata.handleExists(pidHandle)) {
            log.debug("Attaching PID handle '{}' to object ID '{}' in DOMS", pidHandle, objectId);
            metadata.attachHandle(pidHandle);
            domsUpdater.update(objectId, metadata);
            domsChanged = true;
        } else {
            log.info("PID handle '{}' already attached to object ID '{}'. Not added to DOMS", pidHandle, objectId);
        }
        return domsChanged;
    }
}
