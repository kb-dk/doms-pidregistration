package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.DatabaseSchema;
import dk.statsbiblioteket.pidregistration.doms.*;
import dk.statsbiblioteket.pidregistration.handlesystem.GlobalHandleRegistry;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidCredentialsException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidResourceException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.MethodFailedException;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.xml.transform.TransformerException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PIDRegistrationsUnitTest {
    private static final PropertyBasedRegistrarConfiguration CONFIG
            = new PropertyBasedRegistrarConfiguration(
            PIDRegistrationsIntegrationTest.class.getResourceAsStream("/doms-pidregistration.properties"));
    private static final String AVIS_ID = "avis";
    private static final String AVIS_DOMS_COLLECTION = "doms:Newspaper_Collection";
    private static final String FIRST_OBJECT_PID = "uuid:a2aa627e-e216-492d-a039-e2d93e2b1e89";
    private static final String SECOND_OBJECT_PID = "uuid:b2744352-2e64-4c10-b7ab-391628bfb0be";
    private static final List<String> AVIS_IDS_UNDER_TEST =
            Arrays.asList(FIRST_OBJECT_PID, SECOND_OBJECT_PID);
    private static final String FIRST_OBJECT_DATASTREAM = "\n" +
            "<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
            "  <dc:identifier>uuid:a2aa627e-e216-492d-a039-e2d93e2b1e89</dc:identifier>\n" +
            "  <dc:identifier>path:B400026951974-RT2/400026951974-08/1756-09-13-02/berlingsketidende-1756-09-13-02-0422A</dc:identifier>\n" +
            "</oai_dc:dc>\n";
    private static final String SECOND_OBJECT_DATASTREAM = "\n" +
            "<oai_dc:dc xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
            "  <dc:identifier>uuid:b2744352-2e64-4c10-b7ab-391628bfb0be</dc:identifier>\n" +
            "  <dc:identifier>path:B400026951974-RT2/400026951974-14/1762-09-13-01/berlingsketidende-1762-09-13-01-0496A.jp2</dc:identifier>\n" +
            "</oai_dc:dc>\n";

    @Test
    public void test_doRegistrations() throws TransformerException, HandleException, MethodFailedException, InvalidResourceException, InvalidCredentialsException {

        new DatabaseSchema(CONFIG).removeIfExists();

        DOMSClient domsClient = mock(DOMSClient.class);
        when(domsClient.getMaxDomsResultSize()).thenReturn(CONFIG.getDomsMaxResultSize());
        when(domsClient.getDatastreamContents(FIRST_OBJECT_PID)).thenReturn(FIRST_OBJECT_DATASTREAM);
        when(domsClient.getDatastreamContents(SECOND_OBJECT_PID)).thenReturn(SECOND_OBJECT_DATASTREAM);

        GlobalHandleRegistry handleRegistry = mock(GlobalHandleRegistry.class);

        DOMSObjectIDQueryer domsObjectIDQueryer = mock(DOMSObjectIDQueryer.class);
        when(domsObjectIDQueryer.findNextIn((new Collection(AVIS_ID, AVIS_DOMS_COLLECTION)), new Date(0)))
                .thenReturn(new DOMSObjectIDQueryResult(AVIS_IDS_UNDER_TEST, new Date(0)));

        DOMSUpdater domsUpdater = mock(DOMSUpdater.class);

        PIDRegistrations PIDRegistrations = new PIDRegistrations(CONFIG, domsClient, handleRegistry, domsObjectIDQueryer, domsUpdater);

        List<String> idsToBePutInDoms = new ArrayList<>();
        idsToBePutInDoms.addAll(AVIS_IDS_UNDER_TEST);



        PIDRegistrations.doRegistrations();



        ArgumentCaptor<DOMSMetadata> firstMetadataArg = ArgumentCaptor.forClass(DOMSMetadata.class);
        PIDHandle firstHandle = new PIDHandle(CONFIG.getHandlePrefix(), FIRST_OBJECT_PID);
        verify(domsUpdater).update(eq(FIRST_OBJECT_PID), firstMetadataArg.capture());
        assertTrue("Metadata was not updated with correct handle", firstMetadataArg.getValue().handleExists(firstHandle));

        ArgumentCaptor<DOMSMetadata> secondMetadataArg = ArgumentCaptor.forClass(DOMSMetadata.class);
        PIDHandle secondHandle = new PIDHandle(CONFIG.getHandlePrefix(), SECOND_OBJECT_PID);
        verify(domsUpdater).update(eq(SECOND_OBJECT_PID), secondMetadataArg.capture());
        assertTrue("Metadata was not updated with correct handle", secondMetadataArg.getValue().handleExists(secondHandle));

        verifyNoMoreInteractions(domsUpdater);

        for (String objectId : idsToBePutInDoms) {
            PIDHandle handle = new PIDHandle(CONFIG.getHandlePrefix(), objectId);
            verify(handleRegistry).registerPid(eq(handle), eq("http://bitfinder.statsbiblioteket.dk/avis/" + objectId));
        }

        verifyNoMoreInteractions(handleRegistry);
    }
}
