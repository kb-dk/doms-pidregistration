package dk.statsbiblioteket.pidregistration.doms;

public class DOMSMetadataQueryer {

    private DOMSClient domsClient;

    public DOMSMetadataQueryer(DOMSClient domsClient) {
        this.domsClient = domsClient;
    }

    public DOMSMetadata getMetadataForObject(String objectId) {
        try {
            String dataStream = domsClient.getDatastreamContents(objectId);
            return new DOMSMetadata(dataStream);
        } catch (Exception serverOperationFailed) {
            throw new BackendMethodFailedException(
                    "Backendmethod failed while trying to to read DC from '"
                            + objectId + "'", serverOperationFailed);
        }
    }
}
