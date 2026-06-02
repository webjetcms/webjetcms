package sk.iway.iwcm.components.customfields.rest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsEntity;
import sk.iway.iwcm.components.customfields.jpa.CustomFieldsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/custom-fields/")
@PreAuthorize("@WebjetSecurityService.hasPermission('customFields')")
@Datatable
public class CustomFieldsRestController extends DatatableRestControllerV2<CustomFieldsEntity, Long> {

    public final CustomFieldsRepository customFieldsRepository;
    public final CustomFieldsService customFieldsService;

    @Autowired
    public CustomFieldsRestController(CustomFieldsRepository customFieldsRepository, CustomFieldsService customFieldsService) {
        super(customFieldsRepository);
        this.customFieldsRepository = customFieldsRepository;
        this.customFieldsService = customFieldsService;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, CustomFieldsEntity> target, Identity user, Errors errors, Long id, CustomFieldsEntity entity) {
        super.validateEditor(request, target, user, errors, id, entity);

        if(errors.hasErrors()) return;

        if("create".equals(target.getAction()) || "edit".equals(target.getAction())) {
            Long existingId = null;
            if(entity.getEntityId() == null || entity.getEntityId() < 1) {
                existingId = customFieldsRepository.getNullEntityId(entity.getClassName(), entity.getAlphabet()).orElse(-1l);
            } else {
                existingId = customFieldsRepository.getEntityId(entity.getClassName(), entity.getAlphabet(), entity.getEntityId()).orElse(-1L);
            }

            boolean isDuplicate = false;
            if("create".equals(target.getAction()) && existingId > 0) isDuplicate = true;
            if("edit".equals(target.getAction()) && existingId > 0 && existingId.equals(id) == false) isDuplicate = true;

            if(isDuplicate) {
                String errorMessage = getProp().getText("settings.custom-fields.duplicity-err");
                // This combination of className, alphabet and entityId already exists, so we cannot allow it
                errors.rejectValue("errorField.className", null, errorMessage); //NOSONAR
                errors.rejectValue("errorField.alphabet", null, errorMessage); //NOSONAR
                errors.rejectValue("errorField.entityId", null, errorMessage); //NOSONAR
            }
        }
    }

    @GetMapping("/autocomplete-class")
    public List<String> getAutocompleteClass(@RequestParam String term) {
        return customFieldsService.getClassOptions(term);
    }

    @Override
    public void getOptions(DatatablePageImpl<CustomFieldsEntity> page) {
        List<String> options = new ArrayList<>();
        options.add(""); //empty option
        //generate options from A-Z
        for(char c = 'A'; c <= 'Z'; c++) {
            options.add(String.valueOf(c));
        }

        page.addOptions("alphabet", options, null, null, false);
    }

    @Override
    public CustomFieldsEntity processFromEntity(CustomFieldsEntity entity, ProcessItemAction action) {
        if (entity.getEntityId() != null && entity.getEntityId() < 1) {
            entity.setEntityId(null);
        }
        if (entity.getBonusEntityId() != null && entity.getBonusEntityId() < 1) {
            entity.setBonusEntityId(null);
        }
        return entity;
    }

}
