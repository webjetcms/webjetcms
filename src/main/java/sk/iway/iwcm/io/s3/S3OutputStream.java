package sk.iway.iwcm.io.s3;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stages a write locally and publishes it only after a successful close. */
public final class S3OutputStream extends OutputStream {
    private final S3Storage storage;
    private final String key;
    private final Path temporary;
    private final OutputStream output;
    private boolean closed;
    private boolean failed;

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

    /** Discards an incomplete copy, including when its input stream fails. */
    public void abort() throws IOException {
        failed = true;
        close();
    }

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
