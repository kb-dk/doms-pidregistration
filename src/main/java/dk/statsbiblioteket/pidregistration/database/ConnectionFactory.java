package dk.statsbiblioteket.pidregistration.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 */
public class ConnectionFactory {
    public Connection createConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(
                    "jdbc:postgresql://hsm-ubuntu.sb.statsbiblioteket.dk:5432/pid", "pid", "pid");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
