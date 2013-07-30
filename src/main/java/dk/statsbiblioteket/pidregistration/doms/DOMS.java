package dk.statsbiblioteket.pidregistration.doms;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import dk.statsbiblioteket.doms.client.DomsWSClient;
import dk.statsbiblioteket.doms.client.DomsWSClientImpl;
import dk.statsbiblioteket.doms.client.exceptions.ServerOperationFailed;
import dk.statsbiblioteket.doms.webservices.authentication.Base64;
import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import java.util.ArrayList;
import java.util.List;

public class DOMS {
    private static final Client REST_CLIENT = Client.create();

    private static final String DC_DATASTREAM_ID = "DC";

    private final PropertyBasedRegistrarConfiguration config;

    public DOMS(PropertyBasedRegistrarConfiguration config) {
        this.config = config;
    }

    public List<String> findObjectsFromQuery(String query) {
        try {
            String objects = REST_CLIENT.resource(config.getFedoraLocation())
                    .path("/risearch").queryParam("type", "tuples")
                    .queryParam("lang", "sparql").queryParam("format", "CSV")
                    .queryParam("flush", "true").queryParam("stream", "on")
                    .queryParam("query", query)
                    .header("Authorization", getBase64Creds())
                    .post(String.class);
            String[] lines = objects.split("\n");
            List<String> foundObjects = new ArrayList<String>();
            for (String line : lines) {
                if (line.startsWith("\"")) {
                    continue;
                }
                if (line.startsWith("info:fedora/")) {
                    line = line.substring("info:fedora/".length());
                }
                if (line.indexOf(',') >= 0) {
                    line = line.substring(0, line.indexOf(','));
                }
                foundObjects.add(line);
            }
            return foundObjects;
        } catch (UniformInterfaceException e) {
            if (e.getResponse().getStatus() == ClientResponse.Status
                    .UNAUTHORIZED.getStatusCode()) {
                throw new BackendInvalidCredsException(
                        "Invalid Credentials Supplied", e);
            } else {
                throw new BackendMethodFailedException(
                        "Server error: " + e.getMessage(), e);
            }
        }
    }

    private String getBase64Creds() {
        return "Basic " + Base64.encodeBytes(
                (config.getUsername() + ":" + config.getPassword()).getBytes());
    }

    public Metadata getMetadataForObject(String objectId) {
        try {
            return new Metadata(getDomsClient().getDataStream(objectId, DC_DATASTREAM_ID));
        } catch (ServerOperationFailed serverOperationFailed) {
            throw new BackendMethodFailedException(
                    "Backendmethod failed while trying to to read DC from '"
                            + objectId + "'", serverOperationFailed);
        }
    }

    private DomsWSClient getDomsClient() {
        DomsWSClient domsClient = new DomsWSClientImpl();
        domsClient.setCredentials(config.getDomsWSAPIEndpoint(),
                                  config.getUsername(), config.getPassword());
        return domsClient;
    }


    public void updateMetadataForObject(String objectId, Metadata metadata) {
        try {
            DomsWSClient domsClient = getDomsClient();
            domsClient.unpublishObjects("Prepare to add handle PID", objectId);

            domsClient.updateDataStream(objectId, DC_DATASTREAM_ID, metadata.getNamespaceAwareDom(),
                                        "Added handle PID");

            domsClient.publishObjects("Prepare to add handle PID", objectId);
        } catch (ServerOperationFailed serverOperationFailed) {
            throw new BackendMethodFailedException(
                    "Backendmethod failed while trying to add handle to '" + objectId + "'",
                    serverOperationFailed);
        }
    }
}
