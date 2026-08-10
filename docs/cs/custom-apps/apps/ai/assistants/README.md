# Přidání poskytovatele

Komunikace s externí AI službou patří do samostatné knihovny [webjet-ai](https://github.com/webjetcms/webjet-ai). Knihovna je nezávislá na frameworku a nesmí importovat třídy z `sk.iway.iwcm` ani číst WebJET `Constants`.

Integrace serverového poskytovatele má tři části:

- implementaci `AiProvider` ve `webjet-ai`, která zajišťuje komunikaci s poskytovatelem, zpracování odpovědí a streamování
- tenkou službu WebJET CMS rozšiřující [LibrarySupportLogic](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/LibrarySupportLogic.java), která propojuje knihovnu s CMS
- volitelnou implementaci [AiAssitantsInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiAssitantsInterface.java) pro pole poskytovatele v editoru asistenta

Poskytovatele zaregistrujte v [AiLibraryConfiguration](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiLibraryConfiguration.java) a konfiguraci CMS mapujte ve [WebjetAiConfigurationService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/WebjetAiConfigurationService.java). Zpracování požadavku a domény, konfigurace, auditování, statistiky, perzistence, makra promptů a dočasné soubory zůstávají ve správě WebJET CMS.

Předchozí transportní SPI systému CMS bylo odstraněno. Existující vlastní serverové poskytovatele je nutné migrovat na rozhraní `AiProvider` z knihovny a CMS adaptér `LibrarySupportLogic`.

## Implementace `AiProvider`

Implementujte `com.webjetcms.ai.AiProvider` v samostatné knihovně nebo v jiné knihovně nezávislé na frameworku, která na ní závisí. Stabilní hodnota vrácená metodou `id()` identifikuje poskytovatele v knihovně i v adaptérech CMS.

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

API klíče, změny koncového bodu, časové limity a důvěryhodné hlavičky vstupují do poskytovatele pouze přes neměnný objekt `AiProviderConfig`. Knihovna nesmí přímo přistupovat k servletovým požadavkům, Spring službám, databázi ani ke konfiguraci CMS. Úplné implementace najdete mezi poskytovateli v [repozitáři webjet-ai](https://github.com/webjetcms/webjet-ai).

## Registrace poskytovatele v CMS

Přidejte instanci poskytovatele do objektu `AiClient`, který spravuje CMS:

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

Vytvořte tenký adaptér rozšiřující `LibrarySupportLogic`. Základní třída mapuje požadavky CMS na požadavky knihovny a ponechává auditování, statistiky, zpracování promptů a dočasných souborů v CMS:

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

Identifikátor poskytovatele se musí shodovat s hodnotou `AiProvider.id()`. Klíč titulku přidejte do překladových souborů CMS.

## Mapování konfigurace WebJET

Konfigurační klíč poskytovatele přidejte do [WebjetAiConfigKeys](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/WebjetAiConfigKeys.java) a namapujte jej ve `WebjetAiConfigurationService`:

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

`WebjetAiConfigurationService.resolve(providerId, request)` vytvoří objekt `AiProviderConfig` pro aktuální požadavek. Metodu rozšiřte, pokud poskytovatel potřebuje koncový bod spravovaný v CMS nebo důvěryhodnou hlavičku s metadaty. Hlavičky zadané uživatelem nikdy nepředávejte přímo a přihlašovací údaje nezapisujte do logu.

## Implementace `AiAssitantsInterface`

Tento CMS adaptér vytvořte, pokud poskytovatel potřebuje výchozí hodnoty nebo vlastní pole v editoru asistenta. Stav konfigurace poskytovatele zjišťujte přes `WebjetAiConfigurationService`:

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

## Výjimka `AiInterface` pouze pro prohlížeč

[BrowserService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/browser/BrowserService.java) implementuje [AiInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiInterface.java) přímo, protože Chrome Built-in AI běží v prohlížeči a nepoužívá serverovou komunikaci s poskytovatelem. Toto je jediný způsob přímé implementace. Noví serveroví poskytovatelé musí používat `AiProvider` a `LibrarySupportLogic`.

## Lokální vývoj

Dokud nebude `com.webjetcms:webjet-ai:0.1.0` dostupná z Maven Central, spouštějte Gradle úlohy CMS z repozitáře CMS s explicitně připojenou sousední knihovnou:

```shell
./gradlew --include-build ../webjet-ai compileJava test
```

Stejnou volbu `--include-build ../webjet-ai` použijte při každém lokálním sestavení a ověřovací úloze CMS. Do `settings.gradle` nepřidávejte trvalý záznam `includeBuild`, nepoužívejte `mavenLocal()` a nekopírujte JAR knihovny do CMS. Běžné sestavení CMS bude až do zveřejnění verze `0.1.0` podle očekávání neúspěšné.

## `AiAssistantsService`

`AiAssistantsService` zpracovává požadavky související s datovou tabulkou [Asistenti](../../../../redactor/ai/settings/README.md). Pomocí dependency injection získává všechny implementace `AiAssitantsInterface` a vybírá je podle identifikátoru poskytovatele.

Důležité metody:

- `getAssistantAndFieldFrom` – vrátí asistenty, kteří splňují podmínky zobrazení ve vybraném poli
- `getClassOptions` – vrátí třídy, na které může být asistent navázán
- `getFieldOptions` – vrátí pole vybrané třídy
- `prepareBeforeSave` – před uložením vyvolá změny specifické pro poskytovatele
- `getProviderSpecificOptions` – přidá do `DatatablePageImpl` možnosti specifické pro poskytovatele
- `getProviderFields` – vrátí dodatečná pole zobrazená v editoru tabulky
- `getAssistantStatus` – oznámí, zda je poskytovatel nakonfigurován

## `AiService`

`AiService` zpracovává požadavky asistentů a pomocí dependency injection získává všechny CMS adaptéry `AiInterface`.

Důležité metody:

- `getProviders` – vrátí nakonfigurované poskytovatele
- `getModelOptions` – vrátí modely poskytovatele, volitelně filtrované podle řetězce
- `getAiResponse` – vrátí úplnou textovou odpověď
- `getAiImageResponse` – vrátí obrázkovou odpověď
- `getAiStreamResponse` – streamuje textovou odpověď přes `BufferedWriter`
- `getBonusHtml` – vrátí doplňkové HTML okna asistenta pro poskytovatele
