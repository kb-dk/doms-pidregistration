package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.Collection;
import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.CentralWebservice;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.CentralWebserviceService;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidCredentialsException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidResourceException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.MethodFailedException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.RecordDescription;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Central web service methods used in this project
 */
public class DOMSClient {
    private static final QName CENTRAL_WEBSERVICE_SERVICE = new QName(
            "http://central.doms.statsbiblioteket.dk/",
            "CentralWebserviceService");

    private static final String DC_DATASTREAM_ID = "DC";

    private final CentralWebservice centralWebservice;
    private int maxDomsResultSize;

    public DOMSClient(PropertyBasedRegistrarConfiguration config) {
        centralWebservice =
                new CentralWebserviceService(config.getDomsWSAPIEndpoint(), CENTRAL_WEBSERVICE_SERVICE)
                        .getCentralWebservicePort();

        Map<String, Object> domsAPILogin = ((BindingProvider) centralWebservice)
                .getRequestContext();
        domsAPILogin.put(BindingProvider.USERNAME_PROPERTY, config.getUsername());
        domsAPILogin.put(BindingProvider.PASSWORD_PROPERTY, config.getPassword());
        maxDomsResultSize = config.getDomsMaxResultSize();
    }

    public String getDatastreamContents(String objectId) throws MethodFailedException, InvalidResourceException, InvalidCredentialsException {
        return getCentralWebservice().getDatastreamContents(objectId, DC_DATASTREAM_ID);
    }

    public List<RecordDescription> getIDsModified(long sinceExclusive, Collection collection) throws InvalidCredentialsException, MethodFailedException {
        return getCentralWebservice().getIDsModified(
                sinceExclusive, collection.getId(), "SummaVisible", "Published", 0, maxDomsResultSize);
    }

    public void markInProgressObject(String objectId) throws MethodFailedException, InvalidResourceException, InvalidCredentialsException {
        getCentralWebservice().markInProgressObject(Arrays.asList(objectId), "Prepare to add handle PID");
    }

    public void modifyDatastream(String objectId, DOMSMetadata metadata) throws MethodFailedException, InvalidResourceException, InvalidCredentialsException {
        getCentralWebservice().modifyDatastream(objectId, DC_DATASTREAM_ID, metadata.getMetadata(), "Adding handle PID");
    }

    public void markPublishedObject(String objectId) throws MethodFailedException, InvalidResourceException, InvalidCredentialsException {
        getCentralWebservice().markPublishedObject(Arrays.asList(objectId), "Done adding handle PID");
    }

    public boolean isActive(String objectId) throws MethodFailedException, InvalidResourceException, InvalidCredentialsException {
        String state = getCentralWebservice().getObjectProfile(objectId).getState();
        return "A".equals(state);
    }

    private CentralWebservice getCentralWebservice() {
        return centralWebservice;
    }
}
