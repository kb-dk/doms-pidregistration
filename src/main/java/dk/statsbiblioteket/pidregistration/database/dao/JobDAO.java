package dk.statsbiblioteket.pidregistration.database.dao;

import dk.statsbiblioteket.pidregistration.Collection;
import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.dto.JobDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 */
public class JobDAO {

    private static final String INSERT_JOB =
            "INSERT INTO job (uuid, collection, state, last_state_change) VALUES (?, ?, ?::job_state, ?)";

    private static final String UPDATE_JOB =
            "UPDATE job SET uuid=?,collection=?,state=?::job_state,last_state_change=? where id=?";


    private static final String GET_JOBS_WITH_STATE =
            "SELECT id, uuid, collection, state, last_state_change FROM job where state=?::job_state limit ?";

    private static final String GET_JOBS_WITH_UUID =
            "SELECT id, uuid, collection, state, last_state_change FROM job where uuid=?";

    private Connection connection;
    private PropertyBasedRegistrarConfiguration configuration;


    public JobDAO(PropertyBasedRegistrarConfiguration configuration, Connection connection) {
        this.configuration = configuration;
        this.connection = connection;
    }

    public void save(JobDTO jobDto) {
        save(Arrays.asList(jobDto));
    }

    public void save(List<JobDTO> jobDtos) {
        try {
            PreparedStatement ps = buildPreparedStatement(INSERT_JOB);

            for (JobDTO jobDto : jobDtos) {
                ps.setString(1, jobDto.getUuid());
                ps.setString(2, jobDto.getCollection().getId());
                ps.setString(3, jobDto.getState().getDatabaseStateName());
                ps.setTimestamp(4, new Timestamp(jobDto.getLastStateChange().getTime()));
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(JobDTO jobDto) {
        update(Arrays.asList(jobDto));
    }

    public void update(List<JobDTO> jobDtos) {
        try {
            PreparedStatement ps = buildPreparedStatement(UPDATE_JOB);

            for (JobDTO jobDto : jobDtos) {
                ps.setString(1, jobDto.getUuid());
                ps.setString(2, jobDto.getCollection().getId());
                ps.setString(3, jobDto.getState().getDatabaseStateName());
                ps.setTimestamp(4, new Timestamp(jobDto.getLastStateChange().getTime()));
                ps.setInt(5, jobDto.getId());
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e.getNextException());
        }
    }

    private PreparedStatement buildPreparedStatement(String query) {
        try {
            return connection.prepareStatement(query);
        } catch (SQLException e) {
            throw new RuntimeException(e.getNextException());
        }
    }

    public JobDTO findPendingJob() {
        return findJobWith(JobDTO.State.PENDING);
    }

    public JobDTO findJobWith(JobDTO.State state) {
        try {
            PreparedStatement ps = buildPreparedStatement(GET_JOBS_WITH_STATE);
            ps.setString(1, state.getDatabaseStateName());
            ps.setInt(2, 1);
            ResultSet resultSet = ps.executeQuery();
            JobDTO result = null;
            if (resultSet.next()) {
                result = new JobDTO();
                result.setId(resultSet.getInt("id"));
                result.setCollection(findCollectionWithId(resultSet.getString("collection")));
                result.setUuid(resultSet.getString("uuid"));
                result.setState(findStateWithID(resultSet.getString("state")));
                result.setLastStateChange(new Date(resultSet.getTimestamp("last_state_change").getTime()));
            }
            return result;

        } catch (SQLException e) {
            throw new RuntimeException(e.getNextException());
        }
    }

    public JobDTO findJobWithUUID(String uuid) {
        try {
            PreparedStatement ps = buildPreparedStatement(GET_JOBS_WITH_UUID);
            ps.setString(1, uuid);
            ResultSet resultSet = ps.executeQuery();
            JobDTO result = null;
            if (resultSet.next()) {
                result = new JobDTO();
                result.setId(resultSet.getInt("id"));
                result.setCollection(findCollectionWithId(resultSet.getString("collection")));
                result.setUuid(resultSet.getString("uuid"));
                result.setState(findStateWithID(resultSet.getString("state")));
                result.setLastStateChange(new Date(resultSet.getTimestamp("last_state_change").getTime()));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e.getNextException());
        }
    }

    private Collection findCollectionWithId(String id) {
        for (Collection collection : configuration.getDomsCollections()) {
            if (collection.getId().equals(id)) {
                return collection;
            }
        }
        return null;
    }

    private JobDTO.State findStateWithID(String id) {
        for (JobDTO.State state : JobDTO.State.values()) {
            if (state.getDatabaseStateName().equals(id)) {
                return state;
            }
        }
        return null;
    }
}
