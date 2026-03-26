package sk.iway.iwcm.components.domain_redirects;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.domainRedirects.DomainRedirectBean;
import sk.iway.iwcm.components.domainRedirects.DomainRedirectDB;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRequest;
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
        List<DomainRedirectBean> listedBeans;
        if (InitServlet.isTypeCloud()) listedBeans = DomainRedirectDB.getRedirectByDestDomain(CloudToolsForCore.getDomainName());
        else listedBeans = DomainRedirectDB.getAllRedirects();
        return new sk.iway.iwcm.system.datatable.DatatablePageImpl<>(listedBeans);
    }

    @Override
    public DomainRedirectBean getOneItem(long id) {
        if (id < 1) {
            DomainRedirectBean bean = new DomainRedirectBean();
            bean.setRedirectTo(CloudToolsForCore.getDomainName());
            return bean;
        }
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

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, DomainRedirectBean> target,
            Identity user, Errors errors, Long id, DomainRedirectBean entity) {

        isDomainValid(entity, errors);
    }

    @Override
    public boolean beforeDelete(DomainRedirectBean entity) {
        return isDomainValid(entity, null);
    }

    @Override
    public void beforeSave(DomainRedirectBean entity) {
        if (isDomainValid(entity, null)==false) throwConstraintViolation(getProp().getText("components.file_archiv.file_rename.nemate_pravo_na_tuto_editaciu"));
    }

    private boolean isDomainValid(DomainRedirectBean entity, Errors errors) {
        if (InitServlet.isTypeCloud()==false) return true;

        boolean isValid = true;
        if (entity.getRedirectTo().contains(CloudToolsForCore.getDomainName())==false)
        {
            isValid = false;
        }

        //check existing row in database for domainName
        if (entity.getRedirectId()!=null && entity.getRedirectId().intValue()>0) {
            String currentDomain = (new SimpleQuery()).forString("SELECT redirect_to FROM domain_redirects WHERE redirect_id=?", entity.getRedirectId());
            if (currentDomain.equals(CloudToolsForCore.getDomainName())==false) {
                isValid = false;
            }
        }

        if (isValid==false && errors != null) errors.rejectValue("errorField.redirectTo", "403", Prop.getInstance().getText("components.file_archiv.file_rename.nemate_pravo_na_tuto_editaciu"));

        return isValid;
    }

    @Override
    public boolean checkItemPerms(DomainRedirectBean entity, Long id) {
        if (InitServlet.isTypeCloud()) {
            return isDomainValid(entity, null);
        }
        return true;
    }

}
