package sk.iway.iwcm.headless.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.headless.dto.PageResponse;
import sk.iway.iwcm.headless.dto.SeoMetadata;
import sk.iway.iwcm.system.spring.WebjetComponentParserInterface;
import sk.iway.iwcm.system.spring.services.WebjetSecurityService;
import sk.iway.iwcm.Identity;

import java.sql.Connection;

/**
 * Service responsible for resolving pages by path and rendering them
 * for the headless API. Reuses existing ShowDoc/ComponentParser internals.
 */
@Service("HeadlessPageService")
public class HeadlessPageService {

    @Autowired
    private WebjetSecurityService webjetSecurityService;

    /**
     * Resolves a page by its virtual path and returns a PageResponse.
     *
     * @param virtualPath the virtual path/slug to resolve
     * @param lng         optional language override
     * @param preview     whether to show unpublished content (requires admin session)
     * @param request     the current HTTP request
     * @param response    the current HTTP response
     * @return PageResponse with resolved page data, or null if not found
     */
    public PageResponse resolvePage(String virtualPath, String lng, boolean preview,
                                    HttpServletRequest request, HttpServletResponse response) {

        if (Tools.isEmpty(virtualPath)) {
            return null;
        }

        DocDB docDB = DocDB.getInstance();
        String domain = DocDB.getDomain(request);

        // Resolve docId from virtual path
        int docId = docDB.getVirtualPathDocId(virtualPath, domain);
        if (docId < 1) {
            return null;
        }

        // Load full document details
        DocDetails doc = docDB.getDoc(docId, -1, !preview);
        if (doc == null) {
            return null;
        }

        // Check access permissions
        Identity currentUser = UsersDB.getCurrentUser(request.getSession());
        if (!DocDB.canAccess(doc, currentUser)) {
            return null;
        }

        // Build PageResponse
        PageResponse pageResponse = new PageResponse(
                doc.getDocId(),
                doc.getTitle(),
                doc.getVirtualPath(),
                getLanguage(doc, lng),
                extractBody(doc, request)
        );

        // Add SEO metadata
        SeoMetadata seo = buildSeoMetadata(doc);
        pageResponse.setSeo(seo);

        return pageResponse;
    }

    /**
     * Renders the page body by invoking the existing WebjetComponentParser pipeline.
     * This reuses the server-side rendering of dynamic WebJET apps.
     *
     * @param docId the document ID to render
     * @param request the HTTP request
     * @param response the HTTP response wrapper for capturing rendered output
     * @return the rendered HTML body string
     */
    public String renderPageBody(int docId, HttpServletRequest request, HttpServletResponse response) {
        try {
            RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
            if (requestBean != null) {
                WebjetComponentParserInterface parser = requestBean.getSpringBean(
                        "webjetComponentParser", WebjetComponentParserInterface.class);
                if (parser != null) {
                    // Create a wrapper to capture the rendered output
                    ContentCapturingResponseWrapper wrapper = new ContentCapturingResponseWrapper(response);
                    // Set up the request with the doc_id so the parser can find it
                    request.setAttribute("doc_id", docId);
                    String docData = getDocData(docId, request);
                    request.setAttribute("doc_data", docData);
                    parser.run(request, wrapper);
                    return wrapper.getCapturedContent();
                }
            }
        } catch (Exception e) {
            Logger.error(HeadlessPageService.class, "HeadlessPageService.renderPageBody", e);
        }
        return getDocData(docId, request);
    }

    /**
     * Extracts the body content from a document.
     */
    public String extractBody(DocDetails doc, HttpServletRequest request) {
        String data = doc.getData();
        if (Tools.isEmpty(data)) {
            return "";
        }
        //execute INCLUDE apps
        data = EditorToolsForCore.renderIncludes(doc, false, request);
        return data;
    }

    /**
     * Gets the document data (htmlData) from the database.
     */
    private String getDocData(int docId, HttpServletRequest request) {
        try (Connection conn = DBPool.getConnection(request)) {
            java.sql.PreparedStatement ps = conn.prepareStatement(
                    "SELECT doc_data FROM documents WHERE doc_id=? AND domain_id=?");
            ps.setInt(1, docId);
            ps.setInt(2, sk.iway.iwcm.common.CloudToolsForCore.getDomainId());
            java.sql.ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("doc_data");
            }
        } catch (Exception e) {
            Logger.error(HeadlessPageService.class, "HeadlessPageService.getDocData", e);
        }
        return "";
    }

    /**
     * Builds SEO metadata from document fields.
     */
    public SeoMetadata buildSeoMetadata(DocDetails doc) {
        SeoMetadata seo = new SeoMetadata();
        String data = doc.getData();

        // Extract meta title from htmlHead
        String metaTitle = extractMetaTag(data, "title");
        if (Tools.isEmpty(metaTitle)) {
            metaTitle = doc.getTitle();
        }
        seo.setMetaTitle(metaTitle);

        // Extract meta description
        String metaDesc = extractMetaTag(data, "description");
        seo.setMetaDescription(metaDesc);

        // Extract meta keywords
        String metaKeywords = extractMetaTag(data, "keywords");
        seo.setMetaKeywords(metaKeywords);

        // Canonical URL
        String canonical = extractMetaTag(data, "canonical");
        seo.setCanonicalUrl(canonical);

        // Robots
        String robots = extractMetaTag(data, "robots");
        seo.setRobots(robots);

        return seo;
    }

    /**
     * Extracts a meta tag value from the document's htmlHead or data.
     */
    private String extractMetaTag(String html, String name) {
        if (Tools.isEmpty(html)) {
            return null;
        }
        String lowerHtml = html.toLowerCase();
        String metaName = "name=\"" + name.toLowerCase() + "\"";
        int idx = lowerHtml.indexOf(metaName);
        if (idx < 0) {
            metaName = "name='" + name.toLowerCase() + "'";
            idx = lowerHtml.indexOf(metaName);
        }
        if (idx < 0) {
            return null;
        }
        int valueStart = lowerHtml.indexOf("content=\"", idx);
        if (valueStart < 0) {
            valueStart = lowerHtml.indexOf("content='", idx);
        }
        if (valueStart < 0) {
            return null;
        }
        valueStart += "content=\"".length();
        int valueEnd = html.indexOf("\"", valueStart);
        if (valueEnd < 0) {
            valueEnd = html.indexOf("'", valueStart);
        }
        if (valueEnd > valueStart) {
            return html.substring(valueStart, valueEnd);
        }
        return null;
    }

    /**
     * Gets the language for a document, with optional override.
     */
    private String getLanguage(DocDetails doc, String lng) {
        if (Tools.isNotEmpty(lng)) {
            return lng;
        }
        GroupDetails group = GroupsDB.getInstance().getGroup(doc.getGroupId());
        if (group != null && Tools.isNotEmpty(group.getLng())) {
            return group.getLng();
        }
        TemplateDetails template = TemplatesDB.getInstance().getTemplate(doc.getTempId());
        if (template != null && Tools.isNotEmpty(template.getLng())) {
            return template.getLng();
        }
        return Constants.getString("defaultLanguage");
    }

    /**
     * Checks if the user has admin session for preview access.
     */
    public boolean hasAdminSession(HttpServletRequest request) {
        return webjetSecurityService.isAdmin();
    }

    /**
     * Wrapper class to capture rendered HTML content from the component parser.
     */
    public static class ContentCapturingResponseWrapper extends jakarta.servlet.http.HttpServletResponseWrapper {
        private StringBuilder content = new StringBuilder();

        public ContentCapturingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public jakarta.servlet.ServletOutputStream getOutputStream() {
            return new jakarta.servlet.ServletOutputStream() {
                @Override
                public void write(int b) {
                    content.append((char) b);
                }

                @Override
                public void write(byte[] b, int off, int len) {
                    content.append(new String(b, off, len));
                }

                public boolean isCommitted() {
                    return false;
                }

                // Abstract method in ServletOutputStream - must implement
                public void setWriteListener(jakarta.servlet.WriteListener listener) {
                    // not supported in sync mode
                }

                // Abstract method in ServletOutputStream - must implement
                public boolean isReady() {
                    return true;
                }

                public void flushBuffer() {
                    // no-op for capture
                }

                public java.io.PrintWriter getWriter() {
                    try {
                        return new java.io.PrintWriter(new java.io.BufferedWriter(
                                new java.io.OutputStreamWriter(getOutputStream(), getCharacterEncoding())), false);
                    } catch (java.io.UnsupportedEncodingException e) {
                        return new java.io.PrintWriter(new java.io.BufferedWriter(
                                new java.io.OutputStreamWriter(getOutputStream())), false);
                    }
                }
            };
        }

        public String getCapturedContent() {
            return content.toString();
        }
    }
}
