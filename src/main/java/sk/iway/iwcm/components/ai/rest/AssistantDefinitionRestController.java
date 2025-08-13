package sk.iway.iwcm.components.ai.rest;

import java.math.BigDecimal;
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
import sk.iway.iwcm.components.ai.providers.openai.OpenAiAssistantsService;
import sk.iway.iwcm.components.ai.providers.openai.OpenAiService;
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
    private final OpenAiService openAiService;
    private final OpenAiAssistantsService openAiAssistantsService;

    @Autowired
    public AssistantDefinitionRestController(AssistantDefinitionRepository repo, OpenAiService openAiService, OpenAiAssistantsService openAiAssistantsService) {
        super(repo);
        this.repo = repo;
        this.openAiService = openAiService;
        this.openAiAssistantsService = openAiAssistantsService;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, AssistantDefinitionEntity> target, Identity user, Errors errors, Long id, AssistantDefinitionEntity entity) {
        if("create".equals(target.getAction()) && Tools.isNotEmpty(entity.getName())) {
            //New ame must be unique
            String prefix = OpenAiAssistantsService.getAssitantPrefix();
            List<String> otherNames = repo.getAssistantNames(prefix + "%", CloudToolsForCore.getDomainId());
            boolean valid = true;
            for(String otherName : otherNames) {
                if( otherName.equalsIgnoreCase( prefix + entity.getName() ) ) {
                    valid = false;
                    break;
                }
            }

            if(valid == false) {
                errors.rejectValue("name", "", "Assistant name must be unique");
            }
        }

        super.validateEditor(request, target, user, errors, id, entity);
    }

    @Override
    public AssistantDefinitionEntity editItem(AssistantDefinitionEntity entity, long id) {
        //Get entity from DB
        AssistantDefinitionEntity existingEntity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Entity with id " + id + " not found"));

        //Copy just allowed params
        existingEntity.setNameAddPrefix( entity.getName() );
        existingEntity.setModel( entity.getModel() );
        existingEntity.setInstructions( entity.getInstructions() );
        existingEntity.setClassName( entity.getClassName() );
        existingEntity.setFieldFrom( entity.getFieldFrom() );
        existingEntity.setFieldTo( entity.getFieldTo() );
        existingEntity.setDescription( entity.getDescription() );
        existingEntity.setTemperature( entity.getTemperature() );

        try {
            if ("openai".equals(existingEntity.getProvider())) {
                openAiAssistantsService.updateAssistant(existingEntity, getProp());
            } else {
                existingEntity.setAssistantKey(null);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }

        return repo.save(existingEntity);
    }

    @Override
    public boolean deleteItem(AssistantDefinitionEntity entity, long id) {
        try {
            if ("openai".equals(entity.getProvider())) {
                openAiAssistantsService.deleteAssistant(entity, getProp());
            }
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
            if ("openai".equals(entity.getProvider())) {
                assistantId = openAiAssistantsService.insertAssistant(entity, getProp());
            }

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
        page.addOptions("provider", openAiService.getSupportedProviders(getProp()), "label", "value", false);
    }

    @Override
    public boolean processAction(AssistantDefinitionEntity entity, String action) {
        if("syncToTable".equals(action)) {
            try {
                openAiAssistantsService.syncToTable(repo, getProp());
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage());
            }

            return true;
        }

        return false;
    }



    @Override
    public void beforeSave(AssistantDefinitionEntity entity) {
        if(entity.getTemperature() == null ) entity.setTemperature(BigDecimal.ONE);
    }

    @Override
    public void afterSave(AssistantDefinitionEntity entity, AssistantDefinitionEntity saved) {
        OpenAiAssistantsService.removeAssistantsFromCache();
    }

    @Override
    public void afterDelete(AssistantDefinitionEntity entity, long id) {
        OpenAiAssistantsService.removeAssistantsFromCache();
    }

    @GetMapping("/autocomplete-class")
    public List<String> getAutocompleteClass(@RequestParam String term) {
        return openAiAssistantsService.getClassOptions(term);
    }

    @GetMapping("/autocomplete-field")
    public List<String> getAutocompleteField(@RequestParam String term, @RequestParam("DTE_Field_className") String className) {
        return openAiAssistantsService.getFieldOptions(term, className);
    }

    @GetMapping("/autocomplete-model")
    public List<String> getAutocompleteModel(@RequestParam String term, @RequestParam("DTE_Field_provider") String provider) {
        return openAiService.getModelOptions(term, provider, getProp());
    }
}
