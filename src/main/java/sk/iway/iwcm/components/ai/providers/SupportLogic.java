package sk.iway.iwcm.components.ai.providers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.FunctionTypes.ModelsExtractor;
import sk.iway.iwcm.components.ai.providers.FunctionTypes.ModelsRequest;
import sk.iway.iwcm.components.ai.providers.FunctionTypes.ResponseExtractor;
import sk.iway.iwcm.components.ai.providers.FunctionTypes.ResponseRequest;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

public class SupportLogic {

    public static List<LabelValue> getSupportedModels(Prop prop, String serviceName, ModelsRequest mr, ModelsExtractor me) {
        List<LabelValue> supportedValues = new ArrayList<>();

        CloseableHttpClient client = HttpClients.createDefault();
        try (CloseableHttpResponse response = client.execute(mr.get())) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, serviceName, "getSupportedModels");

            String value = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
            if (Tools.isEmpty(value)) return supportedValues;

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(value);

            supportedValues = me.apply(root);

            return supportedValues;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return supportedValues;
    }

    public static AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request, ResponseRequest rr, ResponseExtractor re) throws IOException {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();

        //Handle replace of INCLUDE tags
        Map<Integer, String> replacedIncludes = new HashMap<>();
        String inputText = IncludesHandler.replaceIncludesWithPlaceholders(inputData.getInputValue(), replacedIncludes);
        String instructions = replacedIncludes.isEmpty() ? assistant.getInstructions() : IncludesHandler.addProtectedTokenInstructionRule(assistant.getInstructions());

        CloseableHttpClient client = HttpClients.createDefault();
        try (CloseableHttpResponse response = client.execute(rr.apply(inputText, instructions))) {
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
                handleErrorMessage(response, prop, "", "getAiResponse");

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNodeRes = mapper.readTree(EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8));

            String responseText = re.apply(jsonNodeRes);
            responseDto.setResponse( replacedIncludes.isEmpty() ? responseText : IncludesHandler.returnIncludesToPlaceholders(responseText, replacedIncludes) );
        }

        return responseDto;
    }

    private static String handleErrorMessage(CloseableHttpResponse response, Prop prop) {
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

    private static String handleErrorMessage(CloseableHttpResponse response, Prop prop, String serviceName, String methodName) {
        String errMsg = handleErrorMessage(response, prop);
        Adminlog.add(Adminlog.TYPE_AI, serviceName + "." + methodName + " FAILED : " + errMsg, -1, -1);
        throw new IllegalStateException(errMsg);
    }

}
