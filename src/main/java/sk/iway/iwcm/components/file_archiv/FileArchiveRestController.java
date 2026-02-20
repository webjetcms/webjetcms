package sk.iway.iwcm.components.file_archiv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.upload.AdminUploadServlet;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/file-archive")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_file_archiv')")
@Datatable
public class FileArchiveRestController extends DatatableRestControllerV2<FileArchivatorBean, Long> {

    private final FileArchiveRepository repository;
    private static final String REFERENCE_ID = "referenceId";

    private static final String NOT_MAIN_FILE = "components.file_archiv.error.not_main_file";
    private static final String NOT_MAIN_FILE_PERMISSION_DENIED = "components.file_archiv.error.not_main_file_no_perms";
    private static final String PERMISSION_DENIED = "admin.operationPermissionDenied";

    private static final String EDIT_DEL_ROLLBACK_PERM = "cmp_fileArchiv_edit_del_rollback";
    private static final String HISTORY_METADATA_EDIT_PERM = "cmp_fileArchiv_history_metadata_edit";

    @Autowired
    public FileArchiveRestController(FileArchiveRepository repository) {
        super(repository);
        this.repository = repository;
    }

    @Override
    public Page<FileArchivatorBean> getAllItems(Pageable pageable) {
        Long entityId = Tools.getLongValue(getRequest().getParameter("id"), -1);

        if(isListOfVersions() && entityId > 0)
            return new DatatablePageImpl<>( repository.findAllByReferenceIdAndUploadedAndDomainId(entityId, -1, CloudToolsForCore.getDomainId(), pageable) );
        else if(isWaitingFile() && entityId > 0)
            return new DatatablePageImpl<>( FileArchiveService.getWaitingFileList(entityId, repository) );
        else if(isListOfPattern() && entityId > 0) {
            FileArchivatorBean mainEntity = repository.findFirstByIdAndDomainId(entityId, CloudToolsForCore.getDomainId()).orElse(null);
            //CAnt be null and cant have set reference to main, because only NON pattern can have patterns
            if(mainEntity == null || Tools.isNotEmpty(mainEntity.getReferenceToMain())) return new DatatablePageImpl<>( new ArrayList<>() );

            //Show only patterns joined to main file BUT onnly mainVersion of pattern (no hisotry versions)
            return new DatatablePageImpl<>( repository.findAllByReferenceToMainAndReferenceIdAndDomainId(mainEntity.getVirtualPath(), -1, CloudToolsForCore.getDomainId()) );
        } else if(isSelectedFilesApp()) {
            /*
             * Call from fileArchiveApp editor, to return list of files by globalIds
             *
             * File MUST BE:
             * - MAIN file (referenceId = -1)
             * - NOT waiting file (uploaded = -1)
             * - NOT pattern (referenceToMain = null/empty)
             */

            int[] globalIds = Tools.getTokensInt(getRequest().getParameter("globalIds"), "+, ");
            if(globalIds.length == 0)
                return new DatatablePageImpl<>( repository.findAllMainFilesUploadedNotPatternIdsIn(Arrays.asList(-1), CloudToolsForCore.getDomainId(), pageable) );

            return new DatatablePageImpl<>( repository.findAllMainFilesUploadedNotPatternIdsIn(Arrays.stream(globalIds).boxed().toList(), CloudToolsForCore.getDomainId(), pageable) );
        }

        //Else classic table
        DatatablePageImpl<FileArchivatorBean> page = new DatatablePageImpl<>( super.getAllItemsIncludeSpecSearch(new FileArchivatorBean(), pageable) );

        //Add options - upload like
        page.addOptions("editorFields.uploadType", FileArchiveService.getUploadTypeOptions(getProp()), "label", "value", false);

        //Add list of icons
        page.addOptions("editorFields.statusIcons", FileArchiveService.getStatusIconOptions(getProp()), "label", "value", false);

        return page;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<FileArchivatorBean> root, CriteriaBuilder builder) {
        if(params.get("searchEditorFields.statusIcons") == null) {
            //if not set, show only main files
            predicates.add( builder.equal(root.get(REFERENCE_ID), -1) );
        }

        if("waitingFiles".equals(params.get("searchEditorFields.statusIcons"))) {
            List<Integer> referenceIds = repository.findDistinctReferenceIds(CloudToolsForCore.getDomainId());
            predicates.add(
                builder.and(
                    builder.equal(root.get(REFERENCE_ID), -1),
                    builder.equal(root.get("uploaded"), -1),
                    root.get("id").in(referenceIds)
                )
            );
        }

        super.addSpecSearch(params, predicates, root, builder);
    }

    @Override
    public FileArchivatorBean getOneItem(long id) {

        FileArchivatorBean entity;
        if(id == -1) {
            entity = new FileArchivatorBean();
            processFromEntity(entity, ProcessItemAction.CREATE, 1);
        } else {
            entity = repository.findById(id).orElse(null);
            processFromEntity(entity, ProcessItemAction.EDIT, 1);
        }

        return entity;
    }

    @Override
    public boolean beforeDelete(FileArchivatorBean entity) {
        if(getUser().isEnabledItem(EDIT_DEL_ROLLBACK_PERM) == false)
            throwError(PERMISSION_DENIED);

        //Some rules are not checked for waiting files
        if(isWaitingFile() == false) {
            if(entity.getReferenceId() > -1)
                throwError(NOT_MAIN_FILE);

            //Cant be deleted, IF there are waiting files for this entity
            List<FileArchivatorBean> waitingFiles = FileArchiveService.getWaitingFileList(entity.getId(), repository);
            if(waitingFiles != null && waitingFiles.isEmpty() == false)
                throwError("components.file_archiv.error.has_waiting_file");
        }

        return true;
    }

    @Override
    public void beforeDuplicate(FileArchivatorBean entity) {
        //Unsupported action
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void beforeSave(FileArchivatorBean entity) {
        //Now check, that we editing main page (exception if we are editing waiting file)
        if(entity.getId() != null && entity.getId() > 0 && entity.getReferenceId() > -1) {
            if(isWaitingFile() == true) {
                // Do nothing - we can edit waiting files that gonna be new versions in future
            } else if(isListOfVersions() == true) {
                // List of version can be edited only with special permission
                if(getUser().isEnabledItem(HISTORY_METADATA_EDIT_PERM) == false) {
                    throwError(NOT_MAIN_FILE_PERMISSION_DENIED);
                }
            } else {
                throwError(NOT_MAIN_FILE);
            }
        }
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, FileArchivatorBean> target, Identity user, Errors errors, Long id, FileArchivatorBean entity) {
        super.validateEditor(request, target, user, errors, id, entity);

        if("remove".equals(target.getAction())) return;

        if("edit".equals(target.getAction()) &&  getUser().isEnabledItem(EDIT_DEL_ROLLBACK_PERM) == false) {
            throwError(PERMISSION_DENIED);
        }

        if(id != null && id > 0) {
            FileArchivatorBean originalDbVersion = getOne(id);
            copyEntityIntoOriginal(entity, originalDbVersion);
            entity = originalDbVersion;
        }

        processToEntity(entity, null);

        if(isListOfVersions() == true) {
            // Do nothing with file, because we edit only metadata of history versions
            return;
        }

        FileArchiveService fas = new FileArchiveService(user, getProp(), entity, repository);
        fas.checkFileProperties(errors);

        if(fas.getSameFiles().isEmpty() == false) {
            //Special notification - show all same files in archive
            StringBuilder sb = new StringBuilder( getProp().getText("components.file_archiv.redundant_file_links"));
            sb.append(":<br>");
            for(FileArchivatorBean fab : fas.getSameFiles()) {
                String path = fab.getVirtualPath();
                if(path.startsWith(FileArchivSupportMethodsService.SEPARATOR) == false) path = FileArchivSupportMethodsService.SEPARATOR + path;
                sb.append("<a href='").append(path).append("' target='blank' style='color: #3366CC;'>").append(path).append("</a>").append("<br>");
            }
            addNotify(new NotifyBean(getDialogHeadingText(), sb.toString(), NotifyType.WARNING, 20000));
        }

        if(fas.getErrorList().isEmpty() == false)
            throwError(fas.getErrorList().get(0), true, fas.getErrorParams());
    }

    @Override
    public FileArchivatorBean insertItem(FileArchivatorBean entity) {
        Logger.debug(FileArchiveRestController.class, "insertItem() IP: " + Tools.getRemoteIP(getRequest())+" file: " + AdminUploadServlet.getOriginalFileName( entity.getEditorFields().getFile() ) + ", oldId:" + entity.getReferenceId());

        processToEntity(entity, ProcessItemAction.CREATE);

        FileArchiveService fas = new FileArchiveService(getUser(), getProp(), entity, repository);
        String result = fas.saveFile();

        if(Tools.isNotEmpty(result)) {
            throwError(result);
        } else if(fas.getErrorList().isEmpty() == false) {
            throwError(fas.getErrorList().get(0));
        }

        return entity;
    }

    @Override
    public FileArchivatorBean editItem(FileArchivatorBean entity, long id) {
        if(id > 0) {
            FileArchivatorBean originalDbVersion = getOne(id);
            copyEntityIntoOriginal(entity, originalDbVersion);
            entity = originalDbVersion;
        }

        Logger.debug(FileArchiveRestController.class, "editItem() IP: " + Tools.getRemoteIP(getRequest())+" file: " + AdminUploadServlet.getOriginalFileName( entity.getEditorFields().getFile() ) + ", oldId:" + entity.getReferenceId());

        processToEntity(entity, ProcessItemAction.EDIT);

        FileArchiveService fas = new FileArchiveService(getUser(), getProp(), entity, repository);
        String result = fas.saveFile();

        if(Tools.isNotEmpty(result))
            throwError(result);

        if(fas.getErrorList().isEmpty() == false)
            throwError(fas.getErrorList().get(0));

        return  entity;
    }

    @Override
    public void afterSave(FileArchivatorBean entity, FileArchivatorBean saved) {
        FileArchiveService fas = new FileArchiveService(getUser(), getProp(), saved, repository);
        int result = fas.checkAndRenameFile();

        if(result == 1) {
            addNotify(new NotifyBean(getDialogHeadingText(), getProp().getText("components.file_archiv.rename_successfull"), NotifyType.SUCCESS, 15000));
        } else if(result == 0) {
            addNotify(new NotifyBean(getDialogHeadingText(), getProp().getText("components.file_archiv.rename_error"), NotifyType.ERROR, 15000));
        }

        //Remove tmp file
        String fileKey = saved.getEditorFields().getFile();
        if(Tools.isNotEmpty( fileKey )) {
            boolean wasRemoved = AdminUploadServlet.deleteTempFile( fileKey );
            if(wasRemoved == false)
                Logger.debug(this, "TEMP subor sa nepodarilo zmazat: " + fileKey);
        }

        setForceReload(true);
    }

    @Override
    public boolean deleteItem(FileArchivatorBean entity, long id) {
        FileArchiveService fas = new FileArchiveService(getUser(), getProp(), entity, repository);

        String result;
        if(isWaitingFile() == false ) {
            //Delete main file and all versions
            result = fas.deleteStructure();
        } else {
            //Delete only waiting file
            result = fas.deleteWaitingFile();
        }

        if(result != null) throwError(result);

        return true;
    }

    @Override
    public FileArchivatorBean processFromEntity(FileArchivatorBean entity, ProcessItemAction action, int rowCount) {
        if(entity != null) {
            FileArchivatorEditorFields faef = new FileArchivatorEditorFields();
            faef.fromFileArchivatorBean(entity, action, getUser(), rowCount, repository);
        }

        return entity;
    }

    @Override
    public FileArchivatorBean processToEntity(FileArchivatorBean entity, ProcessItemAction action) {
        if(entity.getEditorFields() == null) {
            entity.setEditorFields( new FileArchivatorEditorFields() );
        } else {
            entity.setFilePath( DocTools.removeCharsDir(entity.getEditorFields().getDir()) );
        }

        if(entity.getDateInsert() == null) entity.setDateInsert( new Date() );

        return entity;
    }

    @Override
    public boolean processAction(FileArchivatorBean entity, String action) {
        //Check, that this is main version
        if(entity.getReferenceId() > -1) {
            addNotify(new NotifyBean(getRollBackText(), getProp().getText(NOT_MAIN_FILE), NotifyType.ERROR, 15000));
            return true;
        }

        if("rollback".equals(action)) {

            if(getUser().isEnabledItem(EDIT_DEL_ROLLBACK_PERM) == false) {
                addNotify(new NotifyBean(getRollBackText(), getProp().getText(PERMISSION_DENIED), NotifyType.ERROR, 15000));
                return true;
            }

            //Cant perform rollback, IF there are waiting files for this entity
            List<FileArchivatorBean> waitingFiles = FileArchiveService.getWaitingFileList(entity.getId(), repository);
            if(waitingFiles != null && waitingFiles.isEmpty() == false) {
                addNotify(new NotifyBean(getRollBackText(), getProp().getText("components.file_archiv.error.has_waiting_file"), NotifyType.ERROR, 15000));
                return true;
            }

            FileArchiveService fas = new FileArchiveService(getUser(), getProp(), entity, repository);

            if(fas.rollback()) {
                addNotify(new NotifyBean(getRollBackText(), getProp().getText("components.rollback_web_pages.successful"), NotifyType.SUCCESS, 15000));
            } else {
                addNotify(new NotifyBean(getRollBackText(), getProp().getText("components.rollback_web_pages.unsuccessful"), NotifyType.ERROR, 15000));
            }
        }

        return true;
    }

    @GetMapping("/autocomplete-product")
    public List<String> getAutocompleteProduct(@RequestParam String term, @RequestParam(required = false, name="DTE_Field_dir") String filePath) {
        List<String> ac = new ArrayList<>();

        filePath = FileArchivSupportMethodsService.normalizeToOldPath(filePath);

        List<FileArchivatorBean> entities = repository.findDistinctAllByProductLikeAndFilePathLikeAndDomainId("%" + term + "%", filePath + "%", CloudToolsForCore.getDomainId());

        //Loop gained entities and add products to autcomplete list "ac"
        for(FileArchivatorBean entity : entities) {
            if (ac.contains(entity.getProduct()) == false) ac.add(entity.getProduct());
        }

        return ac;
    }

    @GetMapping("/autocomplete-category")
    public List<String> getAutocompleteCategory(@RequestParam String term, @RequestParam(required = false, name="DTE_Field_dir") String filePath) {
        List<String> ac = new ArrayList<>();

        filePath = FileArchivSupportMethodsService.normalizeToOldPath(filePath);

        List<FileArchivatorBean> entities = repository.findDistinctAllByCategoryLikeAndFilePathLikeAndDomainId("%" + term + "%", filePath + "%", CloudToolsForCore.getDomainId());

        //Loop gained entities and add categories to autcomplete list "ac"
        for(FileArchivatorBean entity : entities) {
            if (ac.contains(entity.getCategory()) == false) ac.add(entity.getCategory());
        }

        return ac;
    }

    private boolean isWaitingFile() {
        return Tools.getBooleanValue(getRequest().getParameter("waitingFiles"), false);
    }
    private boolean isListOfVersions() {
        return Tools.getBooleanValue(getRequest().getParameter("listOfVersions"), false);
    }
    private boolean isListOfPattern() {
        return Tools.getBooleanValue(getRequest().getParameter("listOfPattern"), false);
    }
    private boolean isSelectedFilesApp() {
        return Tools.getBooleanValue(getRequest().getParameter("selectedFilesApp"), false);
    }

    @GetMapping("/history-versions")
    public List<String> getHistoryVersions(@RequestParam long id) {
        List<String> result = new ArrayList<>();

        //Add main file as option
        FileArchivatorBean main = repository.findById(id).orElse(null);
        if(main != null) result.add( main.toStringForOption() );

        List<FileArchivatorBean> historyFiles = repository.findAllByReferenceIdAndUploadedAndDomainId(id, -1, CloudToolsForCore.getDomainId());
        for(FileArchivatorBean historyVersion : historyFiles) {
            result.add( historyVersion.toStringForOption() );
        }

        return result;
    }

    @GetMapping("/autocomplete")
    public List<String> getAutocomplete(@RequestParam String term, @RequestParam(required = true, name="DTE_Field_filePath") String filePath, @RequestParam(required = true, name="DTE_Field_fileName") String fileName) {
        List<String> ac = new ArrayList<>();
        List<FileArchivatorBean> files = repository.findAllMainFilesUploadedNotPatternsVirtualPathLike("%" + term + "%", CloudToolsForCore.getDomainId());
        for(FileArchivatorBean file : files) {
            //Filter out files with reference to main
            if(Tools.isNotEmpty(file.getReferenceToMain())) continue;

            //Filter out file himself
            if(file.getVirtualPath().equals( filePath + fileName )) continue;

            ac.add( file.getVirtualPath() );
        }

        return ac;
    }

    private String getRollBackText() { return getProp().getText("components.file_archiv.rollback_last"); }
    private String getDialogHeadingText() { return getProp().getText("components.file_archiv.dialog_heading"); }
}