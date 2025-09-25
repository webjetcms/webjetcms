package sk.iway.iwcm.components.ai.providers.openai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.components.ai.providers.IncludesHandler;

/**
 * Handler for streaming responses from OpenAI API
 */
public class OpenAiStreamHandler {

    private enum STREAM_STATUS {
        DELTA("response.output_text.delta"),
        COMPLETED("response.completed"),
        DONE("done");

        private String response;

        STREAM_STATUS(String response) {
            // Base response chat
            this.response = response;
        }

        public String value() {
            return this.response;
        }
    }

    private STREAM_STATUS actualStatus;
    private JsonNode usageChunk;
    private ObjectMapper mapper = new ObjectMapper();

    IncludesHandler includeHandler;

    public OpenAiStreamHandler(Map<Integer, String> replacedIncludes) {
        this.includeHandler = new IncludesHandler(replacedIncludes);
    }

    public final JsonNode getUsageChunk() {
        return this.usageChunk;
    }

    private void handleEvent(String line) {
        String event = line.substring(6).trim();

        if (event.equals(STREAM_STATUS.DELTA.value())) {
            actualStatus = STREAM_STATUS.DELTA;
        } else if (event.equals(STREAM_STATUS.COMPLETED.value())) {
            actualStatus = STREAM_STATUS.COMPLETED;
        } else if (event.equals(STREAM_STATUS.DONE.value())) {
            actualStatus = STREAM_STATUS.DONE;
        } else {
            actualStatus = null;
        }
    }

    private void pushAnswerPart(String answerPart, BufferedWriter writer) throws IOException{
        if(answerPart == null) return;
        includeHandler.handleLine(answerPart, writer);
    }

    public final void handleBufferedReader(BufferedReader reader, BufferedWriter writer) throws IOException {

        String line;
        while ((line = reader.readLine()) != null) {

            if(line.startsWith("event:")) {
                handleEvent(line);
                continue;
            }

            //Evenet we are not interested in (like some setup step for thred)
            if(actualStatus == null) continue;

            if(line.startsWith("data:") == true) {
                line = line.substring(6).trim();
            } else {
                continue;
            }

            if(actualStatus == STREAM_STATUS.DELTA) {
                JsonNode mainChunk = mapper.readTree(line);
                pushAnswerPart(mainChunk.path("delta").asText(null), writer);
            }
            else if(actualStatus == STREAM_STATUS.COMPLETED) {
                usageChunk = mapper.readTree(line).path("response");
            }
            else if(actualStatus == STREAM_STATUS.DONE) {
                return;
            }
        }
    }
}