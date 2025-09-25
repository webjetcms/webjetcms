package sk.iway.iwcm.components.ai.providers.openrouter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiInterface;
import sk.iway.iwcm.components.ai.providers.FunctionTypes.*;
import sk.iway.iwcm.components.ai.providers.IncludesHandler;
import sk.iway.iwcm.components.ai.providers.SupportLogic;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;
import sk.iway.iwcm.components.ai.rest.AiTempFileStorage;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Service for OpenRouter API - https://openrouter.ai/
 */
@Service
public class OpenRouterService extends OpenRouterSupportService implements AiInterface {

    private static final String MODELS_URL = "https://openrouter.ai/api/v1/models";
    private static final String RESPONSES_URL = "https://openrouter.ai/api/v1/chat/completions";

    private static final CloseableHttpClient client = HttpClients.createDefault();

    public String getProviderId() {
        return "openrouter";
    }

    public String getTitleKey() {
        return "components.ai_assistants.provider.openrouter.title";
    }

    public boolean isInit() {
       return Tools.isNotEmpty(getApiKey());
    }

    public List<LabelValue> getSupportedModels(Prop prop, HttpServletRequest request) {

        ModelsRequest mr  = () -> {
            HttpGet get = new HttpGet(MODELS_URL);
            setHeaders(get, true);
            return get;
        };

        ModelsExtractor me = (root) -> {
            List<LabelValue> supportedValues = new ArrayList<>();
             for (JsonNode model : root.get("data")) {
                String modelId = model.get("id").asText();
                String created = model.get("created").asText();
                supportedValues.add(new LabelValue(modelId, created));
            }
            supportedValues.sort(Comparator.comparingLong((LabelValue o) -> Long.parseLong(o.getValue())).reversed());
            for (LabelValue modelValue : supportedValues) modelValue.setValue(modelValue.getLabel());
            return supportedValues;
        };

        return SupportLogic.getSupportedModels(prop, getServiceName(), mr, me);
    }

    @Override
    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws Exception {

        ResponseRequest rr = (instructions, inputText, inputPrompt) -> {
            JSONObject mainObject = getBaseMainObject(instructions, inputText, inputPrompt);
            mainObject.put("model", assistant.getModel());

            HttpPost post = new HttpPost(RESPONSES_URL);
            post.setEntity(getRequestBody(mainObject.toString()));
            setHeaders(post, true);

            return post;
        };

        ResponseExtractor re = (jsonNodeRes) -> {
            // Navigate: choices[0].message.content
            return jsonNodeRes.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();
        };

        return SupportLogic.getAiResponse(assistant, inputData, prop, statRepo, request, rr, re);
    }

    @Override
    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, BufferedWriter writer, HttpServletRequest request) throws Exception {
       //Handle replace of INCLUDE tags
        Map<Integer, String> replacedIncludes = IncludesHandler.replaceIncludesWithPlaceholders(inputData);
        String instructions = AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData, replacedIncludes);

        AssistantResponseDTO responseDto = new AssistantResponseDTO();

        JSONObject mainObject = getBaseMainObject(instructions, inputData.getInputValue(), inputData.getUserPrompt());
        mainObject.put("model", assistant.getModel());
        mainObject.put("stream", true);

        HttpPost post = new HttpPost(RESPONSES_URL);
        post.setEntity(getRequestBody(mainObject.toString()));
        setHeaders(post, true);
        //post.setHeader("Accept", "text/event-stream");

        try (CloseableHttpResponse response = client.execute(post)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, getServiceName(), "getAiStreamResponse");


            HttpEntity entity = response.getEntity();
            try (InputStream inputStream = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))) {

                    OpenRouterStreamHandler openRouterStreamHandler = new OpenRouterStreamHandler(replacedIncludes);
                    openRouterStreamHandler.handleBufferedReader(reader, writer);

                    handleUsage(responseDto, assistant, statRepo, request, openRouterStreamHandler.totalTokenCount);
            }

        }

        return responseDto;
    }

    @Override
    public AssistantResponseDTO getAiImageResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws Exception {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        Path tempFileFolder = AiTempFileStorage.getFileFolder();

        //Main JSON object
        JSONObject mainObject = new JSONObject();
        //Contents Array
        JSONArray messagesArray = new JSONArray();

        String instructions = AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData, null);

        if(inputData.getInputValueType().equals(InputDataDTO.InputValueType.IMAGE)) {
            //ITS IMAGE EDIT - I GOT IMAGE to edit AND I WILL RETURN IMAGE
            byte[] fileBytes = Files.readAllBytes(inputData.getInputFile().toPath());
            addMessageWithImage(messagesArray, instructions, inputData.getMimeType(), Base64.getEncoder().encodeToString(fileBytes));
        } else {
            //ITS IMAGE GENERATION - INPUT IS TEXT RETUN IMAGE
            addMessage(messagesArray, instructions);
        }

        mainObject.put("messages", messagesArray);
        mainObject.put("model", assistant.getModel());

        HttpPost httpPost = new HttpPost(RESPONSES_URL);
        setHeaders(httpPost, true);
        httpPost.setEntity(new StringEntity(mainObject.toString(), java.nio.charset.StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = client.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, getServiceName(), "getAiImageResponse");

            String responseText = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(responseText);

            String finishReason = getFinishReason(json);
            if (Tools.isNotEmpty(finishReason) && "STOP".equalsIgnoreCase(finishReason) == false) {
                responseDto.setError(finishReason);
                return responseDto;
            }

            JSONArray images = getImages(json);
            try {
                long datePart = Tools.getNow();
                for(int i = 0; i < images.length(); i++) {
                    JSONObject image = images.getJSONObject(i);

                    if(image.has("image_url") == true) {
                        //Its image (rest its just some crap)
                        String imageUrl = image.getJSONObject("image_url").getString("url");
                        // Its combination of prefix + format + base64 -> parse it
                        if(imageUrl.startsWith("data:"))
                            imageUrl = imageUrl.substring(5);
                        String[] urlParts = imageUrl.split(";");
                        if(urlParts.length != 2) throw new IllegalStateException("Something wrong");

                        String format = urlParts[0];
                        if(format.startsWith("image/")) format = format.substring(6);

                        String base64Image = urlParts[1];
                        if(base64Image.startsWith("base64,")) base64Image = base64Image.substring(7);

                        //Date pars is added so we can delet all images from same request (same request == same date time part)
                        String tmpFileName = "tmp-ai-" + DocTools.removeChars(assistant.getName()) + "-" + datePart + "-";

                        try {
                            tmpFileName = AiTempFileStorage.addImage(base64Image, tmpFileName, format, tempFileFolder);

                            //If no error, add file
                            responseDto.addTempFile(tmpFileName);
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            handleUsage(responseDto, assistant, statRepo, request, json);
        }

        return responseDto;
    }

    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop) {
        return null;
    }
}
