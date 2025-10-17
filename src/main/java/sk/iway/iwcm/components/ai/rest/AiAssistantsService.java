package sk.iway.iwcm.components.ai.rest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.components.ai.providers.IncludesHandler;
import sk.iway.iwcm.editor.appstore.AppManager;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/**
 * Service for AI Assistants management / datatables, editor
 */
@Service
public class AiAssistantsService {

    private static final String CLASS_FIELD_MAP_KEY = "AiAssistantsService_classFieldsMap";
    public static final String EMPTY_VALUE = "EMPTY_VALUE";
    private static final String CACHE_KEY_PREFIX = "AiAssistantsService.assistants-";

    private final List<AiAssitantsInterface> aiAssitantsInterfaces;

    @Autowired
    public AiAssistantsService(List<AiAssitantsInterface> aiAssitantsInterfaces) {
        this.aiAssitantsInterfaces = aiAssitantsInterfaces;
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
     *
     * @param fieldTo
     * @param srcClass
     * @return Pair<String, String> where -> <aiAssistantName, fromField>
     */
    public static List<AssistantDefinitionEntity> getAssistantAndFieldFrom(String fieldTo, sk.iway.iwcm.system.datatable.json.DataTableColumn column, String srcClass) {
        List<AssistantDefinitionEntity> assistants = new ArrayList<>();
        if(Tools.isEmpty(fieldTo) || Tools.isEmpty(srcClass)) return assistants;

        AiAssistantsService aiAssistantsService = Tools.getSpringBean("aiAssistantsService", AiAssistantsService.class);
        if (aiAssistantsService == null) return assistants;

        //if column.attr has disabled value skip
        String disabled = getColumnEditorAttr(column, "disabled");
        if (Tools.isNotEmpty(disabled)) return assistants;
        boolean strictBinding = false;
        if ((column.getClassName()!=null && column.getClassName().contains("ai-off")) ||
            getColumnEditorAttr(column, "class").contains("ai-off")) {
            strictBinding = true;
        }

        //for custom fields try to detect type and update renderFormat
        String colRenderFormat = column.getRenderFormat();
        if (Tools.isNotEmpty(fieldTo) && fieldTo.startsWith("field") && fieldTo.length()==6) {
            String titleKey = column.getTitleKeyOriginal();
            //use default language for detection
            Prop propType = Prop.getInstance(Constants.getString("defaultLanguage"));
            if (Tools.isNotEmpty(titleKey)) {
                String type = propType.getText(titleKey + ".type");
                if (type.equals("image")) {
                    colRenderFormat = "dt-format-image";
                } else if (type.equals("link")) {
                    colRenderFormat = "dt-format-elfinder";
                } else if (type.equals("dir")) {
                    colRenderFormat = "dt-format-elfinder";
                }
            }
        }

        String[] classNames = Tools.getTokens(column.getClassName(), " ", true);
        String[] renderFormats = Tools.getTokens(colRenderFormat, " ", true);

        for(AssistantDefinitionEntity aiAssistant : getAssistantsFromDB(null)) {

            //get provider and test if is configured
            AiAssitantsInterface provider = aiAssistantsService.getProvider(aiAssistant.getProvider());
            if(provider == null || provider.isInit()==false) continue;

            //assistant is disabled
            if(Tools.isFalse(aiAssistant.getActive())) continue;

            boolean addAssistant = false;
            String[] entityClasses = Tools.getTokens(aiAssistant.getClassName(), "\n,;", true);

            for (String entityClass : entityClasses) {
                boolean isMatchingClass;
                if (strictBinding) isMatchingClass = srcClass.equals(entityClass);
                else isMatchingClass = isMatching(entityClass, srcClass);

                if (isMatchingClass) {
                    String[] toFields = Tools.getTokens(aiAssistant.getFieldTo(), "\n,;", true);
                    for (String field : toFields) {
                        if (strictBinding) {
                            if (field.equals(fieldTo)) {
                                addAssistant = true;
                                break;
                            }
                        } else {
                            if (isMatching(field, fieldTo)) {
                                addAssistant = true;
                                break;
                            }
                            for (String className : classNames) {
                                if (isMatching(field, className)) {
                                    addAssistant = true;
                                    break;
                                }
                            }
                            for (String renderFormat : renderFormats) {
                                if (isMatching(field, renderFormat)) {
                                    addAssistant = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (addAssistant) break;
            }
            if (addAssistant) {
                assistants.add(aiAssistant);
            }
        }
        return assistants;
    }

    public List<String> getFieldOptions(String term, String className) {
        List<String> ac = new ArrayList<>();
        if(Tools.isEmpty(term) || Tools.isEmpty(className)) return ac;

        List<String> fieldNames = getClassFieldMap().get(className);
        if(fieldNames == null) return ac;

        for(String fieldName : fieldNames) {
            if(fieldName.toLowerCase().contains( term.toLowerCase() ))
                ac.add(fieldName);
        }

        return ac;
    }

    /* PUBLIC METHODS */

    public List<String> getClassOptions(String term) {
        List<String> ac = new ArrayList<>();
        if(Tools.isEmpty(term)) return ac;

        for(String className : getClassFieldMap().keySet()) {
            if(className.toLowerCase().contains( term.toLowerCase() ))
                ac.add(className);
        }

        return ac;
    }

    public void prepareBeforeSave(AssistantDefinitionEntity assistantEnity, Prop prop) {
        for(AiAssitantsInterface assistantInterface : aiAssitantsInterfaces) {
            if(assistantInterface.isInit() == true && assistantInterface.getProviderId().equals(assistantEnity.getProvider())) {
                assistantInterface.prepareBeforeSave(assistantEnity);
                return;
            }
        }

        throw new IllegalStateException(getNoPermittedString(prop));
    }

    public void getProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) {
        for(AiAssitantsInterface assistantInterface : aiAssitantsInterfaces) {
            //For all
            assistantInterface.setProviderSpecificOptions(page, prop);
        }
    }

    public List<String> getProviderFields(String provider, String action, Prop prop) {
        for(AiAssitantsInterface assistantInterface : aiAssitantsInterfaces) {
            if(assistantInterface.isInit() == true && assistantInterface.getProviderId().equals(provider)) {
                return assistantInterface.getFieldsToShow(action);
            }
        }

        throw new IllegalStateException(getNoPermittedString(prop));
    }

    public boolean getAssistantStatus(String providerId) {
        if(Tools.isEmpty(providerId)) return false;
        for(AiAssitantsInterface assistantInterface : aiAssitantsInterfaces) {
            if(providerId.equals(assistantInterface.getProviderId()))
                return assistantInterface.isInit();
        }

        return false;
    }

    /* PRIVATE SUPPORT METHODS */
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
        List<AssistantDefinitionEntity> cachedAssistants = (List<AssistantDefinitionEntity>) c.getObject(cacheKey, List.class);
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
        c.removeObject(CACHE_KEY_PREFIX + CloudToolsForCore.getDomainId());
    }

    /**
     * Execute prompt macro, replaces {inputText}, {userPrompt} and adds rule for INCLUDE protected tokens if includes are used
     * @param instructions
     * @param inputData
     * @param replacedIncludes
     * @return
     */
    public static String executePromptMacro(String instructions, InputDataDTO inputData, Map<Integer, String> replacedIncludes) {
        if(inputData == null) return instructions;
        if(Tools.isEmpty(instructions)) {
            //to fill inputData if original input is empty
            instructions = "{\nuserPrompt:{userPrompt}\ninputText:{inputText}\n}";
        }

        if (instructions.contains("{inputText}") || instructions.contains("{userPrompt}")) {

            //for append we must clear inputValue, because it will be duplicated into final result
            if ("append".equals(inputData.getReplaceMode()) || "replace".equals(inputData.getReplaceMode())) inputData.setInputValue("");

            instructions = Tools.replace(instructions, "{inputText}", nvl(inputData.getInputValue(), ""));
            instructions = Tools.replace(instructions, "{userPrompt}", nvl(inputData.getUserPrompt(), ""));

            //clear values, so it will be not appended into final prompt, clear both,
            //because it instructions contains {userPrompt} we expect that it will contain also
            //inputValue if it is required for the action
            inputData.setInputValue("");
            inputData.setUserPrompt("");
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

        if (replacedIncludes != null && replacedIncludes.isEmpty()==false) instructions = IncludesHandler.addProtectedTokenInstructionRule(instructions);

        return instructions;
    }

    private static String nvl(String value, String defaultValue) {
        if(Tools.isEmpty(value)) return defaultValue;

        //remove new lines and escape quotes, we are expecting JSON format for instructions for HTML code/replace in PB
        value = value.replace("\n", " ").replace("\r", " ").replace("\"", "\\\"").replace("'", "\\'");

        return value;
    }

    private String getNoPermittedString(Prop prop) {
        return prop.getText("config.not_permitted_action_err");
    }

    private AiAssitantsInterface getProvider(String provider) {
        for(AiAssitantsInterface assistantInterface : aiAssitantsInterfaces) {
            if(assistantInterface.getProviderId().equals(provider)) {
                return assistantInterface;
            }
        }
        return null;
    }
}
