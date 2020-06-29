package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.DatabaseSchema;
import dk.statsbiblioteket.pidregistration.doms.DOMSClient;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadata;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadataQueryer;
import dk.statsbiblioteket.pidregistration.doms.DOMSObjectIDQueryer;
import dk.statsbiblioteket.pidregistration.doms.DOMSUpdater;
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
import java.nio.charset.StandardCharsets;
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

    @Mocked
    private DOMSObjectIDQueryer domsObjectIdQueryer = null;

    private DOMSClient domsClient = new DOMSClient(CONFIG);
    private DOMSMetadataQueryer domsMetadataQueryer = new DOMSMetadataQueryer(domsClient);
    private DOMSUpdater domsUpdater = new DOMSUpdater(domsClient);
    private boolean isInTestmode = true;
    private GlobalHandleRegistry handleRegistry = new GlobalHandleRegistry(CONFIG, isInTestmode);
    private Map<String, DOMSMetadata> domsOriginals = new HashMap<String, DOMSMetadata>();

    private static final String REKLAME_ID = "reklamefilm";
    private static final String REKLAME_DOMS_COLLECTION = "doms:Collection_Reklamefilm";
    private static final List<String> REKLAME_IDS_UNDER_TEST =
            Arrays.asList("uuid:bff36b9a-a38e-4cf8-a03a-efe6c7a58f4a",
                          "uuid:4e8a6e1f-b5d1-4e38-8ce8-15c49ce6ca13");

    private static final String TV_ID = "radiotv";
    private static final String TV_DOMS_COLLECTION = "doms:RadioTV_Collection";
    private static final List<String> TV_IDS_UNDER_TEST =
            Arrays.asList("uuid:001fdf2b-a05a-40de-a43b-787f1ba9041f",
                          "uuid:0019f31d-b6f7-4ef2-81f6-89b116c64272");
    @Before
    public void setup() throws TransformerException {
        domsOriginals = new HashMap<String, DOMSMetadata>();
        List<String> storedContents = new ArrayList<String>(REKLAME_IDS_UNDER_TEST);
        storedContents.addAll(TV_IDS_UNDER_TEST);
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
        new NonStrictExpectations() {{

            domsObjectIdQueryer.findNextIn(new Collection(TV_ID, TV_DOMS_COLLECTION), new Date(0));
            returns(TV_IDS_UNDER_TEST, new ArrayList<String>());

            domsObjectIdQueryer.findNextIn(new Collection(REKLAME_ID, REKLAME_DOMS_COLLECTION), new Date(0));
            returns(REKLAME_IDS_UNDER_TEST, new ArrayList<String>());
        }};

        new DatabaseSchema(CONFIG).removeIfExists();

        PIDRegistrations PIDRegistrations = new PIDRegistrations(CONFIG, domsClient, handleRegistry);

        List<String> idsToBePutInDoms = new ArrayList<String>();
        idsToBePutInDoms.addAll(REKLAME_IDS_UNDER_TEST);
        idsToBePutInDoms.addAll(TV_IDS_UNDER_TEST);
        String alreadyModified = "uuid:001fdf2b-a05a-40de-a43b-787f1ba9041f";
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
            String url = new String((handleResolver.resolveHandle(handle.asString()))[1].getData(), 
                    StandardCharsets.UTF_8);
            assertNotNull(url);
            assertTrue(url.startsWith("http://"));
        }

        metadata = domsMetadataQueryer.getMetadataForObject(alreadyModified);
        assertTrue(metadata.handleExists(new PIDHandle(CONFIG.getHandlePrefix(), alreadyModified)));
    }

    @After
    public void teardown() throws HandleException, TransformerException {
        restoreDoms();
        restoreGlobalHandleRegistry();
    }

    private void restoreDoms() throws TransformerException {
        List<String> idsToRestore = new ArrayList<String>();
        idsToRestore.addAll(TV_IDS_UNDER_TEST);
        idsToRestore.addAll(REKLAME_IDS_UNDER_TEST);
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

        HandleResolver handleResolver = new HandleResolver();

        List<String> idsToRestore = new ArrayList<String>();
        idsToRestore.addAll(TV_IDS_UNDER_TEST);
        idsToRestore.addAll(REKLAME_IDS_UNDER_TEST);
        for (String objectId : idsToRestore) {
            String handle = new PIDHandle(CONFIG.getHandlePrefix(), objectId).asString();
            DeleteHandleRequest request = new DeleteHandleRequest(handle.getBytes(defaultEncoding), authInfo);
            handleResolver.processRequest(request);
        }
    }

    private PrivateKey loadPrivateKey() throws PrivateKeyException {
        String path = CONFIG.getPrivateKeyPath();
        String password = CONFIG.getPrivateKeyPassword();
        PrivateKeyLoader privateKeyLoader =
                path == null ? new PrivateKeyLoader(password) : new PrivateKeyLoader(password, path);
        return privateKeyLoader.load();
    }
}
