package dk.statsbiblioteket.pidregistration.database.dao;

import dk.statsbiblioteket.pidregistration.database.DatabaseException;
import dk.statsbiblioteket.pidregistration.database.dto.JobDTO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

public class JobsIterator implements Iterator {

    private ResultSet jobs;
    private JobsDAO jobsDAO;

    public JobsIterator(ResultSet jobs, JobsDAO jobsDAO) {
        this.jobs = jobs;
        this.jobsDAO = jobsDAO;
    }

    @Override
    public boolean hasNext() {
        try {
            return jobs.isLast() || jobs.isAfterLast();
        } catch (SQLException e) {
            throw new DatabaseException(e.getNextException());
        }
    }

    @Override
    public JobDTO next() {
        JobDTO result = null;
        try {
            if(jobs.next()){
                result = jobsDAO.resultSetToJobDTO(jobs);
            }
        } catch (SQLException e) {
            throw new DatabaseException(e.getNextException());
        }
        return result;
    }
}
