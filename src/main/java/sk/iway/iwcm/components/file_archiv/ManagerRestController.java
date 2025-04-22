package sk.iway.iwcm.components.file_archiv;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.file_archiv.BasicManagerBean.ManagerType;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/file-archive-manager")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuFileArchivManagerCategory')")
@Datatable
public class ManagerRestController extends DatatableRestControllerV2<BasicManagerBean, Long> {
    
    private final FileArchiveRepository repository;
    private static final String MANAGER_TYPE = "manager-type";

    @Autowired
    public ManagerRestController(FileArchiveRepository repository) {
        super(null);
        this.repository = repository;
    }

    @Override
    public Page<BasicManagerBean> getAllItems(Pageable pageable) {
        return new DatatablePageImpl<>( getValues() );
    }

    @Override
    public BasicManagerBean getOneItem(long id) {
        if(id < 1) {
            return new BasicManagerBean(-1L, "");
        } else {
            List<BasicManagerBean> allValues = getValues();
            return allValues.get((int)id - 1);
        }
    }

    @Override
    public BasicManagerBean editItem(BasicManagerBean entity, long id) {
        ManagerType type = ManagerType.fromString( getRequest().getParameter(MANAGER_TYPE) );

        if(type == ManagerType.CATEGORY) {
            repository.updateCategory(entity.getName(), entity.getNewName(), CloudToolsForCore.getDomainId());
        } else if(type == ManagerType.PRODUCT) {
            repository.updateProduct(entity.getName(), entity.getNewName(), CloudToolsForCore.getDomainId());
        }

        if(type != ManagerType.NONE) {
            entity.setName( entity.getNewName() );
            entity.setNewName(null);
            return entity;
        } 
        else return null;
    }

    @Override
    public boolean deleteItem(BasicManagerBean entity, long id) {
        ManagerType type = ManagerType.fromString( getRequest().getParameter(MANAGER_TYPE) );

        if(type == ManagerType.CATEGORY) {
            repository.updateCategory(entity.getName(), "", CloudToolsForCore.getDomainId());
        } else if(type == ManagerType.PRODUCT) {
            repository.updateProduct(entity.getName(), "", CloudToolsForCore.getDomainId());
        }
        
        return true;
    }

    private List<BasicManagerBean> getValues() {
        ManagerType type = ManagerType.fromString( getRequest().getParameter(MANAGER_TYPE) );
        List<String> values = new ArrayList<>();

        if(type == ManagerType.CATEGORY) {
            values = repository.getDistinctCategory(CloudToolsForCore.getDomainId());
        } else if(type == ManagerType.PRODUCT) {
            values = repository.getDistinctProduct(CloudToolsForCore.getDomainId());
        }

        return BasicManagerBean.getEntitiesFromStrings(values);
    }

    
}
