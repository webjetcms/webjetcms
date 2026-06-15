package sk.iway.iwcm.components.ai.providers.gemini;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;

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
        ArrayNode partsArray = MAPPER.createArrayNode();

        // Add text parts.
        for (String part : parts)
            if(Tools.isNotEmpty(part)) addTextPart(partsArray, part);

        // Image input.
        if(inputData != null && InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType()))
            addImagePart(partsArray, inputData);

        if (partsArray.size() > 0) addUserContent(contentsArray, partsArray);

        mainObject.set("contents", contentsArray);
        return mainObject;
    }

    protected ObjectNode getTextMainObject(String systemInstruction, InputDataDTO inputData, String... userParts) throws IOException {
        ObjectNode mainObject = getBaseMainObjectWithImage(inputData, userParts);
        ArrayNode contentsArray = (ArrayNode) mainObject.path("contents");
        if (contentsArray.size() == 0) {
            ArrayNode parts = MAPPER.createArrayNode();
            addTextPart(parts, "Apply the task instructions to the provided data.");
            addUserContent(contentsArray, parts);
        }

        if (Tools.isNotEmpty(systemInstruction)) {
            ObjectNode systemContent = MAPPER.createObjectNode();
            ArrayNode parts = MAPPER.createArrayNode();

            ObjectNode partValue = MAPPER.createObjectNode();
            partValue.put("text", systemInstruction);
            parts.add(partValue);

            systemContent.set(PARTS, parts);
            mainObject.set("systemInstruction", systemContent);
        }

        return mainObject;
    }

    private void addUserContent(ArrayNode contentsArray, ArrayNode parts) {
        ObjectNode content = MAPPER.createObjectNode();
        content.put("role", "user");
        content.set(PARTS, parts);
        contentsArray.add(content);
    }

    private void addTextPart(ArrayNode parts, String value) {
        ObjectNode partValue = MAPPER.createObjectNode();
        partValue.put("text", value);
        parts.add(partValue);
    }

    private void addImagePart(ArrayNode parts, InputDataDTO inputData) throws IOException {
        ObjectNode inlineData = MAPPER.createObjectNode();
        inlineData.put("mime_type", inputData.getMimeType());
        inlineData.put("data", inputData.getFileAsBase64());

        ObjectNode inlineDataPart = MAPPER.createObjectNode();
        inlineDataPart.set("inline_data", inlineData);

        parts.add(inlineDataPart);
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
