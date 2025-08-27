package sk.iway.iwcm.components.ai.providers.openai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.json.JSONArray;
import org.json.JSONObject;

public class OpenAiStreamHandler {

    private enum STREAM_STATUS {
        CREATED("response.created", "thread.run.created"),
        DELTA("response.output_text.delta", "thread.message.delta"),
        COMPLETED("response.completed", "thread.run.step.completed"),
        DONE("done", "done");

        private String response;
        private String thread;

        STREAM_STATUS(String response, String thread) {
            // Base response chat
            this.response = response;
            // Chat using Assistants
            this.thread = thread;
        }

        public String value(boolean usingAssistants) {
            return usingAssistants == true ? this.thread : this.response;
        }
    }

    public OpenAiStreamHandler(boolean usingAssistants) {
        this.usingAssistants = usingAssistants;
    }

    private STREAM_STATUS actualStatus;
    private String runId = null;
    private JSONObject usageChunk;
    private boolean usingAssistants;

    public final String getRunId() {
        return this.runId;
    }

    public final JSONObject getUsageChunk() {
        return this.usageChunk;
    }

    private void handleEvent(String line) {
        String event = line.substring(6).trim();

        if (event.equals(STREAM_STATUS.CREATED.value(usingAssistants))) {
            actualStatus = STREAM_STATUS.CREATED;
        } else if (event.equals(STREAM_STATUS.DELTA.value(usingAssistants))) {
            actualStatus = STREAM_STATUS.DELTA;
        } else if (event.equals(STREAM_STATUS.COMPLETED.value(usingAssistants))) {
            actualStatus = STREAM_STATUS.COMPLETED;
        } else if (event.equals(STREAM_STATUS.DONE.value(usingAssistants))) {
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
                if(usingAssistants == true)
                    runId = json.getString("id");
                else
                    runId = json.getJSONObject("response").getString("id");
            }
            else if(actualStatus == STREAM_STATUS.DELTA) {
                JSONObject mainChunk = new JSONObject(line);

                if(usingAssistants == true) {
                    JSONObject delta = mainChunk.getJSONObject("delta");
                    JSONArray contentArray = delta.getJSONArray("content");

                    for (int i = 0; i < contentArray.length(); i++) {
                        JSONObject item = contentArray.getJSONObject(i);
                        if ("text".equals(item.getString("type"))) {
                            JSONObject textObj = item.getJSONObject("text");
                            pushAnswePart(textObj.optString("value", null), writer);
                        }
                    }
                } else {
                    pushAnswePart(mainChunk.getString("delta"), writer);
                }
            }
            else if(actualStatus == STREAM_STATUS.COMPLETED) {
                if(usingAssistants == true)
                    usageChunk = new JSONObject(line);
                else
                    usageChunk = new JSONObject(line).getJSONObject("response");
            }
            else if(actualStatus == STREAM_STATUS.DONE) {
                return;
            }
        }
    }
}
