package dk.statsbiblioteket.pidregistration;

import junit.framework.TestCase;
import org.apache.commons.cli.CommandLine;

/**
 * Test command line parsing.
 */
public class PIDRegistrationsCommandLineInterfaceTest extends TestCase {
    public void testParseOptions() throws Exception {
        CommandLine cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{});
        assertNull(cl);
        cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{"-c", "config.file"});
        assertNull(cl);
        cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{"--help"});
        assertNull(cl);
        cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{"-x"});
        assertNull(cl);
        cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{"-d", "date"});
        assertNull(cl);
        cl = PIDRegistrationsCommandLineInterface.parseOptions(new String[]{"-d", "2013-01-29"});
        assertNotNull(cl);
        assertEquals("2013-01-29", cl.getOptionValue("d"));
    }
}
