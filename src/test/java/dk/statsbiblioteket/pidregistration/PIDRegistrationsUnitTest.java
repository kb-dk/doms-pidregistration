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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.xml.transform.TransformerException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class PIDRegistrationsUnitTest {
    private static final PropertyBasedRegistrarConfiguration CONFIG
            = new PropertyBasedRegistrarConfiguration(
            PIDRegistrationsIntegrationTest.class.getResourceAsStream("/doms-pidregistration.properties"));
    private static final PropertyBasedRegistrarConfiguration CONFIG_SPY = spy(CONFIG);
    private static final String HANDLE_PREFIX = CONFIG.getHandlePrefix();
    private static final String ID = "avis";
    private static final String DOMS_COLLECTION = "doms:Newspaper_Collection";
    private static final int MAX_DOMS_RESULT_SIZE = 10;

    private DOMSClient domsClient;
    private GlobalHandleRegistry handleRegistry;
    private DOMSUpdater domsUpdater;
    private PIDRegistrations PIDRegistrations;
    private DOMSObjectIDQueryer domsObjectIDQueryer;
    private JobsDAO jobsDAO;

    @BeforeEach
    public void setup() {

        domsClient = mock(DOMSClient.class);
        when(domsClient.getMaxDomsResultSize()).thenReturn(MAX_DOMS_RESULT_SIZE);

        when(CONFIG_SPY.getDomsMaxResultSize()).thenReturn(MAX_DOMS_RESULT_SIZE);

        handleRegistry = mock(GlobalHandleRegistry.class);

        domsObjectIDQueryer = mock(DOMSObjectIDQueryer.class);

        domsUpdater = mock(DOMSUpdater.class);

        ConnectionFactory connectionFactory = mock(ConnectionFactory.class, RETURNS_DEEP_STUBS);
        jobsDAO = mock(JobsDAO.class);
        when(connectionFactory.createJobsDAO(any())).thenReturn(jobsDAO);

        PIDRegistrations =
                new PIDRegistrations(CONFIG_SPY, domsClient, handleRegistry, domsObjectIDQueryer, domsUpdater, connectionFactory);
    }

    @Test
    public void test_doRegistrations_for2ObjectsWithoutHandle() throws TransformerException, HandleException,
            MethodFailedException, InvalidResourceException, InvalidCredentialsException, SQLException {

        List<String> idsUnderTest =
                Arrays.asList("uuid:some-id", "uuid:some-other-id");

        Collection collection = new Collection(ID, DOMS_COLLECTION);

        when(domsObjectIDQueryer.findNextIn(collection, new Date(0)))
                .thenReturn(new DOMSObjectIDQueryResult(idsUnderTest, new Date(0)));

        boolean withHandle = false;
        createAndUseMetadata(idsUnderTest, withHandle);

        Iterator<JobDTO> jobs = createJobsIterator(idsUnderTest, collection);
        when(jobsDAO.findJobsPending(anyInt())).thenReturn(jobs);



        PIDRegistrations.doRegistrations();



        verifyHandlesUpdatedAtDoms(idsUnderTest);
        verifyHandlesUpdatedInRegistry(idsUnderTest);

        verifyNoMoreInteractions(domsUpdater);
        verifyNoMoreInteractions(handleRegistry);
    }

    @Test
    public void test_doRegistrations_for2ObjectsWithHandle() throws TransformerException, HandleException,
            MethodFailedException, InvalidResourceException, InvalidCredentialsException {

        List<String> idsUnderTest =
                Arrays.asList("uuid:some-id", "uuid:some-other-id");

        Collection collection = new Collection(ID, DOMS_COLLECTION);

        when(domsObjectIDQueryer.findNextIn(collection, new Date(0)))
                .thenReturn(new DOMSObjectIDQueryResult(idsUnderTest, new Date(0)));

        boolean withHandle = true;
        createAndUseMetadata(idsUnderTest, withHandle);

        Iterator<JobDTO> jobs = createJobsIterator(idsUnderTest, collection);
        when(jobsDAO.findJobsPending(anyInt())).thenReturn(jobs);



        PIDRegistrations.doRegistrations();



        // DOMS metadata should not be updated when handles already exists.
        verifyNoMoreInteractions(domsUpdater);

        verifyHandlesUpdatedInRegistry(idsUnderTest);

        verifyNoMoreInteractions(handleRegistry);
    }


    @Test
    public void test_doRegistrations_for2ObjectsWith4Threads() throws MethodFailedException, InvalidResourceException,
            InvalidCredentialsException {

        List<String> idsUnderTest =
                Arrays.asList("uuid:some-id", "uuid:some-other-id");

        Collection collection = new Collection(ID, DOMS_COLLECTION);

        when(domsObjectIDQueryer.findNextIn(collection, new Date(0)))
                .thenReturn(new DOMSObjectIDQueryResult(idsUnderTest, new Date(0)));

        boolean withHandle = false;
        createAndUseMetadata(idsUnderTest, withHandle);

        Iterator<JobDTO> jobs = createJobsIterator(idsUnderTest, collection);
        when(jobsDAO.findJobsPending(anyInt())).thenReturn(jobs);

        when(CONFIG_SPY.getNumberOfThreads()).thenReturn(4);



        PIDRegistrations.doRegistrations();



        verifyHandlesUpdatedAtDoms(idsUnderTest);
        verifyHandlesUpdatedInRegistry(idsUnderTest);

        verifyNoMoreInteractions(domsUpdater);
        verifyNoMoreInteractions(handleRegistry);
    }

    @Test
    public void test_doRegistrations_for15ObjectsWith4Threads() throws MethodFailedException, InvalidResourceException,
            InvalidCredentialsException {

        List<String> idsUnderTest = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            idsUnderTest.add("uuid:id-" + i);
        }

        Collection collection = new Collection(ID, DOMS_COLLECTION);

        when(domsObjectIDQueryer.findNextIn(collection, new Date(0)))
                .thenReturn(new DOMSObjectIDQueryResult(idsUnderTest.subList(0, MAX_DOMS_RESULT_SIZE), new Date(0)),
                        new DOMSObjectIDQueryResult(idsUnderTest.subList(MAX_DOMS_RESULT_SIZE, 15), new Date(0)));

        boolean withHandle = false;
        createAndUseMetadata(idsUnderTest, withHandle);

        Iterator<JobDTO> jobs = createJobsIterator(idsUnderTest, collection);
        when(jobsDAO.findJobsPending(anyInt())).thenReturn(jobs);

        when(CONFIG_SPY.getNumberOfThreads()).thenReturn(4);



        PIDRegistrations.doRegistrations();



        verifyHandlesUpdatedAtDoms(idsUnderTest);
        verifyHandlesUpdatedInRegistry(idsUnderTest);

        verifyNoMoreInteractions(domsUpdater);
        verifyNoMoreInteractions(handleRegistry);
    }

    private void verifyHandlesUpdatedAtDoms(List<String> ids) {
        // Verify that metadata at DOMS is updated with handles.
        for(String id : ids){
            PIDHandle handle = new PIDHandle(HANDLE_PREFIX, id);
            ArgumentCaptor<DOMSMetadata> firstMetadataArg = ArgumentCaptor.forClass(DOMSMetadata.class);
            verify(domsUpdater).update(eq(id), firstMetadataArg.capture());
            assertTrue(firstMetadataArg.getValue().handleExists(handle), "Metadata was not updated with correct handle");
        }
    }

    private void verifyHandlesUpdatedInRegistry(List<String> ids) {
        // Verify that handle registry is updated with handles.
        for (String objectId : ids) {
            PIDHandle handle = new PIDHandle(HANDLE_PREFIX, objectId);
            verify(handleRegistry).registerPid(eq(handle), eq(CONFIG.getPidPrefix() + "/" + ID + "/" + objectId));
        }
    }

    private Iterator<JobDTO> createJobsIterator(List<String> ids, Collection collection) {
        return ids.stream().map(id -> new JobDTO(id, collection.getId(), null)).iterator();
    }

    private void createAndUseMetadata(List<String> ids, boolean withHandle) throws MethodFailedException,
            InvalidResourceException, InvalidCredentialsException {
        for(String id : ids){
            when(domsClient.getDatastreamContents(id)).
                    thenReturn(MetadataGenerator.createDatastream(id, "some/path",
                            withHandle ? Optional.of("hdl:" + HANDLE_PREFIX + "/" + id) : Optional.empty()));
        }
    }



    @Disabled
    @Test
    public void test_doRegistrations_PerformanceWith4Threads() throws MethodFailedException,
            InvalidResourceException, InvalidCredentialsException {

        test_doRegistrations_PerformanceWithXThreads(4);
    }

    @Disabled
    @Test
    public void test_doRegistrations_PerformanceWith1Thread() throws MethodFailedException,
            InvalidResourceException, InvalidCredentialsException {

        test_doRegistrations_PerformanceWithXThreads(1);
    }

    private void test_doRegistrations_PerformanceWithXThreads(int numberOfThreads) throws MethodFailedException,
            InvalidResourceException, InvalidCredentialsException {

        List<String> idsUnderTest = new ArrayList<>();
        for (int i = 0; i < 35; i++) {
            idsUnderTest.add("uuid:id-" + i);
        }

        Collection collection = new Collection(ID, DOMS_COLLECTION);

        when(domsObjectIDQueryer.findNextIn(collection, new Date(0)))
                .thenReturn(new DOMSObjectIDQueryResult(idsUnderTest.subList(0, 10), new Date(0)),
                        new DOMSObjectIDQueryResult(idsUnderTest.subList(10, 20), new Date(0)),
                        new DOMSObjectIDQueryResult(idsUnderTest.subList(20, 30), new Date(0)),
                        new DOMSObjectIDQueryResult(idsUnderTest.subList(30, 35), new Date(0)));

        boolean withHandle = false;
        createAndUseMetadata(idsUnderTest, withHandle);

        doAnswer(invocationOnMock -> {
            Thread.sleep(100);
            return null;}
        ).when(domsUpdater).update(anyString(), any());

        doAnswer(invocationOnMock -> {
            Thread.sleep(100);
            return null;}
        ).when(handleRegistry).registerPid(any(), anyString());

        Iterator<JobDTO> jobs = createJobsIterator(idsUnderTest, collection);
        when(jobsDAO.findJobsPending(anyInt())).thenReturn(jobs);

        when(CONFIG_SPY.getNumberOfThreads()).thenReturn(numberOfThreads);


        long startTime = System.currentTimeMillis();
        PIDRegistrations.doRegistrations();
        long endTime = System.currentTimeMillis();
        System.out.println("Registration with " + numberOfThreads + " thread(s) took " + (endTime - startTime) + " ms.");
    }
}
