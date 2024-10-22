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

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
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
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_elfinder')")
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
    public boolean processAction(FileHistoryEntity entity, String action) {
        if ("rollBack".equals(action)) {
            IwcmFile historyFile = new IwcmFile( Tools.getRealPath( entity.getHistoryPath() + entity.getId() ) );
            
            if(historyFile.exists() == false){
                addNotify(new NotifyBean(getProp().getText("elfinder.file_prop.rollback.title"), getProp().getText("elfinder.file_prop.rollback.src_file_not_found.err"), NotifyType.ERROR, 15000));
                return true;
            }

            IwcmFile currentFile = new IwcmFile( Tools.getRealPath( entity.getFileUrl() ) );
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
}