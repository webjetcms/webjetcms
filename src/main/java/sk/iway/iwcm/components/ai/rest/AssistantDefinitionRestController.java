package sk.iway.iwcm.components.ai.rest;

import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEditorFields;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.jpa.SupportedActions;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.components.ai.stat.rest.AiStatService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

/**
 * REST controller for AI assistant definitions - handles datatable requests
 */
@RestController
@RequestMapping("/admin/rest/ai/assistant-definition/")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_ai_tools')")
@Datatable
public class AssistantDefinitionRestController extends DatatableRestControllerV2<AssistantDefinitionEntity, Long> {

    private final AssistantDefinitionRepository repo;
    private final AiService aiService;
    private final AiAssistantsService aiAssistantsService;
    private final AiStatRepository aiStatRepository;
    public static final String GROUPS_PREFIX = "components.ai_assistants.groups.";

    @Autowired
    public AssistantDefinitionRestController(AssistantDefinitionRepository repo, AiService aiService, AiAssistantsService aiAssistantsService, AiStatRepository aiStatRepository) {
        super(repo);
        this.repo = repo;
        this.aiService = aiService;
        this.aiAssistantsService = aiAssistantsService;
        this.aiStatRepository = aiStatRepository;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, AssistantDefinitionEntity> target, Identity user, Errors errors, Long id, AssistantDefinitionEntity entity) {
        List<AssistantDefinitionEntity> sameEntities = repo.getEntitiesCount(entity.getName(), entity.getProvider(), CloudToolsForCore.getDomainId());

        if("create".equals(target.getAction()) || "edit".equals(target.getAction())) {
            if(Tools.isEmpty(entity.getAction())) {
                errors.rejectValue("action", "", getProp().getText("jakarta.validation.constraints.NotNull.message"));
            }

            if(Boolean.TRUE.equals(entity.getUseTokenLimits()) && (entity.getTokenLimit() == null || entity.getTokenLimit() <= 0)) {
                errors.rejectValue("errorField.tokenLimit", "", getProp().getText("components.ai_assistants.token_limit.invalid"));
            }

            if(sameEntities != null && sameEntities.size() > 0) {
                if("create".equals(target.getAction())) {
                    //This combination already exist
                    errors.rejectValue("name",  "", getProp().getText("components.ai_assistants.unique_name_err"));
                } else {
                    boolean isOneOfThem = false;
                    for(AssistantDefinitionEntity sameEntity : sameEntities) {
                        if(sameEntity.getId().equals(entity.getId()) == true) {
                            isOneOfThem = true;
                            break;
                        }
                    }
                    if(isOneOfThem == false) errors.rejectValue("name",  "", getProp().getText("components.ai_assistants.unique_name_err"));
                }
            }
        }

        super.validateEditor(request, target, user, errors, id, entity);
    }

    @Override
    public AssistantDefinitionEntity getOneItem(long id) {
        if(id < 1) {
            AssistantDefinitionEntity newOne = new AssistantDefinitionEntity();
            newOne.setActive(true);
            newOne.setUseTokenLimits(false);
            return newOne;
        }

        AssistantDefinitionEntity assistant = repo.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElse(null);

        if(assistant != null) {
            //Remove EMPTY_VALUES so you force the user to set it
            if(AiAssistantsService.EMPTY_VALUE.equals( assistant.getAction() )) assistant.setAction("");
            if(AiAssistantsService.EMPTY_VALUE.equals( assistant.getClassName() )) assistant.setClassName("");
            if(AiAssistantsService.EMPTY_VALUE.equals( assistant.getFieldFrom() )) assistant.setFieldFrom("");
            if(AiAssistantsService.EMPTY_VALUE.equals( assistant.getFieldTo() )) assistant.setFieldTo("");

            processFromEntity(assistant, null);
        }

        return assistant;
    }

    @Override
    public void getOptions(DatatablePageImpl<AssistantDefinitionEntity> page) {
        Long nextTokenReset = aiAssistantsService.getNextTokenReset();
        page.addSummary("nextTokenReset", nextTokenReset == null ? 0L : nextTokenReset);
        page.addOptions("provider", aiService.getProviders(getProp()), "label", "value", false);
        page.addOptions("action", SupportedActions.getSupportedActions(getProp()), "label", "value", false);
        page.addOptions("groupName", aiService.getGroupsOptions(getProp()), "label", "value", false);
        page.addOptions("editorFields.statusIcons", aiAssistantsService.getStatusIconOptions(getProp()), "label", "value", false);

        //Every assistant should set their specific selects
        aiAssistantsService.getProviderSpecificOptions(page, getProp());
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<AssistantDefinitionEntity> root, CriteriaBuilder builder) {
        super.addSpecSearch(params, predicates, root, builder);
        aiAssistantsService.addStatusFilter(params.get("searchEditorFields.statusIcons"), predicates, root, builder);
    }

    @Override
    public boolean processAction(AssistantDefinitionEntity entity, String action) {
        boolean resetSelected = "resetTokenUsage".equals(action);
        if (!resetSelected && !"resetAllTokenUsage".equals(action)) return super.processAction(entity, action);

        int domainId = CloudToolsForCore.getDomainId();
        String message = getProp().getText("components.ai_assistants.token_reset.error");
        NotifyType notifyType = NotifyType.ERROR;
        try {
            if (resetSelected && entity != null && entity.getId() != null) {
                int updated = repo.resetUsedTokensByIdAndDomainId(entity.getId(), domainId);
                if (updated > 0) {
                    Adminlog.add(Adminlog.TYPE_AI, "Reset token usage for AI assistant " + entity.getId() + " in domain " + domainId + ".", entity.getId(), Long.valueOf(domainId));
                    message = getProp().getText("components.ai_assistants.token_reset.selected.success", String.valueOf(entity.getId()));
                    notifyType = NotifyType.SUCCESS;
                }
            } else if (!resetSelected && entity == null) {
                int updated = repo.resetUsedTokensByDomainId(domainId);
                Adminlog.add(Adminlog.TYPE_AI, "Reset token usage for all AI assistants in domain " + domainId + ". Updated assistants: " + updated + ".", -1, domainId);
                message = getProp().getText("components.ai_assistants.token_reset.all.success", String.valueOf(updated));
                notifyType = NotifyType.SUCCESS;
            }
        } catch (Exception e) {
            Logger.error(AssistantDefinitionRestController.class, e);
        }

        addNotify(new NotifyBean(getProp().getText("components.ai_assistants.token_reset.title"), message, notifyType, 15000));
        // Return the notification without a generic action error that would hide it.
        return true;
    }

    @Override
    public void beforeSave(AssistantDefinitionEntity entity) {
        //
        aiAssistantsService.prepareBeforeSave(entity, getProp());

        entity.setLastUpdate(new Date());

        //Default logic
        if(entity.getKeepHtml() == null) entity.setKeepHtml(false);
        if(entity.getUseStreaming() == null) entity.setUseStreaming(false);
        if(Boolean.FALSE.equals(entity.getUseTokenLimits())) {
            entity.setTokenLimit(0L);
            entity.setUsedTokens(0L);
        }
    }

    @Override
    public AssistantDefinitionEntity processFromEntity(AssistantDefinitionEntity entity, ProcessItemAction action) {

        if(entity != null ) {
            entity.setUseTokenLimits(entity.getTokenLimit() != null && entity.getTokenLimit() > 0);
            AssistantDefinitionEditorFields adef = entity.getEditorFields();
            if(adef == null) adef = new AssistantDefinitionEditorFields();
            adef.fromAssistantDefinition(entity, aiAssistantsService.getAssistantStatus(entity.getProvider()),  getProp());
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

    /**
     * Returns recent daily usage for an assistant belonging to the current domain.
     *
     * @param assistantId assistant being edited
     * @return daily token totals keyed by epoch milliseconds, or an empty map when the assistant is unavailable
     */
    @GetMapping("/token-usage")
    public Map<Long, Long> getTokenUsage(@RequestParam("assistantId") Long assistantId) {
        if (repo.findFirstByIdAndDomainId(assistantId, CloudToolsForCore.getDomainId()).isEmpty()) return Map.of();
        return AiStatService.getAssistantDailyTokens(assistantId, aiStatRepository);
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

    @GetMapping(value = "/translate-key", produces = "text/plain; charset=UTF-8")
    public String getTranslatedKey(@RequestParam(name = "key") String key) {
        return getProp().getText(key);
    }
}
