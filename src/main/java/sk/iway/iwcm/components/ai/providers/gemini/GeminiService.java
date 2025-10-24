
package sk.iway.iwcm.components.ai.providers.gemini;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiInterface;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;

/**
 * Service for Google Gemini AI model integration. We do not use any official SDK, but
 * rather direct REST calls, so its easy to maintain and we can see what is going on.
 * docs: https://ai.google.dev/gemini-api/docs
 */
@Service
public class GeminiService extends GeminiSupportService implements AiInterface {

    private static final String PROVIDER_ID = "gemini";
    private static final String TITLE_KEY = "components.ai_assistants.provider.gemini.title";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    public String getProviderId() {
        return PROVIDER_ID;
    }

    public String getTitleKey() {
        return TITLE_KEY;
    }

    public boolean isInit() {
        return Tools.isNotEmpty(getApiKey());
    }

    public String getServiceName() {
        return "GeminiService";
    }

    public int addUsageAndReturnTotal(StringBuilder sb, int addTokens, JsonNode root) {
        int totalTokens = addTokens;
        JsonNode usage = null;

        // Gemini return "usage" (when text answer) OR "usageMetadata" (when image answer) -> like and idiot
        if(root.has("usage")) usage = root.path("usage");
        else if(root.has("usageMetadata")) usage = root.path("usageMetadata");

        if(usage != null) {
            int candToken = usage.path("candidatesTokenCount").asInt(0);
            int thouToken = usage.path("thoughtsTokenCount").asInt(0);
            int promToken = usage.path("promptTokenCount").asInt(0);
            totalTokens = usage.path("totalTokenCount").asInt(0) + addTokens;

            sb.append("\t candidatesTokenCount: ").append(candToken).append("\n");
            sb.append("\t thoughtsTokenCount: ").append(thouToken).append("\n");
            sb.append("\t promptTokenCount: ").append(promToken).append("\n");
            sb.append("\t totalTokenCount: ").append(totalTokens).append("\n");
        }
        return totalTokens;
    }

    public HttpRequestBase getModelsRequest (HttpServletRequest request) {
        HttpGet httpGet = new HttpGet(BASE_URL);
        setHeaders(httpGet, request);
        return httpGet;
    }

    public List<LabelValue> extractModels(JsonNode root) {
        String modelPrefix = "models/";
        List<LabelValue> supportedValues = new ArrayList<>();
        for (JsonNode model : (ArrayNode) root.get("models")) {
            String id = model.path("name").asText("");
            if(id.startsWith(modelPrefix)) id = id.substring(modelPrefix.length());
            String displayName = model.path("displayName").asText(id);
            supportedValues.add(new LabelValue(displayName, id));
            supportedValues.sort(Comparator.comparing(LabelValue::getValue));
        }
        return supportedValues;
    }

    public HttpRequestBase getResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request) throws IOException {
        // Build base request body
        ObjectNode mainObject;
        if(InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType()))
            mainObject = getBaseMainObjectWithImage(inputData, instructions, inputData.getUserPrompt());
        else
            mainObject = getBaseMainObject(instructions, inputData.getInputValue(), inputData.getUserPrompt());

        HttpPost httpPost = new HttpPost(BASE_URL + assistant.getModel() + ":generateContent");
        setHeaders(httpPost, request);
        httpPost.setEntity(getRequestBody(mainObject.toString()));

        return httpPost;
    }

    public String extractResponseText(JsonNode jsonNodeRes) {
        ArrayNode parts = getParts(jsonNodeRes);
        return parts.get(0).path("text").asText();
    }

    public HttpRequestBase getStremResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request) throws IOException {
        //Prepare body object
        ObjectNode mainObject;
        if(InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType()))
            mainObject = getBaseMainObjectWithImage(inputData, instructions, inputData.getUserPrompt());
        else
            mainObject = getBaseMainObject(instructions, inputData.getInputValue(), inputData.getUserPrompt());

        HttpPost httpPost = new HttpPost(BASE_URL + assistant.getModel() + ":streamGenerateContent");
        setHeaders(httpPost, request);
        httpPost.setHeader("Accept", "text/event-stream");
        httpPost.setHeader("Accept-Encoding", "identity");
        httpPost.setEntity(new StringEntity(mainObject.toString(), java.nio.charset.StandardCharsets.UTF_8));

        return httpPost;
    }

    public Pair<String, JsonNode> handleBufferedReader(BufferedReader reader,  BufferedWriter writer, Map<Integer, String> replacedIncludes) throws IOException {
        GeminiStreamHandler streamHandler = new GeminiStreamHandler(replacedIncludes);
        streamHandler.handleBufferedReader(reader, writer);
        return new Pair<>(streamHandler.getWholeResponse(), streamHandler.getUsageChunk());
    }

    @Override
    public String getStreamEncoding(HttpEntity entity) {
        String charset = java.nio.charset.StandardCharsets.UTF_8.name();
        Header contentType = entity.getContentType();
        if (contentType != null) {
            String value = contentType.getValue();
            String[] parts = value.split(";");
            for (String part : parts) {
                part = part.trim();
                if (part.toLowerCase().startsWith("charset=")) {
                    charset = part.substring(8);
                    break;
                }
            }
        }
        return charset;
    }

    public HttpRequestBase getImageResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request, Prop prop) throws IOException {
        ObjectNode mainObject;
        if(inputData.getInputValueType().equals(InputDataDTO.InputValueType.IMAGE))
            //ITS IMAGE EDIT - I GOT IMAGE to edit AND I WILL RETURN IMAGE
            mainObject = getBaseMainObjectWithImage(inputData, instructions);
        else
            //ITS IMAGE GENERATION - INPUT IS TEXT RETUN IMAGE
            mainObject = getBaseMainObject(instructions);

        HttpPost httpPost = new HttpPost( BASE_URL + assistant.getModel() + ":generateContent");
        setHeaders(httpPost, request);
        httpPost.setEntity(new StringEntity(mainObject.toString(), java.nio.charset.StandardCharsets.UTF_8));

        return httpPost;
    }

    public String getFinishError(JsonNode jsonNodeRes) {
        // Extract finishReason from Jackson JsonNode response
        String finishReason = null;
        String finishText = null;
        try {
            JsonNode candidates = jsonNodeRes.path("candidates");
            if (candidates.isArray()) {
                for (JsonNode candidate : candidates) {
                    JsonNode frNode = candidate.get("finishReason");
                    if (frNode != null && frNode.isTextual()) {
                        // Last non-null value wins (in case of multiple candidates)
                        finishReason = frNode.asText();
                    }

                    JsonNode frNode2 = candidate.get("finishMessage");
                    if (frNode2 != null && frNode2.isTextual()) {
                        // Last non-null value wins (in case of multiple candidates)
                        finishText = frNode2.asText();
                    }
                }
            }
        } catch (Exception e) {
            Logger.error(GeminiService.class, e);
        }

        //Gemini do not give better explanation that just this (unlike OpenAI with detail object)
        if(Tools.isEmpty(finishReason) || "STOP".equalsIgnoreCase(finishReason)) return null; //its ok
        else {
            if(Tools.isNotEmpty(finishText)) return "(" + finishReason + ") " + finishText;
            return finishReason;
        }
    }

    public ArrayNode getImages(JsonNode jsonNodeRes) {
        return getParts(jsonNodeRes);
    }

    public String getImageFormat(JsonNode jsonNodeRes, JsonNode jsonImage) {
        String format = jsonImage.path("inlineData").path("mimeType").asText("png");
        if(format.startsWith("image/")) format = format.substring(6);
        if(format.startsWith(".") == false) format = "." + format;
        return format;
    }

    public String getImageBase64(JsonNode jsonNodeRes, JsonNode jsonImage) {
        return jsonImage.path("inlineData").path("data").asText(null);
    }

    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop) {
        //No special params to show
        return null;
    }

    public String  getModelForImageNameGeneration() {
        return Constants.getString("ai_gemini_generateFileNameModel");
    }
}