package dk.statsbiblioteket.pidregistration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.DatabaseSchema;
import dk.statsbiblioteket.pidregistration.doms.DOMSClient;
import dk.statsbiblioteket.pidregistration.doms.DOMSObjectIDQueryResult;
import dk.statsbiblioteket.pidregistration.doms.DOMSObjectIDQueryer;
import dk.statsbiblioteket.pidregistration.handlesystem.GlobalHandleRegistry;
import net.handle.hdllib.HandleException;

@Disabled
public class PidRegistrationPerformanceTest {

    private static final PropertyBasedRegistrarConfiguration CONFIG
            = new PropertyBasedRegistrarConfiguration(
            PIDRegistrationsIntegrationTest.class.getResourceAsStream("/doms-pidregistration.properties"));

//    @Mocked
    private DOMSObjectIDQueryer domsObjectIdQueryer = null;

    private boolean inTestmode = false;
    private DOMSClient domsClient = new DOMSClient(CONFIG);
    private GlobalHandleRegistry handleRegistry = new GlobalHandleRegistry(CONFIG, inTestmode);
    private PIDRegistrations pidRegistrations;

    private static final String REKLAME_ID = "reklamefilm";
    private static final String REKLAME_DOMS_COLLECTION = "doms:Collection_Reklamefilm";

    private static final String TV_ID = "radiotv";
    private static final String TV_DOMS_COLLECTION = "doms:RadioTV_Collection";

    @Test
    public void testRegistrations() throws TransformerException, HandleException, IOException {
        domsObjectIdQueryer = mock(DOMSObjectIDQueryer.class);
        when(domsObjectIdQueryer.findNextIn(new Collection(TV_ID, TV_DOMS_COLLECTION), new Date(0)))
            .thenReturn(new DOMSObjectIDQueryResult(fetchIds("radiotv.txt"), new Date(0)));

        when(domsObjectIdQueryer.findNextIn(new Collection(REKLAME_ID, REKLAME_DOMS_COLLECTION), new Date(0)))
            .thenReturn(new DOMSObjectIDQueryResult(fetchIds("reklamefilm.txt"), new Date(0)));

        new DatabaseSchema(CONFIG).removeIfExists();

        pidRegistrations = new PIDRegistrations(CONFIG, domsClient, handleRegistry);

        long start = System.currentTimeMillis();
        pidRegistrations.doRegistrations();
        long end = System.currentTimeMillis();
        System.out.println("Registration took " + (end - start) + " ms. (Not including DOMS query)");
    }

    private ArrayList<String> fetchIds(String filename) throws IOException {
        ArrayList<String> result = new ArrayList<String>();
        String resourceName = "/" + filename;
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resourceName),
                StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            result.add(line);
        }
        reader.close();
        return result;
    }

    @AfterEach
    public void teardown() throws HandleException, TransformerException {
        long start = System.currentTimeMillis();
        pidRegistrations.doUnregistrations();
        pidRegistrations = null;
        long end = System.currentTimeMillis();

        System.out.println("Restoring DOMS and handle registry took " + (end - start) + " ms");
    }
}
