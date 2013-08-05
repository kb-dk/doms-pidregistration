package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.PIDHandle;
import dk.statsbiblioteket.util.xml.DOM;
import org.junit.Test;

import javax.xml.transform.TransformerException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MetadataTest {

    @Test
    public void testWithHandle() {
        DOMSMetadata metadata = new DOMSMetadata(DOM.streamToDOM(getClass().getResourceAsStream("/with.xml"), true));
        assertTrue(metadata.handleExists(new PIDHandle("109.3.1", "uuid:001fdf2b-a05a-40de-a43b-787f1ba9041f")));
    }

    @Test
    public void testWithoutHandle() throws TransformerException {
        DOMSMetadata metadata = new DOMSMetadata(DOM.streamToDOM(getClass().getResourceAsStream("/without.xml"), true));
        assertFalse(metadata.handleExists(new PIDHandle("109.3.1", "uuid:001fdf2b-a05a-40de-a43b-787f1ba9041f")));
    }

    @Test
    public void testAttachHandle() throws TransformerException {
        DOMSMetadata metadata = new DOMSMetadata(DOM.streamToDOM(getClass().getResourceAsStream("/without.xml"), true));
        PIDHandle handle = new PIDHandle("109.3.1", "uuid:001fdf2b-a05a-40de-a43b-787f1ba9041f");
        metadata.attachHandle(handle);
        assertTrue(metadata.handleExists(handle));
    }
}
