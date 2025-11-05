package sk.iway.iwcm.doc.showdoc;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.tags.CombineTag;

import jakarta.servlet.http.HttpServletRequest;

/**
 * ThymeleafShowdocController.java
 *
 * Zobrazuje web stranku pomocou Thymeleaf sablony
 *
 * Title        webjet8
 * Company      Interway a.s. (www.interway.sk)
 * Copyright    Interway a.s. (c) 2001-2019
 * @author      $Author: lpasek $
 * @version     $Revision: 1.0 $
 * created      14.2.2022 09:29
 */
@Controller
@RequestMapping("/thymeleaf")
public class ThymeleafShowdocController {
    /**
     * Metoda zobrazuje view pre thymeleaf šablony a injectuje objekty ninja, docDetails, docDetailsOriginal, groupDetails a tempDetails
     * @param request HttpServletRequest
     * @param model Model
     * @return String of view
     */
    @RequestMapping(path = "/showdoc", method = {RequestMethod.POST, RequestMethod.GET})
    public String showdoc(HttpServletRequest request, Model model) {

        String template = (String) request.getAttribute("thymeleafTemplateFile");
        if (template == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found");
        }
        model.addAttribute("request", request);
        model.addAttribute("session", request.getSession());
        model.addAttribute("ninja", request.getAttribute("ninja"));
        model.addAttribute("docDetails", request.getAttribute("docDetails"));
        model.addAttribute("docDetailsOriginal", request.getAttribute("docDetailsOriginal"));
        model.addAttribute("groupDetails", request.getAttribute("pageGroupDetails"));
        model.addAttribute("tempDetails", request.getAttribute("templateDetails"));
        model.addAttribute("templatesGroupDetails", request.getAttribute("templatesGroupDetails"));
        model.addAttribute("version", CombineTag.getVersion());

        return template;
    }
}
