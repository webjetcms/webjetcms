package sk.iway.iwcm.kokos;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import sk.iway.Html2Text;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@Service
public class OpenAiService extends OpenAiSupportService {

    private static final CloseableHttpClient client = HttpClients.createDefault();
    private static final String CHACHE_KEY_MODELS = "OPENAI_MODELS";
    private static final int CACHE_MODELS_TIME = 24 * 60;
    private static final String SERVICE_NAME = "OpenAiService";

    public List<LabelValue> getSupportedModels(Prop prop) {
        List<LabelValue> supportedValues = new ArrayList<>();

        try {
            //First check, if they are cached
            Cache c = Cache.getInstance();
            @SuppressWarnings("unchecked")
            List<LabelValue> cachedModels = (List<LabelValue>)c.getObject(CHACHE_KEY_MODELS);
            if(cachedModels != null) return cachedModels;
        } catch (Exception e) {
            //LOGG - and try get from openAI
        }

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
            try {
                Cache c = Cache.getInstance();
                c.setObject(CHACHE_KEY_MODELS, supportedValues, CACHE_MODELS_TIME);
            } catch (Exception e) {
                //LOGG -
            }
            return supportedValues;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return supportedValues;
    }


    public String getAiResponse(String assistantName, String content, Prop prop) throws IOException, InterruptedException {

        if(Tools.isEmpty(assistantName)) throw new IllegalStateException("No assistant found.");

        if(Tools.isEmpty(content)) throw new IllegalStateException("No content provided for assistant.");

        OpenAiAssistantsRepository repo = Tools.getSpringBean("openAiAssistantsRepository", OpenAiAssistantsRepository.class);
        if(repo == null) throw new IllegalStateException("Something went wrong.");

        String prefix = OpenAiAssistantsService.getAssitantPrefix();
        Optional<OpenAiAssistantsEntity> assistant = repo.findFirstByNameAndDomainId(prefix + assistantName, CloudToolsForCore.getDomainId());

        if(assistant.isPresent() == false) throw new IllegalStateException("No assistant found.");

        String assistantId = assistant.get().getAssistantKey();
        BigDecimal temperature = assistant.get().getTemperature();
        Boolean keepHtml = assistant.get().getKeepHtml();

        if(Tools.isFalse(keepHtml)) {
            Html2Text html2Text = new Html2Text(content);
            content = html2Text.getText();
        }

        // 1. Create thread
        String threadId = createThread(prop);

        try {
            // 2. Add message
            addMessage(threadId, content, prop);

            // 3. Create run
            String runId = createRun(threadId, assistantId, temperature, prop);

            // 4. Wait for run to complete
            waitForRunCompletion(threadId, runId, assistant.get(), prop);

            // 5. Get assistant's reply
            return getLatestMessage(threadId, prop);

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

    private void waitForRunCompletion(String threadId, String runId, OpenAiAssistantsEntity dbAssitant, Prop prop) throws IOException, InterruptedException {
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
                    }
                    break;
                }
                if ("failed".equals(status)) throw new RuntimeException("Run failed: " + res);
                Thread.sleep(1000);
            }
        }
    }

    private String getLatestMessage(String threadId, Prop prop) throws IOException {
        HttpGet get = new HttpGet(THREADS_URL + threadId + "/messages");
        addHeaders(get, false, true);
        try (CloseableHttpResponse response = client.execute(get)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "getLatestMessage");
            JSONObject res = new JSONObject(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));
            JSONArray data = res.getJSONArray("data");
            JSONObject firstMessage = data.getJSONObject(0);
            JSONArray contentArray = firstMessage.getJSONArray("content");
            return contentArray.getJSONObject(0).getJSONObject("text").getString("value");
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