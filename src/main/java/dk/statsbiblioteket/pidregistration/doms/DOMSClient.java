package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.CentralWebservice;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.CentralWebserviceService;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.util.Map;

public class DOMSClient {
    private static final QName CENTRAL_WEBSERVICE_SERVICE = new QName(
            "http://central.doms.statsbiblioteket.dk/",
            "CentralWebserviceService");

    private static final String DC_DATASTREAM_ID = "DC";

    private final CentralWebservice centralWebservice;

    public DOMSClient(PropertyBasedRegistrarConfiguration config) {
        centralWebservice =
                new CentralWebserviceService(config.getDomsWSAPIEndpoint(), CENTRAL_WEBSERVICE_SERVICE)
                        .getCentralWebservicePort();

        Map<String, Object> domsAPILogin = ((BindingProvider) centralWebservice)
                .getRequestContext();
        domsAPILogin.put(BindingProvider.USERNAME_PROPERTY, config.getUsername());
        domsAPILogin.put(BindingProvider.PASSWORD_PROPERTY, config.getPassword());
    }

    public CentralWebservice getCentralWebservice() {
        return centralWebservice;
    }

    public String getDatastreamId() {
        return DC_DATASTREAM_ID;
    }
}
