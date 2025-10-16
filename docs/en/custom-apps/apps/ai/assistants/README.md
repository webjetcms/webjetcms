# Adding a provider

To add support for a new provider (`provider`), you must meet the following conditions:
- create 2 classes with annotation `@Service`, ideally in a package `sk.iway.iwcm.components.ai.providers.PROVIDER_NAME` where implementations already exist for `openai`, `gemini`, `openrouter` a `browser`
- one class must implement the interface [AiInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiInterface.java) and implement all mandatory methods
- the second class must implement the interface [AiAssitantsInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiAssitantsInterface.java) and implement all mandatory methods

Classes will take care of the rest [AiService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/rest/AiService.java) a [AiAssistantsService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/rest/AiAssistantsService.java) that dynamically process the request according to the provider identifier.

## Implementations `AiAssitantsInterface`

This implementation is simple - it is just a few methods used for extensibility.

Example of implementation:

```java
/**
 * Service for OpenAI assistants - handles provider specific options
 */
@Service
public class OpenAiAssistantsService implements AiAssitantsInterface {

    public String getProviderId() {
        return "openai";
    }

    public boolean isInit() {
        return Tools.isNotEmpty(OpenAiSupportService.getApiKey());
    }

    public void prepareBeforeSave(AssistantDefinitionEntity assistantEnity) {
        if(Tools.isEmpty(assistantEnity.getModel())) assistantEnity.setModel("gpt-3.5-turbo");
    }

    public void setProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) {
        //open AI specific options
        page.addOptions("imagesQuality", getQualityOptions(), "label", "value", false);
        page.addOptions("imagesSize", getSizeOptions(), "label", "value", false);
    }

    public List<String> getFieldsToShow(String action) {
        if("create".equals(action)) return List.of("model", "useStreaming", "useTemporal");
        else if("edit".equals(action)) return List.of("model", "useStreaming", "useTemporal");
        else return new ArrayList<>();
    }

    private List<LabelValue> getQualityOptions() {
        List<LabelValue> qualityOptions = new ArrayList<>();
        qualityOptions.add(new LabelValue("low", "low"));
        qualityOptions.add(new LabelValue("medium", "medium"));
        qualityOptions.add(new LabelValue("high", "high"));
        return qualityOptions;
    }

    private List<LabelValue> getSizeOptions() {
        List<LabelValue> sizeOptions = new ArrayList<>();
        sizeOptions.add(new LabelValue("auto", "auto"));
        sizeOptions.add(new LabelValue("1024x1024", "1024x1024"));
        sizeOptions.add(new LabelValue("1024x1536", "1024x1536"));
        sizeOptions.add(new LabelValue("1536x1024", "1536x1024"));
        return sizeOptions;
    }
}
```

## Implementations `AiInterface`

This implementation is more complex because it handles all communication with the provider. You have 2 options.

### Option one

Your class implements an interface `AiInterface` and at the same time extends the abstract class [SupportLogic](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/SupportLogic.java). This class already contains implementations of mandatory methods `AiInterface` and most of the necessary logic. Whereas `SupportLogic` implemented by [SupportLogicInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/SupportLogicInterface.java), you only implement methods from this interface.

Advantage: you avoid a lot of redundant logic common to all providers. Many implementation problems and bugs are already solved. This is how providers work `openai`, `gemini` a `openrouter`. If any part is different, you can overload the necessary method.

In the following example you do not see directly the extension of the `SupportLogic` because the class `OpenAiSupportService` is already expanding it.

Example implementation :

```java
/**
 * Service for OpenAI assistants - handles calls to OpenAI API
 * We do not use any official SDK, but rather direct REST calls, so its easy to maintain and we can see what is going on.
 * docs: https://platform.openai.com/docs/api-reference
 */
@Service
public class OpenAiService extends OpenAiSupportService implements AiInterface {

    private static final String PROVIDER_ID = "openai";
    private static final String TITLE_KEY = "components.ai_assistants.provider.openai.title";

    private static final ObjectMapper mapper = new ObjectMapper();

    public String getProviderId() {
        return PROVIDER_ID;
    }

    public String getServiceName() {
        return "OpenAiService";
    }

    public String getTitleKey() {
        return TITLE_KEY;
    }

    public boolean isInit() {
        return Tools.isNotEmpty(getApiKey());
    }

    public int addUsageAndReturnTotal(StringBuilder sb, int addTokens, JsonNode root) {
        int totalTokens = addTokens;
        if(root.has("usage")) {
            JsonNode usage = root.path("usage");
            int inputTokens = usage.path("input_tokens").asInt(0);
            int outputTokens = usage.path("output_tokens").asInt(0);
            totalTokens = usage.path("total_tokens").asInt(0) + addTokens;

            sb.append("\t input_tokens: ").append(inputTokens).append("\n");
            sb.append("\t output_tokens: ").append(outputTokens).append("\n");
            sb.append("\t total_tokens: ").append(totalTokens).append("\n");
        }
        return totalTokens;
    }

    public HttpRequestBase getModelsRequest (HttpServletRequest request) {
        HttpGet get = new HttpGet(MODELS_URL);
        addHeaders(get, true);
        return get;
    }

    public List<LabelValue> extractModels(JsonNode root) {
        List<LabelValue> supportedValues = new ArrayList<>();
        for (JsonNode model : root.get("data")) {
            String modelId = model.get("id").asText();
            String created = model.get("created").asText();
            supportedValues.add(new LabelValue(modelId, created));
        }
        supportedValues.sort(Comparator.comparingLong((LabelValue o) -> Long.parseLong(o.getValue())).reversed());
        for (LabelValue modelValue : supportedValues) modelValue.setValue(modelValue.getLabel());
        return supportedValues;
    }

    public HttpRequestBase getResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request) {
        ObjectNode mainObject = getBaseMainObject(instructions, inputData.getInputValue(), inputData.getUserPrompt());
        mainObject.put(MODEL.value(), assistant.getModel());
        mainObject.put(STORE.value(), !assistant.getUseTemporal());

        HttpPost post = new HttpPost(RESPONSES_URL);
        post.setEntity(getRequestBody(mainObject.toString()));
        addHeaders(post, true);

        return post;
    }

    public String extractResponseText(JsonNode jsonNodeRes) {
        ArrayNode data = (ArrayNode) jsonNodeRes.path(OUTPUT);
        JsonNode firstMessage = data.get(0);
        ArrayNode contentArray = (ArrayNode) firstMessage.path("content");
        return  contentArray.get(0).path("text").asText();
    }

    public HttpRequestBase getStremResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request) {
        ObjectNode mainObject = getBaseMainObject(instructions, inputData.getInputValue(), inputData.getUserPrompt());
        mainObject.put(MODEL.value(), assistant.getModel());
        mainObject.put(STORE.value(), !assistant.getUseTemporal());
        mainObject.put(STREAM.value(), assistant.getUseStreaming());

        HttpPost post = new HttpPost(RESPONSES_URL);
        post.setEntity(getRequestBody(mainObject.toString()));
        addHeaders(post, true);
        post.setHeader("Accept", "text/event-stream");

        return post;
    }

    public JsonNode handleBufferedReader(BufferedReader reader,  BufferedWriter writer, Map<Integer, String> replacedIncludes) throws IOException {
        OpenAiStreamHandler streamHandler = new OpenAiStreamHandler(replacedIncludes);
        streamHandler.handleBufferedReader(reader, writer);
        return streamHandler.getUsageChunk();
    }


    public HttpRequestBase getImageResponseRequest(String instructions, InputDataDTO inputData, AssistantDefinitionEntity assistant, HttpServletRequest request, Prop prop) throws IOException {
        if(inputData.getInputValueType().equals(InputDataDTO.InputValueType.IMAGE)) {
            //ITS IMAGE EDIT - I GOT IMAGE to edit AND I WILL RETURN IMAGE
            return getEditImagePost(inputData, assistant.getModel(), instructions, prop);
        } else {
            //ITS IMAGE GENERATION - INPUT IS TEXT RETUN IMAGE
            return getCreateImagePost(inputData, assistant.getModel(), instructions);
        }
    }

    public String getFinishError(JsonNode jsonNodeRes) {
        // OpenAI in new reponse API use STATUS instead of finish_reason

        // Need to be in try catch because IMAGES for example do not return status :)
        String status = null;
        try { status= jsonNodeRes.path(OUTPUT).get(0).path("status").asText("");
        } catch(Exception e) { return null; }

        if("completed".equalsIgnoreCase(status)) {
            //All good
            return null;
        } else if("incomplete".equalsIgnoreCase(status)) {
            // Problem, try extract reason
            return jsonNodeRes.path(OUTPUT).get(0).path("incomplete_details").path("reason").asText(status);
        } else {
            // UNKNOWN
            return status;
        }
    }

    public ArrayNode getImages(JsonNode jsonNodeRes) {
        JsonNode imagesArr = jsonNodeRes.path("data");
        return imagesArr.isArray() ? (ArrayNode) imagesArr : mapper.createArrayNode();
    }

    public String getImageFormat(JsonNode jsonNodeRes, JsonNode jsonImage) {
        //OpeAI returns global fomat, not local
        return "." + jsonNodeRes.path("output_format").asText( "png");
    }

    public String getImageBase64(JsonNode jsonNodeRes, JsonNode jsonImage) {
        return jsonImage.path("b64_json").asText(null);
    }

    public String getModelForImageNameGeneration() {
        return Constants.getString("ai_openAi_generateFileNameModel");
    }

    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop) {
        if("edit_image".equals(assistant.getAction()) || "generate_image".equals(assistant.getAction())) {
            String model = assistant.getModel();

            return """
                <div class='bonus-content row mt-3'>
                    <div class='col-sm-4'>
                        <label for='bonusContent-imageCount'>%s</label>
                        <input id='bonusContent-imageCount' type='number' class='form-control' value=1>
                    </div>
                    %s
                    %s
                </div>
            """.formatted(
                prop.getText("components.ai_assistants.imageCount"),
                getImageSizeSelect(model, prop),
                getImageQualitySelect(model, prop)
            );
        }

        return "";
    }

    private HttpPost getEditImagePost(InputDataDTO inputData, String model, String instructions, Prop prop) throws IOException {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        builder.addTextBody(MODEL.value(), model);
        builder.addTextBody("prompt", instructions);
        builder.addTextBody("n", inputData.getImageCount() == null ? "1" : inputData.getImageCount().toString());

        if(inputData.getImageQuality() != null) {
            builder.addTextBody("quality", inputData.getImageQuality());
        }

        //1024x1024 is valid for gpt-image dalle-3 and dalle-2 ... soo best default value
        builder.addTextBody("size", inputData.getImageSize() == null ? "1024x1024" : inputData.getImageSize());

        BufferedImage image = ImageIO.read( inputData.getInputFile() );
        if (image == null) throw new IllegalStateException("Image not founded or not a Image.");

        //Set image
        if("dall-e-2".equals(model)) {
           throw new IllegalStateException( prop.getText("components.ai_assistants.not_supproted_action_err") );
        } else {
            builder.addBinaryBody("image", inputData.getInputFile(), inputData.getContentType(), inputData.getInputFile().getName());
        }

        //Set entity and headers
        HttpPost post = new HttpPost(IMAGES_EDITS_URL);
        post.setEntity(builder.build());
        addHeaders(post, false);

        return post;
    }

    private HttpPost getCreateImagePost(InputDataDTO inputData, String model, String instructions) {
        ObjectNode json = mapper.createObjectNode();
        json.put(MODEL.value(), model);
        json.put("prompt", instructions);
        json.put("n", inputData.getImageCount() == null ? 1 : inputData.getImageCount());

        if(inputData.getImageQuality() != null) {
            json.put("quality", inputData.getImageQuality());
        }

        //1024x1024 is valid for gpt-image dalle-3 and dalle-2 ... soo best default value
        json.put("size", inputData.getImageSize() == null ? "1024x1024" : inputData.getImageSize());

        if("dall-e-2".equals(model) || "dall-e-3".equals(model)) {
            json.put("response_format", "b64_json");
        }

        HttpPost post = new HttpPost(IMAGES_GENERATION_URL);
        post.setEntity(getRequestBody(json.toString()));
        addHeaders(post, true);

        return post;
    }
}
```

### The possibility of a second

Your class implements an interface `AiInterface` and all its mandatory methods. It is more time consuming - use this path only if the implementation is significantly different from other providers.

Example of implementation:

```java
/**
 * Service for Chrome Built-in AI:
 * https://developer.chrome.com/docs/ai/built-in
 */
@Service
public class BrowserService implements AiInterface {

    protected static final String PROVIDER_ID = "browser";
    private static final String TITLE_KEY = "components.ai_assistants.provider.browser.title";

    public String getProviderId() {
        return PROVIDER_ID;
    }

    public boolean isInit() {
        return Constants.getBoolean("ai_browserAiEnabled");
    }

    public Pair<String, String> getProviderInfo(Prop prop) {
        return new Pair<>(PROVIDER_ID, prop.getText(TITLE_KEY));
    }

    public AssistantResponseDTO getAiAssistantResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws Exception {
        return null;
    }

    public AssistantResponseDTO getAiResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) {
        return null;
    }

    public AssistantResponseDTO getAiImageResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, HttpServletRequest request) throws Exception {
        return null;
    }

    public List<LabelValue> getSupportedModels(Prop prop, HttpServletRequest request) {
        ArrayList<LabelValue> models = new ArrayList<>();
        models.add(new LabelValue("Gemini Nano", "v3Nano"));
        return models;
    }

    public AssistantResponseDTO getAiStreamResponse(AssistantDefinitionEntity assistant, InputDataDTO inputData, Prop prop, AiStatRepository statRepo, PrintWriter writer, HttpServletRequest request) throws Exception {
        return null;
    }

    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop) {
        return null;
    }
}
```

## `AiAssistantsService`

`AiAssistantsService` takes care of the requirements related to the data table [Assistants](../../../../redactor/ai/settings/README.md). It provides support methods for selection fields and uses automatic `dependency injection` to get all interface implementations `AiAssitantsInterface`. It iterates over the list and performs an action on the correct implementation according to the identifier when the provider is queried.

Important methods:
- `getAssistantAndFieldFrom` - returns assistants that meet the display conditions for the selected field
- `getClassOptions` - returns a list of classes to which the assistant can be bound (filtering by the string in the full name)
- `getFieldOptions` - returns a list of fields of the selected class (name filtering by the search string)
- `prepareBeforeSave` - finds the assistant provider and invokes the method of the same name over it to make adjustments before saving
- `getProviderSpecificOptions` - will be added to the `DatatablePageImpl` provider-specific options
- `getProviderFields` - returns additional fields for display in the table editor
- `getAssistantStatus` - returns the status of the provider (whether it is configured)

## `AiService`

`AiService` handles AI requests from assistants. Provides methods to get answers from providers selected by the assistant. Uses `dependency injection` to obtain all implementations `AiInterface`.

Important methods:
- `getProviders` - returns a list of configured providers
- `getModelOptions` - returns a list of models from the given provider; there is a version with filtering by string
- `getAiResponse` - returns the text response as a whole
- `getAiImageResponse` - returns a picture answer
- `getAiStreamResponse` - returns a text response streamed via `PrintWriter`
- `getBonusHtml` - by provider identifier returns HTML additional content of the assistant window
