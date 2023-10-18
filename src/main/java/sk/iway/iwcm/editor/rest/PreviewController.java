package sk.iway.iwcm.editor.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import sk.iway.iwcm.doc.DocDetails;

/**
 * Nastavi objekty potrebne pre nahlad stranky, DocDetails objekt pre nahlad ocakava v
 * session.getAttribute("ShowdocAction.showDocData");
 */
@Controller
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuWebpages')")
public class PreviewController {

    @GetMapping("/admin/webpages/preview/")
    public String preview(HttpServletRequest request, HttpServletResponse response) {

        DocDetails doc = (DocDetails)request.getSession().getAttribute("ShowdocAction.showDocData");
        if (doc == null) {
            return "forward:/404.jsp";
        }

        request.setAttribute("isPreview", Boolean.TRUE);
        request.setAttribute("xssTestDisabled", "true");
        request.setAttribute("ShowdocAction.showDocData", doc);

        return "forward:/showdoc.do";
    }

}
