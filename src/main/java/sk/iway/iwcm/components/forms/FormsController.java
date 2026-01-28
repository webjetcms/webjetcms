package sk.iway.iwcm.components.forms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.CryptoFactory;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.form_settings.rest.FormSettingsService;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.components.multistep_form.rest.MultistepFormsService;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.form.FormDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/forms-list")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_form')")
public class FormsController extends DatatableRestControllerV2<FormsEntity, Long> {

    private final FormsServiceImpl formsService;
    private final FormSettingsRepository formSettingsRepository;
    private final FormStepsRepository formStepsRepository;
    private final FormItemsRepository formItemsRepository;

    @Autowired
    public FormsController(FormsRepository formsRepository, FormsServiceImpl formsService, FormSettingsRepository formSettingsRepository, FormStepsRepository formStepsRepository, FormItemsRepository formItemsRepository) {
        super(formsRepository);
        this.formsService = formsService;
        this.formSettingsRepository = formSettingsRepository;
        this.formStepsRepository = formStepsRepository;
        this.formItemsRepository = formItemsRepository;
    }

    @Override
    public Page<FormsEntity> getAllItems(Pageable pageable) {
        Page<FormsEntity> page = null;
        page = formsService.getAllItems(page, pageable, getRequest(), getUser());

        if(page == null) {
            addNotify(new NotifyBean(getProp().getText("admin.operationPermissionDenied"), getProp().getText("components.forms.permsDeniedNote"), NotifyType.ERROR, 15000));
            page = new DatatablePageImpl<>(new ArrayList<>());
        }

        DatatablePageImpl<FormsEntity> pageImpl = new DatatablePageImpl<>(page);
        pageImpl.addOptions("formType", FormsService.FORM_TYPE.getSelectOptions(getProp()), "label", "value", false);

        processFromEntity(pageImpl, ProcessItemAction.GETALL);
        return pageImpl;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, FormsEntity> target, Identity user, Errors errors, Long id, FormsEntity entity) {
        if("remove".equals(target.getAction()) == false) {
            // Check if form is trying save as rowView == false but got rowView items
            String formName = entity.getFormName();
            if(Tools.isEmpty(formName)) return;

            formName = DocTools.removeChars(formName, true);
            if(Tools.isTrue(formSettingsRepository.isRowView(formName, CloudToolsForCore.getDomainId()))) {
                int count = formItemsRepository.countItemsThatHasType(formName, CloudToolsForCore.getDomainId(), MultistepFormsService.getRowViewItemTypes());
                if(count > 0) errors.rejectValue("errorField.formSettings.rowView", null, getProp().getText("components.form_items.hasRowViewFields"));
            }

            // Check that selected name is unique
            if("create".equals(target.getAction()) && formsService.isFormNameUnique(formName) == false)
                errors.rejectValue("errorField.formName", null, getProp().getText("editor.form.formName.must_be_unique_err"));
        }
    }

    @Override
    public FormsEntity insertItem(FormsEntity entity) {
        // Create form pattern record
        entity.setData("");
        entity.setUserId(Long.valueOf(-1));
        entity.setDocId(-1);
        entity.setDomainId(CloudToolsForCore.getDomainId());
        entity.setCreateDate(null);
        return super.insertItem(entity);
    }

    @Override
    public void beforeSave(FormsEntity entity) {
        entity.setFormName( DocTools.removeChars(entity.getFormName(), false) );
    }

    @Override
    public void afterSave(FormsEntity entity, FormsEntity saved) {
        if(entity.getFormSettings().getId() == null || entity.getFormSettings().getId() == -1L) {
            // Its new saved form

            // save new settings
            entity.getFormSettings().setFormName(entity.getFormName());
            entity.getFormSettings().setDomainId(CloudToolsForCore.getDomainId());
            formSettingsRepository.save(entity.getFormSettings());

            // All new forms are multistep - add dsefault first step
            FormStepEntity fse = new FormStepEntity();
            fse.setSortPriority(10);
            fse.setFormName(entity.getFormName());
            fse.setStepName("");
            fse.setStepSubName("");
            fse.setDomainId(CloudToolsForCore.getDomainId());
            formStepsRepository.save(fse);

            if ("multistep".equals(entity.getFormType())) {
                setRedirect("/apps/form/admin/form-content/?formName=" + Tools.URLEncode(saved.getFormName()));
            }
        }
    }

    @Override
    public FormsEntity getOneItem(long id) {
        FormsEntity entity = formsService.getById(id);
        if (entity == null) {
            entity = new FormsEntity();
            entity.setFormType( FormsService.FORM_TYPE.MULTISTEP.value() );
            return entity;
        }

        if (formsService.isFormAccessible(entity.getFormName(), getUser())) {
            int domainId = CloudToolsForCore.getDomainId();
            formsService.prepareForm(entity, domainId);

            if(formsService.getFormName(getRequest()) == null) {
                // No formName set in request, its from-list -> add info about fomr settings
                FormSettingsEntity formSettings = formSettingsRepository.findByFormNameAndDomainId(DocTools.removeChars(entity.getFormName(), false), domainId);
                FormSettingsService.prepareSettingsForEdit(formSettings);
                entity.setFormSettings(formSettings);
            }

            return processFromEntity(entity, ProcessItemAction.GETONE);
        }

        return null;
    }

    @Override
    public Page<FormsEntity> findByColumns(Map<String, String> params, Pageable pageable, FormsEntity search) {
        Page<FormsEntity> page = formsService.findByColumns(params, pageable, search, getRequest(), getUser());
        if(page != null) return page;
        return super.findByColumns(params, pageable, search);
    }

    @Override
    public FormsEntity editItem(FormsEntity entity, long id) {
        String formName = formsService.getFormName(getRequest());
        if(formName != null && entity.getCreateDate() != null) {
            // Edit of form detail, we can change ONLY note
            String note = entity.getNote();
            formsService.updateNote(note, id);
            return formsService.getById(id);
        } else if(formName == null) {
            // We are editing FORM (main form format) we can change only form_settings not form itself
            entity.getFormSettings().setFormName( DocTools.removeChars(entity.getFormName(), false) );
            FormSettingsService.prepareSettingsForSave(entity.getFormSettings(), formSettingsRepository);
            formSettingsRepository.save(entity.getFormSettings());
            return formsService.getById(id);
        }
        return null;
    }

    @Override
    public boolean deleteItem(FormsEntity entity, long id) {
        return formsService.deleteItem(entity, id, formStepsRepository, formItemsRepository);
    }

    @Override
    public boolean processAction(FormsEntity entity, String action) {
        String formName = entity.getFormName();
        if ("archiveForm".equals(action)) {
            boolean success = FormDB.setFormName(formName, "Archiv-"+formName);
            Adminlog.add(Adminlog.TYPE_FORM_ARCHIVE, "Archivacia formularu: "+formName, -1, -1);
            return success;
        } else if ("archiveFormDetail".equals(action) && entity.getId()!=null) {
            String idsQuery = String.valueOf(entity.getId());
            int smallestId = entity.getId().intValue();
            boolean success = FormDB.setFormName(formName, "Archiv-"+formName, (" AND id IN (" + idsQuery + ")"), smallestId, false);
			Adminlog.add(Adminlog.TYPE_FORM_ARCHIVE, "Archivacia formularu: "+formName, -1, -1);
            return success;
        }
        return false;
    }

    @Override
    public boolean checkItemPerms(FormsEntity entity, Long id) {
        if (InitServlet.isTypeCloud()) {
            if (entity.getDomainId()!=CloudToolsForCore.getDomainId()) return false;
            FormsEntity old = getRepo().getById(entity.getId());
            if (old != null && old.getDomainId()!=CloudToolsForCore.getDomainId()) return false;
        }
        return true;
    }

    @GetMapping(path = "/columns/{formName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public FormColumns getColumnNames(@PathVariable String formName) {
        return formsService.getColumnNames(formName, getUser(), getProp());
    }

    /**
     * Get all regular expressions.
     * Available to all admins (it's used on variety of apps like webpages, news, etc)
     */
    @GetMapping(path="/regexps")
    @PreAuthorize(value = "@WebjetSecurityService.isAdmin()")
    public List<LabelValue> getAllRegularExpression() {
        Prop prop = getProp();
        List<LabelValue> regexps = new ArrayList<>();
        List<String[]> all = FormDB.getInstance().getAllRegularExpression();
        for (String[] regexp : all) {
            regexps.add(new LabelValue(prop.getText(regexp[0]), regexp[1]));
        }
        return regexps;
    }

    @GetMapping(path="/html/")
    public String getHtml(@RequestParam long id) {
        FormsEntity entity = formsService.getById(id);
        if (entity == null || Tools.isEmpty(entity.getFormName())) return null;

        if (formsService.isFormAccessible(entity.getFormName(), getUser())==false) return null;

        //html kod necitame v entite, musime ziskat takto
        String html = (new SimpleQuery()).forString("SELECT html FROM forms WHERE id=?", id);
        html = CryptoFactory.decrypt(html);
        if (html.contains("<body")==false) {
            html = Tools.replace(html, "\n", "\n<br/>");
        }
        return html;
    }

    @Override
    public FormsEntity processFromEntity(FormsEntity entity, ProcessItemAction action) {
        if(Tools.isEmpty(entity.getFormType()))
             entity.setFormType( FormsService.FORM_TYPE.UNKNOWN.value() );
        return entity;
    }
}