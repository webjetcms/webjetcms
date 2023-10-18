package sk.iway.iwcm.doc;

import com.fasterxml.jackson.annotation.JsonProperty;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.jstree.JsTreeItemState;
import sk.iway.iwcm.admin.jstree.JsTreeItemType;

public class DocumentsJsTreeItem extends JsTreeItem {
    @JsonProperty("docDetails")
    private DocDetails docDetails;

    public DocumentsJsTreeItem(DocDetails docDetails, int groupDefaultDocId) {
        this.docDetails = docDetails;

        setId("docId-" + docDetails.getDocId());
        setText(docDetails.getTitle());
        setVirtualPath(docDetails.getVirtualPath());

        setIcon(getIconPrivate(groupDefaultDocId));
        setState(getStatePrivate());

        setChildren(false);
        setType(JsTreeItemType.PAGE);
    }

    private String getIconPrivate(int groupDefaultDocId) {
        if (!docDetails.isAvailable()) {
            addLiClass("is-not-public");
        }

        if (Tools.isNotEmpty(docDetails.getExternalLink())) {
            addTextIcon("fas fa-external-link-alt");
        }

        if (!docDetails.isSearchable()) {
            addTextIcon("fas fa-eye-slash");
        }

        if (Tools.isNotEmpty(docDetails.getPasswordProtected())) {
            addTextIcon("fas fa-lock");
        }

        String faPrefix = "fas";
        if (docDetails.isShowInMenu()==false) faPrefix = "fal";
        if (docDetails.getDocId()==groupDefaultDocId) addLiClass("is-default-page");

        return faPrefix+" fa-globe";
    }

    private JsTreeItemState getStatePrivate() {
        return new JsTreeItemState();
    }

    public DocDetails getDocDetails() {
        return docDetails;
    }

    public void setDocDetails(DocDetails docDetails) {
        this.docDetails = docDetails;
    }
}
