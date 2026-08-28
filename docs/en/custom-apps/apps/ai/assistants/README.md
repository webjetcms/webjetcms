# Adding a provider

Communication with an external AI service, including embeddings, belongs to a separate library [webjet-ai](https://github.com/webjetcms/webjet-ai). The library is framework-independent and may not import classes from `sk.iway.iwcm`, use Spring, or read WebJET `Constants`.

Integrating a new server provider into a project has three parts:

- implementation of `AiProvider`, which ensures communication with the provider's API,
- Spring bean of type `AiProvider`, through which the implementation is added to the built-in providers,
- one CMS service that extends [LibrarySupportLogic](../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/LibrarySupportLogic.java) and implements [AiAssitantsInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiAssitantsInterface.java).

[AiLibraryConfiguration](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiLibraryConfiguration.java) automatically binds the built-in library providers to all project Spring beans of type `AiProvider`. Do not modify this class when adding a provider. Domain processing, configuration, auditing, statistics, persistence, prompt macros, and temporary files remain managed by WebJET CMS.

The provider identifier is a public configuration value. It must be stable, non-empty, unique, and exactly the same in all three parts. We recommend using lowercase letters, for example `acme`. A duplicate identifier, including a collision with a built-in provider, will cause an error when starting the application.

## 1. Implementation `AiProvider`

Place the implementation in a separate library independent of WebJET CMS. The following minimal example is complete and compilable; replace the fixed response with a call to the vendor's API:

```java
package com.example.webjet.ai;

import java.util.List;

import com.webjetcms.ai.AiOperation;
import com.webjetcms.ai.AiProvider;
import com.webjetcms.ai.AiProviderConfig;
import com.webjetcms.ai.AiProviderException;
import com.webjetcms.ai.AiRequest;
import com.webjetcms.ai.AiResponse;
import com.webjetcms.ai.AiStreamListener;
import com.webjetcms.ai.ModelInfo;

public final class AcmeProvider implements AiProvider {

    public static final String PROVIDER_ID = "acme";

    @Override
    public String id() {
        return PROVIDER_ID;
    }

    @Override
    public List<ModelInfo> listModels(AiProviderConfig config) throws AiProviderException {
        requireConfigured(config);
        return List.of(new ModelInfo("acme-text-1", "Acme Text 1"));
    }

    @Override
    public AiResponse execute(AiRequest request, AiProviderConfig config) throws AiProviderException {
        requireConfigured(config);
        validateTextRequest(request);
        return AiResponse.text("Response from " + request.model());
    }

    @Override
    public AiResponse stream(
        AiRequest request,
        AiProviderConfig config,
        AiStreamListener listener
    ) throws AiProviderException {
        if (listener == null) {
            throw new AiProviderException(PROVIDER_ID, "Stream listener is required");
        }

        AiResponse response = execute(request, config);
        try {
            listener.onTextDelta(response.text());
        } catch (Exception exception) {
            throw new AiProviderException(PROVIDER_ID, "Stream listener failed", exception);
        }
        return response;
    }

    private static void requireConfigured(AiProviderConfig config) throws AiProviderException {
        if (config == null || config.isConfigured() == false) {
            throw new AiProviderException(PROVIDER_ID, "Acme API key is not configured");
        }
    }

    private static void validateTextRequest(AiRequest request) throws AiProviderException {
        if (request == null || request.operation() != AiOperation.TEXT) {
            throw new AiProviderException(PROVIDER_ID, "Only text requests are supported");
        }
        if (request.model() == null || request.model().isBlank()) {
            throw new AiProviderException(PROVIDER_ID, "Model is required");
        }
    }
}
```

The API key, endpoint, timeouts, and trusted headers are only passed to the provider through the immutable object `AiProviderConfig`. The provider must not directly access the servlet request, Spring services, database, or CMS configuration. The instance is used concurrently and throughout the `AiClient` lifecycle, so it must be thread-safe and should reuse transport resources. Release your own resources in the `close()` method.

The `embed` method has a default implementation that reports that the provider does not support embeddings. If the provider is to work with semantic search, implement `AiProvider.embed(EmbeddingRequest, AiProviderConfig)`. The response must contain exactly one `EmbeddingVector` for each input, in the same order and with the number of dimensions from `EmbeddingOptions`. [EmbeddingService](../../../../../src/main/java/sk/iway/iwcm/rag/embedding/EmbeddingService.java) checks the number and dimension of vectors.

For a full description of transport, embeddings, error handling, and lifecycle, see the documentation [Implementing and using a custom AI provider](https://github.com/webjetcms/webjet-ai/blob/main/docs/custom-providers.md).

## 2. Registering a Spring bean

In your project, create a configuration class that Spring component scan will find, and expose the implementation as a bean of type `AiProvider`:

```java
package com.example.webjet.cms.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.webjetcms.ai.AiProvider;
import com.example.webjet.ai.AcmeProvider;

@Configuration
public class AcmeAiConfiguration {

    @Bean(destroyMethod = "")
    public AiProvider acmeAiProvider() {
        return new AcmeProvider();
    }
}
```

The CMS passes all such beans to `AiClient.discover(...)`, which adds them to the built-in providers `openai`, `gemini`, and `openrouter`. The value of `destroyMethod = ""` is important: the provider lifecycle after successful client creation is owned by `AiClient`, so Spring should not call `close()` a second time.

Simply placing the JAR file on the classpath does not register the provider. The project must always explicitly instantiate it as a Spring bean. Do not modify the core bean `webjetAiClient` and do not create another `AiClient`.

## 3. CMS adapter and configuration

Create a single Spring service that both extends `LibrarySupportLogic` and implements `AiAssitantsInterface`. The base class will provide `AiClient` calls, auditing, statistics, prompt macros, temporary files, and model-specific fields for images:

```java
package com.example.webjet.cms.ai;

import java.util.List;

import org.springframework.stereotype.Service;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiProviderConfig;
import com.example.webjet.ai.AcmeProvider;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.components.ai.providers.LibrarySupportLogic;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

@Service
public class AcmeService extends LibrarySupportLogic implements AiAssitantsInterface {

    private static final String API_KEY = "ai_acmeAuthKey";
    private static final String IMAGE_NAME_MODEL = "ai_acme_generateFileNameModel";

    public AcmeService(
        AiClient aiClient,
        WebjetAiConfigurationService configurationService
    ) {
        super(aiClient, configurationService);
    }

    @Override
    public String getProviderId() {
        return AcmeProvider.PROVIDER_ID;
    }

    @Override
    public String getTitleKey() {
        return "components.ai_assistants.provider.acme.title";
    }

    @Override
    public String getApiKey() {
        return Constants.getString(API_KEY);
    }

    @Override
    public String getImageNameModel() {
        return Constants.getString(IMAGE_NAME_MODEL);
    }

    @Override
    public void configure(AiProviderConfig.Builder builder, String trustedReferer) {
        builder.trustedHeader("Referer", trustedReferer);
    }

    @Override
    public List<String> getFieldsToShow(String action) {
        if ("create".equals(action) || "edit".equals(action)) {
            return List.of("model", "useStreaming", "useTemporal");
        }
        return List.of();
    }

    @Override
    public void prepareBeforeSave(AssistantDefinitionEntity assistant) {
        if (Tools.isEmpty(assistant.getModel())) {
            assistant.setModel("acme-text-1");
        }
    }

    @Override
    public void setProviderSpecificOptions(
        DatatablePageImpl<AssistantDefinitionEntity> page,
        Prop prop
    ) {
        // Add provider-specific editor options when needed.
    }
}
```

The `getApiKey()` method is both the source of the credentials and the provider availability condition. For a provider without an API key, override `isInit()` accordingly. `getImageNameModel()` is only needed when the CMS generates the name of the stored image through that provider. The `configure(...)` method is optional; use it only to securely set your own endpoint or headers. The `trustedReferer` value is validated by the CMS. Never pass user-supplied headers directly and do not log secret values.

Create the configuration variables `ai_acmeAuthKey` and `ai_acme_generateFileNameModel` in the **Settings → Configuration** section; save the API key encrypted. Add the name to all project translation files, for example:

```properties
components.ai_assistants.provider.acme.title=Acme
```

[WebjetAiConfigurationService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/WebjetAiConfigurationService.java) adds common timeouts, creates an immutable `AiProviderConfig`, and calls `configure(...)` on the selected adapter. Therefore, the new provider is not added to any `switch` block or to the central list of configuration keys.

## 4. Embeddings and semantic search

The library defines provider-independent types `EmbeddingRequest`, `EmbeddingOptions`, `EmbeddingResponse` and `EmbeddingVector`. The RAG CMS module calls `AiClient.embed` via [EmbeddingService](../../../../../src/main/java/sk/iway/iwcm/rag/embedding/EmbeddingService.java) and does not contain a provider-specific HTTP client.

To use a new provider for RAG, all conditions must apply:

- `AcmeProvider` implements `embed(...)`,
- `AcmeProvider` is registered as a Spring bean,
- `AcmeService.getProviderId()` returns the same identifier,
- system assistants `RAG-EMB-INDEX` and `RAG-EMB-SEARCH` use provider `acme`, exactly the same model and the correct value `ragEmbeddingDimensions`.

The previous CMS SPI embedding has been removed. Do not create your own `EmbeddingProvider`.

## 5. Image generation options

Image options are not defined in the CMS adapter. The provider publishes them without a network call via `AiProvider.imageOptions(model, operation)`. For example:

```java
@Override
public Map<String, ImageOptionDefinition> imageOptions(
    String model,
    AiOperation operation
) {
    if ("acme-image-1".equals(model) && operation == AiOperation.GENERATE_IMAGE) {
        return Map.of(
            ImageOptions.COUNT, ImageOptionDefinition.integerRange(1, 4),
            ImageOptions.QUALITY, ImageOptionDefinition.choices("standard", "high"),
            "aspectRatio", ImageOptionDefinition.choices("1:1", "16:9", "9:16")
        );
    }
    return Map.of();
}
```

`LibrarySupportLogic` will automatically display only the supported fields **Number of images**, **Size**, **Quality**, and **Aspect ratio** from the metadata and send only their supported values ​​in the request. For the portable dimension, use the key `size`, the provider key `resolution` is also supported. For aspect ratio, the keys `aspectRatio` and `aspect_ratio` are recognized. An empty map means that the CMS will not display additional fields for the given model and operation. Metadata must not require an API key or a network call.

## Exception `AiInterface` browser only

[BrowserService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/browser/BrowserService.java) implements [AiInterface](../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiInterface.java) directly because Chrome Built-in AI runs in the browser and does not use server-side communication with the provider. This is the only way to implement it directly. New server-side providers must use `AiProvider` and `LibrarySupportLogic`.

## Local development

For concurrent local development of the CMS and a neighboring repository `webjet-ai`, use Gradle composite build:

```shell
./gradlew --include-build ../webjet-ai compileJava test
```

Use the same `--include-build ../webjet-ai` option for each local build and test task that is to use unreleased library changes. Otherwise, the `com.webjetcms:webjet-ai` version specified by the `webjetAiVersion` variable in `build.gradle` will be used.

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

- `getProviders` – returns all available providers and marks unconfigured ones
- `getModelOptions` – returns provider models, optionally filtered by string
- `getAiResponse` – returns a full text response
- `getAiImageResponse` – returns an image response
- `getAiStreamResponse` – streams text response via `BufferedWriter`
- `getBonusHtml` – returns additional fields supported by the selected image model
