package sk.iway.iwcm.kokos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sk.iway.Html2Text;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@Service
public class OpenAiService {

    private static final OkHttpClient client = new OkHttpClient();
    private static final String CHACHE_KEY_MODELS = "OPENAI_MODELS";
    private static final int CACHE_MODELS_TIME = 24 * 60;

    private final String API_KEY;

    @Autowired
    public OpenAiService() {
        API_KEY = Constants.getString("open_ai_auth_key");
    }

    public List<LabelValue> getSupportedModels() {
        List<LabelValue> supportedValues = new ArrayList<>();

        if(Tools.isEmpty(API_KEY)) throw new IllegalStateException("OpenAI API key is not set.");

        try {
            //First check, if they are cached
            Cache c = Cache.getInstance();
            @SuppressWarnings("unchecked")
            List<LabelValue> cachedModels = (List<LabelValue>)c.getObject(CHACHE_KEY_MODELS);
            if(cachedModels != null) return cachedModels;
        } catch (Exception e) {
            //LOGG - and try get from openAI
        }

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/models")
                .get()
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String value = response.body().string();

            if(Tools.isEmpty(value)) return supportedValues;

            // Parse the JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(value);

            // Loop over the "data" array and print only IDs
            for (JsonNode model : root.get("data")) {
                //System.out.println(model.get("id").asText());

                String modelId = model.get("id").asText();
                String created = model.get("created").asText();

                //TMP set created as Value so we can sort by it
                supportedValues.add( new LabelValue(modelId, created) );
            }

            //Sort them
            supportedValues.sort(Comparator.comparingLong((LabelValue o) -> Long.parseLong(o.getValue())).reversed());

            //chnage value
            for(LabelValue modelValue : supportedValues) modelValue.setValue( modelValue.getLabel() );

            try {
                //Chache them
                Cache c = Cache.getInstance();
                c.setObject(CHACHE_KEY_MODELS, supportedValues, CACHE_MODELS_TIME);
            } catch(Exception e) {
                //LOGG -
            }

            return supportedValues;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return supportedValues;
    }


    public String getAiResponse(String assistantName, String content) throws IOException, InterruptedException {

        if(Tools.isEmpty(assistantName)) throw new IllegalStateException("No assistant found.");

        OpenAiAssistantsRepository repo = Tools.getSpringBean("openAiAssistantsRepository", OpenAiAssistantsRepository.class);
        if(repo == null) throw new IllegalStateException("Something went wrong.");

        String prefix = OpenAiAssistantsService.getAssitantPrefix();
        Optional<OpenAiAssistantsEntity> assistant = repo.findFirstByNameAndDomainId(prefix + assistantName, CloudToolsForCore.getDomainId());

        String assistantId = null;
        if(assistant.isPresent()) {
            assistantId = assistant.get().getAssistantKey();
        } else {
            throw new IllegalStateException("No assistant found.");
        }

        if(Tools.isEmpty(content)) {
            return "No content provided to generate perex.";
        }

        Html2Text html2Text = new Html2Text(content);

        // 1. Create thread
        String threadId = createThread();

        try {
            // 2. Add message
            addMessage(threadId, html2Text.getText());

            // 3. Create run
            String runId = createRun(threadId, assistantId);

            // 4. Wait for run to complete
            waitForRunCompletion(threadId, runId);

            // 5. Get assistant's reply
            return getLatestMessage(threadId);

        } finally {
            // 6. Delete thread (cleanup)
            deleteThread(threadId);
        }
    }

    private String createThread() throws IOException {
        if(Tools.isEmpty(API_KEY)) throw new IllegalStateException("OpenAI API key is not set.");

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/threads")
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .post(RequestBody.create("{}", MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            JSONObject res = new JSONObject(response.body().string());
            return res.getString("id");
        }
    }

    private void addMessage(String threadId, String prompt) throws IOException {
        if(Tools.isEmpty(API_KEY)) throw new IllegalStateException("OpenAI API key is not set.");

        JSONObject json = new JSONObject();
        json.put("role", "user");
        json.put("content", prompt);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/threads/" + threadId + "/messages")
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException(response.body().string());
        }
    }

    private String createRun(String threadId, String assistantId) throws IOException {
        if(Tools.isEmpty(API_KEY)) throw new IllegalStateException("OpenAI API key is not set.");

        JSONObject json = new JSONObject();
        json.put("assistant_id", assistantId);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/threads/" + threadId + "/runs")
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("OpenAI-Beta", "assistants=v2")
                .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            JSONObject res = new JSONObject(response.body().string());
            return res.getString("id");
        }
    }

    private void waitForRunCompletion(String threadId, String runId) throws IOException, InterruptedException {
        if(Tools.isEmpty(API_KEY)) throw new IllegalStateException("OpenAI API key is not set.");

        while (true) {
            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/threads/" + threadId + "/runs/" + runId)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("OpenAI-Beta", "assistants=v2")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                JSONObject res = new JSONObject(response.body().string());
                String status = res.getString("status");

                if ("completed".equals(status)) break;
                if ("failed".equals(status)) throw new RuntimeException("Run failed: " + res);

                Thread.sleep(1000);
            }
        }
    }

    private String getLatestMessage(String threadId) throws IOException {
        if(Tools.isEmpty(API_KEY)) throw new IllegalStateException("OpenAI API key is not set.");

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/threads/" + threadId + "/messages")
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("OpenAI-Beta", "assistants=v2")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            JSONObject res = new JSONObject(response.body().string());
            JSONArray data = res.getJSONArray("data");

            JSONObject firstMessage = data.getJSONObject(0);
            JSONArray contentArray = firstMessage.getJSONArray("content");
            return contentArray.getJSONObject(0).getJSONObject("text").getString("value");
        }
    }

    private void deleteThread(String threadId) throws IOException {
        if(Tools.isEmpty(API_KEY)) throw new IllegalStateException("OpenAI API key is not set.");

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/threads/" + threadId)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("OpenAI-Beta", "assistants=v2")
                .delete()
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("Thread deleted: " + threadId);
        }
    }
}