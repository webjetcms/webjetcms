package sk.iway.iwcm.kokos;

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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.editor.appstore.AppManager;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.utils.Pair;

@Service
public class OpenAiAssistantsService extends OpenAiSupportService {

    private final OkHttpClient client = new OkHttpClient();
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
    public static Pair<String, String> getAssistantAndFieldFrom(String fieldTo, String srcClass) {
        for(OpenAiAssistantsEntity aiAssistant : getAssistantsFromDB(null)) {
            if(aiAssistant.getClassName().equals(srcClass) && aiAssistant.getFieldTo().equals(fieldTo)) {
                return new Pair<>(aiAssistant.getName(), aiAssistant.getFieldFrom());
            }
        }

        return null;
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
    public void syncToTable(OpenAiAssistantsRepository repo, Prop prop) throws IOException {
        List<OpenAiAssistantsEntity> openAiAssitants = getAssistantsFromOpenAI(prop);
        //Filter only our
        openAiAssitants = filterAssistants(openAiAssitants);

        List<OpenAiAssistantsEntity> tableAiAssitants = getAssistantsFromDB(repo);

        //Check if openAi assiatnt is table -> YES(update), NO(insert)
        for(OpenAiAssistantsEntity openAiAssistant : openAiAssitants) {
            Long recordId = null;

            for(OpenAiAssistantsEntity tableAiAssitant : tableAiAssitants) {
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

    private List<OpenAiAssistantsEntity> filterAssistants(List<OpenAiAssistantsEntity> assistantsList) {
        String prefix = getAssitantPrefix();
        List<OpenAiAssistantsEntity> filteredAssitants = new ArrayList<>();

        for(OpenAiAssistantsEntity assistant : assistantsList) {
            if(assistant.getFullName().startsWith(prefix))
                filteredAssitants.add(assistant);
        }

        return filteredAssitants;
    }

    public String insertAssistant(OpenAiAssistantsEntity entity, Prop prop) throws IOException {
        // Create JSON body
        JSONObject json = new JSONObject();
        json.put(ASSISTANT_FIELDS.NAME.value(), entity.getFullName());
        json.put(ASSISTANT_FIELDS.INSTRUCTIONS.value(), entity.getInstructions());
        json.put(ASSISTANT_FIELDS.MODEL.value(), entity.getModel());
        json.put(ASSISTANT_FIELDS.TEMPERATURE.value(), entity.getTemperature());

        RequestBody body = RequestBody.create(
            json.toString(),
            MediaType.parse("application/json")
        );

        Request.Builder builder = new Request.Builder().url(ASSISTANTS_URL).post(body);
        addHeaders(builder, true, true);

        try (Response response = client.newCall(builder.build()).execute()) {
            if(response.isSuccessful() == false)
                handleErrorMessage(response, prop, SERVICE_NAME, "insertAssistant");

            String responseBody = response.body().string();

            // Extract assistant_id
            JSONObject responseJson = new JSONObject(responseBody);
            String assistantId = responseJson.getString("id");

            return assistantId;
        }
    }

    public void updateAssistant(OpenAiAssistantsEntity assistantEnity, Prop prop) throws IOException {
        JSONObject json = new JSONObject();
        json.put(ASSISTANT_FIELDS.NAME.value(), assistantEnity.getFullName());
        json.put(ASSISTANT_FIELDS.INSTRUCTIONS.value(), assistantEnity.getInstructions());
        json.put(ASSISTANT_FIELDS.MODEL.value(), assistantEnity.getModel());
        json.put(ASSISTANT_FIELDS.TEMPERATURE.value(), assistantEnity.getTemperature());
        json.put(ASSISTANT_FIELDS.DESCRIPTION.value(), assistantEnity.getDescription());

        Request.Builder builder = new Request.Builder()
            .url(ASSISTANTS_URL + "/" + assistantEnity.getAssistantKey())
            .post(getRequestBody(json.toString()));

        addHeaders(builder, true, true);

        try (Response response = client.newCall(builder.build()).execute()) {
            if(response.isSuccessful() == false)
                handleErrorMessage(response, prop, SERVICE_NAME, "updateAssistant");
        }
    }

    public void deleteAssistant(OpenAiAssistantsEntity assistantEnity, Prop prop) throws IOException {
        Request.Builder builder = new Request.Builder()
            .url(ASSISTANTS_URL + "/" + assistantEnity.getAssistantKey())
            .delete();

        addHeaders(builder, false, true);

        try (Response response = client.newCall(builder.build()).execute()) {
            if(response.isSuccessful() == false)
                handleErrorMessage(response, prop, SERVICE_NAME, "deleteAssistant");
        }
    }

    private static List<OpenAiAssistantsEntity> getAssistantsFromDB(OpenAiAssistantsRepository repo) {
        int domainId = CloudToolsForCore.getDomainId();

        Cache c = Cache.getInstance();
        Object assistantsObj = c.getObject(ALL_ASSISTANTS_KEY + "_" + domainId);
        if(assistantsObj != null) {
            //We have cached list for this domain, return it
            @SuppressWarnings("unchecked")
            List<OpenAiAssistantsEntity> items = (List<OpenAiAssistantsEntity>) assistantsObj;
            return items;
        } else {
            //Load them form DB
            String prefix = getAssitantPrefix();

            if(repo == null)
                repo = Tools.getSpringBean("openAiAssistantsRepository", OpenAiAssistantsRepository.class);

            List<OpenAiAssistantsEntity> tableAiAssitants = repo.findAllByNameLikeAndDomainId(prefix + "%", CloudToolsForCore.getDomainId());

            //Cache list of assistants
            c.setObject(ALL_ASSISTANTS_KEY + "_" + domainId, tableAiAssitants, 60);
            return tableAiAssitants;
        }
    }

    public static void removeAssistantsFromCache() {
        Cache c = Cache.getInstance();
        c.removeObject(ALL_ASSISTANTS_KEY + "_" + CloudToolsForCore.getDomainId());
    }

    private List<OpenAiAssistantsEntity> getAssistantsFromOpenAI(Prop prop) throws IOException {
        List<OpenAiAssistantsEntity> items = new ArrayList<>();

        String jsonResponse = getAllAssistantsRequest(prop);

        if(Tools.isEmpty(jsonResponse)) return items;

        JSONObject root = new JSONObject(jsonResponse);
        JSONArray assistants = root.getJSONArray("data");

        for (int i = 0; i < assistants.length(); i++) {
            JSONObject assistant = assistants.getJSONObject(i);
            OpenAiAssistantsEntity entity = new OpenAiAssistantsEntity();
            entity.setId(-1L);
            entity.setName( getValue(assistant, ASSISTANT_FIELDS.NAME) );
            entity.setAssistantKey( getValue(assistant, ASSISTANT_FIELDS.ID) );
            entity.setInstructions( getValue(assistant, ASSISTANT_FIELDS.INSTRUCTIONS) );
            entity.setModel( getValue(assistant, ASSISTANT_FIELDS.MODEL) );
            entity.setTemperature( getValue(assistant, ASSISTANT_FIELDS.TEMPERATURE) );
            entity.setCreated( getValue(assistant, ASSISTANT_FIELDS.CREATED_AT) );
            entity.setDescription( getValue(assistant, ASSISTANT_FIELDS.DESCRIPTION) );

            items.add(entity);
        }

        return items;
    }

    private String getAllAssistantsRequest(Prop prop) throws IOException{
        Request.Builder builder = new Request.Builder().url(ASSISTANTS_URL).get();
        addHeaders(builder, false, true);

        try (Response response = client.newCall(builder.build()).execute()) {
            if(response.isSuccessful() == false)
                handleErrorMessage(response, prop, SERVICE_NAME, "getAllAssistantsRequest");

            return response.body().string();
        }
    }
}
