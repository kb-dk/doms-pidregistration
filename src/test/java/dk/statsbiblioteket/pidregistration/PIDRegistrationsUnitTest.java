package dk.statsbiblioteket.pidregistration;

import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import dk.statsbiblioteket.pidregistration.database.ConnectionFactory;
import dk.statsbiblioteket.pidregistration.database.dao.JobsDAO;
import dk.statsbiblioteket.pidregistration.database.dao.JobsIterator;
import dk.statsbiblioteket.pidregistration.database.dto.JobDTO;
import dk.statsbiblioteket.pidregistration.doms.*;
import dk.statsbiblioteket.pidregistration.handlesystem.GlobalHandleRegistry;
import dk.statsbiblioteket.pidregistration.utilities.MetadataGenerator;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidCredentialsException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidResourceException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.MethodFailedException;
import net.handle.hdllib.HandleException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.xml.transform.TransformerException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PIDRegistrationsUnitTest {
    private static final PropertyBasedRegistrarConfiguration CONFIG
            = new PropertyBasedRegistrarConfiguration(
            PIDRegistrationsIntegrationTest.class.getResourceAsStream("/doms-pidregistration.properties"));
    private static final String HANDLE_PREFIX = CONFIG.getHandlePrefix();
    private static final String ID = "avis";
    private static final String DOMS_COLLECTION = "doms:Newspaper_Collection";
    private static final String FIRST_OBJECT_PID = "uuid:some-id";
    private static final String SECOND_OBJECT_PID = "uuid:some-other-id";
    private static final List<String> IDS_UNDER_TEST =
            Arrays.asList(FIRST_OBJECT_PID, SECOND_OBJECT_PID);

    private DOMSClient domsClient;
    private GlobalHandleRegistry handleRegistry;
    private DOMSUpdater domsUpdater;
    private PIDRegistrations PIDRegistrations;

    @Before
    public void setup() {
        Collection collection = new Collection(ID, DOMS_COLLECTION);
        JobDTO firstJob = new JobDTO(FIRST_OBJECT_PID, collection, null);
        JobDTO secondJob = new JobDTO(SECOND_OBJECT_PID, collection, null);

        domsClient = mock(DOMSClient.class);
        when(domsClient.getMaxDomsResultSize()).thenReturn(CONFIG.getDomsMaxResultSize());

        handleRegistry = mock(GlobalHandleRegistry.class);

        DOMSObjectIDQueryer domsObjectIDQueryer = mock(DOMSObjectIDQueryer.class);
        when(domsObjectIDQueryer.findNextIn(collection, new Date(0)))
                .thenReturn(new DOMSObjectIDQueryResult(IDS_UNDER_TEST, new Date(0)));

        domsUpdater = mock(DOMSUpdater.class);

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class, RETURNS_DEEP_STUBS);
        JobsDAO jobsDAO = mock(JobsDAO.class);
        JobsIterator jobs = mock(JobsIterator.class);

        when(connectionFactory.getJobsDAO()).thenReturn(jobsDAO);
        when(jobsDAO.findJobsPending(anyInt())).thenReturn(jobs);
        when(jobs.next()).thenReturn(firstJob, secondJob, null);

        PIDRegistrations =
                new PIDRegistrations(CONFIG, domsClient, handleRegistry, domsObjectIDQueryer, domsUpdater, connectionFactory);
    }

    @Test
    public void test_doRegistrations_forObjectsWithoutHandle() throws TransformerException, HandleException, MethodFailedException,
            InvalidResourceException, InvalidCredentialsException, SQLException {

        when(domsClient.getDatastreamContents(FIRST_OBJECT_PID)).thenReturn(MetadataGenerator.createDatastream(FIRST_OBJECT_PID, "some/path", Optional.empty()));
        when(domsClient.getDatastreamContents(SECOND_OBJECT_PID)).thenReturn(MetadataGenerator.createDatastream(SECOND_OBJECT_PID, "some/other/path", Optional.empty()));

        List<String> idsToBePutInDoms = new ArrayList<>();
        idsToBePutInDoms.addAll(IDS_UNDER_TEST);



        PIDRegistrations.doRegistrations();



        String handlePrefix = CONFIG.getHandlePrefix();

        // Verify that metadata at DOMS is updated with handles.
        PIDHandle firstHandle = new PIDHandle(handlePrefix, FIRST_OBJECT_PID);
        ArgumentCaptor<DOMSMetadata> firstMetadataArg = ArgumentCaptor.forClass(DOMSMetadata.class);
        verify(domsUpdater).update(eq(FIRST_OBJECT_PID), firstMetadataArg.capture());
        assertTrue("Metadata was not updated with correct handle", firstMetadataArg.getValue().handleExists(firstHandle));

        PIDHandle secondHandle = new PIDHandle(handlePrefix, SECOND_OBJECT_PID);
        ArgumentCaptor<DOMSMetadata> secondMetadataArg = ArgumentCaptor.forClass(DOMSMetadata.class);
        verify(domsUpdater).update(eq(SECOND_OBJECT_PID), secondMetadataArg.capture());
        assertTrue("Metadata was not updated with correct handle", secondMetadataArg.getValue().handleExists(secondHandle));

        verifyNoMoreInteractions(domsUpdater);

        // Verify that handle registry is updated with handles.
        for (String objectId : idsToBePutInDoms) {
            PIDHandle handle = new PIDHandle(handlePrefix, objectId);
            verify(handleRegistry).registerPid(eq(handle), eq(CONFIG.getPidPrefix() + "/" + ID + "/" + objectId));
        }

        verifyNoMoreInteractions(handleRegistry);
    }

    @Test
    public void test_doRegistrations_forObjectsWithHandle() throws TransformerException, HandleException,
            MethodFailedException, InvalidResourceException, InvalidCredentialsException {

        when(domsClient.getDatastreamContents(FIRST_OBJECT_PID))
                .thenReturn(MetadataGenerator.createDatastream(FIRST_OBJECT_PID, "some/path", Optional.of("hdl:" + HANDLE_PREFIX + "/" + FIRST_OBJECT_PID)));
        when(domsClient.getDatastreamContents(SECOND_OBJECT_PID))
                .thenReturn(MetadataGenerator.createDatastream(SECOND_OBJECT_PID, "some/other/path", Optional.of("hdl:" + HANDLE_PREFIX + "/" + SECOND_OBJECT_PID)));

        List<String> idsToBePutInDoms = new ArrayList<>();
        idsToBePutInDoms.addAll(IDS_UNDER_TEST);



        PIDRegistrations.doRegistrations();



        // DOMS metadata should not be updated when handles already exists.
        verifyNoMoreInteractions(domsUpdater);

        // Verify that handle registry is updated with handles.
        for (String objectId : idsToBePutInDoms) {
            PIDHandle handle = new PIDHandle(HANDLE_PREFIX, objectId);
            verify(handleRegistry).registerPid(eq(handle), eq(CONFIG.getPidPrefix() + "/" + ID + "/" + objectId));
        }

        verifyNoMoreInteractions(handleRegistry);
    }


}
