package sk.iway.iwcm.system.spring.events;

import sk.iway.iwcm.doc.DocDetails;

public class DocumentPublishEvent {

    private DocDetails document;
    private String oldVirtualPath;

    public DocumentPublishEvent(DocDetails document) {
        this.document = document;
    }

    public DocDetails getDocument() {
        return document;
    }

    public String getOldVirtualPath() {
        return oldVirtualPath;
    }

    public void setOldVirtualPath(String oldVirtualPath) {
        this.oldVirtualPath = oldVirtualPath;
    }
}