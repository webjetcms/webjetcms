package sk.iway.iwcm.components.ai.providers.gemini;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONArray;
import org.json.JSONObject;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.components.ai.stat.rest.AiStatService;

public class GeminiSupportService {

    protected static final String AUTH_KEY = "ai_gemini_auth_key";
    protected static final String SERVICE_NAME = "GeminiService";

    protected void handleUsage(AssistantResponseDTO responseDto, JSONObject source, AssistantDefinitionEntity dbAssitant, AiStatRepository statRepo, HttpServletRequest request) {
        if (source.has("usageMetadata")) {
            JSONObject usage = source.getJSONObject("usageMetadata");
            handleUsage(responseDto, dbAssitant, statRepo, request,
                usage.optInt("candidatesTokenCount", 0),
                usage.optInt("thoughtsTokenCount", 0),
                usage.optInt("promptTokenCount", 0),
                usage.optInt("totalTokenCount", 0)
            );
        }
    }

    protected void handleUsage(AssistantResponseDTO responseDto, GeminiStreamHandler streamHandler, AssistantDefinitionEntity dbAssitant, AiStatRepository statRepo, HttpServletRequest request) {
        handleUsage(responseDto, dbAssitant, statRepo, request,
            streamHandler.getCandidatesTokenCount(),
            streamHandler.getThoughtsTokenCount(),
            streamHandler.getPromptTokenCount(),
            streamHandler.getTotalTokenCount()
        );
    }

    protected void handleUsage(AssistantResponseDTO responseDto, AssistantDefinitionEntity dbAssitant, AiStatRepository statRepo, HttpServletRequest request, Integer candToken, Integer thouToken, Integer promToken, Integer totToken) {
        StringBuilder sb = new StringBuilder("");
        sb.append(SERVICE_NAME).append(" -> run was succesfull");
        sb.append("\n\n");
        sb.append("Assitant name : ").append(dbAssitant.getName()).append("\n");
        sb.append("From field : ").append(dbAssitant.getFieldFrom()).append("\n");
        sb.append("To field : ").append(dbAssitant.getFieldTo()).append("\n");
        sb.append("\n");
        sb.append("Action cost: \n");
        sb.append("\t candidatesTokenCount: ").append(candToken).append("\n");
        sb.append("\t thoughtsTokenCount: ").append(thouToken).append("\n");
        sb.append("\t promptTokenCount: ").append(promToken).append("\n");
        sb.append("\t totalTokenCount: ").append(totToken).append("\n");
        Adminlog.add(Adminlog.TYPE_AI, sb.toString(), totToken, -1);

        AiStatService.addRecord(dbAssitant.getId(), totToken, statRepo, request);

        responseDto.setTotalTokens(totToken);
    }

    protected void addPartWithFile(JSONArray contentsArray, String value, String mimeType, String fileData) {
        JSONObject inlineData = new JSONObject()
            .put("mime_type", mimeType)
            .put("data", fileData);

        JSONObject part = new JSONObject()
            .put("inline_data", inlineData);

        addPart(contentsArray, value, part);
    }

    protected void addPart(JSONArray contentsArray, String value) {
        addPart(contentsArray, value, null);
    }

    protected void addPart(JSONArray contentsArray, String value, JSONObject inlineData) {
        JSONObject content = new JSONObject()
            .put("role", "user");

        JSONObject partValue = new JSONObject()
            .put("text", value);

        JSONArray parts = new JSONArray();
        parts.put(partValue);

        if(inlineData != null) parts.put(inlineData);

        content.put("parts", parts);
        contentsArray.put(content);
    }

    protected <T extends HttpRequestBase> void setHeaders(T http, HttpServletRequest request) {
        String apiKey = Constants.getString(AUTH_KEY);
        if(Tools.isEmpty(apiKey)) throw new IllegalStateException("Gemini API key is not set.");
        http.setHeader("Content-Type", "application/json");
        http.setHeader("x-goog-api-key", apiKey);
        http.setHeader("referer", request.getHeader("referer"));
    }

    protected JSONArray getParts(JSONObject json) {
        JSONArray candidates = json.getJSONArray("candidates");
        JSONObject firstCandidate = candidates.getJSONObject(0);
        JSONObject content = firstCandidate.getJSONObject("content");
        return content.getJSONArray("parts");
    }
}