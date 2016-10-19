package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.DatabaseSchema;
import dk.statsbiblioteket.pidregistration.doms.*;
import dk.statsbiblioteket.pidregistration.handlesystem.GlobalHandleRegistry;
import dk.statsbiblioteket.pidregistration.handlesystem.PrivateKeyException;
import dk.statsbiblioteket.pidregistration.handlesystem.PrivateKeyLoader;
import dk.statsbiblioteket.util.xml.DOM;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import net.handle.hdllib.DeleteHandleRequest;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.transform.TransformerException;
import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Test handle objects using online Fedora.
 * NOTE: This test will _only_ work if the fedora mentioned in the test
 * config is available and contains the expected data.
 */
@Ignore
public class PIDRegistrationsIntegrationTest {
    private static final PropertyBasedRegistrarConfiguration CONFIG
            = new PropertyBasedRegistrarConfiguration(
            PIDRegistrationsIntegrationTest.class.getResourceAsStream("/doms-pidregistration.properties"));

//    @Mocked
//    private DOMSObjectIDQueryer domsObjectIdQueryer = null;

    private DOMSClient domsClient = new DOMSClient(CONFIG);
    private DOMSMetadataQueryer domsMetadataQueryer = new DOMSMetadataQueryer(domsClient);
    private DOMSUpdater domsUpdater = new DOMSUpdater(domsClient);
    private boolean isInTestmode = true;
    private GlobalHandleRegistry handleRegistry = new GlobalHandleRegistry(CONFIG, isInTestmode);
    private Map<String, DOMSMetadata> domsOriginals = new HashMap<String, DOMSMetadata>();

    private static final String AVIS_ID = "avis";
    private static final String AVIS_DOMS_COLLECTION = "doms:Newspaper_Collection";
    private static final List<String> AVIS_IDS_UNDER_TEST =
            Arrays.asList("uuid:a2aa627e-e216-492d-a039-e2d93e2b1e89", "uuid:b2744352-2e64-4c10-b7ab-391628bfb0be");

    @Before
    public void setup() throws TransformerException {
        domsOriginals = new HashMap<String, DOMSMetadata>();
        List<String> storedContents = new ArrayList<String>(AVIS_IDS_UNDER_TEST);
//        storedContents.addAll(TV_IDS_UNDER_TEST);
        for (String objectId : storedContents) {
            String resourceName = "/" + objectId.subSequence(5, objectId.length()) + ".xml";
            DOMSMetadata original = new DOMSMetadata(
                    DOM.domToString(DOM.streamToDOM(getClass().getResourceAsStream(resourceName)))
            );
            domsOriginals.put(objectId, original);
        }
    }

    @Test
    public void testRegistrations() throws TransformerException, HandleException {

        DOMSObjectIDQueryer domsObjectIDQueryer = mock(DOMSObjectIDQueryer.class);
        when(domsObjectIDQueryer.findNextIn((new Collection(AVIS_ID, AVIS_DOMS_COLLECTION)), new Date(0)))
                .thenReturn(new DOMSObjectIDQueryResult(AVIS_IDS_UNDER_TEST, new Date(0)));

        new DatabaseSchema(CONFIG).removeIfExists();

        PIDRegistrations PIDRegistrations = new PIDRegistrations(CONFIG, domsClient, handleRegistry, domsObjectIDQueryer, new DOMSUpdater(domsClient));

        List<String> idsToBePutInDoms = new ArrayList<String>();
        idsToBePutInDoms.addAll(AVIS_IDS_UNDER_TEST);
//        idsToBePutInDoms.addAll(TV_IDS_UNDER_TEST);
        String alreadyModified = "uuid:a2aa627e-e216-492d-a039-e2d93e2b1e89";
        idsToBePutInDoms.remove(alreadyModified);

        HandleResolver handleResolver = new HandleResolver();

        for (String objectId : idsToBePutInDoms) {
            DOMSMetadata metadata = domsMetadataQueryer.getMetadataForObject(objectId);
            PIDHandle handle = new PIDHandle(CONFIG.getHandlePrefix(), objectId);
            assertFalse(metadata.handleExists(handle));

            try {
                handleResolver.resolveHandle(handle.asString());
                fail("resolvehandle is expected to throw exception when no handle exists");
            } catch (HandleException e) {
                assertEquals(HandleException.HANDLE_DOES_NOT_EXIST, e.getCode());
            }
        }

        DOMSMetadata metadata = domsMetadataQueryer.getMetadataForObject(alreadyModified);
        assertTrue(metadata.handleExists(new PIDHandle(CONFIG.getHandlePrefix(), alreadyModified)));

        PIDRegistrations.doRegistrations();

        for (String objectId : idsToBePutInDoms) {
            metadata = domsMetadataQueryer.getMetadataForObject(objectId);
            PIDHandle handle = new PIDHandle(CONFIG.getHandlePrefix(), objectId);
            assertTrue(metadata.handleExists(handle));
            String url = new String((handleResolver.resolveHandle(handle.asString()))[1].getData());
            assertNotNull(url);
            assertTrue(url.startsWith("http://"));
        }

        metadata = domsMetadataQueryer.getMetadataForObject(alreadyModified);
        assertTrue(metadata.handleExists(new PIDHandle(CONFIG.getHandlePrefix(), alreadyModified)));
    }

    @Test
    public void testSimpleRegistrations() throws TransformerException, HandleException {

        DOMSObjectIDQueryer domsObjectIDQueryer = mock(DOMSObjectIDQueryer.class);
        when(domsObjectIDQueryer.findNextIn((new Collection(AVIS_ID, AVIS_DOMS_COLLECTION)), new Date(0)))
                .thenReturn(new DOMSObjectIDQueryResult(AVIS_IDS_UNDER_TEST, new Date(0)));

        new DatabaseSchema(CONFIG).removeIfExists();

        GlobalHandleRegistry handleRegistry = mock(GlobalHandleRegistry.class);
        PIDRegistrations PIDRegistrations = new PIDRegistrations(CONFIG, domsClient, handleRegistry, domsObjectIDQueryer, new DOMSUpdater(domsClient));

        List<String> idsToBePutInDoms = new ArrayList<String>();
        idsToBePutInDoms.addAll(AVIS_IDS_UNDER_TEST);

        HandleResolver handleResolver = new HandleResolver();



        for (String objectId : idsToBePutInDoms) {
            DOMSMetadata metadata = domsMetadataQueryer.getMetadataForObject(objectId);
            PIDHandle handle = new PIDHandle(CONFIG.getHandlePrefix(), objectId);
            assertFalse(metadata.handleExists(handle));

            try {
                handleResolver.resolveHandle(handle.asString());
                fail("resolvehandle is expected to throw exception when no handle exists");
            } catch (HandleException e) {
                assertEquals(HandleException.HANDLE_DOES_NOT_EXIST, e.getCode());
            }
        }


        PIDRegistrations.doRegistrations();



        for (String objectId : idsToBePutInDoms) {
            DOMSMetadata metadata = domsMetadataQueryer.getMetadataForObject(objectId);
            PIDHandle handle = new PIDHandle(CONFIG.getHandlePrefix(), objectId);
            assertTrue(metadata.handleExists(handle));
            verify(handleRegistry).registerPid(eq(handle), eq("http://bitfinder.statsbiblioteket.dk/avis/" + objectId));
        }

        verifyNoMoreInteractions(handleRegistry);
    }

    @After
    public void teardown() throws HandleException, TransformerException {
        restoreDoms();
//        restoreGlobalHandleRegistry();
    }

    private void restoreDoms() throws TransformerException {
        List<String> idsToRestore = new ArrayList<String>();
//        idsToRestore.addAll(TV_IDS_UNDER_TEST);
        idsToRestore.addAll(AVIS_IDS_UNDER_TEST);
        for (String objectId : idsToRestore) {
            if (domsOriginals.containsKey(objectId)) {
                domsUpdater.update(objectId, domsOriginals.get(objectId));
            }
        }
    }

    private void restoreGlobalHandleRegistry() throws HandleException {
        String adminIdPrefix = "0.NA/";
        Charset defaultEncoding = Charset.forName("UTF8");
        int adminIdIndex = 300;

        String adminId = adminIdPrefix + CONFIG.getHandlePrefix();

        PublicKeyAuthenticationInfo authInfo = new PublicKeyAuthenticationInfo(
                adminId.getBytes(defaultEncoding), adminIdIndex, loadPrivateKey());

        HandleResolver handleResolver = mock(HandleResolver.class);

        List<String> idsToRestore = new ArrayList<String>();
//        idsToRestore.addAll(TV_IDS_UNDER_TEST);
        idsToRestore.addAll(AVIS_IDS_UNDER_TEST);
        for (String objectId : idsToRestore) {
            String handle = new PIDHandle(CONFIG.getHandlePrefix(), objectId).asString();
            DeleteHandleRequest request = new DeleteHandleRequest(handle.getBytes(defaultEncoding), authInfo);
            handleResolver.processRequest(request);
        }
    }

    private PrivateKey loadPrivateKey() throws PrivateKeyException {
//        String path = CONFIG.getPrivateKeyPath();
//        String password = CONFIG.getPrivateKeyPassword();
//        PrivateKeyLoader privateKeyLoader =
//                path == null ? new PrivateKeyLoader(password) : new PrivateKeyLoader(password, path);
//        return privateKeyLoader.load();
        return null;
    }
}
