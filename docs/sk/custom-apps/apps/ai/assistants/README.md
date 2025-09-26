# Pridanie poskytovateľa

Aby ste pridali podporu pre nového poskytovateľa (`provider`), musíte splniť nasledujúce podmienky:

- vytvoriť 2 triedy s anotáciou `@Service`, najlepšie v lokalite `sk.iway.iwcm.components.ai.providers.PROVIDER_NAME`, kde sú už implementácie pre `openai`, `gemini` a `browser`
- jedna trieda by mala implementovať rozhranie [AiInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiInterface.java) a teda implementovať všetky povinné metódy
- druhá trieda by mala implementovať rozhranie [AiAssitantsInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiAssitantsInterface.java) a teda implementovať všetky povinné metódy

O všetko ostatné sa už postarajú triedy [AiService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/rest/AiService.java) a [AiAssistantsService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/rest/AiAssistantsService.java), ktoré dynamicky podľa identifikátora poskytovateľa spracujú požiadavku.

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

/**
 * Service for Chrome Built-in AI:
 * https://developer.chrome.com/docs/ai/built-in
 */
@Service
public class BrowserAssistantsService implements AiAssitantsInterface {

    public String getProviderId() {
        return BrowserService.PROVIDER_ID;
    }

    public boolean isInit() {
        return Constants.getBoolean("ai_browserAiEnabled");
    }

    public List<String> getFieldsToShow(String action) {
        return List.of( "useStreaming");
    }

    public void prepareBeforeSave(AssistantDefinitionEntity assistantEnity) {
        //no checks  needed for now
    }

    public void setProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) {
        //Local provider do not have specific fields that need to set options
    }
}
```

## `AiAssistantsService`

`AiAssistantsService` sa primárne stará o potreby spojené s dátovou tabuľkou [Assistenti](../../../../redactor/ai/settings/README.md). Trieda poskytuje podporné metódy pre rôzne výberové polia a pod. Využíva princíp automatického `dependency injection`, aby získala zoznam všetkých tried, ktoré implementujú rozhranie `AiAssitantsInterface`. Pri požiadavke na dáta, ktoré sú špecifické pre konkrétneho poskytovateľa, prejde zoznam a vyvolá potrebnú akciu nad správnou implementáciou podľa identifikátora poskytovateľa.

Dôležité metódy, ktoré trieda poskytuje:

- `getAssistantAndFieldFrom` – vráti asistentov, ktorí spĺňajú podmienky zobrazenia na zvolenom poli
- `getClassOptions` – vráti zoznam všetkých tried, na ktoré môže byť asistent naviazaný (filter podľa reťazca v plnom názve triedy)
- `getFieldOptions` – vráti zoznam všetkých polí zvolenej triedy (z predchádzajúceho zoznamu), ktorých názov obsahuje hľadaný reťazec
- `prepareBeforeSave` – nájde poskytovateľa pre daného asistenta a vyvolá na ňom rovnako pomenovanú povinnú metódu na špecifické úpravy pred uložením
- `getProviderSpecificOptions` – nájde poskytovateľa a vyvolá metódu na doplnenie špecifických možností do `DatatablePageImpl`
- `getProviderFields` – nájde poskytovateľa a vyvolá metódu, ktorá vráti zoznam dodatočných polí na zobrazenie pre editor tabuľky
- `getAssistantStatus` – podľa identifikátora poskytovateľa vráti jeho status (či je nakonfigurovaný alebo nie)

## `AiService`

`AiService` sa primárne stará o potreby spojené so spracovaním AI žiadostí od vyvolaných asistentov. Poskytuje metódy na získanie odpovede od poskytovateľa zvoleného asistentom. Využíva princíp automatického `dependency injection`, aby získala zoznam všetkých tried, ktoré implementujú rozhranie `AiInterface`.

Dôležité metódy, ktoré trieda poskytuje:

- `getProviders` – vráti zoznam všetkých nakonfigurovaných poskytovateľov
- `getModelOptions` – vráti zoznam všetkých modelov daného poskytovateľa; existuje aj verzia tejto metódy, ktorá tieto modely filtruje na základe požadovaného reťazca v názve
- `getAiResponse` – vráti TEXTOVÚ odpoveď od daného poskytovateľa ako celok
- `getAiImageResponse` – vráti OBRÁZKOVÚ odpoveď od daného poskytovateľa
- `getAiStreamResponse` – vráti TEXTOVÚ odpoveď od daného poskytovateľa, pričom odpoveď je streamovaná cez zadaný `PrintWriter`
- `getBonusHtml` – podľa identifikátora poskytovateľa vráti HTML kód, ktorý sa použije ako bonusový obsah v okne spusteného asistenta