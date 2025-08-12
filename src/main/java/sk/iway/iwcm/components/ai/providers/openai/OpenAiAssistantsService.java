package sk.iway.iwcm.components.ai.providers.openai;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.editor.appstore.AppManager;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Service
public class OpenAiAssistantsService extends OpenAiSupportService {

    private final CloseableHttpClient client = HttpClients.createDefault();
    private static final String CLASS_FIELD_MAP_KEY = "OpenAiAssistantsService_classFieldsMap";
    private static final String ALL_ASSISTANTS_KEY = "OpenAiAssistantsService_allAssistants";
    private static final String SERVICE_NAME = "OpenAiAssistantsService";

    public static final String getAssitantPrefix() {
        return "WebJET.CMS-" + CloudToolsForCore.getDomainId() + "-";
    }

    /**
     *
     * @param fieldTo
     * @param srcClass
     * @return Pair<String, String> where -> <aiAssistantName, fromField>
     */
    public static List<AssistantDefinitionEntity> getAssistantAndFieldFrom(String fieldTo, String srcClass) {
        List<AssistantDefinitionEntity> assistants = new ArrayList<>();
        if(Tools.isEmpty(fieldTo) || Tools.isEmpty(srcClass)) return assistants;
        for(AssistantDefinitionEntity aiAssistant : getAssistantsFromDB(null)) {
            boolean addAssistant = false;
            String[] entityClasses = Tools.getTokens(aiAssistant.getClassName(), "\n,;", true);
            for (String entityClass : entityClasses) {
                boolean isMatchingClass = isMatching(entityClass, srcClass);

                if (isMatchingClass) {
                    String[] toFields = Tools.getTokens(aiAssistant.getFieldTo(), "\n,;", true);
                    for (String field : toFields) {
                        if (isMatching(field, fieldTo)) {
                            addAssistant = true;
                            break;
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

    private static boolean isMatching(String text, String search) {
        if (search.equals(text)) return true;
        else if ("*".equals(text)) return true;
        else if (text.startsWith("%") && text.endsWith("!") && text.length()>4 && search.endsWith(text.substring(1, text.length()-1))) return true;
        else if (text.startsWith("%") && text.length()>2 && search.indexOf(text.substring(1))!=-1) return true;
        else if (text.endsWith("!") && text.length()>=2 && search.equals(text.substring(0, text.length()-1))) return true;

        return false;
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

    public List<String> getClassOptions(String term) {
        List<String> ac = new ArrayList<>();
        if(Tools.isEmpty(term)) return ac;

        for(String className : getClassFieldMap().keySet()) {
            if(className.toLowerCase().contains( term.toLowerCase() ))
                ac.add(className);
        }

        return ac;
    }

    private Map<String, List<String>> getClassFieldMap() {
        Cache c = Cache.getInstance();
        Object mapObject = c.getObject(CLASS_FIELD_MAP_KEY);

        if(mapObject == null) {
            Map<String, List<String>> classFieldsMap = new HashMap<>();
            classFieldsMap = thisShit(classFieldsMap, Entity.class);
            classFieldsMap = thisShit(classFieldsMap, WebjetAppStore.class);
            c.setObject(CLASS_FIELD_MAP_KEY, classFieldsMap, 60);
            return classFieldsMap;
        } else {
            @SuppressWarnings("unchecked")
            Map<String, List<String>> classFieldsMap = (Map<String, List<String>>) mapObject;
            return classFieldsMap;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, List<String>> thisShit(Map<String, List<String>> classFieldsMap, Class foundClass) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(foundClass));

        for (String packageName : AppManager.getPackageNames()) {
			for (BeanDefinition beanDef : provider.findCandidateComponents(packageName)) {
                try {

                    String fqdn = beanDef.getBeanClassName();
					if (fqdn == null || classFieldsMap.get(fqdn) != null) continue;

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
     * LOAD assiatsnts from OpenAI. Filter only WebJET CMS assiatnts for cuyrrent domain.
     * Update or insert them in table (DB) when needed.
     */
    public void syncToTable(AssistantDefinitionRepository repo, Prop prop) throws IOException {
        List<AssistantDefinitionEntity> openAiAssitants = getAssistantsFromOpenAI(prop);
        //Filter only our
        openAiAssitants = filterAssistants(openAiAssitants);

        List<AssistantDefinitionEntity> tableAiAssitants = getAssistantsFromDB(repo);

        //Check if openAi assiatnt is table -> YES(update), NO(insert)
        for(AssistantDefinitionEntity openAiAssistant : openAiAssitants) {
            Long recordId = null;

            for(AssistantDefinitionEntity tableAiAssitant : tableAiAssitants) {
                if(tableAiAssitant.getFullName().equalsIgnoreCase( openAiAssistant.getFullName() )) {
                    recordId = tableAiAssitant.getId();
                    break;
                }
            }

            openAiAssistant.setId(recordId);
            openAiAssistant.setDomainId(CloudToolsForCore.getDomainId());
            openAiAssistant.setClassName(EMPTY_VALUE);
            openAiAssistant.setFieldFrom(EMPTY_VALUE);
            openAiAssistant.setFieldTo(EMPTY_VALUE);

            repo.save(openAiAssistant);
        }
    }

    private List<AssistantDefinitionEntity> filterAssistants(List<AssistantDefinitionEntity> assistantsList) {
        String prefix = getAssitantPrefix();
        List<AssistantDefinitionEntity> filteredAssitants = new ArrayList<>();

        for(AssistantDefinitionEntity assistant : assistantsList) {
            if(assistant.getFullName().startsWith(prefix))
                filteredAssitants.add(assistant);
        }

        return filteredAssitants;
    }

    public String insertAssistant(AssistantDefinitionEntity entity, Prop prop) throws IOException {
        JSONObject json = new JSONObject();
        json.put(ASSISTANT_FIELDS.NAME.value(), entity.getFullName());
        json.put(ASSISTANT_FIELDS.INSTRUCTIONS.value(), entity.getInstructions());
        json.put(ASSISTANT_FIELDS.MODEL.value(), entity.getModel());
        json.put(ASSISTANT_FIELDS.TEMPERATURE.value(), entity.getTemperature());

        HttpPost post = new HttpPost(ASSISTANTS_URL);
        post.setEntity(getRequestBody(json.toString()));
        addHeaders(post, true, true);

        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "insertAssistant");
            String responseBody = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
            JSONObject responseJson = new JSONObject(responseBody);
            String assistantId = responseJson.getString("id");
            return assistantId;
        }
    }

    public void updateAssistant(AssistantDefinitionEntity assistantEnity, Prop prop) throws IOException {
        JSONObject json = new JSONObject();
        json.put(ASSISTANT_FIELDS.NAME.value(), assistantEnity.getFullName());
        json.put(ASSISTANT_FIELDS.INSTRUCTIONS.value(), assistantEnity.getInstructions());
        json.put(ASSISTANT_FIELDS.MODEL.value(), assistantEnity.getModel());
        json.put(ASSISTANT_FIELDS.TEMPERATURE.value(), assistantEnity.getTemperature());
        json.put(ASSISTANT_FIELDS.DESCRIPTION.value(), assistantEnity.getDescription());

        HttpPost post = new HttpPost(ASSISTANTS_URL + "/" + assistantEnity.getAssistantKey());
        post.setEntity(getRequestBody(json.toString()));
        addHeaders(post, true, true);

        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "updateAssistant");
        }
    }

    public void deleteAssistant(AssistantDefinitionEntity assistantEnity, Prop prop) throws IOException {
        HttpDelete delete = new HttpDelete(ASSISTANTS_URL + "/" + assistantEnity.getAssistantKey());
        addHeaders(delete, false, true);
        try (CloseableHttpResponse response = client.execute(delete)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "deleteAssistant");
        }
    }

    private static List<AssistantDefinitionEntity> getAssistantsFromDB(AssistantDefinitionRepository repo) {
        int domainId = CloudToolsForCore.getDomainId();

        Cache c = Cache.getInstance();
        Object assistantsObj = c.getObject(ALL_ASSISTANTS_KEY + "_" + domainId);
        if(assistantsObj != null) {
            //We have cached list for this domain, return it
            @SuppressWarnings("unchecked")
            List<AssistantDefinitionEntity> items = (List<AssistantDefinitionEntity>) assistantsObj;
            return items;
        } else {
            //Load them form DB
            String prefix = getAssitantPrefix();

            if(repo == null)
                repo = Tools.getSpringBean("assistantDefinitionRepository", AssistantDefinitionRepository.class);

            List<AssistantDefinitionEntity> tableAiAssitants = repo.findAllByNameLikeAndDomainId(prefix + "%", CloudToolsForCore.getDomainId());

            //Cache list of assistants
            c.setObject(ALL_ASSISTANTS_KEY + "_" + domainId, tableAiAssitants, 60);
            return tableAiAssitants;
        }
    }

    public static void removeAssistantsFromCache() {
        Cache c = Cache.getInstance();
        c.removeObject(ALL_ASSISTANTS_KEY + "_" + CloudToolsForCore.getDomainId());
    }

    private List<AssistantDefinitionEntity> getAssistantsFromOpenAI(Prop prop) throws IOException {
        List<AssistantDefinitionEntity> items = new ArrayList<>();
        String jsonResponse = getAllAssistantsRequest(prop);
        if (Tools.isEmpty(jsonResponse)) return items;
        JSONObject root = new JSONObject(jsonResponse);
        JSONArray assistants = root.getJSONArray("data");
        for (int i = 0; i < assistants.length(); i++) {
            JSONObject assistant = assistants.getJSONObject(i);
            AssistantDefinitionEntity entity = new AssistantDefinitionEntity();
            entity.setId(-1L);
            entity.setName(getValue(assistant, ASSISTANT_FIELDS.NAME));
            entity.setAssistantKey(getValue(assistant, ASSISTANT_FIELDS.ID));
            entity.setInstructions(getValue(assistant, ASSISTANT_FIELDS.INSTRUCTIONS));
            entity.setModel(getValue(assistant, ASSISTANT_FIELDS.MODEL));
            entity.setTemperature(getValue(assistant, ASSISTANT_FIELDS.TEMPERATURE));
            entity.setCreated(getValue(assistant, ASSISTANT_FIELDS.CREATED_AT));
            entity.setDescription(getValue(assistant, ASSISTANT_FIELDS.DESCRIPTION));
            items.add(entity);
        }
        return items;
    }

    private String getAllAssistantsRequest(Prop prop) throws IOException {
        HttpGet get = new HttpGet(ASSISTANTS_URL);
        addHeaders(get, false, true);
        try (CloseableHttpResponse response = client.execute(get)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "getAllAssistantsRequest");
            return EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
