package sk.iway.iwcm.system.elfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@RestController
@RequestMapping("/admin/rest/elfinder/file-history/")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuFbrowser')")
@Datatable
public class FileHistoryRestController extends DatatableRestControllerV2<FileHistoryEntity, Long> {

    private final FileHistoryRepository fileHistoryRepository;

    @Autowired
    public FileHistoryRestController(FileHistoryRepository fileHistoryRepository) {
        super(fileHistoryRepository);
        this.fileHistoryRepository = fileHistoryRepository;
    }

    @Override
    public Page<FileHistoryEntity> getAllItems(Pageable pageable) {
        String filePath = Tools.getStringValue(getRequest().getParameter("filePath"), null);

        //Without filePath, return empty page
        if(filePath == null) return new DatatablePageImpl<>(new ArrayList<>());

        //Get data based on filePath and domainId
        Page<FileHistoryEntity> page = fileHistoryRepository.findAllByFileUrlAndDomainIdOrderByChangeDateDesc(filePath, CloudToolsForCore.getDomainId(), pageable);

        Map<Integer, String> usersMap = new HashMap<>();
        for(FileHistoryEntity entity : page.getContent()) {
            if(usersMap.get(entity.getUserId()) == null) {
               UserDetails user = UsersDB.getUser(entity.getUserId());
               usersMap.put(entity.getUserId(), user == null ? "" : user.getFullName());
            }

            entity.setUserName(usersMap.get(entity.getUserId()));
        }

        return page;
    }

    @Override
    public FileHistoryEntity insertItem(FileHistoryEntity entity) {
        throwError("datatables.error.recordIsNotEditable");
        return null;
    }

    @Override
    public FileHistoryEntity editItem(FileHistoryEntity entity, long id) {
        throwError("datatables.error.recordIsNotEditable");
        return null;
    }

    @Override
    public boolean processAction(FileHistoryEntity entity, String action) {
        if ("rollBack".equals(action)) {
            String fileUrl = entity == null ? null : entity.getFileUrl();
            String historyPath = entity == null ? null : entity.getHistoryPath();
            Identity user = getUser();

            if (entity == null || entity.getId() == null || entity.getId() < 1 || user == null ||
                isSafeVirtualPath(fileUrl) == false || fileUrl.endsWith("/") ||
                isSafeVirtualPath(historyPath) == false || historyPath.endsWith("/") == false) {
                addNotify(new NotifyBean(getProp().getText("elfinder.file_prop.rollback.title"), getProp().getText("user.rights.no_folder_rights"), NotifyType.ERROR, 15000));
                return true;
            }

            IwcmFile historyFile = new IwcmFile( Tools.getRealPath( historyPath + entity.getId() ) );
            IwcmFile currentFile = new IwcmFile( Tools.getRealPath( fileUrl ) );
            IwcmFile currentFolder = currentFile.getParentFile();
            String sourceFolder = getHistorySourceFolder(historyFile);

            if (currentFolder == null || sourceFolder == null ||
                user.isFolderWritable(currentFolder.getVirtualPath()) == false ||
                user.isFolderWritable(sourceFolder) == false) {
                addNotify(new NotifyBean(getProp().getText("elfinder.file_prop.rollback.title"), getProp().getText("user.rights.no_folder_rights"), NotifyType.ERROR, 15000));
                return true;
            }

            if(historyFile.exists() == false){
                addNotify(new NotifyBean(getProp().getText("elfinder.file_prop.rollback.title"), getProp().getText("elfinder.file_prop.rollback.src_file_not_found.err"), NotifyType.ERROR, 15000));
                return true;
            }

            if(currentFile.exists() == false) {
                addNotify(new NotifyBean(getProp().getText("elfinder.file_prop.rollback.title"), getProp().getText("elfinder.file_prop.rollback.src_file_not_found.err"), NotifyType.ERROR, 15000));
                return true;
            }

            if(FileTools.copyFile(historyFile, currentFile) == true) addNotify(new NotifyBean(getProp().getText("elfinder.file_prop.rollback.title"), getProp().getText("elfinder.file_prop.rollback.success"), NotifyType.SUCCESS, 15000));
            else addNotify(new NotifyBean(getProp().getText("elfinder.file_prop.rollback.title"), getProp().getText("elfinder.file_prop.rollback.error"), NotifyType.ERROR, 15000));

            return true;
        }

        return false;
    }

    /**
     * Maps the physical history folder back to the original virtual folder used by file-browser permissions.
     * History files are stored below {@code fileHistoryPath} (by default {@code /WEB-INF/libfilehistory/})
     * while preserving the original folder structure. For example,
     * {@code /WEB-INF/libfilehistory/images/gallery/} maps to {@code /images/gallery/}.
     * The configured history root must match on an exact folder boundary before it is removed.
     *
     * @param historyFile history file that will be used as the rollback source
     * @return original virtual source folder, or {@code null} when the file is outside the history root
     */
    private String getHistorySourceFolder(IwcmFile historyFile) {
        String configuredHistoryRoot = Constants.getString("fileHistoryPath");
        if (isSafeVirtualPath(configuredHistoryRoot) == false) return null;

        IwcmFile historyFolderFile = historyFile.getParentFile();
        if (historyFolderFile == null) return null;

        String historyRoot = IwcmFile.fromVirtualPath(configuredHistoryRoot).getVirtualPath();
        String historyFolder = historyFolderFile.getVirtualPath();
        if (historyRoot.equals(historyFolder)) return "/";
        if (historyFolder.startsWith(historyRoot + "/") == false) return null;

        String sourceFolder = historyFolder.substring(historyRoot.length());
        return isSafeVirtualPath(sourceFolder) ? sourceFolder : null;
    }

    /**
     * Validates a request-controlled virtual path before it is passed to {@link Tools#getRealPath(String)}.
     * Only absolute virtual paths are accepted; traversal, encoded/special symbols, backslashes and control
     * characters are rejected so path normalization cannot change the resource being authorized.
     *
     * @param path virtual path to validate
     * @return {@code true} when the path is safe to resolve
     */
    private static boolean isSafeVirtualPath(String path) {
        return Tools.isNotEmpty(path) && path.startsWith("/") && path.indexOf('\\') == -1 &&
            path.indexOf('\0') == -1 && path.indexOf('\r') == -1 && path.indexOf('\n') == -1 &&
            FileBrowserTools.hasForbiddenSymbol(path) == false;
    }
}
