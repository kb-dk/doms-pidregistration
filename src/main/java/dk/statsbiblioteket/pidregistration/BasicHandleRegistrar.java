package dk.statsbiblioteket.pidregistration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/** Use DomsHandler for performing Queries and adding identifiers
 * and a HandleHandler instance for registering handles. */
public class BasicHandleRegistrar implements HandleRegistrar {
    private final RepositoryHandler repositoryHandler;
    private final PidResolverHandler pidResolverHandler;
    private final Log log = LogFactory.getLog(getClass());
    private int success = 0;
    private int failure = 0;

    public BasicHandleRegistrar(RepositoryHandler repositoryHandler,
                                PidResolverHandler pidResolverHandler) {
        this.repositoryHandler = repositoryHandler;
        this.pidResolverHandler = pidResolverHandler;
    }

    @Override
    public String addHandles(String query, String urlPattern) {
        List<String> pids = repositoryHandler.findObjectFromQuery(query);
        for (String pid : pids) {
            try {
                log.debug("Adding handle to '" + pid + "'");
                String handle = repositoryHandler.addHandleToObject(pid);
                log.debug("registering handle '" + handle + "'");
                pidResolverHandler.registerPid(pid, handle, urlPattern);
                log.info("Added handle '" + handle + "' for pid '" + pid
                                 + "' using url pattern '" + urlPattern + "'");
                success++;
            } catch (Exception e) {
                failure++;
                log.error("Error handling pid'" + pid + "'", e);
            }
        }
        String message = "Done adding handles. #success: " + success
                + " #failure: " + failure;
        log.info(message);
        return message;
    }
}
