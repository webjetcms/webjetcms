package sk.iway.iwcm.components.ai.providers.openai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
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

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.components.ai.stat.rest.AiStatService;
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


    /*  */

    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo) throws IOException, InterruptedException {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        String assistantId = assistant.getAssistantKey();
        BigDecimal temperature = assistant.getTemperature();

        // 1. Create thread
        String threadId = createThread(prop);

        try {
            // 2. Add message
            addMessage(assistant, threadId, inputData, prop);

            // 3. Create run
            String runId = createRun(threadId, assistantId, temperature, prop);

            // 4. Wait for run to complete
            waitForRunCompletion(threadId, runId, assistant, prop, responseDto, statRepo);

            // 5. Get assistant's reply
            return getLatestMessage(threadId, prop, responseDto);

        } finally {
            // 6. Delete thread (cleanup)
            deleteThread(threadId);
        }
    }

    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, PrintWriter writer) throws IOException, InterruptedException {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        String assistantId = assistant.getAssistantKey();
        BigDecimal temperature = assistant.getTemperature();

        // 1. Create thread
        String threadId = createThread(prop);

        try {
            // 2. Add message
            addMessage(assistant, threadId, inputData, prop);

            // 3. Create run
            JSONObject json = new JSONObject();
            json.put("assistant_id", assistantId);
            json.put("temperature", temperature);
            json.put("stream", true);

            HttpPost post = new HttpPost(THREADS_URL + threadId + "/runs");
            post.setEntity(getRequestBody(json.toString()));
            addHeaders(post, true, true);
            post.setHeader("Accept", "text/event-stream");

            try (CloseableHttpResponse response = client.execute(post)) {

                HttpEntity entity = response.getEntity();
                InputStream inputStream = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                OpenAiStreamHandler streamHandler = new OpenAiStreamHandler(true);
                streamHandler.handleBufferedReader(reader, writer);

                //
                handleUsage(responseDto, streamHandler.getUsageChunk(), assistant, streamHandler.getRunId(), threadId, statRepo);
            }
        } finally {
            // 6. Delete thread (cleanup)
            deleteThread(threadId);
        }

        return responseDto;
    }

    private String createThread(Prop prop) throws IOException {
        HttpPost post = new HttpPost("https://api.openai.com/v1/threads");
        post.setEntity(getRequestBody("{}"));
        addHeaders(post, true, true);
        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "createThread");
            JSONObject res = new JSONObject(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));
            return res.getString("id");
        }
    }

    private void addMessage(AssistantDefinitionEntity assistant, String threadId, InputDataDTO inputData, Prop prop) throws IOException {
        JSONObject json = new JSONObject();
        json.put("role", "user");

        String content = inputData.getInputValue();
        if (Tools.isTrue(assistant.getUserPromptEnabled())) {
            content = AiAssistantsService.executePromptMacro("", inputData);
        }

        json.put("content", content);

        HttpPost post = new HttpPost(THREADS_URL + threadId + "/messages");
        post.setEntity(getRequestBody(json.toString()));
        addHeaders(post, true, true);
        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "addMessage");
        }
    }

    private String createRun(String threadId, String assistantId, BigDecimal temperature, Prop prop) throws IOException {
        JSONObject json = new JSONObject();
        json.put("assistant_id", assistantId);
        json.put("temperature", temperature);
        HttpPost post = new HttpPost(THREADS_URL + threadId + "/runs");
        post.setEntity(getRequestBody(json.toString()));
        addHeaders(post, true, true);
        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "createRun");
            JSONObject res = new JSONObject(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));
            return res.getString("id");
        }
    }

    private void waitForRunCompletion(String threadId, String runId, AssistantDefinitionEntity dbAssitant, Prop prop, AssistantResponseDTO responseDto, AiStatRepository statRepo) throws IOException, InterruptedException {
        while (true) {
            HttpGet get = new HttpGet(THREADS_URL + threadId + "/runs/" + runId);
            addHeaders(get, false, true);
            try (CloseableHttpResponse response = client.execute(get)) {
                if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                    handleErrorMessage(response, prop, SERVICE_NAME, "waitForRunCompletion");
                JSONObject res = new JSONObject(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));
                String status = res.getString("status");
                if ("completed".equals(status)) {

                    handleUsage(responseDto, res, dbAssitant, runId, threadId, statRepo);

                    break;
                }
                if ("failed".equals(status)) throw new RuntimeException("Run failed: " + res);
                Thread.sleep(1000);
            }
        }
    }

    private AssistantResponseDTO getLatestMessage(String threadId, Prop prop, AssistantResponseDTO responseDto) throws IOException {
        HttpGet get = new HttpGet(THREADS_URL + threadId + "/messages");
        addHeaders(get, false, true);
        try (CloseableHttpResponse response = client.execute(get)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "getLatestMessage");
            JSONObject res = new JSONObject(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));
            JSONArray data = res.getJSONArray("data");
            JSONObject firstMessage = data.getJSONObject(0);
            JSONArray contentArray = firstMessage.getJSONArray("content");
            responseDto.setResponse(contentArray.getJSONObject(0).getJSONObject("text").getString("value"));
            return responseDto;
        }
    }

    private void handleUsage(AssistantResponseDTO responseDto, JSONObject source, AssistantDefinitionEntity dbAssitant, String runId, String threadId, AiStatRepository statRepo) {

        if (source.has("usage")) {
            //"total_tokens" "output_tokens" "input_tokens"
            JSONObject usage = source.getJSONObject("usage");
            int promptTokens = usage.optInt("prompt_tokens", 0);
            int completionTokens = usage.optInt("completion_tokens", 0);
            int totalTokens = usage.optInt("total_tokens", 0);
            StringBuilder sb = new StringBuilder("");

            sb.append(SERVICE_NAME).append(" -> run with id: ").append(runId).append(" on thred: ").append(threadId).append(" was succesfull");
            sb.append("\n\n");
            sb.append("Assitant name : ").append(dbAssitant.getName()).append("\n");
            sb.append("From field : ").append(dbAssitant.getFieldFrom()).append("\n");
            sb.append("To field : ").append(dbAssitant.getFieldTo()).append("\n");
            sb.append("\n");
            sb.append("Action cost: \n");
            sb.append("\t prompt_tokens: ").append(promptTokens).append("\n");
            sb.append("\tcompletion_tokens: ").append(completionTokens).append("\n");
            sb.append("\t total_tokens: ").append(totalTokens).append("\n");
            Adminlog.add(Adminlog.TYPE_AI, sb.toString(), totalTokens, -1);

            AiStatService.addRecord(dbAssitant.getName(), totalTokens, statRepo);

            responseDto.setTotalTokens(totalTokens);
        }
    }

    private void deleteThread(String threadId) throws IOException {
        HttpDelete delete = new HttpDelete(THREADS_URL + threadId);
        addHeaders(delete, false, true);
        try (CloseableHttpResponse response = client.execute(delete)) {
        } catch(Exception e) {
            e.printStackTrace();
        }
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