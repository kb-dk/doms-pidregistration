package dk.statsbiblioteket.pidregistration.configuration;

import dk.statsbiblioteket.pidregistration.Collection;
import junit.framework.TestCase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

/**
 * Test properties load as expected.
 */
public class PropertyBasedRegistrarConfigurationTest extends TestCase {
    private PropertyBasedRegistrarConfiguration config
            = new PropertyBasedRegistrarConfiguration(
            getClass().getResourceAsStream("/pidregistration.properties"));

    public void testGetFedoraLocation() {
        assertEquals("http://alhena:7980/fedora", config.getFedoraLocation());
    }

    public void testGetUsername() {
        assertEquals("fedoraAdmin", config.getUsername());
    }

    public void testGetPassword() {
        assertEquals("fedoraAdminPass", config.getPassword());
    }

    public void testGetDomsWSAPIEndpoint() throws MalformedURLException {
        assertEquals(
                new URL("http://alhena:7980/centralWebservice-service/central/?wsdl"),
                config.getDomsWSAPIEndpoint());
    }

    public void testGetHandlePrefix() {
        assertEquals("109.3.1", config.getHandlePrefix());
    }

    public void testGetPrivateKeyPath() {
        assertNull(config.getPrivateKeyPath());
    }

    public void testGetPrivateKeyPassword() {
        assertEquals("", config.getPrivateKeyPassword());
    }

    public void testGetPidPrefix() {
        assertEquals("http://bitfinder.statsbiblioteket.dk", config.getPidPrefix());
    }

    public void testGetDomsMaxResultSize() {
        assertEquals(10000, config.getDomsMaxResultSize());
    }

    public void testGetDomsCollections() {
        assertEquals(
                Arrays.asList(new Collection("doms:RadioTV_Collection"), new Collection("doms:Collection_Reklamefilm")),
                config.getDomsCollections()
        );
    }
}
