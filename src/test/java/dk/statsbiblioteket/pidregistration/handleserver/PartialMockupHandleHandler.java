package dk.statsbiblioteket.pidregistration.handleserver;

import dk.statsbiblioteket.pidregistration.configuration.RegistrarConfiguration;

/**
 * Version of HandleHandler that mocks up the three updating methods, and
 * counts invocations and remembers parameters in public variables.
 */
public class PartialMockupHandleHandler extends HandleHandler {
    public int addUrlToPidAtServerCount;
    public String addUrlToPidAtServerPid;
    public String addUrlToPidAtServerUrl;
    public int replaceUrlOfPidAtServerCount;
    public String replaceUrlOfPidAtServerPid;
    public String replaceUrlOfPidAtServerUrl;
    public int addPidToServerCount;
    public String addPidToServerPid;
    public String addPidToServerUrl;

    /**
     * Initialize handle handler.
     *
     * @param config The configuration used.
     */
    public PartialMockupHandleHandler(RegistrarConfiguration config) {
        super(config);
    }

    @Override
    public void addUrlToPidAtServer(String pid, String url)
            throws RegisteringPidFailedException {
        addUrlToPidAtServerCount++;
        addUrlToPidAtServerPid = pid;
        addUrlToPidAtServerUrl = url;
    }

    @Override
    public void replaceUrlOfPidAtServer(String pid, String url)
            throws RegisteringPidFailedException {
        replaceUrlOfPidAtServerCount++;
        replaceUrlOfPidAtServerPid = pid;
        replaceUrlOfPidAtServerUrl = url;
    }

    @Override
    public void addPidToServer(String pid, String url)
            throws RegisteringPidFailedException {
        addPidToServerCount++;
        addPidToServerPid = pid;
        addPidToServerUrl = url;
    }
}
