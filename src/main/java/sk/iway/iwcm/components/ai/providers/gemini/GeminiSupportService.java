package sk.iway.iwcm.components.ai.providers.gemini;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.methods.HttpRequestBase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.providers.SupportLogic;

/**
 * Support service for Gemini AI model integration - common methods
 */
public abstract class GeminiSupportService extends SupportLogic {

    protected static final String AUTH_KEY = "ai_geminiAuthKey";
    protected static final String SERVICE_NAME = "GeminiService";
    protected static final String PARTS = "parts";
    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected ObjectNode getBaseMainObject(String... parts) throws IOException {
        return getBaseMainObjectWithImage(null, parts);
    }

    protected ObjectNode getBaseMainObjectWithImage(InputDataDTO inputData, String... parts) throws IOException {
        ObjectNode mainObject = MAPPER.createObjectNode();
        ArrayNode contentsArray = MAPPER.createArrayNode();

        // Add parts
        for (String part : parts)
            if(Tools.isNotEmpty(part)) addTextPart(contentsArray, part);

        //Image input
        if(inputData != null && InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType()))
            addImagePart(contentsArray, inputData);

        mainObject.set("contents", contentsArray);
        return mainObject;
    }

    private void addTextPart(ArrayNode contentsArray, String value) {
        ObjectNode content = MAPPER.createObjectNode();
        content.put("role", "user");
        ArrayNode parts = MAPPER.createArrayNode();

        ObjectNode partValue = MAPPER.createObjectNode();
        partValue.put("text", value);
        parts.add(partValue);

        content.set(PARTS, parts);
        contentsArray.add(content);
    }

    private void addImagePart(ArrayNode contentsArray, InputDataDTO inputData) throws IOException {
        ObjectNode content = MAPPER.createObjectNode();
        content.put("role", "user");

        ObjectNode inlineData = MAPPER.createObjectNode();
        inlineData.put("mime_type", inputData.getMimeType());
        inlineData.put("data", inputData.getFileAsBase64());

        ObjectNode inlineDataPart = MAPPER.createObjectNode();
        inlineDataPart.set("inline_data", inlineData);

        ArrayNode parts = MAPPER.createArrayNode();
        parts.add(inlineDataPart);

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