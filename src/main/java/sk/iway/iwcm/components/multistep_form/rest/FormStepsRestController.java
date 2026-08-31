package sk.iway.iwcm.components.multistep_form.rest;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.forms.FormsServiceImpl;
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
    private final FormsServiceImpl formsService;

    @Autowired
    public FormStepsRestController(FormStepsRepository formStepsRepository, FormItemsRepository formItemsRepository, MultistepFormsService multistepFormsService, FormsServiceImpl formsService) {
        super(formStepsRepository);
        this.formStepsRepository = formStepsRepository;
        this.formItemsRepository = formItemsRepository;
        this.multistepFormsService = multistepFormsService;
        this.formsService = formsService;
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
        FormStepEntity entity = (id == -1) ? new FormStepEntity() : formStepsRepository.getReferenceById(id);
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
    public boolean checkItemPerms(FormStepEntity entity, Long id) {
        if(entity == null) return false;

        int domainId = CloudToolsForCore.getDomainId();
        if(entity.getDomainId() != null && entity.getDomainId().intValue() != domainId) return false;

        String formName = entity.getFormName();
        if(id != null && id.longValue() > 0) {
            FormStepEntity stored = formStepsRepository.findFirstByIdAndDomainId(id, domainId).orElse(null);
            if(stored == null || Objects.equals(stored.getFormName(), formName) == false) return false;
            formName = stored.getFormName();
        }

        return Tools.isNotEmpty(formName) && getUser() != null && formsService.isFormAccessible(formName, getUser());
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
        Long sourceId = entity == null ? null : entity.getIdForDuplication();
        FormStepEntity source = sourceId == null || sourceId.longValue() < 1 ? null :
            formStepsRepository.findFirstByIdAndDomainId(sourceId, CloudToolsForCore.getDomainId()).orElse(null);
        if(source == null || Objects.equals(source.getFormName(), entity.getFormName()) == false || checkItemPerms(source, sourceId) == false) {
            throwConstraintViolation("components.file_archiv.file_rename.nemate_pravo_na_tuto_editaciu");
        }

        int tmpId = -getUser().getUserId();

        //IF something went wrong, delete all awaiting duplicate
        formItemsRepository.deleteAllByFormNameAndStepIdAndDomainId(entity.getFormName(), Long.valueOf(tmpId), CloudToolsForCore.getDomainId());

        //Now insert new items that gonna be set after duplicate - stepId gonna be -currentUserId
        List<FormItemEntity> stepItemsToDuplicate = formItemsRepository.getAllStepItems(entity.getIdForDuplication(), CloudToolsForCore.getDomainId());
        for(FormItemEntity stepItem : stepItemsToDuplicate) {
            stepItem.setId(null);
            stepItem.setStepId( Long.valueOf(-getUser().getUserId()) );
            stepItem.setItemFormId(""); //remove itemFormId so in afterDuplicate its generated new one
        }
        formItemsRepository.saveAll(stepItemsToDuplicate);
    }

    @Override
    public void afterDuplicate(FormStepEntity entity, Long originalId) {
        int tmpId = -getUser().getUserId();

        // Find all items taht are awaiting step duplicate to have id, and set id
        for(FormItemEntity stepItem : formItemsRepository.findItemsToDuplicate(entity.getFormName(), Long.valueOf(tmpId), CloudToolsForCore.getDomainId())) {
            stepItem.setStepId(entity.getId());
            stepItem.setItemFormId( multistepFormsService.getValidItemFormId(stepItem) );
            formItemsRepository.save(stepItem);
        }

        // Now update form pattern
        multistepFormsService.updateFormPattern(entity.getFormName());

        // Now update step positions
        multistepFormsService.updateStepsPositions(entity.getFormName());
    }

    @GetMapping(value="/get-step", params={"form-name", "step-id", "language"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getFormStepHtml(@RequestParam("form-name") String formName, @RequestParam("step-id") Long stepId, @RequestParam("language") String language, HttpServletRequest request) {
        String encoding = SetCharacterEncodingFilter.getEncoding();
        if (Tools.isEmpty(encoding)) encoding = "UTF-8"; // Fallback
        String contentTypeWithCharset = MediaType.TEXT_HTML_VALUE + "; charset=" + encoding;

        try {
            // This is called only from ADMIN section so there is no CSRF and formCount, sooo formCount must be set
            FormHtmlHandler formHtmlHandler = new FormHtmlHandler(formName, 1, request);
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
        String formName = MultistepFormsService.getFormName(request);

        // call super row reorder to update positions
        ResponseEntity<Boolean> response = super.rowReorder(request, rowReorderDto);

        if(response.getStatusCode().is2xxSuccessful() && response.getBody() == Boolean.TRUE) {
            // All good, now update steps positions in form
            multistepFormsService.updateStepsPositions(formName);
        }

        return response;
    }

    @Override
    protected boolean checkRowReorderScope(HttpServletRequest request, List<FormStepEntity> entities) {
        String formName = MultistepFormsService.getFormName(request);
        if(Tools.isEmpty(formName) || entities == null || entities.isEmpty() || getUser() == null) return false;

        int domainId = CloudToolsForCore.getDomainId();
        for(FormStepEntity entity : entities) {
            if(entity == null || Objects.equals(formName, entity.getFormName()) == false ||
                entity.getDomainId() == null || entity.getDomainId().intValue() != domainId) return false;
        }

        return formsService.isFormAccessible(formName, getUser());
    }
}
