package sk.iway.iwcm.components.ai.providers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiOperation;
import com.webjetcms.ai.AiPromptTemplate;
import com.webjetcms.ai.AiProviderConfig;
import com.webjetcms.ai.AiProviderException;
import com.webjetcms.ai.AiRequest;
import com.webjetcms.ai.AiResponse;
import com.webjetcms.ai.BinaryContent;
import com.webjetcms.ai.GeneratedMedia;
import com.webjetcms.ai.ImageOptions;
import com.webjetcms.ai.ModelInfo;
import com.webjetcms.ai.TokenUsage;
import com.webjetcms.ai.security.PromptInjectionDefense.UntrustedSource;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.SupportedActions;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;
import sk.iway.iwcm.components.ai.rest.AiTempFileStorage;
import sk.iway.iwcm.components.ai.security.PromptInjectionDefense;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.components.ai.stat.rest.AiStatService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;
import sk.iway.iwcm.utils.Pair;

/**
 * CMS adapter around the framework-neutral {@code webjet-ai} client.
 *
 * <p>CMS-side provider integrations should extend this class.</p>
 */
public abstract class LibrarySupportLogic implements AiInterface {

    private static final String METHOD_TEXT_RESPONSE = "getAiResponse";
    private static final String METHOD_TEXT_STREAM_RESPONSE = "getAiStreamResponse";
    private static final String METHOD_IMAGE_RESPONSE = "getAiImageResponse";
    private static final String IMAGE_AUDIT_RESPONSE =
        "Provider response for image contains binary data, so image payloads are not audited.";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiClient aiClient;
    private final WebjetAiConfigurationService configurationService;

    protected LibrarySupportLogic(AiClient aiClient, WebjetAiConfigurationService configurationService) {
        this.aiClient = aiClient;
        this.configurationService = configurationService;
    }

    @Override
    public boolean isInit() {
        return configurationService.isConfigured(this);
    }

    @Override
    public List<LabelValue> getSupportedModels(Prop prop, HttpServletRequest request) {
        List<LabelValue> values = new ArrayList<>();
        try {
            AiProviderConfig config = configurationService.resolve(this, request);
            for (ModelInfo model : aiClient.listModels(getProviderId(), config)) {
                values.add(new LabelValue(model.displayLabel(), model.id()));
            }
        } catch (AiProviderException exception) {
            StringBuilder audit = new StringBuilder(getProviderId()).append(" getSupportedModels -> FAILED");
            audit.append("\n\nError message:\n").append(providerErrorMessage(exception, prop));
            audit.append("\n\nFull response:\n").append(auditValue(exception.rawResponse()));
            Adminlog.add(Adminlog.TYPE_AI, audit.toString(), -1, -1);
        } catch (Exception exception) {
            Logger.error(LibrarySupportLogic.class, "Error processing models " + exception.getMessage());
        }
        return values;
    }

    @Override
    public AssistantResponseDTO getAiResponse(
        AssistantDefinitionEntity assistant,
        InputDataDTO inputData,
        Prop prop,
        AiStatRepository statRepo,
        HttpServletRequest request
    ) throws ProviderCallException {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        Pair<String, String> inputPair = new Pair<>(inputData.getInputValue(), inputData.getUserPrompt());

        try {
            IncludesHandler includesHandler = IncludesHandler.protectIncludes(inputData);
            AiResponse response = aiClient.execute(
                getProviderId(),
                prepareRequest(AiOperation.TEXT, assistant, inputData, includesHandler),
                configurationService.resolve(this, request)
            );

            response = response.withText(includesHandler.restoreIncludes(response.text()));
            responseDto.setResponse(response.text());
            successAdminLog(
                responseDto,
                inputPair,
                assistant,
                METHOD_TEXT_RESPONSE,
                responseDto.getResponse(),
                response.usage(),
                statRepo,
                request
            );
        } catch (AiProviderException exception) {
            errorAdminLog(assistant, inputPair, METHOD_TEXT_RESPONSE, exception, prop);
        } catch (Exception exception) {
            errorAdminLog(
                assistant,
                inputPair,
                METHOD_TEXT_RESPONSE,
                new Pair<>(safeMessage(exception), null)
            );
        }
        return responseDto;
    }

    @Override
    public AssistantResponseDTO getAiStreamResponse(
        AssistantDefinitionEntity assistant,
        InputDataDTO inputData,
        Prop prop,
        AiStatRepository statRepo,
        BufferedWriter writer,
        HttpServletRequest request
    ) throws ProviderCallException {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        Pair<String, String> inputPair = new Pair<>(inputData.getInputValue(), inputData.getUserPrompt());

        try {
            IncludesHandler includeHandler = IncludesHandler.protectIncludes(inputData);

            AiResponse response = aiClient.stream(
                getProviderId(),
                prepareRequest(AiOperation.TEXT, assistant, inputData, includeHandler),
                configurationService.resolve(this, request),
                delta -> includeHandler.handleLine(delta, writer)
            );
            includeHandler.finish(writer);

            responseDto.setResponse(includeHandler.getWholeResponse());
            successAdminLog(
                responseDto,
                inputPair,
                assistant,
                METHOD_TEXT_STREAM_RESPONSE,
                responseDto.getResponse(),
                response.usage(),
                statRepo,
                request
            );
        } catch (AiProviderException exception) {
            errorAdminLog(assistant, inputPair, METHOD_TEXT_STREAM_RESPONSE, exception, prop);
        } catch (Exception exception) {
            errorAdminLog(
                assistant,
                inputPair,
                METHOD_TEXT_STREAM_RESPONSE,
                new Pair<>(safeMessage(exception), null)
            );
        }
        return responseDto;
    }

    @Override
    public AssistantResponseDTO getAiImageResponse(
        AssistantDefinitionEntity assistant,
        InputDataDTO inputData,
        Prop prop,
        AiStatRepository statRepo,
        HttpServletRequest request
    ) throws ProviderCallException {
        AssistantResponseDTO responseDto = new AssistantResponseDTO();
        Pair<String, String> inputPair = new Pair<>(inputData.getInputValue(), inputData.getUserPrompt());

        try {
            Path tempFileFolder = AiTempFileStorage.getFileFolder();
            AiOperation operation = imageOperation(inputData);
            AiResponse response = aiClient.execute(
                getProviderId(),
                prepareRequest(operation, assistant, inputData, null),
                configurationService.resolve(this, request)
            );

            long datePart = Tools.getNow();
            for (GeneratedMedia image : response.media()) {
                try {
                    String format = image.suggestedFileExtension()
                        .map(extension -> "." + extension)
                        .orElseThrow(() -> new IOException("Image format is not supported: " + image.mediaType()));
                    if (FileTools.isImage(format) == false) {
                        throw new IOException("Image format is not valid: " + format);
                    }
                    String filePrefix = "tmp-ai-" + DocTools.removeChars(assistant.getName()) + "-" + datePart + "-";
                    responseDto.addTempFile(AiTempFileStorage.addImage(image.data(), filePrefix, format, tempFileFolder));
                } catch (IOException exception) {
                    Logger.error(LibrarySupportLogic.class, "Error processing image " + exception.getMessage());
                }
            }

            if (responseDto.getTempFiles() == null || responseDto.getTempFiles().isEmpty()) {
                throw new IllegalStateException(prop.getText("components.ai_assistants.no_image.err"));
            }

            GeneratedImageName generatedFileName = getGeneratedImageName(assistant, inputData, request);
            responseDto.setGeneratedFileName(generatedFileName.fileName());

            successAdminLog(
                responseDto,
                inputPair,
                assistant,
                METHOD_IMAGE_RESPONSE,
                IMAGE_AUDIT_RESPONSE,
                safeUsage(response.usage()).plus(generatedFileName.usage()),
                statRepo,
                request
            );
        } catch (AiProviderException exception) {
            errorAdminLog(assistant, inputPair, METHOD_IMAGE_RESPONSE, exception, prop);
        } catch (Exception exception) {
            errorAdminLog(
                assistant,
                inputPair,
                METHOD_IMAGE_RESPONSE,
                new Pair<>(safeMessage(exception), null)
            );
        }
        return responseDto;
    }

    protected final WebjetAiConfigurationService configurationService() {
        return configurationService;
    }

    static AiRequest prepareRequest(
        AiOperation operation,
        AssistantDefinitionEntity assistant,
        InputDataDTO inputData,
        IncludesHandler includesHandler
    ) throws IOException {
        AiPromptTemplate.ExpansionResult expansion = AiAssistantsService.expandPromptMacros(
            assistant.getInstructions(),
            inputData
        );
        String instructions = expansion.instructions();
        if (includesHandler != null && includesHandler.hasIncludes()) {
            instructions = instructions + "\n" + includesHandler.preservationInstructions();
        }

        String inputText = inputData.getInputValue();
        String userPrompt = inputData.getUserPrompt();
        if (inputData.isStructuredInput()) {
            if (expansion.consumedSources().contains(UntrustedSource.INPUT_TEXT)) inputText = "";
            if (expansion.consumedSources().contains(UntrustedSource.USER_PROMPT)) userPrompt = "";
        } else if (expansion.consumedSources().isEmpty() == false) {
            // Preserve legacy CMS behavior for editor/page-builder assistants.
            inputText = "";
            userPrompt = "";
        }

        AiRequest request = buildRequest(operation, assistant, inputData, instructions, inputText, userPrompt);
        EnumSet<UntrustedSource> suspiciousSources = EnumSet.noneOf(UntrustedSource.class);
        suspiciousSources.addAll(expansion.suspiciousSources());
        suspiciousSources.addAll(request.suspiciousSources());
        PromptInjectionDefense.auditDetections(suspiciousSources, assistant.getId());
        return request;
    }

    static AiOperation imageOperation(InputDataDTO inputData) {
        return InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType())
            ? AiOperation.EDIT_IMAGE
            : AiOperation.GENERATE_IMAGE;
    }

    static AiRequest buildRequest(
        AiOperation operation,
        AssistantDefinitionEntity assistant,
        InputDataDTO inputData,
        String instructions,
        String inputText,
        String userPrompt
    ) throws IOException {
        AiRequest.Builder builder = AiRequest.builder()
            .operation(operation)
            .model(assistant.getModel())
            .instructions(instructions)
            .userPrompt(userPrompt)
            .store(Tools.isTrue(assistant.getUseTemporal()) == false)
            .imageOptions(new ImageOptions(
                inputData.getImageCount(),
                inputData.getImageSize(),
                inputData.getImageQuality()
            ));

        if (InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType())) {
            if (inputData.getInputFile() == null) return builder.build();
            builder.inputMedia(BinaryContent.from(inputData.getInputFile().toPath(), inputData.getMimeType()));
        } else {
            builder.inputText(inputText);
        }
        return builder.build();
    }

    GeneratedImageName getGeneratedImageName(
        AssistantDefinitionEntity assistant,
        InputDataDTO inputData,
        HttpServletRequest request
    ) {
        String defaultFileName = "ai_generated_image_" + System.currentTimeMillis();
        if (SupportedActions.GENERATE_IMAGE.getAction().equals(assistant.getAction()) == false) {
            return new GeneratedImageName(defaultFileName, TokenUsage.EMPTY);
        }

        Pair<String, String> instructionValuePair = getImageNameInstructions(assistant, inputData);
        if (instructionValuePair == null) return new GeneratedImageName(defaultFileName, TokenUsage.EMPTY);

        try {
            AiRequest rawRequest = AiRequest.builder()
                .operation(AiOperation.TEXT)
                .model(getImageNameModel())
                .instructions(instructionValuePair.getFirst())
                .inputText(instructionValuePair.getSecond())
                .store(false)
                .build();
            PromptInjectionDefense.auditDetections(rawRequest.suspiciousSources(), assistant.getId());
            AiResponse response = aiClient.execute(
                getProviderId(),
                rawRequest,
                configurationService.resolve(this, request)
            );
            if (Tools.isEmpty(response.text())) return new GeneratedImageName(defaultFileName, TokenUsage.EMPTY);
            return new GeneratedImageName(response.text(), safeUsage(response.usage()));
        } catch (Exception exception) {
            Logger.error(LibrarySupportLogic.class, "Error processing image name " + exception.getMessage());
            return new GeneratedImageName(defaultFileName, TokenUsage.EMPTY);
        }
    }

    private Pair<String, String> getImageNameInstructions(
        AssistantDefinitionEntity assistant,
        InputDataDTO inputData
    ) {
        String instructions = configurationService.imageNamePrompt();
        if (Tools.isEmpty(instructions)) return null;

        String value;
        try {
            if (Tools.isNotEmpty(inputData.getUserPrompt())) {
                value = inputData.getUserPrompt();
            } else {
                value = extractImageNameValue(assistant, inputData);
            }
        } catch (Exception exception) {
            return null;
        }
        return new Pair<>(instructions, value);
    }

    private String extractImageNameValue(
        AssistantDefinitionEntity assistant,
        InputDataDTO inputData
    ) throws JsonProcessingException {
        try {
            JsonNode instructions = MAPPER.readTree(assistant.getInstructions());
            String value = instructions.has("inputText") ? inputData.getInputValue() : "";
            if (Tools.isEmpty(value)) {
                value = joinAllValues(
                    AiAssistantsService.expandPromptMacros(assistant.getInstructions(), inputData).instructions(),
                    "."
                );
            }
            return value;
        } catch (JsonProcessingException exception) {
            return assistant.getInstructions();
        }
    }

    private String joinAllValues(String json, String delimiter) {
        if (Tools.isEmpty(json)) return "";
        List<String> values = new ArrayList<>();
        try {
            collectValues(MAPPER.readTree(json), values);
        } catch (JsonProcessingException exception) {
            return json;
        }
        return String.join(delimiter, values);
    }

    private void collectValues(JsonNode node, List<String> values) {
        if (node == null || node.isNull()) return;
        if (node.isContainerNode()) {
            node.elements().forEachRemaining(child -> collectValues(child, values));
        } else if (node.isTextual()) {
            values.add(node.asText());
        } else if (node.isNumber() || node.isBoolean()) {
            values.add(node.asText());
        }
    }

    private void successAdminLog(
        AssistantResponseDTO responseDto,
        Pair<String, String> inputPair,
        AssistantDefinitionEntity assistant,
        String methodName,
        String textResponse,
        TokenUsage usage,
        AiStatRepository statRepo,
        HttpServletRequest request
    ) {
        StringBuilder audit = new StringBuilder("SUCCESS: ");
        audit.append(assistant.getName()).append(" (").append(assistant.getProvider()).append(") ");
        audit.append(methodName).append('\n');
        appendAssistantInfo(assistant, audit);

        audit.append("\nAction cost:\n");
        int totalTokens = appendUsageAndReturnTotal(audit, usage);

        int auditMaxLength = configurationService.auditMaxLength();
        if (auditMaxLength > 0) {
            StringBuilder details = new StringBuilder("\nInput value:\n");
            details.append(Tools.isEmpty(inputPair.getFirst()) ? " - " : inputPair.getFirst());
            if (Tools.isNotEmpty(inputPair.getSecond())) {
                details.append("\nUser prompt:\n").append(inputPair.getSecond());
            }
            details.append("\nAI response:\n").append(textResponse);
            audit.append(DB.prepareString(details.toString(), auditMaxLength));
        }

        try {
            Adminlog.add(Adminlog.TYPE_AI, audit.toString(), totalTokens, -1);
            AiStatService.addRecord(assistant.getId(), totalTokens, statRepo, request);
        } catch (Exception exception) {
            Logger.error(LibrarySupportLogic.class, "Error processing adminlog " + exception.getMessage());
        }
        responseDto.setTotalTokens(totalTokens);
    }

    private int appendUsageAndReturnTotal(StringBuilder audit, TokenUsage usage) {
        TokenUsage normalizedUsage = safeUsage(usage);
        audit.append("\t input_tokens: ").append(normalizedUsage.inputTokens()).append('\n');
        audit.append("\t output_tokens: ").append(normalizedUsage.outputTokens()).append('\n');
        for (Map.Entry<String, Long> detail : normalizedUsage.details().entrySet()) {
            audit.append("\t ").append(detail.getKey()).append(": ").append(detail.getValue()).append('\n');
        }
        audit.append("\t total_tokens: ").append(normalizedUsage.totalTokens()).append('\n');
        return Tools.safeLongToInt(normalizedUsage.totalTokens());
    }

    private void errorAdminLog(
        AssistantDefinitionEntity assistant,
        Pair<String, String> inputPair,
        String methodName,
        AiProviderException exception,
        Prop prop
    ) throws ProviderCallException {
        errorAdminLog(
            assistant,
            inputPair,
            methodName,
            new Pair<>(providerErrorMessage(exception, prop), exception.rawResponse())
        );
    }

    private void errorAdminLog(
        AssistantDefinitionEntity assistant,
        Pair<String, String> inputPair,
        String methodName,
        Pair<String, String> error
    ) throws ProviderCallException {
        StringBuilder audit = new StringBuilder("ERROR: ");
        audit.append(assistant.getName()).append(" (").append(assistant.getProvider()).append(") ");
        audit.append(methodName).append('\n');
        appendAssistantInfo(assistant, audit);
        audit.append("\nError message:\n").append(error.getFirst());
        audit.append("\nInput value:\n").append(auditValue(inputPair.getFirst()));
        if (Tools.isNotEmpty(inputPair.getSecond())) {
            audit.append("\nUser prompt:\n").append(auditValue(inputPair.getSecond()));
        }
        audit.append("\nFull response:\n").append(auditValue(error.getSecond()));
        Adminlog.add(Adminlog.TYPE_AI, audit.toString(), -1, -1);
        throw new ProviderCallException(error.getFirst());
    }

    private void appendAssistantInfo(AssistantDefinitionEntity assistant, StringBuilder audit) {
        audit.append("\nAssistant ID: ").append(assistant.getId()).append('\n');
        if (Tools.isNotEmpty(assistant.getName())) audit.append("Assistant name: ").append(assistant.getName()).append('\n');
        if (Tools.isNotEmpty(assistant.getProvider())) audit.append("Provider: ").append(assistant.getProvider()).append('\n');
        if (Tools.isNotEmpty(assistant.getModel())) audit.append("Model: ").append(assistant.getModel()).append('\n');
        if (Tools.isNotEmpty(assistant.getFieldFrom())) audit.append("Field from: ").append(assistant.getFieldFrom()).append('\n');
        if (Tools.isNotEmpty(assistant.getFieldTo())) audit.append("Field to: ").append(assistant.getFieldTo());
    }

    private String providerErrorMessage(AiProviderException exception, Prop prop) {
        String message = exception.getMessage();
        if (Tools.isEmpty(message)) message = prop.getText("html_area.insert_image.error_occured");
        String statusMarker = "(" + exception.statusCode() + ")";
        return exception.statusCode() > 0 && message.contains(statusMarker) == false
            ? statusMarker + " " + message
            : message;
    }

    private String auditValue(String value) {
        if (Tools.isEmpty(value)) return " - ";
        int maxLength = Math.max(configurationService.auditMaxLength(), 1000);
        return DB.prepareString(value, maxLength);
    }

    private static TokenUsage safeUsage(TokenUsage usage) {
        return usage == null ? TokenUsage.EMPTY : usage;
    }

    private String safeMessage(Exception exception) {
        return Tools.isEmpty(exception.getLocalizedMessage())
            ? exception.getClass().getSimpleName()
            : exception.getLocalizedMessage();
    }

    record GeneratedImageName(String fileName, TokenUsage usage) {
    }
}
