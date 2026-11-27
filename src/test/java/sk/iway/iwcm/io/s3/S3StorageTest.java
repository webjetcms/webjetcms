package sk.iway.iwcm.io.s3;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;

/** Verifies the bounds and invalidation of metadata caches without a network dependency. */
class S3StorageTest {
    /** Repeated sibling lookups share a listing and explicit invalidation forces a new request. */
    @Test
    void sharesListingsUntilInvalidated() {
        MinioClient client = mock(MinioClient.class);
        when(client.listObjects(any(ListObjectsArgs.class))).thenReturn(List.of());
        S3Storage storage = new S3Storage(client, "autotest", 5000, 2);
        assertNull(storage.metadata("domain/files/one"));
        assertNull(storage.metadata("domain/files/two"));
        verify(client, times(1)).listObjects(any(ListObjectsArgs.class));
        storage.clearCache();
        storage.metadata("domain/files/one");
        verify(client, times(2)).listObjects(any(ListObjectsArgs.class));
    }

    /** The least recently used directory is evicted when the configured bound is reached. */
    @Test
    void evictsOldestDirectory() {
        MinioClient client = mock(MinioClient.class);
        when(client.listObjects(any(ListObjectsArgs.class))).thenReturn(List.of());
        S3Storage storage = new S3Storage(client, "autotest", 5000, 1);
        storage.list("first");
        storage.list("second");
        storage.list("first");
        verify(client, times(3)).listObjects(any(ListObjectsArgs.class));
    }

    /** A zero TTL immediately exposes external changes and failed listings are never cached. */
    @Test
    void disablingCacheAndRetryingFailures() {
        MinioClient client = mock(MinioClient.class);
        when(client.listObjects(any(ListObjectsArgs.class))).thenThrow(new IllegalStateException("offline"))
                .thenReturn(List.of());
        S3Storage storage = new S3Storage(client, "autotest", 0, 2);
        assertThrows(java.io.UncheckedIOException.class, () -> storage.list("files"));
        assertTrue(storage.list("files").isEmpty());
        assertTrue(storage.list("files").isEmpty());
        verify(client, times(3)).listObjects(any(ListObjectsArgs.class));
    }
}
