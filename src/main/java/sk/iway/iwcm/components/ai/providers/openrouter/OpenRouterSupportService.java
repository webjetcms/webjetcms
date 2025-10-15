package sk.iway.iwcm.components.ai.providers.openrouter;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
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

    protected ObjectNode getBaseMainObject(String... contents) {
        ObjectNode mainObject = MAPPER.createObjectNode();
        ArrayNode messagesArray = MAPPER.createArrayNode();
        for (String content : contents) if (Tools.isNotEmpty(content)) addMessage(messagesArray, content);
        mainObject.set("messages", messagesArray);
        return mainObject;
    }

    protected void addMessageWithImage(ArrayNode messagesArray, String value, String format, String base64Img) {
        ObjectNode imageMsg = MAPPER.createObjectNode();
        imageMsg.put("role", "user");

        ObjectNode imageUrl = MAPPER.createObjectNode();
        imageUrl.put("url", "data:" + format + ";base64," + base64Img);

        ArrayNode content = MAPPER.createArrayNode();
        ObjectNode contentItem = MAPPER.createObjectNode();
        contentItem.put("type", "image_url");
        contentItem.set("image_url", imageUrl);
        content.add(contentItem);

        imageMsg.set("content", content);
        messagesArray.add(imageMsg);

        addMessage(messagesArray, value);
    }

    protected void addMessage(ArrayNode messagesArray, String value) {
        ObjectNode message = MAPPER.createObjectNode();
        message.put("role", "user");
        message.put("content", value);
        messagesArray.add(message);
    }

    public static String handleFinishReasonValue(String finishReason) {
        if(Tools.isEmpty(finishReason) || "STOP".equalsIgnoreCase(finishReason)) return null; // good
        else if("length".equalsIgnoreCase(finishReason)) return "The model reached the max_tokens limit before it could stop naturally.";
        else if("content_filter".equalsIgnoreCase(finishReason)) return "The output was flagged/blocked by the content filter.";
        else if("tool_calls".equalsIgnoreCase(finishReason)) return "The model stopped because it emitted a tool (function) call.";
        else return finishReason; //Jut in case
    }
}
