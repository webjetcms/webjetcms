package sk.iway.iwcm.editor.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import sk.iway.iwcm.editor.UploadFileAction;

/**
 * Upload image into CKEditor by drag/drop
 */
@Controller
public class EditorUpload {

    @PostMapping(path = "/admin/web-pages/upload/")
    @PreAuthorize("@WebjetSecurityService.isAdmin()")
    public String submit(@RequestParam("uploadFile") MultipartFile uploadFile, HttpServletRequest request, HttpServletResponse response) {
        //Check that file is present
        if(uploadFile == null) return null;

        try {
            String result=UploadFileAction.execute(request, response, uploadFile);
            if (result!=null && sk.iway.iwcm.branch.BranchContexts.current().isBranch())
                throw new IllegalArgumentException("File type, size or upload permission is not allowed");
        } catch(Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
            if (sk.iway.iwcm.branch.BranchContexts.current().isBranch()) {
                response.setStatus(409);
                try {
                    response.setContentType("application/json");
                    response.getWriter().write(new org.json.JSONObject().put("uploaded",0).put("error",new org.json.JSONObject().put("message",ex.getMessage())).toString());
                } catch (java.io.IOException io) {throw new IllegalStateException(io);}
            }
        }

        return null;
    }
}
