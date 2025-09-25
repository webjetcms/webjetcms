package sk.iway.iwcm.components.ai.providers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;
import sk.iway.iwcm.components.ai.rest.AiTempFileStorage;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.components.ai.stat.rest.AiStatService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;

public abstract class SupportLogic implements SupportLogicInterface {

    public List<LabelValue> getSupportedModels(Prop prop, HttpServletRequest request) {
        List<LabelValue> supportedValues = new ArrayList<>();

        CloseableHttpClient client = HttpClients.createDefault();
        try (CloseableHttpResponse response = client.execute( getModelsRequest(request) )) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, getServiceName(), "getSupportedModels");

            String value = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
            if (Tools.isEmpty(value)) return supportedValues;

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(value);

            supportedValues = extractModels(root);

            return supportedValues;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return supportedValues;
    }

    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws IOException {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();

        //Handle replace of INCLUDE tags
        Map<Integer, String> replacedIncludes = IncludesHandler.replaceIncludesWithPlaceholders(inputData);
        String instructions = AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData, replacedIncludes);

        CloseableHttpClient client = HttpClients.createDefault();
        try (CloseableHttpResponse response = client.execute( getResponseRequest(instructions, inputData, assistant, request)) ) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, "", "getAiResponse");

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNodeRes = mapper.readTree(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));

            String responseText = extractResponseText(jsonNodeRes);
            responseDto.setResponse( replacedIncludes.isEmpty() ? responseText : IncludesHandler.returnIncludesToPlaceholders(responseText, replacedIncludes) );

            //Usage
            handleUsage(responseDto, assistant, jsonNodeRes, 0, statRepo, request);
        }

        return responseDto;
    }

    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, BufferedWriter writer, HttpServletRequest request) throws Exception {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();

        //Handle replace of INCLUDE tags
        Map<Integer, String> replacedIncludes = IncludesHandler.replaceIncludesWithPlaceholders(inputData);
        String instructions = AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData, replacedIncludes);

        CloseableHttpClient client = HttpClients.createDefault();
        try (CloseableHttpResponse response = client.execute( getStremResponseRequest(instructions, inputData, assistant, request) )) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, getServiceName(), "getAiStreamResponse");

            HttpEntity entity = response.getEntity();
            String encoding = getStreamEncoding(entity);

            try (InputStream inputStream = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, encoding))) {

                    JsonNode usage = handleBufferedReader(reader, writer, replacedIncludes);
                    handleUsage(responseDto, assistant, usage, 0, statRepo, request);
            }
        }

        return responseDto;
    }

    public AssistantResponseDTO getAiImageResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws IOException {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        Path tempFileFolder = AiTempFileStorage.getFileFolder();

        //Pair<String, Integer> generatedFileName = generateImageName(assistant, inputData);
        Pair<String, Integer> generatedFileName = getGeneratedImageName(assistant, inputData, prop, statRepo, request);
        responseDto.setGeneratedFileName(generatedFileName.getFirst());

        String instructions = AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData, null);

        CloseableHttpClient client = HttpClients.createDefault();
        try (CloseableHttpResponse response = client.execute( getImageResponseRequest(instructions, inputData, assistant, request, prop) )) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, getServiceName(), "getAiImageResponse");

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNodeRes = mapper.readTree(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));

            String finishReason = getFinishReason(jsonNodeRes);
            if (Tools.isNotEmpty(finishReason) && "STOP".equalsIgnoreCase(finishReason) == false) {
                responseDto.setError(finishReason);
                return responseDto;
            }

            ArrayNode imagesArr = getImages(jsonNodeRes);
            try {
                long datePart = Tools.getNow();
                for(JsonNode jsonImage : imagesArr) {
                    String format = getImageFormat(jsonNodeRes, jsonImage);
                    String base64Image = getImageBase64(jsonNodeRes, jsonImage);

                    if(Tools.isEmpty(base64Image)) throw new IllegalStateException("Image acquire unsuccesfull");

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

                handleUsage(responseDto, assistant, jsonNodeRes, generatedFileName.getSecond(), statRepo, request);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return responseDto;
    }


    private void handleUsage(AssistantResponseDTO responseDto, AssistantDefinitionEntity assistant, JsonNode jsonNodeRes, int addTokens, AiStatRepository statRepo, HttpServletRequest request) {
        StringBuilder sb = new StringBuilder("");
        sb.append(getServiceName()).append(" -> run was succesfull");
        sb.append("\n\n");
        sb.append("Assitant name : ").append(assistant.getName()).append("\n");
        sb.append("From field : ").append(assistant.getFieldFrom()).append("\n");
        sb.append("To field : ").append(assistant.getFieldTo()).append("\n");
        sb.append("\n");
        sb.append("Action cost: \n");
        int totalTokens = addUsageAndReturnTotal(sb, addTokens, jsonNodeRes);

        try {
            Adminlog.add(Adminlog.TYPE_AI, sb.toString(), totalTokens, -1);
            AiStatService.addRecord(assistant.getId(), totalTokens, statRepo, request);
        } catch (Exception e) {
            e.printStackTrace();
        }

        responseDto.setTotalTokens(totalTokens);
    }

    public String handleErrorMessage(CloseableHttpResponse response, Prop prop) {
        int code = response.getStatusLine().getStatusCode();
        String defaultErrMsg = " (" + code + ") " + prop.getText("html_area.insert_image.error_occured");
        try {
            String responseBody = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode array = mapper.readTree(responseBody);

            //Gemini send it aj array with objects ... OpenAI just as object (we need to handle it)
            JSONObject jsonObject;
            if(array.isArray()) {
                jsonObject = new JSONObject(array.get(0).toString());
            } else {
                jsonObject = new JSONObject(responseBody);
            }

            if (jsonObject.has("error")) {
                JSONObject error = jsonObject.getJSONObject("error");

                //This handles OpenRouter error detail message
                if(error.has("metadata")) {
                    JSONObject errMetadata = error.getJSONObject("metadata");
                    if(errMetadata.has("raw")) {
                        return " (" + code + ") " + errMetadata.getString("raw");
                    }
                }

                if (error.has("message")) {
                    String errorMessage = error.getString("message");
                    return " (" + code + ") " + errorMessage;
                } else {
                    return defaultErrMsg;
                }
            } else {
                return defaultErrMsg;
            }
        } catch (IOException ex) {
            return defaultErrMsg;
        }
    }

    public String handleErrorMessage(CloseableHttpResponse response, Prop prop, String serviceName, String methodName) {
        String errMsg = handleErrorMessage(response, prop);
        Adminlog.add(Adminlog.TYPE_AI, serviceName + "." + methodName + " FAILED : " + errMsg, -1, -1);
        throw new IllegalStateException(errMsg);
    }

    private Pair<String, Integer> getGeneratedImageName(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) {
        String defaultFileName = "ai_generated_image_" + new java.util.Date().getTime();

        if("generate_image".equals(assistant.getAction()) == false) return new Pair<>(defaultFileName, 0);

        Pair<String, String> instructionValuePair = AiInterface.getInstructionsAndValue(assistant, inputData);
        if(instructionValuePair == null) return new Pair<>(defaultFileName, 0);

        AssistantDefinitionEntity fakeAssistant = new AssistantDefinitionEntity();
        fakeAssistant.setId(-1L);
        fakeAssistant.setModel( getModelForImageNameGeneration() );
        fakeAssistant.setUseStreaming(false);
        fakeAssistant.setInstructions(instructionValuePair.getFirst());
        fakeAssistant.setUseTemporal(true);

        InputDataDTO fakeData = new InputDataDTO();
        fakeData.setInputValue(instructionValuePair.getSecond());

        try {
            AssistantResponseDTO response = getAiResponse(fakeAssistant, fakeData, prop, statRepo, request);

            if(Tools.isNotEmpty(response.getError())) return new Pair<>(defaultFileName, 0);

            return new Pair<>(response.getResponse(), response.getTotalTokens());

        } catch(Exception e) {
            e.printStackTrace();
            return new Pair<>(defaultFileName, 0);
        }
    }
}
