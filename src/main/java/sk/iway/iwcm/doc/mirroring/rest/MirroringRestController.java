package sk.iway.iwcm.doc.mirroring.rest;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.mirroring.dto.MirroringDTO;
import sk.iway.iwcm.doc.mirroring.jpa.MirroringEditorFields;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/webpages/mirroring")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_mirroring')")
@Datatable
public class MirroringRestController extends DatatableRestControllerV2<MirroringDTO, Long> {

    private MirroringService mirroringService;

    @Autowired
    public MirroringRestController(MirroringService mirroringService) {
        super(null);
        this.mirroringService = mirroringService;
    }

    @Override
    public Page<MirroringDTO> getAllItems(Pageable pageable) {
        DatatablePageImpl<MirroringDTO> page = new DatatablePageImpl<>( mirroringService.getPage(pageable, getRequest()) );
        processFromEntity(page, ProcessItemAction.GETALL);
        page.addOptions("editorFields.statusIcons", mirroringService.getStatusIconOptions(getProp()), "label", "value", false);
        return page;
    }

    @Override
    public Page<MirroringDTO> findByColumns(Map<String, String> params, Pageable pageable, MirroringDTO search) {
        Page<MirroringDTO> page = mirroringService.getFilteredPage(pageable, getRequest());
        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public MirroringDTO insertItem(MirroringDTO entity) {
        throwError(getProp().getText("config.not_permitted_action_err"));
        return null;
    }

    @Override
    public MirroringDTO editItem(MirroringDTO entity, long id) {
        MirroringDTO result = mirroringService.editItem(entity, getRequest());

        if(result == null)
            addNotify(new NotifyBean(getErrTitle(), getErrText(), NotifyType.ERROR, 15000));

        //Because edit can delete entity (when all connections are removed)
        setForceReload(true);
        return result;
    }

    @Override
    public MirroringDTO getOneItem(long syncId) {
        MirroringDTO entity = mirroringService.getOneItem((int) syncId, getRequest());

        if(entity == null)
            addNotify(new NotifyBean(getErrTitle(), getErrText(), NotifyType.ERROR, 15000));

        // DO NOT call process from entity
        return entity;
    }

    @Override
    public boolean deleteItem(MirroringDTO entity, long syncId) {
        boolean result = mirroringService.deleteItem((int) syncId, getRequest());

        if(result == false)
            addNotify(new NotifyBean(getErrTitle(), getErrText(), NotifyType.ERROR, 15000));

        return true;
    }

    @Override
    public MirroringDTO processFromEntity(MirroringDTO entity, ProcessItemAction action, int rowCount) {
       if(entity == null) return entity;

       MirroringEditorFields mef = new MirroringEditorFields();
       mef.from(entity);

       if(rowCount == 1)
            mef.setFieldsDefinition( mirroringService.getFields() );

        entity.setEditorFields(mef);
        return entity;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, MirroringDTO> target, Identity user, Errors errors, Long id, MirroringDTO entity) {
        mirroringService.validateEditor(entity, errors, getRequest());
    }

    private String getErrTitle() {
        return getProp().getText("datatables.error.title.js");
    }

    private String getErrText() {
        return getProp().getText("datatable.error.unknown");
    }
}