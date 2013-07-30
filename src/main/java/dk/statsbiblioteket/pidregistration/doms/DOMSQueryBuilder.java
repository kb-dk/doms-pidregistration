package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.Collection;
import dk.statsbiblioteket.pidregistration.UnknownCollectionException;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 */
public class DOMSQueryBuilder {
    private static final String QUERY_TEMPLATE = "SELECT ?object ?date WHERE {\n" +
            " ?object <info:fedora/fedora-system:def/model#hasModel> <info:fedora/doms:%s> ;\n" +
            "         <info:fedora/fedora-system:def/view#lastModifiedDate> ?date .\n" +
            " FILTER (\n" +
            "   ?date >= '%s'^^xsd:dateTime\n" +
            " )\n" +
            "} ORDER BY ?date";

    private Collection collection;
    private Date fromInclusive;

    public DOMSQueryBuilder(Collection collection, Date fromInclusive) {
        this.collection = collection;
        this.fromInclusive = fromInclusive;
    }

    public String build() {
        SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'");
        return String.format(QUERY_TEMPLATE, translate(), iso8601Format.format(fromInclusive));
    }

    private String translate() {
        switch (collection) {
            case DOMS_RADIO_TV:
                return "ContentModel_Program";
            case DOMS_REKLAMEFILM:
                return "ContentModel_Reklamefilm";
            default: throw new UnknownCollectionException("unknown collection: " + collection);
        }
    }
}
