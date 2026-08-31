package sk.iway.iwcm.components.ai.providers.local.translate;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiOperation;
import com.webjetcms.ai.AiRequest;
import com.webjetcms.ai.TranslationOptions;

import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.components.ai.providers.IncludesHandler;
import sk.iway.iwcm.components.ai.providers.LibrarySupportLogic;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

/**
 * Adapts the local M2M100 provider to CMS assistants and enforces explicitly configured plain-text translation.
 */
@Service
public class LocalTranslateService extends LibrarySupportLogic implements AiAssitantsInterface {

    public static final String DEFAULT_MODEL = LocalTranslateProvider.MODEL_ID;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TRANSLATOR_API = "Translator";

    private final LocalTranslateProvider localTranslateProvider;

    @Autowired
    public LocalTranslateService(
        AiClient aiClient,
        WebjetAiConfigurationService configurationService,
        LocalTranslateProvider localTranslateProvider
    ) {
        super(aiClient, configurationService);
        this.localTranslateProvider = localTranslateProvider;
    }

    @Override
    public String getProviderId() {
        return LocalTranslateProvider.PROVIDER_ID;
    }

    @Override
    public String getTitleKey() {
        return "components.ai_assistants.provider.local-translation.title";
    }

    @Override
    public boolean isInit() {
        return localTranslateProvider.isConfigured();
    }

    @Override
    public List<String> getFieldsToShow(String action) {
        if ("create".equals(action) || "edit".equals(action)) return List.of("model");
        return List.of();
    }

    /**
     * Validates translation instructions and applies the fixed local translation settings.
     *
     * @param assistantEntity assistant configuration being prepared for persistence
     * @throws IllegalStateException if the instructions do not contain valid translation options
     */
    @Override
    public void prepareBeforeSave(AssistantDefinitionEntity assistantEntity) {
        try {
            parseTranslationOptions(assistantEntity.getInstructions());
        } catch (IOException exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        }
        assistantEntity.setModel(DEFAULT_MODEL);
        assistantEntity.setUseStreaming(false);
        assistantEntity.setUseTemporal(true);
        assistantEntity.setUserPromptEnabled(false);
        assistantEntity.setKeepHtml(true);
    }

    @Override
    public void setProviderSpecificOptions(DatatablePageImpl<AssistantDefinitionEntity> page, Prop prop) {
    }

    /**
     * Builds a validated plain-text request for the local translation provider.
     *
     * @param operation requested AI operation
     * @param assistant assistant configuration supplying the model and translation instructions
     * @param inputData input data to translate
     * @param includesHandler handler used to detect protected WebJET INCLUDE commands; may be {@code null}
     * @return request containing normalized translation options
     * @throws IOException if the operation or input is unsupported, or the translation options are invalid
     */
    @Override
    protected AiRequest prepareProviderRequest(
        AiOperation operation,
        AssistantDefinitionEntity assistant,
        InputDataDTO inputData,
        IncludesHandler includesHandler
    ) throws IOException {
        if (operation != AiOperation.TEXT) {
            throw new IOException("Local translation supports text requests only");
        }
        if (InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType())) {
            throw new IOException("Local translation does not support image input");
        }
        if (includesHandler != null && includesHandler.hasIncludes()) {
            throw new IOException("Local translation cannot safely process WebJET INCLUDE commands");
        }
        if (inputData.getInputValue() == null || inputData.getInputValue().isBlank()) {
            throw new IOException("Local translation input must not be blank");
        }
        if (Jsoup.parseBodyFragment(inputData.getInputValue()).body().children().isEmpty() == false) {
            throw new IOException("Local translation cannot safely process HTML input");
        }

        return AiRequest.builder()
            .operation(operation)
            .model(assistant.getModel())
            .inputText(inputData.getInputValue())
            .store(false)
            .translationOptions(parseTranslationOptions(assistant.getInstructions()))
            .build();
    }

    /**
     * Parses and validates local translation options from JSON or a {@code Translator:} instruction.
     *
     * @param instructions assistant instructions containing source and target language configuration
     * @return normalized translation options
     * @throws IOException if the instructions are missing, malformed, or contain unsupported values
     */
    static TranslationOptions parseTranslationOptions(String instructions) throws IOException {
        if (Tools.isEmpty(instructions)) {
            throw new IOException("Local translation instructions must define sourceLanguage and targetLanguage");
        }

        String configJson = instructions.trim();
        if (configJson.startsWith("{") == false) {
            int separator = configJson.indexOf(':');
            if (separator < 0 || TRANSLATOR_API.equalsIgnoreCase(configJson.substring(0, separator).trim()) == false) {
                throw new IOException("Local translation instructions must use the Translator JSON format");
            }
            configJson = configJson.substring(separator + 1).trim();
        }

        JsonNode config = MAPPER.readTree(configJson);
        if (config == null || config.isObject() == false) {
            throw new IOException("Local translation instructions must contain a JSON object");
        }

        String sourceLanguage = requiredLanguage(config, "sourceLanguage");
        String targetLanguage = requiredLanguage(config, "targetLanguage");
        Integer maximumOutputTokens = optionalPositiveInteger(config, "maximumOutputTokens");
        return new TranslationOptions(sourceLanguage, targetLanguage, maximumOutputTokens);
    }

    /**
     * Reads, validates, and normalizes a required language option.
     *
     * @param config parsed translation configuration
     * @param fieldName name of the required language field
     * @return normalized lowercase language code, with {@code cz} converted to {@code cs}
     * @throws IOException if the field is invalid or its dynamic user language cannot be resolved
     */
    private static String requiredLanguage(JsonNode config, String fieldName) throws IOException {
        JsonNode value = config.get(fieldName);
        if (value == null || value.isTextual() == false || value.textValue().isBlank()) {
            throw new IOException("Local translation instructions must define " + fieldName);
        }

        String language = value.textValue().trim();
        if ("autodetect".equalsIgnoreCase(language)) {
            throw new IOException(
                "Local translation requires an explicit " + fieldName + "; autodetect is not supported"
            );
        }
        if ("userLng".equalsIgnoreCase(language)) {
            RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
            if (requestBean == null || Tools.isEmpty(requestBean.getLng())) {
                throw new IOException("The current user language is not available for local translation");
            }
            language = requestBean.getLng();
        }

        String normalized = language.toLowerCase(Locale.ROOT).replace('-', '_');
        return "cz".equals(normalized) ? "cs" : normalized;
    }

    /**
     * Reads an optional positive integer bounded by the local provider output-token limit.
     *
     * @param config parsed translation configuration
     * @param fieldName name of the optional integer field
     * @return validated integer, or {@code null} when the field is absent or null
     * @throws IOException if the value is not a positive integer or exceeds the supported limit
     */
    private static Integer optionalPositiveInteger(JsonNode config, String fieldName) throws IOException {
        JsonNode value = config.get(fieldName);
        if (value == null || value.isNull()) return null;
        if (value.isIntegralNumber() == false || value.canConvertToInt() == false || value.intValue() < 1) {
            throw new IOException("Local translation " + fieldName + " must be a positive integer");
        }
        if (value.intValue() > LocalTranslateProvider.MAXIMUM_OUTPUT_TOKENS) {
            throw new IOException(
                "Local translation " + fieldName + " must not exceed "
                    + LocalTranslateProvider.MAXIMUM_OUTPUT_TOKENS
            );
        }
        return value.intValue();
    }
}
