package sk.iway.iwcm.io.s3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

/** Verifies staged publication and cleanup without requiring an S3 server. */
class S3OutputStreamTest {
    /** Closing publishes an empty file exactly once and removes its staging file. */
    @Test
    void publishesEmptyFileOnce() throws Exception {
        S3Storage storage = mock(S3Storage.class);
        AtomicReference<Path> staged = new AtomicReference<>();
        doAnswer(call -> {
            Path path = call.getArgument(1);
            staged.set(path);
            assertEquals(0, Files.size(path));
            return null;
        }).when(storage).upload(eq("key"), any(Path.class));
        S3OutputStream output = new S3OutputStream(storage, "key");
        output.flush();
        assertFalse(output.isPublished());
        verifyNoInteractions(storage);
        output.close();
        output.close();
        assertTrue(output.isPublished());
        verify(storage, times(1)).upload(eq("key"), any(Path.class));
        assertFalse(Files.exists(staged.get()));
        assertThrows(IOException.class, () -> output.write(1));
    }

    /** A failed source copy can discard bytes without overwriting the destination. */
    @Test
    void abortDoesNotPublish() throws Exception {
        S3Storage storage = mock(S3Storage.class);
        S3OutputStream output = new S3OutputStream(storage, "key");
        output.write(new byte[] {1, 2, 3});
        output.abort();
        output.close();
        assertFalse(output.isPublished());
        verifyNoInteractions(storage);
    }

    /** A failed upload remains a failure and cannot be silently retried by a second close. */
    @Test
    void cleansStagingAfterUploadFailure() throws Exception {
        S3Storage storage = mock(S3Storage.class);
        AtomicReference<Path> staged = new AtomicReference<>();
        doAnswer(call -> {
            staged.set(call.getArgument(1));
            throw new IOException("offline");
        }).when(storage).upload(anyString(), any(Path.class));
        S3OutputStream output = new S3OutputStream(storage, "key");
        output.write(new byte[] {0, 1, 2, 3}, 1, 2);
        assertThrows(IOException.class, output::close);
        assertFalse(output.isPublished());
        assertFalse(Files.exists(staged.get()));
        output.close();
        verify(storage, times(1)).upload(anyString(), any(Path.class));
    }
}
