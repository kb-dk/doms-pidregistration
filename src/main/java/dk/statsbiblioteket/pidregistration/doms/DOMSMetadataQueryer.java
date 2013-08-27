package dk.statsbiblioteket.pidregistration.doms;

public class DOMSMetadataQueryer {

    private DOMSClient domsClient;

    public DOMSMetadataQueryer(DOMSClient domsClient) {
        this.domsClient = domsClient;
    }

    public DOMSMetadata getMetadataForObject(String objectId) {
        try {
            String dataStream =
                    domsClient.getCentralWebservice().getDatastreamContents(objectId, domsClient.getDatastreamId());
            return new DOMSMetadata(dataStream);
        } catch (Exception serverOperationFailed) {
            throw new BackendMethodFailedException(
                    "Backendmethod failed while trying to to read DC from '"
                            + objectId + "'", serverOperationFailed);
        }
    }
}
