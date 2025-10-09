# Pridanie poskytovateľa

Aby ste pridali podporu pre nového poskytovateľa (`provider`), musíte splniť nasledujúce podmienky:

- vytvoriť 2 triedy s anotáciou `@Service`, ideálne v balíku `sk.iway.iwcm.components.ai.providers.PROVIDER_NAME`, kde už existujú implementácie pre `openai`, `gemini`, `openrouter` a `browser`
- jedna trieda musí implementovať rozhranie [AiInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiInterface.java) a implementovať všetky povinné metódy
- druhá trieda musí implementovať rozhranie [AiAssitantsInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiAssitantsInterface.java) a implementovať všetky povinné metódy

O ostatné sa postarajú triedy [AiService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/rest/AiService.java) a [AiAssistantsService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/rest/AiAssistantsService.java), ktoré dynamicky podľa identifikátora poskytovateľa spracujú požiadavku.

## Implementácie `AiAssitantsInterface`

Táto implementácia je jednoduchá – ide len o niekoľko metód slúžiacich na rozšíriteľnosť.

Príklad implementácie:

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

## Implementácie `AiInterface`

Táto implementácia je zložitejšia, pretože zabezpečuje celú komunikáciu s poskytovateľom. Máte 2 možnosti.

### Možnosť prvá

Vaša trieda implementuje rozhranie `AiInterface` a zároveň rozširuje abstraktnú triedu [SupportLogic](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/SupportLogic.java). Táto trieda už obsahuje implementácie povinných metód `AiInterface` a väčšinu potrebnej logiky. Keďže `SupportLogic` implementuje [SupportLogicInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/SupportLogicInterface.java), vy implementujete iba metódy z tohto rozhrania.

Výhoda: vyhnete sa množstvu redundantnej logiky spoločnej pre všetkých poskytovateľov. Mnohé implementačné problémy a chyby sú už vyriešené. Takto fungujú poskytovatelia `openai`, `gemini` a `openrouter`. Ak sa nejaká časť líši, môžete potrebnú metódu preťažiť.

V nasledujúcom príklade nevidíte priamo rozšírenie o `SupportLogic`, pretože trieda `OpenAiSupportService` ju už rozširuje.

Príklad implementácie :

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

### Možnosť druha

Vaša trieda implementuje rozhranie `AiInterface` a všetky jeho povinné metódy. Je to časovo náročnejšie – použite túto cestu iba v prípade, že implementácia sa výrazne odlišuje od ostatných poskytovateľov.

Príklad implementácie:

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

`AiAssistantsService` sa stará o požiadavky súvisiace s dátovou tabuľkou [Assistenti](../../../../redactor/ai/settings/README.md). Poskytuje podporné metódy pre výberové polia a používa automatický `dependency injection`, aby získala všetky implementácie rozhrania `AiAssitantsInterface`. Pri požiadavke na poskytovateľa iteruje zoznam a vykoná akciu nad správnou implementáciou podľa identifikátora.

Dôležité metódy:

- `getAssistantAndFieldFrom` – vráti asistentov, ktorí spĺňajú podmienky zobrazenia na zvolenom poli
- `getClassOptions` – vráti zoznam tried, na ktoré môže byť asistent naviazaný (filtrovanie podľa reťazca v plnom názve)
- `getFieldOptions` – vráti zoznam polí zvolenej triedy (filtrovanie názvu podľa hľadaného reťazca)
- `prepareBeforeSave` – nájde poskytovateľa asistenta a vyvolá nad ním rovnomennú metódu na úpravy pred uložením
- `getProviderSpecificOptions` – doplní do `DatatablePageImpl` špecifické možnosti poskytovateľa
- `getProviderFields` – vráti dodatočné polia na zobrazenie v editore tabuľky
- `getAssistantStatus` – vráti status poskytovateľa (či je nakonfigurovaný)

## `AiService`

`AiService` spracováva AI požiadavky asistentov. Poskytuje metódy na získanie odpovedí od poskytovateľov vybraných asistentom. Používa `dependency injection` na získanie všetkých implementácií `AiInterface`.

Dôležité metódy:

- `getProviders` – vráti zoznam nakonfigurovaných poskytovateľov
- `getModelOptions` – vráti zoznam modelov daného poskytovateľa; existuje verzia s filtrovaním podľa reťazca
- `getAiResponse` – vráti textovú odpoveď ako celok
- `getAiImageResponse` – vráti obrázkovú odpoveď
- `getAiStreamResponse` – vráti textovú odpoveď streamovanú cez `PrintWriter`
- `getBonusHtml` – podľa identifikátora poskytovateľa vráti HTML doplnkový obsah okna asistenta
