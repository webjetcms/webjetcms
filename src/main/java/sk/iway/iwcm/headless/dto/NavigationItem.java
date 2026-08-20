package sk.iway.iwcm.headless.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Represents a single node in the navigation tree.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NavigationItem {

    private int docId;
    private String title;
    private String virtualPath;
    private String language;
    private int level;
    private boolean hasChildren;
    private List<NavigationItem> children;

    public NavigationItem() {
    }

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVirtualPath() {
        return virtualPath;
    }

    public void setVirtualPath(String virtualPath) {
        this.virtualPath = virtualPath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    public List<NavigationItem> getChildren() {
        return children;
    }

    public void setChildren(List<NavigationItem> children) {
        this.children = children;
    }
}
