package sk.iway.iwcm.components.ai.rest;

import java.util.List;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
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

    private static final String DUPLICATE_NAME = "components.ai_assistants.skills.duplicate_name";
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

    /** The autocomplete displays the localized value and inserts links using the group key. */
    public record GroupOption(String key, String value) {}

    @GetMapping("/autocomplete-tool-group")
    public List<GroupOption> autocompleteToolGroup(@RequestParam(defaultValue = "") String term) {
        String query = "%".equals(term) ? "" : DB.internationalToEnglish(term.strip()).toLowerCase(Locale.ROOT);
        return tools.assignmentTools(getRequest(), getProp()).stream()
                .map(tool -> new GroupOption(tool.group(), tool.groupLabel())).distinct()
                .filter(group -> DB.internationalToEnglish(group.value()).toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    @Override
    public AiSkillEntity insertItem(AiSkillEntity entity) {
        if (duplicateName(entity.getName(), -1L)) throwError(DUPLICATE_NAME);
        return super.insertItem(entity);
    }

    @Override
    protected void copyEntityIntoOriginal(AiSkillEntity entity, AiSkillEntity original) {
        if (original == null) throwError("datatables.error.domainId");
        if (duplicateName(entity.getName(), original.getId())) throwError(DUPLICATE_NAME);
        super.copyEntityIntoOriginal(entity, original);
    }

    private boolean duplicateName(String name, long excludedId) {
        if (name == null) return false;
        return skills.exists((root, query, builder) -> builder.and(
                builder.equal(root.get("domainId"), getDomainId()),
                builder.equal(builder.upper(root.get("name")), builder.upper(builder.literal(name))),
                builder.notEqual(root.get("id"), excludedId)));
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, AiSkillEntity> target, Identity user,
            Errors errors, Long id, AiSkillEntity entity) {
        if (!target.isInsert() && !target.isUpdate()) return;
        // Check imported names when the save path has resolved the actual target row.
        if (isImporting() || Tools.isNotEmpty(target.getUpdateByColumn())) return;
        long excludedId = target.isUpdate() && id != null ? id : -1L;
        if (duplicateName(entity.getName(), excludedId)) {
            errors.rejectValue("errorField.name", "", getProp().getText(DUPLICATE_NAME));
        }
    }

    @Override
    public AiSkillEntity processToEntity(AiSkillEntity entity, ProcessItemAction action) {
        entity.setDomainId(getDomainId());
        return entity;
    }
}
