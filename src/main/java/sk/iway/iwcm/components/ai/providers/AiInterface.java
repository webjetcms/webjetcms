package sk.iway.iwcm.components.ai.providers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;

/**
 * Interface for AI providers - main methods to implement in specific provider services
 */
public interface AiInterface {

    static final String GENERATE_FILE_NAME_KEY = "ai_generateFileNamePrompt";

    public List<LabelValue> getSupportedModels(Prop prop, HttpServletRequest request);

    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws Exception;
    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, BufferedWriter writer, HttpServletRequest request) throws Exception;
    public AssistantResponseDTO getAiImageResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws Exception;

    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop);

    public String getProviderId();
    public String getTitleKey();
    public boolean isInit();

    default String handleErrorMessage(CloseableHttpResponse response, Prop prop) {
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

    default String handleErrorMessage(CloseableHttpResponse response, Prop prop, String serviceName, String methodName) {
        String errMsg = handleErrorMessage(response, prop);
        Adminlog.add(Adminlog.TYPE_AI, serviceName + "." + methodName + " FAILED : " + errMsg, -1, -1);
        throw new IllegalStateException(errMsg);
    }

    default Pair<String, String> getInstructionsAndValue(AssistantDefinitionEntity assistant, InputDataDTO inputData) {
        String useInstructions = Constants.getString(GENERATE_FILE_NAME_KEY);
        if(Tools.isEmpty(useInstructions) == true) return null;

        String value = "";
        try {
            //Value is something special
            if(Tools.isNotEmpty(inputData.getUserPrompt()) == true) value = inputData.getUserPrompt();
            else {
                //
                try {
                    JSONObject setInstructions = new JSONObject( assistant.getInstructions() );
                    if(setInstructions.has("inputText") == true) value = inputData.getInputValue();
                    if(Tools.isEmpty(value)) value = joinAllValues(AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData) , ".");
                } catch (JSONException e) {
                    value = assistant.getInstructions();
                }

            }
        } catch(Exception e) {
            return null;
        }

        return new Pair<>(useInstructions, value);
    }

    private String joinAllValues(String json, String delimiter) {
        Object root = new JSONTokener(json).nextValue();
        List<String> values = new ArrayList<>();
        collectValues(root, values);
        return String.join(delimiter, values);
    }

    private void collectValues(Object node, List<String> out) {
        if (node == JSONObject.NULL || node == null) return;

        if (node instanceof JSONObject obj) {
        List<String> keys = new ArrayList<>(obj.keySet());
        for (String k : keys) collectValues(obj.get(k), out);
        return;
        }

        if (node instanceof JSONArray arr) {
        for (int i = 0; i < arr.length(); i++) collectValues(arr.get(i), out);
        return;
        }

        // Scalar types
        if (node instanceof String s) out.add(s);
        else if (node instanceof Number n) out.add(n.toString());
        else if (node instanceof Boolean b) out.add(Boolean.toString(b));
        // (skip nulls)
    }

    default StringEntity getRequestBody(String stringBody) {
        if(Tools.isEmpty(stringBody) == true) stringBody = "{}";
        return new StringEntity(stringBody, java.nio.charset.StandardCharsets.UTF_8);
    }
}