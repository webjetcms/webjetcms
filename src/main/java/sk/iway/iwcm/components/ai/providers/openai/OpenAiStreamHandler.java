package sk.iway.iwcm.components.ai.providers.openai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.json.JSONArray;
import org.json.JSONObject;

public class OpenAiStreamHandler {

    private enum STREAM_STATUS {
        CREATED("thread.run.created"),
        DELTA("thread.message.delta"),
        COMPLETED("thread.run.step.completed"),
        DONE("done");

        private String value;

        STREAM_STATUS(String value) {
            this.value = value;
        }

        public String value() {
            return value;
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
                runId = new JSONObject(line).getString("id");
            }
            else if(actualStatus == STREAM_STATUS.DELTA) {
                JSONObject mainChunk = new JSONObject(line);
                JSONObject delta = mainChunk.getJSONObject("delta");
                JSONArray contentArray = delta.getJSONArray("content");

                for (int i = 0; i < contentArray.length(); i++) {
                    JSONObject item = contentArray.getJSONObject(i);
                    if ("text".equals(item.getString("type"))) {
                        JSONObject textObj = item.getJSONObject("text");
                        pushAnswePart(textObj.optString("value", null), writer);
                    }
                }
            }
            else if(actualStatus == STREAM_STATUS.COMPLETED) {
                usageChunk = new JSONObject(line);
            }
            else if(actualStatus == STREAM_STATUS.DONE) {
                return;
            }
        }
    }
}
