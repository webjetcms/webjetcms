package sk.iway.iwcm.components.ai.security;

import java.util.List;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;

/**
 * WebJET adapter for the pure prompt-security implementation in {@code webjet-ai}.
 * Detection audit remains a CMS responsibility.
 */
public final class PromptInjectionDefense {

    private static final List<String> PROMPT_MACROS =
        List.of("{inputText}", "{userPrompt}", "{language}", "{userLanguage}");

    public enum UntrustedSource {
        INPUT_TEXT,
        USER_PROMPT
    }

    private PromptInjectionDefense() { }

    public static String hardenSystemInstructions(String instructions) {
        return com.webjetcms.ai.security.PromptInjectionDefense.hardenSystemInstructions(instructions);
    }

    public static String getSecurityInstructions(String instructions) {
        return com.webjetcms.ai.security.PromptInjectionDefense.getSecurityInstructions(instructions);
    }

    public static String getTaskInstructions(String instructions) {
        return com.webjetcms.ai.security.PromptInjectionDefense.getTaskInstructions(instructions);
    }

    public static void protectInputData(InputDataDTO inputData) {
        if (inputData == null) return;

        if (InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType()) == false) {
            inputData.setInputValue(getProtectedInputText(inputData));
        }
        inputData.setUserPrompt(getProtectedUserPrompt(inputData));
    }

    public static String getProtectedInputText(InputDataDTO inputData) {
        if (inputData == null || InputDataDTO.InputValueType.IMAGE.equals(inputData.getInputValueType())) return null;
        return protectUntrustedText(inputData.getInputValue(), UntrustedSource.INPUT_TEXT, inputData.getAssistantId());
    }

    public static String getProtectedUserPrompt(InputDataDTO inputData) {
        if (inputData == null) return null;
        return protectUntrustedText(inputData.getUserPrompt(), UntrustedSource.USER_PROMPT, inputData.getAssistantId());
    }

    public static String protectUntrustedText(String value, UntrustedSource source, Long assistantId) {
        com.webjetcms.ai.security.PromptInjectionDefense.UntrustedSource librarySource = toLibrarySource(source);
        boolean alreadyProtected = com.webjetcms.ai.security.PromptInjectionDefense.isProtectedUntrustedText(value, librarySource);
        com.webjetcms.ai.security.PromptInjectionDefense.ProtectionResult result =
            com.webjetcms.ai.security.PromptInjectionDefense.protectUntrustedText(value, librarySource);
        if (alreadyProtected == false) auditDetection(result, source, assistantId);
        return result.protectedText();
    }

    public static String wrapUntrustedText(String value, UntrustedSource source, Long assistantId) {
        com.webjetcms.ai.security.PromptInjectionDefense.ProtectionResult result =
            com.webjetcms.ai.security.PromptInjectionDefense.wrapUntrustedText(value, toLibrarySource(source));
        auditDetection(result, source, assistantId);
        return result.protectedText();
    }

    public static String neutralizePromptMacroTokens(String value) {
        if (value == null || value.isBlank()) return value;

        String safeValue = value;
        for (String macro : PROMPT_MACROS) {
            safeValue = safeValue.replace(
                macro,
                "PROMPT_MACRO(" + macro.substring(1, macro.length() - 1) + ")"
            );
        }
        return safeValue;
    }

    public static boolean isProtectedUntrustedText(String value, UntrustedSource source) {
        return com.webjetcms.ai.security.PromptInjectionDefense.isProtectedUntrustedText(value, toLibrarySource(source));
    }

    public static boolean containsPromptInjection(String value) {
        return com.webjetcms.ai.security.PromptInjectionDefense.containsPromptInjection(value);
    }

    public static String stripUnsafeCharacters(String value) {
        return com.webjetcms.ai.security.PromptInjectionDefense.stripUnsafeCharacters(value);
    }

    private static void auditDetection(
        com.webjetcms.ai.security.PromptInjectionDefense.ProtectionResult result,
        UntrustedSource source,
        Long assistantId
    ) {
        if (result.suspiciousContentDetected() == false) return;

        String message = "Detected possible prompt-injection patterns from source " + source.name()
            + " for assistant " + assistantId;
        Logger.warn(PromptInjectionDefense.class, message);
        Adminlog.add(Adminlog.TYPE_AI, message, null, null);
    }

    private static com.webjetcms.ai.security.PromptInjectionDefense.UntrustedSource toLibrarySource(
        UntrustedSource source
    ) {
        return com.webjetcms.ai.security.PromptInjectionDefense.UntrustedSource.valueOf(source.name());
    }

}
