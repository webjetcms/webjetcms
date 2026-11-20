package sk.iway.iwcm.branch;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.system.WJResponseWrapper;
import sk.iway.iwcm.tags.BuffTag;

/** Renders actual LIVE/WORK pages through ShowDoc without applying a saved merge choice. */
class BranchPageContent {
    String render(long branchId, String domain, int userId, int docId, BranchDecision.Source version,
            HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        BranchContext context = version == BranchDecision.Source.LIVE ? BranchContext.live(domain, userId)
            : new BranchContext(branchId, domain, userId, BranchContext.Mode.ADMIN);
        Object forced = request.getAttribute("ShowdocAction.showDocData");
        Object buffered = request.getAttribute(BuffTag.IS_BUFF_TAG);
        Object preview = request.getAttribute("isPreview");
        try (var ignored = BranchContexts.open(request, context)) {
            DocDetails doc = version == BranchDecision.Source.LIVE ? DocDB.getInstance().getDoc(docId, -1, false)
                : BranchView.current().document(docId, false);
            if (doc == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            request.setAttribute("ShowdocAction.showDocData", doc);
            request.setAttribute("isPreview", Boolean.TRUE);
            request.setAttribute(BuffTag.IS_BUFF_TAG, "true");
            var capture = new WJResponseWrapper(response, request);
            request.getRequestDispatcher("/showdoc.do?docid=" + docId + "&historyid=-1&NO_WJTOOLBAR=true").include(request, capture);
            if (capture.getRedirectURL() != null) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
            String html = capture.getStrWriterAsString();
            var document = Jsoup.parse(html);
            document.outputSettings().prettyPrint(false);
            document.select("#branch-preview-status, #branch-preview-styles, meta[http-equiv=refresh], base, noscript").remove();
            // Static previews must not keep content hidden until an animation script runs.
            document.select("[data-aos]").removeAttr("data-aos");
            document.select("form button, form input[type=submit], form input[type=image]").attr("disabled", "");
            // srcdoc resolves relative resources against this page rather than the administration URL.
            String pagePath = doc.getVirtualPath();
            if (pagePath == null || !pagePath.startsWith("/") || pagePath.startsWith("//")) pagePath = "/";
            document.head().prependElement("base").attr("href", pagePath);
            return document.outerHtml();
        } finally {
            restore(request, "ShowdocAction.showDocData", forced);
            restore(request, BuffTag.IS_BUFF_TAG, buffered);
            restore(request, "isPreview", preview);
        }
    }

    /** Rewrites WORK resources only after diffing, so preview URLs never create false differences. */
    String prepareWork(String html, long branchId, String domain, int userId, HttpServletRequest request) {
        var document = Jsoup.parse(html);
        var base = document.selectFirst("base[href]");
        String pagePath = base == null ? "/" : base.attr("href");
        Object path = request.getAttribute(BranchPreviewUrls.LOGICAL_PATH);
        request.setAttribute(BranchPreviewUrls.LOGICAL_PATH, pagePath);
        try (var ignored = BranchContexts.open(request, new BranchContext(branchId, domain, userId, BranchContext.Mode.PREVIEW))) {
            document = Jsoup.parse(BranchPreviewUrls.rewriteHtml(html, request));
            document.outputSettings().prettyPrint(false);
            document.select("#branch-preview-status, #branch-preview-styles, base").remove();
            document.head().prependElement("base").attr("href", pagePath);
            return document.outerHtml();
        } finally { restore(request, BranchPreviewUrls.LOGICAL_PATH, path); }
    }

    private static void restore(HttpServletRequest request, String key, Object value) {
        if (value == null) request.removeAttribute(key);
        else request.setAttribute(key, value);
    }
}
