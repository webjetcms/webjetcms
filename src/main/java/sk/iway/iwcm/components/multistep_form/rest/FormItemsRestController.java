package sk.iway.iwcm.components.multistep_form.rest;

import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.components.forms.FormsService;
import sk.iway.iwcm.components.forms.RegExpRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/form-items")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuInquiry')")
@Datatable
public class FormItemsRestController extends DatatableRestControllerV2<FormItemEntity, Long> {

    private final FormItemsRepository formItemsRepository;
    private final RegExpRepository regExpRepository;

    private final MultistepFormsService multistepFormsService;

    @Autowired
    public FormItemsRestController(FormItemsRepository formItemsRepository, RegExpRepository regExpRepository, MultistepFormsService multistepFormsService) {
        super(formItemsRepository);
        this.formItemsRepository = formItemsRepository;
        this.regExpRepository = regExpRepository;
        this.multistepFormsService = multistepFormsService;
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
        page.addOptions("stepId", multistepFormsService.getFormStepsOptions(MultistepFormsService.getFormName(getRequest())), "label", "value", false);
        page.addOptions("regexValidationArr", MultistepFormsService.getRegExOptions(regExpRepository, getRequest()), "label", "value", false);

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

        if(Tools.isEmpty(entity.getFormName()))
            throw new IllegalStateException("NO FORM NAME");

        if(entity.getStepId() == null || entity.getStepId() < 1)
            throw new IllegalStateException("IVALID stepId");
    }

    @Override
    public FormItemEntity getOneItem(long id) {
        FormItemEntity entity;
        if(id == -1) {
            entity = new FormItemEntity();
            entity.setFormName(MultistepFormsService.getFormName(getRequest()));

            int stepId = Tools.getIntValue(getRequest().getParameter("stepId"), -1);
            if(stepId != -1) entity.setStepId(stepId);
        } else entity = formItemsRepository.getById(id);

        entity.setRegexValidationArr( Tools.getTokensInteger(entity.getRegexValidation(), "+") );

        return entity;
    }

    @Override
    public void beforeSave(FormItemEntity entity) {
        if("captcha".equalsIgnoreCase(entity.getFieldType()))
            entity.setRequired(true); //captcha is allways required

        StringBuilder sb = new StringBuilder("");
        for(Integer regexId : entity.getRegexValidationArr()) sb.append(regexId).append("+");
        entity.setRegexValidation(sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "");

        //Prepare itemFormId - if insert
        if(entity.getId() == null || entity.getId() < 1) {
            //Set itemFormId
            String itemFormId = multistepFormsService.getValidItemFormId(entity); // generate unique
            entity.setItemFormId(itemFormId);
        }
    }

    @Override
    public void afterSave(FormItemEntity entity, FormItemEntity saved) {
        // After save ensure that form pattern is updated
        multistepFormsService.updateFormPattern(entity.getFormName());
    }

    @GetMapping("/field-preview")
    public String kokos(FormItemEntity fie) {
        return EditorToolsForCore.renderIncludes(FormsService.getFieldHtml(fie, getProp()), false, getRequest());
    }
}