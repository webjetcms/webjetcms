package sk.iway.iwcm.components.ai.providers.openai;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.providers.SupportLogic;
import sk.iway.iwcm.i18n.Prop;

/**
 * Support service for OpenAI integration - common methods
 */
public abstract class OpenAiSupportService extends SupportLogic {

    protected static final String MODELS_URL = "https://api.openai.com/v1/models";
    protected static final String IMAGES_GENERATION_URL = "https://api.openai.com/v1/images/generations";
    protected static final String IMAGES_EDITS_URL = "https://api.openai.com/v1/images/edits";
    protected static final String RESPONSES_URL = "https://api.openai.com/v1/responses";

    protected static final String OUTPUT = "output";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    protected enum ASSISTANT_FIELDS {
        MODEL("model"),
        INPUT("input"),
        STORE("store"),
        STREAM("stream");

        private final String value;

        ASSISTANT_FIELDS(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    protected final void addHeaders(org.apache.http.client.methods.HttpRequestBase request, boolean addContentType) {
        String apiKey = getApiKey();
        if(Tools.isEmpty(apiKey)) throw new IllegalStateException("OpenAI API key is not set.");
        request.setHeader("Authorization", "Bearer " + apiKey);
        if(addContentType) request.setHeader("Content-Type", "application/json; charset=utf-8");
    }

    /**
     * Generates HTML select element for image size options based on the model.
     * @param model
     * @param prop
     * @return
     */
    protected String getImageSizeSelect(String model, Prop prop) {
        if(Tools.isEmpty(model)) return "";
        String square = prop.getText("components.gallery.tui-image-editor.Square");

        String options = "";
        if("gpt-image-1".equals(model)) {
            options =
                """
                    <option value="auto">auto</option>
                    <option value="1024x1024">%s (1024x1024)</option>
                    <option value="1024x1536">%s (1024x1536)</option>
                    <option value="1536x1024">%s (1536x1024)</option>
                """.formatted(
                    square,
                    prop.getText("components.datatables-data-export.Na_vysku"),
                    prop.getText("components.datatables-data-export.Na_sirku")
                );
        } else if("dall-e-3".equals(model)) {
            options =
                """
                    <option value="1024x1024">%s (1024x1024)</option>
                    <option value="1024x1792">%s (1024x1792)</option>
                    <option value="1792x1024">%s (1792x1024)</option>
                """.formatted(
                    square,
                    prop.getText("components.datatables-data-export.Na_vysku"),
                    prop.getText("components.datatables-data-export.Na_sirku")
                );
        } else if("dall-e-2".equals(model)) {
            options =
                """
                    <option value="256x256">%s (256x256)</option>
                    <option value="512x512">%s (512x512)</option>
                    <option value="1024x1024">%s (1024x1024)</option>
                """.formatted(
                    square,
                    square,
                    square
                );
        } else {
            return "";
        }

        return
            """
                <div class='col-sm-4'>
                    <label for='bonusContent-imageSize'>%s</label>
                    <select id='bonusContent-imageSize' class='form-control'>
                        %s
                    </select>
                </div>
            """.formatted(prop.getText("components.ai_assistants.imageSize"), options);
    }

    protected String getImageQualitySelect(String model, Prop prop) {
        if(Tools.isEmpty(model)) return "";

        String options = "";
        if("gpt-image-1".equals(model)) {
            options =
                """
                    <option value="low">low</option>
                    <option value="medium" selected="selected">medium</option>
                    <option value="high">high</option>
                """;
        } else if("dall-e-3".equals(model)) {
            options =
                """
                    <option value="standard">standard</option>
                    <option value="hd">hd</option>
                """;
        } else {
            //For exmaple, dall-e-2 do not support quality param
            return "";
        }

        return
            """
                <div class='col-sm-4'>
                    <label for='bonusContent-imageQuality'>%s</label>
                    <select id='bonusContent-imageQuality' class='form-control'>
                        %s
                    </select>
                </div>
            """.formatted(prop.getText("components.ai_assistants.imageQuality"), options);
    }

    public static String getApiKey() {
        return Constants.getString("ai_openAiAuthKey");
    }

    protected ObjectNode getBaseMainObject(String systemInput, String... userInputs) throws IOException {
        return getBaseMainObjectWithImage(systemInput, null, userInputs);
    }

    protected ObjectNode getBaseMainObjectWithImage(String systemInput, InputDataDTO inputData, String... userInputs) throws IOException {
        ObjectNode mainObject = MAPPER.createObjectNode();
        ArrayNode inputsArray = MAPPER.createArrayNode();

        //System instructions
        addInput(inputsArray, systemInput, true);

        //USer input's
        for(String userInput : userInputs)
            if (Tools.isNotEmpty(userInput)) addInput(inputsArray, userInput, false);

        //Image input
        if(inputData != null && InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType()))
            addImageInput(inputsArray, inputData);


        mainObject.set("input", inputsArray);

        return mainObject;
    }

    protected void addInput(ArrayNode inputsArray, String value, boolean isSystem) {
        ObjectNode input = MAPPER.createObjectNode()
            .put("role", isSystem ? "system" : "user")
            .put("content", value);

        inputsArray.add(input);
    }

    protected void addImageInput(ArrayNode inputsArray, InputDataDTO inputData) throws IOException {
        ObjectNode input = MAPPER.createObjectNode()
            .put("role", "user");

        ObjectNode content = MAPPER.createObjectNode()
            .put("type", "input_image")
            .put("image_url", "data:" + inputData.getMimeType() + ";base64," + inputData.getFileAsBase64());

        // Wrap content in an array - NEEDED from Jackson ≥2.17
        ArrayNode contentArray = MAPPER.createArrayNode().add(content);

        input.set("content", contentArray);
        inputsArray.add(input);
    }
}
