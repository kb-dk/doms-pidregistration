package dk.statsbiblioteket.pidregistration;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 */
public class PIDDOMSQueryBuilder {
    private static final String QUERY_TEMPLATE = "SELECT ?object ?cm ?date WHERE {\n" +
            " ?object <info:fedora/fedora-system:def/model#hasModel> ?cm ;\n" +
            "         <info:fedora/fedora-system:def/view#lastModifiedDate> ?date .\n" +
            " FILTER (\n" +
            "   ?cm = <info:fedora/doms:ContentModel_Program> || ?cm = <info:fedora/doms:ContentModel_Reklamfilm>\n" +
            " )\n" +
            " FILTER (\n" +
            "   ?date >= '%s'^^xsd:dateTime\n" +
            " )\n" +
            "} ORDER BY ?date";

    public static String buildQuery(Date fromInclusive) {
        SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'");
        return String.format(QUERY_TEMPLATE, iso8601Format.format(fromInclusive));
    }
}
