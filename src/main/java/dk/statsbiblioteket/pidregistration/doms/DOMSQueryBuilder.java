package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.Collection;
import dk.statsbiblioteket.pidregistration.UnknownCollectionException;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Responsible for building the DOMS queries. In this context queries can be varied with respect to media collection
 * and date last modified. Furthermore, the query structure supports pagination.
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
    private Date fromLastModificationDateInclusive;
    private int pageSize;
    private int offset = 0;

    /**
     * Construction
     *
     * @param collection The media collection in question
     * @param fromLastModificationDateInclusive the last modification date
     * @param pageSize the page size used for pagination
     */
    public DOMSQueryBuilder(Collection collection, Date fromLastModificationDateInclusive, int pageSize) {
        this.collection = collection;
        this.fromLastModificationDateInclusive = fromLastModificationDateInclusive;
        this.pageSize = pageSize;
    }

    /**
     * Get next query with respect to pagination (for performance reasons)
     *
     * @return the next query
     */
    public String next() {
        SimpleDateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.'SSS'Z'");
        String query =
                String.format(QUERY_TEMPLATE + " " + PAGE_TEMPLATE,
                              translate(), iso8601Format.format(fromLastModificationDateInclusive), pageSize, offset);
        offset += pageSize;
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
