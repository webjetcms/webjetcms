package sk.iway.iwcm.admin.upload;

import java.util.Date;

import org.json.JSONObject;
import org.springframework.validation.BeanPropertyBindingResult;

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
import sk.iway.iwcm.system.datatable.DatatableRequest;

/**
 * Coordinates file archive uploads shared by {@link AdminUploadServlet} and {@link AdminUploadController}.
 * Centralizes archive permission checks, metadata validation, new-file creation, and version updates.
 */
public class FileArchiveUploadService {

    private FileArchiveUploadService() {
        // utility class
    }

    /**
     * Validates that the user has permission to upload to the given archive folder.
     *
     * @param user current user
     * @param destinationFolder raw destination folder from the request
     * @param referer HTTP referer header
     * @return an error key when validation fails; otherwise {@code null}
     */
    public static String validateArchiveUploadPermission(Identity user, String destinationFolder, String referer) {
        String archiveFolder = FileArchivSupportMethodsService.normalizePath(destinationFolder);
        String archiveRootFolder = FileArchivSupportMethodsService.normalizePath(FileArchivatorKit.getArchivPath());

        if (Tools.isEmpty(archiveFolder) || Tools.isEmpty(archiveRootFolder)
            || Tools.isEmpty(referer) || referer.endsWith("/apps/file-archive/admin/") == false
            || user == null || user.isAdmin() == false || user.isEnabledItem("cmp_file_archiv") == false
            || archiveFolder.startsWith(archiveRootFolder) == false
            || FileBrowserTools.hasForbiddenSymbol(archiveFolder)
            || user.isFolderWritable(archiveFolder) == false) {
            return "admin.upload_iframe.wrong_upload_dir";
        }
        return null;
    }

    /**
     * Normalizes an archive folder path.
     *
     * @param destinationFolder raw destination folder
     * @return normalized folder path
     */
    public static String normalizeArchiveFolder(String destinationFolder) {
        return FileArchivSupportMethodsService.normalizePath(destinationFolder);
    }

    /**
     * Saves a newly assembled file as a new archive record using default metadata.
     *
     * @param user current user
     * @param prop localization provider
     * @param destinationFolder normalized archive folder
     * @param fileName sanitized file name
     * @param originalName original file name used to derive the virtual name
     * @param fileKey temporary upload key
     * @param output JSON object populated with the result
     */
    public static void saveNewArchiveFile(Identity user, Prop prop, String destinationFolder, String fileName,
                                          String originalName, String fileKey, JSONObject output) {
        saveNewArchiveFile(user, prop, destinationFolder, fileName, originalName, fileKey,
            FileArchiveBulkUploadOptions.none(), output);
    }

    /**
     * Saves a newly assembled file as a new archive record with validated bulk metadata.
     *
     * @param user current user
     * @param prop localization provider
     * @param destinationFolder normalized archive folder
     * @param fileName sanitized file name
     * @param originalName original file name used to derive the virtual name
     * @param fileKey temporary upload key
     * @param bulkUploadOptions metadata to apply before validation and persistence
     * @param output JSON object populated with the result
     */
    static void saveNewArchiveFile(Identity user, Prop prop, String destinationFolder, String fileName,
                                   String originalName, String fileKey, FileArchiveBulkUploadOptions bulkUploadOptions,
                                   JSONObject output) {
        FileArchiveRepository repository = Tools.getSpringBean("fileArchiveRepository", FileArchiveRepository.class);
        Long existingFileId = FileArchiveService.getId(destinationFolder, fileName, repository);

        if (existingFileId > 0 || FileTools.isFile(destinationFolder + fileName)) {
            output.put("exists", true);
        } else {
            FileArchivatorBean entity = new FileArchivatorBean();
            entity.setDateInsert(new Date());
            entity.setFilePath(destinationFolder);
            entity.setShowFile(true);
            String name = FileTools.getFileNameWithoutExtension(originalName);
            //replace -_ and other symbols with space
            name = name.replaceAll("[-_]+", " ");
            entity.setVirtualFileName(name);

            FileArchivatorEditorFields editorFields = new FileArchivatorEditorFields();
            editorFields.setDir(destinationFolder);
            editorFields.setFile(fileKey);
            entity.setEditorFields(editorFields);
            String optionsError = bulkUploadOptions.applyTo(entity);
            if (Tools.isNotEmpty(optionsError)) {
                putError(output, prop, optionsError);
            } else if (validateAndSaveArchiveEntity(user, prop, entity, repository, output)) {
                output.put("name", entity.getFileName());
                output.put("destinationFolder", entity.getFilePath());
                output.put("virtualPath", entity.getVirtualPath());
                output.put("exists", false);
            }

            AdminUploadServlet.deleteTempFile(fileKey);
        }
    }

    /**
     * Replaces an existing archive file with a temporary upload using default metadata.
     *
     * @param user current user
     * @param prop localization provider
     * @param archiveFolder normalized archive folder
     * @param fileName file name to replace
     * @param fileKey temporary upload key
     * @param output JSON object populated with the result
     */
    public static void overwriteArchiveFile(Identity user, Prop prop, String archiveFolder, String fileName,
                                            String fileKey, JSONObject output) {
        overwriteArchiveFile(user, prop, archiveFolder, fileName, fileKey, FileArchiveBulkUploadOptions.none(), output);
    }

    static void overwriteArchiveFile(Identity user, Prop prop, String archiveFolder, String fileName,
                                     String fileKey, FileArchiveBulkUploadOptions bulkUploadOptions,
                                     JSONObject output) {
        saveArchiveFileVersion(user, prop, archiveFolder, fileName, fileKey, "replacement", bulkUploadOptions, output);
    }

    /**
     * Adds a temporary upload as a new version of an archive file using default metadata.
     *
     * @param user current user
     * @param prop localization provider
     * @param archiveFolder normalized archive folder
     * @param fileName file receiving the new version
     * @param fileKey temporary upload key
     * @param output JSON object populated with the result
     */
    public static void uploadNewArchiveFileVersion(Identity user, Prop prop, String archiveFolder, String fileName,
                                                   String fileKey, JSONObject output) {
        uploadNewArchiveFileVersion(user, prop, archiveFolder, fileName, fileKey,
            FileArchiveBulkUploadOptions.none(), output);
    }

    static void uploadNewArchiveFileVersion(Identity user, Prop prop, String archiveFolder, String fileName,
                                            String fileKey, FileArchiveBulkUploadOptions bulkUploadOptions,
                                            JSONObject output) {
        saveArchiveFileVersion(user, prop, archiveFolder, fileName, fileKey, "new_version", bulkUploadOptions, output);
    }

    /**
     * Saves a replacement or new version after applying bulk metadata to the existing archive entity.
     *
     * @param user current user
     * @param prop localization provider
     * @param archiveFolder normalized archive folder
     * @param fileName existing archive file name
     * @param fileKey temporary upload key
     * @param uploadType archive operation type
     * @param bulkUploadOptions metadata to apply before validation and persistence
     * @param output JSON object populated with the result
     */
    private static void saveArchiveFileVersion(Identity user, Prop prop, String archiveFolder, String fileName,
                                               String fileKey, String uploadType, FileArchiveBulkUploadOptions bulkUploadOptions,
                                               JSONObject output) {
        FileArchiveRepository repository = Tools.getSpringBean("fileArchiveRepository", FileArchiveRepository.class);
        Long existingFileId = FileArchiveService.getId(archiveFolder, fileName, repository);
        FileArchivatorBean entity = repository.findFirstByIdAndDomainId(existingFileId, CloudToolsForCore.getDomainId()).orElse(null);
        if (entity == null) {
            AdminUploadServlet.deleteTempFile(fileKey);
            putError(output, prop, "components.file_archiv.not_found_archiv_record");
            return;
        }

        FileArchivatorEditorFields editorFields = new FileArchivatorEditorFields();
        editorFields.setDir(archiveFolder);
        editorFields.setFile(fileKey);
        editorFields.setUploadType(uploadType);
        entity.setEditorFields(editorFields);
        String optionsError = bulkUploadOptions.applyTo(entity);
        if (Tools.isNotEmpty(optionsError)) {
            AdminUploadServlet.deleteTempFile(fileKey);
            putError(output, prop, optionsError);
            return;
        }

        boolean saved = validateAndSaveArchiveEntity(user, prop, entity, repository, output);
        AdminUploadServlet.deleteTempFile(fileKey);
        if (saved) {
            output.put("success", true);
            output.put("virtualPath", entity.getVirtualPath());
        }
    }

    /**
     * Validates and persists an archive entity, translating the first failure into the response object.
     *
     * @param user current user
     * @param prop localization provider
     * @param entity archive entity to validate and save
     * @param repository archive repository
     * @param output JSON object populated when validation or persistence fails
     * @return {@code true} when the entity was saved successfully
     */
    static boolean validateAndSaveArchiveEntity(Identity user, Prop prop, FileArchivatorBean entity,
                                                FileArchiveRepository repository, JSONObject output) {
        FileArchiveService fileArchiveService = new FileArchiveService(user, prop, entity, repository);
        DatatableRequest<Long, FileArchivatorBean> validationTarget = new DatatableRequest<>();
        validationTarget.setErrorField(entity);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(validationTarget, "datatableRequest");
        boolean requireEditPermission = entity.getId() != null && entity.getId() > 0;
        fileArchiveService.checkFileProperties(errors, requireEditPermission);

        if (fileArchiveService.getErrorList().isEmpty() == false) {
            putError(output, prop, fileArchiveService.getErrorList().get(0), fileArchiveService.getErrorParams());
            return false;
        }
        if (errors.hasErrors()) {
            putLocalizedError(output, errors.getAllErrors().get(0).getDefaultMessage());
            return false;
        }

        String result = fileArchiveService.saveFile();
        if (Tools.isEmpty(result) && fileArchiveService.getErrorList().isEmpty() == false) {
            result = fileArchiveService.getErrorList().get(0);
        }
        if (Tools.isNotEmpty(result)) {
            putError(output, prop, result, fileArchiveService.getErrorParams());
            return false;
        }
        return true;
    }

    private static void putError(JSONObject output, Prop prop, String errorKey) {
        putError(output, prop, errorKey, null);
    }

    private static void putError(JSONObject output, Prop prop, String errorKey, String[] errorParams) {
        output.put("success", false);
        String error = errorParams != null && errorParams.length > 0
            ? prop.getTextWithParams(errorKey, errorParams)
            : prop.getText(errorKey);
        output.put("error", error);
    }

    private static void putLocalizedError(JSONObject output, String error) {
        output.put("success", false);
        output.put("error", error);
    }
}
