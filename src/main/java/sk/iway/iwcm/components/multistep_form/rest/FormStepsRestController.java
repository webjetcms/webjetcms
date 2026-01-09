package sk.iway.iwcm.components.multistep_form.rest;

import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/form-steps")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuInquiry')")
@Datatable
public class FormStepsRestController extends DatatableRestControllerV2<FormStepEntity, Long> {

    private final FormStepsRepository formStepsRepository;
    private final FormItemsRepository formItemsRepository;

    private final MultistepFormsService multistepFormsService;

    @Autowired
    public FormStepsRestController(FormStepsRepository formStepsRepository, FormItemsRepository formItemsRepository, MultistepFormsService multistepFormsService) {
        super(formStepsRepository);
        this.formStepsRepository = formStepsRepository;
        this.formItemsRepository = formItemsRepository;
        this.multistepFormsService = multistepFormsService;
    }

    @Override
    public Page<FormStepEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<FormStepEntity> page = new DatatablePageImpl<>(super.getAllItemsIncludeSpecSearch(new FormStepEntity(), pageable));
        return page;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<FormStepEntity> root, CriteriaBuilder builder) {
        super.addSpecSearch(params, predicates, root, builder);
        predicates.add(builder.equal(root.get("formName"), MultistepFormsService.getFormName(params)));
    }

    @Override
    public FormStepEntity getOneItem(long id) {
        FormStepEntity entity = (id == -1) ? new FormStepEntity() : formStepsRepository.getById(id);
        if(id < 1) entity.setFormName(MultistepFormsService.getFormName(getRequest()));
        // Copy value so it can be used during duplicate action
        entity.setIdForDuplication(entity.getId());
        return entity;
    }

    @Override
    public void afterSave(FormStepEntity entity, FormStepEntity saved) {
        // After save ensure that form pattern is updated
        // !! do not call, when action was duplication
        if(entity.getId().equals(entity.getIdForDuplication()) == false)
            multistepFormsService.updateFormPattern(entity.getFormName());
    }

    @Override
    public void beforeSave(FormStepEntity entity) {
        if(entity.getDomainId() == null) entity.setDomainId(CloudToolsForCore.getDomainId());
    }

    @Override
    public void afterDelete(FormStepEntity entity, long id) {
        // After delete remove all step items binded to this form step
        formItemsRepository.deleteAllByStepIdAndDomainId(id, CloudToolsForCore.getDomainId());
    }

    @Override
    public void beforeDuplicate(FormStepEntity entity) {
        int tmpId = -getUser().getUserId();

        //IF something went wrong, delete all awaiting duplicate
        formItemsRepository.deleteAllByFormNameAndStepIdAndDomainId(entity.getFormName(), Long.valueOf(tmpId), CloudToolsForCore.getDomainId());

        //Now insert new items that gonna be set after duplicate - stepId gonna be -currentUserId
        List<FormItemEntity> stepItemsToDuplicate = formItemsRepository.getAllStepItems(entity.getIdForDuplication(), CloudToolsForCore.getDomainId());
        for(FormItemEntity stepItem : stepItemsToDuplicate) {
            stepItem.setId(null);
            stepItem.setStepId(-getUser().getUserId());
            stepItem.setItemFormId(""); //remove itemFormId so in afterDuplicate its generated new one
        }
        formItemsRepository.saveAll(stepItemsToDuplicate);
    }

    @Override
    public void afterDuplicate(FormStepEntity entity, Long originalId) {
        int tmpId = -getUser().getUserId();

        // Find all items taht are awaiting step duplicate to have id, and set id
        for(FormItemEntity stepItem : formItemsRepository.findItemsToDuplicate(entity.getFormName(), Long.valueOf(tmpId), CloudToolsForCore.getDomainId())) {
            stepItem.setStepId(entity.getId().intValue());
            stepItem.setItemFormId( multistepFormsService.getValidItemFormId(stepItem) );
            formItemsRepository.save(stepItem);
        }

        // Now update form pattern
        multistepFormsService.updateFormPattern(entity.getFormName());
    }
}