package dk.statsbiblioteket.pidregistration.handlesystem;

import dk.statsbiblioteket.pidregistration.PIDHandle;
import dk.statsbiblioteket.pidregistration.configuration.PropertyBasedRegistrarConfiguration;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import net.handle.hdllib.HandleValue;
import org.junit.Test;

public class GlobalHandleRegistryTest {
    private static final PIDHandle PID = new PIDHandle("109.3.1", "uuid:663d0baa-c08f-4b6e-bd07-35ec0b382ebb");
    private static final String URL
            = "pid.statsbiblioteket.dk/doms_radioTVCollection/uuid:663d0baa-c08f-4b6e-bd07-35ec0b382ebb";

    private PropertyBasedRegistrarConfiguration config
            = new PropertyBasedRegistrarConfiguration(
            getClass().getResourceAsStream("/doms-pidregistration.properties"));

    private boolean inTestmode = true;
    @Mocked(methods = "registerPid(PIDHandle, String)", inverse = true)
    private GlobalHandleRegistry globalHandleRegistry = new GlobalHandleRegistry(config, inTestmode);

    @Test
    public void testCreateNewPidRegistration() {
        new NonStrictExpectations() {
            {
                invoke(globalHandleRegistry, "lookupHandle", PID); result = null;
            }
        };

        globalHandleRegistry.registerPid(PID, URL);

        new Verifications() {
            {
                invoke(globalHandleRegistry, "createPidWithUrl", PID, URL); times = 1;
                invoke(globalHandleRegistry, "addUrlToPid", withAny(PIDHandle.class), anyString); times = 0;
                invoke(globalHandleRegistry, "replaceUrlOfPid", withAny(PIDHandle.class), anyString); times = 0;
            }
        };
    }

    @Test
    public void testAddUrlToPid() {
        new NonStrictExpectations() {
            {
                invoke(globalHandleRegistry, "findFirstWithTypeUrl", HandleValue[].class); result = null;
            }
        };

        globalHandleRegistry.registerPid(PID, URL);

        new Verifications() {
            {
                invoke(globalHandleRegistry, "createPidWithUrl", withAny(PIDHandle.class), anyString); times = 0;
                invoke(globalHandleRegistry, "addUrlToPid", PID, URL); times = 1;
                invoke(globalHandleRegistry, "replaceUrlOfPid", withAny(PIDHandle.class), anyString); times = 0;
            }
        };
    }
    
    @Test
    public void testReplaceUrlAtPidWithDifferentUrl() {
        new NonStrictExpectations() {
            {
                invoke(globalHandleRegistry, "findFirstWithTypeUrl", withAny(HandleValue[].class)); result = URL;
            }
        };
        final String someOtherUrl = "http://some.other.url";

        globalHandleRegistry.registerPid(PID, someOtherUrl);

        new Verifications() {
            {
                invoke(globalHandleRegistry, "createPidWithUrl", withAny(PIDHandle.class), anyString); times = 0;
                invoke(globalHandleRegistry, "addUrlToPid", withAny(PIDHandle.class), anyString); times = 0;
                invoke(globalHandleRegistry, "replaceUrlOfPid", PID, someOtherUrl); times = 1;
            }
        };
    }
    
    @Test
    public void testThatReplacingUrlAtPidWithSameUrlDoesNothing() {
        new NonStrictExpectations() {
            {
                invoke(globalHandleRegistry, "findFirstWithTypeUrl", withAny(HandleValue[].class)); result = URL;
            }
        };

        globalHandleRegistry.registerPid(PID, URL);

        new Verifications() {
            {
                invoke(globalHandleRegistry, "createPidWithUrl", withAny(PIDHandle.class), anyString); times = 0;
                invoke(globalHandleRegistry, "addUrlToPid", withAny(PIDHandle.class), anyString); times = 0;
                invoke(globalHandleRegistry, "replaceUrlOfPid", withAny(PIDHandle.class), anyString); times = 0;
            }
        };
    }
}
