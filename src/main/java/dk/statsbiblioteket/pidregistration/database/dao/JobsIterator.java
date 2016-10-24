package dk.statsbiblioteket.pidregistration.database.dao;

import dk.statsbiblioteket.pidregistration.database.DatabaseException;
import dk.statsbiblioteket.pidregistration.database.dto.JobDTO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class JobsIterator implements Iterator<JobDTO> {

    private ResultSet jobs;
    private JobsDAO jobsDAO;
    private JobDTO element;

    public JobsIterator(ResultSet jobs, JobsDAO jobsDAO) {
        this.jobs = jobs;
        this.jobsDAO = jobsDAO;
    }

    @Override
    public boolean hasNext() {
        if (element == null){
            try {
                if (jobs.next()) {
                    element = jobsDAO.resultSetToJobDTO(jobs);
                    return true;
                } else {
                    return false;
                }
            } catch (SQLException e) {
                throw new DatabaseException(e);
            }
        } else {
            return true;
        }
    }

    @Override
    public JobDTO next() {
        if (hasNext()){
            JobDTO result = element;
            element = null;
            return result;
        } else {
            throw new NoSuchElementException();
        }
    }
}
