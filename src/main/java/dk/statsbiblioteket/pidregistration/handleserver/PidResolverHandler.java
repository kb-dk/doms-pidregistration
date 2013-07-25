package dk.statsbiblioteket.pidregistration.handleserver;

/** Register and updated resolving infor for PIDs. */
public interface PidResolverHandler {
    /**
     * Register pid in the pid-server, so that it resolves to a URL
     * generated from the given urlPattern.
     *
     * @param repositoryId The ID of the repository object in question.
     * @param pid          The pid to be registered.
     * @param urlPattern   The URL-pattern that makes a repository ID into a
     *                     URL.
     * @throws RegisteringPidFailedException If resolving the pid failed
     *                                       unexpectedly.
     */
    void registerPid(String repositoryId, String pid, String urlPattern)
            throws RegisteringPidFailedException;

    /**
     * Add URL to given pid at the server.
     *
     * @param pid PID which needs to have url added
     * @param url The url to be used in the pid
     * @throws RegisteringPidFailedException In case we couldn't add URL
     *                                       to pid at the server.
     */
    void addUrlToPidAtServer(String pid, String url)
            throws RegisteringPidFailedException;

    /**
     * Replace the URL of given pid at the server.
     *
     *
     * @param pid             PID, the url of which has to be replaced
     * @param url             The new url, to be used in the pid instead of the
     *                        existing
     * @throws RegisteringPidFailedException In case we couldn't replace URL
     *                                       of pid at the server.
     */
    void replaceUrlOfPidAtServer(String pid, String url)
            throws RegisteringPidFailedException;

    /**
     * Add given pid to the pid server, so that the pid resolves to the
     * given url.
     *
     * @param pid PID to be added to pid server.
     * @param url URL that the pid should resolve to.
     * @throws RegisteringPidFailedException In case we couldn't create a new
     *                                       pid at the server.
     */
    void addPidToServer(String pid, String url)
            throws RegisteringPidFailedException;
}
