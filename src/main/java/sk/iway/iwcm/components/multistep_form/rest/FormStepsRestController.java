package sk.iway.iwcm.components.multistep_form.rest;

import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepEntity;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.RowReorderDto;

@RestController
@RequestMapping("/admin/rest/form-steps")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_form')")
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
        // After save ensure that form pattern is updated, and all step positions
        // !! do not call, when action was duplication
        if(isDuplicate() == false) {
            multistepFormsService.updateFormPattern(entity.getFormName());
            multistepFormsService.updateStepsPositions(entity.getFormName());
        }
    }

    @Override
    public void beforeSave(FormStepEntity entity) {
        if(entity.getDomainId() == null) entity.setDomainId(CloudToolsForCore.getDomainId());
        // We set default values 1/1 but after save action will replace this values
        entity.setCurrentPosition(1);
        entity.setMaxPosition(1);
    }

    @Override
    public void afterDelete(FormStepEntity entity, long id) {
        // After delete remove all step items binded to this form step
        formItemsRepository.deleteAllByStepIdAndDomainId(id, CloudToolsForCore.getDomainId());

        // Now update form pattern
        multistepFormsService.updateFormPattern(entity.getFormName());

        // Now update steps positions
        multistepFormsService.updateStepsPositions(entity.getFormName());
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

        // Now update step positions
        multistepFormsService.updateStepsPositions(entity.getFormName());
    }

    @GetMapping(value="/get-step", params={"form-name", "step-id"}, produces = MediaType.TEXT_HTML)
    public ResponseEntity<String> getFormStepHtml(@RequestParam("form-name") String formName, @RequestParam("step-id") Long stepId, HttpServletRequest request) {
        String encoding = SetCharacterEncodingFilter.getEncoding();
        if (Tools.isEmpty(encoding)) encoding = "UTF-8"; // Fallback
        String contentTypeWithCharset = MediaType.TEXT_HTML + "; charset=" + encoding;

        try {
            FormHtmlHandler formHtmlHandler = new FormHtmlHandler(formName, request);
            return ResponseEntity.ok()
                .header("Content-Type", contentTypeWithCharset)
                .body( formHtmlHandler.getFormStepHtml(stepId, request) );
        } catch (Exception e) {
            Logger.error(FormStepsRestController.class, "getFormStepHtml() failed. " + e.getLocalizedMessage());

            return ResponseEntity.badRequest()
                .header("Content-Type", contentTypeWithCharset)
                .body("");
        }
    }

    @Override
    @PostMapping(value = "/row-reorder", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> rowReorder(HttpServletRequest request, @RequestBody RowReorderDto rowReorderDto) {
        // call super row reorder to update positions
        ResponseEntity<Boolean> response = super.rowReorder(request, rowReorderDto);

        if(response.getStatusCode().is2xxSuccessful() && response.getBody() == Boolean.TRUE) {
            // All good, now update steps positions in form
            multistepFormsService.updateStepsPositions(rowReorderDto);
        }

        return response;
    }
}