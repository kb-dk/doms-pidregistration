package dk.statsbiblioteket.pidregistration;

import junit.framework.TestCase;

/**
 * Test BasicHandleRegistrar calls the expected methods the expected number of
 * times.
 */
public class BasicHandleRegistrarTest extends TestCase {
    public void testAddHandles() throws Exception {
        MockupRepositoryHandler repositoryHandler
                = new MockupRepositoryHandler();
        MockupPidResolverHandler pidResolverHandler
                = new MockupPidResolverHandler();
        String result = new BasicHandleRegistrar(repositoryHandler, pidResolverHandler)
                .addHandles("foo", "bar");
        assertEquals(1, repositoryHandler.findObjectFromQueryCount);
        assertEquals("foo", repositoryHandler.findObjectFromQueryQuery);
        assertEquals(1, repositoryHandler.findObjectFromQueryCount);
        assertEquals("info:fedora/foo", repositoryHandler.addHandleToObjectPid);
        assertEquals(1, pidResolverHandler.registerPidCount);
        assertEquals("109.3.1/foo", pidResolverHandler.registerPidPid);
        assertEquals("info:fedora/foo", pidResolverHandler.registerPidRepositoryId);
        assertEquals("bar", pidResolverHandler.registerPidUrlPattern);
        assertEquals("Done adding handles. #success: 1 #failure: 0", result);
    }
}
