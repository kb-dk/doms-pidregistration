package dk.statsbiblioteket.pidregistration.configuration;

import dk.statsbiblioteket.pidregistration.Collection;
import junit.framework.TestCase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Test properties load as expected.
 */
public class PropertyBasedRegistrarConfigurationTest extends TestCase {
    private PropertyBasedRegistrarConfiguration config
            = new PropertyBasedRegistrarConfiguration(
            getClass().getResourceAsStream("/doms-pidregistration.properties"));

    public void testGetFedoraLocation() {
        assertEquals("http://achernar:7880/fedora", config.getFedoraLocation());
    }

    public void testGetUsername() {
        assertEquals("fedoraAdmin", config.getUsername());
    }

    public void testGetPassword() {
        assertEquals("fedoraAdminPass", config.getPassword());
    }

    public void testGetDomsWSAPIEndpoint() throws MalformedURLException {
        assertEquals(
                new URL("http://achernar:7880/centralWebservice-service/central/?wsdl"),
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
        Set<Collection> expectedSet = new HashSet<Collection>();
//        expectedSet.add(new Collection("radiotv", "doms:RadioTV_Collection"));
//        expectedSet.add(new Collection("reklamefilm", "doms:Collection_Reklamefilm"));
        expectedSet.add(new Collection("avis", "doms:Newspaper_Collection"));
        assertEquals(expectedSet, config.getDomsCollections());
    }
}
