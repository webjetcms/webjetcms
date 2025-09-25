package sk.iway.iwcm.components.ai.providers.openrouter;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.providers.SupportLogic;

public abstract class OpenRouterSupportService extends SupportLogic {

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
    }

    public final StringEntity getRequestBody(String stringBody) {
        if(Tools.isEmpty(stringBody) == true) stringBody = "{}";
        return new StringEntity(stringBody, java.nio.charset.StandardCharsets.UTF_8);
    }

    protected JSONObject getBaseMainObject(String... contents) {
        //Main JSON object
        JSONObject mainObject = new JSONObject();
        //Contents Array
        JSONArray messagesArray = new JSONArray();
        //Add parts
        for (String content : contents) if(Tools.isNotEmpty(content)) addMessage(messagesArray, content);

        mainObject.put("messages", messagesArray);
        return mainObject;
    }

    protected void addMessageWithImage(JSONArray messagesArray, String value, String format, String base64Img) {
        JSONObject imageMsg = new JSONObject()
            .put("role", "user");

        JSONObject imageUrl = new JSONObject()
            .put("url", "data:" + format + ";base64," + base64Img);

         JSONArray content = new JSONArray()
            .put(
                new JSONObject()
                    .put("type", "image_url")
                    .put("image_url", imageUrl)
            );

        imageMsg.put("content", content);
        messagesArray.put(imageMsg);

        addMessage(messagesArray, value);
    }

    protected void addMessage(JSONArray messagesArray, String value) {
        JSONObject message = new JSONObject()
            .put("role", "user")
            .put("content", value);

        messagesArray.put(message);
    }
}
