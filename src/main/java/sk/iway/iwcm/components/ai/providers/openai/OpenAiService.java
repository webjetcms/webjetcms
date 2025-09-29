package sk.iway.iwcm.components.ai.providers.openai;

import static sk.iway.iwcm.components.ai.providers.openai.OpenAiSupportService.ASSISTANT_FIELDS.MODEL;
import static sk.iway.iwcm.components.ai.providers.openai.OpenAiSupportService.ASSISTANT_FIELDS.STORE;
import static sk.iway.iwcm.components.ai.providers.openai.OpenAiSupportService.ASSISTANT_FIELDS.STREAM;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiInterface;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Service for OpenAI assistants - handles calls to OpenAI API
 * We do not use any official SDK, but rather direct REST calls, so its easy to maintain and we can see what is going on.
 * docs: https://platform.openai.com/docs/api-reference
 */
@Service
public class OpenAiService extends OpenAiSupportService implements AiInterface {

    private static final String PROVIDER_ID = "openai";
    private static final String TITLE_KEY = "components.ai_assistants.provider.openai.title";

    private static final ObjectMapper mapper = new ObjectMapper();

    public String getProviderId() {
        return PROVIDER_ID;
    }

    public String getServiceName() {
        return "OpenAiService";
    }

    public String getTitleKey() {
        return TITLE_KEY;
    }

    public boolean isInit() {
        return Tools.isNotEmpty(getApiKey());
    }

    public int addUsageAndReturnTotal(StringBuilder sb, int addTokens, JsonNode root) {
        int totalTokens = addTokens;
        if(root.has("usage")) {
            JsonNode usage = root.path("usage");
            int inputTokens = usage.path("input_tokens").asInt(0);
            int outputTokens = usage.path("output_tokens").asInt(0);
            totalTokens = usage.path("total_tokens").asInt(0) + addTokens;

            sb.append("\t input_tokens: ").append(inputTokens).append("\n");
            sb.append("\t output_tokens: ").append(outputTokens).append("\n");
            sb.append("\t total_tokens: ").append(totalTokens).append("\n");
        }
        return totalTokens;
    }

    public HttpRequestBase getModelsRequest (HttpServletRequest request) {
        HttpGet get = new HttpGet(MODELS_URL);
        addHeaders(get, true);
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

    public HttpRequestBase getResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request) {
        ObjectNode mainObject = getBaseMainObject(instructions, inputData.getInputValue(), inputData.getUserPrompt());
        mainObject.put(MODEL.value(), assistant.getModel());
        mainObject.put(STORE.value(), !assistant.getUseTemporal());

        HttpPost post = new HttpPost(RESPONSES_URL);
        post.setEntity(getRequestBody(mainObject.toString()));
        addHeaders(post, true);

        return post;
    }

    public String extractResponseText(JsonNode jsonNodeRes) {
        ArrayNode data = (ArrayNode) jsonNodeRes.path(OUTPUT);
        JsonNode firstMessage = data.get(0);
        ArrayNode contentArray = (ArrayNode) firstMessage.path("content");
        return  contentArray.get(0).path("text").asText();
    }

    public HttpRequestBase getStremResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request) {
        ObjectNode mainObject = getBaseMainObject(instructions, inputData.getInputValue(), inputData.getUserPrompt());
        mainObject.put(MODEL.value(), assistant.getModel());
        mainObject.put(STORE.value(), !assistant.getUseTemporal());
        mainObject.put(STREAM.value(), assistant.getUseStreaming());

        HttpPost post = new HttpPost(RESPONSES_URL);
        post.setEntity(getRequestBody(mainObject.toString()));
        addHeaders(post, true);
        post.setHeader("Accept", "text/event-stream");

        return post;
    }

    public JsonNode handleBufferedReader(BufferedReader reader,  BufferedWriter writer, Map<Integer, String> replacedIncludes) throws IOException {
        OpenAiStreamHandler streamHandler = new OpenAiStreamHandler(replacedIncludes);
        streamHandler.handleBufferedReader(reader, writer);
        return streamHandler.getUsageChunk();
    }


    public HttpRequestBase getImageResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request, Prop prop) throws IOException {
        if(inputData.getInputValueType().equals(InputDataDTO.InputValueType.IMAGE)) {
            //ITS IMAGE EDIT - I GOT IMAGE to edit AND I WILL RETURN IMAGE
            return getEditImagePost(inputData, assistant.getModel(), instructions, prop);
        } else {
            //ITS IMAGE GENERATION - INPUT IS TEXT RETUN IMAGE
            return getCreateImagePost(inputData, assistant.getModel(), instructions);
        }
    }

    public String getFinishError(JsonNode jsonNodeRes) {
        // OpenAI in new reponse API use STATUS instead of finish_reason

        // Need to be in try catch because IMAGES for example do not return status :)
        String status = null;
        try { status= jsonNodeRes.path(OUTPUT).get(0).path("status").asText("");
        } catch(Exception e) { return null; }

        if("completed".equalsIgnoreCase(status)) {
            //All good
            return null;
        } else if("incomplete".equalsIgnoreCase(status)) {
            // Problem, try extract reason
            return jsonNodeRes.path(OUTPUT).get(0).path("incomplete_details").path("reason").asText(status);
        } else {
            // UNKNOWN
            return status;
        }
    }

    public ArrayNode getImages(JsonNode jsonNodeRes) {
        JsonNode imagesArr = jsonNodeRes.path("data");
        return imagesArr.isArray() ? (ArrayNode) imagesArr : mapper.createArrayNode();
    }

    public String getImageFormat(JsonNode jsonNodeRes, JsonNode jsonImage) {
        //OpeAI returns global fomat, not local
        return "." + jsonNodeRes.path("output_format").asText( "png");
    }

    public String getImageBase64(JsonNode jsonNodeRes, JsonNode jsonImage) {
        return jsonImage.path("b64_json").asText(null);
    }

    public String getModelForImageNameGeneration() {
        return Constants.getString("ai_openAi_generateFileNameModel");
    }

    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop) {
        if("edit_image".equals(assistant.getAction()) || "generate_image".equals(assistant.getAction())) {
            String model = assistant.getModel();

            return """
                <div class='bonus-content row mt-3'>
                    <div class='col-sm-4'>
                        <label for='bonusContent-imageCount'>%s</label>
                        <input id='bonusContent-imageCount' type='number' class='form-control' value=1>
                    </div>
                    %s
                    %s
                </div>
            """.formatted(
                prop.getText("components.ai_assistants.imageCount"),
                getImageSizeSelect(model, prop),
                getImageQualitySelect(model, prop)
            );
        }

        return "";
    }

    private HttpPost getEditImagePost(InputDataDTO inputData, String model, String instructions, Prop prop) throws IOException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        builder.addTextBody(MODEL.value(), model);
        builder.addTextBody("prompt", instructions);
        builder.addTextBody("n", inputData.getImageCount() == null ? "1" : inputData.getImageCount().toString());

        if(inputData.getImageQuality() != null) {
            builder.addTextBody("quality", inputData.getImageQuality());
        }

        //1024x1024 is valid for gpt-image dalle-3 and dalle-2 ... soo best default value
        builder.addTextBody("size", inputData.getImageSize() == null ? "1024x1024" : inputData.getImageSize());

        BufferedImage image = ImageIO.read( inputData.getInputFile() );
        if (image == null) throw new IllegalStateException("Image not founded or not a Image.");

        //Set image
        if("dall-e-2".equals(model)) {
           throw new IllegalStateException( prop.getText("components.ai_assistants.not_supproted_action_err") );
        } else {
            builder.addBinaryBody("image", inputData.getInputFile(), inputData.getContentType(), inputData.getInputFile().getName());
        }

        //Set entity and headers
        HttpPost post = new HttpPost(IMAGES_EDITS_URL);
        post.setEntity(builder.build());
        addHeaders(post, false);

        return post;
    }

    private HttpPost getCreateImagePost(InputDataDTO inputData, String model, String instructions) {
        ObjectNode json = mapper.createObjectNode();
        json.put(MODEL.value(), model);
        json.put("prompt", instructions);
        json.put("n", inputData.getImageCount() == null ? 1 : inputData.getImageCount());

        if(inputData.getImageQuality() != null) {
            json.put("quality", inputData.getImageQuality());
        }

        //1024x1024 is valid for gpt-image dalle-3 and dalle-2 ... soo best default value
        json.put("size", inputData.getImageSize() == null ? "1024x1024" : inputData.getImageSize());

        if("dall-e-2".equals(model) || "dall-e-3".equals(model)) {
            json.put("response_format", "b64_json");
        }

        HttpPost post = new HttpPost(IMAGES_GENERATION_URL);
        post.setEntity(getRequestBody(json.toString()));
        addHeaders(post, true);

        return post;
    }
}