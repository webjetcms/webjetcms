package sk.iway.iwcm.admin.upload;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.common.UploadFileTools;

/**
 * Shared security checks for chunk upload and conflict resolution endpoints.
 */
final class AdminUploadValidator {

    static final String ERROR_INVALID_FOLDER = "admin.upload_iframe.wrong_upload_dir";
    static final String ERROR_INVALID_FILE = "components.forum.new.upload_not_allowed_filetype";

    private static final String[] ALLOWED_ROOTS = { "/images", "/files", "/shared" };
    private static final String PROTECTED_UPLOAD_FOLDER = "/files/protected/upload/";
    private static final String FEEDBACK_FORM_FOLDER = "/files/protected/feedback-form/";

    private AdminUploadValidator() {
        // Utility class.
    }

    /**
     * Adds the trailing slash expected by the existing upload code.
     */
    static String normalizeDestinationFolder(String destinationFolder) {
        if (Tools.isNotEmpty(destinationFolder) && destinationFolder.endsWith("/") == false) {
            return destinationFolder + "/";
        }
        return destinationFolder;
    }

    /**
     * Applies the same file-name normalization to every admin upload endpoint.
     */
    static String normalizeFileName(String fileName) {
        if (fileName == null) return null;

        String normalized = DB.internationalToEnglish(fileName);
        return DocTools.removeCharsDir(normalized, true).toLowerCase();
    }

    /**
     * Validates a chunk upload. The two protected folders are exempt from the
     * write-permission check only when the upload remains in temporary storage.
     *
     * @return localization key describing the error, or {@code null} when valid
     */
    static String validateChunk(String destinationFolder, String fileName, String uploadType,
            long fileSize, boolean writeDirectlyToDestination, Identity user,
            HttpServletRequest request) {
        return validate(destinationFolder, fileName, uploadType, fileSize, user, request,
            writeDirectlyToDestination == false);
    }

    /**
     * Validates constraints independent of the destination folder. File-archive
     * uploads use this before their dedicated destination permission check.
     *
     * @return localization key describing the error, or {@code null} when valid
     */
    static String validateUserAndFile(String fileName, String uploadType, long fileSize,
            Identity user, HttpServletRequest request) {
        String errorKey = validateUser(user);
        if (errorKey != null) return errorKey;

        return validateFile(fileName, uploadType, fileSize, user, request);
    }

    /**
     * Validates overwrite and keepboth requests and always checks folder writability.
     *
     * @return localization key describing the error, or {@code null} when valid
     */
    static String validateConflict(String destinationFolder, String fileName, String uploadType,
            long fileSize, Identity user, HttpServletRequest request) {
        return validate(destinationFolder, fileName, uploadType, fileSize, user, request, false);
    }

    /**
     * Applies the checks shared by all administrative upload endpoints.
     */
    private static String validate(String destinationFolder, String fileName, String uploadType,
            long fileSize, Identity user, HttpServletRequest request,
            boolean allowProtectedFolderExceptions) {
        String errorKey = validateUser(user);
        if (errorKey != null) return errorKey;

        if (isAllowedDestinationFolder(destinationFolder) == false) {
            return ERROR_INVALID_FOLDER;
        }
        boolean skipFolderPermission = allowProtectedFolderExceptions
            && isFolderPermissionException(destinationFolder);
        if (skipFolderPermission == false && user.isFolderWritable(destinationFolder) == false) {
            return ERROR_INVALID_FOLDER;
        }
        return validateFile(fileName, uploadType, fileSize, user, request);
    }

    private static String validateUser(Identity user) {
        if (user == null || user.isAdmin() == false) {
            return "admin.logon.timeoutTitle";
        }
        return null;
    }

    /**
     * Validates the normalized file name and upload limits.
     */
    private static String validateFile(String fileName, String uploadType, long fileSize,
            Identity user, HttpServletRequest request) {
        if (Tools.isEmpty(fileName) || fileName.indexOf('/') >= 0 || fileName.indexOf('\\') >= 0
                || FileTools.isFileTypeForbiddenForUpload(fileName)
                || UploadFileTools.isFileAllowed(uploadType, fileName, fileSize, user, request) == false) {
            return ERROR_INVALID_FILE;
        }
        return null;
    }

    /**
     * Allows only an exact /images, /files or /shared path segment.
     */
    static boolean isAllowedDestinationFolder(String destinationFolder) {
        if (Tools.isEmpty(destinationFolder) || destinationFolder.indexOf('\\') >= 0
                || destinationFolder.contains("//")
                || FileBrowserTools.hasForbiddenSymbol(destinationFolder)) {
            return false;
        }

        for (String root : ALLOWED_ROOTS) {
            if (destinationFolder.equals(root) || destinationFolder.startsWith(root + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Preserves the two staging-folder exceptions used by the existing chunk upload.
     */
    private static boolean isFolderPermissionException(String destinationFolder) {
        return PROTECTED_UPLOAD_FOLDER.equals(destinationFolder)
            || FEEDBACK_FORM_FOLDER.equals(destinationFolder);
    }
}
