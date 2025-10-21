package sk.iway.iwcm.components.ai.providers.openrouter;

import java.io.IOException;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.providers.SupportLogic;

public abstract class OpenRouterSupportService extends SupportLogic {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    public String getServiceName() {
        return "OpenRouter";
    }

    protected static String getApiKey() {
        return Constants.getString("ai_openRouterAuthKey");
    }

    protected <T extends HttpRequestBase> void setHeaders(T http, boolean addContentType) {
        String apiKey = getApiKey();
        if(Tools.isEmpty(apiKey)) throw new IllegalStateException("OpenRouter API key is not set.");
        http.setHeader("Authorization", "Bearer " + apiKey);
        if(addContentType) http.setHeader("Content-Type", "application/json; charset=utf-8");

        //Optional. Site URL for rankings on openrouter.ai
        http.setHeader("HTTP-Referer", "https://www.webjetcms.com/");
        //Optional. Site title for rankings on openrouter.ai
        http.setHeader("X-Title", "WebJET CMS");
    }

    public final StringEntity getRequestBody(String stringBody) {
        if(Tools.isEmpty(stringBody) == true) stringBody = "{}";
        return new StringEntity(stringBody, java.nio.charset.StandardCharsets.UTF_8);
    }

    protected ObjectNode getBaseMainObject(String... contents) throws IOException {
        return getBaseMainObjectWithImage(null, contents);
    }

    protected ObjectNode getBaseMainObjectWithImage(InputDataDTO inputData, String... contents) throws IOException {
        ObjectNode mainObject = MAPPER.createObjectNode();

        ArrayNode contentArray = MAPPER.createArrayNode();

        // Text content
        for (String content : contents)
            if (Tools.isNotEmpty(content)) addContent(contentArray, content);

        // Image content
        if(inputData != null && InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType()))
            addImageContent(contentArray, inputData);

        ObjectNode message = MAPPER.createObjectNode();
        message.put("role", "user");
        message.set("content", contentArray);

        ArrayNode messagesArray = MAPPER.createArrayNode();
        messagesArray.add(message);

        mainObject.set("messages", messagesArray);
        return mainObject;
    }

    protected void addImageContent(ArrayNode contentArray, InputDataDTO inputData) throws IOException {
        ObjectNode contentItem = MAPPER.createObjectNode();
        contentItem.put("type", "image_url");

        ObjectNode imageUrl = MAPPER.createObjectNode();
        imageUrl.put("url", "data:" + inputData.getMimeType() + ";base64," + inputData.getFileAsBase64());

        contentItem.set("image_url", imageUrl);
        contentArray.add(contentItem);
    }

    private void addContent(ArrayNode contentArray, String value) {
        ObjectNode contentItem = MAPPER.createObjectNode();
        contentItem.put("type", "text");
        contentItem.put("text", value);
        contentArray.add(contentItem);
    }

    public static String handleFinishReasonValue(String finishReason) {
        if(Tools.isEmpty(finishReason) || "STOP".equalsIgnoreCase(finishReason)) return null; // good
        else if("length".equalsIgnoreCase(finishReason)) return "The model reached the max_tokens limit before it could stop naturally.";
        else if("content_filter".equalsIgnoreCase(finishReason)) return "The output was flagged/blocked by the content filter.";
        else if("tool_calls".equalsIgnoreCase(finishReason)) return "The model stopped because it emitted a tool (function) call.";
        else return finishReason; //Jut in case
    }
}
