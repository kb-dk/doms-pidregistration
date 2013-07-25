package dk.statsbiblioteket.pidregistration;

import junit.framework.TestCase;
import org.apache.commons.cli.CommandLine;

/**
 * Test command line parsing.
 */
public class HandleRegistrarToolTest extends TestCase {
    public void testParseOptions() throws Exception {
        CommandLine cl = HandleRegistrarTool.parseOptions(new String[]{});
        assertNull(cl);
        cl = HandleRegistrarTool.parseOptions(new String[]{"-c", "config.file"});
        assertNull(cl);
        cl = HandleRegistrarTool.parseOptions(new String[]{"-u", "urlpattern"});
        assertNull(cl);
        cl = HandleRegistrarTool.parseOptions(new String[]{"-q", "query"});
        assertNull(cl);
        cl = HandleRegistrarTool.parseOptions(new String[]{"--help"});
        assertNull(cl);
        cl = HandleRegistrarTool.parseOptions(new String[]{"-x"});
        assertNull(cl);
        cl = HandleRegistrarTool.parseOptions(new String[]{"-q", "query", "-u", "urlpattern"});
        assertNotNull(cl);
        assertEquals("query", cl.getOptionValue("q"));
        assertEquals("urlpattern", cl.getOptionValue("u"));
        cl = HandleRegistrarTool.parseOptions(new String[]{"--query", "query", "-u", "urlpattern"});
        assertNotNull(cl);
        assertEquals("query", cl.getOptionValue("q"));
        assertEquals("urlpattern", cl.getOptionValue("u"));
        cl = HandleRegistrarTool.parseOptions(new String[]{"--query", "query", "--url-pattern", "urlpattern"});
        assertNotNull(cl);
        assertEquals("query", cl.getOptionValue("q"));
        assertEquals("urlpattern", cl.getOptionValue("u"));
        cl = HandleRegistrarTool.parseOptions(new String[]{"--query", "query", "--url-pattern", "urlpattern", "-c", "configfile"});
        assertNotNull(cl);
        assertEquals("query", cl.getOptionValue("q"));
        assertEquals("urlpattern", cl.getOptionValue("u"));
        assertEquals("configfile", cl.getOptionValue("c"));
        cl = HandleRegistrarTool.parseOptions(new String[]{"--query", "query", "--url-pattern", "urlpattern", "--config-file", "configfile"});
        assertNotNull(cl);
        assertEquals("query", cl.getOptionValue("q"));
        assertEquals("urlpattern", cl.getOptionValue("u"));
        assertEquals("configfile", cl.getOptionValue("c"));
        cl = HandleRegistrarTool.parseOptions(new String[]{"-c", "config/handleregistrar.properties", "-q", "select $object from <#ri> where $object <dc:identifier> 'uuid:00022366-955b-4cb5-9646-e04c9262bd6f'", "-u", "http://devel05.statsbiblioteket.dk:9381/kultur/#/?recordId=doms_radioTVCollection:%s"});
        assertNotNull(cl);
        assertEquals("select $object from <#ri> where $object <dc:identifier> 'uuid:00022366-955b-4cb5-9646-e04c9262bd6f'", cl.getOptionValue("q"));
        assertEquals("http://devel05.statsbiblioteket.dk:9381/kultur/#/?recordId=doms_radioTVCollection:%s", cl.getOptionValue("u"));
        assertEquals("config/handleregistrar.properties", cl.getOptionValue("c"));
    }
}
