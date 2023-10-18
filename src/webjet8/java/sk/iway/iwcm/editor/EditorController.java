package sk.iway.iwcm.editor;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import sk.iway.iwcm.Tools;

@Controller
@RequestMapping(path = "/admin")
public class EditorController {

    @Autowired
    private HttpServletRequest request;

    /* jeeff: toto nefunguje, spracuje sa mapovanie na *.do vo web.xml a sem sa to ani nedostane, riesi sa v StrutsRedirectServlet
    @GetMapping("/editor.do")
    public String getDocDo()
    {
        int docId = Tools.getIntValue(request.getParameter("docid"), -1);
        int historyId = Tools.getIntValue(request.getParameter("historyid"), -1);

        return getDoc(docId, historyId, null, request);
    }
     */

    @GetMapping("/editor")
    @PreAuthorize("@WebjetSecurityService.isAdmin()")
    public String getDoc(Model model) {

        //spracovava sa to tu, pretoze parametre su nepovinne
        int docId = Tools.getIntValue(request.getParameter("docid"), -1);
        int historyId = Tools.getIntValue(request.getParameter("historyid"), -1);

        return getDoc(docId, historyId, model, request);
    }

    private static String getDoc(int docid, int historyId, Model model, HttpServletRequest request)
    {
        if (request.getParameter("ajaxLoad") == null && request.getParameter("isPopup") == null && request.getParameter("inline") == null && request.getParameter("quitLink") == null)
        {
            //STRUTS verzia uz nefunguje, vzdy teda ideme takto
            return "redirect:/admin/webpages/?"+ Tools.sanitizeHttpHeaderParam(request.getQueryString());
        }

        /*EditorForm editorForm = EditorDB.getEditorForm(request, docid, historyId, -1);

        model.addAttribute("editorForm", editorForm);
        request.setAttribute("editorForm", editorForm);
        request.getSession().setAttribute("editorForm", editorForm);

        return "/admin/skins/webjet8/editor";*/

        return "redirect:/admin/webpages/?"+ Tools.sanitizeHttpHeaderParam(request.getQueryString())+"&iniframe=true";
    }
}
