package sk.iway.iwcm.components.ai.providers.gemini;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.methods.HttpRequestBase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected void addPartWithFile(ArrayNode contentsArray, String value, String mimeType, String fileData) {
        ObjectNode inlineData = MAPPER.createObjectNode();
        inlineData.put("mime_type", mimeType);
        inlineData.put("data", fileData);

        ObjectNode part = MAPPER.createObjectNode();
        part.set("inline_data", inlineData);

        addPart(contentsArray, value, part);
    }

    protected ObjectNode getBaseMainObject(String... parts) {
        ObjectNode mainObject = MAPPER.createObjectNode();
        ArrayNode contentsArray = MAPPER.createArrayNode();

        // Add parts
        for (String part : parts) {
            if (Tools.isNotEmpty(part)) addPart(contentsArray, part);
        }

        mainObject.set("contents", contentsArray);
        return mainObject;
    }

    protected void addPart(ArrayNode contentsArray, String value) {
        addPart(contentsArray, value, null);
    }

    protected void addPart(ArrayNode contentsArray, String value, ObjectNode inlineData) {
        ObjectNode content = MAPPER.createObjectNode();
        content.put("role", "user");

        ObjectNode partValue = MAPPER.createObjectNode();
        partValue.put("text", value);

        ArrayNode parts = MAPPER.createArrayNode();
        parts.add(partValue);

        if (inlineData != null) parts.add(inlineData);

        content.set(PARTS, parts);
        contentsArray.add(content);
    }

    protected <T extends HttpRequestBase> void setHeaders(T http, HttpServletRequest request) {
        String apiKey = getApiKey();
        if(Tools.isEmpty(apiKey)) throw new IllegalStateException("Gemini API key is not set.");
        http.setHeader("Content-Type", "application/json; charset=utf-8");
        http.setHeader("x-goog-api-key", apiKey);
        http.setHeader("referer", request.getHeader("referer"));
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