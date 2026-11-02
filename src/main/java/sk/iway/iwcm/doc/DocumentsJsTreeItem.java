package sk.iway.iwcm.doc;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.jstree.JsTreeItemState;
import sk.iway.iwcm.admin.jstree.JsTreeItemType;

public class DocumentsJsTreeItem extends JsTreeItem {
    @JsonProperty("docDetails")
    private DocDetails docDetails;

    /** Accessible language counterparts in the main webpage tree, excluding this page. */
    @Getter
    @Setter
    private List<MirrorLink> mirrors;

    /** Navigation metadata for one permitted mirrored page. */
    public record MirrorLink(int docId, String language, String path) { }

    public DocumentsJsTreeItem(DocDetails docDetails, int groupDefaultDocId) {
        this.docDetails = docDetails;

        setId("docId-" + docDetails.getDocId());
        setText(Tools.replace(docDetails.getTitle(), "&#47;", "/"));
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
            addTextIcon("ti ti-external-link");
        }

        if (!docDetails.isSearchable()) {
            addTextIcon("ti ti-eye-off");
        }

        if (Tools.isNotEmpty(docDetails.getPasswordProtected())) {
            addTextIcon("ti ti-lock");
        }

        String icon = "article";
        String suffix = "-filled";
        if (docDetails.isShowInMenu()==false) suffix = "";
        if (docDetails.getDocId()==groupDefaultDocId) {
            addLiClass("is-default-page");
            icon = "star";
        }

        return "ti ti-"+icon+suffix;
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
