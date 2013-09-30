package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.doms.DOMSClient;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadata;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadataQueryer;
import dk.statsbiblioteket.pidregistration.doms.DOMSObjectIDQueryer;
import dk.statsbiblioteket.pidregistration.doms.DOMSUpdater;
import dk.statsbiblioteket.pidregistration.handlesystem.GlobalHandleRegistry;
import dk.statsbiblioteket.pidregistration.handlesystem.PrivateKeyException;
import dk.statsbiblioteket.pidregistration.handlesystem.PrivateKeyLoader;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import net.handle.hdllib.DeleteHandleRequest;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.transform.TransformerException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Ignore
public class PidRegistrationPerformanceTest {

    private static final PropertyBasedRegistrarConfiguration CONFIG
            = new PropertyBasedRegistrarConfiguration(
            PIDRegistrationsIntegrationTest.class.getResourceAsStream("/pidregistration.properties"));

    @Mocked
    private DOMSObjectIDQueryer domsObjectIdQueryer = null;

    private DOMSClient domsClient = new DOMSClient(CONFIG);
    private DOMSMetadataQueryer domsMetadataQueryer = new DOMSMetadataQueryer(domsClient);
    private DOMSUpdater domsUpdater = new DOMSUpdater(domsClient);
    private GlobalHandleRegistry handleRegistry = new GlobalHandleRegistry(CONFIG);

    private List<String> idsToHandle = new ArrayList<String>();
    private Map<String, DOMSMetadata> domsOriginals = new HashMap<String, DOMSMetadata>();

    private static final String REKLAME_ID = "reklamefilm";
    private static final String REKLAME_DOMS_COLLECTION = "doms:Collection_Reklamefilm";

    private static final String TV_ID = "radiotv";
    private static final String TV_DOMS_COLLECTION = "doms:RadioTV_Collection";

    @Test
    public void testRegistrations() throws TransformerException, HandleException, IOException {
        new NonStrictExpectations() {{

            domsObjectIdQueryer.findNextIn(new Collection(TV_ID, TV_DOMS_COLLECTION));
            returns(fetchIds("radiotv.txt"), new ArrayList<String>());

            domsObjectIdQueryer.findNextIn(new Collection(REKLAME_ID, REKLAME_DOMS_COLLECTION));
            returns(fetchIds("reklamefilm.txt"), new ArrayList<String>());
        }};

        PIDRegistrations PIDRegistrations = new PIDRegistrations(CONFIG, domsClient, handleRegistry, createTestDate());

        idsToHandle.addAll(fetchIds("radiotv.txt"));
        idsToHandle.addAll(fetchIds("reklamefilm.txt"));

        for (String objectId : idsToHandle) {
            DOMSMetadata metadata = domsMetadataQueryer.getMetadataForObject(objectId);
            domsOriginals.put(objectId, metadata);
        }

        long start = System.currentTimeMillis();
        PIDRegistrations.doRegistrations();
        long end = System.currentTimeMillis();
        System.out.println("Registration took " + (end - start) + " ms. (Not including DOMS query)");
    }

    private ArrayList<String> fetchIds(String filename) throws IOException {
        ArrayList<String> result = new ArrayList<String>();
        String resourceName = "/" + filename;
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resourceName)));
        String line;
        while ((line = reader.readLine()) != null) {
            result.add(line);
        }
        reader.close();
        return result;
    }

    private Date createTestDate() {
        Calendar january1st2013 = Calendar.getInstance();
        january1st2013.clear();
        january1st2013.set(2013, Calendar.JANUARY, 1);
        return january1st2013.getTime();
    }

    @After
    public void teardown() throws HandleException, TransformerException {
        long start = System.currentTimeMillis();
        restoreDoms();
        long end = System.currentTimeMillis();

        System.out.println("Restoring DOMS took " + (end - start) + " ms");

        restoreGlobalHandleRegistry();
        end = System.currentTimeMillis();

        System.out.println("Restoring handle registry took " + (end - start) + " ms");
    }

    private void restoreDoms() throws TransformerException {
        for (String objectId : domsOriginals.keySet()) {
            domsUpdater.update(objectId, domsOriginals.get(objectId));
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

        List<String> idsToRestore = idsToHandle;
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
