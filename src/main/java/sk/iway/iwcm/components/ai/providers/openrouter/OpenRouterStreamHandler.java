package sk.iway.iwcm.components.ai.providers.openrouter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.providers.IncludesHandler;

public class OpenRouterStreamHandler {

    boolean waitingForUsage = false;
    int totalTokenCount = 0;
    IncludesHandler includeHandler;

    public OpenRouterStreamHandler(Map<Integer, String> replacedIncludes) {
        this.includeHandler = new IncludesHandler(replacedIncludes, null);
    }

    public final JsonNode getUsageChunk() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        ObjectNode usage = mapper.createObjectNode();
        usage.put("total_tokens", totalTokenCount);

        root.set("usage", usage);
        return root;
    }

    public final void handleBufferedReader(BufferedReader reader, BufferedWriter writer) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if("".equals(line)) continue;

            if (line.startsWith("data: ")) {
                String data = line.substring(6);

                if("[DONE]".equals(data)) break;

                String content = "";
                String finishReason = "";

                JsonNode root = mapper.readTree(data);
                JsonNode dataNode = root.path("choices").get(0);

                JsonNode delta = dataNode.path("delta");
                if (!delta.isMissingNode() && delta.has("content"))
                    content = delta.path("content").asText();

                if(Tools.isEmpty(content) == false) includeHandler.handleLine(content, writer);

                JsonNode finishReasonNode = dataNode.path("finish_reason");
                if (!finishReasonNode.isMissingNode())
                    finishReason = finishReasonNode.asText();

                if("stop".equals(finishReason)) {
                    //Signal that now, we are waiting for usage
                    waitingForUsage = true;
                }

                if(waitingForUsage == true) {
                    JsonNode usage = root.path("usage");
                    if (usage.isMissingNode() || usage.isNull()) continue;
                    totalTokenCount = usage.path("total_tokens").asInt(0);
                }
            }
        }
    }

}
