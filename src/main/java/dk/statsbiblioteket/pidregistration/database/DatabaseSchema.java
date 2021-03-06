package dk.statsbiblioteket.pidregistration.database;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 */
public class DatabaseSchema {
    private static final Logger log = LoggerFactory.getLogger(DatabaseSchema.class);

    private ConnectionFactory connectionFactory;

    public DatabaseSchema(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void createIfNotExist() {
        if (!exists()) {
            log.info("Database schema did not exist. Creating...");
            executeScript("schema.ddl");
        }
    }

    private boolean exists() {
        try (Connection connection = connectionFactory.createConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            ResultSet resultSet = metadata.getTables("public", null, null, new String[]{"TABLE"});
            boolean exists = resultSet.next();
            return exists;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void executeScript(String filename) {
        try (
                Connection connection = connectionFactory.createConnection();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(DatabaseSchema.class.getResourceAsStream("/" + filename), "UTF-8"))
        ) {
            String str;
            String script = "";

            while ((str = in.readLine()) != null) {
                script += str + "\n";
            }

            Statement statement = connection.createStatement();
            statement.executeUpdate(script);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeIfExists() {
        if (exists()) {
            log.info("Database schema existed. Removing...");
            executeScript("removedb.ddl");
        }
    }
}
