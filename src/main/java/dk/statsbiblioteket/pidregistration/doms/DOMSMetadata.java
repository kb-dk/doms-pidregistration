package dk.statsbiblioteket.pidregistration.doms;

import dk.statsbiblioteket.pidregistration.PIDHandle;
import dk.statsbiblioteket.util.xml.DOM;
import dk.statsbiblioteket.util.xml.XPathSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;

/**
 * Responsible for querying and modifying the DOMS XML metadata. This metadata is where the handle is located after
 * it has been constructed for the DOMS object
 */
public class DOMSMetadata {
    private static final Logger log = LoggerFactory.getLogger(DOMSMetadata.class);

    private static final String OAI_NAMESPACE = "http://www.openarchives.org/OAI/2.0/oai_dc/";
    private static final String DC_NAMESPACE = "http://purl.org/dc/elements/1.1/";
    private static final String XPATH_HANDLE_PATTERN = "/oai_dc:dc/dc:identifier[text()='hdl:%s']";

    private static final XPathSelector NAMESPACED_SELECTOR = DOM.createXPathSelector(
            "oai_dc", OAI_NAMESPACE,
            "dc", DC_NAMESPACE
    );

    private Document namespaceAwareDom;

    public DOMSMetadata(String metadata) {
        this.namespaceAwareDom = DOM.stringToDOM(metadata, true);
    }

    public boolean handleExists(PIDHandle handle) {
        return findHandleNodes(handle).getLength() != 0;
    }

    private NodeList findHandleNodes(PIDHandle handle) {
        String xpath = String.format(XPATH_HANDLE_PATTERN, handle.asString());
        return NAMESPACED_SELECTOR.selectNodeList(namespaceAwareDom, xpath);
    }

    public void attachHandle(PIDHandle handle) {
        namespaceAwareDom.getFirstChild().appendChild(buildIdentifierNode(handle));
    }

    private Node buildIdentifierNode(PIDHandle handle) {
        Node result = namespaceAwareDom.createElementNS(DC_NAMESPACE, "dc:identifier");
        result.setTextContent("hdl:" + handle.asString());
        return result;
    }

    public void detachHandle(PIDHandle handle) {
        if (handleExists(handle)) {
            NodeList handles = findHandleNodes(handle);
            Node handleToRemove = handles.item(handles.getLength() - 1);
            String expectedContent = buildIdentifierNode(handle).getTextContent();
            String actualContent = handleToRemove.getTextContent();

            if (!expectedContent.equals(actualContent)) {
                log.error("when detaching handle {}, expected content ({}) did not match actual content ({}). Ignoring.");
            } else {
                namespaceAwareDom.getFirstChild().removeChild(handleToRemove);
            }
        }
    }

    public String getMetadata() {
        try {
            return DOM.domToString(namespaceAwareDom);
        } catch (TransformerException e) {
            throw new RuntimeException("unexpected error when generating metadata XML", e);
        }
    }
}
