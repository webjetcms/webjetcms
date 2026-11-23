package sk.iway.iwcm.components.ai.rest;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import com.webjetcms.ai.AiPromptTemplate;

import it.sauronsoftware.cron4j.InvalidPatternException;
import it.sauronsoftware.cron4j.SchedulingPattern;
import jakarta.persistence.Entity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.jpa.SupportedActions;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.appstore.AppManager;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.cron.CronDB;
import sk.iway.iwcm.system.cron.CronTask;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Service for AI Assistants management / datatables, editor
 */
@Service
public class AiAssistantsService {

    private static final String CLASS_FIELD_MAP_KEY = "AiAssistantsService_classFieldsMap";
    public static final String EMPTY_VALUE = "EMPTY_VALUE";
    /** Virtual entity binding for assistants offered in the global AI Chat. */
    public static final String CHAT_TARGET = "CHAT_SEARCH";
    private static final String TOP_SEARCH_FIELD = "top-search-field";
    private static final String CACHE_KEY_PREFIX = "AiAssistantsService.assistants-";

    private final List<AiAssitantsInterface> aiAssitantsInterfaces;

    @Autowired
    public AiAssistantsService(List<AiAssitantsInterface> aiAssitantsInterfaces) {
        this.aiAssitantsInterfaces = aiAssitantsInterfaces;
    }

    /**
     * Finds the earliest scheduled token reset among enabled cron jobs in the database.
     *
     * @return next reset as an epoch timestamp in milliseconds, or null when none is scheduled
     */
    public Long getNextTokenReset() {
        String taskClass = AiTokenUsageReset.class.getName();
        ZonedDateTime now = ZonedDateTime.now();
        Long nextReset = null;
        for (CronTask task : CronDB.getCronTasks(taskClass)) {
            if (!task.isEnableTask() || !taskClass.equals(task.getTask())) continue;
            try {
                Long nextRun = getNextTokenReset(task, now);
                if (nextRun != null && (nextReset == null || nextRun < nextReset)) nextReset = nextRun;
            } catch (InvalidPatternException e) {
                Logger.error(AiAssistantsService.class, "Invalid token reset cron schedule " + task.getId() + ": " + e.getMessage());
            }
        }
        return nextReset;
    }

    /**
     * Finds the next run using the scheduler's seven-field matching rules and server time zone.
     * Skips nonmatching days, hours and minutes to avoid scanning every second.
     * The bundled cron parser supports explicit years up to 2100.
     *
     * @param task enabled token reset job
     * @param now time after which to search
     * @return next execution timestamp, or null if no supported future date matches
     */
    private Long getNextTokenReset(CronTask task, ZonedDateTime now) {
        SchedulingPattern pattern = new SchedulingPattern(task.receiveCronPattern());
        SchedulingPattern dates = new SchedulingPattern("* * * " + task.getDaysOfMonth() + " " + task.getMonths() + " " + task.getDaysOfWeek() + " " + task.getYears());
        SchedulingPattern hours = new SchedulingPattern("* * " + task.getHours() + " * * * *");
        SchedulingPattern minutes = new SchedulingPattern("* " + task.getMinutes() + " * * * * *");
        ZonedDateTime candidate = now.truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
        while (candidate.getYear() <= 2100) {
            long timestamp = candidate.toInstant().toEpochMilli();
            if (!dates.match(timestamp)) {
                candidate = candidate.toLocalDate().plusDays(1).atStartOfDay(candidate.getZone());
            } else if (!hours.match(timestamp)) {
                candidate = candidate.plusSeconds(3600 - candidate.getMinute() * 60 - candidate.getSecond());
            } else if (!minutes.match(timestamp)) {
                candidate = candidate.plusSeconds(60 - candidate.getSecond());
            } else if (pattern.match(timestamp)) {
                return timestamp;
            } else {
                candidate = candidate.plusSeconds(1);
            }
        }
        return null;
    }

    /**
     * Get value from column.editor.attr, NEVER returns NULL
     * @param column
     * @param name
     * @return
     */
    private static String getColumnEditorAttr(sk.iway.iwcm.system.datatable.json.DataTableColumn column, String name) {
        if (column.getEditor()!=null && column.getEditor().getAttr() != null && Tools.isNotEmpty(column.getEditor().getAttr().get(name))) {
            return column.getEditor().getAttr().get(name);
        }
        return "";
    }

    /**
     * Finds active, configured AI assistants applicable to the target field.
     *
     * @param fieldTo target entity field name
     * @param column column metadata used to match classes and render formats
     * @param srcClass fully qualified source entity class name
     * @return matching assistant definitions, or an empty list when none apply
     */
    public static List<AssistantDefinitionEntity> getAssistantAndFieldFrom(String fieldTo, sk.iway.iwcm.system.datatable.json.DataTableColumn column, String srcClass) {
        List<AssistantDefinitionEntity> assistants = new ArrayList<>();
        if(Tools.isEmpty(fieldTo) || Tools.isEmpty(srcClass)) return assistants;

        AiAssistantsService aiAssistantsService = Tools.getSpringBean("aiAssistantsService", AiAssistantsService.class);
        if (aiAssistantsService == null) return assistants;

        if (Tools.isNotEmpty(getColumnEditorAttr(column, "disabled"))) return assistants;
        boolean strictBinding = (column.getClassName() != null && column.getClassName().contains("ai-off"))
            || getColumnEditorAttr(column, "class").contains("ai-off");

        List<String> targets = new ArrayList<>();
        targets.add(fieldTo);
        if (!strictBinding) {
            String format = column.getRenderFormat();
            String title = column.getTitleKeyOriginal();
            if (isCustomField(fieldTo) && Tools.isNotEmpty(title)) {
                String type = Prop.getInstance(Constants.getString("defaultLanguage")).getText(title + ".type");
                if ("image".equals(type)) format = "dt-format-image";
                else if ("link".equals(type) || "dir".equals(type)) format = "dt-format-elfinder";
            }
            targets.addAll(Arrays.asList(Tools.getTokens(column.getClassName(), " ", true)));
            targets.addAll(Arrays.asList(Tools.getTokens(format, " ", true)));
        }

        BiPredicate<String, String> matches = strictBinding ? String::equals : AiAssistantsService::isMatching;
        for (AssistantDefinitionEntity assistant : getAssistantsFromDB(null)) {
            if (Tools.isFalse(assistant.getActive()) || !aiAssistantsService.getAssistantStatus(assistant.getProvider())) continue;
            if (Arrays.stream(Tools.getTokens(assistant.getClassName(), "\n,;", true))
                    .noneMatch(binding -> matches.test(binding, srcClass))) continue;
            boolean matchesField = Arrays.stream(Tools.getTokens(assistant.getFieldTo(), "\n,;", true))
                .filter(binding -> !TOP_SEARCH_FIELD.equals(binding))
                .anyMatch(binding -> targets.stream().anyMatch(target -> matches.test(binding, target)));
            if (matchesField) assistants.add(assistant);
        }
        return assistants;
    }

    public List<String> getFieldOptions(String term, String className) {
        return getFieldOptions(term, className, null);
    }

    public List<String> getFieldOptions(String term, String className, String targetAction) {
        List<String> ac = new ArrayList<>();
        if(Tools.isEmpty(term) || Tools.isEmpty(className)) return ac;

        // The editor replaces the entire input when a suggestion is selected.
        int start = 1 + Math.max(term.lastIndexOf(','), Math.max(term.lastIndexOf(';'), term.lastIndexOf('\n')));
        String prefix = term.substring(0, start);
        String search = term.substring(start).trim().toLowerCase(Locale.ROOT);
        if (SupportedActions.LIVE_CHAT.getAction().equals(targetAction) && TOP_SEARCH_FIELD.contains(search)) {
            ac.add(prefix + TOP_SEARCH_FIELD);
        }

        for(String fieldName : getClassFieldMap().getOrDefault(className, List.of())) {
            if(fieldName.toLowerCase(Locale.ROOT).contains(search))
                ac.add(prefix + fieldName);
        }

        return ac;
    }

    /* PUBLIC METHODS */

    public List<String> getClassOptions(String term) {
        List<String> ac = new ArrayList<>();
        if(Tools.isEmpty(term)) return ac;

        if(CHAT_TARGET.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT))) {
            ac.add(CHAT_TARGET);
        }
        for(String className : getClassFieldMap().keySet()) {
            if(className.toLowerCase().contains( term.toLowerCase() ))
                ac.add(className);
        }

        return ac;
    }

    /** Matches explicit global chat bindings in the entity list or the target field list. */
    public static boolean isGlobalChatAssistant(AssistantDefinitionEntity assistant) {
        return assistant != null && SupportedActions.LIVE_CHAT.getAction().equals(assistant.getAction())
            && (Arrays.asList(Tools.getTokens(assistant.getClassName(), "\n,;", true)).contains(CHAT_TARGET)
                || Arrays.asList(Tools.getTokens(assistant.getFieldTo(), "\n,;", true)).contains(TOP_SEARCH_FIELD));
    }

    public void prepareBeforeSave(AssistantDefinitionEntity assistantEnity, Prop prop) {
        if (Tools.isEmpty(assistantEnity.getFieldTo()) && isGlobalChatAssistant(assistantEnity)
            && CHAT_TARGET.equals(assistantEnity.getClassName().trim())) {
            // Keep the required field binding valid without inventing an editor field for global chat.
            assistantEnity.setFieldTo("*");
        }
        configuredProvider(assistantEnity.getProvider(), prop).prepareBeforeSave(assistantEnity);
    }

    public void getProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) {
        aiAssitantsInterfaces.forEach(provider -> provider.setProviderSpecificOptions(page, prop));
    }

    public List<String> getProviderFields(String provider, String action, Prop prop) {
        return configuredProvider(provider, prop).getFieldsToShow(action);
    }

    public boolean getAssistantStatus(String providerId) {
        return Tools.isNotEmpty(providerId) && aiAssitantsInterfaces.stream()
            .filter(provider -> providerId.equals(provider.getProviderId())).findFirst()
            .map(AiAssitantsInterface::isInit).orElse(false);
    }

    private AiAssitantsInterface configuredProvider(String id, Prop prop) {
        return aiAssitantsInterfaces.stream().filter(provider -> provider.getProviderId().equals(id) && provider.isInit())
            .findFirst().orElseThrow(() -> new IllegalStateException(prop.getText("config.not_permitted_action_err")));
    }

    /**
     * Returns the status column's filter options with their matching row icons.
     *
     * @param prop translations for the current user
     * @return localized status options
     */
    public List<LabelValue> getStatusIconOptions(Prop prop) {
        return List.of(
            new LabelValue("<i class=\"ti ti-ban text-danger\" aria-hidden=\"true\"></i> " + prop.getText("components.ai_assistants.status.not_active.label"), "not-active"),
            new LabelValue("<i class=\"ti ti-alert-triangle text-warning\" aria-hidden=\"true\"></i> " + prop.getText("components.ai_assistants.status.not_init.label"), "not-init"),
            new LabelValue("<i class=\"ti ti-gauge-off text-warning\" aria-hidden=\"true\"></i> " + prop.getText("components.ai_assistants.status.out_of_tokens.label"), "out-of-tokens")
        );
    }

    /**
     * Filters assistants before pagination, using the same status precedence as their row icons.
     *
     * @param status selected row status
     * @param predicates search conditions to extend
     * @param root assistant entity in the query
     * @param builder criteria builder
     */
    public void addStatusFilter(String status, List<Predicate> predicates, Root<AssistantDefinitionEntity> root, CriteriaBuilder builder) {
        if ("not-active".equals(status)) {
            predicates.add(builder.isFalse(root.get("active")));
            return;
        }
        if (!"not-init".equals(status) && !"out-of-tokens".equals(status)) return;

        predicates.add(builder.or(builder.isNull(root.get("active")), builder.isTrue(root.get("active"))));
        List<String> initializedProviders = aiAssitantsInterfaces.stream()
            .filter(AiAssitantsInterface::isInit)
            .map(AiAssitantsInterface::getProviderId)
            .toList();
        Predicate initialized = initializedProviders.isEmpty() ? builder.disjunction()
            : builder.and(builder.isNotNull(root.get("provider")), root.get("provider").in(initializedProviders));

        if ("not-init".equals(status)) {
            predicates.add(builder.not(initialized));
        } else {
            predicates.add(initialized);
            predicates.add(builder.greaterThan(root.<Long>get("tokenLimit"), 0L));
            predicates.add(builder.greaterThanOrEqualTo(root.<Long>get("usedTokens"), root.<Long>get("tokenLimit")));
        }
    }

    /* PRIVATE SUPPORT METHODS */
    static boolean isCustomField(String name) {
        return name != null && name.startsWith("field") && name.length() == "fieldX".length();
    }

    @SuppressWarnings("java:S1871")
    protected static boolean isMatching(String text, String fieldName) {
        if (fieldName.equals(text)) return true;
        else if ("*".equals(text)) return true;
        else if (text.startsWith("%") && text.endsWith("!") && text.length()>4 && fieldName.endsWith(text.substring(1, text.length()-1))) return true;
        else if (text.startsWith("%") && text.length()>2 && fieldName.indexOf(text.substring(1))!=-1) return true;
        else if (text.endsWith("!") && text.length()>=2 && fieldName.equals(text.substring(0, text.length()-1))) return true;

        return false;
    }

    private Map<String, List<String>> getClassFieldMap() {
        Cache c = Cache.getInstance();
        Object mapObject = c.getObject(CLASS_FIELD_MAP_KEY);

        if(mapObject == null) {
            Map<String, List<String>> classFieldsMap = new HashMap<>();
            classFieldsMap = findClassesWithAnnotation(classFieldsMap, Entity.class);
            classFieldsMap = findClassesWithAnnotation(classFieldsMap, WebjetAppStore.class);
            c.setObject(CLASS_FIELD_MAP_KEY, classFieldsMap, 60);
            return classFieldsMap;
        } else {
            @SuppressWarnings("unchecked")
            Map<String, List<String>> classFieldsMap = (Map<String, List<String>>) mapObject;
            return classFieldsMap;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, List<String>> findClassesWithAnnotation(Map<String, List<String>> classFieldsMap, Class foundClass) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(foundClass));

        for (String packageName : AppManager.getPackageNames()) {
			for (BeanDefinition beanDef : provider.findCandidateComponents(packageName)) {
                try {

                    String fqdn = beanDef.getBeanClassName();
					if (fqdn == null || classFieldsMap.get(fqdn) != null) { continue; }

                    List<String> fieldsNames = new ArrayList<>();
                    for (Field field : AuditEntityListener.getDeclaredFieldsTwoLevels( Class.forName(fqdn) )) {
                        if (field.isAnnotationPresent(DataTableColumn.class)) {
                            fieldsNames.add( field.getName() );
                        }
                    }

                    classFieldsMap.put(fqdn, fieldsNames);

                } catch (Exception e) {
					Logger.error(AppManager.class, "Got exception: " + e.getMessage());
				}
            }
        }

        return classFieldsMap;
    }

    /**
     * Get assistants from the database, it is heavily used for generating AI button in fields, so it is cached for 60 minutes
     * @param repo
     * @return
     */
    public static List<AssistantDefinitionEntity> getAssistantsFromDB(AssistantDefinitionRepository repo) {

        Cache c = Cache.getInstance();
        String cacheKey = CACHE_KEY_PREFIX + CloudToolsForCore.getDomainId();

        @SuppressWarnings("unchecked")
        List<AssistantDefinitionEntity> cachedAssistants = c.getObject(cacheKey, List.class);
        if(cachedAssistants != null) {
            return cachedAssistants;
        }

        if(repo == null)
            repo = Tools.getSpringBean("assistantDefinitionRepository", AssistantDefinitionRepository.class);

        List<AssistantDefinitionEntity> dbAiAssitants = repo.findAllByDomainId(CloudToolsForCore.getDomainId());

        c.setObject(cacheKey, dbAiAssitants, 60);
        return dbAiAssitants;
    }

    public static void clearCache() {
        Cache c = Cache.getInstance();
        c.removeObject(CACHE_KEY_PREFIX + CloudToolsForCore.getDomainId(), true);
    }

    /**
     * Resolves CMS-owned prompt variables and delegates untrusted prompt expansion to {@code webjet-ai}.
     *
     * @param instructions CMS instruction template
     * @param inputData assistant input and trusted backend variables
     * @return expanded instructions with consumed and suspicious source metadata
     */
    public static AiPromptTemplate.ExpansionResult expandPromptMacros(
        String instructions,
        InputDataDTO inputData
    ) {
        if(inputData == null) {
            return AiPromptTemplate.expand(instructions, null, null, AiAssistantsService::formatPromptValue);
        }
        if(Tools.isEmpty(instructions)) {
            //to fill inputData if original input is empty
            instructions = "{\nuserPrompt:{userPrompt}\ninputText:{inputText}\n}";
        }

        //replace user language
        if (instructions.contains("{userLanguage}")) {
            RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
            if (rb != null) {
                String userLang = rb.getLng();
                Prop prop = Prop.getInstance("en");
                String language = prop.getText("language."+userLang);
                instructions = Tools.replace(instructions, "{userLanguage}", language);
            }
        }
        //replace language (language of last shown page from lng cookie)
        if (instructions.contains("{language}")) {
            RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
            if (rb != null) {
                String userLang = rb.getLngCookie();
                Prop prop = Prop.getInstance("en");
                String language = prop.getText("language."+userLang);
                instructions = Tools.replace(instructions, "{language}", language);
            }
        }

        String inputText = inputData.getInputValue();
        if (InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType())
            || "append".equals(inputData.getReplaceMode())
            || "replace".equals(inputData.getReplaceMode())) {
            inputText = "";
        }
        AiPromptTemplate.ExpansionResult expansion = AiPromptTemplate.expand(
            instructions,
            inputText,
            inputData.getUserPrompt(),
            AiAssistantsService::formatPromptValue
        );
        return expansion.withInstructions(
            applyBonusParamsMacro(expansion.instructions(), inputData.getBonusParams())
        );
    }

    private static String formatPromptValue(String value) {
        if(Tools.isEmpty(value)) return "";

        //remove new lines and escape quotes, we are expecting JSON format for instructions for HTML code/replace in PB
        value = value.replace("\n", " ").replace("\r", " ").replace("\"", "\\\"").replace("'", "\\'");

        return value;
    }

    private static String applyBonusParamsMacro(String instructions, Map<String, String> bonusParams) {

        if(bonusParams == null || bonusParams.isEmpty() || Tools.isEmpty(instructions)) return instructions;

        for (Map.Entry<String, String> entry : bonusParams.entrySet()) {
            String value = entry.getValue();
            if(Tools.isEmpty(value)) continue;

            String macro = "{"+entry.getKey()+"}";
            if (instructions.contains(macro)) {
                instructions = Tools.replace(instructions, macro, value);
            }
        }

        return instructions;
    }

    /**
     * Sends a threshold notification and conditionally advances its marker without locking during delivery.
     * Concurrent calls may deliver duplicate notifications.
     * Usage must already be committed so a notification failure cannot roll back token accounting.
     *
     * @param assistantId assistant whose usage changed
     * @param domainId domain owning the assistant
     * @param prop translations for the request language
     */
    public static void notifyUsage(Long assistantId, Integer domainId, Prop prop) {
        AssistantDefinitionRepository assistants = Tools.getSpringBean("assistantDefinitionRepository", AssistantDefinitionRepository.class);
        AssistantDefinitionEntity assistant = assistants.findForTokenNotification(assistantId, domainId).orElse(null);
        if (assistant == null || Tools.isEmpty(assistant.getNotifEmail())) return;

        long limit = assistant.getTokenLimit() == null ? 0 : assistant.getTokenLimit();
        long used = assistant.getUsedTokens() == null ? 0 : assistant.getUsedTokens();
        if (limit <= 0) return;

        // ceil(limit * 0.75), without floating-point rounding or multiplication overflow.
        int level = used >= limit ? 100 : used >= limit - limit / 4 ? 75 : 0;
        int previousLevel = assistant.getNotifLevel() == null ? 0 : assistant.getNotifLevel();
        if (level <= previousLevel) return;

        String domain = GroupsDB.getInstance().getDomain(domainId);
        String key = "components.ai_assistants.token_notification." + level;
        String senderName = SendMail.getDefaultSenderName("ai", "WebJET CMS");
        String senderEmail = SendMail.getDefaultSenderEmail("ai", Tools.isEmpty(domain) ? null : "no-reply@" + domain);
        String body = prop.getText(key + ".body", Tools.escapeHtml(assistant.getName()),
            Tools.escapeHtml(domain), Long.toString(used), Long.toString(limit));

        String subject = prop.getText(key + ".subject");
        // Handle fallback explicitly so only a confirmed send or queue advances the marker.
        boolean sentOrQueued = SendMail.sendCapturingException(senderName, senderEmail, assistant.getNotifEmail(),
            null, null, null, subject, body, null, null, false).getFirst();
        if (sentOrQueued == false) {
            sentOrQueued = SendMail.sendLater(senderName, senderEmail, assistant.getNotifEmail(),
                null, null, null, subject, body, null, null, null);
        }
        if (sentOrQueued) {
            assistants.updateNotificationLevel(assistantId, domainId, level, limit, used);
        }
    }
}
