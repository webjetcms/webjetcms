package sk.iway.iwcm.dmail.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.dmail.DomainLimitBean;
import sk.iway.iwcm.dmail.DomainLimitsDB;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/dmail/domain-limits")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_dmail_domainlimits')")
@Datatable
public class DomainLimitRestController extends DatatableRestControllerV2<DomainLimitBean, Long>{

    @Autowired
    public DomainLimitRestController() {
        super(null);
    }

    @Override
    public Page<DomainLimitBean> getAllItems(Pageable pageable) {

        DomainLimitsDB db = DomainLimitsDB.getInstance();

        List<DomainLimitBean> items =  db.getAll();

        DatatablePageImpl<DomainLimitBean> page = new DatatablePageImpl<>(items);
        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public DomainLimitBean insertItem(DomainLimitBean entity) {

        if(entity.save()) {
            return entity;
        } else {
            throwError("");
            return null;
        }
    }

    @Override
    public DomainLimitBean getOneItem(long id) {
        DomainLimitsDB db = DomainLimitsDB.getInstance();

        DomainLimitBean bean = db.getById((int) id);
        if (bean == null) bean = new DomainLimitBean();

        return bean;
    }

    @Override
    public DomainLimitBean editItem(DomainLimitBean entity, long id) {

        if(entity.save()) {
            return entity;
        } else {
            throwError("");
            return null;
        }
    }

    @Override
    public boolean deleteItem(DomainLimitBean entity, long id) {
       return entity.delete();
    }

    @Override
    public DomainLimitBean processFromEntity(DomainLimitBean entity, ProcessItemAction action) {
        if (entity.isActive()==false) {
            BaseEditorFields bef = new BaseEditorFields();
            bef.addRowClass("is-disabled");
            entity.setEditorFields(bef);
        }
        return entity;
    }

}
