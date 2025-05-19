package sk.iway.iwcm.components.forms;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_form')")
public class AttachmentController {

    private final FormsServiceImpl formsService;

    @Autowired
    public AttachmentController(FormsServiceImpl formsService) {
        this.formsService = formsService;
    }

    /**
     * Download attachment for form
     * @param name - filename in /WEB-INF/formfiles/ folder
     * @param request
     * @param response
     * @return
     */
    @GetMapping("/apps/forms/admin/attachment/")
	public String execute(@RequestParam String name, HttpServletRequest request, HttpServletResponse response) {
        try {
            return formsService.downloadAttachment(name, request, response);
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }
        return null;
   }
}