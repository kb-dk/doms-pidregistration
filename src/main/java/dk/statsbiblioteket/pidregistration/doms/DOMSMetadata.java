package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.PIDHandle;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.TransformerException;

/**
 * Responsible for querying and modifying the DOMS XML metadata. This metadata is where the handle is located after
 * it has been constructed for the DOMS object
 */
public class DOMSMetadata {
    private static final String OAI_NAMESPACE = "http://www.openarchives.org/OAI/2.0/oai_dc/";
    private static final String DC_NAMESPACE = "http://purl.org/dc/elements/1.1/";

    private static final XPathSelector NAMESPACED_SELECTOR = DOM.createXPathSelector(
            "oai_dc", OAI_NAMESPACE,
            "dc", DC_NAMESPACE
    );

    private Document namespaceAwareDom;

    public DOMSMetadata(String metadata) {
        this.namespaceAwareDom = DOM.stringToDOM(metadata, true);
    }

    public boolean handleExists(PIDHandle handle) {
        String xpath = String.format("/oai_dc:dc/dc:identifier[text()='hdl:%s']", handle.asString());
        return NAMESPACED_SELECTOR.selectNodeList(namespaceAwareDom, xpath).getLength() != 0;
    }

    public void attachHandle(PIDHandle handle) {
        namespaceAwareDom.getFirstChild().appendChild(buildIdentifierNode(handle));
    }

    private Node buildIdentifierNode(PIDHandle handle) {
        Node result = namespaceAwareDom.createElementNS(DC_NAMESPACE, "dc:identifier");
        result.setTextContent("hdl:" + handle.asString());
        return result;
    }

    public String getMetadata() {
        try {
            return DOM.domToString(namespaceAwareDom);
        } catch (TransformerException e) {
            throw new RuntimeException("unexpected error when generating metadata XML", e);
        }
    }
}
