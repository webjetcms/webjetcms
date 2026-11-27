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
    /** Provider-neutral metadata; directory keys have no trailing slash. */
    public record Metadata(String key, boolean directory, long size, long modified, String etag) { }
    private record Listing(long loadedAt, Map<String, Metadata> entries) { }

    private static volatile S3Storage instance;
    private final MinioClient client;
    private final String bucket;
    private final long ttlMillis;
    private final int maxDirectories;
    private final Map<String, Listing> cache = new LinkedHashMap<>(64, 0.75f, true);

    /** Creates a storage client with bounded metadata caching. Also used by integration tests. */
    public S3Storage(MinioClient client, String bucket, long ttlMillis, int maxDirectories) {
        if (bucket == null || bucket.isBlank()) throw new IllegalArgumentException("s3BucketName is required");
        this.client = client;
        this.bucket = bucket;
        this.ttlMillis = Math.max(0, ttlMillis);
        this.maxDirectories = Math.max(0, maxDirectories);
    }

    /** Lazily initializes connection settings after WebJET has loaded its configuration. */
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

    /** Reads all pages, sharing a listing between sibling metadata lookups. Failures are never cached. */
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

    /** Opens an object without converting connection or permission failures into a missing file. */
    public InputStream open(String key) throws IOException {
        return open(key, 0, null);
    }

    /** Opens a byte range directly on the server, avoiding downloads of skipped bytes. */
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

    /** Publishes a completed local staging file using bounded multipart buffers. */
    public void upload(String key, Path staged) throws IOException {
        Metadata existing = metadata(key);
        if (existing != null && existing.directory()) throw new IOException("Destination is a directory: " + key);
        validateAncestors(key);
        try (InputStream input = Files.newInputStream(staged)) {
            put(key, input, Files.size(staged), false);
        }
    }

    /** Atomically creates an empty object when it does not already exist. */
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

    /** Creates markers for missing ancestors, preserving empty directories in S3. */
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

    /** Deletes a file or an empty directory, matching the java.io.File contract. */
    public boolean delete(String key) {
        Metadata metadata = metadata(key);
        if (metadata == null) return false;
        if (metadata.directory() && !list(key).isEmpty()) return false;
        remove(metadata.directory() ? key + "/" : key);
        return true;
    }

    /** Copies a subtree before removing any source objects. A failed deletion is reported. */
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

    private void validateAncestors(String key) throws IOException {
        String ancestor = parent(key);
        while (!ancestor.isEmpty()) {
            Metadata metadata = metadata(ancestor);
            if (metadata != null && !metadata.directory()) throw new IOException("Parent is a file: " + ancestor);
            ancestor = parent(ancestor);
        }
    }

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
