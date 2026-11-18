# Adding a provider

Communication with an external AI service, including embeddings, belongs to a separate library [webjet-ai](https://github.com/webjetcms/webjet-ai). The library is framework-independent and may not import classes from `sk.iway.iwcm`, use Spring, or read WebJET `Constants`.

Integrating a new server provider into a project has three parts:

- implementation of `AiProvider`, which ensures communication with the provider's API,
- Spring bean of type `AiProvider`, through which the implementation is added to the built-in providers,
- one CMS service that extends [LibrarySupportLogic](../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/LibrarySupportLogic.java) and implements [AiAssitantsInterface](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiAssitantsInterface.java).

[AiLibraryConfiguration](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiLibraryConfiguration.java) automatically binds the built-in library providers to all project Spring beans of type `AiProvider`. Do not modify this class when adding a provider. Domain processing, configuration, auditing, statistics, persistence, prompt macros, and temporary files remain managed by WebJET CMS.

The provider identifier is a public configuration value. It must be stable, non-empty, unique, and exactly the same in all three parts. We recommend using lowercase letters, for example `acme`. A duplicate identifier, including a collision with a built-in provider, will cause an error when starting the application.

## Division of responsibilities

When simplifying adapters, keep the following tasks in CMS:

| Task | Implementation in CMS |
| --- | --- |
| Permissions, assistant selection, and allowed action control | `AssistantController`, `AiService` |
| Provider configuration and domain context | `WebjetAiConfigurationService` ; `AiTaskRegistry` passes context to the worker thread |
| Preparing CMS inputs and macros, protecting and restoring `INCLUDE` commands | `LibrarySupportLogic`, `IncludesHandler` |
| Audit and record token consumption by user and domain | `LibrarySupportLogic`, `AiStatService` |
| Saving and deleting temporary images, checking the destination folder | `AiTempFileStorage`, `AiTempFileRemover` |
| Binding chat continuation to session, user, domain, and assistant | `AiChatSession` ; the browser receives an opaque token instead of the provider's response ID |
| Logging and stopping user tasks | `AiTaskRegistry` ; passes `AiCancellationToken` to the library |
| Inserting a response into the editor, working with selections, and reverting changes | `EditorAi` ; transcription, draft and restoration of the conversation is managed by `AiChat` |

The library receives the prepared `AiRequest` and `AiProviderConfig`, returns `AiResponse` and reports errors with an exception `AiProviderException`. It provides the provider protocol, its models and capability metadata, stream transmission and error classification. Based on this data, the CMS decides on the availability of features and maintains assistant settings, such as consent to save the conversation via `useTemporal=false`.

The common JavaScript functions `requestAiText` and `parseAiResponse` remain part of the CMS. The first communicates with the CMS REST services, the second prepares content for the editor. Permissions, sessions, and saving changes are still managed by the respective CMS classes listed in the table.

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

## Built-in local models

The `com.webjetcms:webjet-ai-local` artifact extends the base library with local model providers. WebJET CMS registers three adapters:

- [LocalTextService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/local/text/LocalTextService.java) for `utter-project/EuroLLM-1.7B-Instruct`,
- [LocalTranslateService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/local/translate/LocalTranslateService.java) for `facebook/m2m100_418M`,
- [LocalEmbeddingService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/local/embedding/LocalEmbeddingService.java) for `intfloat/multilingual-e5-base`.

Each adapter uses a separate global path to the model ZIP package. [ConfiguredLocalProvider](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/local/ConfiguredLocalProvider.java) loads the path without domain context, opens the provider lazily on the first call, and safely reuses one instance until the application exits. Therefore, changing the path requires a restart.

The path can be an absolute path on the server or a path starting with `/WEB-INF/`, which the adapter translates via `Tools.getRealPath()` to a physical path in the deployed application. The scripts described in the [AI assistants](../../../../redactor/ai/settings/README.md) documentation will create approved packages in the `/WEB-INF/local-ai-models/` folder, for example `/WEB-INF/local-ai-models/eurollm-1.7b-instruct-q4-k-m.zip` and `/WEB-INF/local-ai-models/m2m100-418m-int8.zip`.

Local translation does not use a regular prompt. [LibrarySupportLogic](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/LibrarySupportLogic.java) creates a request for it with `TranslationOptions` and rejects HTML, `INCLUDE` commands, structured input, user prompts, and input macros in instructions. Instructions must contain JSON with `sourceLanguage` and `targetLanguage` ; the optional value of `maximumOutputTokens` is at most `200`. Local text generation rejects prompt expansion where the protection detects an attempt to `prompt injection`.

## Exception `AiInterface` browser only

[BrowserService](../../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/browser/BrowserService.java) implements [AiInterface](../../../../../src/main/java/sk/iway/iwcm/components/ai/providers/AiInterface.java) directly because Chrome Built-in AI runs in the browser and does not use server-side communication with the provider. This is the only way to implement it directly. New server-side providers must use `AiProvider` and `LibrarySupportLogic`.

## Local development

For concurrent local development of the CMS and a neighboring repository `webjet-ai`, use Gradle composite build:

```shell
./gradlew --include-build ../webjet-ai compileJava test
```

Use the same `--include-build ../webjet-ai` option for each local build and test task that is to use unreleased library changes. Otherwise, the `com.webjetcms:webjet-ai-local` version specified by the `webjetAiVersion` variable in `build.gradle` will be used.

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

### Chat and streaming support

`LibrarySupportLogic` selects an operation and validates streaming according to `AiClient.capabilities(providerId, model)`. Operation `CHAT` will be used for an assistant with action `LIVE_CHAT`, explicit `useTemporal=false`, chat support in the adapter, and a model that does not explicitly reject chat. Otherwise, operation `TEXT` remains. Metadata itself does not allow saving the conversation.

Streaming must be enabled in the assistant and the metadata must not indicate the selected operation or its streaming as unsupported. The result is passed in an existing `DataTableAi.useStreaming` and the same check is used when calling the streaming REST service. The stored value `useStreaming` is not changed. Unknown model or streaming support (`UNKNOWN`) does not block the request.

The browser provider retains its original setting. Local adapters do not load models when detecting capabilities; their restrictions set when saving the assistant still apply.

### Saved conversations

Both global chat and field chat use `AiChatHost` from `ai/ai-chat-host.js`. The host manages the assistant selection, history, and file panel; individual conversations are displayed by the `AiChat` component. The host retrieves the detail and passes it to the `AiChat.restoreHistory(detail)` method, which restores the messages and continuation rules without losing the newer draft or attachments of the rejected request. Common styles are in `_md-ai-chat.scss` under the `ai-chat` and `ai-chat-host` classes.

The list of available assistants is supplied by `QuickSearch` via callback `loadAssistants(signal)`, which calls `GET /admin/rest/ai/assistant/chat-assistants/`. The host provides loading state, retry on error, and request cancellation on expiration. The global menu requires action `live_chat` and the exact token `top-search-field` in `fieldTo` or `CHAT_SEARCH` in `className`. The values ​​in both lists can be separated by a comma, semicolon, or newline. When selecting assistants for editor fields, `top-search-field` is skipped and the other target fields are used. For the entity itself `CHAT_SEARCH`, an empty `fieldTo` is filled in to `*` on save.

`persistentChat` metadata allows saving only to active assistants with `live_chat` action, `useTemporal=false` explicit, OpenAI or Gemini provider, and chat support in adapter and model. `AiChatService` checks for quick search binding when starting and continuing a global chat, and when determining history availability. After the last such binding is removed, the history remains for reading. Chats with field context use the accessibility rules in the editor.

The saved chat is served by `AiChatController`, `AiChatService` and `AiChatStore` in the CMS. Endpoints under `/admin/rest/ai/chat` require `cmp_ai_button` permission, a logged in chat owner and the correct domain:

| Method and path | Purpose |
| --- | --- |
| `GET /threads?page=0&query=...` | Paginated history; also searches within message text, assistant and provider names regardless of diacritics. |
| `GET /threads/{id}` | Interview details, news, attachments and availability of continuation. |
| `POST /messages` | New message or continuation. JSON contains `assistantId`, `requestId`, `userPrompt`, `threadId` and `revision` if continuing, and `retryMessageId` if retrying a completed attempt. |
| `POST /threads/{id}/stop` | Stopping an active request from the chat owner. |
| `DELETE /threads/{id}` | Deleting responses from the provider and subsequently local history with attachments. |
| `GET /threads/{id}/attachments/{attachmentId}` | Authorized attachment loading; `download=true` forces download. |

For attachments, `POST /messages` uses the `multipart/form-data` format: the `message` field contains the JSON request and the `files` fields are files. `AiChatAttachments` checks the format and limits: 5 files, 10 MB in total, 100,000 characters of extracted text and up to 200 PDF pages. Supported formats are listed in the [user guide](../../../../redactor/ai/chat/README.md#attachments). The text of the documents and the description of the images are used as context; the image description is created by the selected model. The common method `LibrarySupportLogic.prepareChatAttachment()` returns the text of one attachment and the token consumption for both the saved and temporary chat. The caller loads the attachments and adds up the consumption; in the case of a saved chat, the price of the already described image remains recorded even after the next attachment fails.

Each chat has a folder `/files/protected/ai-chat/<domainId>/<userId>/<threadId>/` with a file `ai_chat.json`. Attachments are given a sequence number and the original extension, for example `1.docx`, `2.png` ; the numbering continues across messages. When downloaded, the original name is restored. Placing under `/files` allows sharing history between cluster nodes via NFS. The folder `/files` can be a symbolic link to a separate disk or shared storage; the chat uses the canonical storage path even when the history is first created. History and attachments are separated by domain and user. Writes are protected by a file lock of the owner in the given domain and atomic exchange of the JSON file. History and attachments are excluded from WAR, Git, ZIP archives and offline export. User operations in the file manager should not access, rename or overwrite them; the client loads them via an authorized endpoint with a CSRF header, including image previews.

`AiChatStore` stores the provider response IDs on the server. `requestId` protects against duplicate submissions and `revision` prevents continuation from an outdated state. After context loss or a change in the provider assistant or configuration, the history may remain read-only. `AiChatService` deletes the stored responses via `AiClient.deleteResponse(providerId, responseId, config)` from the `webjet-ai` library. The results of both `DELETED` and `ALREADY_ABSENT` confirm that the response is no longer with the provider; only after all responses have been confirmed will the CMS delete the local history and attachments. In the event of an exception, the state `DELETE_PENDING` remains and the local data is preserved for a repeat of the action, skipping already confirmed responses.

To regularly delete old conversations, add the `sk.iway.iwcm.components.ai.rest.AiChatCleanup` class to the scheduled tasks. The parameters are in the order `počet dní|ID domény|názov domény`, for example `90|7|www.example.sk`. In a normal installation without dividing data by domain, the ID is `1` ; the name must match the domain stored with the conversations. The task processes all owners in the specified domain, including chats for fields. For external storage, it selects the directory of the specified domain. Create a separate task for another domain. Deletion is not automatically enabled and the number of days must be positive.

The task selects entire conversations by `thread.updated`, not by age of individual attachments. Before marking for deletion, it re-verifies the last activity under lock and skips ongoing generation. It uses the same provider-based deletion of responses as manual deletion, and will also retry incomplete deletions in `DELETE_PENDING` state on the next run.

To clear chat history, use `AiChatCleanup` or `AiChatService.deleteOlderThan(scope, cutoff)` to preserve locks and also remove provider responses.

The `webjet-ai` library since version `2.1.0` reports the ID via `AiLifecycleListener` already during response processing. The CMS stores it for later deletion even in case of error or generation stop; it postpones the continuation of the conversation only after successful completion. In case of error, it uses `AiProviderException.lifecycle()`: the `NOT_SUBMITTED` and `REJECTED` states confirm that no deletion response was generated. The `UNKNOWN` or `RESPONSE_OBSERVED` state without an available ID remains unresolved and prevents local history deletion unless the CMS has captured the ID earlier. The HTTP status itself and the possibility of repeating the request do not confirm a rejection by the provider.

Field chat uses a host component `AiChatHost` with a context of `AiChatContext` for the saved record: `entityType` is the REST path of the table source without the trailing slash and query parameters, `entityId` is the ID of the edited record, and `fieldName` is the target field. `EditorAi` supplies assistants directly from the column definition, the content of the field or selected text, and the action to insert a response. The global assistant list is not loaded for the field. New records and bulk edit use `AiChat` without persistence. The editor accesses both variants via `conversation.view` with the methods `mount`, `unmount`, `destroy`, and `isPending` ; for the host, `isPending` also includes conversations running in the background after switching the assistant.

The request `POST /messages` at the field also contains an object `context` with these three values ​​and `inputValue` with the source content. The context is saved at the time of the conversation and cannot be changed by the continuation. The source content is saved at the time of the question so that a retry of a failed attempt uses the original input along with the original attachments. The new question receives the current content of the field.

When a valid selection is made in CKEditor or the PageBuilder text editor, `EditorAi` also sends `inputIsSelectedTextInEditor=true`. `LibrarySupportLogic` when the `live_chat` action is performed, it adds an instruction to the instructions to edit the HTML fragment itself without new wrappers or unwanted headings. The instruction takes precedence over the rules for generating the entire page, so it also works with the existing Website Builder definition and does not require recognition of configurable wrappers. The flag is also transmitted via `InputDataDTO` during a temporary chat and is stored in `ai_chat.json` when asked; repetition uses the original value. A missing flag means `false`.

History calls `GET /threads?page=0&entityType=...&entityId=...&fieldName=...`, or `query`. The server requires all three filters and compares them exactly before paging. Without filters, it returns only individual conversations without context. Context is used to sort private history, it does not provide access to the entity itself; control of the owner and domain of the conversation is maintained. When the editor is closed, running requests are canceled and the written messages are discarded, saved conversations and attachments remain available.

For chat assistants, OpenAI and Gemini support the `POST /admin/rest/ai/assistant/response/` and `POST /admin/rest/ai/assistant/response-stream/` endpoints as well as `multipart/form-data`. The `message` field contains JSON `InputDataDTO` including the field context, `userPrompt`, `assistantId`, `timestamp` and optionally `conversationId` ; repeated fields `files` contain attachments. Without attachments, the request remains in JSON. The server uses the same validation and content extraction as the stored chat, but holds the files only while the request is being processed. The contents of the attachments are appended to the field context. Attachment support for the field does not require temporary chat to be disabled.

### Transferring the ongoing response to the administration

Text generation in the editor and chat use `requestAiText` from the file `ai-rest-transport.js`. The function sends a request with a CSRF token, checks the HTTP and JSON response, and uses `readAiStream` when streaming. It returns text and CMS metadata without modifying the content. The function `parseAiResponse` from the file `ai-response-parser.js` separates the HTML and explanation for generation in the editor and inserting the response from the chat. Chat management is provided by `AiChat`. For HTTP errors 401 and 403, the transfer function displays an existing logout or CSRF token error warning.

Editor generation and unsaved chat call `POST /admin/rest/ai/assistant/response-stream/` with header `Accept: application/x-ndjson`. Saved chat uses `POST /admin/rest/ai/chat/messages` with the same header; it also sends a `metadata` message before the text with the chat and message identifiers and the current revision. The server sends separate JSON messages separated by a newline:

```json
{"type":"delta","text":"Prvá časť odpovede.\n"}
{"type":"delta","text":"Pokračovanie odpovede."}
{"type":"result","result":{"response":"Prvá časť odpovede.\nPokračovanie odpovede.","totalTokens":42,"error":null,"tempFiles":[],"generatedFileName":null}}
```

The `delta` messages contain only the generated text. The final message `result` contains `AssistantResponseDTO` including token consumption and any errors. The client composes incomplete lines between network blocks and also processes multiple messages in one block. The response is considered successful only after the final message is received without errors and the transmission is completed.

Without an explicit request for `application/x-ndjson`, the original format `text/plain` remains on the `response-stream` endpoints for older clients; the `/chat/messages` endpoint returns a regular JSON response. CMS adapters continue to write plain text to `BufferedWriter` ; message separation is provided by controllers via `AssistantStreamWriter`, without changing the library interface `webjet-ai`.

### Provider errors

The error response retains the `error` text field and may also contain `errorCategory`, `providerStatusCode` and `retryable`. The same fields are in the regular JSON response and in the final message `result` of the stream. `providerStatusCode` is a positive HTTP status of the external service, not the CMS endpoint; if the value is unknown, it is not sent. `retryable` is a provider recommendation (`true` or `false`), if the information is missing, it is not sent. If the value is `false`, the chat will not offer to repeat the same request; the user can repeat the stopped response. Neither the full provider response nor the exception reason are sent to the browser.

| `errorCategory` | Chat behavior |
| --- | --- |
| `CANCELLED` | Displays the stopped response and preserves the last successful continuation. |
| `INVALID_CONTINUATION` | Offers to start a new conversation. Only after the user chooses, will it cancel the invalid continuation; it will preserve the transcript of messages and the text in progress. |
| `UNSUPPORTED_OPERATION` | It will explain the need to change the assistant or model instead of repeating the same request. |
| `UNKNOWN` | It uses normal error handling; it also handles an unknown future category. |

Chat requests do not automatically retry or start a new conversation. The library determines categories based on reliable provider data, not the error message text. Gemini remote continuation errors therefore remain `UNKNOWN` for now. Existing CMS validation errors, such as an expired or invalid chat token in the CMS session, are also handled.

### Stopping the request

`AiTaskRegistry` creates a separate `AiCancellationToken` for each task. The **Stop** button, starting a new chat, or closing the editor will cancel both the token and the `Future` of the respective task via the existing `stop-assistant` service. `LibrarySupportLogic` passes the token to the library for text, chat, and image requests, including the subsequent generation of the image title. Other concurrent requests remain active.

OpenAI, Gemini, and OpenRouter will abort the HTTP request, including waiting for the next part of the stream. A paused chat will retain the last successful continuation; a partial response will not replace it. Closing the chat window itself will only hide its contents and will not stop the ongoing request.

A custom provider may override the `execute` and `stream` variants with the `AiCancellationToken` parameter and attach a cancellation of its transport to it. Default implementations, including local models, check for cancellation before and after execution and when passing response parts; they cannot force an abort of a blocked native computation. Canceling an HTTP connection does not guarantee that processing or billing on the external service's side will stop, and does not delete already stored conversation context.
