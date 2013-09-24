package dk.statsbiblioteket.pidregistration.database;

import dk.statsbiblioteket.pidregistration.Collection;
import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.dao.JobDAO;
import dk.statsbiblioteket.pidregistration.database.dto.JobDTO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

/**
 */
public class Dummy {
    private static final PropertyBasedRegistrarConfiguration CONFIG
            = new PropertyBasedRegistrarConfiguration(
            Dummy.class.getResourceAsStream("/pidregistration.properties"));

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        Connection connection = connectionFactory.createConnection();

        connection.setAutoCommit(false);
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

        JobDAO jobDAO = new JobDAO(CONFIG, connection);

        Collection collection = null;
        for (Collection c : CONFIG.getDomsCollections()) {
            collection = c;
        }

        jobDAO.save(
                Arrays.asList(
                        new JobDTO("uuid:bff36b9a-a38e-4cf8-a03a-efe6c7a58f4a", collection, JobDTO.State.PENDING),
                        new JobDTO("uuid:bff36b9a-a38e-4cf8-a03a-efe6c7a58f4b", collection, JobDTO.State.PENDING)
                )
        );

        connection.commit();
        connection.setAutoCommit(true);


        JobDTO jobDto = jobDAO.findPendingJob();

        jobDto.setState(JobDTO.State.DONE);

        connection.setAutoCommit(false);
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

        jobDAO.update(jobDto);

        connection.commit();

        connection.close();
    }
}
