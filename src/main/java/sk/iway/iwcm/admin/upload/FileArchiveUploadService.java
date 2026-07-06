package sk.iway.iwcm.admin.upload;

import java.util.Date;

import org.json.JSONObject;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.components.file_archiv.FileArchiveRepository;
import sk.iway.iwcm.components.file_archiv.FileArchiveService;
import sk.iway.iwcm.components.file_archiv.FileArchivSupportMethodsService;
import sk.iway.iwcm.components.file_archiv.FileArchivatorBean;
import sk.iway.iwcm.components.file_archiv.FileArchivatorEditorFields;
import sk.iway.iwcm.components.file_archiv.FileArchivatorKit;
import sk.iway.iwcm.i18n.Prop;

/**
 * Shared service for file archive upload operations used by both AdminUploadServlet and AdminUploadController.
 * Centralizes archive permission checks, new file creation, and overwrite logic.
 */
public class FileArchiveUploadService {

    private FileArchiveUploadService() {
        // utility class
    }

    /**
     * Validates that the user has permission to upload to the given archive folder.
     * @param user - current user
     * @param destinationFolder - raw destination folder from the request
     * @param referer - HTTP referer header
     * @return error key if validation fails, null if OK
     */
    public static String validateArchiveUploadPermission(Identity user, String destinationFolder, String referer) {
        String archiveFolder = FileArchivSupportMethodsService.normalizePath(destinationFolder);
        String archiveRootFolder = FileArchivSupportMethodsService.normalizePath(FileArchivatorKit.getArchivPath());

        if (Tools.isEmpty(referer) || referer.endsWith("/apps/file-archive/admin/") == false
            || user == null || user.isAdmin() == false || user.isEnabledItem("cmp_file_archiv") == false
            || archiveFolder.startsWith(archiveRootFolder) == false
            || FileBrowserTools.hasForbiddenSymbol(archiveFolder)
            || user.isFolderWritable(archiveFolder) == false) {
            return "admin.upload_iframe.wrong_upload_dir";
        }
        return null;
    }

    /**
     * Returns the normalized archive folder path.
     * @param destinationFolder - raw destination folder
     * @return normalized path
     */
    public static String normalizeArchiveFolder(String destinationFolder) {
        return FileArchivSupportMethodsService.normalizePath(destinationFolder);
    }

    /**
     * Saves a new file into the archive (non-existing file). Called after chunk assembly in AdminUploadServlet.
     * @param user - current user
     * @param prop - localization instance
     * @param destinationFolder - normalized archive folder
     * @param fileName - sanitized file name
     * @param originalName - original file name (used for virtualFileName)
     * @param fileKey - temp file key (random string)
     * @param output - JSON output to populate with result
     */
    public static void saveNewArchiveFile(Identity user, Prop prop, String destinationFolder, String fileName,
                                          String originalName, String fileKey, JSONObject output) {
        FileArchiveRepository repository = Tools.getSpringBean("fileArchiveRepository", FileArchiveRepository.class);
        Long existingFileId = FileArchiveService.getId(destinationFolder, fileName, repository);

        if (existingFileId > 0 || FileTools.isFile(destinationFolder + fileName)) {
            output.put("exists", true);
        } else {
            FileArchivatorBean entity = new FileArchivatorBean();
            entity.setDateInsert(new Date());
            entity.setFilePath(destinationFolder);
            entity.setShowFile(true);
            entity.setVirtualFileName(FileTools.getFileNameWithoutExtension(originalName));

            FileArchivatorEditorFields editorFields = new FileArchivatorEditorFields();
            editorFields.setDir(destinationFolder);
            editorFields.setFile(fileKey);
            entity.setEditorFields(editorFields);

            String result = saveArchiveEntity(user, prop, entity, repository);

            if (Tools.isNotEmpty(result)) {
                putError(output, prop, result);
            } else {
                output.put("name", entity.getFileName());
                output.put("destinationFolder", entity.getFilePath());
                output.put("virtualPath", entity.getVirtualPath());
                output.put("exists", false);
            }

            AdminUploadServlet.deleteTempFile(fileKey);
        }
    }

    /**
     * Overwrites an existing archive file with a new upload. Called from AdminUploadController.
     * @param user - current user
     * @param prop - localization instance
     * @param archiveFolder - normalized archive folder
     * @param fileName - file name to overwrite
     * @param fileKey - temp file key
     * @param output - JSON output to populate with result
     */
    public static void overwriteArchiveFile(Identity user, Prop prop, String archiveFolder, String fileName,
                                            String fileKey, JSONObject output) {
        saveArchiveFileVersion(user, prop, archiveFolder, fileName, fileKey, "replacement", output);
    }

    /**
     * Uploads a new version of an existing archive file. Called from AdminUploadController.
     * @param user - current user
     * @param prop - localization instance
     * @param archiveFolder - normalized archive folder
     * @param fileName - file name whose new version is uploaded
     * @param fileKey - temp file key
     * @param output - JSON output to populate with result
     */
    public static void uploadNewArchiveFileVersion(Identity user, Prop prop, String archiveFolder, String fileName,
                                                   String fileKey, JSONObject output) {
        saveArchiveFileVersion(user, prop, archiveFolder, fileName, fileKey, "new_version", output);
    }

    private static void saveArchiveFileVersion(Identity user, Prop prop, String archiveFolder, String fileName,
                                               String fileKey, String uploadType, JSONObject output) {
        FileArchiveRepository repository = Tools.getSpringBean("fileArchiveRepository", FileArchiveRepository.class);
        Long existingFileId = FileArchiveService.getId(archiveFolder, fileName, repository);
        FileArchivatorBean entity = repository.findFirstByIdAndDomainId(existingFileId, CloudToolsForCore.getDomainId()).orElse(null);
        if (entity == null) {
            putError(output, prop, "components.file_archiv.not_found_archiv_record");
            return;
        }

        FileArchivatorEditorFields editorFields = new FileArchivatorEditorFields();
        editorFields.setDir(archiveFolder);
        editorFields.setFile(fileKey);
        editorFields.setUploadType(uploadType);
        entity.setEditorFields(editorFields);

        String result = saveArchiveEntity(user, prop, entity, repository);

        if (Tools.isNotEmpty(result)) {
            putError(output, prop, result);
        } else {
            AdminUploadServlet.deleteTempFile(fileKey);
            output.put("success", true);
            output.put("virtualPath", entity.getVirtualPath());
        }
    }

    private static String saveArchiveEntity(Identity user, Prop prop, FileArchivatorBean entity, FileArchiveRepository repository) {
        FileArchiveService fileArchiveService = new FileArchiveService(user, prop, entity, repository);
        String result = fileArchiveService.saveFile();
        if (Tools.isEmpty(result) && fileArchiveService.getErrorList().isEmpty() == false) {
            result = fileArchiveService.getErrorList().get(0);
        }
        return result;
    }

    private static void putError(JSONObject output, Prop prop, String errorKey) {
        output.put("success", false);
        output.put("error", prop.getText(errorKey));
    }
}
