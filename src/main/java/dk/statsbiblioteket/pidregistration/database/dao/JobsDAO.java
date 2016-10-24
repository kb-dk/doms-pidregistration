package dk.statsbiblioteket.pidregistration.database.dao;

import dk.statsbiblioteket.pidregistration.Collection;
import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.DatabaseException;
import dk.statsbiblioteket.pidregistration.database.dto.JobDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 */
public class JobsDAO {

    private static final String INSERT_JOB =
            "INSERT INTO jobs (uuid, collection, state, created, last_state_change) VALUES (?, ?, ?::job_state, ?, ?)";

    private static final String UPDATE_JOB =
            "UPDATE jobs SET uuid=?,collection=?,state=?::job_state,created=?,last_state_change=? where id=?";

    private static final String GET_JOBS_WITH_STATE =
            "SELECT id, uuid, collection, state, created, last_state_change FROM jobs where state=?::job_state limit ?";

    private static final String GET_JOBS_WITH_UUID =
            "SELECT id, uuid, collection, state, created, last_state_change FROM jobs where uuid=?";

    private Connection connection;
    private PropertyBasedRegistrarConfiguration configuration;


    public JobsDAO(PropertyBasedRegistrarConfiguration configuration, Connection connection) {
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
                ps.setString(2, jobDto.getCollectionId());
                ps.setString(3, jobDto.getState().getDatabaseStateName());
                ps.setTimestamp(4, new Timestamp(jobDto.getCreated().getTime()));
                ps.setTimestamp(5, new Timestamp(jobDto.getLastStateChange().getTime()));
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            throw new DatabaseException(e.getNextException());
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
                ps.setString(2, jobDto.getCollectionId());
                ps.setString(3, jobDto.getState().getDatabaseStateName());
                ps.setTimestamp(4, new Timestamp(jobDto.getCreated().getTime()));
                ps.setTimestamp(5, new Timestamp(jobDto.getLastStateChange().getTime()));
                ps.setInt(6, jobDto.getId());
                ps.addBatch();
            }

            ps.executeBatch();
        } catch (SQLException e) {
            throw new DatabaseException(e.getNextException());
        }
    }

    private PreparedStatement buildPreparedStatement(String query) {
        try {
            return connection.prepareStatement(query);
        } catch (SQLException e) {
            throw new DatabaseException(e.getNextException());
        }
    }

    public JobDTO findJobDone() {
        return findJob(JobDTO.State.DONE);
    }

    public JobDTO findJobPending() {
        return findJob(JobDTO.State.PENDING);
    }

    public Iterator<JobDTO> findJobsPending(int batchSizeLimit) {
        return findJobs(JobDTO.State.PENDING, batchSizeLimit);
    }

    public JobDTO findJobError() {
        return findJob(JobDTO.State.ERROR);
    }

    public JobDTO findJob(JobDTO.State state) {
        try {
            PreparedStatement ps = buildPreparedStatement(GET_JOBS_WITH_STATE);
            ps.setString(1, state.getDatabaseStateName());
            ps.setInt(2, 1);
            ResultSet resultSet = ps.executeQuery();
            JobDTO result = null;
            if (resultSet.next()) {
                result = resultSetToJobDTO(resultSet);
            }
            return result;

        } catch (SQLException e) {
            throw new DatabaseException(e.getNextException());
        }
    }

    public Iterator<JobDTO> findJobs(JobDTO.State state, int batchSizeLimit) {
        try {
            PreparedStatement ps = buildPreparedStatement(GET_JOBS_WITH_STATE);
            ps.setString(1, state.getDatabaseStateName());
            ps.setInt(2, batchSizeLimit);
            ResultSet resultSet = ps.executeQuery();
            return new JobsIterator(resultSet);

        } catch (SQLException e) {
            throw new DatabaseException(e.getNextException());
        }
    }

    public static JobDTO resultSetToJobDTO(ResultSet resultSet) throws SQLException {
        JobDTO result = new JobDTO();
        result.setId(resultSet.getInt("id"));
        result.setCollectionId(resultSet.getString("collection"));
        result.setUuid(resultSet.getString("uuid"));
        result.setState(findStateWithID(resultSet.getString("state")));
        result.setCreated(new Date(resultSet.getTimestamp("created").getTime()));
        result.setLastStateChange(new Date(resultSet.getTimestamp("last_state_change").getTime()));
        return result;
    }

    public JobDTO findJobWithUUID(String uuid) {
        try {
            PreparedStatement ps = buildPreparedStatement(GET_JOBS_WITH_UUID);
            ps.setString(1, uuid);
            ResultSet resultSet = ps.executeQuery();
            JobDTO result = null;
            if (resultSet.next()) {
                result = resultSetToJobDTO(resultSet);
            }
            return result;
        } catch (SQLException e) {
            throw new DatabaseException(e.getNextException());
        }
    }

    private static JobDTO.State findStateWithID(String id) {
        for (JobDTO.State state : JobDTO.State.values()) {
            if (state.getDatabaseStateName().equals(id)) {
                return state;
            }
        }
        return null;
    }
}
