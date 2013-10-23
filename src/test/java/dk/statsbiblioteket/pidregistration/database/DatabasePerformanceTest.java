package dk.statsbiblioteket.pidregistration.database;

import dk.statsbiblioteket.pidregistration.Collection;
import dk.statsbiblioteket.pidregistration.PIDRegistrationsIntegrationTest;
import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.dao.JobsDAO;
import dk.statsbiblioteket.pidregistration.database.dto.JobDTO;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Ignore
public class DatabasePerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(DatabasePerformanceTest.class);

    private static final PropertyBasedRegistrarConfiguration CONFIG
            = new PropertyBasedRegistrarConfiguration(
            PIDRegistrationsIntegrationTest.class.getResourceAsStream("/doms-pidregistration.properties"));

    private Connection connection;
    private JobsDAO jobsDao;

    @Test
    public void testPerformance() {
        DatabaseSchema databaseSchema = new DatabaseSchema(CONFIG);
        databaseSchema.removeIfExists();
        databaseSchema.createIfNotExist();
        setupConnection();

        int jobCount = 0;
        log.info("Adding jobs to database");
        Set<Collection> collections = CONFIG.getDomsCollections();
        for (Collection collection : collections) {
            for (int i = 0; i < 100; i++) {
                beginTransaction();
                int jobsAdded = persistObjects(collection, generate1000ObjectIds());
                commitTransaction();
                jobCount += jobsAdded;
                log.info("Jobs added so far: {}", jobCount);
            }
        }
        log.info("Added {} jobs", jobCount);
        log.info("Adding handles");
        teardownConnection();
    }

    private List<String> generate1000ObjectIds() {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < 10000; i++) {
            result.add("uuid:" + UUID.randomUUID());
        }
        return result;
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
        connection = new ConnectionFactory(CONFIG).createConnection();
        jobsDao = new JobsDAO(CONFIG, connection);
    }

    private void teardownConnection() {
        try {
            jobsDao = null;
            connection.close();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    private int persistObjects(Collection collection, List<String> objectIds) {
        List<JobDTO> jobDtos = new ArrayList<JobDTO>();
        int count = 0;
        for (String objectId : objectIds) {
            if (jobsDao.findJobWithUUID(objectId) != null) {
                log.debug("Job with UUID " + objectId + " already exists in job list. Ignoring");
            } else {
                jobDtos.add(new JobDTO(objectId, collection, JobDTO.State.PENDING));
                count++;
            }
        }

        jobsDao.save(jobDtos);
        return count;
    }
}
