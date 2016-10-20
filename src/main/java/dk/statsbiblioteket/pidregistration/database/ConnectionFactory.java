package dk.statsbiblioteket.pidregistration.database;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.dao.CollectionTimestampsDAO;
import dk.statsbiblioteket.pidregistration.database.dao.JobsDAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 */
public class ConnectionFactory {
    private PropertyBasedRegistrarConfiguration configuration;
    private JobsDAO jobsDAO;
    private CollectionTimestampsDAO collectionTimestampsDAO;

    public ConnectionFactory(PropertyBasedRegistrarConfiguration configuration) {
        this.configuration = configuration;
    }

    public Connection createConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            Connection connection = DriverManager.getConnection(
                    configuration.getDatabaseUrl(),
                    configuration.getDatabaseUsername(),
                    configuration.getDatabasePassword()
            );
            jobsDAO = new JobsDAO(configuration, connection);
            collectionTimestampsDAO = new CollectionTimestampsDAO(configuration, connection);
            return connection;
        } catch (ClassNotFoundException e) {
            throw new DatabaseException(e);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    public JobsDAO getJobsDAO() {
        return jobsDAO;
    }

    public CollectionTimestampsDAO getCollectionTimestampsDAO() {
        return collectionTimestampsDAO;
    }
}
