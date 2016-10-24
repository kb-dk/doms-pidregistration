package dk.statsbiblioteket.pidregistration.database.dao;

import dk.statsbiblioteket.pidregistration.database.DatabaseException;
import dk.statsbiblioteket.pidregistration.database.dto.JobDTO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class JobsIterator implements Iterator<JobDTO> {

    private ResultSet jobs;
    private JobDTO element;

    public JobsIterator(ResultSet jobs) {
        this.jobs = jobs;
    }

    @Override
    public boolean hasNext() {
        if (element == null){
            try {
                if (jobs.next()) {
                    element = JobsDAO.resultSetToJobDTO(jobs);
                    return true;
                } else {
                    jobs.close();
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
