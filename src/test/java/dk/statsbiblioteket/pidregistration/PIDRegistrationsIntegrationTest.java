package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.ConnectionFactory;
import dk.statsbiblioteket.pidregistration.database.DatabaseSchema;
import dk.statsbiblioteket.pidregistration.doms.DOMSClient;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadata;
import dk.statsbiblioteket.pidregistration.doms.DOMSMetadataQueryer;
import dk.statsbiblioteket.pidregistration.doms.DOMSObjectIDQueryResult;
import dk.statsbiblioteket.pidregistration.doms.DOMSObjectIDQueryer;
import dk.statsbiblioteket.pidregistration.doms.DOMSUpdater;
import dk.statsbiblioteket.pidregistration.handlesystem.GlobalHandleRegistry;
import dk.statsbiblioteket.pidregistration.utilities.MetadataGenerator;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.transform.TransformerException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
    private static final String ID = "avis";
    private static final String DOMS_COLLECTION = "doms:Newspaper_Collection";
    private static final String FIRST_OBJECT_PID = "uuid:a2aa627e-e216-492d-a039-e2d93e2b1e89";
    private static final String SECOND_OBJECT_PID = "uuid:b2744352-2e64-4c10-b7ab-391628bfb0be";
    private static final String FIRST_OBJECT_METADATA_PATH = "B400026951974-RT2/400026951974-08/1756-09-13-02/berlingsketidende-1756-09-13-02-0422A";
    private static final String SECOND_OBJECT_METADATA_PATH = "B400026951974-RT2/400026951974-14/1762-09-13-01/berlingsketidende-1762-09-13-01-0496A.jp2";
    private static final List<String> IDS_UNDER_TEST =
            Arrays.asList(FIRST_OBJECT_PID, SECOND_OBJECT_PID);

    private DOMSUpdater domsUpdater;

    @Test
    public void test_doRegistrations_forObjectsWithoutHandle() throws TransformerException, HandleException {

        new DatabaseSchema(new ConnectionFactory(CONFIG)).removeIfExists();

        DOMSClient domsClient = new DOMSClient(CONFIG);
        DOMSMetadataQueryer domsMetadataQueryer = new DOMSMetadataQueryer(domsClient);
        domsUpdater = new DOMSUpdater(domsClient);

        DOMSObjectIDQueryer domsObjectIDQueryer = mock(DOMSObjectIDQueryer.class);
        when(domsObjectIDQueryer.findNextIn((new Collection(ID, DOMS_COLLECTION)), new Date(0)))
                .thenReturn(new DOMSObjectIDQueryResult(IDS_UNDER_TEST, new Date(0)));

        GlobalHandleRegistry handleRegistry = mock(GlobalHandleRegistry.class);

        PIDRegistrations PIDRegistrations =
                new PIDRegistrations(CONFIG, domsClient, handleRegistry,
                        domsObjectIDQueryer, domsUpdater);

        HandleResolver handleResolver = new HandleResolver();



        for (String objectId : IDS_UNDER_TEST) {
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



        for (String objectId : IDS_UNDER_TEST) {
            DOMSMetadata metadata = domsMetadataQueryer.getMetadataForObject(objectId);
            PIDHandle handle = new PIDHandle(CONFIG.getHandlePrefix(), objectId);
            assertTrue(metadata.handleExists(handle));
            verify(handleRegistry).registerPid(eq(handle), eq("http://bitfinder.statsbiblioteket.dk/avis/" + objectId));
        }

        verifyNoMoreInteractions(handleRegistry);


        restoreDoms();
    }

    private void restoreDoms() throws TransformerException {
        String metadata = MetadataGenerator.createDatastream(FIRST_OBJECT_PID, FIRST_OBJECT_METADATA_PATH, Optional.empty());
        domsUpdater.update(FIRST_OBJECT_PID, new DOMSMetadata(metadata));

        metadata = MetadataGenerator.createDatastream(SECOND_OBJECT_PID, SECOND_OBJECT_METADATA_PATH, Optional.empty());
        domsUpdater.update(SECOND_OBJECT_PID, new DOMSMetadata(metadata));
    }
}
