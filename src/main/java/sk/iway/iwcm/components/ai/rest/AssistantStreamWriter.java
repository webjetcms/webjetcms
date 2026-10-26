package sk.iway.iwcm.components.ai.rest;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;

/**
 * Frames generated text and the terminal response as newline-delimited JSON.
 * Buffers text until flush so the surrounding BufferedWriter cannot split a
 * Unicode surrogate pair between JSON messages at its internal buffer boundary.
 */
class AssistantStreamWriter extends Writer {

    private final Writer output;
    private final ObjectMapper mapper = new ObjectMapper();
    private final StringBuilder pending = new StringBuilder();

    AssistantStreamWriter(Writer output) {
        this.output = output;
    }

    @Override
    public void write(char[] chars, int offset, int length) {
        pending.append(chars, offset, length);
    }

    @Override
    public void flush() throws IOException {
        if (pending.length() > 0) {
            writeMessage(Map.of("type", "delta", "text", pending.toString()));
            pending.setLength(0);
        }
        output.flush();
    }

    /** Writes the terminal response after all pending text has been flushed. */
    void writeResult(AssistantResponseDTO response) throws IOException {
        flush();
        writeMessage(Map.of("type", "result", "result", response));
        output.flush();
    }

    @Override
    public void close() throws IOException {
        flush();
        output.close();
    }

    private void writeMessage(Map<String, ?> message) throws IOException {
        output.write(mapper.writeValueAsString(message));
        output.write('\n');
    }
}
