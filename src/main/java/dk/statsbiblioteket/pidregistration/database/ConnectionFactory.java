package dk.statsbiblioteket.pidregistration.database;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 */
public class ConnectionFactory {
    private PropertyBasedRegistrarConfiguration configuration;

    public ConnectionFactory(PropertyBasedRegistrarConfiguration configuration) {
        this.configuration = configuration;
    }

    public Connection createConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(
                    configuration.getDatabaseUrl(),
                    configuration.getDatabaseUsername(),
                    configuration.getDatabasePassword()
            );
        } catch (ClassNotFoundException e) {
            throw new DatabaseException(e);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }
}
