package sk.iway.iwcm.components.ai.providers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;

/**
 * Provides logic to hnalde INCLUDES in strings. To replace them with placeholders, then swap them back. Even when output is parse to line.
 */
public class IncludesHandler {

    private final Map<Integer, String> replacedIncludes;
    private final StringBuilder wholeResponse = new StringBuilder();

    private int state = 0;
    private boolean digits = false;
    private StringBuilder buffer = new StringBuilder();
    private static final String PREFIX = "__LOCK_";
    private static final Pattern LOCK_PATTERN = Pattern.compile("__LOCK_(\\d+)__");

    /**
     *
     * @param replacedIncludes
     */
    public IncludesHandler(Map<Integer, String> replacedIncludes) {
        this.replacedIncludes = replacedIncludes;
    }

    public String getWholeResponse() {
        return wholeResponse.toString();
    }

    /**
     * USing logic with buffer, method is trying find placeholder __LOCK_X__, swap them with includes and without waiting for whole text etite/flush parts.
     * @param line
     * @param writer
     * @throws IOException
     */
    public void handleLine(String line, BufferedWriter writer) throws IOException {
        for (char c : line.toCharArray()) {
            buffer.append(c);

            if (!digits) {
                // Matching the prefix first
                if (c == PREFIX.charAt(state)) {
                    state++;
                    if (state == PREFIX.length()) {
                        setStatus(0, true); // reset for suffix "__"
                    }
                } else {
                    // restart if mismatch
                    setStatus((c == PREFIX.charAt(0)) ? 1 : 0, false, writer);
                }
            } else {
                // digits + suffix "__"
                if (Character.isDigit(c)) {
                    // stay in digits section
                } else if (c == '_' && state < 2) {
                    state++;
                } else if (state == 2) {
                    // already saw "__" after digits
                    setStatus(0, false, writer);
                } else {
                    // reset if not digit or suffix
                    setStatus((c == PREFIX.charAt(0)) ? 1 : 0, false, writer);
                }
            }
        }

        // Check if ended exactly with suffix "__"
        if(digits && state == 2) {
            setStatus(0, false, writer);
        }
    }

    private void setStatus(int state, boolean digits)  {
        this.state = state;
        this.digits = digits;
    }

    private void setStatus(int state, boolean digits, BufferedWriter writer) throws IOException {
        this.state = state;
        this.digits = digits;

        Matcher m = LOCK_PATTERN.matcher(buffer);
        String stringToWrite = "";
        if(m.find()) {
            stringToWrite = IncludesHandler.returnIncludesToPlaceholders(buffer.toString(), replacedIncludes);
        } else {
            stringToWrite = buffer.toString();
        }
        wholeResponse.append(stringToWrite);

        writer.write(stringToWrite);
        writer.flush();

        buffer.setLength(0);
    }

    public static Map<Integer, String> replaceIncludesWithPlaceholders(InputDataDTO inputData) {
        Map<Integer, String> replacedIncludes = new HashMap<>();
        String inputText = inputData.getInputValue();
        if (Tools.isEmpty(inputText)) return replacedIncludes;

        // Find and replace all !INCLUDE()! with __INCLUDE_PLACEHOLDER_x value (x is number)
        String textToHandle = inputText;
        try {
            String regex = "(!INCLUDE\\([^)]+\\)!)";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(textToHandle);

            int findIncludes = 1;
            while (matcher.find()) {
                String replaceText = PREFIX + findIncludes + "__";
                replacedIncludes.put(findIncludes, matcher.group());
                textToHandle = textToHandle.replaceFirst(Pattern.quote(matcher.group()), Matcher.quoteReplacement(replaceText));
                findIncludes++;
            }

            inputData.setInputValue(textToHandle);
        } catch (Exception ex) {
            //Something went wrong, return original text
            Logger.debug(IncludesHandler.class, "Error while extracting !INLCUDE()! for AI assiatnt.", ex);
            replacedIncludes.clear();
        }
        return replacedIncludes;
    }

    public static final String addProtectedTokenInstructionRule(String instructions) {
        StringBuilder sb = new StringBuilder(instructions);
        sb.append("\n");
        sb.append("\"rule:\" ");
        sb.append("\"You will see placeholders such as __LOCK_1__, __LOCK_2__ , etc. These are protected tokens.");
        sb.append("Always copy them exactly as provided. Do not translate, rewrite, expand, explain, or change them in any way.");
        sb.append("Output must contain ALL the __LOCK_X__ placeholders.\"");
        return sb.toString();
    }

    public static final String returnIncludesToPlaceholders(String responseText, Map<Integer, String> replacedIncludes) {
        String textToHandle = responseText;
        try {
            for(Map.Entry<Integer, String> entry : replacedIncludes.entrySet()) {
                String replaceText = PREFIX + entry.getKey() + "__";
                textToHandle = textToHandle.replace(replaceText, Matcher.quoteReplacement(entry.getValue()));
            }
            return textToHandle;

        } catch (Exception ex) {
            //Something went wrong, return text with placeholders
            Logger.debug(IncludesHandler.class, "Error while returning !INLCUDE()! in AI assistant response.", ex);
            return responseText;
        }
    }
}