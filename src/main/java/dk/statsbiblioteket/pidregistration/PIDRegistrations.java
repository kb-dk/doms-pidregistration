package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.ConnectionFactory;
import dk.statsbiblioteket.pidregistration.database.DatabaseException;
import dk.statsbiblioteket.pidregistration.database.dao.JobDAO;
import dk.statsbiblioteket.pidregistration.database.dto.JobDTO;
import dk.statsbiblioteket.pidregistration.doms.DOMSClient;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadata;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadataQueryer;
import dk.statsbiblioteket.pidregistration.doms.DOMSObjectIDQueryer;
import dk.statsbiblioteket.pidregistration.doms.DOMSUpdater;
import dk.statsbiblioteket.pidregistration.handlesystem.GlobalHandleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
    private Connection connection;
    private JobDAO jobDao;

    public PIDRegistrations(PropertyBasedRegistrarConfiguration configuration,
                            DOMSClient domsClient,
                            GlobalHandleRegistry handleRegistry,
                            Date fromInclusive) {
        this.configuration = configuration;
        this.handleRegistry = handleRegistry;

        domsMetadataQueryer = new DOMSMetadataQueryer(domsClient);
        domsObjectIdQueryer = new DOMSObjectIDQueryer(domsClient, fromInclusive);
        domsUpdater = new DOMSUpdater(domsClient);

        connection = new ConnectionFactory().createConnection();
        jobDao = new JobDAO(configuration, connection);
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
        Set<Collection> collections = configuration.getDomsCollections();

        log.info("Adding jobs to database");
        for (Collection collection : collections) {
            List<String> objectIds = domsObjectIdQueryer.findNextIn(collection);
            while (!objectIds.isEmpty()) {

                beginTransaction();
                persistObjects(collection, objectIds);
                commitTransaction();
                objectIds = domsObjectIdQueryer.findNextIn(collection);
            }
        }
        log.info("Done adding jobs");
        log.info("Adding handles");
        JobDTO jobDto;
        while((jobDto = jobDao.findPendingJob()) != null) {
            handleObject(jobDto);
        }

        String message = String.format("Done adding handles. #success: %s #failure: %s", success, failure);
        log.info(message);
        closeConnection();
    }

    private void beginTransaction() {
        try {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    private void commitTransaction() {
        try {
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                throw new DatabaseException(e);
            }
            throw new DatabaseException(e);
        }
    }

    private void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    private void persistObjects(Collection collection, List<String> objectIds) {
        List<JobDTO> jobDtos = new ArrayList<JobDTO>();
        for (String objectId : objectIds) {
            if (jobDao.findJobWithUUID(objectId) != null) {
                log.info("Job with UUID " + objectId + " already exists in job list. Ignoring");
            } else {
                jobDtos.add(new JobDTO(objectId, collection, JobDTO.State.PENDING));
            }
        }

        jobDao.save(jobDtos);
    }


    private void handleObject(JobDTO jobDto) {
        try {
            log.info(String.format("Handling object ID '%s'", jobDto.getUuid()));
            PIDHandle pidHandle = buildHandle(jobDto.getUuid());
            boolean domsChanged = updateDoms(pidHandle);
            boolean handleRegistryChanged = handleRegistry.registerPid(
                    pidHandle,
                    buildUrl(jobDto.getCollection(), pidHandle)
            );

            if (domsChanged || handleRegistryChanged) {
                success++;
                jobDto.setState(JobDTO.State.DONE);
                jobDao.update(jobDto);
            }
        } catch (Exception e) {
            failure++;
            jobDto.setState(JobDTO.State.ERROR);
            jobDao.update(jobDto);
            log.error(String.format("Error handling object ID '%s'", jobDto.getUuid()), e);
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
            log.info(String.format("Attaching PID handle '%s' to object ID '%s' in DOMS", pidHandle, objectId));
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
