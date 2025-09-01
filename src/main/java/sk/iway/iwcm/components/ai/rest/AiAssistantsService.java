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
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.editor.appstore.AppManager;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Service
public class AiAssistantsService {

    private static final String CLASS_FIELD_MAP_KEY = "AiAssistantsService_classFieldsMap";
    public static final String EMPTY_VALUE = "EMPTY_VALUE";

    private final List<AiAssitantsInterface> aiAssitantsInterfaces;

    @Autowired
    public AiAssistantsService(List<AiAssitantsInterface> aiAssitantsInterfaces) {
        this.aiAssitantsInterfaces = aiAssitantsInterfaces;
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
        for(AssistantDefinitionEntity aiAssistant : getAssistantsFromDB(null)) {
            boolean addAssistant = false;
            String[] entityClasses = Tools.getTokens(aiAssistant.getClassName(), "\n,;", true);
            String[] classNames = Tools.getTokens(column.getClassName(), " ", true);
            String[] renderFormats = Tools.getTokens(column.getRenderFormat(), " ", true);
            for (String entityClass : entityClasses) {
                boolean isMatchingClass = isMatching(entityClass, srcClass);

                if (isMatchingClass) {
                    String[] toFields = Tools.getTokens(aiAssistant.getFieldTo(), "\n,;", true);
                    for (String field : toFields) {
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

    /* PRIVATE SUPPORT METHODS */
    @SuppressWarnings("java:S1871")
    private static boolean isMatching(String text, String search) {
        if (search.equals(text)) return true;
        else if ("*".equals(text)) return true;
        else if (text.startsWith("%") && text.endsWith("!") && text.length()>4 && search.endsWith(text.substring(1, text.length()-1))) return true;
        else if (text.startsWith("%") && text.length()>2 && search.indexOf(text.substring(1))!=-1) return true;
        else if (text.endsWith("!") && text.length()>=2 && search.equals(text.substring(0, text.length()-1))) return true;

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

    private static List<AssistantDefinitionEntity> getAssistantsFromDB(AssistantDefinitionRepository repo) {
        if(repo == null)
            repo = Tools.getSpringBean("assistantDefinitionRepository", AssistantDefinitionRepository.class);

        List<AssistantDefinitionEntity> dbAiAssitants = repo.findAllByDomainId(CloudToolsForCore.getDomainId());

        return dbAiAssitants;
    }

    public static String executePromptMacro(String text, InputDataDTO inputData) {
        if(inputData == null) return text;
        if(Tools.isEmpty(text)) {
            //to fill inputData if original input is empty
            text = "{\nuserPrompt:{userPrompt}\ninputText:{inputText}\n}";
        }

        text = Tools.replace(text, "{inputText}", nvl(inputData.getInputValue(), ""));
        text = Tools.replace(text, "{userPrompt}", nvl(inputData.getUserPrompt(), ""));

        return text;
    }

    private static String nvl(String value, String defaultValue) {
        if(Tools.isEmpty(value)) return defaultValue;
        return value;
    }

    private String getNoPermittedString(Prop prop) {
        return prop.getText("config.not_permitted_action_err");
    }
}
