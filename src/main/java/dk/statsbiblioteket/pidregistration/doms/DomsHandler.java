package dk.statsbiblioteket.pidregistration.doms;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import dk.statsbiblioteket.pidregistration.configuration.RegistrarConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import dk.statsbiblioteket.doms.client.DomsWSClient;
import dk.statsbiblioteket.doms.client.DomsWSClientImpl;
import dk.statsbiblioteket.doms.client.exceptions.ServerOperationFailed;
import dk.statsbiblioteket.doms.webservices.authentication.Base64;
import dk.statsbiblioteket.util.xml.DefaultNamespaceContext;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

/** Methods for accessing Fedora */
public class DomsHandler implements RepositoryHandler {
    private static final Client REST_CLIENT = Client.create();

    private static final String HANDLE_URI_NAMESPACE = "hdl:";
    private static final String DC_DATASTREAM_ID = "DC";
    private static final String DC_IDENTIFIER_ELEMENT = "identifier";
    private static final String DC_PREFIX = "dc";
    private static final String DC_NAMESPACE_URI
            = "http://purl.org/dc/elements/1.1/";
    private static final NamespaceContext dsNamespaceContext
            = new DefaultNamespaceContext();
    static {
        ((DefaultNamespaceContext) dsNamespaceContext)
                .setNameSpace(DC_NAMESPACE_URI,
                              DC_PREFIX);
    }

    private final RegistrarConfiguration config;

    public DomsHandler(RegistrarConfiguration config) {
        this.config = config;
    }

    @Override
    public List<String> findObjectFromQuery(String query) {
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

    @Override
    public String addHandleToObject(String pid) {
        DomsWSClient domsClient = new DomsWSClientImpl();
        domsClient.setCredentials(config.getDomsWSAPIEndpoint(),
                                  config.getUsername(), config.getPassword());

        // Generate handle from UUID
        String handle = config.getHandlePrefix() + "/" + dk.statsbiblioteket
                .doms.client.utils.Constants.ensurePID(pid);
        //Read DC datastream
        Document dataStream;
        try {
            dataStream = domsClient.getDataStream(pid, DC_DATASTREAM_ID);
        } catch (ServerOperationFailed serverOperationFailed) {
            throw new BackendMethodFailedException(
                    "Backendmethod failed while trying to to read DC from '"
                            + pid + "'", serverOperationFailed);
        }
        //TODO: Consider JAXB over document manipulation
        //Check handle is not already there
        boolean found = false;
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(dsNamespaceContext);
        NodeList result;
        try {
            result = (NodeList) xPath
                    .evaluate("//" + DC_PREFIX + ":" + DC_IDENTIFIER_ELEMENT,
                              dataStream,
                              XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new InconsistentDataException(
                    "Unexpected trouble working with DC datastream from '" + pid
                            + "'", e);
        }
        if (result.getLength() == 0) {
            throw new InconsistentDataException(
                    "Unexpected trouble working with DC datastream from '" + pid
                            + "': No dc:identifier elements");
        }
        Node item;
        int i = 0;
        do {
            if ((item = result.item(i++)).getTextContent().trim()
                    .equals(HANDLE_URI_NAMESPACE + handle)) {
                found = true;
            }
        } while (!found && i < result.getLength());
        //If not
        if (!found) {
            //Add it to DC XML
            Node newItem = dataStream
                    .createElementNS(DC_NAMESPACE_URI, DC_IDENTIFIER_ELEMENT);
            newItem.setTextContent(HANDLE_URI_NAMESPACE + handle);
            item.getParentNode().insertBefore(newItem, item.getNextSibling());
            try {
                //Unpublish
                domsClient.unpublishObjects("Prepare to add handle PID", pid);
                //Update DC datastream
                domsClient.updateDataStream(pid, DC_DATASTREAM_ID, dataStream,
                                            "Added handle PID");
                //Publish
                domsClient.publishObjects("Prepare to add handle PID", pid);
            } catch (ServerOperationFailed serverOperationFailed) {
                throw new BackendMethodFailedException(
                        "Backendmethod failed while trying to add handle '"
                                + handle + "' to '" + pid + "'",
                        serverOperationFailed);
            }
        }
        return handle;
    }

    private String getBase64Creds() {
        return "Basic " + Base64.encodeBytes(
                (config.getUsername() + ":" + config.getPassword()).getBytes());
    }

}
