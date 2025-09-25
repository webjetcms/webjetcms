package sk.iway.iwcm.components.ai.providers;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

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
                //
                try {
                    JSONObject setInstructions = new JSONObject( assistant.getInstructions() );
                    if(setInstructions.has("inputText") == true) value = inputData.getInputValue();
                    if(Tools.isEmpty(value)) value = joinAllValues(AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData, null) , ".");
                } catch (JSONException e) {
                    value = assistant.getInstructions();
                }

            }
        } catch(Exception e) {
            return null;
        }

        return new Pair<>(useInstructions, value);
    }

    public static String joinAllValues(String json, String delimiter) {
        Object root = new JSONTokener(json).nextValue();
        List<String> values = new ArrayList<>();
        collectValues(root, values);
        return String.join(delimiter, values);
    }

    public static void collectValues(Object node, List<String> out) {
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