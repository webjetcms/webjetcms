package sk.iway.iwcm.components.ai.providers.gemini;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiInterface;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;
import sk.iway.iwcm.components.ai.rest.AiTempFileStorage;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;

@Service
public class GeminiService extends GeminiSupportService implements AiInterface {

    private static final CloseableHttpClient client = HttpClients.createDefault();
    private static final String PROVIDER_ID = "gemini";
    private static final String TITLE_KEY = "components.ai_assistants.provider.gemini.title";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    public String getProviderId() {
        return PROVIDER_ID;
    }

    public boolean isInit() {
        return Tools.isNotEmpty(Constants.getString(AUTH_KEY));
    }

    public Pair<String, String> getProviderInfo(Prop prop) {
        return new Pair<>(PROVIDER_ID, prop.getText(TITLE_KEY));
    }

    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws Exception {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();

        //Prepare body object
        JSONObject mainObject = getBaseMainObject(assistant.getInstructions(), inputData.getInputValue());

        HttpPost httpPost = new HttpPost(BASE_URL + assistant.getModel() + ":generateContent");
        setHeaders(httpPost, request);
        httpPost.setEntity(new StringEntity(mainObject.toString(), java.nio.charset.StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = client.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "getAiResponse");

            String responseData = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(responseData);
            JSONArray parts = getParts(json);
            responseDto.setResponse(parts.getJSONObject(0).getString("text"));

            handleUsage(responseDto, json, 0, assistant, statRepo, request);
        }

        return responseDto;
    }

    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, PrintWriter writer, HttpServletRequest request) throws Exception {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();

        //Prepare body object
        JSONObject mainObject = getBaseMainObject(assistant.getInstructions(), inputData.getInputValue());

        HttpPost httpPost = new HttpPost(BASE_URL + assistant.getModel() + ":streamGenerateContent");
        setHeaders(httpPost, request);
        httpPost.setHeader("Accept", "text/event-stream");
        httpPost.setHeader("Accept-Encoding", "identity");
        httpPost.setEntity(new StringEntity(mainObject.toString(), java.nio.charset.StandardCharsets.UTF_8));

        CloseableHttpClient streamClient = HttpClients.custom().disableContentCompression().build();

        try (CloseableHttpResponse response = streamClient.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "getAiStreamResponse");

            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            GeminiStreamHandler streamHandler = new GeminiStreamHandler();
            streamHandler.handleBufferedReader(reader, writer);

            handleUsage(responseDto, streamHandler, 0, assistant, statRepo, request);
        }

        return responseDto;
    }

    public AssistantResponseDTO getAiImageResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws Exception {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        Path tempFileFolder = AiTempFileStorage.getFileFolder();

        Pair<String, Integer> generatedFileName = generateImageName(assistant, inputData, prop, request);
        responseDto.setGeneratedFileName( generatedFileName.getFirst() );

        //Main JSON object
        JSONObject mainObject = new JSONObject();
        //Contents Array
        JSONArray contentsArray = new JSONArray();

        if(inputData.getInputValueType().equals(InputDataDTO.InputValueType.IMAGE)) {
            //ITS IMAGE EDIT - I GOT IMAGE to edit AND I WILL RETURN IMAGE
            byte[] fileBytes = Files.readAllBytes(inputData.getInputFile().toPath());
            addPartWithFile(contentsArray, AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData), inputData.getMimeType(), Base64.getEncoder().encodeToString(fileBytes));
        } else {
            //ITS IMAGE GENERATION - INPUT IS TEXT RETUN IMAGE
            addPart(contentsArray, AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData));
        }

        mainObject.put("contents", contentsArray);

        HttpPost httpPost = new HttpPost( BASE_URL + assistant.getModel() + ":generateContent");
        setHeaders(httpPost, request);
        httpPost.setEntity(new StringEntity(mainObject.toString(), java.nio.charset.StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = client.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "getAiImageResponse");

            String responseText = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(responseText);

            String finishReason = null;
            try {
                JSONArray candidates = json.optJSONArray("candidates");
                if (candidates != null && candidates.length() > 0) {
                    for (int i = 0; i < candidates.length(); i++) {
                        JSONObject candidate = candidates.getJSONObject(i);
                        if (candidate.has("finishReason")) {
                            finishReason = candidate.getString("finishReason");
                        }
                    }
                }
            } catch (Exception e) {
                Logger.error(GeminiService.class, e);
            }
            if (Tools.isNotEmpty(finishReason) && "STOP".equals(finishReason) == false) {
                responseDto.setError(finishReason);
                return responseDto;
            }

            JSONArray parts = getParts(json);

            try {
                long datePart = Tools.getNow();
                for(int i = 0; i < parts.length(); i++) {
                    JSONObject part = parts.getJSONObject(i);
                    if(part.has("inlineData") == true) {
                        //Its image (rest its just some crap)
                        String base64Image = part.getJSONObject("inlineData").getString("data");
                        String format = part.getJSONObject("inlineData").optString("mimeType", "png");
                        if(format.startsWith("image/")) format = format.substring(6);
                        if(format.startsWith(".") == false) format = "." + format;

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

            handleUsage(responseDto, json, generatedFileName.getSecond(), assistant, statRepo, request);
        }

        return responseDto;
    }

    public List<LabelValue> getSupportedModels(Prop prop, HttpServletRequest request) {
        List<LabelValue> supportedValues = new ArrayList<>();
        String modelPrefix = "models/";

        HttpGet httpGet = new HttpGet(BASE_URL);
        setHeaders(httpGet, request);

        try (CloseableHttpResponse response = client.execute(httpGet)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "getSupportedModels");


            String value = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
            if (Tools.isEmpty(value)) return supportedValues;

            JSONObject root = new JSONObject(value);
            JSONArray models = root.getJSONArray("models");

            for (int i = 0; i < models.length(); i++) {
                JSONObject model = models.getJSONObject(i);

                String id = model.getString("name");
                if(id.startsWith(modelPrefix)) id = id.substring(modelPrefix.length());

                String displayName = model.optString("displayName", id);

                supportedValues.add(new LabelValue(displayName, id));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return supportedValues;
    }

    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop) {
        //No special params to show
        return null;
    }

    private Pair<String, Integer> generateImageName(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, HttpServletRequest request) {
        String defaultFileName = "ai_generated_image_" + new java.util.Date().getTime();
        int totalTokens = 0;

        //Now supportes only images
        if("generate_image".equals(assistant.getAction()) == false) return new Pair<>(defaultFileName, totalTokens);

        Pair<String, String> instructionValuePair = getInstructionsAndValue(assistant, inputData);
        if(instructionValuePair == null) return new Pair<>(defaultFileName, totalTokens);

        //Prepare body object
        JSONObject mainObject = getBaseMainObject(instructionValuePair.getFirst(), instructionValuePair.getSecond());

        HttpPost httpPost = new HttpPost(BASE_URL + "gemini-2.5-flash" + ":generateContent");
        setHeaders(httpPost, request);
        httpPost.setEntity(new StringEntity(mainObject.toString(), java.nio.charset.StandardCharsets.UTF_8));

        try (CloseableHttpResponse response = client.execute(httpPost)) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, SERVICE_NAME, "getAiResponse");

            JSONObject json = new JSONObject(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));
            JSONArray parts = getParts(json);
            String imageName = parts.getJSONObject(0).getString("text");

            if (json.has("usageMetadata")) {
                JSONObject usage = json.getJSONObject("usageMetadata");
                int candToken = usage.optInt("candidatesTokenCount", 0);
                int thouToken = usage.optInt("thoughtsTokenCount", 0);
                int promToken = usage.optInt("promptTokenCount", 0);
                totalTokens = usage.optInt("totalTokenCount", 0);

                StringBuilder sb = new StringBuilder("");
                sb.append(SERVICE_NAME).append(" -> generating name for image succesfull.");
                sb.append("\nAction cost: \n");
                sb.append("\t candidatesTokenCount: ").append(candToken).append("\n");
                sb.append("\t thoughtsTokenCount: ").append(thouToken).append("\n");
                sb.append("\t promptTokenCount: ").append(promToken).append("\n");
                sb.append("\t totalTokenCount: ").append(totalTokens).append("\n");
                Adminlog.add(Adminlog.TYPE_AI, sb.toString(), totalTokens, -1);
            }

            return new Pair<>(imageName, totalTokens);
        } catch (Exception e) {
            return new Pair<>(defaultFileName, totalTokens);
        }
    }
}
