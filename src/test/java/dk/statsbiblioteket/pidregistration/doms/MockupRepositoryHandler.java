package dk.statsbiblioteket.pidregistration.doms;

import java.util.Arrays;
import java.util.List;

/**
 * A mockup implementation, which on any search will return a list containing
 * the only the pid "info:fedora/foo", and on any call to update with that pid
 * will succeed and return the handle 109.3.1/foo, and fail on any other
 * pid.
 *
 * Calls will be counted and last parameters tracked in public field variables.
 */
public class MockupRepositoryHandler implements RepositoryHandler {
    public int findObjectFromQueryCount;
    public String findObjectFromQueryQuery;
    public int addHandleToObjectCount;
    public String addHandleToObjectPid;

    @Override
    public List<String> findObjectFromQuery(String query)
            throws BackendInvalidCredsException, BackendMethodFailedException {
        findObjectFromQueryCount++;
        findObjectFromQueryQuery = query;
        return Arrays.asList("info:fedora/foo");
    }

    @Override
    public String addHandleToObject(String pid) {
        addHandleToObjectCount++;
        addHandleToObjectPid = pid;
        if (!pid.equals("info:fedora/foo")) {
            throw new BackendMethodFailedException("Mockup fail");
        }
        return "109.3.1/foo";
    }
}
