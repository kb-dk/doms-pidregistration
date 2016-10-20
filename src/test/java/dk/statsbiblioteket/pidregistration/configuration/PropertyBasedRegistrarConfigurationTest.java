package dk.statsbiblioteket.pidregistration.configuration;

import dk.statsbiblioteket.pidregistration.Collection;
import junit.framework.TestCase;
import org.junit.Test;

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
            getClass().getResourceAsStream("/doms-pidregistration-testfile.properties"));

    @Test
    public void testGetFedoraLocation() {
        assertEquals("http://achernar:7880/fedora", config.getFedoraLocation());
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
                new URL("http://achernar:7880/centralWebservice-service/central/?wsdl"),
                config.getDomsWSAPIEndpoint());
    }

    @Test
    public void testGetDomsWSAPIEndpointTimeout() {
        assertEquals(43200000, config.getDomsWSAPIEndpointTimeout());
    }

    @Test
    public void testGetHandlePrefix() {
        assertEquals("109.3.1", config.getHandlePrefix());
    }

    @Test
    public void testGetPrivateKeyPath() {
        assertEquals("http://key:1337/path", config.getPrivateKeyPath());
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
        expectedSet.add(new Collection("avis", "doms:Newspaper_Collection"));
        assertEquals(expectedSet, config.getDomsCollections());
    }

    @Test
    public void testGetDatabaseUrl() {
        assertEquals("jdbc:postgresql://localhost:5432/pidreg-devel", config.getDatabaseUrl());
    }

    @Test
    public void testGetDatabaseUsername() {
        assertEquals("pidreg", config.getDatabaseUsername());
    }

    @Test
    public void testGetDatabasePassword() {
        assertEquals("pidreg", config.getDatabasePassword());
    }

    @Test
    public void testGetNumberOfThreads() {
        assertEquals(10, config.getNumberOfThreads());
    }
}
