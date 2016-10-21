package dk.statsbiblioteket.pidregistration.utilities;

import java.util.Optional;

public class MetadataGenerator {
    private static final String DC_NAMESPACE = "http://purl.org/dc/elements/1.1/";
    private static final String OAI_NAMESPACE = "http://www.openarchives.org/OAI/2.0/oai_dc/";
    private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String XSI_SCHEMALOCATION = "http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd";

    public static String createDatastream(String objectPID, String path, Optional<String> handle) {
        return "<oai_dc:dc xmlns:dc=\"" + DC_NAMESPACE +
                "\" xmlns:oai_dc=\"" + OAI_NAMESPACE +
                "\" xmlns:xsi=\"" + XSI_NAMESPACE +
                "\" xsi:schemaLocation=\"" + XSI_SCHEMALOCATION + "\">\n" +
                "  <dc:identifier>" + objectPID + "</dc:identifier>\n" +
                "  <dc:identifier>path:" + path + "</dc:identifier>\n" +
                (handle.isPresent() ? "<dc:identifier>" + handle.get() + "</dc:identifier>" : "") +
                "</oai_dc:dc>\n";
    }
}
