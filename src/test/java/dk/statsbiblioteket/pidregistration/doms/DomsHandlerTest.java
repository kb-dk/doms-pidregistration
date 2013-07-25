package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import dk.statsbiblioteket.doms.client.DomsWSClient;
import dk.statsbiblioteket.doms.client.DomsWSClientImpl;
import dk.statsbiblioteket.util.xml.DefaultNamespaceContext;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.util.List;

/**
 * Test handle objects using online Fedora.
 * NOTE: This test will _only_ work if the fedora mentioned in the test
 * config is available and contains the expected data.
 *
 * Expected data is:
 * Object with UUID uuid:001fdf2b-a05a-40de-a43b-787f1ba9041f exists and has handle 109.3.1/uuid:001fdf2b-a05a-40de-a43b-787f1ba9041f and label 'Jeg er et dyr!'
 * Object with UUID uuid:0019f31d-b6f7-4ef2-81f6-89b116c64272 exists and has no handles
 */
public class DomsHandlerTest extends TestCase {

    private static final String PID1 = "uuid:001fdf2b-a05a-40de-a43b-787f1ba9041f";
    private static final String PID2 = "uuid:0019f31d-b6f7-4ef2-81f6-89b116c64272";
    private PropertyBasedRegistrarConfiguration config;
    private Document d1;
    private Document d2;
    private DomsWSClient domsClient;
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

    public void setUp() throws Exception {
        config = new PropertyBasedRegistrarConfiguration(
                getClass().getResourceAsStream("/handleregistrar.properties"));
        domsClient = new DomsWSClientImpl();
        domsClient.setCredentials(config.getDomsWSAPIEndpoint(),
                                  config.getUsername(), config.getPassword());
        d1 = domsClient.getDataStream(PID1, "DC");
        d2 = domsClient.getDataStream(PID2, "DC");
    }

    public void tearDown() throws Exception {
        domsClient.unpublishObjects("Restored original content from unit test",
                                    PID1);
        domsClient.updateDataStream(PID1, "DC", d1,
                                    "Restored original content from unit test");
        domsClient.publishObjects("Restored original content from unit test",
                                  PID1);
        domsClient.unpublishObjects("Restored original content from unit test",
                                    PID2);
        domsClient.updateDataStream(PID2, "DC", d2,
                                    "Restored original content from unit test");
        domsClient.publishObjects("Restored original content from unit test",
                                  PID2);
    }

    public void testFindObjectFromQuery() throws Exception {
        DomsHandler domsHandler = new DomsHandler(config);
        List<String> pids = domsHandler.findObjectFromQuery(
                "select ?object\n"
                        + "where {\n"
                        + "?object <info:fedora/fedora-system:def/model#label> 'Jeg er et dyr!' .\n"
                        + "}");
        assertTrue("List of found finds does not contain '" + PID1 + "', but: " + pids, pids.contains(PID1));
        pids = domsHandler.findObjectFromQuery(
                "select ?object ?date\n"
                        + "where {\n"
                        + "?object <info:fedora/fedora-system:def/model#label> 'Jeg er et dyr!' .\n"
                        + "?object <info:fedora/fedora-system:def/view#lastModifiedDate> ?date .\n"
                        + "}");
        assertTrue("List of found finds does not contain '" + PID1 + "', but: " + pids, pids.contains(PID1));
    }

    public void testAddHandleToObject() throws Exception {
        DomsHandler domsHandler = new DomsHandler(config);

        // Call method on object with existing handle
        String handle1 = domsHandler.addHandleToObject(PID1);
        // Check correct handle generated
        assertEquals("109.3.1/" + PID1, handle1);
        // Read new DC datastream from Doms
        Document d1New = domsClient.getDataStream(PID1, "DC");
        // Find identifiers from old and new DC datastream
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(dsNamespaceContext);
        NodeList result1before = (NodeList) xPath
                    .evaluate("//dc:identifier",
                              d1,
                              XPathConstants.NODESET);
        NodeList result1after = (NodeList) xPath
                    .evaluate("//dc:identifier",
                              d1New,
                              XPathConstants.NODESET);
        // ensure DC datastream unchanged
        assertEquals(result1before.getLength(), result1after.getLength());
        for (int i = 0; i < result1before.getLength(); i++) {
            assertEquals(result1before.item(i).getTextContent(), result1after.item(i).getTextContent());
        }


        // Call method on object without existing handle
        String handle2 = domsHandler.addHandleToObject(PID2);
        // Check correct handle generated
        assertEquals("109.3.1/" + PID2, handle2);
        // Read new DC datastream from Doms
        Document d2New = domsClient.getDataStream(PID2, "DC");
        // Find identifiers from old and new DC datastream
        NodeList result2before = (NodeList) xPath
                    .evaluate("//dc:identifier",
                              d2,
                              XPathConstants.NODESET);
        NodeList result2after = (NodeList) xPath
                    .evaluate("//dc:identifier",
                              d2New,
                              XPathConstants.NODESET);
        // ensure DC datastream now has new handle PID
        assertEquals(result2before.getLength() + 1, result2after.getLength());
        boolean found = false;
        // Wasn't there before
        for (int i = 0; i < result2before.getLength(); i++) {
            found |= result2before.item(i).getTextContent().equals("hdl:" + handle2);
        }
        assertFalse(found);
        // Is there now
        for (int i = 0; i < result2after.getLength(); i++) {
            found |= result2after.item(i).getTextContent().equals("hdl:" + handle2);
        }
        assertTrue(found);
    }
}
