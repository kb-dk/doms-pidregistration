package dk.statsbiblioteket.pidregistration;

/**
 * Given an RDF query that returns a list of object ID's, add a handle ID to
 * each object, and register these objects
 * in the handle server, resolving to URLs with the given pattern.
 */
public interface HandleRegistrar {
    /**
     * Given a query that returns a list of object ids; add a handle ID for each
     * object, and register the handle in the
     * handle server using the url pattern.
     *
     * @param query      An RDF query that returns a list of object ids.
     *                   Example:
     *                   select $object from <#ri> where $object <dc:identifier> 'uuid:00022366-955b-4cb5-9646-e04c9262bd6f'
     * @param urlPattern A pattern for the url to register in the handle server.
     *                   Should use the format of {@link java.util.Formatter},
     *                   using a single %s to denote where the DOMS ID should be
     *                   inserted into the string.
     *                   This is the ID without info:/fedora prepended, and it
     *                   is NOT the handle id.
     *                   Example:
     *                   https://www.statsbiblioteket.dk/kultur/?recordId=doms_radioTVCollection:%s
     * @return           Human readable status.
     */
    public String addHandles(String query, String urlPattern);
}
