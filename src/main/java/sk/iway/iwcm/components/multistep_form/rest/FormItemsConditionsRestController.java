package sk.iway.iwcm.components.multistep_form.rest;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import  sk.iway.iwcm.components.multistep_form.jpa.ConditionType;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsConditionsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.JoinOperatorType;
import sk.iway.iwcm.components.multistep_form.jpa.OperatorType;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/form-items-conditions")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_form')")
@Datatable
public class FormItemsConditionsRestController extends DatatableRestControllerV2<FormItemsConditionEntity, Long> {

    private final MultistepFormsService multistepFormsService;
    private final FormItemsRepository formItemsRepository;

    @Autowired
    public FormItemsConditionsRestController(FormItemsConditionsRepository formItemsConditionsRepository, MultistepFormsService multistepFormsService, FormItemsRepository formItemsRepository) {
        super(formItemsConditionsRepository);
        this.multistepFormsService = multistepFormsService;
        this.formItemsRepository = formItemsRepository;
    }

    @Override
    public Page<FormItemsConditionEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<FormItemsConditionEntity> page = new DatatablePageImpl<>( super.getAllItemsIncludeSpecSearch(new FormItemsConditionEntity(), pageable) );

        String formName = Tools.getStringValue(getRequest().getParameter("formName"), null);
        int stepId = Tools.getIntValue(getRequest().getParameter("stepId"), -1);

        page.addOptions("itemFormId", multistepFormsService.getAvailableConditionFields(formName, stepId, getProp()), "label", "value", true);
        page.addOptions("operator", OperatorType.getLabelValues(getProp()), "label", "value", true);
        page.addOptions("joinOperatorType", JoinOperatorType.getLabelValues(), "label", "value", true);

        return page;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<FormItemsConditionEntity> root, CriteriaBuilder builder) {
        int itemId = Tools.getIntValue(params.get("itemId"), -1);
        predicates.add(builder.equal(root.get("formItemId"), itemId));

        ConditionType conditionType = ConditionType.fromString( getRequest().getParameter("conditionType") );
        if(conditionType == null) {
            // Return no results for missing or invalid condition type.
            predicates.add(builder.disjunction());
        } else {
            predicates.add(builder.equal(root.get("conditionType"), conditionType));
        }

        predicates.add(builder.equal(root.get("domainId"), CloudToolsForCore.getDomainId()));
    }

    @Override
    public Pageable addSpecSort(Map<String, String> params, Pageable pageable) {
        // Remove client sort to keep condition rows ordered by sortPriority.
        params.remove("sort");

        return PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Order.asc("sortPriority"))
        );
    }

    @Override
    public void beforeSave(FormItemsConditionEntity entity) {
        int itemId = Tools.getIntValue(getRequest().getParameter("itemId"), -1);
        if(itemId < 1) throw new IllegalArgumentException("itemId is required and must be a positive integer.");

        ConditionType conditionType = ConditionType.fromString( getRequest().getParameter("conditionType") );
        if(conditionType == null) throw new IllegalArgumentException("conditionType was empty or incorrect.");

        String formName = Tools.getStringValue(getRequest().getParameter("formName"), null);
        if(Tools.isEmpty(formName)) throw new IllegalArgumentException("formName is required.");

        entity.setFormItemId((long) itemId);
        entity.setConditionType(conditionType);
        entity.setFormName(formName);
        entity.setDomainId(CloudToolsForCore.getDomainId());
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, FormItemsConditionEntity> target, Identity user, Errors errors, Long id, FormItemsConditionEntity entity) {
        // Check if value is required - if NOT, just set empty like "-"
        if(entity.getOperator() != null && entity.getOperator().needsValue() == false) {
            // value can be empty but DB requires some value
            entity.setValue("-");
        }

        // Check if conditions is not pointing on itself (item)
        int itemId = Tools.getIntValue(getRequest().getParameter("itemId"), -1);
        String itemFormId = entity.getItemFormId();
        String formName = Tools.getStringValue(getRequest().getParameter("formName"), null);

        if(itemId > 0 && Tools.isNotEmpty(itemFormId)) {
            formItemsRepository.countItemsByIdAndItemFormId(formName, Long.valueOf(itemId), itemFormId, CloudToolsForCore.getDomainId()).ifPresent(count -> {
                if(count > 0) errors.rejectValue("errorField.itemFormId", "", getProp().getText("components.form_items_condition.cant_point_to_self_err"));
            });
        }

        // Run default validator
        super.validateEditor(request, target, user, errors, id, entity);
    }

    @Override
    public FormItemsConditionEntity processFromEntity(FormItemsConditionEntity entity, ProcessItemAction action) {
        if(entity != null && entity.getOperator() != null && entity.getOperator().needsValue() == false)
            entity.setValue("");

        return entity;
    }

    @Override
    public void afterSave(FormItemsConditionEntity entity, FormItemsConditionEntity saved) {
        if (saved != null) {
            FormConditionsHandler.clearConditionsCache(saved.getFormItemId(), saved.getDomainId());
        }
    }

    @Override
    public void afterDelete(FormItemsConditionEntity entity, long id) {
        if (entity != null) {
            FormConditionsHandler.clearConditionsCache(entity.getFormItemId(), entity.getDomainId());
        }
    }
}
