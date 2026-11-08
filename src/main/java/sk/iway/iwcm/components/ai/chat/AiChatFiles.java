package sk.iway.iwcm.components.ai.chat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.locks.ReentrantLock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/** Serializes updates across processes and replaces complete JSON files atomically. */
public final class AiChatFiles {
    private static final ObjectMapper MAPPER = new ObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final ReentrantLock[] LOCKS = new ReentrantLock[64];
    static {
        for (int i = 0; i < LOCKS.length; i++) LOCKS[i] = new ReentrantLock();
    }

    private AiChatFiles() { }

    @FunctionalInterface
    public interface Operation<T> { T apply(Path directory) throws IOException; }

    public static <T> T locked(Path root, int userId, Operation<T> operation) throws IOException {
        if (userId < 1) throw new IllegalArgumentException("Invalid chat owner");
        Path directory = root.resolve(Integer.toString(userId));
        // Serialize callers in this JVM before acquiring the OS lock shared with other processes.
        // FileChannel.lock() alone can reject overlapping locks from threads in the same JVM.
        ReentrantLock local = LOCKS[Math.floorMod(directory.hashCode(), LOCKS.length)];
        local.lock();
        try {
            directory(root);
            directory(directory);
            Path lockPath = directory.resolve(".lock");
            try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                 FileLock lock = channel.lock()) {
                cleanup(directory);
                return operation.apply(directory);
            }
        } finally {
            local.unlock();
        }
    }

    public static AiChatDocument read(Path path) throws IOException {
        try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            AiChatDocument document = MAPPER.readValue(input, AiChatDocument.class);
            if (document == null || document.getSchemaVersion() != 1 || document.getThread() == null
                || document.getMessages() == null || document.getMessages().stream().anyMatch(m -> m == null || m.getId() == null)) {
                throw new IOException("Invalid chat file");
            }
            return document;
        } catch (NoSuchFileException exception) {
            return null;
        }
    }

    public static void write(Path path, AiChatDocument document) throws IOException {
        cleanup(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), ".chat-", ".tmp");
        try {
            if (Files.getFileStore(temporary).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(temporary, PosixFilePermissions.fromString("rw-------"));
            }
            byte[] json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(document);
            try (FileChannel output = FileChannel.open(temporary, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer bytes = ByteBuffer.wrap(json);
                while (bytes.hasRemaining()) output.write(bytes);
                output.force(true);
            }
            // Readers must see either the previous complete transcript or the new one, never partially written JSON.
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void cleanup(Path directory) throws IOException {
        try (var unfinished = Files.newDirectoryStream(directory, ".chat-*.tmp")) {
            for (Path path : unfinished) Files.delete(path);
        }
    }

    /** Creates a private storage directory and rejects symbolic links in its resolved path. */
    public static void directory(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            Path parent = path.getParent();
            if (parent != null) directory(parent);
            try {
                Files.createDirectory(path);
                if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
                    Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"));
                }
            } catch (FileAlreadyExistsException exception) {
                // Another process may create this directory before taking the shared lock.
            }
        }
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Invalid chat storage directory");
        if (!path.toRealPath().equals(path.toAbsolutePath().normalize())) throw new IOException("Symbolic chat storage path");
    }
}
