package sk.iway.iwcm.system.elfinder;

import java.util.ArrayList;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;


@RestController
@RequestMapping("/admin/rest/elfinder/folder-properties/")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_elfinder')")
@Datatable
public class FolderPropertiesRestController extends DatatableRestControllerV2<FolderPropertiesEntity, Long> {
    
    private final FolderPropertiesRepository folderPropertiesRepository;
    private HttpServletResponse response;

    @Autowired
    public FolderPropertiesRestController(FolderPropertiesRepository folderPropertiesRepository, HttpServletResponse response) {
        super(folderPropertiesRepository);
        this.folderPropertiesRepository = folderPropertiesRepository;
        this.response = response;
    }

    @Override
    public Page<FolderPropertiesEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<FolderPropertiesEntity> page = new DatatablePageImpl<>( new ArrayList<>() );
        page.addOptions("editorFields.permisions", UserGroupsDB.getInstance().getUserGroupsByTypeId(UserGroupDetails.TYPE_PERMS), "userGroupName", "userGroupId", false);
        return page;
    }

    @Override
    public FolderPropertiesEntity getOneItem(long id) {
        return FolderPropertiesService.getOneItem(getRequest(), getUser(), folderPropertiesRepository);
    }

    @Override
    public void beforeSave(FolderPropertiesEntity entity) {
        if(getUser().isFolderWritable(entity.getDirUrl() + "/") == false)   
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        if(entity.getLogonDocId() == null) entity.setLogonDocId( -1 );
        entity.getEditorFields().toFolderProperties(entity);
    }

    @Override
    public void afterSave(FolderPropertiesEntity entity, FolderPropertiesEntity saved) {
        //refreshni zoznam v PathFilter
		PathFilter.reloadProtectedDirs();
    }

    @GetMapping("/fulltext-index")
    public void indexFolder(@RequestParam(required = true) String dir) {
        try {
            FolderPropertiesService.indexFolder(dir, getRequest(), response);
        } catch (Exception e) {
            Logger.error(e);
        }
    }
}