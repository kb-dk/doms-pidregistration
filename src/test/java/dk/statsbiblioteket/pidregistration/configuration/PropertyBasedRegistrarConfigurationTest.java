package dk.statsbiblioteket.pidregistration.configuration;

import junit.framework.TestCase;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Test properties load as expected.
 */
public class PropertyBasedRegistrarConfigurationTest extends TestCase {
    private RegistrarConfiguration config
            = new PropertyBasedRegistrarConfiguration(
            getClass().getResourceAsStream("/handleregistrar.properties"));

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
}
