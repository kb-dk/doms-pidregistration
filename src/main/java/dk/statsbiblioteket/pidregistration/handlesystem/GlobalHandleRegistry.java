package dk.statsbiblioteket.pidregistration.handlesystem;

import dk.statsbiblioteket.pidregistration.PIDHandle;
import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import net.handle.hdllib.AbstractMessage;
import net.handle.hdllib.AbstractRequest;
import net.handle.hdllib.AbstractResponse;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.security.PrivateKey;

/**
 * Responsible for communications with the global handle registry
 */
public class GlobalHandleRegistry {
    private static final Logger log = LoggerFactory.getLogger(GlobalHandleRegistry.class);

    private static final String ADMIN_ID_PREFIX = "0.NA/";
    private static final Charset DEFAULT_ENCODING = Charset.forName("UTF8");
    private static final int ADMIN_ID_INDEX = 300;
    private static final int ADMIN_RECORD_INDEX = 200;
    private static final int URL_RECORD_INDEX = 1;
    private final PropertyBasedRegistrarConfiguration config;

    private HandleResolver handleResolver;
    private HandleRequestBuilder handleRequestBuilder;

    public GlobalHandleRegistry(PropertyBasedRegistrarConfiguration config) {
        this.config = config;
        String adminId = ADMIN_ID_PREFIX + config.getHandlePrefix();

        PublicKeyAuthenticationInfo pubKeyAuthInfo = new PublicKeyAuthenticationInfo(
                adminId.getBytes(DEFAULT_ENCODING), ADMIN_ID_INDEX, loadPrivateKey());

        handleResolver = new HandleResolver();
        handleRequestBuilder = new HandleRequestBuilder(adminId,
                                            ADMIN_ID_INDEX,
                                            ADMIN_RECORD_INDEX,
                                            URL_RECORD_INDEX,
                                            DEFAULT_ENCODING,
                                            pubKeyAuthInfo);
    }

    private PrivateKey loadPrivateKey() throws PrivateKeyException {
        String path = config.getPrivateKeyPath();
        String password = config.getPrivateKeyPassword();
        PrivateKeyLoader privateKeyLoader =
                path == null ? new PrivateKeyLoader(password) : new PrivateKeyLoader(password, path);
        return privateKeyLoader.load();
    }

    /**
     * Registers/Updates the URL in the global handle registry.
     * @param handle the handle
     * @param url the URL
     * @return true if URL was registered. False if nothing was done because the same URL already was
     * already registered on the handle
     * @throws RegisteringPidFailedException external server failed
     */
    public boolean registerPid(PIDHandle handle, String url)
            throws RegisteringPidFailedException {
        log.info("Registering handle '" + handle + "' for url '" + url + "' in global handle registry");
        HandleValue[] values = lookupHandle(handle);

        if (values == null) {
            log.info("Handle '" + handle + "' was previously unknown. Adding with url '" + url + "'");
            createPidWithUrl(handle, url);
            return true;
        }

        String urlAtServer = findFirstWithTypeUrl(values);
        if (urlAtServer == null) {
            log.info("Handle '" + handle + "' already registered, but with no url. Adding '" + url + "'");
            addUrlToPid(handle, url);
            return true;
        }

        if (!urlAtServer.equalsIgnoreCase(url)) {
            log.info("Handle '" + handle + "' already registered with different" + " url '" + urlAtServer + "'. Replacing with '" + url + "'");
            replaceUrlOfPid(handle, url);
            return true;
        }

        log.info("Handle '" + handle + "' already registered with url '" + url + "'. Doing nothing.");
        return false;
    }

    /**
     * Delete a handle in the global handle registry.
     * @param handle the handle to delete
     * @throws RegisteringPidFailedException external server failed
     */
    public void deletePid(PIDHandle handle) {
        AbstractRequest request = handleRequestBuilder.buildDeleteHandleRequest(handle);
        processRequest(request);
    }

    private HandleValue[] lookupHandle(PIDHandle handle) {
        try {
            return handleResolver.resolveHandle(handle.asString());
        } catch (HandleException e) {
            if (e.getCode() == HandleException.HANDLE_DOES_NOT_EXIST) {
                return null;
            } else {
                throw new RegisteringPidFailedException(
                        "Did not succeed in resolving handle, existing or not.",
                        e);
            }
        }
    }

    private String findFirstWithTypeUrl(HandleValue[] handleValues) {
        for (HandleValue value : handleValues) {
            String type = value.getTypeAsString().toUpperCase();
            int index = value.getIndex();
            if (index == URL_RECORD_INDEX && type.equals("URL")) {
                return value.getDataAsString();
            }
        }
        return null;
    }

    private void createPidWithUrl(PIDHandle handle, String url)
            throws RegisteringPidFailedException {
        AbstractRequest request = handleRequestBuilder.buildCreateHandleRequest(handle, url);
        processRequest(request);
    }

    private void addUrlToPid(PIDHandle handle, String url)
            throws RegisteringPidFailedException {
        AbstractRequest request = handleRequestBuilder.buildAddUrlRequest(handle, url);
        processRequest(request);
    }

    private void replaceUrlOfPid(PIDHandle handle, String url)
            throws RegisteringPidFailedException {
        AbstractRequest request = handleRequestBuilder.buildModifyUrlRequest(handle, url);
        processRequest(request);
    }

    private void processRequest(AbstractRequest request) {
        try {
            long start = System.currentTimeMillis();
            AbstractResponse response = handleResolver.processRequest(request);
            if (response.responseCode != AbstractMessage.RC_SUCCESS) {
                throw new RegisteringPidFailedException(
                        "Failed trying to register a handle at the server, response was" + response);
            }
            long end = System.currentTimeMillis();

            log.debug("processing handle request (" + request.getClass().getName() + ") took " + (end - start) + " ms.");

        } catch (HandleException e) {
            throw new RegisteringPidFailedException(
                    "Could not process the request to register a handle at the server.",
                    e);
        }
    }
}
