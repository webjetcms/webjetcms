package sk.iway.iwcm.components.ai.providers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpRequestBase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

public interface SupportLogicInterface {
    String getServiceName();
    int addUsageAndReturnTotal(StringBuilder sb, int addTokens, JsonNode root);

    HttpRequestBase getModelsRequest(HttpServletRequest request);
    List<LabelValue> extractModels(JsonNode root);

    HttpRequestBase getResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request);
    String extractResponseText(JsonNode jsonNodeRes);

    HttpRequestBase getStremResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request);
    JsonNode handleBufferedReader(BufferedReader reader,  BufferedWriter writer, Map<Integer, String> replacedIncludes) throws IOException;

    default String getStreamEncoding(HttpEntity entity) {
        return java.nio.charset.StandardCharsets.UTF_8.name();
    }

    HttpRequestBase getImageResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request, Prop prop) throws IOException;
    String getFinishReason(JsonNode jsonNodeRes);
    ArrayNode getImages(JsonNode jsonNodeRes);

    String getImageFormat(JsonNode jsonNodeRes, JsonNode jsonImage);
    String getImageBase64(JsonNode jsonNodeRes, JsonNode jsonImage);

    String getModelForImageNameGeneration();
}