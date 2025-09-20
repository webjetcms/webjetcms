package sk.iway.iwcm.components.ai.providers.openai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.json.JSONObject;

/**
 * Handler for streaming responses from OpenAI API
 */
public class OpenAiStreamHandler {

    private enum STREAM_STATUS {
        CREATED("response.created"),
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
    private String runId = null;
    private JSONObject usageChunk;

    public final String getRunId() {
        return this.runId;
    }

    public final JSONObject getUsageChunk() {
        return this.usageChunk;
    }

    private void handleEvent(String line) {
        String event = line.substring(6).trim();

        if (event.equals(STREAM_STATUS.CREATED.value())) {
            actualStatus = STREAM_STATUS.CREATED;
        } else if (event.equals(STREAM_STATUS.DELTA.value())) {
            actualStatus = STREAM_STATUS.DELTA;
        } else if (event.equals(STREAM_STATUS.COMPLETED.value())) {
            actualStatus = STREAM_STATUS.COMPLETED;
        } else if (event.equals(STREAM_STATUS.DONE.value())) {
            actualStatus = STREAM_STATUS.DONE;
        } else {
            actualStatus = null;
        }
    }

    private void pushAnswePart(String answerPart, PrintWriter writer) {
        if(answerPart == null) return;
        writer.write(answerPart);
        writer.flush();
    }

    public final void handleBufferedReader(BufferedReader reader, PrintWriter writer) throws IOException {

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

            if(actualStatus == STREAM_STATUS.CREATED) {
                JSONObject json = new JSONObject(line);
                runId = json.getJSONObject("response").getString("id");
            }
            else if(actualStatus == STREAM_STATUS.DELTA) {
                JSONObject mainChunk = new JSONObject(line);
                    pushAnswePart(mainChunk.getString("delta"), writer);
            }
            else if(actualStatus == STREAM_STATUS.COMPLETED) {
                usageChunk = new JSONObject(line).getJSONObject("response");
            }
            else if(actualStatus == STREAM_STATUS.DONE) {
                return;
            }
        }
    }
}
