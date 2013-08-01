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

    private static final String PAGE_TEMPLATE = "LIMIT %s OFFSET %s";

    private Collection collection;
    private Date fromInclusive;
    private int windowSize;
    private int offset = 0;

    public DOMSQueryBuilder(Collection collection, Date fromInclusive, int windowSize) {
        this.collection = collection;
        this.fromInclusive = fromInclusive;
        this.windowSize = windowSize;
    }

    public String next() {
        SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'");
        String query =
                String.format(QUERY_TEMPLATE + " " + PAGE_TEMPLATE,
                              translate(), iso8601Format.format(fromInclusive), windowSize, offset);
        offset += windowSize;
        return query;
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
