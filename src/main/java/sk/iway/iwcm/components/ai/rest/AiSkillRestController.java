package sk.iway.iwcm.components.ai.rest;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.ai.jpa.AiSkillEntity;
import sk.iway.iwcm.components.ai.jpa.AiSkillRepository;
import sk.iway.iwcm.components.ai.tools.AiToolService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

/** Manages shared assistant skills in the current administration domain. */
@RestController
@RequestMapping("/admin/rest/settings/ai-skills")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_ai_tools')")
@Datatable
public class AiSkillRestController extends DatatableRestControllerV2<AiSkillEntity, Long> {

    private final AiToolService tools;
    private final AiSkillRepository skills;

    public AiSkillRestController(AiSkillRepository skills, AiToolService tools) {
        super(skills, AiSkillEntity.class, true);
        this.tools = tools;
        this.skills = skills;
    }

    @Override
    public void getOptions(DatatablePageImpl<AiSkillEntity> page) {
        page.addOptions("toolNames", tools.assignmentTools(getRequest(), getProp()), "description", "name", true);
    }

    @Override
    protected void copyEntityIntoOriginal(AiSkillEntity entity, AiSkillEntity original) {
        if (original == null) throwError("datatables.error.domainId");
        super.copyEntityIntoOriginal(entity, original);
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, AiSkillEntity> target, Identity user,
            Errors errors, Long id, AiSkillEntity entity) {
        if (target.isInsert() && skills.existsByDomainIdAndNameIgnoreCase(getDomainId(), entity.getName())) {
            errors.rejectValue("errorField.name", "", getProp().getText("components.ai_assistants.skills.duplicate_name"));
        }
    }

    @Override
    public AiSkillEntity processToEntity(AiSkillEntity entity, ProcessItemAction action) {
        entity.setDomainId(getDomainId());
        return entity;
    }
}
