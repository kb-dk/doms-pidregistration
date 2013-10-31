package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.ConnectionFactory;
import dk.statsbiblioteket.pidregistration.database.DatabaseException;
import dk.statsbiblioteket.pidregistration.database.DatabaseSchema;
import dk.statsbiblioteket.pidregistration.database.dao.JobsDAO;
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

    private DOMSClient domsClient;
    private DOMSMetadataQueryer domsMetadataQueryer;
    private DOMSUpdater domsUpdater;
    private Connection connection;
    private JobsDAO jobsDao;

    private Integer numberOfObjectsToTest;

    public PIDRegistrations(
            PropertyBasedRegistrarConfiguration configuration,
            DOMSClient domsClient,
            GlobalHandleRegistry handleRegistry) {
        this(configuration, domsClient, handleRegistry, null);
    }

    public PIDRegistrations(
            PropertyBasedRegistrarConfiguration configuration,
            DOMSClient domsClient,
            GlobalHandleRegistry handleRegistry,
            Integer numberOfObjectsToTest) {
        this.configuration = configuration;
        this.domsClient = domsClient;
        this.handleRegistry = handleRegistry;
        this.numberOfObjectsToTest = numberOfObjectsToTest;

        domsMetadataQueryer = new DOMSMetadataQueryer(domsClient);
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
        DatabaseSchema databaseSchema = new DatabaseSchema(configuration);
        databaseSchema.createIfNotExist();

        setupConnection();

        Date lastJobCreated = jobsDao.getLastJobCreated();
        DOMSObjectIDQueryer domsObjectIdQueryer = new DOMSObjectIDQueryer(
                domsClient,
                new Date(lastJobCreated == null || isTestMode() ? 0 : lastJobCreated.getTime() - 10000)
        );

        Set<Collection> collections = configuration.getDomsCollections();

        int jobCount = 0;
        log.info("Adding jobs to database");
        for (Collection collection : collections) {
            int jobCountPerCollection = 0;
            List<String> objectIds = domsObjectIdQueryer.findNextIn(collection);
            while (!objectIds.isEmpty()) {
                beginTransaction();
                List<JobDTO> jobsToBeAdded = buildJobs(collection, objectIds);
                if (isTestMode() && jobsToBeAdded.size() > numberOfObjectsToTest) {
                    jobsToBeAdded = jobsToBeAdded.subList(0, numberOfObjectsToTest);
                }
                jobsDao.save(jobsToBeAdded);
                commitTransaction();

                int jobsAdded = jobsToBeAdded.size();

                jobCountPerCollection += jobsAdded;
                jobCount += jobsAdded;
                log.info("Jobs added so far: {}", jobCount);

                if (isTestMode() && jobCountPerCollection >= numberOfObjectsToTest) {
                    break;
                }

                objectIds = domsObjectIdQueryer.findNextIn(collection);
            }
        }
        log.info("Added {} jobs", jobCount);
        log.info("Adding handles");
        JobDTO jobDto;

        while ((jobDto = jobsDao.findJobError()) != null) {
            jobDto.setState(JobDTO.State.PENDING);
            jobsDao.update(jobDto);
        }

        while ((jobDto = jobsDao.findJobPending()) != null) {
            handleObject(jobDto);
        }

        String message = String.format("Done adding handles. #success: %s #failure: %s", success, failure);
        log.info(message);

        teardownConnection();
    }

    private boolean isTestMode() {
        return numberOfObjectsToTest != null;
    }

    public void doUnregistrations() {
        setupConnection();

        log.info("Restoring DOMS and global handle registry to previous state...");
        JobDTO jobDto;
        int errors = 0;
        while ((jobDto = jobsDao.findJobDone()) != null) {
            try {
                String objectId = jobDto.getUuid();
                log.info("Restoring {}", objectId);
                PIDHandle handle = buildHandle(objectId);

                restoreGlobalHandleRegistry(handle);
                restoreDoms(objectId);

                jobDto.setState(JobDTO.State.DELETED);
                jobsDao.update(jobDto);
            } catch (Exception e) {
                errors++;
                log.error("Error when trying to restore {}", jobDto.getUuid(), e);
                jobDto.setState(JobDTO.State.ERROR);
                jobsDao.update(jobDto);
            }
        }
        log.info("{} errors encountered unregistrating objects IDs", errors);
        teardownConnection();
    }

    private void restoreDoms(String objectId) {
        DOMSMetadata metadata = domsMetadataQueryer.getMetadataForObject(objectId);
        metadata.detachHandle(buildHandle(objectId));
        domsUpdater.update(objectId, metadata);
    }

    private void restoreGlobalHandleRegistry(PIDHandle handle) {
        handleRegistry.deletePid(handle);
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

    private void setupConnection() {
        connection = new ConnectionFactory(configuration).createConnection();
        jobsDao = new JobsDAO(configuration, connection);
    }

    private void teardownConnection() {
        try {
            jobsDao = null;
            connection.close();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    private List<JobDTO> buildJobs(Collection collection, List<String> objectIds) {
        List<JobDTO> result = new ArrayList<JobDTO>();
        for (String objectId : objectIds) {
            if (jobsDao.findJobWithUUID(objectId) != null) {
                log.debug("Job with UUID " + objectId + " already exists in job list. Ignoring");
            } else {
                result.add(new JobDTO(objectId, collection, JobDTO.State.PENDING));
            }
        }

        return result;
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
            }

            jobDto.setState(JobDTO.State.DONE);
            jobsDao.update(jobDto);
        } catch (Exception e) {
            failure++;

            jobDto.setState(JobDTO.State.ERROR);
            jobsDao.update(jobDto);
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
