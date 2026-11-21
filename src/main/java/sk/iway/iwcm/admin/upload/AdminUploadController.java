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
import sk.iway.iwcm.users.UsersDB;

import sk.iway.iwcm.i18n.Prop;

/**
 * Resolves conflicts reported by the administrative file upload flow.
 * Supports discarding temporary uploads, replacing existing files, and retaining both
 * standard files or file archive versions.
 */
@RestController
@RequestMapping("/admin/upload/")
public class AdminUploadController {

    /**
     * Discards a temporary upload when the user declines to replace an existing file.
     *
     * @param fileKey unique key of the temporary upload
     * @param request current HTTP request
     * @return JSON containing the operation success flag
     */
    @PostMapping(path="/skipkey", produces = MediaType.APPLICATION_JSON_VALUE)
    public String skipkey(@RequestParam String fileKey, HttpServletRequest request)
    {
        AdminUploadServlet.requireUploadContext(fileKey, request);
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
     * Delegates to archive-aware processing when {@code uploadType} is {@code fileArchive}.
     *
     * @param fileKey unique key of the temporary upload
     * @param destinationFolder target folder path
     * @param fileName name of the file to overwrite
     * @param uploadType upload category, such as {@code fileArchive}
     * @param request current HTTP request
     * @return JSON containing the operation result
     */
    @PostMapping(path="/overwrite", produces = MediaType.APPLICATION_JSON_VALUE)

    public String overwrite(@RequestParam String fileKey, @RequestParam String destinationFolder, @RequestParam String fileName, @RequestParam String uploadType, HttpServletRequest request)
    {
        AdminUploadServlet.requireUploadContext(fileKey, request);
        if (sk.iway.iwcm.branch.BranchContexts.current().isBranch()) return processBranchFile(fileKey,destinationFolder,fileName,uploadType,false,request);
        if ("fileArchive".equals(uploadType)) {
            return processArchiveFile(fileKey, destinationFolder, fileName, false, request);
        }
        return processOverwrite(fileKey, destinationFolder, fileName, uploadType, false, request);
    }

    /**
     * Retains the existing and uploaded files by creating an archive version or renaming a standard upload.
     *
     * @param fileKey unique key of the temporary upload
     * @param destinationFolder target folder path
     * @param fileName original file name
     * @param uploadType upload category, such as {@code fileArchive}
     * @param request current HTTP request
     * @return JSON containing the operation result
     */
    @PostMapping(path="/keepboth", produces = MediaType.APPLICATION_JSON_VALUE)

    public String keepboth(@RequestParam String fileKey, @RequestParam String destinationFolder, @RequestParam String fileName, @RequestParam String uploadType, HttpServletRequest request)
    {
        AdminUploadServlet.requireUploadContext(fileKey, request);
        if (sk.iway.iwcm.branch.BranchContexts.current().isBranch()) return processBranchFile(fileKey,destinationFolder,fileName,uploadType,true,request);
        if ("fileArchive".equals(uploadType)) {
            return processArchiveFile(fileKey, destinationFolder, fileName, true, request);
        }
        return processOverwrite(fileKey, destinationFolder, fileName, uploadType, true, request);
    }

    private static String processBranchFile(String key,String folder,String name,String type,boolean keepBoth,HttpServletRequest request) {
        JSONObject output=new JSONObject();
        try {
            if ("fileArchive".equals(type)) throw new IllegalArgumentException("File archive is unavailable in a publishing branch");
            folder=AdminUploadValidator.normalizeDestinationFolder(folder);
            name=AdminUploadValidator.normalizeFileName(name);
            String temporaryPath=AdminUploadServlet.getTempFilePath(key);
            if (temporaryPath==null) throw new IllegalArgumentException("Upload expired; upload the file again");
            java.nio.file.Path source=java.nio.file.Path.of(temporaryPath);
            String error=AdminUploadValidator.validateConflict(folder,name,type,java.nio.file.Files.size(source),UsersDB.getCurrentUser(request),request);
            if (error!=null) throw new IllegalArgumentException(Prop.getInstance(request).getText(error));
            if (keepBoth) name=AdminUploadServlet.branchKeepBothName(folder,name);
            sk.iway.iwcm.branch.BranchFiles.put(folder+name,source,request,type);
            AdminUploadServlet.deleteTempFile(key);
            output.put("success",true).put("virtualPath",folder+name);
        } catch (Exception ex) {
            output.put("success",false).put("error",ex.getMessage());
        }
        return output.toString();
    }

    /**
     * Validates an archive conflict request and then replaces the file or creates a new version.
     * Bulk metadata is applied to the resulting archive record after permission and input validation.
     *
     * @param fileKey unique key of the temporary upload
     * @param destinationFolder raw destination folder from the request
     * @param fileName name of the archive file
     * @param keepBoth {@code true} to create a new version; {@code false} to replace the current file
     * @param request current HTTP request used for localization and user identity
     * @return JSON containing the operation result
     */
    private static String processArchiveFile(String fileKey, String destinationFolder, String fileName, boolean keepBoth, HttpServletRequest request) {
        JSONObject output = new JSONObject();
        Prop prop = Prop.getInstance(request);
        Identity user = UsersDB.getCurrentUser(request);
        String referer = request.getHeader("referer");

        String errorKey = FileArchiveUploadService.validateArchiveUploadPermission(user, destinationFolder, referer);
        if (errorKey != null) {
            AdminUploadServlet.deleteTempFile(fileKey);
            output.put("success", false);
            output.put("error", prop.getText(errorKey));
            return output.toString();
        }

        String archiveFolder = FileArchiveUploadService.normalizeArchiveFolder(destinationFolder);
        FileArchiveBulkUploadOptions bulkUploadOptions = FileArchiveBulkUploadOptions.fromRequest(request);
        if (bulkUploadOptions.getErrorKey() != null) {
            AdminUploadServlet.deleteTempFile(fileKey);
            output.put("success", false);
            output.put("error", prop.getText(bulkUploadOptions.getErrorKey()));
            return output.toString();
        }
        if (keepBoth == false && bulkUploadOptions.isSaveLater()) {
            AdminUploadServlet.deleteTempFile(fileKey);
            output.put("success", false);
            output.put("error", prop.getText(FileArchiveBulkUploadOptions.ERROR_SAVE_LATER_REPLACE));
            return output.toString();
        }

        if (keepBoth) {
            FileArchiveUploadService.uploadNewArchiveFileVersion(user, prop, archiveFolder, fileName, fileKey,
                bulkUploadOptions, output);
        } else {
            FileArchiveUploadService.overwriteArchiveFile(user, prop, archiveFolder, fileName, fileKey,
                bulkUploadOptions, output);
        }

        return output.toString();
    }

    /**
     * Resolves a standard upload conflict by replacing the destination or assigning a unique file name.
     *
     * @param fileKey unique key of the temporary upload
     * @param destinationFolder target folder path
     * @param fileName requested destination file name
     * @param uploadType upload category used during validation
     * @param keepBoth {@code true} to retain both files by renaming the upload
     * @param request current HTTP request
     * @return JSON containing the operation result
     */
    private static String processOverwrite(String fileKey, String destinationFolder, String fileName, String uploadType, boolean keepBoth, HttpServletRequest request) {
        JSONObject output = new JSONObject();

        boolean success = false;
        destinationFolder = AdminUploadValidator.normalizeDestinationFolder(destinationFolder);
        fileName = AdminUploadValidator.normalizeFileName(fileName);

        Identity user = UsersDB.getCurrentUser(request);
        // File content and size were already checked by the chunk endpoint.
        String errorKey = AdminUploadValidator.validateConflict(
            destinationFolder, fileName, uploadType, 0, user, request
        );

        String destinationFileName = fileName;

        if (errorKey == null && keepBoth) {
            destinationFileName = UploadService.getKeppBothFileName(destinationFolder, fileName);
        }

        if (errorKey == null && destinationFileName!=null) {

            try {
                UploadService uploadService = new UploadService(fileKey, destinationFolder, destinationFileName, request);

                output.put("virtualPath", uploadService.getVirtualPath());

                uploadService.process();
                errorKey = uploadService.getErrorKey();
                success = errorKey == null;
            }
            catch (Exception ex) {
                Logger.error(AdminUploadController.class, ex);
                errorKey = "multiple_files_upload.upload_error";
            }
        }
        else if (errorKey == null) {
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
