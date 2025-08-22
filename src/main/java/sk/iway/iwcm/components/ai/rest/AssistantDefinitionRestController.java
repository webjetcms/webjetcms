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
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/ai/assistant-definition/")
@PreAuthorize("@WebjetSecurityService.isAdmin()")
@Datatable
public class AssistantDefinitionRestController extends DatatableRestControllerV2<AssistantDefinitionEntity, Long> {

    private final AssistantDefinitionRepository repo;
    private final AiService aiService;
    private final AiAssistantsService aiAssistantsService;

    @Autowired
    public AssistantDefinitionRestController(AssistantDefinitionRepository repo, AiService aiService, AiAssistantsService aiAssistantsService) {
        super(repo);
        this.repo = repo;
        this.aiService = aiService;
        this.aiAssistantsService = aiAssistantsService;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, AssistantDefinitionEntity> target, Identity user, Errors errors, Long id, AssistantDefinitionEntity entity) {
        if("create".equals(target.getAction()) && Tools.isNotEmpty(entity.getName())) {
            //New ame must be unique
            String prefix = AiAssistantsService.getAssitantPrefix();
            List<String> otherNames = repo.getAssistantNames(prefix + "%", CloudToolsForCore.getDomainId());
            for(String otherName : otherNames) {
                if( otherName.equalsIgnoreCase( prefix + entity.getName() ) ) {
                    errors.rejectValue("name", "", "Assistant name must be unique");
                    break;
                }
            }
        }

        if("create".equals(target.getAction()) || "edit".equals(target.getAction())) {
            if(Tools.isEmpty(entity.getAction())) {
                errors.rejectValue("action", "", getProp().getText("javax.validation.constraints.NotNull.message"));
            }
        }

        super.validateEditor(request, target, user, errors, id, entity);
    }

    @Override
    public AssistantDefinitionEntity getOneItem(long id) {
        if(id < 1) return new AssistantDefinitionEntity();

        AssistantDefinitionEntity assistant = repo.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElse(null);

        if(assistant != null) {
            //Remove EMPTY_VALUES so you force the user to set it
            if(AiAssistantsService.EMPTY_VALUE.equals( assistant.getAction() )) assistant.setAction("");
            if(AiAssistantsService.EMPTY_VALUE.equals( assistant.getClassName() )) assistant.setClassName("");
            if(AiAssistantsService.EMPTY_VALUE.equals( assistant.getFieldFrom() )) assistant.setFieldFrom("");
            if(AiAssistantsService.EMPTY_VALUE.equals( assistant.getFieldTo() )) assistant.setFieldTo("");
        }

        return assistant;
    }

    @Override
    public AssistantDefinitionEntity editItem(AssistantDefinitionEntity entity, long id) {
        //Get entity from DB
        AssistantDefinitionEntity existingEntity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Entity with id " + id + " not found"));

        //Safety measure, for disabled fields
        entity.setNameAddPrefix( entity.getName() );
        entity.setAssistantKey(existingEntity.getAssistantKey());
        entity.setCreated(existingEntity.getCreated());

        try {
            aiAssistantsService.updateAssistant(entity, getProp());
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }

        return repo.save(entity);
    }

    @Override
    public boolean deleteItem(AssistantDefinitionEntity entity, long id) {
        try {
            aiAssistantsService.deleteAssistant(entity, getProp());
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }

        repo.delete(entity);

        return true;
    }

    @Override
    public AssistantDefinitionEntity insertItem(AssistantDefinitionEntity entity) {
        String assistantId = null;

        entity.setNameAddPrefix( entity.getName() );

        try {
            assistantId = aiAssistantsService.insertAssistant(entity, getProp());

            entity.setCreated(new Date());
            entity.setAssistantKey(assistantId);
            entity.setDomainId(CloudToolsForCore.getDomainId());

            return repo.save(entity);
        } catch(Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    @Override
    public void getOptions(DatatablePageImpl<AssistantDefinitionEntity> page) {
        page.addOptions("provider", aiService.getSupportedProviders(getProp()), "label", "value", false);
        page.addOptions("action", aiService.getSupportedActions(getProp()), "label", "value", false);

        //Every assistant should set their specific selects
        aiAssistantsService.getProviderSpecificOptions(page, getProp());
    }

    @Override
    public boolean processAction(AssistantDefinitionEntity entity, String action) {
        if("syncToTable".equals(action)) {
            try {
                aiAssistantsService.syncToTable(repo, getProp());
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage());
            }

            return true;
        }

        return false;
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
    public void afterSave(AssistantDefinitionEntity entity, AssistantDefinitionEntity saved) {
        AiAssistantsService.removeAssistantsFromCache();
    }

    @Override
    public void afterDelete(AssistantDefinitionEntity entity, long id) {
        AiAssistantsService.removeAssistantsFromCache();
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
        return aiService.getModelOptions(term, provider, getProp());
    }

    @GetMapping("/provider-fields")
    public List<String> getProviderFields(@RequestParam(name = "provider") String provider, @RequestParam(name = "action") String action) {
        return aiAssistantsService.getProviderFields(provider, action, getProp());
    }

    @GetMapping("/autocomplete-group")
    public List<String> getAutocompleteGroup(@RequestParam String term) {
        return repo.getGroupNames(term, CloudToolsForCore.getDomainId());
    }
}
