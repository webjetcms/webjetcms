package sk.iway.iwcm.components.ai.providers.openrouter;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.poi.ss.formula.functions.T;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.gemini.GeminiService;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.components.ai.stat.rest.AiStatService;

public abstract class OpenRouterSupportService {

    protected String getServiceName() {
        return "OpenRouter";
    }

    protected static String getApiKey() {
        return Constants.getString("ai_openRouterAuthKey");
    }

    protected <T extends HttpRequestBase> void setHeaders(T http, boolean addContentType) {
        String apiKey = getApiKey();
        if(Tools.isEmpty(apiKey)) throw new IllegalStateException("OpenRouter API key is not set.");
        http.setHeader("Authorization", "Bearer " + apiKey);
        if(addContentType) http.setHeader("Content-Type", "application/json; charset=utf-8");
    }

    public final StringEntity getRequestBody(String stringBody) {
        if(Tools.isEmpty(stringBody) == true) stringBody = "{}";
        return new StringEntity(stringBody, java.nio.charset.StandardCharsets.UTF_8);
    }

    protected JSONObject getBaseMainObject(String... contents) {
        //Main JSON object
        JSONObject mainObject = new JSONObject();
        //Contents Array
        JSONArray messagesArray = new JSONArray();
        //Add parts
        for (String content : contents) if(Tools.isNotEmpty(content)) addMessage(messagesArray, content);

        mainObject.put("messages", messagesArray);
        return mainObject;
    }

    protected void addMessageWithImage(JSONArray messagesArray, String value, String format, String base64Img) {
        JSONObject imageMsg = new JSONObject()
            .put("role", "user");

        JSONObject imageUrl = new JSONObject()
            .put("url", "data:" + format + ";base64," + base64Img);

         JSONArray content = new JSONArray()
            .put(
                new JSONObject()
                    .put("type", "image_url")
                    .put("image_url", imageUrl)
            );

        imageMsg.put("content", content);
        messagesArray.put(imageMsg);

        addMessage(messagesArray, value);
    }

    protected void addMessage(JSONArray messagesArray, String value) {
        JSONObject message = new JSONObject()
            .put("role", "user")
            .put("content", value);

        messagesArray.put(message);
    }

    protected void handleUsage(AssistantResponseDTO responseDto, AssistantDefinitionEntity dbAssitant, AiStatRepository statRepo, HttpServletRequest request, JsonNode root) {
        if(root.has("usage")) {
            JsonNode usage = root.path("usage");
            int totalTokens = usage.path("total_tokens").asInt(0);
            handleUsage(responseDto, dbAssitant, statRepo, request, totalTokens);
        }
    }

    protected void handleUsage(AssistantResponseDTO responseDto, AssistantDefinitionEntity dbAssitant, AiStatRepository statRepo, HttpServletRequest request, JSONObject json) {
        if(json.has("usage")) {
            int totalTokens = json.getJSONObject("usage").getInt("total_tokens");
            handleUsage(responseDto, dbAssitant, statRepo, request, totalTokens);
        }
    }

    protected void handleUsage(AssistantResponseDTO responseDto, AssistantDefinitionEntity dbAssitant, AiStatRepository statRepo, HttpServletRequest request, int totalTokens) {
        StringBuilder sb = new StringBuilder("");
        sb.append(getServiceName()).append(" -> run was succesfull");
        sb.append("\n\n");
        sb.append("Assitant name : ").append(dbAssitant.getName()).append("\n");
        sb.append("From field : ").append(dbAssitant.getFieldFrom()).append("\n");
        sb.append("To field : ").append(dbAssitant.getFieldTo()).append("\n");
        sb.append("\n");
        sb.append("Action cost: \n");
        sb.append("\t totalTokenCount: ").append(totalTokens).append("\n");

        Adminlog.add(Adminlog.TYPE_AI, sb.toString(), totalTokens, -1);

        AiStatService.addRecord(dbAssitant.getId(), totalTokens, statRepo, request);

        responseDto.setTotalTokens(totalTokens);
    }

    protected JSONArray getImages(JSONObject json) {
        JSONArray choices = json.getJSONArray("choices");
        JSONObject firstChoice = choices.getJSONObject(0);
        JSONObject message = firstChoice.getJSONObject("message");
        return message.getJSONArray("images");
    }

    protected String getFinishReason(JSONObject json) {
        try {
            JSONArray choices = json.getJSONArray("choices");
            JSONObject firstChoice = choices.getJSONObject(0);
            return firstChoice.getString("finish_reason");
        } catch(Exception e) {
            Logger.error(GeminiService.class, "Fetting finish_reason : " + e);
            return "";
        }
    }
}
