package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.PIDHandle;
import dk.statsbiblioteket.util.xml.DOM;

import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetadataTest {

    @Test
    public void testWithHandle() throws TransformerException {
        String metadataString = DOM.domToString(DOM.streamToDOM(getClass().getResourceAsStream("/with.xml"), true));
        DOMSMetadata metadata = new DOMSMetadata(metadataString);
        assertTrue(metadata.handleExists(new PIDHandle("109.3.1", "uuid:001fdf2b-a05a-40de-a43b-787f1ba9041f")));
    }

    @Test
    public void testWithoutHandle() throws TransformerException {
        String metadataString = DOM.domToString(DOM.streamToDOM(getClass().getResourceAsStream("/without.xml"), true));
        DOMSMetadata metadata = new DOMSMetadata(metadataString);
        assertFalse(metadata.handleExists(new PIDHandle("109.3.1", "uuid:001fdf2b-a05a-40de-a43b-787f1ba9041f")));
    }

    @Test
    public void testAttachHandle() throws TransformerException {
        String metadataString = DOM.domToString(DOM.streamToDOM(getClass().getResourceAsStream("/without.xml"), true));
        DOMSMetadata metadata = new DOMSMetadata(metadataString);
        PIDHandle handle = new PIDHandle("109.3.1", "uuid:001fdf2b-a05a-40de-a43b-787f1ba9041f");
        metadata.attachHandle(handle);
        assertTrue(metadata.handleExists(handle));
    }
}
