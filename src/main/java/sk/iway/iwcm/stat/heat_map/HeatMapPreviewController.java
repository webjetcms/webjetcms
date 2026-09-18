package sk.iway.iwcm.stat.heat_map;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;

/** Renders the server-selected heat-map revision in a request-scoped preview. */
@Controller
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_stat')")
public class HeatMapPreviewController {

    private final HeatMapHistoryService historyService;
    private final HeatMapAccess access;

    public HeatMapPreviewController(HeatMapHistoryService historyService, HeatMapAccess access) {
        this.historyService = historyService;
        this.access = access;
    }

    /**
     * Prepares an authenticated preview without using or replacing editor session state.
     * The history ID is always selected on the server from the requested date range.
     */
    @GetMapping(HeatMapPreviewRequest.PREVIEW_PATH)
    public void preview(@RequestParam("docId") int docId,
            @RequestParam(value = "dateRange", required = false) String dateRange,
            HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        HeatMapHistoryService.PreviewSelection selection = historyService.resolve(request, docId, dateRange);
        DocDetails document = selection.document();
        if (document == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);

        TemplateDetails template = TemplatesDB.getInstance().getTemplate(document.getTempId());
        if (template != null) document.setTempName(template.getTempName());

        String domain = access.currentDomain(request);
        HeatMapPreviewRequest previewRequest = new HeatMapPreviewRequest(request, docId);
        request.setAttribute("heatMapPreview", Boolean.TRUE);
        request.setAttribute("heatMapPreviewDomain", domain);
        request.setAttribute("isPreview", Boolean.TRUE);
        request.setAttribute("NO_WJTOOLBAR", Boolean.TRUE);
        request.setAttribute("xssTestDisabled", "true");
        request.setAttribute("ShowdocAction.showDocData", document);
        request.setAttribute("path_filter_orig_path", document.getVirtualPath());
        request.setAttribute("path_filter_query_string", "");
        request.setAttribute("docid", Integer.toString(docId));
        request.setAttribute("doc_id", docId);
        request.setAttribute("group_id", Integer.toString(document.getGroupId()));
        String path = document.getVirtualPath();
        if (path == null || !path.startsWith("/") || path.startsWith("//")) path = "/showdoc.do?docid=" + docId;
        request.setAttribute("heatMapPreviewBasePath", request.getContextPath() + path);

        RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (requestBean != null) {
            requestBean.setDocId(docId);
            requestBean.setGroupId(document.getGroupId());
            requestBean.setUrl(document.getVirtualPath());
            requestBean.setDomain(domain);
            requestBean.setParameters(previewRequest.getParameterMap());
            requestBean.setQueryString(previewRequest.getQueryString());
            requestBean.setRequest(previewRequest);
        }
        request.getRequestDispatcher("/showdoc.do").forward(previewRequest, response);
    }
}
