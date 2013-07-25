package dk.statsbiblioteket.pidregistration;

import junit.framework.TestCase;

import java.util.Calendar;
import java.util.List;

/**
 */
public class PIDTest extends TestCase {

    private PropertyBasedRegistrarConfiguration config
            = new PropertyBasedRegistrarConfiguration(
            getClass().getResourceAsStream("/handleregistrar.properties"));
    private DomsHandler domsHandler;

    @Override
    protected void setUp() throws Exception {
         domsHandler = new DomsHandler(config);
    }

    /*
    - Do query against DOMS for candidates to add to the PID server
    - For all candidates:
      - Check that it hasn't already been added if so, log. if not:
      - Add to pid server
      - Add to DOMS object
    - Done
     */
    public void testSomething() {


        // - Do query against DOMS for candidates to add to the PID server
        Calendar january1st2013 = Calendar.getInstance();
        january1st2013.clear();
        january1st2013.set(2013, Calendar.JANUARY, 1);

        String query = PIDDOMSQueryBuilder.buildQuery(january1st2013.getTime());

        List<String> objectIds = domsHandler.findObjectFromQuery(query);

        // - For all candidates:
        for (String id : objectIds) {


        }








        assertTrue(true);
    }

    @Override
    protected void tearDown() throws Exception {
        domsHandler = null;
    }
}
