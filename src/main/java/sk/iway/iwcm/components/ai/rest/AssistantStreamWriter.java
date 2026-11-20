package sk.iway.iwcm.components.ai.rest;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.CancellationException;

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
    private boolean finished;

    AssistantStreamWriter(Writer output) {
        this.output = output;
    }

    @Override
    public synchronized void write(char[] chars, int offset, int length) {
        if (!finished) pending.append(chars, offset, length);
    }

    @Override
    public synchronized void flush() throws IOException {
        if (pending.length() > 0) {
            writeMessage(Map.of("type", "delta", "text", pending.toString()));
            pending.setLength(0);
        }
        output.flush();
    }

    /** A saved-chat worker can deliver a late callback after the request has finished. */
    synchronized void writeDelta(String text) {
        if (finished) throw new CancellationException();
        try {
            pending.append(text);
            flush();
        } catch (IOException disconnected) {
            finished = true;
            throw new CancellationException();
        }
    }

    synchronized void writeResult(AssistantResponseDTO response) throws IOException {
        if (finished) return;
        finished = true;
        flush();
        writeMessage(Map.of("type", "result", "result", response));
        output.flush();
    }

    void writeMetadata(Map<String, ?> metadata) throws IOException {
        writeMessage(Map.of("type", "metadata", "metadata", metadata));
        output.flush();
    }

    @Override
    public synchronized void close() throws IOException {
        finished = true;
        flush();
        output.close();
    }

    private void writeMessage(Map<String, ?> message) throws IOException {
        output.write(mapper.writeValueAsString(message));
        output.write('\n');
    }
}
