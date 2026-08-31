package sk.iway.iwcm.components.ai.providers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;

/** Protects CMS INCLUDE commands while text is transformed by an AI provider. */
public class IncludesHandler {

    private static final Pattern INCLUDE_PATTERN = Pattern.compile("!INCLUDE\\([^)]+\\)!");
    private static final String TOKEN_PREFIX = "__WJ_AI_INCLUDE_";

    private final Map<String, String> includes;
    private final Map<String, Integer> occurrences = new LinkedHashMap<>();
    private final int maximumTokenLength;
    private final StringBuilder pending = new StringBuilder();
    private final StringBuilder wholeResponse = new StringBuilder();
    private boolean finished;

    private IncludesHandler(Map<String, String> includes) {
        this.includes = Map.copyOf(includes);
        includes.keySet().forEach(token -> occurrences.put(token, 0));
        maximumTokenLength = includes.keySet().stream().mapToInt(String::length).max().orElse(0);
    }

    public String getWholeResponse() {
        return wholeResponse.toString();
    }

    /**
     * Flushes any trailing fragment retained while checking a split protected token.
     *
     * @param writer destination for restored response content
     * @throws IOException if the restored content cannot be written
     */
    public void finish(BufferedWriter writer) throws IOException {
        if (finished) return;
        finished = true;
        write(writer, restoreKnownTokens(pending.toString()));
        pending.setLength(0);
        logValidation();
    }

    /**
     * Restores and writes the part of a streamed response that is safe to emit.
     *
     * @param line next streamed response fragment
     * @param writer destination for restored response content
     * @throws IOException if the restored content cannot be written
     * @throws IllegalStateException if the stream has already been finished
     */
    public void handleLine(String line, BufferedWriter writer) throws IOException {
        if (finished) throw new IllegalStateException("INCLUDE stream is already finished");
        if (line == null || line.isEmpty()) return;

        pending.append(line);
        int emitUntil = partialTokenStart();
        String available = pending.substring(0, emitUntil);
        pending.delete(0, emitUntil);
        write(writer, restoreKnownTokens(available));
    }

    /**
     * Protects all INCLUDE commands in the DTO input and returns their restoration state.
     *
     * When commands are present, their placeholder tokens replace the original input value in the supplied DTO.
     *
     * @param inputData input whose INCLUDE commands should be protected
     * @return handler containing the protected commands, or an empty handler when protection is unnecessary or fails
     */
    static IncludesHandler protectIncludes(InputDataDTO inputData) {
        if (inputData.isStructuredInput()) return empty();

        String inputText = inputData.getInputValue();
        if (inputText == null || inputText.isEmpty()) return empty();

        try {
            Matcher matcher = INCLUDE_PATTERN.matcher(inputText);
            if (matcher.find() == false) return empty();

            String requestPrefix = newRequestPrefix(inputText);
            Map<String, String> includes = new LinkedHashMap<>();
            StringBuffer protectedText = new StringBuffer();
            int index = 1;

            do {
                String token = requestPrefix + index++ + "__";
                includes.put(token, matcher.group());
                matcher.appendReplacement(protectedText, Matcher.quoteReplacement(token));
            } while (matcher.find());
            matcher.appendTail(protectedText);

            inputData.setInputValue(protectedText.toString());
            return new IncludesHandler(includes);
        } catch (RuntimeException ex) {
            Logger.debug(IncludesHandler.class, "Error while protecting !INCLUDE()! for AI assistant.", ex);
            return empty();
        }
    }

    /**
     * Restores INCLUDE commands and reports tokens lost or duplicated by the provider.
     *
     * @param value provider response containing protected tokens
     * @return response with all recognized tokens restored
     */
    String restoreIncludes(String value) {
        String restored = restoreKnownTokens(value);
        logValidation();
        return restored;
    }

    public boolean hasIncludes() {
        return includes.isEmpty() == false;
    }

    /**
     * Returns instructions requiring the provider to preserve all protected tokens.
     *
     * @return preservation instructions, or an empty string when no tokens are protected
     */
    String preservationInstructions() {
        if (includes.isEmpty()) return "";
        String example = includes.keySet().iterator().next();
        return "The input contains WebJET INCLUDE tokens such as " + example + ". "
            + "Copy every INCLUDE token exactly as provided. Do not translate, rewrite, expand, explain, "
            + "remove, or otherwise change it. The output must contain every INCLUDE token.";
    }

    /**
     * Restores recognized tokens while recording how often each token occurred.
     *
     * @param value response fragment containing protected tokens
     * @return fragment with all recognized tokens restored
     */
    private String restoreKnownTokens(String value) {
        if (value == null || value.isEmpty() || includes.isEmpty()) return value;

        String restored = value;
        for (Map.Entry<String, String> entry : includes.entrySet()) {
            int count = countOccurrences(value, entry.getKey());
            if (count > 0) occurrences.merge(entry.getKey(), count, Integer::sum);
            restored = restored.replace(entry.getKey(), entry.getValue());
        }
        return restored;
    }

    /**
     * Returns the start of a trailing fragment that may be an incomplete known token.
     *
     * @return index at which a possible incomplete token begins, or the pending length when none exists
     */
    private int partialTokenStart() {
        int maximumSuffixLength = Math.min(pending.length(), Math.max(0, maximumTokenLength - 1));
        for (int length = maximumSuffixLength; length > 0; length--) {
            int start = pending.length() - length;
            String suffix = pending.substring(start);
            for (String token : includes.keySet()) {
                if (suffix.length() < token.length() && token.startsWith(suffix)) return start;
            }
        }
        return pending.length();
    }

    private void write(BufferedWriter writer, String value) throws IOException {
        if (value != null && value.isEmpty() == false) {
            wholeResponse.append(value);
            writer.write(value);
        }
        writer.flush();
    }

    private void logValidation() {
        int missing = 0;
        int duplicated = 0;
        for (int count : occurrences.values()) {
            if (count == 0) missing++;
            if (count > 1) duplicated++;
        }
        if (missing == 0 && duplicated == 0) return;

        Logger.warn(
            IncludesHandler.class,
            "AI response changed protected INCLUDE tokens: missing=" + missing + ", duplicated=" + duplicated
        );
    }

    private static IncludesHandler empty() {
        return new IncludesHandler(Map.of());
    }

    private static String newRequestPrefix(String inputText) {
        String prefix;
        do {
            prefix = TOKEN_PREFIX + UUID.randomUUID().toString().replace("-", "") + "_";
        } while (inputText.contains(prefix));
        return prefix;
    }

    private static int countOccurrences(String value, String token) {
        int count = 0;
        int index = value.indexOf(token);
        while (index >= 0) {
            count++;
            index = value.indexOf(token, index + token.length());
        }
        return count;
    }
}
