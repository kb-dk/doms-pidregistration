package dk.statsbiblioteket.pidregistration;

import junit.framework.TestCase;
import net.handle.hdllib.AbstractResponse;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.Common;
import net.handle.hdllib.CreateHandleRequest;
import net.handle.hdllib.DeleteHandleRequest;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleResolver;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.PublicKeyAuthenticationInfo;
import net.handle.hdllib.Util;
import net.handle.hdllib.ValueReference;

import java.io.File;
import java.nio.charset.Charset;

/**
 * Test communication with handle server.
 *
 * This test will destroy the following handles on the server if present:
 * - hdl:109.3.1/test1
 * - hdl:109.3.1/test2
 * - hdl:109.3.1/test3
 * - hdl:109.3.1/test4
 * It assumes the certificate to update the handle server is stored at ~/.config/handle/privadm.cert
 */
public class HandleHandlerTest extends TestCase {
    private static final String PID1 = "test1";
    private static final String PID2 = "test2";
    private static final String PID3 = "test3";
    private static final String PID4 = "test4";
    private static final String URL_PATTERN
            = "http://devel05.statsbiblioteket.dk:9381/kultur/#/?recordId=doms_radioTVCollection:%s";

    private static final String ADMIN_ID = "0.NA/109.3.1";
    private static final Charset DEFAULT_ENCODING = Charset.forName("UTF8");
    private static final int ADMIN_INDEX = 300;
    private static final int ADMIN_RECORD_INDEX = 200;
    private static final int URL_RECORD_INDEX = 1;
    private static final ValueReference[] REFERENCES = null;
    private static final Boolean ADMIN_READ = true;
    private static final Boolean ADMIN_WRITE = true;
    private static final Boolean PUBLIC_READ = true;
    private static final Boolean PUBLIC_WRITE = false;

    private PublicKeyAuthenticationInfo authInfo;

    private PropertyBasedRegistrarConfiguration config
            = new PropertyBasedRegistrarConfiguration(
            getClass().getResourceAsStream("/handleregistrar.properties"));

    public void setUp() throws Exception {
        authInfo = new PublicKeyAuthenticationInfo(
                ADMIN_ID.getBytes(DEFAULT_ENCODING), ADMIN_INDEX,
                Util.getPrivateKeyFromFileWithPassphrase(new File(
                        System.getProperty("user.home")
                                + "/.config/handle/admpriv.bin"), ""));
        HandleResolver handleResolver = new HandleResolver();
        AdminRecord admin = new AdminRecord(ADMIN_ID.getBytes(DEFAULT_ENCODING),
                                            ADMIN_INDEX,
                                            AdminRecord.PRM_ADD_HANDLE,
                                            AdminRecord.PRM_DELETE_HANDLE,
                                            AdminRecord.PRM_ADD_NA,
                                            AdminRecord.PRM_DELETE_NA,
                                            AdminRecord.PRM_READ_VALUE,
                                            AdminRecord.PRM_MODIFY_VALUE,
                                            AdminRecord.PRM_REMOVE_VALUE,
                                            AdminRecord.PRM_ADD_VALUE,
                                            AdminRecord.PRM_MODIFY_ADMIN,
                                            AdminRecord.PRM_REMOVE_ADMIN,
                                            AdminRecord.PRM_ADD_ADMIN,
                                            AdminRecord.PRM_LIST_HANDLES);
        HandleValue adminRecord = new HandleValue(ADMIN_RECORD_INDEX,
                                                  "HS_ADMIN".getBytes(
                                                          DEFAULT_ENCODING),
                                                  Encoder.encodeAdminRecord(
                                                          admin),
                                                  HandleValue.TTL_TYPE_RELATIVE,
                                                  Common.DEFAULT_SESSION_TIMEOUT,
                                                  (int)(System.currentTimeMillis()
                                                          / 1000), REFERENCES,
                                                  ADMIN_READ, ADMIN_WRITE,
                                                  PUBLIC_READ, PUBLIC_WRITE);

        //Create PID1 with correct URL
        HandleValue[] values = {adminRecord,
                new HandleValue(URL_RECORD_INDEX,
                                "URL".getBytes(DEFAULT_ENCODING),
                                String.format(URL_PATTERN, PID1).getBytes(DEFAULT_ENCODING),
                                HandleValue.TTL_TYPE_RELATIVE,
                                Common.DEFAULT_SESSION_TIMEOUT,
                                (int) (System.currentTimeMillis() / 1000), REFERENCES,
                                ADMIN_READ, ADMIN_WRITE, PUBLIC_READ,
                                PUBLIC_WRITE)};
        CreateHandleRequest request = new CreateHandleRequest(
                ("109.3.1/" + PID1).getBytes(DEFAULT_ENCODING), values, authInfo);
        handleResolver.processRequest(request);

        //Create PID2 with alternate URL
        values = new HandleValue[]{adminRecord,
                new HandleValue(URL_RECORD_INDEX,
                                "URL".getBytes(DEFAULT_ENCODING),
                                "http://example.com".getBytes(DEFAULT_ENCODING),
                                HandleValue.TTL_TYPE_RELATIVE,
                                Common.DEFAULT_SESSION_TIMEOUT,
                                (int) (System.currentTimeMillis() / 1000), REFERENCES,
                                ADMIN_READ, ADMIN_WRITE, PUBLIC_READ,
                                PUBLIC_WRITE)};
        request = new CreateHandleRequest(
                ("109.3.1/" + PID2).getBytes(DEFAULT_ENCODING), values, authInfo);
        handleResolver.processRequest(request);

        //Create PID3 with no URL
        values = new HandleValue[]{adminRecord};
        request = new CreateHandleRequest(
                ("109.3.1/" + PID3).getBytes(DEFAULT_ENCODING), values, authInfo);
        handleResolver.processRequest(request);

    }

    public void tearDown() throws Exception {
        HandleResolver handleResolver = new HandleResolver();
        DeleteHandleRequest request = new DeleteHandleRequest(
                ("109.3.1/" + PID1).getBytes(DEFAULT_ENCODING), authInfo);
        handleResolver.processRequest(request);
        request = new DeleteHandleRequest(
                ("109.3.1/" + PID2).getBytes(DEFAULT_ENCODING), authInfo);
        handleResolver.processRequest(request);
        request = new DeleteHandleRequest(
                ("109.3.1/" + PID3).getBytes(DEFAULT_ENCODING), authInfo);
        handleResolver.processRequest(request);
        request = new DeleteHandleRequest(
                ("109.3.1/" + PID4).getBytes(DEFAULT_ENCODING), authInfo);
        handleResolver.processRequest(request);
    }

    public void testRegisterPid() throws Exception {
        PartialMockupHandleHandler handler = new PartialMockupHandleHandler(
                config);
        handler.registerPid(PID1, "109.3.1/" + PID1, URL_PATTERN);
        assertEquals(0, handler.addPidToServerCount);
        assertEquals(0, handler.addUrlToPidAtServerCount);
        assertEquals(0, handler.replaceUrlOfPidAtServerCount);
        handler.registerPid(PID2, "109.3.1/" + PID2, URL_PATTERN);
        assertEquals(0, handler.addPidToServerCount);
        assertEquals(0, handler.addUrlToPidAtServerCount);
        assertEquals(1, handler.replaceUrlOfPidAtServerCount);
        assertEquals("109.3.1/" + PID2, handler.replaceUrlOfPidAtServerPid);
        assertEquals(String.format(URL_PATTERN, PID2),
                     handler.replaceUrlOfPidAtServerUrl);
        handler.registerPid(PID3, "109.3.1/" + PID3, URL_PATTERN);
        assertEquals(0, handler.addPidToServerCount);
        assertEquals(1, handler.addUrlToPidAtServerCount);
        assertEquals(1, handler.replaceUrlOfPidAtServerCount);
        assertEquals("109.3.1/" + PID3, handler.addUrlToPidAtServerPid);
        assertEquals(String.format(URL_PATTERN, PID3),
                     handler.addUrlToPidAtServerUrl);
        handler.registerPid(PID4, "109.3.1/" + PID4, URL_PATTERN);
        assertEquals(1, handler.addPidToServerCount);
        assertEquals(1, handler.addUrlToPidAtServerCount);
        assertEquals(1, handler.replaceUrlOfPidAtServerCount);
        assertEquals("109.3.1/" + PID4, handler.addPidToServerPid);
        assertEquals(String.format(URL_PATTERN, PID4),
                     handler.addPidToServerUrl);
    }

    public void testAddPidToServer() throws Exception {
        HandleHandler handler = new HandleHandler(config);
        String url = String.format(URL_PATTERN, PID1);
        handler.addPidToServer("109.3.1/" + PID4, url);
        HandleValue[] handleValues = new HandleResolver()
                .resolveHandle("109.3.1/" + PID4, null, null);
        assertNotNull(handleValues);
        boolean found = false;
        for (HandleValue handleValue : handleValues) {
            found |= handleValue.getDataAsString().equals(url) && handleValue
                    .getTypeAsString().equals("URL");
        }
        assertTrue(found);
    }

    public void testAddUrlToPidAtServer() throws Exception {
        HandleHandler handler = new HandleHandler(config);
        String url = String.format(URL_PATTERN, PID1);
        handler.addUrlToPidAtServer("109.3.1/" + PID3, url);
        HandleValue[] handleValues = new HandleResolver()
                .resolveHandle("109.3.1/" + PID3, null, null);
        assertNotNull(handleValues);
        boolean found = false;
        for (HandleValue handleValue : handleValues) {
            found |= handleValue.getDataAsString().equals(url) && handleValue
                    .getTypeAsString().equals("URL");
        }
        assertTrue(found);
    }

    public void testReplaceUrlOfPidAtServer() throws Exception {
        HandleHandler handler = new HandleHandler(config);
        String url = String.format(URL_PATTERN, PID1);
        handler.replaceUrlOfPidAtServer("109.3.1/" + PID2, url);
        HandleValue[] handleValues = new HandleResolver()
                .resolveHandle("109.3.1/" + PID2, null, null);
        assertNotNull(handleValues);
        boolean found = false;
        for (HandleValue handleValue : handleValues) {
            found |= handleValue.getDataAsString().equals(url) && handleValue
                    .getTypeAsString().equals("URL");
        }
        assertTrue(found);
    }

}
