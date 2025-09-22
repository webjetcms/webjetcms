package sk.iway.iwcm.components.ai.providers.gemini;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import lombok.Getter;
import sk.iway.iwcm.Tools;

/**
 * Handler for streaming responses from Gemini API
 */
@Getter
public class GeminiStreamHandler {

    boolean waitingForText = false;
    boolean waitingForUsage = false;

    Integer totalTokenCount = 0;
    Integer candidatesTokenCount = 0;
    Integer thoughtsTokenCount = 0;
    Integer promptTokenCount = 0;

    public final void handleBufferedReader(BufferedReader reader, BufferedWriter writer) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {

            line = line.stripLeading();

            if(skip(line)) continue;

            if(line.startsWith("\"candidates\":")) {
                waitingForText = true;
                continue;
            } else if(line.startsWith("\"usageMetadata\":")) {
                waitingForUsage = true;
                continue;
            }

            if(waitingForText == true) {
                pushAnswePart(line, writer);
            } else if(waitingForUsage == true) {
                setUsage(line);
            }
        }
    }

    private void setUsage(String line) {
        if(Tools.isEmpty(line) == true) return;

        String prefixA = "\"totalTokenCount\":";
        String prefixB = "\"candidatesTokenCount\":";
        String prefixC = "\"thoughtsTokenCount\":";
        String prefixD = "\"promptTokenCount\":";

        if(line.startsWith(prefixA)) {
            totalTokenCount = getUsage(line, prefixA);
        } else if(line.startsWith(prefixB)) {
            candidatesTokenCount = getUsage(line, prefixB);
        } else if(line.startsWith(prefixC)) {
            thoughtsTokenCount = getUsage(line, prefixC);
        } else if(line.startsWith(prefixD)) {
            promptTokenCount = getUsage(line, prefixD);
        }
    }

    private int getUsage(String line, String prefix) {
        line = line.substring(prefix.length());
        line = line.trim();
        if(line.endsWith(",")) line = line.substring(0, line.length() - 1);
        return Tools.getIntValue(line, 0);
    }

    private void pushAnswePart(String answerPart, BufferedWriter writer) throws IOException {
        if(answerPart == null) return;

        //Remove prefix
        String prefix = "\"text\": ";
        if(answerPart.startsWith(prefix))
            answerPart = answerPart.substring(prefix.length());
        else return;


        //Start and end allways have ""
        answerPart = answerPart.substring(1, answerPart.length() - 1);

        answerPart = escapeText(answerPart);

        for (char c : answerPart.toCharArray()) {
            writer.write(c);
            writer.flush();
            try {
                Thread.sleep(3); // 50ms delay per character
            } catch (InterruptedException ignored) {}
        }

        //Text was handled
        waitingForText = false;
    }

    private String escapeText(String input) {
        return
            input
                .replace("\\u003c", "<")
                .replace("\\u003e", ">")
                .replace("\\u0026", "&")
                .replace("\\n", "\n")
                .replace("\\t", "\t");
    }

    private boolean skip(String line) {
        if(Tools.isEmpty(line)) return true;
        List<String> listToSkip = List.of("{", "[", "]", "}");
        for(String toSkip : listToSkip)
            if(line.startsWith(toSkip)) return true;

        return false;
    }
}
