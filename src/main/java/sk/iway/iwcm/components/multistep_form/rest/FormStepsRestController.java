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

    @Autowired
    public FormStepsRestController(FormStepsRepository formStepsRepository, FormItemsRepository formItemsRepository) {
        super(formStepsRepository);
        this.formStepsRepository = formStepsRepository;
        this.formItemsRepository = formItemsRepository;
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
        return entity;
    }

    // @Override
    // public void beforeSave(FormStepEntity entity) {
    //     String formName = MultistepFormsService.getFormName(getRequest());
    //     if(Tools.isEmpty(formName)) throw new IllegalStateException("FORM NAME MUST BE PROVIDED");
    //     entity.setFormName(formName);
    //     super.beforeSave(entity);
    // }

    // @Override
    // public void afterDelete(FormStepEntity entity, long id) {
    //     // After delete remove all items bbind to this form step
    //     formItemsRepository.deleteAllByStepIdAndDomainId(id, CloudToolsForCore.getDomainId());
    // }

    // @Override
    // public void beforeDuplicate(FormStepEntity entity) {
    //     UserDetails currentUser = getUser();
    //     //IF something went wrong, delete all awaiting duplicate
    //     formItemsRepository.deleteAllByFormNameAndStepIdAndDomainId(entity.getFormName(), Long.valueOf(-currentUser.getUserId()), CloudToolsForCore.getDomainId());

    //     //Now insert new items that gonna be set after duplicate - stepId gonna be -currentUserId
    //     List<FormItemEntity> stepItems = formItemsRepository.findAllByStepIdAndDomainIdOrderBySortPriorityAsc(entity.getId(), CloudToolsForCore.getDomainId());
    //     for(FormItemEntity stepItem : stepItems) {
    //         stepItem.setId(null);
    //         stepItem.setStepId(-currentUser.getUserId());
    //     }
    //     formItemsRepository.saveAll(stepItems);
    // }

    @Override
    public void afterDuplicate(FormStepEntity entity, Long originalId) {
        // Find all items taht are awaiting step duplicate to have id, and set id
        List<FormItemEntity> stepItems = formItemsRepository.findAllByStepIdAndDomainIdOrderBySortPriorityAsc(Long.valueOf(-getUser().getUserId()), CloudToolsForCore.getDomainId());
        for(FormItemEntity stepItem : stepItems) stepItem.setStepId(entity.getId().intValue());
        formItemsRepository.saveAll(stepItems);
    }
}
