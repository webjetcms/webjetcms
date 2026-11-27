package sk.iway.iwcm.io.s3;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Stages object content in a temporary local file and uploads it during {@link #close()}.
 * A failed write or flush suppresses publication; callers must {@link #abort()} after a source read failure.
 */
public final class S3OutputStream extends OutputStream {
    private final S3Storage storage;
    private final String key;
    private final Path temporary;
    private final OutputStream output;
    private boolean closed;
    private boolean failed;

    /**
     * Opens a temporary file for a pending S3 upload without changing the destination object.
     *
     * @param storage destination storage client
     * @param key destination object key including the captured domain namespace
     * @throws IOException if the temporary file cannot be created or opened
     */
    public S3OutputStream(S3Storage storage, String key) throws IOException {
        this.storage = storage;
        this.key = key;
        temporary = Files.createTempFile("webjet-s3-", ".tmp");
        try {
            output = Files.newOutputStream(temporary);
        } catch (IOException ex) {
            Files.deleteIfExists(temporary);
            throw ex;
        }
    }

    @Override
    public void write(int value) throws IOException {
        write(new byte[] {(byte) value}, 0, 1);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
        ensureOpen();
        try {
            output.write(bytes, offset, length);
        } catch (IOException ex) {
            failed = true;
            throw ex;
        }
    }

    /**
     * Flushes the local staging stream without uploading the object.
     *
     * @throws IOException if the stream is closed or the local flush fails
     */
    @Override
    public void flush() throws IOException {
        ensureOpen();
        try {
            output.flush();
        } catch (IOException ex) {
            failed = true;
            throw ex;
        }
    }

    /**
     * Closes and discards a pending write, including after a failure in its source stream.
     * An object already published by an earlier close is unaffected.
     *
     * @throws IOException if closing the staging stream or deleting its temporary file fails
     */
    public void abort() throws IOException {
        failed = true;
        close();
    }

    /**
     * Uploads the staged content unless a write, flush or abort marked it as failed, then deletes the staging file.
     * Subsequent calls do nothing, including after an upload failure.
     *
     * @throws IOException if closing, uploading or deleting the staging file fails
     */
    @Override
    public void close() throws IOException {
        if (closed) return;
        closed = true;
        try {
            output.close();
            if (!failed) storage.upload(key, temporary);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) throw new IOException("Stream is closed");
    }
}
