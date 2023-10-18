package sk.iway.iwcm.admin.upload;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import sk.iway.iwcm.Logger;

@Controller
@RequestMapping("/admin/upload/")
public class AdminUploadController {

    @PostMapping(path="/skipkey", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String skipkey(@RequestParam String fileKey, HttpServletRequest request)
    {
        JSONObject output = new JSONObject();

        boolean deleted = AdminUploadServlet.deleteTempFile(fileKey);

        try {
            output.put("success", deleted);
        } catch (Exception e) {
            Logger.error(AdminUploadController.class, e);
        }

        return output.toString();
    }

    @PostMapping(path="/overwrite", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String overwrite(@RequestParam String fileKey, @RequestParam String destinationFolder, @RequestParam String fileName, @RequestParam String uploadType, HttpServletRequest request)
    {
        return processOverwrite(fileKey, destinationFolder, fileName, false, request);
    }

    @PostMapping(path="/keepboth", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String keepboth(@RequestParam String fileKey, @RequestParam String destinationFolder, @RequestParam String fileName, @RequestParam String uploadType, HttpServletRequest request)
    {
        return processOverwrite(fileKey, destinationFolder, fileName, true, request);
    }

    /**
     * Vykona proces prepisania suboru alebo ponechania oboch
     * @param fileKey
     * @param destinationFolder
     * @param fileName
     * @param keepBoth - ak je nastavene na true ponecha oba subory, novemu da suffix -xxx
     * @param request
     * @return
     */
    private static String processOverwrite(String fileKey, String destinationFolder, String fileName, boolean keepBoth, HttpServletRequest request) {
        JSONObject output = new JSONObject();

        boolean success = false;
        String errorKey = null;

        String destinationFileName = fileName;

        if (keepBoth) {
            destinationFileName = UploadService.getKeppBothFileName(destinationFolder, fileName);
        }

        if (destinationFileName!=null) {

            try {
                UploadService uploadService = new UploadService(fileKey, destinationFolder, destinationFileName, request);

                output.put("virtualPath", uploadService.getVirtualPath());

                uploadService.process();

                success = true;
            }
            catch (Exception ex) {
                Logger.error(AdminUploadController.class, ex);
                errorKey = "multiple_files_upload.upload_error";
            }
        }
        else {
            errorKey = "multiple_files_upload.upload_error";
        }

        try {
            output.put("success", success);
            if (errorKey != null) output.put("error", errorKey);
        } catch (Exception e) {
            Logger.error(AdminUploadController.class, e);
        }

        return output.toString();
    }
}