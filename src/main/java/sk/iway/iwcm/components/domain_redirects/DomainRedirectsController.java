package sk.iway.iwcm.components.domain_redirects;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.domainRedirects.DomainRedirectBean;
import sk.iway.iwcm.components.domainRedirects.DomainRedirectDB;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/settings/domain-redirect")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_redirects')")
public class DomainRedirectsController extends DatatableRestControllerV2<DomainRedirectBean, Long> {

    @Autowired
    public DomainRedirectsController() {
        super(null);
    }

    @Override
    public Page<DomainRedirectBean> getAllItems(Pageable pageable) {
        List<DomainRedirectBean> listedBeans = DomainRedirectDB.getAllRedirects();
        return new PageImpl<>(listedBeans);
    }

    @Override
    public DomainRedirectBean getOneItem(long id) {
        return DomainRedirectDB.getRedirect((int) id);
    }

    @Override
    public DomainRedirectBean insertItem(DomainRedirectBean entity) {
        DomainRedirectDB.insert(entity);
        return entity;
    }

    @Override
    public DomainRedirectBean editItem(DomainRedirectBean entity, long id) {
        entity.setRedirectId((int)id);
        return DomainRedirectDB.update(entity);
    }

    @Override
    public boolean deleteItem(DomainRedirectBean domainRedirectBean, long id){
        DomainRedirectDB.delete((int)id);
        return true;
    }
}
