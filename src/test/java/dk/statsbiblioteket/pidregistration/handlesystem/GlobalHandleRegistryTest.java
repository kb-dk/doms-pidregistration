package dk.statsbiblioteket.pidregistration.handlesystem;

import dk.statsbiblioteket.pidregistration.PIDHandle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import net.handle.hdllib.HandleValue;

public class GlobalHandleRegistryTest {
    private static final PIDHandle PID = new PIDHandle("109.3.1", "uuid:663d0baa-c08f-4b6e-bd07-35ec0b382ebb");
    private static final String URL
            = "pid.statsbiblioteket.dk/doms_radioTVCollection/uuid:663d0baa-c08f-4b6e-bd07-35ec0b382ebb";

    @Test
    public void testCreateNewPidRegistration() {
        GlobalHandleRegistry globalHandleRegistry = mock(GlobalHandleRegistry.class);
        when(globalHandleRegistry.registerPid(eq(PID), eq(URL))).thenCallRealMethod();

        globalHandleRegistry.registerPid(PID, URL);

        verify(globalHandleRegistry, times(1)).createPidWithUrl(PID, URL);
        verify(globalHandleRegistry, times(0)).addUrlToPid(any(PIDHandle.class), anyString());
        verify(globalHandleRegistry, times(0)).replaceUrlOfPid(any(PIDHandle.class), anyString());
    }

    @Test
    public void testAddUrlToPid() {
        GlobalHandleRegistry globalHandleRegistry = mock(GlobalHandleRegistry.class);
        when(globalHandleRegistry.registerPid(eq(PID), eq(URL))).thenCallRealMethod();
        HandleValue hv = new HandleValue();
        when(globalHandleRegistry.lookupHandle(eq(PID))).thenReturn(new HandleValue[]{hv});

        globalHandleRegistry.registerPid(PID, URL);

        verify(globalHandleRegistry, times(0)).createPidWithUrl(any(PIDHandle.class), anyString());
        verify(globalHandleRegistry, times(1)).addUrlToPid(PID, URL);
        verify(globalHandleRegistry, times(0)).replaceUrlOfPid(any(PIDHandle.class), anyString());
    }
    
    @Test
    public void testReplaceUrlAtPidWithDifferentUrl() {
        final String someOtherUrl = "http://some.other.url";
        
        GlobalHandleRegistry globalHandleRegistry = mock(GlobalHandleRegistry.class);
        when(globalHandleRegistry.registerPid(eq(PID), eq(someOtherUrl))).thenCallRealMethod();
        HandleValue hv = new HandleValue(); 
        when(globalHandleRegistry.lookupHandle(eq(PID))).thenReturn(new HandleValue[]{hv});
        when(globalHandleRegistry.findFirstWithTypeUrl(any(HandleValue[].class))).thenReturn(URL);

        globalHandleRegistry.registerPid(PID, someOtherUrl);
        
        verify(globalHandleRegistry, times(0)).createPidWithUrl(any(PIDHandle.class), anyString());
        verify(globalHandleRegistry, times(0)).addUrlToPid(any(PIDHandle.class), anyString());
        verify(globalHandleRegistry, times(1)).replaceUrlOfPid(any(PIDHandle.class), eq(someOtherUrl));
    }
    
    @Test
    public void testThatReplacingUrlAtPidWithSameUrlDoesNothing() {
        GlobalHandleRegistry globalHandleRegistry = mock(GlobalHandleRegistry.class);
        when(globalHandleRegistry.registerPid(eq(PID), eq(URL))).thenCallRealMethod();
        HandleValue hv = new HandleValue();
        when(globalHandleRegistry.lookupHandle(eq(PID))).thenReturn(new HandleValue[]{hv});
        when(globalHandleRegistry.findFirstWithTypeUrl(any(HandleValue[].class))).thenReturn(URL);

        globalHandleRegistry.registerPid(PID, URL);
        
        verify(globalHandleRegistry, times(0)).createPidWithUrl(any(PIDHandle.class), anyString());
        verify(globalHandleRegistry, times(0)).addUrlToPid(any(PIDHandle.class), anyString());
        verify(globalHandleRegistry, times(0)).replaceUrlOfPid(any(PIDHandle.class), anyString());
    }
}
