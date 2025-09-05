package sk.iway.iwcm.components.ai.providers.openai;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiInterface;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;
import sk.iway.iwcm.components.ai.rest.AiTempFileStorage;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.components.ai.stat.rest.AiStatService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;

import static sk.iway.iwcm.components.ai.providers.openai.OpenAiSupportService.ASSISTANT_FIELDS.*;

@Service
public class OpenAiService extends OpenAiSupportService implements AiInterface {

    private static final CloseableHttpClient client = HttpClients.createDefault();
    private static final String SERVICE_NAME = "OpenAiService";
    private static final String PROVIDER_ID = "openai";
    private static final String TITLE_KEY = "components.ai_assistants.provider.openai.title";

    public String getProviderId() {
        return PROVIDER_ID;
    }

    public boolean isInit() {
        return Tools.isNotEmpty(getApiKey());
    }

    public Pair<String, String> getProviderInfo(Prop prop) {
        return new Pair<>(PROVIDER_ID, prop.getText(TITLE_KEY));
    }

    public List<LabelValue> getSupportedModels(Prop prop, HttpServletRequest request) {
        List<LabelValue> supportedValues = new ArrayList<>();

        HttpGet get = new HttpGet(MODELS_URL);
        addHeaders(get, true, false);
        try (CloseableHttpResponse response = client.execute(get)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "getSupportedModels");

            String value = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
            if (Tools.isEmpty(value)) return supportedValues;

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(value);
            for (JsonNode model : root.get("data")) {
                String modelId = model.get("id").asText();
                String created = model.get("created").asText();
                supportedValues.add(new LabelValue(modelId, created));
            }
            supportedValues.sort(Comparator.comparingLong((LabelValue o) -> Long.parseLong(o.getValue())).reversed());

            for (LabelValue modelValue : supportedValues) modelValue.setValue(modelValue.getLabel());

            return supportedValues;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return supportedValues;
    }

    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop,  AiStatRepository statRepo, PrintWriter writer, HttpServletRequest request) throws IOException {

        AssistantResponseDTO responseDto = new AssistantResponseDTO();

        JSONObject json = new JSONObject();
        json.put(MODEL.value(), assistant.getModel());
        json.put(INSTRUCTIONS.value(), assistant.getInstructions());
        json.put(INPUT.value(), inputData.getInputValue());
        json.put(STORE.value(), !assistant.getUseTemporal());
        json.put(STREAM.value(), assistant.getUseStreaming());

        HttpPost post = new HttpPost(RESPONSES_URL);
        post.setEntity(getRequestBody(json.toString()));
        addHeaders(post, true, true);
        post.setHeader("Accept", "text/event-stream");

        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "getAiStreamResponse");

            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            OpenAiStreamHandler streamHandler = new OpenAiStreamHandler();
            streamHandler.handleBufferedReader(reader, writer);

            //
            handleUsage(responseDto, streamHandler.getUsageChunk(), assistant, streamHandler.getRunId(), statRepo, request);
        }

        return responseDto;
    }

    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws IOException {

        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        HttpPost post = new HttpPost(RESPONSES_URL);

        JSONObject json = new JSONObject();
        json.put(MODEL.value(), assistant.getModel());
        json.put(STORE.value(), !assistant.getUseTemporal());
        json.put(INSTRUCTIONS.value(), assistant.getInstructions());
        json.put(INPUT.value(), inputData.getInputValue());

        post.setEntity(getRequestBody(json.toString()));
        addHeaders(post, true, false);

        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "getAiResponse");

            JSONObject res = new JSONObject(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));
            JSONArray data = res.getJSONArray("output");
            JSONObject firstMessage = data.getJSONObject(0);
            JSONArray contentArray = firstMessage.getJSONArray("content");
            responseDto.setResponse(contentArray.getJSONObject(0).getString("text"));

            handleUsage(responseDto, res, assistant, res.optString("id", "NO_RUN_ID"), statRepo, request);

            return responseDto;
        }
    }

    public AssistantResponseDTO getAiImageResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws IOException {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        Path tempFileFolder = AiTempFileStorage.getFileFolder();

        HttpPost post;
        if(inputData.getInputValueType().equals(InputDataDTO.InputValueType.IMAGE)) {
            //ITS IMAGE EDIT - I GOT IMAGE to edit AND I WILL RETURN IMAGE
            post = getEditImagePost(inputData, assistant.getModel(), assistant.getInstructions(), prop);
        } else {
            //ITS IMAGE GENERATION - INPUT IS TEXT RETUN IMAGE
            post = getCreateImagePost(inputData, assistant.getModel(), assistant.getInstructions());
        }

        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "getAiImageResponse");

            JSONObject res = new JSONObject(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));
            String format = "." + res.optString("output_format", "png");
            JSONArray imageArr = res.getJSONArray("data");

            try {
                long datePart = new java.util.Date().getTime();
                for(int i = 0; i < imageArr.length(); i++) {
                    JSONObject jsonImage = imageArr.getJSONObject(i);
                    String base64Image = jsonImage.getString("b64_json");
                    //Date pars is added so we can delet all images from same request (same request == same date time part)
                    String tmpFileName = "tmp_ai_" + assistant.getName() + "_" + datePart + "_";

                    try {
                        tmpFileName = AiTempFileStorage.addImage(base64Image, tmpFileName, format, tempFileFolder);

                        //If no error, add file
                        responseDto.addTempFile(tmpFileName);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            handleUsage(responseDto, res, assistant, "NO_RUN_ID", statRepo, request);
        }

        return responseDto;
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

    private void handleUsage(AssistantResponseDTO responseDto, JSONObject source, AssistantDefinitionEntity dbAssitant, String runId, AiStatRepository statRepo, HttpServletRequest request) {

        if (source.has("usage")) {
            //"total_tokens" "output_tokens" "input_tokens"
            JSONObject usage = source.getJSONObject("usage");
            int inputTokens = usage.optInt("input_tokens", 0);
            int outputTokens = usage.optInt("output_tokens", 0);
            int totalTokens = usage.optInt("total_tokens", 0);
            StringBuilder sb = new StringBuilder("");

            sb.append(SERVICE_NAME).append(" -> run with id: ").append(runId).append(" was succesfull");
            sb.append("\n\n");
            sb.append("Assitant name : ").append(dbAssitant.getName()).append("\n");
            sb.append("From field : ").append(dbAssitant.getFieldFrom()).append("\n");
            sb.append("To field : ").append(dbAssitant.getFieldTo()).append("\n");
            sb.append("\n");
            sb.append("Action cost: \n");
            sb.append("\t input_tokens: ").append(inputTokens).append("\n");
            sb.append("\t output_tokens: ").append(outputTokens).append("\n");
            sb.append("\t total_tokens: ").append(totalTokens).append("\n");
            Adminlog.add(Adminlog.TYPE_AI, sb.toString(), totalTokens, -1);

            AiStatService.addRecord(dbAssitant.getId(), totalTokens, statRepo, request);

            responseDto.setTotalTokens(totalTokens);
        }
    }

    private HttpPost getEditImagePost(InputDataDTO inputData, String model, String instructions, Prop prop) throws IOException {
        HttpPost post = new HttpPost(IMAGES_EDITS_URL);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        //builder.addTextBody(MODEL.value(), "gpt-image-1");
        builder.addTextBody(MODEL.value(), model);
        builder.addTextBody("prompt", AiAssistantsService.executePromptMacro(instructions, inputData));
        builder.addTextBody("n", inputData.getImageCount() == null ? "1" : inputData.getImageCount().toString());

        if(inputData.getImageQuality() != null) {
            builder.addTextBody("quality", inputData.getImageQuality());
        }

        //1024x1024 is valid for gpt-image dalle-3 and dalle-2 ... soo best default value
        builder.addTextBody("size", inputData.getImageSize() == null ? "1024x1024" : inputData.getImageSize());

        BufferedImage image = ImageIO.read( inputData.getInputFile() );
        if (image == null) throw new IllegalStateException("Image not founded or not a Image.");

        ContentType contentType;
        String fileName = inputData.getInputFile().getName();
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            contentType = ContentType.create("image/jpeg");
        } else if (fileName.endsWith(".webp")) {
            contentType = ContentType.create("image/webp");
        } else {
            contentType = ContentType.create("image/png"); // default
        }

        //Set image
        if("dall-e-2".equals(model)) {
           throw new IllegalStateException( prop.getText("components.ai_assistants.not_supproted_action_err") );
        } else {
            builder.addBinaryBody("image", inputData.getInputFile(), contentType, fileName);
        }

        //Set entity and headers
        post.setEntity(builder.build());

        addHeaders(post, false, false);

        return post;
    }

    private HttpPost getCreateImagePost(InputDataDTO inputData, String model, String instructions) {
        HttpPost post = new HttpPost(IMAGES_GENERATION_URL);

        JSONObject json = new JSONObject();
        //json.put(MODEL.value(), "gpt-image-1");
        json.put(MODEL.value(), model);
        json.put("prompt", AiAssistantsService.executePromptMacro(instructions, inputData));
        json.put("n", inputData.getImageCount() == null ? 1 : inputData.getImageCount());

        if(inputData.getImageQuality() != null) {
            json.put("quality", inputData.getImageQuality());
        }

        //1024x1024 is valid for gpt-image dalle-3 and dalle-2 ... soo best default value
        json.put("size", inputData.getImageSize() == null ? "1024x1024" : inputData.getImageSize());

        if("dall-e-2".equals(model) || "dall-e-3".equals(model)) {
            json.put("response_format", "b64_json");
        }

        post.setEntity(getRequestBody(json.toString()));

        addHeaders(post, true, false);

        return post;
    }
}