package dk.statsbiblioteket.pidregistration;


import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test command line parsing.
 */
public class PIDRegistrationsCommandLineInterfaceTest {
    
    @Test
    public void testParseOptions() throws Exception {
        CommandLine cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{});
        assertNotNull(cl);
        cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{"-c", "config.file"});
        assertNull(cl);
        cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{"--help"});
        assertNull(cl);
        cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{"-x"});
        assertNull(cl);
        cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{"-d", "date"});
        assertNull(cl);
        cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{"-d", "2013-01-29"});
        assertNull(cl);
        cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{"-t"});
        assertNull(cl);
        cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{"-t 100"});
        assertTrue(cl.hasOption("t"));
        cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{"--test=100"});
        assertTrue(cl.hasOption("t"));
    }
}
