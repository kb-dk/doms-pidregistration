package dk.statsbiblioteket.pidregistration.configuration;

import dk.statsbiblioteket.pidregistration.Collection;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test properties load as expected.
 */
public class PropertyBasedRegistrarConfigurationTest {
    private PropertyBasedRegistrarConfiguration config
            = new PropertyBasedRegistrarConfiguration(
            getClass().getResourceAsStream("/doms-pidregistration.properties"));

    @Test
    public void testGetFedoraLocation() {
        assertEquals("http://alhena:7980/fedora", config.getFedoraLocation());
    }

    @Test
    public void testGetUsername() {
        assertEquals("fedoraAdmin", config.getUsername());
    }

    @Test
    public void testGetPassword() {
        assertEquals("fedoraAdminPass", config.getPassword());
    }

    @Test
    public void testGetDomsWSAPIEndpoint() throws MalformedURLException {
        assertEquals(
                new URL("http://alhena:7980/centralWebservice-service/central/?wsdl"),
                config.getDomsWSAPIEndpoint());
    }

    @Test
    public void testGetHandlePrefix() {
        assertEquals("109.3.1", config.getHandlePrefix());
    }

    @Test
    public void testGetPrivateKeyPath() {
        assertNull(config.getPrivateKeyPath());
    }
    
    @Test
    public void testGetPrivateKeyPassword() {
        assertEquals("", config.getPrivateKeyPassword());
    }

    @Test
    public void testGetPidPrefix() {
        assertEquals("http://bitfinder.statsbiblioteket.dk", config.getPidPrefix());
    }

    @Test
    public void testGetDomsMaxResultSize() {
        assertEquals(10000, config.getDomsMaxResultSize());
    }

    @Test
    public void testGetDomsCollections() {
        Set<Collection> expectedSet = new HashSet<Collection>();
        expectedSet.add(new Collection("radiotv", "doms:RadioTV_Collection"));
        expectedSet.add(new Collection("reklamefilm", "doms:Collection_Reklamefilm"));
        assertEquals(expectedSet, config.getDomsCollections());
    }
}
