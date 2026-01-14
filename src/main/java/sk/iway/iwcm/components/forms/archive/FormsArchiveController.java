package sk.iway.iwcm.components.forms.archive;

import java.util.ArrayList;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormColumns;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/forms/archive-list")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_form')")
public class FormsArchiveController extends DatatableRestControllerV2<FormsArchiveEntity, Long> {

    private final FormsArchiveServiceImpl formsService;
    private final FormSettingsRepository formSettingsRepository;
    private final FormStepsRepository formStepsRepository;
    private final FormItemsRepository formItemsRepository;

    @Autowired
    public FormsArchiveController(FormsArchiveRepository formsRepository, FormsArchiveServiceImpl formsService, FormSettingsRepository formSettingsRepository, FormStepsRepository formStepsRepository, FormItemsRepository formItemsRepository) {
        super(formsRepository);
        this.formsService = formsService;
        this.formSettingsRepository = formSettingsRepository;
        this.formStepsRepository = formStepsRepository;
        this.formItemsRepository = formItemsRepository;
    }

    @Override
    public Page<FormsArchiveEntity> getAllItems(Pageable pageable) {
        Page<FormsArchiveEntity> page = null;
        page = formsService.getAllItems(page, pageable, getRequest(), getUser());

        if(page == null) {
            addNotify(new NotifyBean(getProp().getText("admin.operationPermissionDenied"), getProp().getText("components.forms.permsDeniedNote"), NotifyType.ERROR, 15000));
            return new DatatablePageImpl<>(new ArrayList<>());
        }

        return page;
    }

    @GetMapping(path = "/columns/{formName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public FormColumns getColumnNames(@PathVariable String formName) {
        return formsService.getColumnNames(formName, getUser(), formSettingsRepository);
    }

    @Override
    public Page<FormsArchiveEntity> findByColumns(Map<String, String> params, Pageable pageable, FormsArchiveEntity search) {
        Page<FormsArchiveEntity> page = formsService.findByColumns(params, pageable, search, getRequest(), getUser());
        if(page != null) return page;
        return super.findByColumns(params, pageable, search);
    }

    @GetMapping(path="/html/")
    public String getHtml(@RequestParam long id) {
        FormsArchiveEntity entity = formsService.getById(id);
        if (entity == null || Tools.isEmpty(entity.getFormName())) return null;

        if (formsService.isFormAccessible(entity.getFormName(), getUser())==false) return null;

        //html kod necitame v entite, musime ziskat takto
        String html = (new SimpleQuery()).forString("SELECT html FROM forms_archive WHERE id=?", id);
        return html;
    }

    @Override
    public FormsArchiveEntity editItem(FormsArchiveEntity entity, long id) {
        String note = entity.getNote();
        formsService.updateNote(note, id);
        return formsService.getById(id);
    }

    @Override
    public boolean deleteItem(FormsArchiveEntity entity, long id) {
        return formsService.deleteItem(entity, id, formStepsRepository, formItemsRepository);
    }

    @Override
    public boolean checkItemPerms(FormsArchiveEntity entity, Long id) {
        if (InitServlet.isTypeCloud()) {
            if (entity.getDomainId()!=CloudToolsForCore.getDomainId()) return false;
            FormsArchiveEntity old = getRepo().getById(entity.getId());
            if (old != null && old.getDomainId()!=CloudToolsForCore.getDomainId()) return false;
        }
        return true;
    }
}
