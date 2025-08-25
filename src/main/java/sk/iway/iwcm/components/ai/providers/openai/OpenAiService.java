package sk.iway.iwcm.components.ai.providers.openai;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
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
import sk.iway.iwcm.Constants;
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

@Service
public class OpenAiService extends OpenAiSupportService implements AiInterface {

    private static final CloseableHttpClient client = HttpClients.createDefault();
    private static final String SERVICE_NAME = "OpenAiService";
    private static final String PROVIDER_ID = "openai";
    private static final String TITLE_KEY = "components.ai_provider.openai.title";
    private static final String AUTH_KEY = "open_ai_auth_key";

    public String getProviderId() {
        return PROVIDER_ID;
    }

    public boolean isInit() {
        return Tools.isNotEmpty(Constants.getString(AUTH_KEY));
    }

    public Pair<String, String> getProviderInfo(Prop prop) {
        return new Pair<>(PROVIDER_ID, prop.getText(TITLE_KEY));
    }

    public List<LabelValue> getSupportedModels(Prop prop) {
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

    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, PrintWriter writer) throws IOException, InterruptedException {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        String assistantId = assistant.getAssistantKey();
        BigDecimal temperature = assistant.getTemperature();

        // 1. Create thread
        String threadId = createThread(prop);

        try {
            // 2. Add message
            addMessage(assistant, threadId, inputData, prop);

            // 3. Create run
            JSONObject json = new JSONObject();
            json.put("assistant_id", assistantId);
            json.put("temperature", temperature);
            json.put("stream", true);

            HttpPost post = new HttpPost(THREADS_URL + threadId + "/runs");
            post.setEntity(getRequestBody(json.toString()));
            addHeaders(post, true, true);
            post.setHeader("Accept", "text/event-stream");

            try (CloseableHttpResponse response = client.execute(post)) {

                HttpEntity entity = response.getEntity();
                InputStream inputStream = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                OpenAiStreamHandler streamHandler = new OpenAiStreamHandler();
                streamHandler.handleBufferedReader(reader, writer);

                //
                handleUsage(responseDto, streamHandler.getUsageChunk(), assistant, streamHandler.getRunId(), threadId, statRepo);
            }
        } finally {
            // 6. Delete thread (cleanup)
            deleteThread(threadId);
        }

        return responseDto;
    }


    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo) throws IOException, InterruptedException {

        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        String assistantId = assistant.getAssistantKey();
        BigDecimal temperature = assistant.getTemperature();

        // 1. Create thread
        String threadId = createThread(prop);

        try {
            // 2. Add message
            addMessage(assistant, threadId, inputData, prop);

            // 3. Create run
            String runId = createRun(threadId, assistantId, temperature, prop);

            // 4. Wait for run to complete
            waitForRunCompletion(threadId, runId, assistant, prop, responseDto, statRepo);

            // 5. Get assistant's reply
            return getLatestMessage(threadId, prop, responseDto);

        } finally {
            // 6. Delete thread (cleanup)
            deleteThread(threadId);
        }
    }

    public AssistantResponseDTO getAiImageResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo) throws IOException {

        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        HttpPost post;

        Path tempFileFolder = AiTempFileStorage.getFileFolder();

        if(inputData.getInputValueType().equals(InputDataDTO.InputValueType.IMAGE)) {
            //ITS IMAGE EDIT - I GOT IMAGE AND I WILL RETURN IMAGE
            post = new HttpPost(IMAGES_EDITS_URL);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addTextBody("model", "gpt-image-1");
            builder.addTextBody("prompt", AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData));
            builder.addTextBody("n", inputData.getImageCount().toString());
            builder.addTextBody("quality", inputData.getImageQuality());
            builder.addTextBody("size", inputData.getImageSize());

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
            builder.addBinaryBody("image", inputData.getInputFile(), contentType, fileName);

            //Set entity and headers
            post.setEntity(builder.build());

            addHeaders(post, false, false);

        } else {
            //ITS IMAGE GENERATION - INPUT IS TEXT RETUN IMAGE
            post = new HttpPost(IMAGES_GENERATION_URL);

            JSONObject json = new JSONObject();
            json.put("model", "gpt-image-1");
            json.put("prompt", AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData));
            json.put("n", inputData.getImageCount());
            json.put("quality", inputData.getImageQuality());
            json.put("size", inputData.getImageSize());

            post.setEntity(getRequestBody(json.toString()));

            addHeaders(post, true, false);
        }

        try (CloseableHttpResponse response = client.execute(post)) {

            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "");


            JSONObject res = new JSONObject(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));
            String format = "." + res.optString("output_format", "png");
            JSONArray imageArr = res.getJSONArray("data");

            try {

                long datePart = new java.util.Date().getTime();
                for(int i = 0; i < imageArr.length(); i++) {
                    JSONObject jsonImage = imageArr.getJSONObject(i);
                    String base64Image = jsonImage.getString("b64_json");
                    //Date pars is added so we can delet all images from same request (same request == same date time part)
                    String tmpFileName = "tmp_ai_" + assistant.getAssistantKey() + "_" + datePart + "_";

                    try {
                        tmpFileName = AiTempFileStorage.addImage(base64Image, tmpFileName, format, tempFileFolder);

                        //If no error, add file
                        responseDto.addTempFile(tmpFileName);
                    } catch (IOException ioe) {
                        //DONT know what to do
                    }
                }

            } catch (Exception e) {}

            handleUsage(responseDto, res, assistant, "NO_RUN_ID", "NO_THREAD_ID", statRepo);

        }

        return responseDto;
    }

    public String getBonusHtml(AssistantDefinitionEntity assistant) {
        if("edit_image".equals(assistant.getAction()) || "generate_image".equals(assistant.getAction())) {
            return """
                <div class='bonus-content'>
                    <div>
                        <label for='bonusContent-imageCount'>Image count</label>
                        <input id='bonusContent-imageCount' type='number' class='form-control' value=1>
                    </div>
                    <div>
                        <label for='bonusContent-imageSize'>Image size</label>
                        <select id='bonusContent-imageSize' class='form-control' value='auto'>
                            <option value="auto">auto</option>
                            <option value="1024x1024">1024x1024</option>
                            <option value="1024x1536">1024x1536</option>
                            <option value="1536x1024">1536x1024</option>
                        </select>
                    </div>
                    <div>
                        <label for='bonusContent-imageQuality'>Image quality</label>
                        <select id='bonusContent-imageQuality' class='form-control' value='low'>
                            <option value="low">low</option>
                            <option value="medium">medium</option>
                            <option value="high">high</option>
                        </select>
                    </div>
                </div>
            """;
        }

        return "";
    }

    private String createThread(Prop prop) throws IOException {
        HttpPost post = new HttpPost("https://api.openai.com/v1/threads");
        post.setEntity(getRequestBody("{}"));
        addHeaders(post, true, true);
        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "createThread");
            JSONObject res = new JSONObject(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));
            return res.getString("id");
        }
    }

    private void addMessage(AssistantDefinitionEntity assistant, String threadId, InputDataDTO inputData, Prop prop) throws IOException {
        JSONObject json = new JSONObject();
        json.put("role", "user");

        String content = inputData.getInputValue();
        if (Tools.isTrue(assistant.getUserPromptEnabled())) {
            content = AiAssistantsService.executePromptMacro("", inputData);
        }

        json.put("content", content);

        HttpPost post = new HttpPost(THREADS_URL + threadId + "/messages");
        post.setEntity(getRequestBody(json.toString()));
        addHeaders(post, true, true);
        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "addMessage");
        }
    }

    private String createRun(String threadId, String assistantId, BigDecimal temperature, Prop prop) throws IOException {
        JSONObject json = new JSONObject();
        json.put("assistant_id", assistantId);
        json.put("temperature", temperature);
        HttpPost post = new HttpPost(THREADS_URL + threadId + "/runs");
        post.setEntity(getRequestBody(json.toString()));
        addHeaders(post, true, true);
        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "createRun");
            JSONObject res = new JSONObject(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));
            return res.getString("id");
        }
    }

    private void waitForRunCompletion(String threadId, String runId, AssistantDefinitionEntity dbAssitant, Prop prop, AssistantResponseDTO responseDto, AiStatRepository statRepo) throws IOException, InterruptedException {
        while (true) {
            HttpGet get = new HttpGet(THREADS_URL + threadId + "/runs/" + runId);
            addHeaders(get, false, true);
            try (CloseableHttpResponse response = client.execute(get)) {
                if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                    handleErrorMessage(response, prop, SERVICE_NAME, "waitForRunCompletion");
                JSONObject res = new JSONObject(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));
                String status = res.getString("status");
                if ("completed".equals(status)) {

                    handleUsage(responseDto, res, dbAssitant, runId, threadId, statRepo);

                    break;
                }
                if ("failed".equals(status)) throw new RuntimeException("Run failed: " + res);
                Thread.sleep(1000);
            }
        }
    }

    private AssistantResponseDTO getLatestMessage(String threadId, Prop prop, AssistantResponseDTO responseDto) throws IOException {
        HttpGet get = new HttpGet(THREADS_URL + threadId + "/messages");
        addHeaders(get, false, true);
        try (CloseableHttpResponse response = client.execute(get)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "getLatestMessage");
            JSONObject res = new JSONObject(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));
            JSONArray data = res.getJSONArray("data");
            JSONObject firstMessage = data.getJSONObject(0);
            JSONArray contentArray = firstMessage.getJSONArray("content");
            responseDto.setResponse(contentArray.getJSONObject(0).getJSONObject("text").getString("value"));
            return responseDto;
        }
    }

    private void deleteThread(String threadId) throws IOException {
        HttpDelete delete = new HttpDelete(THREADS_URL + threadId);
        addHeaders(delete, false, true);
        try (CloseableHttpResponse response = client.execute(delete)) {
            // System.out.println("Thread deleted: " + threadId);
            //TODO
        }
    }

    private void handleUsage(AssistantResponseDTO responseDto, JSONObject source, AssistantDefinitionEntity dbAssitant, String runId, String threadId, AiStatRepository statRepo) {

        if (source.has("usage")) {
            JSONObject usage = source.getJSONObject("usage");
            int promptTokens = usage.optInt("prompt_tokens", 0);
            int completionTokens = usage.optInt("completion_tokens", 0);
            int totalTokens = usage.optInt("total_tokens", 0);
            StringBuilder sb = new StringBuilder("");
            sb.append(SERVICE_NAME).append(" -> run with id: ").append(runId).append(" on thred: ").append(threadId).append(" was succesfull");
            sb.append("\n\n");
            sb.append("Assitant name : ").append(dbAssitant.getName()).append("\n");
            sb.append("From field : ").append(dbAssitant.getFieldFrom()).append("\n");
            sb.append("To field : ").append(dbAssitant.getFieldTo()).append("\n");
            sb.append("\n");
            sb.append("Action cost: \n");
            sb.append("\t prompt_tokens: ").append(promptTokens).append("\n");
            sb.append("\tcompletion_tokens: ").append(completionTokens).append("\n");
            sb.append("\t total_tokens: ").append(totalTokens).append("\n");
            Adminlog.add(Adminlog.TYPE_AI, sb.toString(), totalTokens, -1);

            AiStatService.addRecord(dbAssitant.getName(), totalTokens, statRepo);

            responseDto.setPromptTokens(promptTokens);
            responseDto.setCompletionTokens(completionTokens);
            responseDto.setTotalTokens(totalTokens);
        }

    }
}