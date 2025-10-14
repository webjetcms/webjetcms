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
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
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

/**
 * Support logic for AI model integration - common methods
 */
public abstract class SupportLogic implements SupportLogicInterface {

    private static final String METHOD_TEXT_RESPONSE = "getAiResponse";
    private static final String METHOD_TEXT_STREAM_RESPONSE = "getAiStreamResponse";
    private static final String METHOD_IMAGE_RESPONSE = "getAiImageResponse";

    private static final String IMAGE_AUDIT_RESPONSE = "Provider response for image contains large Base64 value, for this reason we do not audit them.";

    public List<LabelValue> getSupportedModels(Prop prop, HttpServletRequest request) {
        List<LabelValue> supportedValues = new ArrayList<>();

        try (CloseableHttpResponse response = HttpClients.createDefault().execute( getModelsRequest(request) )) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300) {
                Pair<String, String> errorPair = handleErrorMessage(response, prop);
                StringBuilder sb = new StringBuilder("");
                sb.append(getServiceName()).append(" getSupportedModels -> FAILED");
                sb.append("\n\nError message: ").append("\n").append(errorPair.getFirst());
                sb.append("\n\nFull response: ").append("\n").append( Tools.isEmpty(errorPair.getSecond()) == true ? " - " : errorPair.getSecond() );
                Adminlog.add(Adminlog.TYPE_AI, sb.toString(), -1, -1);
                return supportedValues;
            }

            String value = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
            if (Tools.isEmpty(value)) return supportedValues;

            JsonNode root = new ObjectMapper().readTree(value);
            return extractModels(root);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return supportedValues;
    }

    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws ProviderCallException {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        Pair<String, String> inputPair = new Pair<>(inputData.getInputValue(), inputData.getUserPrompt());
        String fullResponse = "";

        try {
            //Handle replace of INCLUDE tags
            Map<Integer, String> replacedIncludes = IncludesHandler.replaceIncludesWithPlaceholders(inputData);
            String instructions = AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData, replacedIncludes);

            try (CloseableHttpResponse response = HttpClients.createDefault().execute( getResponseRequest(instructions, inputData, assistant, request)) ) {
                if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                    errorAdminLog(assistant, inputPair, METHOD_TEXT_RESPONSE, response, prop);

                fullResponse = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
                JsonNode jsonNodeRes = new ObjectMapper().readTree(fullResponse);

                String finishError = getFinishError(jsonNodeRes);
                if(Tools.isNotEmpty(finishError)) throw new IllegalStateException(finishError);

                String responseText = extractResponseText(jsonNodeRes);
                responseDto.setResponse( replacedIncludes.isEmpty() ? responseText : IncludesHandler.returnIncludesToPlaceholders(responseText, replacedIncludes) );

                //Usage
                succesAdminLog(responseDto, inputPair, assistant, METHOD_TEXT_RESPONSE, responseDto.getResponse(), jsonNodeRes, 0, statRepo, request);
            }
        } catch(ProviderCallException pce) {
            // Error from response, audited already. Only re-throw
            throw pce;
        } catch (Exception e) {
            // Other type of error - audit and throw
            errorAdminLog(assistant, inputPair, METHOD_TEXT_RESPONSE, new Pair<>(e.getLocalizedMessage(), fullResponse));
        }

        return responseDto;
    }

    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, BufferedWriter writer, HttpServletRequest request) throws ProviderCallException {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        Pair<String, String> inputPair = new Pair<>(inputData.getInputValue(), inputData.getUserPrompt());

        try {
            //Handle replace of INCLUDE tags
            Map<Integer, String> replacedIncludes = IncludesHandler.replaceIncludesWithPlaceholders(inputData);
            String instructions = AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData, replacedIncludes);

            try (CloseableHttpResponse response = HttpClients.createDefault().execute( getStremResponseRequest(instructions, inputData, assistant, request) )) {
                if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                    errorAdminLog(assistant, inputPair, METHOD_TEXT_STREAM_RESPONSE, response, prop);


                HttpEntity entity = response.getEntity();
                String encoding = getStreamEncoding(entity);

                try (InputStream inputStream = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, encoding))) {
                        Pair<String, JsonNode> results = handleBufferedReader(reader, writer, replacedIncludes);
                        succesAdminLog(responseDto, inputPair, assistant, METHOD_TEXT_STREAM_RESPONSE, results.getFirst(), results.getSecond(), 0, statRepo, request);
                }
            }
        } catch(ProviderCallException pce) {
            // Error from response, audited already. Only re-throw
            throw pce;
        } catch (Exception e) {
            // Other type of error - audit and throw
            errorAdminLog(assistant, inputPair, METHOD_TEXT_STREAM_RESPONSE, new Pair<>(e.getLocalizedMessage(), null));
        }

        return responseDto;
    }

    public AssistantResponseDTO getAiImageResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws ProviderCallException {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        Pair<String, String> inputPair = new Pair<>(inputData.getInputValue(), inputData.getUserPrompt());
        String responseText = null;

        try {
            Path tempFileFolder = AiTempFileStorage.getFileFolder();
            Pair<String, Integer> generatedFileName = getGeneratedImageName(assistant, inputData, prop, statRepo, request);
            responseDto.setGeneratedFileName(generatedFileName.getFirst());

            String instructions = AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData, null);

            try (CloseableHttpResponse response = HttpClients.createDefault().execute( getImageResponseRequest(instructions, inputData, assistant, request, prop) )) {
                if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                    errorAdminLog(assistant, inputPair, METHOD_IMAGE_RESPONSE, response, prop);

                responseText = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
                JsonNode jsonNodeRes = new ObjectMapper().readTree(responseText);

                String finishError = getFinishError(jsonNodeRes);
                if(Tools.isNotEmpty(finishError)) throw new IllegalStateException(finishError);

                ArrayNode imagesArr = getImages(jsonNodeRes);
                long datePart = Tools.getNow();
                for(JsonNode jsonImage : imagesArr) {

                    try { //temporal fix for google gemini retarded bug, where first (maybe not allways first) child in array is "text": "`" and not an actual image
                        if(jsonImage.has("text") == true) continue;
                    } catch(Exception e) { }

                    try { // In try catch, if one image fails, other's can be processed
                        String format = getImageFormat(jsonNodeRes, jsonImage);
                        if(format.startsWith(".") == false) format = "." + format;
                        if(FileTools.isImage(format) == false) throw new IllegalStateException("Image format is not valid: " + format);

                        String base64Image = getImageBase64(jsonNodeRes, jsonImage);
                        if(Tools.isEmpty(base64Image)) throw new IllegalStateException("Image acquire unsuccessful");

                        //Date pars is added so we can delet all images from same request (same request == same date time part)
                        String tmpFileName = "tmp-ai-" + DocTools.removeChars(assistant.getName()) + "-" + datePart + "-";

                        tmpFileName = AiTempFileStorage.addImage(base64Image, tmpFileName, format, tempFileFolder);

                        //If no error, add file
                        responseDto.addTempFile(tmpFileName);
                    } catch (IOException ioe) {
                        // Log and continue with next image
                        ioe.printStackTrace();
                    }
                }

                // Last check, at least one VALID image must be generated
                if(responseDto.getTempFiles() == null || responseDto.getTempFiles().isEmpty())
                    throw new IllegalStateException(prop.getText("components.ai_assistants.no_image.err"));

                //handleUsage(responseDto, assistant, jsonNodeRes, generatedFileName.getSecond(), statRepo, request);
                succesAdminLog(responseDto, inputPair, assistant, METHOD_IMAGE_RESPONSE, IMAGE_AUDIT_RESPONSE, jsonNodeRes, generatedFileName.getSecond(), statRepo, request);
            }
        } catch(ProviderCallException pce) {
            // Error from response, audited already. Only re-throw
            throw pce;
        } catch (Exception e) {
            // Other type of error - audit and throw
            errorAdminLog(assistant, inputPair, METHOD_IMAGE_RESPONSE, new Pair<>(e.getLocalizedMessage(), responseText));
        }

        return responseDto;
    }

    public Pair<String, String> handleErrorMessage(CloseableHttpResponse response, Prop prop) {
        int code = response.getStatusLine().getStatusCode();
        String defaultErrMsg = " (" + code + ") " + prop.getText("html_area.insert_image.error_occured");
        String responseBody = "";

        try {
            responseBody = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
            JsonNode root = new ObjectMapper().readTree(responseBody);

            // Gemini sends an array with one object; OpenAI sends a single object
            JsonNode objNode = root.isArray() && root.size() > 0 ? root.get(0) : root;
            if (objNode == null || objNode.isMissingNode()) return new Pair<>(defaultErrMsg, responseBody);

            JsonNode errorNode = objNode.get("error");
            if (errorNode == null || errorNode.isMissingNode()) return new Pair<>(defaultErrMsg, responseBody);

            // Handle OpenRouter error detail message metadata.raw
            JsonNode metadataNode = errorNode.get("metadata");
            if (metadataNode != null && metadataNode.has("raw")) {
                String raw = metadataNode.get("raw").asText(null);
                if (Tools.isNotEmpty(raw)) return new Pair<>(" (" + code + ") " + raw, responseBody);
            }

            JsonNode messageNode = errorNode.get("message");
            if (messageNode != null && Tools.isNotEmpty(messageNode.asText())) {
                return new Pair<>(" (" + code + ") " + messageNode.asText(), responseBody);
            }
            return new Pair<>(defaultErrMsg, responseBody);
        } catch (IOException ex) {
            return new Pair<>(defaultErrMsg, responseBody);
        }
    }

    private Pair<String, Integer> getGeneratedImageName(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) {
        String defaultFileName = "ai_generated_image_" + new java.util.Date().getTime();

        if("generate_image".equals(assistant.getAction()) == false) return new Pair<>(defaultFileName, 0);

        Pair<String, String> instructionValuePair = AiInterface.getInstructionsAndValue(assistant, inputData);
        if(instructionValuePair == null) return new Pair<>(defaultFileName, 0);

        AssistantDefinitionEntity fakeAssistant = new AssistantDefinitionEntity();
        fakeAssistant.setId(-1L);
        fakeAssistant.setName("Image name generator");
        fakeAssistant.setProvider(assistant.getProvider());
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

    private int getAuditMaxLength() { return Constants.getInt("ai_auditMaxLength"); }

    private void setAssistantInfo(AssistantDefinitionEntity assistant, StringBuilder sb) {
        sb.append("\n\nAssistant ID: ").append(assistant.getId()).append("\n");
        if (Tools.isNotEmpty(assistant.getName())) sb.append("Assistant name: ").append(assistant.getName()).append("\n");
        if (Tools.isNotEmpty(assistant.getProvider())) sb.append("Provider: ").append(assistant.getProvider()).append("\n");
        if (Tools.isNotEmpty(assistant.getModel())) sb.append("Model: ").append(assistant.getModel()).append("\n");
        if (Tools.isNotEmpty(assistant.getFieldFrom())) sb.append("Field from: ").append(assistant.getFieldFrom()).append("\n");
        if (Tools.isNotEmpty(assistant.getFieldTo())) sb.append("Field to: ").append(assistant.getFieldTo());
    }

    private void succesAdminLog(AssistantResponseDTO responseDto, Pair<String, String> inputPair, AssistantDefinitionEntity assistant, String methodName, String textResponse, JsonNode usageJsonNodeRes, int addTokens, AiStatRepository statRepo, HttpServletRequest request) {
        StringBuilder sb = new StringBuilder("");
        sb.append(getServiceName()).append(" ").append(methodName).append(" -> SUCCESSFUL");

        // Assistant Info
        setAssistantInfo(assistant, sb);

        //Usage info - Only for successful requests
        sb.append("\n");
        sb.append("Action cost: \n");
        int totalTokens = addUsageAndReturnTotal(sb, addTokens, usageJsonNodeRes);

        int auditMaxLength = getAuditMaxLength();
        if(auditMaxLength > 0) {
            StringBuilder bonusInfo = new StringBuilder();
            bonusInfo.append("\n\nAI Input value: ").append("\n").append( Tools.isEmpty(inputPair.getFirst()) == true ? " - " : inputPair.getFirst() );
            bonusInfo.append("\n\nAI user prompt: ").append("\n").append( Tools.isEmpty(inputPair.getSecond()) == true ? " - " : inputPair.getSecond() );
            bonusInfo.append("\n\nAI response: ").append("\n").append( textResponse );

            sb.append( DB.prepareString(bonusInfo.toString(), auditMaxLength) );
        }

        try {
            Adminlog.add(Adminlog.TYPE_AI, sb.toString(), totalTokens, -1);
            AiStatService.addRecord(assistant.getId(), totalTokens, statRepo, request);
        } catch (Exception e) { e.printStackTrace(); }

        responseDto.setTotalTokens(totalTokens);
    }

    private void errorAdminLog(AssistantDefinitionEntity assistant, Pair<String, String> inputPair, String methodName, CloseableHttpResponse response, Prop prop) throws ProviderCallException{
        Pair<String, String> errPair = handleErrorMessage(response, prop);
        errorAdminLog(assistant, inputPair, methodName, errPair);
    }

    private void errorAdminLog(AssistantDefinitionEntity assistant, Pair<String, String> inputPair, String methodName, Pair<String, String> errPair) throws ProviderCallException{
        StringBuilder sb = new StringBuilder("");
        sb.append(getServiceName()).append(" ").append(methodName).append(" -> FAILED");

        // Assistant Info
        setAssistantInfo(assistant, sb);

        // Add error
        sb.append("\n\nError message: ").append("\n").append(errPair.getFirst());

        // Bonus info
        sb.append("\n\nAI Input value: ").append("\n").append( Tools.isEmpty(inputPair.getFirst()) == true ? " - " : inputPair.getFirst() );
        sb.append("\n\nAI user prompt: ").append("\n").append( Tools.isEmpty(inputPair.getSecond()) == true ? " - " : inputPair.getSecond() );
        sb.append("\n\nFull response: ").append("\n").append( Tools.isEmpty(errPair.getSecond()) == true ? " - " : errPair.getSecond() );

        Adminlog.add(Adminlog.TYPE_AI, sb.toString(), -1, -1);

        throw new ProviderCallException(errPair.getFirst());
    }
}