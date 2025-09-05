package sk.iway.iwcm.components.ai.rest;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.jpa.SupportedActions;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/ai/assistant-definition/")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_ai_tools')")
@Datatable
public class AssistantDefinitionRestController extends DatatableRestControllerV2<AssistantDefinitionEntity, Long> {

    private final AssistantDefinitionRepository repo;
    private final AiService aiService;
    private final AiAssistantsService aiAssistantsService;
    public static final String GROUPS_PREFIX = "components.ai_assistants.groups.";

    @Autowired
    public AssistantDefinitionRestController(AssistantDefinitionRepository repo, AiService aiService, AiAssistantsService aiAssistantsService) {
        super(repo);
        this.repo = repo;
        this.aiService = aiService;
        this.aiAssistantsService = aiAssistantsService;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, AssistantDefinitionEntity> target, Identity user, Errors errors, Long id, AssistantDefinitionEntity entity) {
        if("create".equals(target.getAction()) || "edit".equals(target.getAction())) {
            if(Tools.isEmpty(entity.getAction())) {
                errors.rejectValue("action", "", getProp().getText("javax.validation.constraints.NotNull.message"));
            }
        }

        super.validateEditor(request, target, user, errors, id, entity);
    }

    @Override
    public AssistantDefinitionEntity getOneItem(long id) {
        if(id < 1) {

            AssistantDefinitionEntity newOne = new AssistantDefinitionEntity();
            newOne.setActive(true);
            return newOne;
        }

        AssistantDefinitionEntity assistant = repo.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElse(null);

        if(assistant != null) {
            //Remove EMPTY_VALUES so you force the user to set it
            if(AiAssistantsService.EMPTY_VALUE.equals( assistant.getAction() )) assistant.setAction("");
            if(AiAssistantsService.EMPTY_VALUE.equals( assistant.getClassName() )) assistant.setClassName("");
            if(AiAssistantsService.EMPTY_VALUE.equals( assistant.getFieldFrom() )) assistant.setFieldFrom("");
            if(AiAssistantsService.EMPTY_VALUE.equals( assistant.getFieldTo() )) assistant.setFieldTo("");

            if(Tools.isNotEmpty(assistant.getDescription())) {
                assistant.setTranslatedDescription( getProp().getText(assistant.getDescription()) );
            }
        }

        return assistant;
    }

    @Override
    public AssistantDefinitionEntity editItem(AssistantDefinitionEntity entity, long id) {
        //Get entity from DB
        AssistantDefinitionEntity existingEntity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Entity with id " + id + " not found"));

        //Safety measure, for disabled fields
        entity.setCreated(existingEntity.getCreated());

        return repo.save(entity);
    }

    @Override
    public AssistantDefinitionEntity insertItem(AssistantDefinitionEntity entity) {
        entity.setCreated(new Date());
        entity.setDomainId(CloudToolsForCore.getDomainId());

        return repo.save(entity);
    }

    @Override
    public void getOptions(DatatablePageImpl<AssistantDefinitionEntity> page) {
        page.addOptions("provider", aiService.getProviders(getProp()), "label", "value", false);
        page.addOptions("action", SupportedActions.getSupportedActions(getProp()), "label", "value", false);
        page.addOptions("groupName", aiService.getGroupsOptions(getProp()), "label", "value", false);

        //Every assistant should set their specific selects
        aiAssistantsService.getProviderSpecificOptions(page, getProp());
    }

    @Override
    public void beforeSave(AssistantDefinitionEntity entity) {
        //
        aiAssistantsService.prepareBeforeSave(entity, getProp());

        //Default logic
        if(entity.getKeepHtml() == null) entity.setKeepHtml(false);
        if(entity.getUseStreaming() == null) entity.setUseStreaming(false);
    }

    @Override
    public AssistantDefinitionEntity processFromEntity(AssistantDefinitionEntity entity, ProcessItemAction action) {
        if(ProcessItemAction.GETALL.equals(action) && Tools.isNotEmpty(entity.getDescription())) {
            entity.setTranslatedDescription( getProp().getText(entity.getDescription()) );
        }
        return entity;
    }



    @Override
    public void afterDelete(AssistantDefinitionEntity entity, long id) {
        afterSave(entity, null);
    }

    @Override
    public void afterDuplicate(AssistantDefinitionEntity entity, Long originalId) {
        afterSave(entity, null);
    }

    @Override
    public void afterSave(AssistantDefinitionEntity entity, AssistantDefinitionEntity saved) {
        AiAssistantsService.clearCache();
    }

    @GetMapping("/autocomplete-class")
    public List<String> getAutocompleteClass(@RequestParam String term) {
        return aiAssistantsService.getClassOptions(term);
    }

    @GetMapping("/autocomplete-field")
    public List<String> getAutocompleteField(@RequestParam String term, @RequestParam("DTE_Field_className") String className) {
        return aiAssistantsService.getFieldOptions(term, className);
    }

    @GetMapping("/autocomplete-model")
    public List<String> getAutocompleteModel(@RequestParam String term, @RequestParam("DTE_Field_provider") String provider) {
        return aiService.getModelOptions(term, provider, getProp(), getRequest());
    }

    @GetMapping("/provider-fields")
    public List<String> getProviderFields(@RequestParam(name = "provider") String provider, @RequestParam(name = "action") String action) {
        return aiAssistantsService.getProviderFields(provider, action, getProp());
    }
}
