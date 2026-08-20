package sk.iway.iwcm.headless.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single search result item referencing a document.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResultItem {

    private int docId;
    private String title;
    private String virtualPath;
    private String language;
    private String perex;
    private String snippet;

    public SearchResultItem() {
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

    public String getPerex() {
        return perex;
    }

    public void setPerex(String perex) {
        this.perex = perex;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
}
