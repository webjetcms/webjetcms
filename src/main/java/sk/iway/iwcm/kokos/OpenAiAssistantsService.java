package sk.iway.iwcm.kokos;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;

@Service
public class OpenAiAssistantsService {

    private final OkHttpClient client = new OkHttpClient();
    private final String API_KEY;

    @Autowired
    public OpenAiAssistantsService() {
        API_KEY = Constants.getString("open_ai_auth_key");
    }

    public static final String getAssitantPrefix() {
        return "WebJET.CMS-" + CloudToolsForCore.getDomainId() + "-";
    }

    /**
     * LOAD assiatsnts from OpenAI. Filter only WebJET CMS assiatnts for cuyrrent domain.
     * Update or insert them in table (DB) when needed.
     */
    public void syncToTable(OpenAiAssistantsRepository repo) {
        String prefix = getAssitantPrefix();

        List<OpenAiAssistantsEntity> openAiAssitants = getAllAssistants();
        //Filter only our
        {
            List<OpenAiAssistantsEntity> filteredAssitants = new ArrayList<>();
            for(OpenAiAssistantsEntity assistant : openAiAssitants) {
                if(assistant.getFullName().startsWith(prefix))
                    filteredAssitants.add(assistant);
            }

            openAiAssitants = new ArrayList<>( filteredAssitants );
        }

        List<OpenAiAssistantsEntity> tableAiAssitants = repo.findAllByNameLikeAndDomainId(prefix + "%", CloudToolsForCore.getDomainId());

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

            repo.save(openAiAssistant);
        }
    }

    public String insertAssistant(String name, String instruction, BigDecimal temperature, String model) throws IOException {
        if(Tools.isEmpty(API_KEY)) throw new IllegalStateException("OpenAI API key is not set.");

        // Create JSON body
        JSONObject json = new JSONObject();
        json.put("name", name);
        json.put("instructions", instruction);
        json.put("model", model);
        json.put("temperature", temperature);

        RequestBody body = RequestBody.create(
            json.toString(),
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
            .url("https://api.openai.com/v1/assistants")
            .addHeader("Authorization", "Bearer " + API_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("OpenAI-Beta", "assistants=v2")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String err = response.body().string();
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            System.out.println("Response: " + responseBody);

            // Extract assistant_id
            JSONObject responseJson = new JSONObject(responseBody);
            String assistantId = responseJson.getString("id");

            System.out.println("Assistant ID: " + assistantId);

            return assistantId;
        }
    }

    private List<OpenAiAssistantsEntity> getAllAssistants() {
        List<OpenAiAssistantsEntity> items = new ArrayList<>();

        String jsonResponse = getAllAssistantsRequest();

        if(Tools.isEmpty(jsonResponse)) return items;

        JSONObject root = new JSONObject(jsonResponse);
        JSONArray assistants = root.getJSONArray("data");

        for (int i = 0; i < assistants.length(); i++) {
            JSONObject assistant = assistants.getJSONObject(i);
            String name = assistant.getString("name");
            String assistantKey = assistant.getString("id");
            String roleDescription = assistant.getString("instructions");
            String model = assistant.getString("model");
            BigDecimal temperature = assistant.getBigDecimal("temperature");

            Long created = assistant.getLong("created_at");

            OpenAiAssistantsEntity entity = new OpenAiAssistantsEntity();
            entity.setId(-1L);
            entity.setName(name);
            entity.setAssistantKey(assistantKey);
            entity.setRoleDescription(roleDescription);
            entity.setModel(model);
            entity.setTemperature(temperature);
            entity.setCreated( new Date(created) );

            items.add(entity);
        }

        return items;
    }

    private String getAllAssistantsRequest() {
        if(Tools.isEmpty(API_KEY)) throw new IllegalStateException("OpenAI API key is not set.");

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/assistants")
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("OpenAI-Beta", "assistants=v2")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response + "\nBody: " + response.body().string());
            }

            //System.out.println("Assistants list:\n" + response.body().string());

            return response.body().string();
        } catch (IOException ioEx) {
            System.out.println(ioEx.getMessage());
        }

        return null;
    }
}
