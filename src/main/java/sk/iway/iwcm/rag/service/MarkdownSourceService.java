package sk.iway.iwcm.rag.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.multidomain.DomainRequestBeanScope;

/** Resolves explicitly configured documentation roots in the web application or local filesystem. */
@Service
public class MarkdownSourceService {

    private static final String FILE_PREFIX = "file:";
    private static final String DOCUMENTATION_ALIAS = "file:/docs";
    private static final Set<String> NAVIGATION_FILES = Set.of("_sidebar.md", "_navbar.md", "_coverpage.md", "_footer.md", "_404.md", "404.md");
    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of("node_modules", "web-inf", "meta-inf");

    /**
     * Reads and normalizes the globally configured documentation roots.
     *
     * @return distinct roots in configuration order, or an empty list when none are configured
     */
    public List<String> getRoots() {
        try (DomainRequestBeanScope scope = DomainRequestBeanScope.open(null)) {
            String configured = Constants.getString("ragMarkdownFolders");
            if (configured == null || configured.isBlank()) return List.of();
            return Arrays.stream(configured.split("[,;\\r\\n]+"))
                .filter(value -> value.isBlank() == false)
                .map(this::normalizeRoot).distinct().toList();
        }
    }

    /**
     * Returns a normalized root only when it exactly matches the configured allowlist.
     *
     * @param folder requested documentation root
     * @return normalized configured root
     * @throws IllegalArgumentException if the root is missing, invalid, or not configured
     */
    public String requireRoot(String folder) {
        String root = normalizeRoot(folder);
        if (getRoots().contains(root) == false) throw new IllegalArgumentException("Documentation folder is not configured");
        return root;
    }

    /**
     * Returns an indexable path below a normalized configured root without requiring the source to exist.
     *
     * @param configuredRoot normalized configured root
     * @param fullPath full logical source path
     * @return eligible root-relative path, or {@code null} when outside the root or ineligible
     */
    public String getRelativePath(String configuredRoot, String fullPath) {
        if (configuredRoot == null || fullPath == null || fullPath.startsWith(configuredRoot + "/") == false) return null;
        String relativePath = fullPath.substring(configuredRoot.length() + 1);
        return isIndexable(relativePath) ? relativePath : null;
    }

    /**
     * Resolves an existing directory, confining virtual roots to the deployed web application.
     *
     * @param folder configured virtual or filesystem documentation root
     * @return canonical directory path
     * @throws IOException if the root cannot be resolved to an allowed existing directory
     */
    public Path resolveRoot(String folder) throws IOException {
        String root = requireRoot(folder);
        if (isDocumentationAlias(root)) return resolveDocumentationAlias(root);
        if (isFileSystemRoot(root)) {
            Path directory = Path.of(root.substring(FILE_PREFIX.length())).toRealPath();
            if (Files.isDirectory(directory) == false) throw new IOException("Documentation root must be a directory");
            return directory;
        }
        Path webRoot = Path.of(Tools.getRealPath("/")).toRealPath();
        Path directory = webRoot.resolve(root.substring(1)).toRealPath();
        if (directory.startsWith(webRoot) == false || Files.isDirectory(directory) == false) {
            throw new IOException("Documentation folder must be a directory inside the web application");
        }
        return directory;
    }

    /**
     * Resolves the documentation alias through the shared filesystem mapping before appending its subtree.
     *
     * @param root normalized {@code file:/docs} root or subtree
     * @return canonical directory inside the mapped documentation root
     * @throws IOException if the mapping is missing, invalid, inaccessible, or escaped by the subtree
     */
    private Path resolveDocumentationAlias(String root) throws IOException {
        Path mappedBase;
        try (DomainRequestBeanScope scope = DomainRequestBeanScope.open(null)) {
            mappedBase = Path.of(FileTools.symlinkReplaceToRootPath("/docs/"));
        } catch (InvalidPathException exception) {
            throw new IOException("The file:/docs alias requires an absolute /docs/ mapping in symlinkTranslate", exception);
        }
        if (mappedBase.isAbsolute() == false || mappedBase.normalize().equals(Path.of("/docs/"))) {
            throw new IOException("The file:/docs alias requires an absolute /docs/ mapping in symlinkTranslate");
        }
        Path docs = mappedBase.toRealPath();
        String relativePath = root.equals(DOCUMENTATION_ALIAS) ? "" : root.substring(DOCUMENTATION_ALIAS.length() + 1);
        Path directory = docs.resolve(relativePath).toRealPath();
        if (directory.startsWith(docs) == false || Files.isDirectory(directory) == false) {
            throw new IOException("Documentation folder must be a directory inside the mapped documentation");
        }
        return directory;
    }

    private boolean isDocumentationAlias(String root) {
        return root.equals(DOCUMENTATION_ALIAS) || root.startsWith(DOCUMENTATION_ALIAS + "/");
    }

    /**
     * Lists eligible language-classified Markdown files recursively without following symbolic links.
     *
     * @param folder configured documentation root
     * @return sorted root-relative source paths
     * @throws IOException if the root or its files cannot be scanned
     */
    public List<String> getFiles(String folder) throws IOException {
        return getFiles(folder, "", true);
    }

    /**
     * Lists selected eligible files while retaining paths relative to the configured root.
     *
     * @param folder configured documentation root
     * @param selectedDirectory root-relative directory, empty or null for the root
     * @param includeSubfolders whether to scan descendant directories
     * @return sorted root-relative source paths
     * @throws IOException if the selected directory or its files cannot be scanned
     */
    public List<String> getFiles(String folder, String selectedDirectory, boolean includeSubfolders) throws IOException {
        Path directory = resolveRoot(folder);
        Path selected = resolveDirectory(directory, requireDirectory(selectedDirectory));
        List<String> paths = new ArrayList<>();
        visitFiles(folder, directory, selected, includeSubfolders, (file, sourcePath) -> paths.add(sourcePath));
        paths.sort(String::compareTo);
        return paths;
    }

    /**
     * Traverses eligible sources for queue selection and indexing without following symbolic links.
     *
     * @param folder configured root used for language detection
     * @param directory canonical root directory used to derive relative paths
     * @param selected validated directory at which traversal starts
     * @param includeSubfolders whether to visit descendant directories
     * @param consumer callback receiving each eligible file and its root-relative path
     * @throws IOException if traversal, path confinement, or the callback fails
     */
    void visitFiles(String folder, Path directory, Path selected, boolean includeSubfolders, SourceFileConsumer consumer) throws IOException {
        Files.walkFileTree(selected, Set.of(), includeSubfolders ? Integer.MAX_VALUE : 1, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attributes) {
                return path.equals(directory) == false && isExcludedDirectory(path.getFileName().toString())
                    ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                String sourcePath = directory.relativize(file).toString().replace('\\', '/');
                if (attributes.isRegularFile() && detectLanguage(folder, sourcePath) != null) {
                    if (file.toRealPath().startsWith(directory) == false) throw new IOException("Markdown source escapes its root");
                    consumer.accept(file, sourcePath);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Processes an eligible Markdown file during a scan and may propagate I/O failures.
     */
    @FunctionalInterface
    interface SourceFileConsumer {
        /**
         * Processes one eligible source encountered during traversal.
         *
         * @param file filesystem path of the source
         * @param sourcePath source path relative to the configured root
         * @throws IOException if the source cannot be processed
         */
        void accept(Path file, String sourcePath) throws IOException;
    }

    /**
     * Lists immediate eligible child directories for lazy loading in the folder selector.
     *
     * @param folder configured documentation root
     * @param selectedDirectory root-relative directory, empty or null for the root
     * @return child nodes sorted by name, excluding symbolic links and excluded directories
     * @throws IOException if the selected directory or its children cannot be inspected
     */
    public List<DirectoryNode> getDirectories(String folder, String selectedDirectory) throws IOException {
        String root = requireRoot(folder);
        Path directory = resolveRoot(root);
        Path selected = resolveDirectory(directory, requireDirectory(selectedDirectory));
        List<DirectoryNode> children = new ArrayList<>();
        try (var entries = Files.newDirectoryStream(selected, this::isBrowsableDirectory)) {
            for (Path child : entries) {
                String relative = directory.relativize(child).toString().replace('\\', '/');
                boolean hasChildren;
                try (var descendants = Files.newDirectoryStream(child, this::isBrowsableDirectory)) {
                    hasChildren = descendants.iterator().hasNext();
                }
                children.add(new DirectoryNode(root, relative, child.getFileName().toString(), hasChildren));
            }
        }
        children.sort(java.util.Comparator.comparing(DirectoryNode::name));
        return children;
    }

    /**
     * Describes one directory node in the lazy-loaded Markdown folder selector.
     *
     * @param root normalized configured root
     * @param directory root-relative directory path
     * @param name directory display name
     * @param children whether eligible child directories exist
     */
    public record DirectoryNode(String root, String directory, String name, boolean children) {}

    private boolean isBrowsableDirectory(Path directory) {
        return Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) && isExcludedDirectory(directory.getFileName().toString()) == false;
    }

    /**
     * Validates a root-relative directory without requiring it to exist, allowing removal of deleted sources.
     *
     * @param directory root-relative directory, empty or null for the root
     * @return validated directory, or an empty string for the root
     * @throws IllegalArgumentException if the relative path is invalid or includes excluded segments
     */
    public String requireDirectory(String directory) {
        if (directory == null || directory.isEmpty()) return "";
        if (isValidRelativePath(directory) == false) throw new IllegalArgumentException("Invalid documentation subfolder");
        return directory;
    }

    /**
     * Matches a root-relative source against a selected directory without touching the filesystem.
     *
     * @param sourcePath root-relative source path
     * @param directory selected root-relative directory
     * @param includeSubfolders whether descendants also match
     * @return {@code true} when the source is indexable and lies in the selected scope
     */
    public boolean isInDirectory(String sourcePath, String directory, boolean includeSubfolders) {
        String selected = requireDirectory(directory);
        if (isIndexable(sourcePath) == false) return false;
        String prefix = selected.isEmpty() ? "" : selected + "/";
        if (sourcePath.startsWith(prefix) == false) return false;
        return includeSubfolders || sourcePath.indexOf('/', prefix.length()) < 0;
    }

    private Path resolveDirectory(Path root, String relativeDirectory) throws IOException {
        Path directory = resolvePath(root, relativeDirectory);
        if (Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS) == false) {
            throw new IOException("Documentation subfolder must be a directory inside its configured root");
        }
        return directory;
    }

    /**
     * Revalidates a queued source path and rejects symlinks, including links in intermediate directories.
     *
     * @param folder configured documentation root
     * @param sourcePath root-relative Markdown path
     * @return resolved path, which may refer to a deleted source
     * @throws IOException if the root is unavailable, a path contains a symlink, or confinement fails
     */
    public Path resolveFile(String folder, String sourcePath) throws IOException {
        if (isIndexable(sourcePath) == false) throw new IllegalArgumentException("Invalid Markdown source path");
        return resolvePath(resolveRoot(folder), sourcePath);
    }

    /**
     * Resolves a validated relative path without following links, also allowing deleted queue sources.
     *
     * @param root canonical documentation root
     * @param relativePath previously validated root-relative path
     * @return resolved path beneath the root
     * @throws IOException if a symbolic link is encountered or an existing path escapes the root
     */
    private Path resolvePath(Path root, String relativePath) throws IOException {
        Path path = root;
        for (String segment : relativePath.split("/")) {
            path = path.resolve(segment);
            if (Files.isSymbolicLink(path)) throw new IOException("Symbolic documentation paths are not supported");
        }
        if (Files.exists(path) && path.toRealPath().startsWith(root) == false) {
            throw new IOException("Markdown source escapes its root");
        }
        return path;
    }

    /**
     * Normalizes a documentation root and validates its virtual or filesystem path syntax.
     *
     * @param folder requested root including an optional {@code file:} prefix
     * @return normalized root without trailing slashes
     * @throws IllegalArgumentException if the root is missing or violates path restrictions
     */
    private String normalizeRoot(String folder) {
        if (folder == null) throw new IllegalArgumentException("Documentation folder is required");
        String root = folder.trim().replaceAll("/+$", "");
        if (isFileSystemRoot(root)) return normalizeFileSystemRoot(root);
        if (root.length() > 512 || root.matches("/[A-Za-z0-9_/-]+") == false || root.contains("//")) {
            throw new IllegalArgumentException("Invalid documentation folder");
        }
        for (String segment : root.substring(1).split("/")) {
            if (isExcludedDirectory(segment)) throw new IllegalArgumentException("Invalid documentation folder");
        }
        return root;
    }

    /**
     * Distinguishes explicit filesystem roots from application-relative paths without checking disk state.
     *
     * @param root logical root, possibly {@code null}
     * @return {@code true} when the root starts with {@code file:}
     */
    public boolean isFileSystemRoot(String root) {
        return root != null && root.startsWith(FILE_PREFIX);
    }

    /**
     * Normalizes a local filesystem root while rejecting network authorities and traversal segments.
     *
     * @param root root beginning with {@code file:}
     * @return normalized filesystem root
     * @throws IllegalArgumentException if the path is invalid, non-absolute, or too long
     */
    private String normalizeFileSystemRoot(String root) {
        String value = root.substring(FILE_PREFIX.length()).replace('\\', '/');
        // Accept the local file:/// spelling, but never a file://host network authority.
        if (value.startsWith("///")) value = value.substring(2);
        if (value.contains("//") || value.indexOf('?') >= 0 || value.indexOf('#') >= 0
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid filesystem documentation folder");
        }
        Path path = Path.of(value);
        if ((path.isAbsolute() == false && isDocumentationAlias(FILE_PREFIX + value) == false) || path.getNameCount() == 0) {
            throw new IllegalArgumentException("Filesystem documentation folder must be an absolute directory path");
        }
        for (Path segment : path) {
            if (segment.toString().equals(".") || segment.toString().equals("..")) {
                throw new IllegalArgumentException("Invalid filesystem documentation folder");
            }
        }
        String normalized = FILE_PREFIX + path.toString().replace('\\', '/');
        if (normalized.length() > 512) throw new IllegalArgumentException("Documentation folder is too long");
        return normalized;
    }

    public boolean isExcludedDirectory(String name) {
        return name.startsWith(".") || EXCLUDED_DIRECTORIES.contains(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns normalized shared language codes, including the standard documentation folder languages.
     *
     * @return configured language codes plus {@code sk}, {@code en}, and {@code cs}, without duplicates
     */
    public List<String> getLanguages() {
        try (DomainRequestBeanScope scope = DomainRequestBeanScope.open(null)) {
            Set<String> languages = new LinkedHashSet<>();
            String[] configured = Constants.getArray("languages");
            if (configured != null) {
                for (String language : configured) languages.add(requireLanguage(language));
            }
            languages.addAll(List.of("sk", "en", "cs"));
            return List.copyOf(languages);
        }
    }

    /**
     * Finds the nearest supported language directory in the configured root and root-relative source path.
     *
     * @param root documentation root
     * @param sourcePath root-relative Markdown path
     * @return nearest recognized language code, or {@code null} for an ineligible or unclassified source
     */
    public String detectLanguage(String root, String sourcePath) {
        if (isIndexable(sourcePath) == false) return null;
        String normalizedRoot = normalizeRoot(root);
        List<String> languages = getLanguages();
        String path = normalizedRoot + "/" + sourcePath;
        String[] directories = path.substring(0, path.lastIndexOf('/')).split("/");
        for (int i = directories.length - 1; i >= 0; i--) {
            String language = directories[i].toLowerCase(Locale.ROOT);
            if (languages.contains(language)) return language;
        }
        return null;
    }

    /**
     * Validates and normalizes a documentation language used for retrieval or chunk metadata.
     *
     * @param language language code with an optional regional suffix
     * @return trimmed lowercase language code
     * @throws IllegalArgumentException if the language code is missing or has invalid syntax
     */
    public String requireLanguage(String language) {
        String normalized = language == null ? "" : language.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 10 || normalized.matches("[a-z]{2,3}(?:-[a-z0-9]{2,6})?") == false) {
            throw new IllegalArgumentException("A valid documentation language is required");
        }
        return normalized;
    }

    public boolean isIndexable(String sourcePath) {
        if (isValidRelativePath(sourcePath) == false) return false;
        String file = sourcePath.substring(sourcePath.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        return file.endsWith(".md") && NAVIGATION_FILES.contains(file) == false;
    }

    /**
     * Applies the same path and directory restrictions to source files and folder selections.
     *
     * @param path root-relative path to validate
     * @return {@code true} when all segments are nonempty and allowed and the path meets length and character restrictions
     */
    private boolean isValidRelativePath(String path) {
        if (path == null || path.length() > 1024 || path.contains("\\")
                || path.matches("^[A-Za-z]:.*") || path.codePoints().anyMatch(Character::isISOControl)) return false;
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || isExcludedDirectory(segment)) return false;
        }
        return true;
    }

    /**
     * Builds a Docsify hash URL, or returns null for filesystem roots without a public web address.
     *
     * @param root documentation root
     * @param sourcePath indexable root-relative Markdown path
     * @return encoded context-relative viewer URL, or {@code null} for filesystem roots
     */
    public String getUrl(String root, String sourcePath) {
        if (isIndexable(sourcePath) == false) throw new IllegalArgumentException("Invalid Markdown source path");
        String normalizedRoot = normalizeRoot(root);
        if (isFileSystemRoot(normalizedRoot)) return null;
        String viewerRoot = findDocsifyRoot(normalizedRoot);
        // The configured root limits indexing; the viewer can be above it and needs the complete route.
        String viewerPath = (normalizedRoot + "/" + sourcePath).substring(viewerRoot.length() + 1);
        String route = viewerPath.substring(0, viewerPath.length() - 3);
        if (route.equals("README")) route = "";
        else if (route.endsWith("/README")) route = route.substring(0, route.length() - 6);
        String encoded = Arrays.stream(route.split("/", -1))
            .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20"))
            .collect(java.util.stream.Collectors.joining("/"));
        return viewerRoot + "/#/" + encoded;
    }

    /**
     * Finds the nearest deployed Docsify shell without mistaking a regular CMS index page for the viewer.
     *
     * @param indexedRoot normalized application-relative documentation root
     * @return nearest ancestor with a Docsify shell, or the indexed root when none can be found
     */
    private String findDocsifyRoot(String indexedRoot) {
        try (DomainRequestBeanScope ignored = DomainRequestBeanScope.open(null)) {
            Path webRoot = Path.of(Tools.getRealPath("/")).toRealPath();
            String candidate = indexedRoot;
            while (true) {
                Path directory = candidate.isEmpty() ? webRoot : webRoot.resolve(candidate.substring(1));
                for (String name : List.of("index.html", "index.jsp")) {
                    Path shell = directory.resolve(name);
                    if (Files.isRegularFile(shell) && shell.toRealPath().startsWith(webRoot)
                            && Files.readString(shell, StandardCharsets.UTF_8).contains("$docsify")) {
                        return candidate;
                    }
                }
                if (candidate.isEmpty()) break;
                candidate = candidate.substring(0, candidate.lastIndexOf('/'));
            }
        } catch (IOException e) {
            // Keep existing URLs for deployments whose viewer is served outside the local filesystem.
        }
        return indexedRoot;
    }
}
