package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.ConnectionFactory;
import dk.statsbiblioteket.pidregistration.database.DatabaseException;
import dk.statsbiblioteket.pidregistration.database.DatabaseSchema;
import dk.statsbiblioteket.pidregistration.database.dao.CollectionTimestampsDAO;
import dk.statsbiblioteket.pidregistration.database.dao.JobsDAO;
import dk.statsbiblioteket.pidregistration.database.dto.JobDTO;
import dk.statsbiblioteket.pidregistration.doms.DOMSClient;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadata;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadataQueryer;
import dk.statsbiblioteket.pidregistration.doms.DOMSObjectIDQueryResult;
import dk.statsbiblioteket.pidregistration.doms.DOMSObjectIDQueryer;
import dk.statsbiblioteket.pidregistration.doms.DOMSUpdater;
import dk.statsbiblioteket.pidregistration.handlesystem.GlobalHandleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Main batch job. Does all the work related to the registration of PIDs in DOMS and in the Global Handle Registry
 */
public class PIDRegistrations {
    private static final int JOBS_QUEUE_LIMIT = 10000;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final DOMSObjectIDQueryer domsObjectIdQueryer;
    private final PropertyBasedRegistrarConfiguration configuration;
    private final GlobalHandleRegistry handleRegistry;
    private final DOMSClient domsClient;
    private final DOMSMetadataQueryer domsMetadataQueryer;
    private final DOMSUpdater domsUpdater;
    private final ConnectionFactory connectionFactory;

    private int failure = 0;
    private int success = 0;

    private Integer numberOfObjectsToTest;

    public PIDRegistrations(
            PropertyBasedRegistrarConfiguration configuration,
            DOMSClient domsClient,
            GlobalHandleRegistry handleRegistry) {
        this(configuration, domsClient, handleRegistry, new DOMSObjectIDQueryer(domsClient),
                new DOMSUpdater(domsClient), new ConnectionFactory(configuration));
    }

    // For PIDRegistrationsCommandLineInterface
    public PIDRegistrations(
            PropertyBasedRegistrarConfiguration configuration,
            DOMSClient domsClient,
            GlobalHandleRegistry handleRegistry,
            Integer numberOfObjectsToTest) {
        this(configuration, domsClient, handleRegistry, new DOMSObjectIDQueryer(domsClient),
                new DOMSUpdater(domsClient), new ConnectionFactory(configuration));
        this.numberOfObjectsToTest = numberOfObjectsToTest;
    }

    public PIDRegistrations(
            PropertyBasedRegistrarConfiguration configuration,
            DOMSClient domsClient,
            GlobalHandleRegistry handleRegistry,
            DOMSObjectIDQueryer domsObjectIdQueryer,
            DOMSUpdater domsUpdater
    ) {
        this(configuration, domsClient, handleRegistry, domsObjectIdQueryer, domsUpdater,
                new ConnectionFactory(configuration));
    }

    public PIDRegistrations(
            PropertyBasedRegistrarConfiguration configuration,
            DOMSClient domsClient,
            GlobalHandleRegistry handleRegistry,
            DOMSObjectIDQueryer domsObjectIdQueryer,
            DOMSUpdater domsUpdater,
            ConnectionFactory connectionFactory
    ) {
        this.configuration = configuration;
        this.domsClient = domsClient;
        this.handleRegistry = handleRegistry;
        this.domsObjectIdQueryer = domsObjectIdQueryer;
        this.domsUpdater = domsUpdater;
        this.connectionFactory = connectionFactory;

        domsMetadataQueryer = new DOMSMetadataQueryer(domsClient);
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
        DatabaseSchema databaseSchema = new DatabaseSchema(connectionFactory);
        databaseSchema.createIfNotExist();

        // Setup connection
        try (Connection connection = connectionFactory.createConnection()) {
            JobsDAO jobsDao = connectionFactory.createJobsDAO(connection);
            CollectionTimestampsDAO collectionTimestampsDao = connectionFactory.createCollectionTimestampsDAO(connection);


            Set<Collection> collections = configuration.getDomsCollections();

            int jobCount = 0;
            log.info("Adding jobs to database");
            for (Collection collection : collections) {
                int jobCountPerCollection = 0;
                Date sinceInclusive = getOrCreateTimestampForCollection(collectionTimestampsDao, collection);
                DOMSObjectIDQueryResult queryResult = domsObjectIdQueryer.findNextIn(collection, sinceInclusive);
                while (!queryResult.isEmpty()) {
                    beginTransaction(connection);

                    Set<String> distinctObjectIds = new HashSet<String>(queryResult.getObjectIds());
                    List<JobDTO> jobsToBeAdded = buildNewJobs(jobsDao, collection, distinctObjectIds);

                    if (isTestMode() && jobsToBeAdded.size() > numberOfObjectsToTest) {
                        jobsToBeAdded = jobsToBeAdded.subList(0, numberOfObjectsToTest);
                    }

                    if (!jobsToBeAdded.isEmpty()) {
                        jobsDao.save(jobsToBeAdded);
                    }

                    sinceInclusive = queryResult.getLatestRead();
                    updateTimestamp(collectionTimestampsDao, collection, sinceInclusive);

                    commitTransaction(connection);

                    int jobsAdded = jobsToBeAdded.size();

                    jobCountPerCollection += jobsAdded;
                    jobCount += jobsAdded;
                    log.info("Jobs added so far: {}", jobCount);

                    if (queryResult.getObjectIds().size() < domsClient.getMaxDomsResultSize()) {
                        log.debug("DOMS client returned {} objects which is less than the max result size of {}. " +
                                        "Assuming no more objects.",
                                queryResult.getObjectIds().size(),
                                domsClient.getMaxDomsResultSize()
                        );
                        break;
                    }

                    if (isTestMode() && jobCountPerCollection >= numberOfObjectsToTest) {
                        break;
                    }

                    queryResult = domsObjectIdQueryer.findNextIn(collection, sinceInclusive);
                }
            }
            log.info("Added {} jobs", jobCount);

            JobDTO jobDto;
            while ((jobDto = jobsDao.findJobError()) != null) {
                jobDto.setState(JobDTO.State.PENDING);
                jobsDao.update(jobDto);
            }

            log.info("Adding handles");
            if (isTestMode()) {
                log.info("Script in test mode. Not adding handles");
            } else {
                handleObjects(jobsDao);
            }

            log.info("Done adding handles. #success: {} #failure: {}", success, failure);

        } catch (SQLException e) {
            log.error("Error when trying to close connection to database", e);
        }
    }

    private boolean isTestMode() {
        return numberOfObjectsToTest != null;
    }

    public void doUnregistrations() {
        try (Connection connection = connectionFactory.createConnection()){
            JobsDAO jobsDao = connectionFactory.createJobsDAO(connection);

            log.info("Restoring DOMS and global handle registry to previous state...");
            JobDTO jobDto;
            int errors = 0;
            while ((jobDto = jobsDao.findJobDone()) != null) {
                try {
                    String objectId = jobDto.getUuid();
                    log.info("Restoring {}", objectId);
                    PIDHandle handle = new PIDHandle(configuration.getHandlePrefix(), objectId);

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

        } catch (SQLException e) {
            log.error("Error when trying to close connection to database", e);
        }
    }

    private void restoreDoms(String objectId) {
        DOMSMetadata metadata = domsMetadataQueryer.getMetadataForObject(objectId);
        metadata.detachHandle(new PIDHandle(configuration.getHandlePrefix(), objectId));
        domsUpdater.update(objectId, metadata);
    }

    private void restoreGlobalHandleRegistry(PIDHandle handle) {
        handleRegistry.deletePid(handle);
    }

    private void beginTransaction(Connection connection) {
        try {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    private void commitTransaction(Connection connection) {
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

    private List<JobDTO> buildNewJobs(JobsDAO jobsDao, Collection collection, Set<String> objectIds) {
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

    private Date getOrCreateTimestampForCollection(CollectionTimestampsDAO collectionTimestampsDao, Collection collection) {
        Date result = collectionTimestampsDao.load(collection);
        if (result == null) {
            result = new Date(0L);
            collectionTimestampsDao.save(collection, result);
        }
        return result;
    }

    private void updateTimestamp(CollectionTimestampsDAO collectionTimestampsDao, Collection collection, Date timestamp) {
        collectionTimestampsDao.update(collection, timestamp);
    }

    private void handleObjects(JobsDAO jobsDao) {

        int numberOfThreads = configuration.getNumberOfThreads();
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        List<Pair<JobDTO, Future<Boolean>>> results;

        do {
            Iterator<JobDTO> pendingJobs = jobsDao.findJobsPending(JOBS_QUEUE_LIMIT);

            results = new ArrayList<>();

            while (pendingJobs.hasNext()) {
                JobDTO jobDto = pendingJobs.next();
                HandleCall handleCall = new HandleCall(
                        jobDto, handleRegistry, configuration, domsMetadataQueryer, domsUpdater);
                Future<Boolean> result = executor.submit(handleCall);
                results.add(new Pair<>(jobDto, result));
            }

            countHandleSuccess(results, jobsDao);

        } while (results.size() == JOBS_QUEUE_LIMIT);

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private void countHandleSuccess(List<Pair<JobDTO, Future<Boolean>>> results, JobsDAO jobsDAO) {
        for (Pair<JobDTO, Future<Boolean>> result : results) {
            JobDTO jobDto = result.getFirst();
            try {
                if(result.getSecond().get()){
                    success++;
                }

                jobDto.setState(JobDTO.State.DONE);

            } catch (Exception e) {
                log.error("Error handling object ID '{}'", jobDto.getUuid(), e);
                failure++;
                jobDto.setState(JobDTO.State.ERROR);
            }
            jobsDAO.update(jobDto);
        }
    }

    private class Pair<T, S> {
        private T first;
        private S second;

        private Pair(T first, S second) {
            this.first = first;
            this.second = second;
        }

        public T getFirst() {
            return first;
        }

        public S getSecond() {
            return second;
        }
    }
}
