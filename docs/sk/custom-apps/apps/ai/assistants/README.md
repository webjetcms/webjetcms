# Pridanie poskytovateľa

Komunikácia s externou AI službou patrí do samostatnej knižnice [webjet-ai](https://github.com/webjetcms/webjet-ai). Knižnica je nezávislá od frameworku a nesmie importovať triedy z `sk.iway.iwcm` ani čítať WebJET `Constants`.

Integrácia serverového poskytovateľa má tri časti:

- implementáciu `AiProvider` v `webjet-ai`, ktorá zabezpečuje komunikáciu s poskytovateľom, spracovanie odpovedí a streamovanie
- tenkú službu WebJET CMS rozširujúcu [LibrarySupportLogic](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/LibrarySupportLogic.java), ktorá prepája knižnicu s CMS
- voliteľnú implementáciu [AiAssitantsInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiAssitantsInterface.java) pre polia poskytovateľa v editore asistenta

Poskytovateľa zaregistrujte v [AiLibraryConfiguration](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiLibraryConfiguration.java) a konfiguráciu CMS mapujte v [WebjetAiConfigurationService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/WebjetAiConfigurationService.java). Spracovanie požiadavky a domény, konfigurácia, auditovanie, štatistiky, perzistencia, makrá promptov a dočasné súbory zostávajú v správe WebJET CMS.

Predchádzajúce transportné SPI systému CMS bolo odstránené. Existujúcich vlastných serverových poskytovateľov je potrebné migrovať na rozhranie `AiProvider` z knižnice a CMS adaptér `LibrarySupportLogic`.

## Implementácia `AiProvider`

Implementujte `com.webjetcms.ai.AiProvider` v samostatnej knižnici alebo v inej knižnici nezávislej od frameworku, ktorá od nej závisí. Stabilná hodnota vrátená metódou `id()` identifikuje poskytovateľa v knižnici aj v adaptéroch CMS.

```java
public final class AcmeProvider implements AiProvider {

    @Override
    public String id() {
        return "acme";
    }

    @Override
    public List<ModelInfo> listModels(AiProviderConfig config) throws AiProviderException {
        // Load and map the provider model catalogue.
        throw new UnsupportedOperationException("Implement provider call");
    }

    @Override
    public AiResponse execute(AiRequest request, AiProviderConfig config) throws AiProviderException {
        // Execute a provider-neutral text or image request.
        throw new UnsupportedOperationException("Implement provider call");
    }

    @Override
    public AiResponse stream(
        AiRequest request,
        AiProviderConfig config,
        AiStreamListener listener
    ) throws AiProviderException {
        // Decode the provider stream and send text fragments to the listener.
        throw new UnsupportedOperationException("Implement provider call");
    }
}
```

API kľúče, zmeny koncového bodu, časové limity a dôveryhodné hlavičky vstupujú do poskytovateľa iba cez nemenný objekt `AiProviderConfig`. Knižnica nesmie priamo pristupovať k servletovým požiadavkám, Spring službám, databáze ani ku konfigurácii CMS. Úplné implementácie nájdete medzi poskytovateľmi v [repozitári webjet-ai](https://github.com/webjetcms/webjet-ai).

## Registrácia poskytovateľa v CMS

Pridajte inštanciu poskytovateľa do objektu `AiClient`, ktorý spravuje CMS:

```java
@Configuration
public class AiLibraryConfiguration {

    @Bean(destroyMethod = "close")
    public AiClient webjetAiClient() {
        return AiClient.of(
            new OpenAiProvider(),
            new GeminiProvider(),
            new OpenRouterProvider(),
            new AcmeProvider()
        );
    }
}
```

Vytvorte tenký adaptér rozširujúci `LibrarySupportLogic`. Základná trieda mapuje požiadavky CMS na požiadavky knižnice a ponecháva auditovanie, štatistiky, spracovanie promptov a dočasných súborov v CMS:

```java
@Service
public class AcmeService extends LibrarySupportLogic {

    public AcmeService(
        AiClient aiClient,
        WebjetAiConfigurationService configurationService
    ) {
        super(aiClient, configurationService);
    }

    @Override
    public String getProviderId() {
        return "acme";
    }

    @Override
    public String getTitleKey() {
        return "components.ai_assistants.provider.acme.title";
    }

    @Override
    public String getBonusHtml(AssistantDefinitionEntity assistant, Prop prop) {
        return "";
    }
}
```

Identifikátor poskytovateľa sa musí zhodovať s hodnotou `AiProvider.id()`. Kľúč titulku pridajte do prekladových súborov CMS.

## Mapovanie konfigurácie WebJET

Konfiguračný kľúč poskytovateľa pridajte do [WebjetAiConfigKeys](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/WebjetAiConfigKeys.java) a namapujte ho v `WebjetAiConfigurationService`:

```java
private String apiKey(String providerId) {
    return switch (providerId) {
        case "openai" -> Constants.getString(WebjetAiConfigKeys.OPENAI_API_KEY);
        case "gemini" -> Constants.getString(WebjetAiConfigKeys.GEMINI_API_KEY);
        case "openrouter" -> Constants.getString(WebjetAiConfigKeys.OPENROUTER_API_KEY);
        case "acme" -> Constants.getString(WebjetAiConfigKeys.ACME_API_KEY);
        default -> "";
    };
}
```

`WebjetAiConfigurationService.resolve(providerId, request)` vytvorí objekt `AiProviderConfig` pre aktuálnu požiadavku. Metódu rozšírte, ak poskytovateľ potrebuje koncový bod spravovaný v CMS alebo dôveryhodnú hlavičku s metadátami. Hlavičky zadané používateľom nikdy neodovzdávajte priamo a prihlasovacie údaje nezapisujte do logu.

## Implementácia `AiAssitantsInterface`

Tento CMS adaptér vytvorte, ak poskytovateľ potrebuje predvolené hodnoty alebo vlastné polia v editore asistenta. Stav konfigurácie poskytovateľa zisťujte cez `WebjetAiConfigurationService`:

```java
@Service
public class AcmeAssistantsService implements AiAssitantsInterface {

    private final WebjetAiConfigurationService configurationService;

    public AcmeAssistantsService(WebjetAiConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public String getProviderId() {
        return "acme";
    }

    @Override
    public boolean isInit() {
        return configurationService.isConfigured(getProviderId());
    }

    @Override
    public void prepareBeforeSave(AssistantDefinitionEntity assistant) {
        if (Tools.isEmpty(assistant.getModel())) {
            assistant.setModel("acme-default-model");
        }
    }

    @Override
    public void setProviderSpecificOptions(
        DatatablePageImpl<AssistantDefinitionEntity> page,
        Prop prop
    ) {
        // Add provider-specific editor options when needed.
    }

    @Override
    public List<String> getFieldsToShow(String action) {
        return List.of("model", "useStreaming", "useTemporal");
    }
}
```

## Výnimka `AiInterface` iba pre prehliadač

[BrowserService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/browser/BrowserService.java) implementuje [AiInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiInterface.java) priamo, pretože Chrome Built-in AI beží v prehliadači a nepoužíva serverovú komunikáciu s poskytovateľom. Toto je jediný spôsob priamej implementácie. Noví serveroví poskytovatelia musia používať `AiProvider` a `LibrarySupportLogic`.

## Lokálny vývoj

Kým nebude `com.webjetcms:webjet-ai` dostupná z Maven Central, spúšťajte Gradle úlohy CMS z repozitára CMS s explicitne pripojenou susednou knižnicou:

```shell
./gradlew --include-build ../webjet-ai compileJava test
```

Rovnakú voľbu `--include-build ../webjet-ai` použite pri každom lokálnom zostavení a overovacej úlohe CMS. Do `settings.gradle` nepridávajte trvalý záznam `includeBuild`, nepoužívajte `mavenLocal()` a nekopírujte JAR knižnice do CMS. Bežné zostavenie CMS bude až do zverejnenia verzie `0.1.0` podľa očakávania neúspešné.

## `AiAssistantsService`

`AiAssistantsService` spracováva požiadavky súvisiace s dátovou tabuľkou [Asistenti](../../../../redactor/ai/settings/README.md). Pomocou dependency injection získava všetky implementácie `AiAssitantsInterface` a vyberá ich podľa identifikátora poskytovateľa.

Dôležité metódy:

- `getAssistantAndFieldFrom` – vráti asistentov, ktorí spĺňajú podmienky zobrazenia vo vybranom poli
- `getClassOptions` – vráti triedy, na ktoré môže byť asistent naviazaný
- `getFieldOptions` – vráti polia vybranej triedy
- `prepareBeforeSave` – pred uložením vyvolá zmeny špecifické pre poskytovateľa
- `getProviderSpecificOptions` – pridá do `DatatablePageImpl` možnosti špecifické pre poskytovateľa
- `getProviderFields` – vráti dodatočné polia zobrazené v editore tabuľky
- `getAssistantStatus` – oznámi, či je poskytovateľ nakonfigurovaný

## `AiService`

`AiService` spracováva požiadavky asistentov a pomocou dependency injection získava všetky CMS adaptéry `AiInterface`.

Dôležité metódy:

- `getProviders` – vráti nakonfigurovaných poskytovateľov
- `getModelOptions` – vráti modely poskytovateľa, voliteľne filtrované podľa reťazca
- `getAiResponse` – vráti úplnú textovú odpoveď
- `getAiImageResponse` – vráti obrázkovú odpoveď
- `getAiStreamResponse` – streamuje textovú odpoveď cez `BufferedWriter`
- `getBonusHtml` – vráti doplnkové HTML okna asistenta pre poskytovateľa
