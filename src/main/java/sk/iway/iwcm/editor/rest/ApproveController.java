package sk.iway.iwcm.editor.rest;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import sk.iway.iwcm.editor.service.EditorService;

@Controller
@RequestMapping("/admin")
public class ApproveController {
    private final EditorService editorService;

    @Autowired
    public ApproveController(EditorService editorService) {
        this.editorService = editorService;
    }

    @RequestMapping("/approve.struts")
    public String approveAction(@RequestParam Map<String, String> params) {
        final String error = "/admin/approve_form";
		final String success = "/admin/approve_success";

        boolean ok = editorService.approveAction();
        if (ok) return success;
        return error;
    }


    @RequestMapping("/approvedel.struts")
    public String approveDelAction(@RequestParam Map<String, String> params) {
        final String error = "/admin/approve_form_delete";
		final String success = "/admin/approve_success_del";

        boolean ok = editorService.approveDelAction();
        if (ok) return success;
        return error;
    }
}
