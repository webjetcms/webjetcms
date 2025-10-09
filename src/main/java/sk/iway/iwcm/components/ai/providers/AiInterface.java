package sk.iway.iwcm.components.ai.providers;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.entity.StringEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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

    List<LabelValue> getSupportedModels(Prop prop, HttpServletRequest request);

    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws Exception;
    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, BufferedWriter writer, HttpServletRequest request) throws Exception;
    public AssistantResponseDTO getAiImageResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws Exception;

    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop);

    public String getProviderId();
    public String getTitleKey();
    public boolean isInit();

    public static Pair<String, String> getInstructionsAndValue(AssistantDefinitionEntity assistant, InputDataDTO inputData) {
        String useInstructions = Constants.getString(GENERATE_FILE_NAME_KEY);
        if(Tools.isEmpty(useInstructions) == true) return null;

        String value = "";
        try {
            //Value is something special
            if(Tools.isNotEmpty(inputData.getUserPrompt()) == true) value = inputData.getUserPrompt();
            else {
                // Try to parse instructions JSON and inspect inputText field
                try {
                    ObjectMapper mapper = getObjectMapper();
                    JsonNode setInstructions = mapper.readTree(assistant.getInstructions());
                    if(setInstructions.has("inputText")) value = inputData.getInputValue();
                    if(Tools.isEmpty(value)) value = joinAllValues(AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData, null) , ".");
                } catch (Exception e) {
                    // Not a JSON structure, just use raw instructions text
                    value = assistant.getInstructions();
                }

            }
        } catch(Exception e) {
            return null;
        }

        return new Pair<>(useInstructions, value);
    }

    public static String joinAllValues(String json, String delimiter) {
        if (Tools.isEmpty(json)) return "";
        List<String> values = new ArrayList<>();
        try {
            JsonNode root = getObjectMapper().readTree(json);
            collectValues(root, values);
        } catch (JsonProcessingException e) {
            // Not JSON, just return raw string
            return json;
        }
        return String.join(delimiter, values);
    }

    public static void collectValues(JsonNode node, List<String> out) {
        if (node == null || node.isNull()) return;

        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            obj.fieldNames().forEachRemaining(f -> collectValues(obj.get(f), out));
            return;
        }
        if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (JsonNode child : arr) collectValues(child, out);
            return;
        }
        if (node.isTextual()) out.add(node.asText());
        else if (node.isNumber()) out.add(node.numberValue().toString());
        else if (node.isBoolean()) out.add(Boolean.toString(node.booleanValue()));
        // other node types (binary, POJO) are ignored for aggregation
    }

    static ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    default StringEntity getRequestBody(String stringBody) {
        if(Tools.isEmpty(stringBody) == true) stringBody = "{}";
        return new StringEntity(stringBody, java.nio.charset.StandardCharsets.UTF_8);
    }
}