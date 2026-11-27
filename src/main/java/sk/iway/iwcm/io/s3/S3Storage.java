package sk.iway.iwcm.io.s3;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.minio.CopyObjectArgs;
import io.minio.SourceObject;
import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.credentials.IamAwsProvider;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Item;
import sk.iway.iwcm.Constants;

/** S3 operations for CMS files. Keys already contain their captured domain namespace. */
public final class S3Storage {
    /**
     * Describes an object or directory returned by a nonrecursive S3 listing.
     *
     * @param key object key including its namespace, without a trailing slash for directories
     * @param directory whether the entry is a directory marker or implicit prefix
     * @param size object size in bytes, or zero for directories
     * @param modified modification time in epoch milliseconds, or zero for directories or unavailable timestamps
     * @param etag provider ETag, or an empty string for directories
     */
    public record Metadata(String key, boolean directory, long size, long modified, String etag) { }
    /**
     * Holds an immutable directory listing and its cache insertion time.
     *
     * @param loadedAt insertion time in epoch milliseconds
     * @param entries immediate children indexed by their full keys
     */
    private record Listing(long loadedAt, Map<String, Metadata> entries) { }

    private static volatile S3Storage instance;
    private final MinioClient client;
    private final String bucket;
    private final long ttlMillis;
    private final int maxDirectories;
    private final Map<String, Listing> cache = new LinkedHashMap<>(64, 0.75f, true);

    /**
     * Creates a storage client with bounded directory metadata caching.
     *
     * @param client configured S3 client
     * @param bucket existing bucket name
     * @param ttlMillis listing lifetime in milliseconds; nonpositive values disable caching
     * @param maxDirectories maximum cached directories; nonpositive values disable caching
     * @throws IllegalArgumentException if the bucket name is null or blank
     */
    public S3Storage(MinioClient client, String bucket, long ttlMillis, int maxDirectories) {
        if (bucket == null || bucket.isBlank()) throw new IllegalArgumentException("s3BucketName is required");
        this.client = client;
        this.bucket = bucket;
        this.ttlMillis = Math.max(0, ttlMillis);
        this.maxDirectories = Math.max(0, maxDirectories);
    }

    /**
     * Lazily initializes the shared client from WebJET's S3 configuration.
     *
     * @return client retained until {@link #reset()} is called
     * @throws IllegalArgumentException if required connection settings are missing or invalid
     */
    public static S3Storage getInstance() {
        S3Storage current = instance;
        if (current == null) {
            synchronized (S3Storage.class) {
                current = instance;
                if (current == null) {
                    String endpoint = Constants.getString("s3Url");
                    if (endpoint.isBlank()) throw new IllegalArgumentException("s3Url is required");
                    MinioClient.Builder builder = MinioClient.builder().endpoint(endpoint);
                    String region = Constants.getString("s3AwsRegion");
                    if (!region.isBlank()) builder.region(region);
                    if (Constants.getBoolean("s3AwsEnable")) {
                        builder.credentialsProvider(new IamAwsProvider(null, null));
                    } else {
                        builder.credentials(Constants.getString("s3AccessKey"), Constants.getString("s3SecretKey"));
                    }
                    MinioClient client = builder.build();
                    client.setTimeout(10000, 60000, 60000);
                    current = new S3Storage(client, Constants.getString("s3BucketName"),
                            Constants.getInt("s3MetaCacheTtlSec") * 1000L, Constants.getInt("s3MetaCacheMaxDirs"));
                    instance = current;
                }
            }
        }
        return current;
    }

    /** Discards the connection configuration; callers must first finish all active file operations. */
    public static synchronized void reset() {
        S3Storage previous = instance;
        instance = null;
        if (previous != null) {
            try { previous.client.close(); }
            catch (Exception ex) { sk.iway.iwcm.Logger.error(S3Storage.class, "Could not close S3 client", ex); }
        }
    }

    /**
     * Lists immediate children across all result pages, sharing cached listings between sibling lookups.
     * Failures are not cached; an object takes precedence over a directory prefix with the same name.
     *
     * @param key directory key without a trailing slash, or an empty string for the bucket root
     * @return immutable metadata map indexed by full child keys, excluding the directory itself
     * @throws UncheckedIOException if the S3 listing fails
     */
    public synchronized Map<String, Metadata> list(String key) {
        String prefix = key.isEmpty() ? "" : key + "/";
        Listing cached = cache.get(prefix);
        if (cached != null && System.currentTimeMillis() - cached.loadedAt() < ttlMillis) return cached.entries();
        Map<String, Metadata> entries = new LinkedHashMap<>();
        try {
            for (Result<Item> result : client.listObjects(ListObjectsArgs.builder().bucket(bucket)
                    .prefix(prefix).recursive(false).build())) {
                Item item = result.get();
                boolean directory = item.isDir() || item.objectName().endsWith("/");
                String name = stripSlash(item.objectName());
                if (name.equals(key)) continue;
                Metadata metadata = new Metadata(name, directory, directory ? 0 : item.size(),
                        directory || item.lastModified() == null ? 0 : item.lastModified().toInstant().toEpochMilli(),
                        directory ? "" : item.etag());
                // A real object wins over an implicit prefix with the same name.
                if (!directory || !entries.containsKey(name)) entries.put(name, metadata);
            }
        } catch (Exception ex) {
            throw failure("list", key, ex);
        }
        Map<String, Metadata> snapshot = Map.copyOf(entries);
        if (ttlMillis > 0 && maxDirectories > 0) {
            cache.put(prefix, new Listing(System.currentTimeMillis(), snapshot));
            while (cache.size() > maxDirectories) cache.remove(cache.keySet().iterator().next());
        }
        return snapshot;
    }

    public Metadata metadata(String key) {
        return list(parent(key)).get(key);
    }

    /**
     * Opens an entire object without treating connection or permission failures as a missing file.
     *
     * @param key full object key including its namespace
     * @return object stream that the caller must close
     * @throws FileNotFoundException if S3 reports {@code NoSuchKey}
     * @throws IOException if the object cannot be opened for another reason
     */
    public InputStream open(String key) throws IOException {
        return open(key, 0, null);
    }

    /**
     * Opens a byte range directly on the server, avoiding downloads of skipped bytes.
     *
     * @param key full object key including its namespace
     * @param offset zero-based byte offset; must be nonnegative
     * @param length number of bytes to request, or null to read through the end of the object
     * @return range stream that the caller must close
     * @throws FileNotFoundException if S3 reports {@code NoSuchKey}
     * @throws IOException if the range cannot be opened for another reason
     */
    public InputStream open(String key, long offset, Long length) throws IOException {
        try {
            GetObjectArgs.Builder args = GetObjectArgs.builder().bucket(bucket).object(key);
            if (offset > 0 || length != null) args.offset(offset);
            if (length != null) args.length(length);
            return client.getObject(args.build());
        } catch (ErrorResponseException ex) {
            if ("NoSuchKey".equals(ex.errorResponse().code())) throw new FileNotFoundException(key);
            throw failure("read", key, ex).getCause();
        } catch (Exception ex) {
            throw failure("read", key, ex).getCause();
        }
    }

    /**
     * Publishes a completed local staging file using bounded multipart buffers and invalidates cached listings.
     *
     * @param key destination object key including its namespace
     * @param staged local file containing the complete object; this method does not delete it
     * @throws IOException if the destination is a directory, an ancestor is a file, or reading or uploading fails
     * @throws UncheckedIOException if a metadata lookup fails
     */
    public void upload(String key, Path staged) throws IOException {
        Metadata existing = metadata(key);
        if (existing != null && existing.directory()) throw new IOException("Destination is a directory: " + key);
        validateAncestors(key);
        try (InputStream input = Files.newInputStream(staged)) {
            put(key, input, Files.size(staged), false);
        }
    }

    /**
     * Creates an empty object using a conditional write to avoid replacing an existing object.
     *
     * @param key destination object key including its namespace
     * @return true if created, or false if metadata already exists or S3 rejects the write precondition
     * @throws IOException if an ancestor is a file or the write fails for another reason
     * @throws UncheckedIOException if a metadata lookup fails
     */
    public boolean create(String key) throws IOException {
        if (metadata(key) != null) return false;
        validateAncestors(key);
        try {
            put(key, new ByteArrayInputStream(new byte[0]), 0, true);
            return true;
        } catch (IOException ex) {
            if (ex.getCause() instanceof ErrorResponseException response
                    && "PreconditionFailed".equals(response.errorResponse().code())) return false;
            throw ex;
        }
    }

    /**
     * Creates markers for the requested directory and missing ancestors, preserving empty directories in S3.
     * Markers already created remain if a later step fails.
     *
     * @param key directory key including its namespace, without a trailing slash
     * @return true if created, or false if the target already exists or an ancestor is a file
     * @throws UncheckedIOException if a metadata lookup or marker write fails
     */
    public boolean mkdirs(String key) {
        Metadata existing = metadata(key);
        if (existing != null) return false;
        StringBuilder path = new StringBuilder();
        try {
            for (String component : key.split("/")) {
                if (!path.isEmpty()) path.append('/');
                path.append(component);
                Metadata ancestor = metadata(path.toString());
                if (ancestor != null && !ancestor.directory()) return false;
                if (ancestor == null) put(path + "/", new ByteArrayInputStream(new byte[0]), 0, false);
            }
            return true;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Deletes a file or empty directory and invalidates cached listings.
     *
     * @param key file or directory key including its namespace, without a trailing slash
     * @return true after deletion, or false if the entry is missing or the directory is not empty
     * @throws UncheckedIOException if a lookup or deletion fails
     */
    public boolean delete(String key) {
        Metadata metadata = metadata(key);
        if (metadata == null) return false;
        if (metadata.directory() && !list(key).isEmpty()) return false;
        remove(metadata.directory() ? key + "/" : key);
        return true;
    }

    /**
     * Moves a file or subtree by copying all source objects before deleting them.
     * An existing destination file can be replaced; overlapping paths and existing directory targets are rejected.
     * The move is not atomic: failures can leave copied targets or partially deleted sources.
     *
     * @param source source key including its namespace, without a trailing slash
     * @param destination destination key including its namespace, without a trailing slash
     * @return true after a completed move or for an existing identical path; false for a missing source or conflict
     * @throws UncheckedIOException if lookup, copying, ancestor validation or source deletion fails
     */
    public boolean rename(String source, String destination) {
        Metadata metadata = metadata(source);
        if (metadata == null) return false;
        if (source.equals(destination)) return true;
        if (S3Path.within(destination, source) || S3Path.within(source, destination)) return false;
        Metadata target = metadata(destination);
        if (target != null && (metadata.directory() || target.directory())) return false;
        List<String> keys = new ArrayList<>();
        try {
            validateAncestors(destination);
            if (metadata.directory()) {
                for (Result<Item> result : client.listObjects(ListObjectsArgs.builder().bucket(bucket)
                        .prefix(source + "/").recursive(true).build())) keys.add(result.get().objectName());
            } else keys.add(source);
            for (String key : keys) {
                if (key.endsWith("/")) {
                    // Directory markers carry no file data; some S3 providers synthesize them in listings.
                    put(destination + key.substring(source.length()), new ByteArrayInputStream(new byte[0]), 0, false);
                    continue;
                }
                client.copyObject(CopyObjectArgs.builder().bucket(bucket)
                        .object(destination + key.substring(source.length()))
                        .source(SourceObject.builder().bucket(bucket).object(key).build()).build());
            }
            keys.sort(java.util.Comparator.reverseOrder());
            for (String key : keys) remove(key);
            return true;
        } catch (Exception ex) {
            throw failure("rename", source, ex);
        } finally {
            clearCache();
        }
    }

    public synchronized void clearCache() {
        cache.clear();
    }

    /**
     * Rejects destination paths whose ancestors are existing files.
     *
     * @param key destination object key
     * @throws IOException if an ancestor is a file
     * @throws UncheckedIOException if ancestor metadata cannot be read
     */
    private void validateAncestors(String key) throws IOException {
        String ancestor = parent(key);
        while (!ancestor.isEmpty()) {
            Metadata metadata = metadata(ancestor);
            if (metadata != null && !metadata.directory()) throw new IOException("Parent is a file: " + ancestor);
            ancestor = parent(ancestor);
        }
    }

    /**
     * Uploads a stream with an inferred content type and invalidates listings even after failure.
     *
     * @param key destination object or directory-marker key
     * @param input content stream owned by the caller
     * @param size content length in bytes
     * @param createOnly whether to require that the destination object does not exist
     * @throws IOException if the S3 write fails
     */
    private void put(String key, InputStream input, long size, boolean createOnly) throws IOException {
        String type = URLConnection.guessContentTypeFromName(key);
        try {
            PutObjectArgs.Builder args = PutObjectArgs.builder().bucket(bucket).object(key)
                    .stream(input, size, 8L * 1024 * 1024).contentType(type == null ? "application/octet-stream" : type);
            if (createOnly) args.headers(Map.of("If-None-Match", "*"));
            client.putObject(args.build());
        } catch (Exception ex) {
            throw failure("write", key, ex).getCause();
        } finally {
            clearCache();
        }
    }

    private void remove(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception ex) {
            throw failure("delete", key, ex);
        } finally {
            clearCache();
        }
    }

    private static String parent(String key) {
        int slash = key.lastIndexOf('/');
        return slash < 0 ? "" : key.substring(0, slash);
    }

    private static String stripSlash(String key) {
        return key.endsWith("/") ? key.substring(0, key.length() - 1) : key;
    }

    private static UncheckedIOException failure(String operation, String key, Exception cause) {
        return new UncheckedIOException(new IOException("S3 " + operation + " failed for " + key, cause));
    }
}
