package sk.iway.iwcm.components.ai.providers.openai;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiInterface;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.components.ai.stat.rest.AiStatService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;


@Service
public class OpenAiService extends OpenAiSupportService implements AiInterface {

    private static final CloseableHttpClient client = HttpClients.createDefault();
    private static final String SERVICE_NAME = "OpenAiService";
    private static final String PROVIDER_ID = "openai";
    private static final String TITLE_KEY = "components.ai_provider.openai.title";
    private static final String AUTH_KEY = "open_ai_auth_key";

    public String getProviderId() {
        return PROVIDER_ID;
    }

    public boolean isInit() {
        return Tools.isNotEmpty(Constants.getString(AUTH_KEY));
    }

    public Pair<String, String> getProviderInfo(Prop prop) {
        return new Pair<>(PROVIDER_ID, prop.getText(TITLE_KEY));
    }

    public List<LabelValue> getSupportedModels(Prop prop) {
        List<LabelValue> supportedValues = new ArrayList<>();

        HttpGet get = new HttpGet(MODELS_URL);
        addHeaders(get, true, false);
        try (CloseableHttpResponse response = client.execute(get)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "getSupportedModels");

            String value = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
            if (Tools.isEmpty(value)) return supportedValues;

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(value);
            for (JsonNode model : root.get("data")) {
                String modelId = model.get("id").asText();
                String created = model.get("created").asText();
                supportedValues.add(new LabelValue(modelId, created));
            }
            supportedValues.sort(Comparator.comparingLong((LabelValue o) -> Long.parseLong(o.getValue())).reversed());

            for (LabelValue modelValue : supportedValues) modelValue.setValue(modelValue.getLabel());

            return supportedValues;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return supportedValues;
    }

    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, String content, Prop prop, AiStatRepository statRepo) throws IOException, InterruptedException {

        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        String assistantId = assistant.getAssistantKey();
        BigDecimal temperature = assistant.getTemperature();

        // 1. Create thread
        String threadId = createThread(prop);

        try {
            // 2. Add message
            addMessage(threadId, content, prop);

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

    private void addMessage(String threadId, String prompt, Prop prop) throws IOException {
        JSONObject json = new JSONObject();
        json.put("role", "user");
        json.put("content", prompt);
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
                    if (res.has("usage")) {
                        JSONObject usage = res.getJSONObject("usage");
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

                        responseDto.setPromptTokens(promptTokens);
                        responseDto.setCompletionTokens(completionTokens);
                        responseDto.setTotalTokens(totalTokens);
                    }
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

    private void deleteThread(String threadId) throws IOException {
        HttpDelete delete = new HttpDelete(THREADS_URL + threadId);
        addHeaders(delete, false, true);
        try (CloseableHttpResponse response = client.execute(delete)) {
            // System.out.println("Thread deleted: " + threadId);
            //TODO
        }
    }
}