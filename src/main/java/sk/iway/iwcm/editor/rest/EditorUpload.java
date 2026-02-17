package sk.iway.iwcm.editor.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import sk.iway.iwcm.editor.UploadFileAction;

/**
 * Upload image into CKEditor by drag/drop
 */
@Controller
public class EditorUpload {

    @RequestMapping(path = "/admin/web-pages/upload/", method = RequestMethod.POST)
    public String submit(@RequestParam("uploadFile") MultipartFile uploadFile, HttpServletRequest request, HttpServletResponse response) {
        //Check that file is present
        if(uploadFile == null) return null;

        try {
            UploadFileAction.execute(request, response, uploadFile);
        } catch(Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        }

        return null;
    }
}