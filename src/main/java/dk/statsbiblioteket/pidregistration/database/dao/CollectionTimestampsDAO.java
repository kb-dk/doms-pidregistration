package dk.statsbiblioteket.pidregistration.database.dao;

import dk.statsbiblioteket.pidregistration.Collection;
import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

/**
 */
public class CollectionTimestampsDAO {
    private static final String GET_COLLECTION_TIMESTAMP =
            "SELECT latest_read FROM collection_timestamps WHERE collection=?";

    private static final String INSERT_COLLECTION_TIMESTAMP =
            "INSERT INTO collection_timestamps (collection, latest_read) VALUES (?, ?)";

    private static final String UPDATE_COLLECTION_TIMESTAMP =
            "UPDATE collection_timestamps SET latest_read=? where collection=?";

    private PropertyBasedRegistrarConfiguration configuration;
    private Connection connection;

    public CollectionTimestampsDAO(PropertyBasedRegistrarConfiguration configuration, Connection connection) {
        this.configuration = configuration;
        this.connection = connection;
    }

    public Date load(Collection collection) {
        try {
            PreparedStatement ps = connection.prepareStatement(GET_COLLECTION_TIMESTAMP);
            ps.setString(1, collection.getId());
            ResultSet resultSet = ps.executeQuery();
            Date result = null;
            if (resultSet.next()) {
                result = new Date(resultSet.getTimestamp("latest_read").getTime());
            }
            return result;
        } catch (SQLException e) {
            throw new DatabaseException(e.getNextException());
        }
    }

    public void save(Collection collection, Date latestRead) {
        try {
            PreparedStatement ps = connection.prepareStatement(INSERT_COLLECTION_TIMESTAMP);
            ps.setString(1, collection.getId());
            ps.setTimestamp(2, new Timestamp(latestRead.getTime()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(e.getNextException());
        }
    }

    public void update(Collection collection, Date latestRead) {
        try {
            PreparedStatement ps = connection.prepareStatement(UPDATE_COLLECTION_TIMESTAMP);
            ps.setTimestamp(1, new Timestamp(latestRead.getTime()));
            ps.setString(2, collection.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException(e.getNextException());
        }
    }
}
