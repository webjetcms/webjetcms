package sk.iway.iwcm.kokos;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
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
        existingEntity.setModel( entity.getModel() );
        existingEntity.setTemperature( entity.getTemperature() );
        existingEntity.setRoleDescription( entity.getRoleDescription() );

        return repo.save(existingEntity);
    }

    @Override
    public OpenAiAssistantsEntity insertItem(OpenAiAssistantsEntity entity) {
        String assistantId;
        String prefix = OpenAiAssistantsService.getAssitantPrefix();

        entity.setName( prefix + entity.getFullName() );

        try {
            assistantId = openAiAssistantsService.insertAssistant(entity.getFullName(), entity.getRoleDescription(), entity.getTemperature(), entity.getModel());

            entity.setCreated(new Date());
            entity.setAssistantKey(assistantId);
            entity.setDomainId(CloudToolsForCore.getDomainId());

            return repo.save(entity);

        } catch(IOException e) {
            //BAdddd
            return null;
        }
    }

    @Override
    public void getOptions(DatatablePageImpl<OpenAiAssistantsEntity> page) {
        page.addOptions("model", openAiService.getSupportedModels(), "label", "value", false);
    }

    @Override
    public boolean processAction(OpenAiAssistantsEntity entity, String action) {

        if("syncToTable".equals(action)) {

            openAiAssistantsService.syncToTable(repo);

            return true;
        } else if("syncFromTable".equals(action)) {

            return true;
        }

        return false;
    }
}
