package sk.iway.iwcm.kokos;

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
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/openai-assistants/")
@PreAuthorize("@WebjetSecurityService.isAdmin()")
@Datatable
public class OpenAiAssistantsRestController extends DatatableRestControllerV2<OpenAiAssistantsEntity, Long> {

    private final OpenAiAssistantsRepository repo;
    private final OpenAiService openAiService;
    private final OpenAiAssistantsService openAiAssistantsService;

    @Autowired
    public OpenAiAssistantsRestController(OpenAiAssistantsRepository repo, OpenAiService openAiService, OpenAiAssistantsService openAiAssistantsService) {
        super(repo);
        this.repo = repo;
        this.openAiService = openAiService;
        this.openAiAssistantsService = openAiAssistantsService;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, OpenAiAssistantsEntity> target, Identity user, Errors errors, Long id, OpenAiAssistantsEntity entity) {
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
    public OpenAiAssistantsEntity editItem(OpenAiAssistantsEntity entity, long id) {
        //Get entity from DB
        OpenAiAssistantsEntity existingEntity = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Entity with id " + id + " not found"));

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
            openAiAssistantsService.updateAssistant(existingEntity, getProp());
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }

        return repo.save(existingEntity);
    }

    @Override
    public boolean deleteItem(OpenAiAssistantsEntity entity, long id) {
        try {
            openAiAssistantsService.deleteAssistant(entity, getProp());
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }

        repo.delete(entity);

        return true;
    }

    @Override
    public OpenAiAssistantsEntity insertItem(OpenAiAssistantsEntity entity) {
        String assistantId;

        entity.setNameAddPrefix( entity.getName() );

        try {
            assistantId = openAiAssistantsService.insertAssistant(entity, getProp());

            entity.setCreated(new Date());
            entity.setAssistantKey(assistantId);
            entity.setDomainId(CloudToolsForCore.getDomainId());

            return repo.save(entity);
        } catch(Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    @Override
    public void getOptions(DatatablePageImpl<OpenAiAssistantsEntity> page) {
        page.addOptions("model", openAiService.getSupportedModels(getProp()), "label", "value", false);
    }

    @Override
    public boolean processAction(OpenAiAssistantsEntity entity, String action) {
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
    public void beforeSave(OpenAiAssistantsEntity entity) {
        if(entity.getTemperature() == null ) entity.setTemperature(BigDecimal.ONE);
    }

    @Override
    public void afterSave(OpenAiAssistantsEntity entity, OpenAiAssistantsEntity saved) {
        OpenAiAssistantsService.removeAssistantsFromCache();
    }

    @Override
    public void afterDelete(OpenAiAssistantsEntity entity, long id) {
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
}
