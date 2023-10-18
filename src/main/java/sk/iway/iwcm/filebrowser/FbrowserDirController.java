package sk.iway.iwcm.filebrowser;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/* Nahrada za struts /admin/fbrowser.savedir.do */
@Controller
@RequestMapping("/admin/")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuFbrowser')")
public class FbrowserDirController {

    @GetMapping("/fbrowser/dirprop/")
    public String submit(@RequestParam String dir, Model model, HttpServletRequest request) {
        return FileBrowserService.editDir(dir, model, request);
    };

    @PostMapping("/fbrowser/dirprop/")
    public String submit(@ModelAttribute("editorForm") EditForm editForm, HttpServletRequest request, HttpServletResponse response) {
        String forward = FileBrowserService.saveDir(editForm, request, response);
        return forward;
    }
}