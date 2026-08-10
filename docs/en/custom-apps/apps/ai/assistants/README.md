# Adding a provider

Communication with an external AI service belongs in the standalone [webjet-ai](https://github.com/webjetcms/webjet-ai) library. The library is framework-neutral and must not import classes from `sk.iway.iwcm` or read WebJET `Constants`.

A server-side provider integration has three parts:

- an `AiProvider` implementation in `webjet-ai`, which handles provider communication, response processing, and streaming
- a thin WebJET CMS service extending [LibrarySupportLogic](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/LibrarySupportLogic.java), which connects the library to the CMS
- an optional [AiAssitantsInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiAssitantsInterface.java) implementation for provider-specific fields in the assistant editor

Register the provider in [AiLibraryConfiguration](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiLibraryConfiguration.java) and map the CMS configuration in [WebjetAiConfigurationService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/WebjetAiConfigurationService.java). Request and domain handling, configuration, auditing, statistics, persistence, prompt macros, and temporary files remain the responsibility of WebJET CMS.

The previous CMS transport SPI has been removed. Existing custom server-side providers must be migrated to the library's `AiProvider` interface and the CMS `LibrarySupportLogic` adapter.

## Implementing `AiProvider`

Implement `com.webjetcms.ai.AiProvider` in the standalone library or in another framework-neutral library that depends on it. The stable value returned by `id()` identifies the provider in both the library and the CMS adapters.

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

API keys, endpoint overrides, timeouts, and trusted headers enter the provider only through the immutable `AiProviderConfig` object. The library must not directly access servlet requests, Spring services, the database, or CMS configuration. Complete implementations are available among the providers in the [webjet-ai repository](https://github.com/webjetcms/webjet-ai).

## Registering the provider in CMS

Add a provider instance to the `AiClient` managed by the CMS:

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

Create a thin adapter extending `LibrarySupportLogic`. The base class maps CMS requests to library requests while keeping auditing, statistics, prompt processing, and temporary-file handling in the CMS:

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

The provider identifier must match the value returned by `AiProvider.id()`. Add the title key to the CMS translation files.

## Mapping WebJET configuration

Add the provider's configuration key to [WebjetAiConfigKeys](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/WebjetAiConfigKeys.java) and map it in `WebjetAiConfigurationService`:

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

`WebjetAiConfigurationService.resolve(providerId, request)` creates an `AiProviderConfig` object for the current request. Extend the method when a provider needs a CMS-managed endpoint or a trusted metadata header. Never forward user-supplied headers directly, and never write credentials to logs.

## Implementing `AiAssitantsInterface`

Create this CMS adapter when the provider needs default values or custom fields in the assistant editor. Check whether the provider is configured through `WebjetAiConfigurationService`:

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

## Browser-only `AiInterface` exception

[BrowserService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/browser/BrowserService.java) implements [AiInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiInterface.java) directly because Chrome Built-in AI runs in the browser and does not use server-side provider communication. This is the only direct implementation path. New server-side providers must use `AiProvider` and `LibrarySupportLogic`.

## Local development

Until `com.webjetcms:webjet-ai:0.1.0` is available from Maven Central, run CMS Gradle tasks from the CMS repository with the sibling library explicitly included:

```shell
./gradlew --include-build ../webjet-ai compileJava test
```

Use the same `--include-build ../webjet-ai` option for every local CMS build and verification task. Do not add a permanent `includeBuild` entry to `settings.gradle`, do not use `mavenLocal()`, and do not copy the library JAR into the CMS. Until version `0.1.0` is published, a plain CMS build is expected to fail.

## `AiAssistantsService`

`AiAssistantsService` handles requests related to the [Assistants](../../../../redactor/ai/settings/README.md) data table. Through dependency injection, it receives all `AiAssitantsInterface` implementations and selects one by its provider identifier.

Important methods:

- `getAssistantAndFieldFrom` – returns assistants that meet the display conditions for the selected field
- `getClassOptions` – returns classes to which an assistant can be bound
- `getFieldOptions` – returns fields of the selected class
- `prepareBeforeSave` – invokes provider-specific changes before saving
- `getProviderSpecificOptions` – adds provider-specific options to `DatatablePageImpl`
- `getProviderFields` – returns additional fields displayed in the table editor
- `getAssistantStatus` – reports whether the provider is configured

## `AiService`

`AiService` handles assistant requests and receives all CMS `AiInterface` adapters through dependency injection.

Important methods:

- `getProviders` – returns configured providers
- `getModelOptions` – returns provider models, optionally filtered by a string
- `getAiResponse` – returns a complete text response
- `getAiImageResponse` – returns an image response
- `getAiStreamResponse` – streams a text response through `BufferedWriter`
- `getBonusHtml` – returns additional assistant-window HTML for the provider
