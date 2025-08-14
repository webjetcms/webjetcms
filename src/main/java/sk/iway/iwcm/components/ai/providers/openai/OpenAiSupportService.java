package sk.iway.iwcm.components.ai.providers.openai;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import org.json.JSONObject;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;

public abstract class OpenAiSupportService {
    protected static final String THREADS_URL = "https://api.openai.com/v1/threads/";
    protected static final String ASSISTANTS_URL = "https://api.openai.com/v1/assistants";
    protected static final String MODELS_URL = "https://api.openai.com/v1/models";
    protected static final String IMAGES_URL = "https://api.openai.com/v1/images/edits";

    protected enum ASSISTANT_FIELDS {
        ID("id", String.class),
        NAME("name", String.class),
        INSTRUCTIONS("instructions", String.class),
        MODEL("model", String.class),
        CREATED_AT("created_at", Date.class),
        DESCRIPTION("description", String.class),
        TEMPERATURE("temperature", BigDecimal.class);

        private final String value;
        private final Class<?> type;

        ASSISTANT_FIELDS(String value, Class<?> type) {
            this.value = value;
            this.type= type;
        }

        public String value() {
            return value;
        }

        public Class<?> type() {
            return type;
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> T getValue(JSONObject assistant, ASSISTANT_FIELDS field) {
        if(field.type() == String.class) {
            return (T) assistant.optString(field.value(), "");
        } else if(field.type() == BigDecimal.class) {
            return (T) assistant.optBigDecimal(field.value(), BigDecimal.ONE);
        } else if(field.type() == Date.class) {
            return (T) new Date( assistant.optLong(field.value(), 0L) );
        }
        return null;
    }


    protected final void addHeaders(org.apache.http.client.methods.HttpRequestBase request, boolean addContentType, boolean isAssistantV2) {
        String apiKey = Constants.getString("open_ai_auth_key");
        if(Tools.isEmpty(apiKey)) throw new IllegalStateException("OpenAI API key is not set.");
        request.setHeader("Authorization", "Bearer " + apiKey);
        if(addContentType) request.setHeader("Content-Type", "application/json");
        if(isAssistantV2) request.setHeader("OpenAI-Beta", "assistants=v2");
    }

    protected final StringEntity getRequestBody(String stringBody) {
        if(Tools.isEmpty(stringBody) == true) stringBody = "{}";
        return new StringEntity(stringBody, java.nio.charset.StandardCharsets.UTF_8);
    }

    protected String handleErrorMessage(CloseableHttpResponse response, Prop prop) {
        int code = response.getStatusLine().getStatusCode();
        String defaultErrMsg = " (" + code + ") " + prop.getText("html_area.insert_image.error_occured");
        try {
            String responseBody = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
            JSONObject jsonObject = new JSONObject(responseBody);
            if (jsonObject.has("error")) {
                JSONObject error = jsonObject.getJSONObject("error");
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

    protected String handleErrorMessage(CloseableHttpResponse response, Prop prop, String serviceName, String methodName) {
        String errMsg = handleErrorMessage(response, prop);
        Adminlog.add(Adminlog.TYPE_AI, serviceName + "." + methodName + " FAILED : " + errMsg, -1, -1);
        throw new IllegalStateException(errMsg);
    }
}
