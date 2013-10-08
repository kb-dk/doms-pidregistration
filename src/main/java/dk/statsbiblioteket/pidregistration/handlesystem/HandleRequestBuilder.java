package dk.statsbiblioteket.pidregistration.handlesystem;

import dk.statsbiblioteket.pidregistration.PIDHandle;
import net.handle.hdllib.AddValueRequest;
import net.handle.hdllib.AdminRecord;
import net.handle.hdllib.AuthenticationInfo;
import net.handle.hdllib.Common;
import net.handle.hdllib.CreateHandleRequest;
import net.handle.hdllib.DeleteHandleRequest;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.ModifyValueRequest;
import net.handle.hdllib.ValueReference;

import java.nio.charset.Charset;

/**
 * Responsible for building requests used modify the global handle registry data
 */
public class HandleRequestBuilder {

    /**
     * Admin index aka Handle index, default value 300
     */
    private static final ValueReference[] REFERENCES = null;
    private static final Boolean ADMIN_READ = true;
    private static final Boolean ADMIN_WRITE = true;
    private static final Boolean PUBLIC_READ = true;
    private static final Boolean PUBLIC_WRITE = false;


    private String adminId;
    private int adminIdIndex;
    private int adminRecordIndex;
    private int valueRecordIndex;

    private Charset encoding;
    private AuthenticationInfo authenticationInfo;

    public HandleRequestBuilder(String adminId, int adminIdIndex, int adminRecordIndex,
                                int valueRecordIndex,
                                Charset encoding, AuthenticationInfo authenticationInfo) {
        this.adminId = adminId;
        this.adminIdIndex = adminIdIndex;
        this.adminRecordIndex = adminRecordIndex;
        this.valueRecordIndex = valueRecordIndex;
        this.encoding = encoding;
        this.authenticationInfo = authenticationInfo;
    }

    /**
     * Used for creating a new handle and attaching a URL to it
     * @param handle the handle
     * @param url the URL
     * @return the finished request
     */
    public CreateHandleRequest buildCreateHandleRequest(PIDHandle handle, String url) {
        HandleValue[] handleValues = {
                buildHandleValue(adminRecordIndex, "HS_ADMIN", Encoder.encodeAdminRecord(buildAdminRecord())),
                buildHandleValue(valueRecordIndex, "URL", encode(url))
        };
        return new CreateHandleRequest(encode(handle.asString()), handleValues, authenticationInfo);
    }

    private AdminRecord buildAdminRecord() {
        return new AdminRecord(encode(adminId),
                               adminIdIndex,
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
    }

    private byte[] encode(String string) {
        return string.getBytes(encoding);
    }

    private HandleValue buildHandleValue(int index, String type, byte[] data) {
        int timestamp = (int) (System.currentTimeMillis() / 1000);
        return new HandleValue(index,
                               encode(type),
                               data, HandleValue.TTL_TYPE_RELATIVE,
                               Common.DEFAULT_SESSION_TIMEOUT,
                               timestamp,
                               REFERENCES,
                               ADMIN_READ,
                               ADMIN_WRITE,
                               PUBLIC_READ,
                               PUBLIC_WRITE);
    }

    /**
     * Used for replacing a URL attached to an existing handle
     * @param handle the handle
     * @param url the URL to replace the existing URL
     * @return the finished request
     */
    public ModifyValueRequest buildModifyUrlRequest(PIDHandle handle, String url) {
        HandleValue handleValue = buildHandleValue(valueRecordIndex, "URL", encode(url));
        return new ModifyValueRequest(encode(handle.asString()), handleValue, authenticationInfo);
    }

    /**
     * Used for adding a URL to an existing handle with no URL attached
     * @param handle the existing handle
     * @param url the URL to be added
     * @return the finished request
     */
    public AddValueRequest buildAddUrlRequest(PIDHandle handle, String url) {
        HandleValue handleValue = buildHandleValue(valueRecordIndex, "URL", encode(url));
        return new AddValueRequest(encode(handle.asString()), handleValue, authenticationInfo);
    }

    /**
     * Used to delete a handle
     * @param handle the handle to delete
     * @return the finished request
     */
    public DeleteHandleRequest buildDeleteHandleRequest(PIDHandle handle) {
        return new DeleteHandleRequest(handle.asString().getBytes(encoding), authenticationInfo);
    }
}
