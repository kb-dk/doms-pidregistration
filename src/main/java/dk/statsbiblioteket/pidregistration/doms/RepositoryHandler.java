package dk.statsbiblioteket.pidregistration.doms;

import java.util.List;

/**
 * Find objects in repository, and update objects with extra IDs in repository.
 */
public interface RepositoryHandler {
    /**
     * Given an iTQL query that returns a list with PIDs as first element,
     * query the repository and return the list of PIDs.
     * @param query An iTQL query that returns a list with PIDs as first element
     * @return List of PIDs
     */
    public List<String> findObjectFromQuery(String query);

    /**
     * Given a repository PID, generate a handle, and update the repository
     * with that handle as an extra identifier.
     * @param pid A repository PID.
     * @return The generated handle.
     */
    public String addHandleToObject(String pid);
}
