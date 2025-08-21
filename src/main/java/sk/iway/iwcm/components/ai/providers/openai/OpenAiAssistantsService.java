package sk.iway.iwcm.components.ai.providers.openai;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@Service
public class OpenAiAssistantsService extends OpenAiSupportService implements AiAssitantsInterface {

    private final CloseableHttpClient client = HttpClients.createDefault();
    private static final String SERVICE_NAME = "OpenAiAssistantsService";
    private static final String PROVIDER_ID = "openai";
    private static final String AUTH_KEY = "open_ai_auth_key";

    public String getProviderId() {
        return PROVIDER_ID;
    }

    public boolean isInit() {
        return Tools.isNotEmpty(Constants.getString(AUTH_KEY));
    }

    public String insertAssistant(AssistantDefinitionEntity assistantEnity, Prop prop) throws IOException {
        JSONObject json = new JSONObject();
        json.put(ASSISTANT_FIELDS.NAME.value(), assistantEnity.getFullName());
        json.put(ASSISTANT_FIELDS.INSTRUCTIONS.value(), assistantEnity.getInstructions());
        json.put(ASSISTANT_FIELDS.MODEL.value(), assistantEnity.getModel());
        json.put(ASSISTANT_FIELDS.TEMPERATURE.value(), assistantEnity.getTemperature());

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

    public List<AssistantDefinitionEntity> getAiAssistants(Prop prop) throws IOException {
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

    public void prepareBeforeSave(AssistantDefinitionEntity assistantEnity) {
        if(assistantEnity.getTemperature() == null ) assistantEnity.setTemperature(BigDecimal.ONE);
        if(assistantEnity.getModel() == null) assistantEnity.setModel("gpt-3.5-turbo");
        if(assistantEnity.getReasoningEffort() == null) assistantEnity.setReasoningEffort("medium");
        if(assistantEnity.getTopP() == null) assistantEnity.setTopP(BigDecimal.ONE);
    }

    public void setProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) {
        //open AI specific options
        page.addOptions("imagesQuality", getQualityOptions(), "label", "value", false);
        page.addOptions("imagesSize", getSizeOptions(), "label", "value", false);
    }

    public List<String> getFieldsToShow(String action) {
        if("create".equals(action)) return List.of("model", "useStreaming", "temperature", "imagesCount", "imagesSize", "imagesQuality");
        else if("edit".equals(action)) return List.of("model", "assistantKey", "useStreaming", "temperature", "imagesCount", "imagesSize", "imagesQuality");
        else return new ArrayList<>();
    }

    private List<LabelValue> getQualityOptions() {
        List<LabelValue> qualityOptions = new ArrayList<>();
        qualityOptions.add(new LabelValue("low", "low"));
        qualityOptions.add(new LabelValue("medium", "medium"));
        qualityOptions.add(new LabelValue("high", "high"));
        return qualityOptions;
    }

    private List<LabelValue> getSizeOptions() {
        List<LabelValue> sizeOptions = new ArrayList<>();
        sizeOptions.add(new LabelValue("auto", "auto"));
        sizeOptions.add(new LabelValue("1024x1024", "1024x1024"));
        sizeOptions.add(new LabelValue("1024x1536", "1024x1536"));
        sizeOptions.add(new LabelValue("1536x1024", "1536x1024"));
        return sizeOptions;
    }
}
