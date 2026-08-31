package sk.iway.iwcm.components.multistep_form.rest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormsService;
import sk.iway.iwcm.components.forms.FormsServiceImpl;
import sk.iway.iwcm.components.forms.RegExpRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/form-items")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_form')")
@Datatable
public class FormItemsRestController extends DatatableRestControllerV2<FormItemEntity, Long> {

    private final FormItemsRepository formItemsRepository;
    private final RegExpRepository regExpRepository;
    private final MultistepFormsService multistepFormsService;
    private final FormSettingsRepository formSettingsRepository;
    private final FormStepsRepository formStepsRepository;
    private final FormsServiceImpl formsService;

    @Autowired
    public FormItemsRestController(FormItemsRepository formItemsRepository, RegExpRepository regExpRepository, MultistepFormsService multistepFormsService, FormSettingsRepository formSettingsRepository, FormStepsRepository formStepsRepository, FormsServiceImpl formsService) {
        super(formItemsRepository);
        this.formItemsRepository = formItemsRepository;
        this.regExpRepository = regExpRepository;
        this.multistepFormsService = multistepFormsService;
        this.formSettingsRepository = formSettingsRepository;
        this.formStepsRepository = formStepsRepository;
        this.formsService = formsService;
    }

    @Override
    public Page<FormItemEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<FormItemEntity> page = new DatatablePageImpl<>(super.getAllItemsIncludeSpecSearch(new FormItemEntity(), pageable));

        //
        Integer lastStep = null;
        boolean even = false;
        for(FormItemEntity item : page.getContent()) {
            if(lastStep == null) lastStep = item.getStepId();
            else if(lastStep != item.getStepId()) {
                lastStep = item.getStepId();
                even = !even;
            }

            if(even) item.setRowClass("even-step");
            else item.setRowClass("odd-step");
        }

        page.addOptions("fieldType", MultistepFormsService.getFieldTypes(getRequest()), "label", "value", false);
        page.addOptions("hiddenFieldsByType", MultistepFormsService.getFiledTypeVisibility(getRequest()), "label", "value", false);
        page.addOptions("stepId", multistepFormsService.getFormStepsOptions(MultistepFormsService.getFormName(getRequest()), getProp()), "label", "value", false);
        page.addOptions("regexValidationArr", MultistepFormsService.getRegExOptions(regExpRepository, getRequest()), "label", "value", false);

        processFromEntity(page, ProcessItemAction.GETALL);

        return page;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<FormItemEntity> root, CriteriaBuilder builder) {

        super.addSpecSearch(params, predicates, root, builder);
        predicates.add(builder.equal(root.get("formName"), MultistepFormsService.getFormName(params)));

        String stepId = params.get("stepId");
        if(Tools.isNotEmpty(stepId)) {
            predicates.add(builder.equal(root.get("stepId"), Tools.getIntValue(stepId, -1)));
        }
    }

    @Override
    public Pageable addSpecSort(Map<String, String> params, Pageable pageable) {
         //remove default sort
        if(params.containsKey("sort"))
            params.remove("sort");

        Pageable pageableNew = PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(
                Sort.Order.asc("stepId"),
                Sort.Order.asc("sortPriority")
            )
        );

        return pageableNew;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, FormItemEntity> target, Identity user, Errors errors, Long id, FormItemEntity entity) {
        super.validateEditor(request, target, user, errors, id, entity);

        //
        if(Tools.isEmpty(entity.getFormName()) || entity.getStepId() == null || entity.getStepId() < 1)
            throw new IllegalStateException(getProp().getText("datatable.error.unknown"));

        //
        boolean isRowView = Tools.isTrue(formSettingsRepository.isRowView(entity.getFormName(), CloudToolsForCore.getDomainId()) );
        if(isRowView == false && MultistepFormsService.getRowViewItemTypes().contains(entity.getFieldType()))
            throw new IllegalStateException(getProp().getText("components.form_items.formIsNotRowView"));
    }

    @Override
    public FormItemEntity getOneItem(long id) {
        FormItemEntity entity;
        if(id == -1) {
            entity = new FormItemEntity();
            entity.setFormName(MultistepFormsService.getFormName(getRequest()));

            int stepId = Tools.getIntValue(getRequest().getParameter("stepId"), -1);
            if(stepId != -1) entity.setStepId(stepId);
        } else {
            entity = formItemsRepository.getById(id);
        }

        return processFromEntity(entity, ProcessItemAction.GETONE);
    }

    @Override
    public void beforeSave(FormItemEntity entity) {
        if("captcha".equalsIgnoreCase(entity.getFieldType()))
            entity.setRequired(true); //captcha is allways required

        StringBuilder sb = new StringBuilder("");
        for(Integer regexId : entity.getRegexValidationArr()) sb.append(regexId).append("+");
        entity.setRegexValidation(sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "");

        //Prepare itemFormId - if insert OR always for radio (because you can change the field label)
        if(entity.getId() == null || entity.getId() < 1 || "radio".equals(entity.getFieldType())) {
            //Set itemFormId
            String itemFormId = multistepFormsService.getValidItemFormId(entity); // generate unique
            entity.setItemFormId(itemFormId);
        }
    }

    @Override
    public boolean checkItemPerms(FormItemEntity entity, Long id) {
        if(entity == null || Tools.isEmpty(entity.getFormName()) || entity.getStepId() == null) return false;

        int domainId = CloudToolsForCore.getDomainId();
        if(entity.getDomainId() != null && entity.getDomainId().intValue() != domainId) return false;

        String formName = entity.getFormName();
        if(id != null && id.longValue() > 0) {
            FormItemEntity stored = formItemsRepository.findFirstByIdAndDomainId(id, domainId).orElse(null);
            if(stored == null || Objects.equals(stored.getFormName(), formName) == false) return false;
            formName = stored.getFormName();
        }

        boolean validStep = formStepsRepository.getValidStep(formName, entity.getStepId().longValue(), domainId).isPresent();
        return validStep && getUser() != null && formsService.isFormAccessible(formName, getUser());
    }

    @Override
    public void afterSave(FormItemEntity entity, FormItemEntity saved) {
        // After save ensure that form pattern is updated
        multistepFormsService.updateFormPattern(entity.getFormName());
    }

    @Override
    public void afterDelete(FormItemEntity entity, long id) {
        // After save ensure that form pattern is updated
        multistepFormsService.updateFormPattern(entity.getFormName());
    }

    private void setItemPreview(FormItemEntity stepItem) {
        JSONObject item = new JSONObject(stepItem);
        String fieldType = item.getString("fieldType");

        item.put("labelOriginal", stepItem.getLabel());
        if (Tools.isEmpty(stepItem.getLabel())) {
            item.put("label", getProp().getText("components.formsimple.label." + fieldType));
        }

        String itemHtml = FormsService.replaceFields(getProp().getText("components.formsimple.input." + fieldType), stepItem.getFormName(), "", item, getProp().getText("components.formsimple.requiredLabelAdd"), false, false, new HashSet<>(), getProp(), getRequest());

        if(itemHtml.contains("!INCLUDE")) {
            itemHtml = EditorToolsForCore.renderIncludes(itemHtml, false, getRequest());
        }

        stepItem.setGeneratedItem(itemHtml);
    }

    @Override
    public FormItemEntity processFromEntity(FormItemEntity entity, ProcessItemAction action) {

        if(ProcessItemAction.GETALL.equals(action))
            setItemPreview(entity);

        String generatedTitle = "";
        if (Tools.isNotEmpty(entity.getLabel())) generatedTitle = entity.getLabel();
        else generatedTitle = getProp().getText("components.formsimple.label." + entity.getFieldType());

        generatedTitle = Tools.html2text(generatedTitle);
        entity.setGeneratedTitle(generatedTitle);

        if(ProcessItemAction.GETONE.equals(action))
            entity.setRegexValidationArr( Tools.getTokensInteger(entity.getRegexValidation(), "+") );

        return entity;
    }

    @Override
    protected boolean checkRowReorderScope(HttpServletRequest request, List<FormItemEntity> entities) {
        String formName = MultistepFormsService.getFormName(request);
        int stepId = Tools.getIntValue(request.getParameter("stepId"), -1);
        if(Tools.isEmpty(formName) || stepId < 1 || entities == null || entities.isEmpty() || getUser() == null) return false;

        int domainId = CloudToolsForCore.getDomainId();
        for(FormItemEntity entity : entities) {
            if(entity == null || Objects.equals(formName, entity.getFormName()) == false ||
                entity.getStepId() == null || entity.getStepId().intValue() != stepId ||
                entity.getDomainId() == null || entity.getDomainId().intValue() != domainId) return false;
        }

        if(formStepsRepository.getValidStep(formName, Long.valueOf(stepId), domainId).isEmpty()) return false;
        return formsService.isFormAccessible(formName, getUser());
    }

    @GetMapping("/default-regex")
    public List<Long> getDefaultRegex(@RequestParam("fieldType") String fieldType) {
        List<Long> regexIds = new ArrayList<>();
        if(Tools.isEmpty(fieldType)) return regexIds;

        String ITEM_KEY_INPUT_PREFIX = "components.formsimple.input.";
        String fieldHtml = getProp().getText(ITEM_KEY_INPUT_PREFIX + fieldType);
        if(Tools.isEmpty(fieldHtml)) return regexIds;

        Pattern classesPattern = Pattern.compile("<input\\s*class=\"([^\"]*)\"", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
		Matcher classesMatcher = classesPattern.matcher(fieldHtml);

        List<String> inputClasses = new ArrayList<>();
        while(classesMatcher.find()) {
            String classAttr = classesMatcher.group(1);
            classAttr = Tools.replace(classAttr, "${classes}", "");
            String[] classes = classAttr.split("\\s+");
            for(String cls : classes) {
                if(!inputClasses.contains(cls) && "form-control".equalsIgnoreCase(cls) == false)
                    inputClasses.add(cls);
            }
        }

        if(inputClasses == null || inputClasses.size() < 1) return regexIds;
        else return regExpRepository.findRegexIdsByTypeIn(inputClasses);
    }
}
