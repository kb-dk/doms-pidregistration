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
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Central web service methods used in this project
 */
public class DOMSClient {
    private static final QName CENTRAL_WEBSERVICE_SERVICE = new QName(
            "http://central.doms.statsbiblioteket.dk/",
            "CentralWebserviceService");

    private static final String CONNECT_TIMEOUT = "com.sun.xml.ws.connect.timeout";
    private static final String REQUEST_TIMEOUT = "com.sun.xml.ws.request.timeout";

    private static final String DC_DATASTREAM_ID = "DC";

    private PropertyBasedRegistrarConfiguration configuration;

    private CentralWebservice centralWebservice;
    private int maxDomsResultSize;

    public DOMSClient(PropertyBasedRegistrarConfiguration configuration) {
        this.configuration = configuration;
        maxDomsResultSize = configuration.getDomsMaxResultSize();
    }

    public String getDatastreamContents(String objectId) throws MethodFailedException, InvalidResourceException, InvalidCredentialsException {
        return getCentralWebservice().getDatastreamContents(objectId, DC_DATASTREAM_ID);
    }

    private CentralWebservice getCentralWebservice() {
        if (centralWebservice == null) {
            disableEntityExpansionLimit();
            centralWebservice =
                    new CentralWebserviceService(configuration.getDomsWSAPIEndpoint(), CENTRAL_WEBSERVICE_SERVICE)
                            .getCentralWebservicePort();
            Map<String, Object> context = ((BindingProvider) centralWebservice).getRequestContext();
            context.put(BindingProvider.USERNAME_PROPERTY, configuration.getUsername());
            context.put(BindingProvider.PASSWORD_PROPERTY, configuration.getPassword());
            context.put(CONNECT_TIMEOUT, configuration.getDomsWSAPIEndpointTimeout());
            context.put(REQUEST_TIMEOUT, configuration.getDomsWSAPIEndpointTimeout());
        }
        return centralWebservice;
    }

    private void disableEntityExpansionLimit() {
        // JDK 1.7 u45+ enables a security feature per default that limits the number of entity expansions allowed
        // This causes JAX-WS to fail after having run for a while.
        System.getProperties().setProperty("jdk.xml.entityExpansionLimit", "0");
    }

    public List<RecordDescription> getIDsModified(Date sinceInclusive, Collection collection) throws InvalidCredentialsException, MethodFailedException {
        return getCentralWebservice().getIDsModified(
                sinceInclusive.getTime(), collection.getDomsName(), "SummaVisible", "Published", 0, maxDomsResultSize);
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

    public int getMaxDomsResultSize() {
        return maxDomsResultSize;
    }
}
