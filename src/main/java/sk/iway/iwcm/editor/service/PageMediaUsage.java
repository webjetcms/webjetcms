package sk.iway.iwcm.editor.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.parser.Parser;
import org.springframework.jdbc.core.RowCallbackHandler;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.PlannedChangesService;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;

/**
 * Finds references to candidate files by their storage or public paths. Matches accumulate
 * within an instance, so page ownership and remaining usage are checked with separate instances.
 */
final class PageMediaUsage {
    private static final String PAGE_COLUMNS = "d.doc_id,d.virtual_path,d.external_link,d.data,d.html_data,d.html_head,d.perex_image,"
        + "d.field_a,d.field_b,d.field_c,d.field_d,d.field_e,d.field_f,d.field_g,d.field_h,d.field_i,d.field_j,"
        + "d.field_k,d.field_l,d.field_m,d.field_n,d.field_o,d.field_p,d.field_q,d.field_r,d.field_s,d.field_t";
    private static final String MEDIA_COLUMNS = "media_fk_id,media_fk_table_name,media_link,media_thumb_link,"
        + "media_info_sk,media_info_cz,media_info_en,media_info_de,field_a,field_b,field_c,field_d,field_e,field_f";
    private static final Set<String> TEXT_FILES = Set.of("html", "htm", "jsp", "jspf", "tag", "tld", "css", "scss",
        "less", "js", "mjs", "json", "xml", "svg", "properties", "ftl", "vm", "txt", "md", "yaml", "yml");
    private static final Pattern ESCAPED_BYTE = Pattern.compile("%[a-fA-F0-9]{2}");
    private static final Pattern EMBED_START = Pattern.compile("!INCLUDE\\((?:\\s|%20)*+/components/app-(docs|html)embed/embed\\.jsp(?:\\s|%20)*+,");
    private static final int MAX_URL_LENGTH = 8192;
    private static final int MAX_INCOMPLETE_EMBED_LENGTH = 8 * MAX_URL_LENGTH;
    private static final Pattern TOKEN = Pattern.compile("[^\\s<>\"'(),;={}]+");
    private static final Pattern QUOTED = Pattern.compile("\"([^\"<>\\r\\n]+)\"|'([^'<>\\r\\n]+)'");

    private final Map<String, Set<String>> files = new HashMap<>();
    private final Map<String, Set<String>> protectedPaths = new HashMap<>();
    private final Set<String> found = new HashSet<>();
    private final Set<String> names = new HashSet<>();
    private final int count;

    /**
     * Indexes the storage and public paths of the files to check. A reference to a folder
     * can protect its direct children when scanning content that will survive deletion.
     *
     * @param candidates virtual storage paths of the files considered for deletion
     * @param publicPaths public path for each candidate whose storage path contains a domain alias
     */
    PageMediaUsage(Set<String> candidates, Map<String, String> publicPaths) {
        count = candidates.size();
        for (String candidate : candidates) {
            for (String path : List.of(candidate, publicPaths.getOrDefault(candidate, candidate))) {
                files.computeIfAbsent(path, key -> new HashSet<>()).add(candidate);
                String parent = path.substring(0, path.lastIndexOf('/'));
                for (String reference : List.of(path, parent)) {
                    protectedPaths.computeIfAbsent(reference.toLowerCase(Locale.ROOT), key -> new HashSet<>()).add(candidate);
                    names.add(reference.substring(reference.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT));
                }
            }
        }
    }

    /**
     * Finds files referenced directly by the page or its attached media before deletion.
     * Relative URLs are resolved against the page's virtual path.
     *
     * @param id page ID to inspect
     * @param domain host used to match absolute URLs
     * @return accumulated matches as candidate storage paths
     * @throws SQLException if the page is missing or its content or media cannot be read
     */
    Set<String> referencedByPage(int id, String domain) throws SQLException {
        try (Connection connection = DBPool.getConnection()) {
            String base;
            try (PreparedStatement query = connection.prepareStatement("SELECT " + PAGE_COLUMNS + " FROM documents d WHERE d.doc_id=?")) {
                query.setInt(1, id);
                try (ResultSet rows = query.executeQuery()) {
                    if (!rows.next()) throw new SQLException("Missing page " + id);
                    base = rows.getString(2);
                    inspectColumns(rows, 3, base, domain, false);
                }
            }
            try (PreparedStatement query = connection.prepareStatement("SELECT " + MEDIA_COLUMNS + " FROM media WHERE media_fk_id=? AND media_fk_table_name=?")) {
                query.setInt(1, id);
                query.setString(2, "documents");
                try (ResultSet rows = query.executeQuery()) {
                    while (rows.next()) inspectColumns(rows, 3, base, domain, false);
                }
            }
        }
        return found;
    }

    /**
     * Finds candidates still referenced by pages, pending changes, component data or site source files.
     * A reference to a candidate's parent folder also preserves the file.
     *
     * @param deleted page IDs to exclude along with their page-specific records
     * @return accumulated candidate storage paths to preserve
     * @throws SQLException if a database source cannot be read
     * @throws IOException if a source directory or file cannot be scanned completely
     */
    Set<String> referencedElsewhere(Set<Integer> deleted) throws SQLException, IOException {
        try (Connection connection = DBPool.getConnection()) {
            scanPages(connection, "documents d", deleted);
            String yes = DB.getBooleanSql(true);
            String no = DB.getBooleanSql(false);
            // Match DocHistoryRepository.findPendingByGroupId, across all pages sharing this storage.
            String pending = "documents_history d WHERE (d.is_delete IS NULL OR d.is_delete=" + no + ") AND "
                + "(d.publicable=" + yes + " OR (d.awaiting_approve LIKE '_%') OR "
                + "(d.actual=" + no + " AND d.approved_by=-1 AND (d.disapproved_by IS NULL OR d.disapproved_by<=0) AND "
                + "d.history_id=(SELECT MAX(h.history_id) FROM documents_history h WHERE h.doc_id=d.doc_id)))";
            scanPages(connection, pending, deleted);
            scanRows(connection, MEDIA_COLUMNS + " FROM media", row -> {
                String table = row.getString(2);
                if (deleted.contains(row.getInt(1)) && ("documents".equals(table) || PlannedChangesService.PLANNED_MEDIA_TABLE.equals(table))) return;
                inspectColumns(row, 3, null, null, true);
            });
            scanRows(connection, "image_path,image_name FROM gallery", row -> {
                String directory = row.getString(1);
                String name = row.getString(2);
                if (directory == null || name == null) return;
                if (!directory.endsWith("/")) directory += "/";
                for (String prefix : List.of("", "s_", "o_")) matchPath(directory + prefix + name, true);
            });
            scanRows(connection, "doc_id,value_string FROM doc_atr", row -> {
                if (!deleted.contains(row.getInt(1))) inspect(row.getString(2), null, null, true);
            });
            for (String source : List.of(
                    "banner_location,banner_redirect,image_link,image_link_mobile,html_code,description_text,primary_link_url,secondary_link_url FROM banner_banners",
                    "image_url,question,option1,option2,option3,option4,option5,option6 FROM quiz_questions",
                    "description FROM quiz_results",
                    "question_text FROM inquiry",
                    "image_path,url,answer_text FROM inquiry_answers",
                    "html_head FROM groups",
                    "html_head FROM groups_scheduler WHERE (when_to_publish IS NOT NULL AND date_published IS NULL) OR awaiting_approve LIKE '_%'",
                    "description FROM calendar",
                    "question FROM document_forum",
                    "tip_text FROM tips_of_the_day",
                    "base_css_path,css,after_body_data FROM templates",
                    "seo_image FROM templates_group")) {
                if (found.size() == count) break;
                scanRows(connection, source, row -> inspectColumns(row, 1, null, null, true));
            }
        }
        Set<String> visited = new HashSet<>();
        for (String root : List.of("/templates", "/components/" + Constants.getInstallName(), "/files", "/images")) {
            scanSources(IwcmFile.fromVirtualPath(root), visited);
        }
        return found;
    }

    private void scanRows(Connection connection, String source, RowCallbackHandler inspectRow) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("SELECT " + source); ResultSet rows = query.executeQuery()) {
            while (found.size() < count && rows.next()) inspectRow.processRow(rows);
        }
    }

    private void scanPages(Connection connection, String from, Set<Integer> deleted) throws SQLException {
        if (found.size() == count) return;
        try (PreparedStatement query = connection.prepareStatement("SELECT " + PAGE_COLUMNS + " FROM " + from)) {
            query.setFetchSize(100);
            try (ResultSet rows = query.executeQuery()) {
                while (rows.next() && found.size() < count) {
                    if (deleted.contains(rows.getInt(1))) continue;
                    String base = rows.getString(2);
                    String link = rows.getString(3);
                    // The external link of a generated fulltext row points back to the indexed file.
                    boolean indexed = link != null && link.startsWith("/files/") && (link + ".html").equals(base);
                    inspectColumns(rows, indexed ? 4 : 3, base, null, true);
                }
            }
        }
    }

    private void inspectColumns(ResultSet rows, int first, String base, String domain, boolean protectFolders) throws SQLException {
        int end = rows.getMetaData().getColumnCount();
        for (int i = first; i <= end && found.size() < count; i++) inspect(rows.getString(i), base, domain, protectFolders);
    }

    /** A directory reached through several source roots is scanned once. */
    private void scanSources(IwcmFile directory, Set<String> visited) throws IOException {
        if (found.size() == count || !directory.exists()) return;
        if (!visited.add(new java.io.File(directory.getAbsolutePath()).getCanonicalPath())) return;
        IwcmFile[] children = directory.listFiles();
        if (children == null) throw new IOException("Cannot read " + directory.getVirtualPath());
        for (IwcmFile child : children) {
            if (found.size() == count) break;
            String name = child.getName();
            if (name.startsWith(".") || "node_modules".equals(name) || "CVS".equals(name)) continue;
            if (child.isDirectory()) scanSources(child, visited);
            else if (child.isFile()) scanSourceFile(child);
        }
    }

    private void scanSourceFile(IwcmFile file) throws IOException {
        String extension = FileTools.getFileExtension(file.getName());
        if (!TEXT_FILES.contains(extension)) return;
        String base = List.of("css", "scss", "less", "svg").contains(extension) ? file.getVirtualPath() : null;
        try (var input = BoundedInputStream.builder().setInputStream(new IwcmInputStream(file)).setPropagateClose(true).get();
                var reader = new InputStreamReader(input, Charset.forName(Constants.FILE_ENCODING))) {
            char[] buffer = new char[4 * MAX_URL_LENGTH];
            String tail = "";
            int length;
            while ((length = reader.read(buffer)) != -1) {
                String chunk = tail + new String(buffer, 0, length);
                inspect(chunk, base, null, true);
                if (found.size() == count) return;
                tail = sourceTail(chunk);
            }
            if (input.getCount() != file.length()) throw new IOException("Incomplete read of " + file.getVirtualPath());
        }
    }

    /** Keeps URLs and unfinished embed parameters that may continue in the next read. */
    private static String sourceTail(String chunk) throws IOException {
        int start = Math.max(0, chunk.length() - MAX_URL_LENGTH);
        // An encoded parameter can span more than the usual URL overlap.
        Matcher openEmbed = EMBED_START.matcher(chunk);
        if (openEmbed.find(Math.max(0, chunk.lastIndexOf(")!"))) && openEmbed.start() < start) {
            start = openEmbed.start();
            if (chunk.length() - start > MAX_INCOMPLETE_EMBED_LENGTH) throw new IOException("Embed parameters exceed scan limit");
        }
        return chunk.substring(start);
    }

    /**
     * Extracts candidate references from text, including quoted paths and encoded Document and HTML Embed parameters.
     *
     * @param value page field or source text to inspect
     * @param base virtual path used to resolve relative URLs, or {@code null} if unknown
     * @param domain host for absolute URLs, or {@code null} to consider any host
     * @param protectFolders whether to match case-insensitively and include direct children of referenced folders
     */
    private void inspect(String value, String base, String domain, boolean protectFolders) {
        if (value == null || value.isBlank()) return;
        Matcher embeds = EMBED_START.matcher(value);
        while (found.size() < count && embeds.find()) {
            int end = value.indexOf(")!", embeds.end());
            if (end < 0) break;
            String params = value.substring(embeds.end(), end).replace("%20", " ").replace("\r", "").replace("\n", "");
            boolean htmlEmbed = "html".equals(embeds.group(1));
            String encoded = new PageParams(params).getValue(htmlEmbed ? "html" : "url", "");
            String decoded = new String(Base64.decodeBase64(encoded), StandardCharsets.UTF_8);
            if (htmlEmbed) {
                if (decoded.startsWith("utf8:")) decoded = Tools.URLDecode(decoded.substring(5));
                inspect(decoded, base, domain, protectFolders);
            } else {
                // Document Embed prepends the site origin to local URLs, independent of the page path.
                inspectToken(decoded, null, domain, protectFolders);
            }
        }
        String text = value.indexOf('&') < 0 ? value : Parser.unescapeEntities(value, false);
        String lower = text.toLowerCase(Locale.ROOT);
        if (names.stream().noneMatch(lower::contains) && !ESCAPED_BYTE.matcher(text).find()) return;
        text = text.replace("\\/", "/");
        // Quoted URLs and standalone media fields can contain literal spaces.
        if (text.length() < 2048) inspectToken(text.trim(), base, domain, protectFolders);
        Matcher quoted = QUOTED.matcher(text);
        int start = 0;
        while (quoted.find() && found.size() < count) {
            inspectTokens(text.substring(start, quoted.start()), base, domain, protectFolders);
            String valueInQuotes = quoted.group(1) != null ? quoted.group(1) : quoted.group(2);
            inspectToken(valueInQuotes, base, domain, protectFolders);
            // CSS, srcset and embedded components can contain several references in one attribute.
            if (protectFolders || valueInQuotes.contains("(") || valueInQuotes.contains(",")) {
                inspectTokens(valueInQuotes, base, domain, protectFolders);
            }
            start = quoted.end();
        }
        if (found.size() < count) inspectTokens(text.substring(start), base, domain, protectFolders);
    }

    private void inspectTokens(String text, String base, String domain, boolean protectFolders) {
        Matcher tokens = TOKEN.matcher(text);
        while (tokens.find() && found.size() < count) inspectToken(tokens.group(), base, domain, protectFolders);
    }

    private void inspectToken(String token, String base, String domain, boolean protectFolders) {
        if (token.length() > MAX_URL_LENGTH || (token.indexOf('/') < 0 && token.indexOf('.') < 0 && token.indexOf('%') < 0 && !names.contains(token.toLowerCase(Locale.ROOT)))) return;
        try {
            int end = StringUtils.indexOfAny(token, '?', '#');
            String url = end < 0 ? token : token.substring(0, end);
            URI uri = URI.create(url.replace(" ", "%20"));
            if (uri.isOpaque() || (uri.getAuthority() != null && domain != null && !domain.equalsIgnoreCase(uri.getHost()))) return;
            if (uri.getRawPath() == null || uri.getRawPath().isEmpty()) return;
            if (!uri.getPath().startsWith("/")) {
                if (Tools.isEmpty(base) || !base.startsWith("/")) {
                    if (protectFolders && (token.contains("/") || token.contains("."))) {
                        // Components without a hosting page can resolve the same relative URL in different folders.
                        String name = uri.normalize().getPath();
                        if (name.endsWith("/")) name = name.substring(0, name.length() - 1);
                        String suffix = "/" + name.substring(name.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
                        protectedPaths.forEach((path, matches) -> {
                            if (path.endsWith(suffix)) found.addAll(matches);
                        });
                    }
                    return;
                }
                uri = URI.create(base.replace(" ", "%20")).resolve(uri);
            }
            String path = uri.normalize().getPath();
            if (path.startsWith("/thumb/")) path = path.substring(6);
            matchPath(path, protectFolders);
        } catch (IllegalArgumentException ignored) {
            // Most tokens in page content are text, not URLs.
        }
    }

    private void matchPath(String path, boolean protectFolders) {
        String key = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        Set<String> matches = protectFolders ? protectedPaths.get(key.toLowerCase(Locale.ROOT)) : files.get(key);
        if (matches != null) found.addAll(matches);
    }
}
