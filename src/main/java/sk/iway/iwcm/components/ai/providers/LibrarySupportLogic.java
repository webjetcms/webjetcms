package sk.iway.iwcm.components.ai.providers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiOperation;
import com.webjetcms.ai.AiProviderConfig;
import com.webjetcms.ai.AiProviderException;
import com.webjetcms.ai.AiRequest;
import com.webjetcms.ai.AiResponse;
import com.webjetcms.ai.BinaryContent;
import com.webjetcms.ai.GeneratedMedia;
import com.webjetcms.ai.ImageOptions;
import com.webjetcms.ai.ModelInfo;
import com.webjetcms.ai.TokenUsage;

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
        return configurationService.isConfigured(getProviderId());
    }

    @Override
    public List<LabelValue> getSupportedModels(Prop prop, HttpServletRequest request) {
        List<LabelValue> values = new ArrayList<>();
        try {
            AiProviderConfig config = configurationService.resolve(getProviderId(), request);
            for (ModelInfo model : aiClient.listModels(getProviderId(), config)) {
                String label = isBlank(model.displayName()) ? model.id() : model.displayName();
                values.add(new LabelValue(label, model.id()));
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
            Map<Integer, String> replacedIncludes = IncludesHandler.replaceIncludesWithPlaceholders(inputData);
            String instructions = prepareInstructions(assistant, inputData, replacedIncludes);
            AiResponse response = aiClient.execute(
                getProviderId(),
                buildRequest(AiOperation.TEXT, assistant, inputData, instructions),
                configurationService.resolve(getProviderId(), request)
            );

            String responseText = response.text();
            responseDto.setResponse(replacedIncludes.isEmpty()
                ? responseText
                : IncludesHandler.returnIncludesToPlaceholders(responseText, replacedIncludes));
            successAdminLog(
                responseDto,
                inputPair,
                assistant,
                METHOD_TEXT_RESPONSE,
                responseDto.getResponse(),
                response.usage(),
                0,
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
            Map<Integer, String> replacedIncludes = IncludesHandler.replaceIncludesWithPlaceholders(inputData);
            String instructions = prepareInstructions(assistant, inputData, replacedIncludes);
            IncludesHandler includeHandler = new IncludesHandler(replacedIncludes);

            AiResponse response = aiClient.stream(
                getProviderId(),
                buildRequest(AiOperation.TEXT, assistant, inputData, instructions),
                configurationService.resolve(getProviderId(), request),
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
                0,
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
            Pair<String, Integer> generatedFileName = getGeneratedImageName(assistant, inputData, prop, statRepo, request);
            responseDto.setGeneratedFileName(generatedFileName.getFirst());

            String instructions = prepareInstructions(assistant, inputData, null);
            AiOperation operation = imageOperation(inputData);
            AiResponse response = aiClient.execute(
                getProviderId(),
                buildRequest(operation, assistant, inputData, instructions),
                configurationService.resolve(getProviderId(), request)
            );

            long datePart = Tools.getNow();
            for (GeneratedMedia image : response.media()) {
                try {
                    String format = imageExtension(image.mediaType());
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

            successAdminLog(
                responseDto,
                inputPair,
                assistant,
                METHOD_IMAGE_RESPONSE,
                IMAGE_AUDIT_RESPONSE,
                response.usage(),
                generatedFileName.getSecond(),
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

    private String prepareInstructions(
        AssistantDefinitionEntity assistant,
        InputDataDTO inputData,
        Map<Integer, String> replacedIncludes
    ) {
        String instructions = AiAssistantsService.executePromptMacro(
            assistant.getInstructions(),
            inputData,
            replacedIncludes
        );
        PromptInjectionDefense.protectInputData(inputData);
        return PromptInjectionDefense.hardenSystemInstructions(instructions);
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
        String instructions
    ) throws IOException {
        AiRequest.Builder builder = AiRequest.builder()
            .operation(operation)
            .model(assistant.getModel())
            .instructions(instructions)
            .userPrompt(inputData.getUserPrompt())
            .store(Tools.isTrue(assistant.getUseTemporal()) == false)
            .imageOptions(new ImageOptions(
                inputData.getImageCount(),
                inputData.getImageSize(),
                inputData.getImageQuality()
            ));

        if (InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType())) {
            if (inputData.getInputFile() == null) return builder.build();
            builder.inputMedia(new BinaryContent(
                Files.readAllBytes(inputData.getInputFile().toPath()),
                inputData.getMimeType(),
                inputData.getInputFile().getName()
            ));
        } else {
            builder.inputText(inputData.getInputValue());
        }
        return builder.build();
    }

    private Pair<String, Integer> getGeneratedImageName(
        AssistantDefinitionEntity assistant,
        InputDataDTO inputData,
        Prop prop,
        AiStatRepository statRepo,
        HttpServletRequest request
    ) {
        String defaultFileName = "ai_generated_image_" + System.currentTimeMillis();
        if (SupportedActions.GENERATE_IMAGE.getAction().equals(assistant.getAction()) == false) {
            return new Pair<>(defaultFileName, 0);
        }

        Pair<String, String> instructionValuePair = getImageNameInstructions(assistant, inputData);
        if (instructionValuePair == null) return new Pair<>(defaultFileName, 0);

        AssistantDefinitionEntity nameAssistant = new AssistantDefinitionEntity();
        nameAssistant.setId(-1L);
        nameAssistant.setName("Image name generator");
        nameAssistant.setProvider(assistant.getProvider());
        nameAssistant.setModel(configurationService.imageNameModel(getProviderId()));
        nameAssistant.setUseStreaming(false);
        nameAssistant.setInstructions(instructionValuePair.getFirst());
        nameAssistant.setUseTemporal(true);

        InputDataDTO nameInput = new InputDataDTO();
        nameInput.setInputValueType(InputDataDTO.InputValueType.TEXT);
        nameInput.setInputValue(instructionValuePair.getSecond());

        try {
            AssistantResponseDTO response = getAiResponse(nameAssistant, nameInput, prop, statRepo, request);
            if (Tools.isNotEmpty(response.getError()) || Tools.isEmpty(response.getResponse())) {
                return new Pair<>(defaultFileName, 0);
            }
            return new Pair<>(response.getResponse(), response.getTotalTokens());
        } catch (Exception exception) {
            Logger.error(LibrarySupportLogic.class, "Error processing image name " + exception.getMessage());
            return new Pair<>(defaultFileName, 0);
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
                    AiAssistantsService.executePromptMacro(assistant.getInstructions(), inputData, null),
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
        int additionalTokens,
        AiStatRepository statRepo,
        HttpServletRequest request
    ) {
        StringBuilder audit = new StringBuilder("SUCCESS: ");
        audit.append(assistant.getName()).append(" (").append(assistant.getProvider()).append(") ");
        audit.append(methodName).append('\n');
        appendAssistantInfo(assistant, audit);

        audit.append("\nAction cost:\n");
        int totalTokens = appendUsageAndReturnTotal(audit, usage, additionalTokens);

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

    private int appendUsageAndReturnTotal(StringBuilder audit, TokenUsage usage, int additionalTokens) {
        TokenUsage safeUsage = usage == null ? TokenUsage.EMPTY : usage;
        long total = safeUsage.totalTokens() + additionalTokens;
        audit.append("\t input_tokens: ").append(safeUsage.inputTokens()).append('\n');
        audit.append("\t output_tokens: ").append(safeUsage.outputTokens()).append('\n');
        for (Map.Entry<String, Long> detail : safeUsage.details().entrySet()) {
            audit.append("\t ").append(detail.getKey()).append(": ").append(detail.getValue()).append('\n');
        }
        audit.append("\t total_tokens: ").append(total).append('\n');
        return Tools.safeLongToInt(total);
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

    private String imageExtension(String mediaType) {
        String value = mediaType == null ? "png" : mediaType.trim().toLowerCase();
        int separator = value.indexOf(';');
        if (separator >= 0) value = value.substring(0, separator);
        if (value.startsWith("image/")) value = value.substring("image/".length());
        if ("jpeg".equals(value)) value = "jpg";
        return value.startsWith(".") ? value : "." + value;
    }

    private String safeMessage(Exception exception) {
        return Tools.isEmpty(exception.getLocalizedMessage())
            ? exception.getClass().getSimpleName()
            : exception.getLocalizedMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
