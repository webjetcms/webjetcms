package sk.iway.iwcm.headless.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Top-level response object for the headless page endpoint.
 * Contains the page metadata, rendered body, and optional SEO metadata.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse {

    private int docId;
    private String title;
    private String virtualPath;
    private String language;
    private String body;
    private SeoMetadata seo;
    private List<NavigationItem> navigation;
    private Map<String, Object> customFields;

    public PageResponse() {
    }

    public PageResponse(int docId, String title, String virtualPath, String language, String body) {
        this.docId = docId;
        this.title = title;
        this.virtualPath = virtualPath;
        this.language = language;
        this.body = body;
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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public SeoMetadata getSeo() {
        return seo;
    }

    public void setSeo(SeoMetadata seo) {
        this.seo = seo;
    }

    public List<NavigationItem> getNavigation() {
        return navigation;
    }

    public void setNavigation(List<NavigationItem> navigation) {
        this.navigation = navigation;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
}
