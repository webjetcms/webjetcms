# Adding a provider

Communication with the external AI service belongs to a separate library [webjet-ai](https://github.com/webjetcms/webjet-ai). The library is framework-independent and may not import classes from `sk.iway.iwcm` or read WebJET `Constants`.

Server provider integration has three parts:

- implementation of `AiProvider` in `webjet-ai`, which ensures communication with the provider, response processing and streaming
- a thin WebJET CMS service extending [LibrarySupportLogic](../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/LibrarySupportLogic.java), which connects the library with the CMS
- optional implementation of [AiAssitantsInterface](../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiAssitantsInterface.java) for provider fields in the assistant editor

Register the provider in [AiLibraryConfiguration](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiLibraryConfiguration.java) and map the CMS configuration in [WebjetAiConfigurationService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/WebjetAiConfigurationService.java). Request and domain processing, configuration, auditing, statistics, persistence, prompt macros, and temporary files remain managed by WebJET CMS.

The previous CMS transport SPI has been removed. Existing custom server providers need to be migrated to the `AiProvider` library interface and the `LibrarySupportLogic` CMS adapter.

## Implementation `AiProvider`

Implement `com.webjetcms.ai.AiProvider` in a standalone library or in another framework-independent library that depends on it. The stable value returned by the `id()` method identifies the provider in both the library and CMS adapters.

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

API keys, endpoint changes, timeouts, and trusted headers are only passed to the provider through the immutable `AiProviderConfig` object. The library must not directly access servlet requests, Spring services, the database, or CMS configuration. Full implementations can be found among the providers in the [webjet-ai repository](https://github.com/webjetcms/webjet-ai).

## Provider registration in CMS

Add a provider instance to the `AiClient` object managed by the CMS:

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

Create a thin adapter extending `LibrarySupportLogic`. The base class maps CMS requests to library requests and leaves auditing, statistics, prompt handling, and temporary files to the CMS:

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

The provider identifier must match the value `AiProvider.id()`. Add the subtitle key to the CMS translation files.

## WebJET configuration mapping

Add the provider configuration key to [WebjetAiConfigKeys](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/WebjetAiConfigKeys.java) and map it in `WebjetAiConfigurationService`:

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

`WebjetAiConfigurationService.resolve(providerId, request)` creates an object `AiProviderConfig` for the current request. Extend the method if the provider needs a CMS-managed endpoint or a trusted metadata header. Never pass user-supplied headers directly and do not log login credentials.

## Implementation `AiAssitantsInterface`

Create this CMS adapter if the provider needs default values ​​or custom fields in the assistant editor. Check the provider configuration status via `WebjetAiConfigurationService`:

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

## Exception `AiInterface` browser only

[BrowserService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/browser/BrowserService.java) implements [AiInterface](../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiInterface.java) directly because Chrome Built-in AI runs in the browser and does not use server-side communication with the provider. This is the only way to implement it directly. New server-side providers must use `AiProvider` and `LibrarySupportLogic`.

## Local development

Until `com.webjetcms:webjet-ai` is available from Maven Central, run CMS Gradle tasks from the CMS repository with the neighboring library explicitly linked:

```shell
./gradlew --include-build ../webjet-ai compileJava test
```

Use the same `--include-build ../webjet-ai` option for every local build and CMS verification task. Do not add a permanent entry `includeBuild` to `settings.gradle`, do not use `mavenLocal()`, and do not copy JAR libraries to CMS. Regular CMS builds will fail as expected until `0.1.0` is released.

## `AiAssistantsService`

`AiAssistantsService` handles requests related to the [Assistants](../../../../redactor/ai/settings/README.md) data table. Using dependency injection, it retrieves all `AiAssitantsInterface` implementations and selects them by provider identifier.

Important methods:

- `getAssistantAndFieldFrom` – returns assistants who meet the display conditions in the selected field
- `getClassOptions` – returns classes to which the assistant can be bound
- `getFieldOptions` – returns fields of the selected class
- `prepareBeforeSave` – invokes provider-specific changes before saving
- `getProviderSpecificOptions` – adds provider-specific options to `DatatablePageImpl`
- `getProviderFields` – returns additional fields displayed in the table editor
- `getAssistantStatus` – reports whether the provider is configured

## `AiService`

`AiService` processes assistant requests and uses dependency injection to obtain all CMS adapters `AiInterface`.

Important methods:

- `getProviders` – returns configured providers
- `getModelOptions` – returns provider models, optionally filtered by string
- `getAiResponse` – returns a full text response
- `getAiImageResponse` – returns an image response
- `getAiStreamResponse` – streams text response via `BufferedWriter`
- `getBonusHtml` – returns additional HTML assistant windows for the provider
