package sk.iway.iwcm.editor.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

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

        //set DocDetails @transient columns
        doc.setPublishStart(doc.getPublishStart());
        doc.setPublishEnd(doc.getPublishEnd());
        doc.setEventDate(doc.getEventDate());

        TemplateDetails temp = TemplatesDB.getInstance().getTemplate(doc.getTempId());
        if (temp != null) {
            doc.setTempName(temp.getTempName());
        }

        UserDetails user = UsersDB.getUser(doc.getAuthorId());
        if (user != null) {
            doc.setAuthorName(user.getFullName());
            doc.setAuthorEmail(user.getEmail());
            doc.setAuthorPhoto(user.getPhoto());
        }

        request.setAttribute("isPreview", Boolean.TRUE);
        request.setAttribute("xssTestDisabled", "true");
        request.setAttribute("ShowdocAction.showDocData", doc);
        request.setAttribute("path_filter_orig_path", doc.getVirtualPath());

        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (rb != null) {
            rb.setDocId(doc.getDocId());
            rb.setUrl(doc.getVirtualPath());
        }

        return "forward:/showdoc.do";
    }

}
