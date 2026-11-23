package sk.iway.iwcm.system.elfinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.io.FileHistoryDB;
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

        int domainId = CloudToolsForCore.getDomainId();
        if (FileHistoryDB.isFileHistoryAccessible(filePath, domainId, getUser()) == false) {
            return new DatatablePageImpl<>(new ArrayList<>());
        }

        //Get data based on filePath and domainId
        Page<FileHistoryEntity> page = fileHistoryRepository.findAllByFileUrlAndDomainIdOrderByChangeDateDesc(filePath, domainId, pageable);

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
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<FileHistoryEntity> root, CriteriaBuilder builder) {
        String filePath = Tools.getStringValue(getRequest().getParameter("filePath"), null);
        int domainId = CloudToolsForCore.getDomainId();
        if (FileHistoryDB.isFileHistoryAccessible(filePath, domainId, getUser()) == false) {
            predicates.add(builder.disjunction());
            return;
        }

        super.addSpecSearch(params, predicates, root, builder);
        predicates.add(builder.equal(root.get("fileUrl"), filePath));
        predicates.add(builder.equal(root.get("domainId"), domainId));
    }

    @Override
    public JSONObject sumItems(FileHistoryEntity entity, String[] columns) {
        return new JSONObject();
    }

    @Override
    public boolean checkItemPerms(FileHistoryEntity entity, Long id) {
        // Keep the existing rollback notification response; processAction performs the full check.
        if (isRollbackActionRequest()) return true;

        return entity != null && entity.getId() != null && id != null && id.longValue() > 0 &&
            entity.getId().longValue() == id.longValue() &&
            FileHistoryDB.isFileHistoryAccessible(entity.getFileUrl(), entity.getDomainId(), getUser());
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
    public boolean deleteItem(FileHistoryEntity entity, long id) {
        throwError("datatables.error.recordIsNotEditable");
        return false;
    }

    @Override
    public boolean processAction(FileHistoryEntity entity, String action) {
        if ("rollBack".equals(action)) {
            String fileUrl = entity == null ? null : entity.getFileUrl();
            String historyPath = entity == null ? null : entity.getHistoryPath();
            IwcmFile historyFile = null;
            IwcmFile currentFile = null;

            if (entity != null && entity.getId() != null) {
                historyFile = FileHistoryDB.getFileHistorySourceFile(fileUrl, historyPath, entity.getId(),
                    entity.getDomainId(), getUser());
                currentFile = FileHistoryDB.getFileHistoryCurrentFile(fileUrl, entity.getDomainId(), getUser());
            }

            if (historyFile == null || currentFile == null) {
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

    private boolean isRollbackActionRequest() {
        return getRequest() != null && getRequest().getRequestURI() != null &&
            getRequest().getRequestURI().endsWith("/action/rollBack");
    }
}
