package sk.iway.iwcm.kokos;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;

public abstract class OpenAiSupportService {
    protected static final String THREADS_URL = "https://api.openai.com/v1/threads/";
    protected static final String ASSISTANTS_URL = "https://api.openai.com/v1/assistants";
    protected static final String MODELS_URL = "https://api.openai.com/v1/models";
    public static final String EMPTY_VALUE = "EMPTY_VALUE";

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

    protected final void addHeaders(Request.Builder builder, boolean addContentType, boolean isAssistantV2) {
        String apiKey = Constants.getString("open_ai_auth_key");

        if(Tools.isEmpty(apiKey)) throw new IllegalStateException("OpenAI API key is not set.");

        builder.addHeader("Authorization", "Bearer " + apiKey);
        if(addContentType) builder.addHeader("Content-Type", "application/json");
        if(isAssistantV2) builder.addHeader("OpenAI-Beta", "assistants=v2");
    }

    protected final RequestBody getRequestBody(String stringBody) {
        if(Tools.isEmpty(stringBody) == true) stringBody = "{}";
        return RequestBody.create(stringBody, MediaType.parse("application/json"));
    }

    protected String handleErrorMessage (Response response, Prop prop) {
        int code = response.code();
        String defaultErrMsg = " (" + code + ") " + prop.getText("html_area.insert_image.error_occured");

        try {
            String responseBody = response.body().string();
            JSONObject jsonObject = new JSONObject(responseBody);

            if (jsonObject.has("error")) {
                JSONObject error = jsonObject.getJSONObject("error");

                if (error.has("message")) {
                    String errorMessage = error.getString("message");

                    //Get err msg return it
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

    protected String handleErrorMessage (Response response, Prop prop, String serviceName, String methodName) {
        String errMsg = handleErrorMessage(response, prop);
        Adminlog.add(Adminlog.TYPE_AI, serviceName + "." + methodName + " FAILED : " + errMsg, -1, -1);
        throw new IllegalStateException( errMsg );
    }
}
