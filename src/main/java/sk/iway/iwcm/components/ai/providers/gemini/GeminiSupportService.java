package sk.iway.iwcm.components.ai.providers.gemini;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.providers.SupportLogic;

/**
 * Support service for Gemini AI model integration - common methods
 */
public abstract class GeminiSupportService extends SupportLogic {

    protected static final String AUTH_KEY = "ai_geminiAuthKey";
    protected static final String SERVICE_NAME = "GeminiService";
    protected static final String PARTS = "parts";

    protected void addPartWithFile(JSONArray contentsArray, String value, String mimeType, String fileData) {
        JSONObject inlineData = new JSONObject()
            .put("mime_type", mimeType)
            .put("data", fileData);

        JSONObject part = new JSONObject()
            .put("inline_data", inlineData);

        addPart(contentsArray, value, part);
    }

    protected JSONObject getBaseMainObject(String... parts) {
        //Main JSON object
        JSONObject mainObject = new JSONObject();
        //Contents Array
        JSONArray contentsArray = new JSONArray();
        //Add parts
        for (String part : parts) {
            if (Tools.isNotEmpty(part)) addPart(contentsArray, part);
        }

        mainObject.put("contents", contentsArray);
        return mainObject;
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

        content.put(PARTS, parts);
        contentsArray.put(content);
    }

    protected <T extends HttpRequestBase> void setHeaders(T http, HttpServletRequest request) {
        String apiKey = getApiKey();
        if(Tools.isEmpty(apiKey)) throw new IllegalStateException("Gemini API key is not set.");
        http.setHeader("Content-Type", "application/json; charset=utf-8");
        http.setHeader("x-goog-api-key", apiKey);
        http.setHeader("referer", request.getHeader("referer"));
    }

    protected JSONArray getParts(JSONObject json) {
        JSONArray candidates = json.getJSONArray("candidates");
        JSONObject firstCandidate = candidates.getJSONObject(0);
        JSONObject content = firstCandidate.getJSONObject("content");
        return content.getJSONArray(PARTS);
    }

    protected ArrayNode getParts(JsonNode root) {
        ArrayNode candidates = (ArrayNode) root.path("candidates");
        JsonNode firstCandidate = candidates.get(0);
        JsonNode content = firstCandidate.path("content");
        return (ArrayNode) content.path(PARTS);
    }

    public static String getApiKey() {
        return Constants.getString(AUTH_KEY);
    }
}