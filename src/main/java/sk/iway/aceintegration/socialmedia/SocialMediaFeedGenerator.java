package sk.iway.aceintegration.socialmedia;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.BreakIterator;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.translation.TranslationService;

/**
 * Publishes the central WebJET news feeds from public social posts.
 * Register {@link #main(String[])} manually as a WebJET cron task on the documentation server.
 * This integration and its credentials are not part of the customer distribution.
 */
public final class SocialMediaFeedGenerator {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> LANGUAGES = List.of("sk", "cs", "en");
    private static final List<String> SOURCES = List.of("youtube", "facebook");
    private static final String ATOM = "http://www.w3.org/2005/Atom";
    private static final String YOUTUBE = "http://www.youtube.com/xml/schemas/2015";
    private static final String MEDIA = "http://search.yahoo.com/mrss/";
    private static final int ITEMS_PER_SOURCE = 3;
    private static final int YOUTUBE_FALLBACK_ITEMS = 4;
    private static final int MAX_FEED_BYTES = 2 * 1024 * 1024;
    private static final int MAX_PUBLIC_FEED_BYTES = 256 * 1024;
    private static final int MAX_IMAGE_BYTES = 3 * 1024 * 1024;
    private static final int MAX_PEREX_LENGTH = 180;
    private static final int MAX_FACEBOOK_TITLE_LENGTH = 120;
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private final Settings settings;
    private final Downloader downloader;
    private final Translator translator;

    SocialMediaFeedGenerator(Settings settings, Downloader downloader, Translator translator) {
        this.settings = settings;
        this.downloader = downloader;
        this.translator = translator;
    }

    /**
     * Runs the generator with server configuration; no cron arguments or automatic registration are needed.
     *
     * @param args unused cron arguments; credentials must be stored in server configuration
     */
    public static void main(String[] args) {
        if (!RUNNING.compareAndSet(false, true)) {
            Logger.warn(SocialMediaFeedGenerator.class, "Social media feed generation is already running");
            return;
        }
        try (SocialMediaHttpClient client = new SocialMediaHttpClient()) {
            Map<String, TranslationService> services = new HashMap<>();
            Translator translator = (text, language) -> {
                TranslationService service = services.computeIfAbsent(language, key -> new TranslationService("sk", key));
                return service.getTranslationEngine() == null ? text : service.translate(text);
            };
            new SocialMediaFeedGenerator(Settings.fromConfiguration(), client, translator).generate();
            Logger.println(SocialMediaFeedGenerator.class, "Social media news feeds generated successfully");
        } catch (Exception exception) {
            Logger.error(SocialMediaFeedGenerator.class, "Social media feed generation failed ("
                + SocialMediaFeedException.safeSummary(exception) + "); existing published files remain valid");
        } finally {
            RUNNING.set(false);
        }
    }

    /** Generates all languages, retaining a failed provider's last successful posts. */
    void generate() throws IOException {
        FeedCache previous = readCache();
        Map<String, List<Post>> sources = new LinkedHashMap<>();
        boolean refreshed = false;
        for (String source : SOURCES) {
            List<Post> oldPosts = previous.sources().getOrDefault(source, List.of());
            try {
                List<Post> fetched = "youtube".equals(source) ? fetchYoutube() : fetchFacebook();
                List<Post> posts = new ArrayList<>();
                for (Post post : fetched) {
                    Post old = oldPosts.stream().filter(item -> item.id().equals(post.id())).findFirst().orElse(null);
                    posts.add(cacheImage(post, old));
                }
                sources.put(source, posts);
                refreshed = true;
            } catch (Exception exception) {
                sources.put(source, oldPosts);
                Logger.warn(SocialMediaFeedGenerator.class, "Unable to refresh " + source
                    + "; retaining " + oldPosts.size() + " cached posts (" + SocialMediaFeedException.safeSummary(exception) + ")");
            }
        }
        int youtubeLimit = sources.get("facebook").isEmpty() ? YOUTUBE_FALLBACK_ITEMS : ITEMS_PER_SOURCE;
        List<Post> posts = sources.entrySet().stream()
            .flatMap(source -> source.getValue().stream().limit("youtube".equals(source.getKey()) ? youtubeLimit : ITEMS_PER_SOURCE))
            .sorted(Comparator.comparing((Post post) -> Instant.parse(post.publishedAt())).reversed().thenComparing(Post::id)).toList();
        if (!refreshed && posts.isEmpty()) throw new SocialMediaFeedException(SocialMediaFeedException.Reason.NO_AVAILABLE_POSTS);
        Map<String, Translation> translations = retainTranslations(sources, previous.translations());
        Map<Path, byte[]> files = new LinkedHashMap<>();
        for (String language : LANGUAGES) {
            ObjectNode feed = MAPPER.createObjectNode();
            ArrayNode news = MAPPER.createArrayNode();
            for (Post post : posts) news.add(toNews(post, language, previous.translations(), translations));
            feed.set("news", news);
            byte[] publicFeed = serialize(feed);
            if (publicFeed.length > MAX_PUBLIC_FEED_BYTES) throw new IOException("Public feed exceeds the dashboard response limit");
            files.put(settings.outputDirectory().resolve("wjnews." + language + ".json"), publicFeed);
        }
        files.put(settings.cacheFile(), serialize(new FeedCache(sources, translations)));
        publishFiles(files);
    }

    /** Keeps translations for cached posts, including the fourth video temporarily hidden by Facebook posts. */
    private static Map<String, Translation> retainTranslations(Map<String, List<Post>> sources, Map<String, Translation> previous) {
        Map<String, Translation> retained = new LinkedHashMap<>();
        for (Post post : sources.values().stream().flatMap(List::stream).toList()) {
            for (String suffix : List.of(":cs:title", ":cs:perex", ":en:title", ":en:perex")) {
                String key = post.id() + suffix;
                Translation translation = previous.get(key);
                if (translation != null) retained.put(key, translation);
            }
        }
        return retained;
    }

    private FeedCache readCache() throws IOException {
        if (!Files.exists(settings.cacheFile())) return new FeedCache(Map.of(), Map.of());
        FeedCache cache = MAPPER.treeToValue(readJson(settings.cacheFile()), FeedCache.class);
        if (cache == null || cache.sources() == null || cache.translations() == null) throw new IOException("Invalid feed cache");
        Map<String, List<Post>> sources = new LinkedHashMap<>();
        for (String source : SOURCES) {
            List<Post> posts = cache.sources().getOrDefault(source, List.of());
            if (posts == null || posts.size() > ("youtube".equals(source) ? YOUTUBE_FALLBACK_ITEMS : ITEMS_PER_SOURCE)) {
                throw new IOException("Invalid cached source");
            }
            for (Post post : posts) {
                if (post == null || !source.equals(post.source()) || post.id() == null
                    || post.title() == null || post.perex() == null || !isPostLink(post.link(), source)
                    || parseDate(post.publishedAt()) == null) throw new IOException("Invalid cached post");
            }
            sources.put(source, posts.stream().map(post -> withImage(post, ownImage(post.image()))).toList());
        }
        return new FeedCache(sources, cache.translations());
    }

    private List<Post> fetchYoutube() throws Exception {
        URI uri = URI.create("https://www.youtube.com/feeds/videos.xml?channel_id=" + settings.youtubeChannelId());
        return parseYoutube(downloader.download(uri, null, MAX_FEED_BYTES), settings.youtubeChannelId());
    }

    private List<Post> fetchFacebook() throws IOException {
        if (settings.facebookToken().isBlank()) throw new SocialMediaFeedException(SocialMediaFeedException.Reason.MISSING_FACEBOOK_TOKEN);
        String fields = URLEncoder.encode("id,message,created_time,permalink_url,full_picture,attachments{title,description}", StandardCharsets.UTF_8);
        URI uri = URI.create("https://graph.facebook.com/" + settings.facebookApiVersion() + "/"
            + settings.facebookPageId() + "/published_posts?fields=" + fields + "&limit=12");
        return parseFacebook(downloader.download(uri, settings.facebookToken(), MAX_FEED_BYTES), settings.facebookPageId());
    }

    /** Parses the public Atom feed with external entities and DTDs disabled. */
    static List<Post> parseYoutube(byte[] bytes, String channelId) throws Exception {
        if (bytes.length > MAX_FEED_BYTES) throw new IOException("Feed exceeds size limit");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        var builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler() {
            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }
        });
        Element root = builder.parse(new ByteArrayInputStream(bytes)).getDocumentElement();
        if (!ATOM.equals(root.getNamespaceURI()) || !"feed".equals(root.getLocalName())) throw new IOException("Invalid Atom feed");
        String feedChannelId = xmlText(root, YOUTUBE, "channelId");
        if (!channelId.equals(feedChannelId) && !(channelId.startsWith("UC") && channelId.substring(2).equals(feedChannelId))) {
            throw new IOException("Unexpected YouTube channel");
        }
        NodeList entries = root.getElementsByTagNameNS(ATOM, "entry");
        List<Post> posts = new ArrayList<>();
        for (int index = 0; index < entries.getLength(); index++) {
            Element entry = (Element) entries.item(index);
            String id = xmlText(entry, YOUTUBE, "videoId");
            String title = cleanText(xmlText(entry, ATOM, "title"), 180);
            String published = parseDate(xmlText(entry, ATOM, "published"));
            if (!id.matches("[A-Za-z0-9_-]{11}") || title.isBlank() || published == null
                || !channelId.equals(xmlText(entry, YOUTUBE, "channelId"))) continue;
            NodeList thumbnails = entry.getElementsByTagNameNS(MEDIA, "thumbnail");
            String image = thumbnails.getLength() == 0 ? "" : ((Element) thumbnails.item(0)).getAttribute("url");
            posts.add(new Post("youtube:" + id, "youtube", title,
                cleanText(xmlText(entry, MEDIA, "description"), 600), "https://www.youtube.com/watch?v=" + id, published, image, null));
        }
        if (entries.getLength() > 0 && posts.isEmpty()) throw new IOException("Atom feed contains no valid entries");
        return latest(posts, YOUTUBE_FALLBACK_ITEMS);
    }

    /** Parses published Page-authored posts and rejects invalid IDs, links and dates. */
    static List<Post> parseFacebook(byte[] bytes, String pageId) throws IOException {
        if (bytes.length > MAX_FEED_BYTES) throw new IOException("Feed exceeds size limit");
        JsonNode root = MAPPER.readTree(bytes);
        if (root == null || root.has("error") || !root.path("data").isArray()) throw new IOException("Invalid Facebook feed");
        List<Post> posts = new ArrayList<>();
        for (JsonNode entry : root.get("data")) {
            String id = entry.path("id").asText("");
            String link = entry.path("permalink_url").asText("");
            String published = parseDate(entry.path("created_time").asText(""));
            if (!id.matches(java.util.regex.Pattern.quote(pageId) + "_[0-9]+") || !isPostLink(link, "facebook") || published == null) continue;
            JsonNode attachment = entry.path("attachments").path("data").path(0);
            String message = cleanText(entry.path("message").asText(""), 600);
            String title = message.isBlank() ? cleanText(attachment.path("title").asText(""), 180)
                : cleanText(message.split("\\R", 2)[0], MAX_FACEBOOK_TITLE_LENGTH);
            if (title.isBlank()) title = "Nový príspevok na Facebooku";
            if (message.isBlank()) message = cleanText(attachment.path("description").asText(""), 600);
            posts.add(new Post("facebook:" + id, "facebook", title, message, link, published,
                entry.path("full_picture").asText(""), null));
        }
        if (!root.get("data").isEmpty() && posts.isEmpty()) throw new IOException("Facebook feed contains no valid entries");
        return latest(posts, ITEMS_PER_SOURCE);
    }

    private static List<Post> latest(List<Post> posts, int limit) {
        Map<String, Post> unique = new LinkedHashMap<>();
        posts.stream().sorted(Comparator.comparing((Post post) -> Instant.parse(post.publishedAt())).reversed())
            .forEach(post -> unique.putIfAbsent(post.id(), post));
        return unique.values().stream().limit(limit).toList();
    }

    private Post cacheImage(Post post, Post previous) {
        String cached = null;
        try {
            URI remote = URI.create(post.remoteImage());
            if (!SocialMediaHttpClient.isImageUrl(remote)) return withImage(post, null);
            String fileName = hash(post.id() + "|" + post.remoteImage()).substring(0, 32) + ".jpg";
            Path path = settings.outputDirectory().resolve("social-media/images").resolve(fileName);
            if (!Files.isRegularFile(path)) {
                byte[] jpeg = thumbnail(downloader.download(remote, null, MAX_IMAGE_BYTES));
                publishFiles(Map.of(path, jpeg));
            }
            cached = "social-media/images/" + fileName;
        } catch (Exception exception) {
            if (previous != null) cached = ownImage(previous.image());
            Logger.warn(SocialMediaFeedGenerator.class, "Unable to cache preview for " + post.id() + "; using text or the previous preview");
        }
        return withImage(post, cached);
    }

    /** Normalizes previous absolute preview URLs to portable paths, accepting only existing local previews. */
    private String ownImage(String image) {
        if (image == null) return null;
        try {
            URI uri = URI.create(image);
            String relative = image;
            if (uri.isAbsolute()) {
                if (!SocialMediaHttpClient.isHttps(uri) || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || !uri.normalize().equals(uri) || uri.getRawPath().contains("%")) return null;
                int index = uri.getRawPath().lastIndexOf("/social-media/images/");
                if (index < 0) return null;
                relative = uri.getRawPath().substring(index + 1);
            }
            return relative.matches("social-media/images/[a-f0-9]{32}\\.jpg")
                && Files.isRegularFile(settings.outputDirectory().resolve(relative)) ? relative : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Post withImage(Post post, String image) {
        return new Post(post.id(), post.source(), post.title(), post.perex(), post.link(), post.publishedAt(), post.remoteImage(), image);
    }

    /** Decodes bounded raster dimensions and re-encodes a small JPEG without upstream metadata. */
    static byte[] thumbnail(byte[] bytes) throws IOException {
        if (bytes.length > MAX_IMAGE_BYTES) throw new IOException("Image exceeds size limit");
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported image");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 8192 || height > 8192 || (long) width * height > 16_000_000) {
                    throw new IOException("Image dimensions exceed limit");
                }
                BufferedImage source = reader.read(0);
                double scale = Math.min(1.0, Math.min(640.0 / width, 480.0 / height));
                BufferedImage target = new BufferedImage(Math.max(1, (int) (width * scale)), Math.max(1, (int) (height * scale)), BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = target.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);
                    graphics.fillRect(0, 0, target.getWidth(), target.getHeight());
                    graphics.drawImage(source, 0, 0, target.getWidth(), target.getHeight(), null);
                } finally {
                    graphics.dispose();
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                if (!ImageIO.write(target, "jpeg", output)) throw new IOException("JPEG writer unavailable");
                return output.toByteArray();
            } finally {
                reader.dispose();
            }
        }
    }

    private ObjectNode toNews(Post post, String language, Map<String, Translation> previous, Map<String, Translation> current) {
        ObjectNode news = MAPPER.createObjectNode();
        String title = translate(post, "title", post.title(), language, previous, current);
        String perex = summarize(post.perex(), post.title());
        news.put("title", title);
        news.put("perex", escapePerex(summarize(translate(post, "perex", perex, language, previous, current), title)));
        news.put("link", post.link());
        news.put("id", post.id());
        news.put("source", post.source());
        news.put("publishedAt", post.publishedAt());
        String image = ownImage(post.image());
        if (image != null) news.put("image", image);
        return news;
    }

    private String translate(Post post, String field, String text, String language, Map<String, Translation> previous, Map<String, Translation> current) {
        if ("sk".equals(language) || text.isBlank()) return text;
        if ("title".equals(field) && "facebook".equals(post.source()) && "Nový príspevok na Facebooku".equals(text)) {
            return "cs".equals(language) ? "Nový příspěvek na Facebooku" : "New Facebook post";
        }
        String key = post.id() + ":" + language + ":" + field;
        String sourceHash = hash(text);
        Translation old = previous.get(key);
        if (old != null && sourceHash.equals(old.sourceHash()) && old.text() != null && !old.text().isBlank()) {
            current.put(key, old);
            return old.text();
        }
        try {
            String translated = cleanText(translator.translate(text, language), "title".equals(field) ? 180 : 600);
            if (!translated.isBlank() && !translated.equals(text)) {
                current.put(key, new Translation(sourceHash, translated));
                return translated;
            }
        } catch (Exception exception) {
            Logger.warn(SocialMediaFeedGenerator.class, "Translation unavailable for " + post.id() + " (" + language + ")");
        }
        if (old != null && old.text() != null && !old.text().isBlank()) {
            current.put(key, old);
            return old.text();
        }
        return text;
    }

    /** Escapes source text before the legacy dashboard's HTML and Markdown processing. */
    static String escapePerex(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;").replace("[", "&#91;").replace("]", "&#93;");
    }

    /** Produces a short plain-text excerpt without repeating the title or exposing long social links. */
    static String summarize(String text, String title) {
        if (text != null && title != null && title.endsWith("…")) {
            String body = text.stripLeading();
            String firstLine = body.split("\\R", 2)[0];
            if (cleanText(firstLine, MAX_FACEBOOK_TITLE_LENGTH).equals(title)) text = body.substring(firstLine.length());
        }
        String summary = summaryText(text);
        String heading = summaryText(title);
        if (!heading.isEmpty() && summary.startsWith(heading)
            && (summary.length() == heading.length() || !Character.isLetterOrDigit(summary.codePointAt(heading.length())))) {
            summary = summary.substring(heading.length()).stripLeading();
        }
        if (summary.isEmpty()) return "";
        BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.ROOT);
        sentences.setText(summary);
        int boundary = sentences.next();
        String firstSentence = summary.substring(0, boundary).stripTrailing();
        if (firstSentence.codePointCount(0, firstSentence.length()) <= MAX_PEREX_LENGTH) return firstSentence;
        int end = summary.offsetByCodePoints(0, MAX_PEREX_LENGTH - 1);
        if (!Character.isWhitespace(summary.codePointAt(end))) {
            int lastSpace = summary.lastIndexOf(' ', end - 1);
            if (lastSpace > 0) end = lastSpace;
        }
        return summary.substring(0, end).stripTrailing() + "…";
    }

    private static String summaryText(String value) {
        if (value == null) return "";
        return value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "")
            .replace("\\n", "\n")
            .replaceAll("!?\\[([^\\]\\r\\n]+)\\]\\([^)]*\\)", "$1")
            .replaceAll("(?i)\\b(?:https?://|ftp://|www\\.)[^\\s<>]+", "")
            .replaceAll("(?U)(?<!\\S)#[\\p{L}\\p{N}_]+", "")
            .replaceAll("(?m)^\\h*(?:[-*+•]\\h+|[0-9]+[.)]\\h+|#{1,6}\\h+)", "")
            .replaceAll("(?U)\\s+", " ").trim();
    }

    private static String cleanText(String value, int limit) {
        if (value == null) return "";
        String text = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").replace("\r\n", "\n").trim();
        if (text.codePointCount(0, text.length()) <= limit) return text;
        return text.substring(0, text.offsetByCodePoints(0, limit - 1)).stripTrailing() + "…";
    }

    private static String xmlText(Element element, String namespace, String tag) {
        NodeList nodes = element.getElementsByTagNameNS(namespace, tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent();
    }

    private static String parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value).toString();
        } catch (DateTimeParseException exception) {
            try {
                return OffsetDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")).toInstant().toString();
            } catch (DateTimeParseException invalid) {
                return null;
            }
        }
    }

    private static boolean isPostLink(String value, String source) {
        try {
            if (value == null || value.matches(".*[\"'<>\\\\].*")) return false;
            URI uri = URI.create(value);
            Set<String> hosts = "youtube".equals(source) ? Set.of("www.youtube.com", "youtube.com") : Set.of("www.facebook.com", "facebook.com");
            return SocialMediaHttpClient.isHttps(uri) && hosts.contains(uri.getHost().toLowerCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static JsonNode readJson(Path path) throws IOException {
        if (Files.size(path) > MAX_FEED_BYTES) throw new IOException("JSON file exceeds size limit");
        return MAPPER.readTree(Files.readAllBytes(path));
    }

    private static byte[] serialize(Object object) throws IOException {
        byte[] bytes = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(object);
        if (bytes.length > MAX_FEED_BYTES) throw new IOException("Generated JSON exceeds size limit");
        return bytes;
    }

    /** Stages every file before replacing any target; each replacement is atomic on the local filesystem. */
    static void publishFiles(Map<Path, byte[]> files) throws IOException {
        Map<Path, Path> staged = new LinkedHashMap<>();
        try {
            for (Map.Entry<Path, byte[]> entry : files.entrySet()) {
                Path parent = entry.getKey().toAbsolutePath().getParent();
                Files.createDirectories(parent);
                Path temporary = Files.createTempFile(parent, ".social-media-", ".tmp");
                staged.put(entry.getKey(), temporary);
                Files.write(temporary, entry.getValue());
            }
            for (Map.Entry<Path, Path> entry : staged.entrySet()) {
                Files.move(entry.getValue(), entry.getKey(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            for (Path temporary : staged.values()) Files.deleteIfExists(temporary);
        }
    }

    record Post(String id, String source, String title, String perex, String link, String publishedAt, String remoteImage, String image) { }
    record Translation(String sourceHash, String text) { }
    record FeedCache(Map<String, List<Post>> sources, Map<String, Translation> translations) { }

    @FunctionalInterface
    interface Downloader {
        byte[] download(URI uri, String token, int limit) throws IOException;
    }

    @FunctionalInterface
    interface Translator {
        String translate(String text, String language);
    }

    record Settings(Path outputDirectory, Path cacheFile,
        String youtubeChannelId, String facebookPageId, String facebookToken, String facebookApiVersion) {

        static Settings fromConfiguration() throws IOException {
            String output = Constants.getString("socialMediaFeedOutputDirectory", "/json/");
            String cache = "/WEB-INF/social-media/feed-cache.json";
            String youtube = Constants.getString("socialMediaYoutubeChannelId", "UCtS48g2dGJ5WVeh89PAOeLA");
            String facebook = Constants.getString("socialMediaFacebookPageId", "237316516345904");
            String version = Constants.getString("socialMediaFacebookApiVersion", "v26.0");
            if (!youtube.matches("UC[A-Za-z0-9_-]{22}") || !facebook.matches("[0-9]+") || !version.matches("v[0-9]+\\.[0-9]+")) {
                throw new IOException("Invalid social account configuration");
            }
            return new Settings(Path.of(Tools.getRealPath(output)), Path.of(Tools.getRealPath(cache)),
                youtube, facebook, readFacebookToken(), version);
        }

        private static String readFacebookToken() {
            String token = Constants.getString("socialMediaFacebookAccessToken");
            if (token.isBlank()) return "";
            try {
                var variable = ConfDB.getVariable("socialMediaFacebookAccessToken");
                if (variable != null && isEncryptedToken(variable.getValue()) && !token.startsWith("encrypted:")) return token;
            } catch (Exception exception) {
                Logger.warn(SocialMediaFeedGenerator.class, "Cannot verify encrypted Facebook token configuration");
            }
            Logger.warn(SocialMediaFeedGenerator.class, "Facebook feed disabled: save the Page token with configuration encryption enabled");
            return "";
        }

        static boolean isEncryptedToken(String value) {
            return value != null && value.startsWith("encrypted:") && value.length() > 32;
        }

        @Override
        public String toString() {
            return "SocialMediaFeedGenerator.Settings[outputDirectory=" + outputDirectory + ", facebookToken=REDACTED]";
        }

    }
}
