package dk.statsbiblioteket.pidregistration;

/**
 * The currently supported collections
 */
public enum Collection {
    DOMS_RADIO_TV("doms_radioTVCollection"),
    DOMS_REKLAMEFILM("doms_reklamefilm");

    private String id;

    private Collection(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
