package sk.iway.iwcm.components.ai.providers.openrouter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

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
import sk.iway.iwcm.components.ai.providers.gemini.GeminiService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;

/**
 * Service for OpenRouter API - https://openrouter.ai/
 */
@Service
public class OpenRouterService extends OpenRouterSupportService implements AiInterface {

    private static final String MODELS_URL = "https://openrouter.ai/api/v1/models";
    private static final String RESPONSES_URL = "https://openrouter.ai/api/v1/chat/completions";

    private static final String CHOICES = "choices";
    private static final String MODEL = "model";

    public String getProviderId() {
        return "openrouter";
    }

    public String getTitleKey() {
        return "components.ai_assistants.provider.openrouter.title";
    }

    public boolean isInit() {
       return Tools.isNotEmpty(getApiKey());
    }

    public int addUsageAndReturnTotal(StringBuilder sb, int addToken, JsonNode root) {
        int totalTokens = addToken;
        if(root.has("usage")) {
            JsonNode usage = root.path("usage");
            totalTokens = usage.path("total_tokens").asInt(0);
            sb.append("\t totalTokenCount: ").append(totalTokens + addToken).append("\n");
        }
        return totalTokens + addToken;
    }

    public HttpRequestBase getModelsRequest (HttpServletRequest request) {
        HttpGet get = new HttpGet(MODELS_URL);
        setHeaders(get, true);
        return get;
    }

    public List<LabelValue> extractModels(JsonNode root) {
        List<LabelValue> supportedValues = new ArrayList<>();
        for (JsonNode model : root.get("data")) {
            String modelId = model.get("id").asText();
            String created = model.get("created").asText();
            supportedValues.add(new LabelValue(modelId, created));
        }
        supportedValues.sort(Comparator.comparingLong((LabelValue o) -> Long.parseLong(o.getValue())).reversed());
        for (LabelValue modelValue : supportedValues) modelValue.setValue(modelValue.getLabel());
        return supportedValues;
    }

    public HttpRequestBase getResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request) throws IOException {
        // Build base request body
        ObjectNode mainObject;
        if(InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType()))
            mainObject = getBaseMainObjectWithImage(inputData, instructions, inputData.getUserPrompt());
        else
            mainObject = getBaseMainObject(instructions, inputData.getInputValue(), inputData.getUserPrompt());

        mainObject.put(MODEL, assistant.getModel());

        HttpPost post = new HttpPost(RESPONSES_URL);
        post.setEntity(getRequestBody(mainObject.toString()));
        setHeaders(post, true);

        return post;
    }

    public String extractResponseText(JsonNode jsonNodeRes) {
        return jsonNodeRes.path(CHOICES)
                .get(0)
                .path("message")
                .path("content")
                .asText();
    }

    public HttpRequestBase getStremResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request) throws IOException {
        // Build base request body
        ObjectNode mainObject;
        if(InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType()))
            mainObject = getBaseMainObjectWithImage(inputData, instructions, inputData.getUserPrompt());
        else
            mainObject = getBaseMainObject(instructions, inputData.getInputValue(), inputData.getUserPrompt());

        mainObject.put(MODEL, assistant.getModel());
        mainObject.put("stream", true);

        HttpPost post = new HttpPost(RESPONSES_URL);
        post.setEntity(getRequestBody(mainObject.toString()));
        setHeaders(post, true);
        return post;
    }

    public Pair<String, JsonNode> handleBufferedReader(BufferedReader reader,  BufferedWriter writer, Map<Integer, String> replacedIncludes) throws IOException {
        OpenRouterStreamHandler openRouterStreamHandler = new OpenRouterStreamHandler(replacedIncludes);
        openRouterStreamHandler.handleBufferedReader(reader, writer);
        return new Pair<>(openRouterStreamHandler.getWholeResponse(), openRouterStreamHandler.getUsageChunk());
    }

    public HttpRequestBase getImageResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request, Prop prop) throws IOException {
        ObjectNode mainObject;
        if(inputData.getInputValueType().equals(InputDataDTO.InputValueType.IMAGE)) {
            //ITS IMAGE EDIT - I GOT IMAGE to edit AND I WILL RETURN IMAGE
            mainObject = getBaseMainObjectWithImage(inputData, instructions);
        } else {
            //ITS IMAGE GENERATION - INPUT IS TEXT RETUN IMAGE
            mainObject = getBaseMainObject(instructions);
        }

        mainObject.put(MODEL, assistant.getModel());

        HttpPost httpPost = new HttpPost(RESPONSES_URL);
        setHeaders(httpPost, true);
        httpPost.setEntity(new StringEntity(mainObject.toString(), java.nio.charset.StandardCharsets.UTF_8));

        return httpPost;
    }

    public String getFinishError(JsonNode jsonNodeRes) {
       try {
            ArrayNode choices = (ArrayNode) jsonNodeRes.path(CHOICES);
            JsonNode firstChoice = choices.get(0);
            return handleFinishReasonValue( firstChoice.path("finish_reason").asText("") );
        } catch(Exception e) {
            Logger.error(GeminiService.class, "Fetting finish_reason : " + e);
            return null;
        }
    }

    public ArrayNode getImages(JsonNode jsonNodeRes) {
        ArrayNode choices = (ArrayNode) jsonNodeRes.path(CHOICES);
        JsonNode firstChoice = choices.get(0);
        JsonNode message = firstChoice.path("message");
        return (ArrayNode) message.path("images");
    }

    public String getImageFormat(JsonNode jsonNodeRes, JsonNode jsonImage) {
        String imageUrl = jsonImage.path("image_url").path("url").asText("");

        // Its combination of prefix + format + base64 -> parse it
        if(imageUrl.startsWith("data:"))
            imageUrl = imageUrl.substring(5);

        String[] urlParts = imageUrl.split(";");
        if(urlParts.length != 2) throw new IllegalStateException("Something wrong");

        String format = urlParts[0];
        if(format.startsWith("image/")) format = format.substring(6);

        return format;
    }

    public String getImageBase64(JsonNode jsonNodeRes, JsonNode jsonImage) {
        String imageUrl = jsonImage.path("image_url").path("url").asText("");

        // Its combination of prefix + format + base64 -> parse it
        if(imageUrl.startsWith("data:"))
            imageUrl = imageUrl.substring(5);

        String[] urlParts = imageUrl.split(";");
        if(urlParts.length != 2) throw new IllegalStateException("Something wrong");

        String base64Image = urlParts[1];
        if(base64Image.startsWith("base64,")) base64Image = base64Image.substring(7);

        return base64Image;
    }

    public String getModelForImageNameGeneration() {
       return Constants.getString("ai_openRouter_generateFileNameModel");
    }

    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop) {
        return null;
    }
}