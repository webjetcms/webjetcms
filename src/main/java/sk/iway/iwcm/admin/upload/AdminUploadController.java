package sk.iway.iwcm.admin.upload;

import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;

/**
 * REST controller handling file upload conflict resolution (skip, overwrite, keep-both).
 * Supports both standard file uploads and file archive uploads.
 */
@RestController
@RequestMapping("/admin/upload/")
public class AdminUploadController {

    /**
     * Skips (deletes) a temporarily uploaded file identified by its key.
     * Called when the user chooses not to overwrite an existing file.
     * @param fileKey - unique key of the temporary uploaded file
     * @param request - HTTP request
     * @return JSON with "success" flag
     */
    @PostMapping(path="/skipkey", produces = MediaType.APPLICATION_JSON_VALUE)
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

    /**
     * Overwrites an existing file with the uploaded temporary file.
     * Delegates to file archive processing when uploadType is "fileArchive".
     * @param fileKey - unique key of the temporary uploaded file
     * @param destinationFolder - target folder path
     * @param fileName - name of the file to overwrite
     * @param uploadType - type of upload (e.g. "fileArchive" for archive files)
     * @param request - HTTP request
     * @return JSON with operation result
     */
    @PostMapping(path="/overwrite", produces = MediaType.APPLICATION_JSON_VALUE)

    public String overwrite(@RequestParam String fileKey, @RequestParam String destinationFolder, @RequestParam String fileName, @RequestParam String uploadType, HttpServletRequest request)
    {
        if ("fileArchive".equals(uploadType)) {
            return processArchiveFile(fileKey, destinationFolder, fileName, false, request);
        }
        return processOverwrite(fileKey, destinationFolder, fileName, false, request);
    }

    /**
     * Keeps both the existing and uploaded file (creates a new version for archive, or renames for standard upload).
     * Delegates to file archive processing when uploadType is "fileArchive".
     * @param fileKey - unique key of the temporary uploaded file
     * @param destinationFolder - target folder path
     * @param fileName - original file name
     * @param uploadType - type of upload (e.g. "fileArchive" for archive files)
     * @param request - HTTP request
     * @return JSON with operation result
     */
    @PostMapping(path="/keepboth", produces = MediaType.APPLICATION_JSON_VALUE)

    public String keepboth(@RequestParam String fileKey, @RequestParam String destinationFolder, @RequestParam String fileName, @RequestParam String uploadType, HttpServletRequest request)
    {
        if ("fileArchive".equals(uploadType)) {
            return processArchiveFile(fileKey, destinationFolder, fileName, true, request);
        }
        return processOverwrite(fileKey, destinationFolder, fileName, true, request);
    }

    /**
     * Processes an upload conflict for a file archive file.
     * Validates archive permissions, then either overwrites or creates a new version.
     * @param fileKey - unique key of the temporary uploaded file
     * @param destinationFolder - raw destination folder from the request
     * @param fileName - name of the archive file
     * @param keepBoth - if true, uploads a new version; if false, overwrites the existing file
     * @param request - HTTP request (used for localization and user identity)
     * @return JSON string with operation result
     */
    private static String processArchiveFile(String fileKey, String destinationFolder, String fileName, boolean keepBoth, HttpServletRequest request) {
        JSONObject output = new JSONObject();
        Prop prop = Prop.getInstance(request);
        Identity user = UsersDB.getCurrentUser(request);
        String referer = request.getHeader("referer");

        String errorKey = FileArchiveUploadService.validateArchiveUploadPermission(user, destinationFolder, referer);
        if (errorKey != null) {
            output.put("success", false);
            output.put("error", prop.getText(errorKey));
            return output.toString();
        }

        String archiveFolder = FileArchiveUploadService.normalizeArchiveFolder(destinationFolder);
        if (keepBoth) {
            FileArchiveUploadService.uploadNewArchiveFileVersion(user, prop, archiveFolder, fileName, fileKey, output);
        } else {
            FileArchiveUploadService.overwriteArchiveFile(user, prop, archiveFolder, fileName, fileKey, output);
        }

        return output.toString();
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
