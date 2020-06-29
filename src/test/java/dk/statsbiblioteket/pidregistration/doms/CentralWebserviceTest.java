package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.CentralWebservice;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.CentralWebserviceService;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.InvalidCredentialsException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.MethodFailedException;
import dk.statsbiblioteket.pidregistration.wsgen.centralwebservice.RecordDescription;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Ignore
public class CentralWebserviceTest {

    private static final QName CENTRAL_WEBSERVICE_SERVICE = new QName(
            "http://central.doms.statsbiblioteket.dk/",
            "CentralWebserviceService");

    @Test
    public void testCentralWebservice() throws MalformedURLException, InvalidCredentialsException, MethodFailedException, ParseException {
        URL endpoint = new URL("http://alhena:7980/centralWebservice-service/central/?wsdl");

        CentralWebservice centralWebservice =
                new CentralWebserviceService(endpoint, CENTRAL_WEBSERVICE_SERVICE).getCentralWebservicePort();

        Map<String, Object> domsAPILogin = ((BindingProvider) centralWebservice)
                .getRequestContext();
        domsAPILogin.put(BindingProvider.USERNAME_PROPERTY, "fedoraAdmin");
        domsAPILogin.put(BindingProvider.PASSWORD_PROPERTY, "fedoraAdminPass");

        long time = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse("2013-08-27").getTime();
        List<RecordDescription> recordDescriptions =
                centralWebservice.getIDsModified(time, "doms:Collection_Reklamefilm", "SummaVisible", "Published", 0, 10000);
        for (RecordDescription recordDescription : recordDescriptions) {
            System.out.println(String.format(Locale.ROOT, "PID: %s, Date: %s", recordDescription.getPid(), recordDescription.getDate()));
        }
    }
}
