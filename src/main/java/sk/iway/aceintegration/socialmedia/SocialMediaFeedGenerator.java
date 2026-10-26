package sk.iway.aceintegration.socialmedia;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.BreakIterator;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

import org.apache.commons.codec.digest.DigestUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.translation.TranslationService;

/**
 * Builds the shared dashboard news feeds from a YouTube channel and a Facebook Page.
 *
 * <p>The documentation server downloads posts, caches local JPEG previews, translates Slovak
 * titles and summaries into Czech and English, and publishes the three {@code wjnews.*.json}
 * files. Customer dashboards read these public files without social-network credentials.
 * A private cache retains provider data and translations for later runs and upstream outages.</p>
 *
 * <p>Register {@link #main(String[])} manually as a WebJET cron task on one documentation-server
 * node. The integration and its credentials are excluded from customer JAR and update ZIP
 * distributions. Download and translation dependencies can be supplied separately for tests.</p>
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
    private static final int MAX_LONG_PEREX_LENGTH = 1000;
    private static final int MAX_DESCRIPTION_CAPTURE_LENGTH = 4000;
    private static final int MAX_FACEBOOK_TITLE_LENGTH = 120;
    private static final Duration IMAGE_RETENTION = Duration.ofDays(30);
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
     * Runs one scheduled refresh using server configuration and the configured translation service.
     *
     * <p>Concurrent invocations in the same classloader are skipped. Failures are logged with
     * safe diagnostics rather than propagated to the cron caller, and the HTTP client is closed
     * after each run. The lock does not coordinate separate cluster nodes.</p>
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

    /**
     * Refreshes provider caches and publishes localized feeds in newest-first order.
     *
     * <p>A failed provider retains its previous posts; a successful empty response clears them.
     * The public feed contains up to three posts per source, or four YouTube videos when no
     * Facebook posts remain. All language payloads and the private cache are prepared before
     * publication. Preview cleanup runs only after every file has been replaced successfully.</p>
     *
     * @throws IOException if the private cache is invalid, both providers fail without usable cached
     *     posts, generated output exceeds its size limit, or serialization or publication fails
     */
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
                String message = "Unable to refresh " + source
                    + "; retaining " + oldPosts.size() + " cached posts (" + SocialMediaFeedException.safeSummary(exception) + ")";
                Logger.warn(SocialMediaFeedGenerator.class, message);
                Adminlog.add(Adminlog.TYPE_CRON, "SocialMediaFeedGenerator: " + message, -1, -1);
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
            for (Post post : posts) news.add(toNews(post, language, translations));
            feed.set("news", news);
            byte[] publicFeed = serialize(feed);
            if (publicFeed.length > MAX_PUBLIC_FEED_BYTES) throw new IOException("Public feed exceeds the dashboard response limit");
            files.put(settings.outputDirectory().resolve("wjnews." + language + ".json"), publicFeed);
        }
        files.put(settings.cacheFile(), serialize(new FeedCache(sources, translations)));
        publishFiles(files);
        cleanupImages(previous, sources);
    }

    /**
     * Removes expired, unreferenced previews after successful feed and cache publication.
     *
     * <p>Images referenced by either cache have their modification time refreshed, giving newly
     * retired images a full thirty-day grace period for browser-cached feeds. All source posts
     * are protected, including fallback data and the fourth YouTube video hidden from the public
     * feed. Only generated JPEG filenames in the image directory are eligible for deletion;
     * subdirectories and symbolic links are skipped. Cleanup failures are logged and suppressed.</p>
     *
     * @param previous validated cache loaded before this refresh
     * @param sources source posts saved by the completed refresh
     */
    private void cleanupImages(FeedCache previous, Map<String, List<Post>> sources) {
        Path images = settings.outputDirectory().resolve("social-media/images");
        try {
            if (!Files.isDirectory(images.getParent(), LinkOption.NOFOLLOW_LINKS)
                || !Files.isDirectory(images, LinkOption.NOFOLLOW_LINKS)) return;
            Set<Path> retained = new HashSet<>();
            for (Map<String, List<Post>> sourcePosts : List.of(previous.sources(), sources)) {
                for (Post post : sourcePosts.values().stream().flatMap(List::stream).toList()) {
                    String image = ownImage(post.image());
                    if (image != null) retained.add(settings.outputDirectory().resolve(image));
                }
            }
            Instant now = Instant.now();
            for (Path image : retained) {
                if (Files.isRegularFile(image, LinkOption.NOFOLLOW_LINKS)) {
                    Files.setAttribute(image, "basic:lastModifiedTime", FileTime.from(now), LinkOption.NOFOLLOW_LINKS);
                }
            }
            try (var entries = Files.newDirectoryStream(images)) {
                for (Path image : entries) {
                    if (retained.contains(image) || !image.getFileName().toString().matches("[a-f0-9]{32}\\.jpg")
                        || !Files.isRegularFile(image, LinkOption.NOFOLLOW_LINKS)) continue;
                    if (Files.getLastModifiedTime(image, LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(now.minus(IMAGE_RETENTION))) {
                        Files.delete(image);
                    }
                }
            }
        } catch (IOException | RuntimeException exception) {
            Logger.warn(SocialMediaFeedGenerator.class, "Unable to clean up old social media previews ("
                + SocialMediaFeedException.safeSummary(exception) + "); published feeds remain valid");
        }
    }

    /**
     * Carries forward translation entries for posts still retained in either provider's cache.
     *
     * @param sources current provider data, including posts omitted from the public feed
     * @param previous translations keyed by post ID, target language and field name
     * @return mutable map containing retained Czech and English title and long-summary translations
     */
    private static Map<String, Translation> retainTranslations(Map<String, List<Post>> sources, Map<String, Translation> previous) {
        Map<String, Translation> retained = new LinkedHashMap<>();
        for (Post post : sources.values().stream().flatMap(List::stream).toList()) {
            for (String suffix : List.of(":cs:title", ":cs:perexLong", ":en:title", ":en:perexLong")) {
                String key = post.id() + suffix;
                Translation translation = previous.get(key);
                if (translation != null) retained.put(key, translation);
            }
        }
        return retained;
    }

    /**
     * Loads and validates the private cache before any provider requests or publication.
     *
     * <p>Known Facebook video permalinks are removed, image references are normalized to existing
     * local files, and legacy {@code :perex} translation keys migrate to {@code :perexLong}.
     * Invalid cache data aborts generation rather than silently replacing the published feeds.</p>
     *
     * @return validated cached sources and translations, or empty maps when no cache file exists
     * @throws IOException if the file cannot be read, exceeds the size limit, or contains invalid data
     */
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
            sources.put(source, posts.stream()
                .filter(post -> !"facebook".equals(source) || !isVideoLink(post.link()))
                .map(post -> withImage(post, ownImage(post.image()))).toList());
        }
        Map<String, Translation> translations = new LinkedHashMap<>(cache.translations());
        for (Map.Entry<String, Translation> entry : cache.translations().entrySet()) {
            if (entry.getKey().endsWith(":perex")) {
                translations.putIfAbsent(entry.getKey() + "Long", entry.getValue());
                translations.remove(entry.getKey());
            }
        }
        return new FeedCache(sources, translations);
    }

    /**
     * Downloads the configured channel's public Atom feed without authentication.
     *
     * @return up to four validated videos, newest first
     * @throws Exception if downloading, secure XML parser setup or feed parsing fails
     */
    private List<Post> fetchYoutube() throws Exception {
        URI uri = URI.create("https://www.youtube.com/feeds/videos.xml?channel_id=" + settings.youtubeChannelId());
        return parseYoutube(downloader.download(uri, null, MAX_FEED_BYTES), settings.youtubeChannelId());
    }

    /**
     * Requests twelve published Page posts with the attachment metadata needed to exclude videos.
     *
     * <p>The Page token is passed separately to the downloader, never placed in the URL.
     * Only this response page is considered; pagination links are not followed.</p>
     *
     * @return up to three validated non-video posts, newest first
     * @throws IOException if the token is missing, the request fails or the response is invalid
     */
    private List<Post> fetchFacebook() throws IOException {
        if (settings.facebookToken().isBlank()) throw new SocialMediaFeedException(SocialMediaFeedException.Reason.MISSING_FACEBOOK_TOKEN);
        String fields = URLEncoder.encode("id,message,created_time,permalink_url,full_picture,"
            + "attachments{title,description,media_type,type,url,unshimmed_url,subattachments{media_type,type,url,unshimmed_url}}", StandardCharsets.UTF_8);
        URI uri = URI.create("https://graph.facebook.com/" + settings.facebookApiVersion() + "/"
            + settings.facebookPageId() + "/published_posts?fields=" + fields + "&limit=12");
        return parseFacebook(downloader.download(uri, settings.facebookToken(), MAX_FEED_BYTES), settings.facebookPageId());
    }

    /**
     * Parses channel videos with DTDs and external XML resources disabled.
     *
     * <p>The feed-level channel ID may omit the {@code UC} prefix, but entry IDs must match the
     * complete channel ID. Invalid entries are skipped; a nonempty feed with no valid entries
     * is rejected. Descriptions are captured for later summarization and previews remain remote.</p>
     *
     * @param bytes downloaded Atom document, limited to the maximum provider response size
     * @param channelId expected full YouTube channel ID, including its {@code UC} prefix
     * @return up to four distinct videos, newest first, with no local image assigned yet
     * @throws Exception if secure parser setup fails or the XML, channel identity or entries are invalid
     */
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
                cleanText(xmlText(entry, MEDIA, "description"), MAX_DESCRIPTION_CAPTURE_LENGTH), "https://www.youtube.com/watch?v=" + id, published, image, null));
        }
        if (entries.getLength() > 0 && posts.isEmpty()) throw new IOException("Atom feed contains no valid entries");
        return latest(posts, YOUTUBE_FALLBACK_ITEMS);
    }

    /**
     * Selects Page-authored announcements after validating identifiers, links and publication dates.
     *
     * <p>Video permalinks and attachments are excluded before applying the three-post limit.
     * Titles come from the message's first line, an attachment title or a fixed fallback.
     * A response containing only valid video posts is a successful empty result.</p>
     *
     * @param bytes downloaded Graph API JSON, limited to the maximum provider response size
     * @param pageId numeric Page ID expected as the prefix of each post ID
     * @return up to three distinct non-video posts, newest first, with no local image assigned yet
     * @throws IOException if the JSON envelope is invalid, exceeds the size limit, reports an error,
     *     or contains records but none have valid post identifiers, links and dates
     */
    static List<Post> parseFacebook(byte[] bytes, String pageId) throws IOException {
        if (bytes.length > MAX_FEED_BYTES) throw new IOException("Feed exceeds size limit");
        JsonNode root = MAPPER.readTree(bytes);
        if (root == null || root.has("error") || !root.path("data").isArray()) throw new IOException("Invalid Facebook feed");
        List<Post> posts = new ArrayList<>();
        int validEntries = 0;
        for (JsonNode entry : root.get("data")) {
            String id = entry.path("id").asText("");
            String link = entry.path("permalink_url").asText("");
            String published = parseDate(entry.path("created_time").asText(""));
            if (!id.matches(java.util.regex.Pattern.quote(pageId) + "_[0-9]+") || !isPostLink(link, "facebook") || published == null) continue;
            validEntries++;
            if (isVideoLink(link) || hasVideoAttachment(entry.path("attachments"))) continue;
            JsonNode attachment = entry.path("attachments").path("data").path(0);
            String message = cleanText(entry.path("message").asText(""), MAX_DESCRIPTION_CAPTURE_LENGTH);
            String title = message.isBlank() ? cleanText(attachment.path("title").asText(""), 180)
                : cleanText(message.split("\\R", 2)[0], MAX_FACEBOOK_TITLE_LENGTH);
            if (title.isBlank()) title = "Nový príspevok na Facebooku";
            if (message.isBlank()) message = cleanText(attachment.path("description").asText(""), MAX_DESCRIPTION_CAPTURE_LENGTH);
            posts.add(new Post("facebook:" + id, "facebook", title, message, link, published,
                entry.path("full_picture").asText(""), null));
        }
        if (!root.get("data").isEmpty() && validEntries == 0) throw new IOException("Facebook feed contains no valid entries");
        return latest(posts, ITEMS_PER_SOURCE);
    }

    /**
     * Recursively checks Graph attachments and subattachments for video metadata or destinations.
     *
     * @param attachments attachment container with an optional {@code data} array; a missing node is allowed
     * @return whether any attachment describes a video, Reel or recognized video link
     */
    private static boolean hasVideoAttachment(JsonNode attachments) {
        for (JsonNode attachment : attachments.path("data")) {
            String type = attachment.path("type").asText("").toLowerCase(Locale.ROOT);
            if ("video".equalsIgnoreCase(attachment.path("media_type").asText(""))
                || "video".equals(type) || type.startsWith("video_")
                || "reel".equals(type) || "reels".equals(type) || type.startsWith("reel_")
                || isVideoLink(attachment.path("url").asText(""))
                || isVideoLink(attachment.path("unshimmed_url").asText(""))
                || hasVideoAttachment(attachment.path("subattachments"))) return true;
        }
        return false;
    }

    /**
     * Recognizes YouTube and Facebook video destinations without making network requests.
     *
     * <p>Facebook {@code l.php} links are inspected through their decoded {@code u} parameter.
     * Unknown or malformed destinations are not classified as videos.</p>
     *
     * @param value candidate HTTP(S) link, possibly {@code null} or blank
     * @return whether the link matches a supported video, live-stream, short or Reel URL
     */
    private static boolean isVideoLink(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null || uri.getUserInfo() != null
                || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) return false;
            if (Set.of("l.facebook.com", "lm.facebook.com", "www.facebook.com", "facebook.com").contains(uri.getHost().toLowerCase(Locale.ROOT))
                && "/l.php".equals(uri.getPath())) {
                uri = URI.create(queryParameter(uri, "u"));
            }
            if (uri.getHost() == null || uri.getUserInfo() != null
                || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) return false;
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath();
            if ("youtu.be".equals(host)) return path.matches("/[A-Za-z0-9_-]{11}/?");
            if (Set.of("youtube.com", "www.youtube.com", "m.youtube.com").contains(host)) {
                return ("/watch".equals(path) && queryParameter(uri, "v").matches("[A-Za-z0-9_-]{11}"))
                    || path.matches("/(?:shorts|embed|live)/[A-Za-z0-9_-]{11}/?");
            }
            if ("fb.watch".equals(host)) return path.length() > 1;
            if (!Set.of("facebook.com", "www.facebook.com", "m.facebook.com", "web.facebook.com").contains(host)) return false;
            return path.matches("/(?:[^/]+/)?videos/.+") || path.matches("/(?:reel|reels)/[^/]+/?")
                || (("/watch".equals(path) || "/watch/".equals(path) || "/watch/live/".equals(path) || "/video.php".equals(path))
                    && !queryParameter(uri, "v").isBlank());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Extracts the first query parameter with the requested literal name and decodes its value.
     *
     * @param uri URI whose raw query is inspected
     * @param name case-sensitive, unencoded parameter name
     * @return UTF-8 decoded value, or an empty string when the parameter is absent
     * @throws IllegalArgumentException if the selected value contains malformed percent encoding
     */
    private static String queryParameter(URI uri, String name) {
        if (uri.getRawQuery() == null) return "";
        for (String parameter : uri.getRawQuery().split("&")) {
            int separator = parameter.indexOf('=');
            if (separator > 0 && name.equals(parameter.substring(0, separator))) {
                return URLDecoder.decode(parameter.substring(separator + 1), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    /**
     * Sorts validated posts by publication time and keeps the newest occurrence of each ID.
     *
     * @param posts posts with publication timestamps accepted by {@link Instant#parse(CharSequence)}
     * @param limit maximum number of distinct posts to return; must be nonnegative
     * @return unmodifiable list of distinct posts, newest first
     */
    private static List<Post> latest(List<Post> posts, int limit) {
        Map<String, Post> unique = new LinkedHashMap<>();
        posts.stream().sorted(Comparator.comparing((Post post) -> Instant.parse(post.publishedAt())).reversed())
            .forEach(post -> unique.putIfAbsent(post.id(), post));
        return unique.values().stream().limit(limit).toList();
    }

    /**
     * Reuses or creates a local JPEG preview named from the post ID and complete upstream image URL.
     *
     * <p>Approved images are downloaded and re-encoded before publication. A well-formed URL outside
     * the image allowlist produces a text-only post. Processing exceptions fall back to an existing
     * previous preview when available. Image errors do not prevent the post from being published.</p>
     *
     * @param post newly fetched post with an upstream image reference
     * @param previous cached version of the same post, or {@code null} for a new post
     * @return copy of the fetched post with a local relative preview path or {@code null} image
     */
    private Post cacheImage(Post post, Post previous) {
        String cached = null;
        try {
            URI remote = URI.create(post.remoteImage());
            if (!SocialMediaHttpClient.isImageUrl(remote)) return withImage(post, null);
            String fileName = DigestUtils.sha256Hex(post.id() + "|" + post.remoteImage()).substring(0, 32) + ".jpg";
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

    /**
     * Resolves a stored preview reference to an existing generated file below the output directory.
     *
     * <p>Legacy absolute HTTPS references are interpreted by their {@code /social-media/images/}
     * suffix; their original host is not contacted. Only the expected hash-based JPEG filename
     * is accepted, and the corresponding file must exist in the current output directory.</p>
     *
     * @param image relative preview path or legacy absolute URL, possibly {@code null}
     * @return portable {@code social-media/images/...jpg} path, or {@code null} for an invalid or missing file
     */
    private String ownImage(String image) {
        if (image == null) return null;
        try {
            URI uri = URI.create(image);
            String relative = image;
            if (uri.isAbsolute()) {
                if (!SocialMediaHttpClient.isHttps(uri) || uri.getRawQuery() != null
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

    /**
     * Converts an upstream raster image into a JPEG preview without upstream metadata.
     *
     * <p>Input byte size and dimensions are checked before full decoding. The first image is
     * scaled proportionally to fit within 640 by 480 pixels without upscaling; transparency
     * is composited over white. Readers, streams and graphics resources are released locally.</p>
     *
     * @param bytes downloaded raster image
     * @return encoded JPEG bytes
     * @throws IOException if the input exceeds a size or dimension limit, cannot be decoded,
     *     or cannot be encoded as JPEG
     */
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

    /**
     * Builds one localized public record while preserving the legacy title, perex and link fields.
     *
     * <p>The long summary is cleaned before and after translation; the short summary is derived
     * from it. Summaries are escaped for the historical HTML/Markdown renderer, while the title
     * remains plain text. Stable identity and source metadata are shared across languages.</p>
     *
     * @param post validated source post with an optional local preview
     * @param language target feed language: {@code sk}, {@code cs} or {@code en}
     * @param translations mutable cache updated when new translations are accepted
     * @return public JSON record, omitting an empty long summary or unavailable preview
     */
    private ObjectNode toNews(Post post, String language, Map<String, Translation> translations) {
        ObjectNode news = MAPPER.createObjectNode();
        String title = translate(post, "title", post.title(), language, translations);
        String source = longSummary(post.perex(), post.title());
        String perexLong = longSummary(translate(post, "perexLong", source, language, translations), title);
        news.put("title", title);
        news.put("perex", escapePerex(summarize(perexLong, "")));
        if (!perexLong.isEmpty()) news.put("perexLong", escapePerex(perexLong));
        news.put("link", post.link());
        news.put("id", post.id());
        news.put("source", post.source());
        news.put("publishedAt", post.publishedAt());
        String image = ownImage(post.image());
        if (image != null) news.put("image", image);
        return news;
    }

    /**
     * Reuses a translation with the same source hash or attempts to translate the current text.
     *
     * <p>Slovak and blank text bypass translation. The generic Facebook title has fixed localized
     * variants. Empty, unchanged or failed translation results are not cached as successes;
     * an older nonblank translation is used instead, even if its source hash differs, or the
     * source text is returned. Such failures are retried on subsequent runs.</p>
     *
     * @param post source post supplying the stable cache identity and provider
     * @param field translated field name, {@code title} or {@code perexLong}
     * @param text non-null source text to translate
     * @param language target feed language
     * @param translations mutable cache keyed by post ID, language and field
     * @return cached, newly translated or fallback text
     */
    private String translate(Post post, String field, String text, String language, Map<String, Translation> translations) {
        if ("sk".equals(language) || text.isBlank()) return text;
        if ("title".equals(field) && "facebook".equals(post.source()) && "Nový príspevok na Facebooku".equals(text)) {
            return "cs".equals(language) ? "Nový příspěvek na Facebooku" : "New Facebook post";
        }
        String key = post.id() + ":" + language + ":" + field;
        String sourceHash = DigestUtils.sha256Hex(text);
        Translation old = translations.get(key);
        if (old != null && (old.text() == null || old.text().isBlank())) old = null;
        if (old != null && sourceHash.equals(old.sourceHash())) return old.text();
        try {
            String translated = cleanText(translator.translate(text, language), "title".equals(field) ? 180 : MAX_DESCRIPTION_CAPTURE_LENGTH);
            if (!translated.isBlank() && !translated.equals(text)) {
                translations.put(key, new Translation(sourceHash, translated));
                return translated;
            }
        } catch (Exception exception) {
            Logger.warn(SocialMediaFeedGenerator.class, "Translation unavailable for " + post.id() + " (" + language + ")");
        }
        return old == null ? text : old.text();
    }

    /**
     * Escapes HTML delimiters and Markdown link brackets for legacy dashboard summary rendering.
     *
     * @param text non-null plain summary, before any HTML entity encoding
     * @return encoded summary that preserves literal text without introducing HTML tags or Markdown links
     */
    static String escapePerex(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;").replace("[", "&#91;").replace("]", "&#93;");
    }

    /**
     * Produces a short excerpt from the first sentence of the cleaned description.
     *
     * @param text source description, possibly {@code null}
     * @param title heading to remove when repeated at the start, possibly {@code null}
     * @return unescaped summary of at most 180 Unicode code points, or an empty string
     */
    static String summarize(String text, String title) {
        String summary = summaryBody(text, title);
        if (summary.isEmpty()) return "";
        BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.ROOT);
        sentences.setText(summary);
        return boundWords(summary.substring(0, sentences.next()).stripTrailing(), MAX_PEREX_LENGTH);
    }

    /**
     * Produces the expanded-card summary without limiting it to the first sentence.
     *
     * @param text source or translated description, possibly {@code null}
     * @param title heading to remove when repeated at the start, possibly {@code null}
     * @return unescaped summary of at most 1000 Unicode code points, or an empty string
     */
    static String longSummary(String text, String title) {
        return boundWords(summaryBody(text, title), MAX_LONG_PEREX_LENGTH);
    }

    /**
     * Removes a repeated leading heading and applies the shared social-text normalization.
     *
     * <p>If the title is a truncated Facebook first line, the entire matching source line is
     * removed before normalization. Otherwise a normalized leading title is removed only when
     * followed by the end of the text or a non-alphanumeric character.</p>
     *
     * @param text description to normalize, possibly {@code null}
     * @param title original or translated heading, possibly {@code null}
     * @return normalized body without the detected repeated heading; may be empty
     */
    private static String summaryBody(String text, String title) {
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
        return summary;
    }

    /**
     * Limits text by Unicode code points, preferring a space boundary when truncation is needed.
     *
     * @param summary non-null normalized text
     * @param limit positive maximum code-point count, including the appended ellipsis
     * @return unchanged text when it fits, otherwise a shortened value ending in an ellipsis
     */
    private static String boundWords(String summary, int limit) {
        if (summary.codePointCount(0, summary.length()) <= limit) return summary;
        int end = summary.offsetByCodePoints(0, limit - 1);
        if (!Character.isWhitespace(summary.codePointAt(end))) {
            int lastSpace = summary.lastIndexOf(' ', end - 1);
            if (lastSpace > 0) end = lastSpace;
        }
        return summary.substring(0, end).stripTrailing() + "…";
    }

    /**
     * Removes social links, hashtags and block markers and collapses whitespace into one line.
     *
     * <p>Markdown links retain their labels. Literal backslash-n sequences become line breaks
     * before block-marker processing. This cleanup does not HTML-escape or sanitize the result;
     * public summaries still require {@link #escapePerex(String)}.</p>
     *
     * @param value text to normalize, possibly {@code null}
     * @return normalized unescaped text, or an empty string for null input
     */
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

    /**
     * Removes unwanted control characters and bounds text without splitting Unicode code points.
     *
     * @param value source text, possibly {@code null}; line breaks and tabs are preserved
     * @param limit positive maximum code-point count, including an ellipsis when truncated
     * @return trimmed text with normalized CRLF line endings, or an empty string for null input
     */
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

    /**
     * Normalizes ISO instants and Facebook timestamps with compact numeric offsets to UTC.
     *
     * @param value provider timestamp, possibly {@code null} or blank
     * @return an {@link Instant} string, or {@code null} when neither supported format can be parsed
     */
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

    /**
     * Checks that a post link uses an approved provider host and the restricted HTTPS URL form.
     *
     * <p>Quotes, angle brackets and backslashes are rejected for legacy link rendering.
     * This check does not verify the path, post ownership or destination availability.</p>
     *
     * @param value candidate permalink, possibly {@code null}
     * @param source provider name, {@code youtube} or {@code facebook}
     * @return whether the URL is suitable for publishing as a provider link
     */
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

    /**
     * Reads a local JSON file after checking its size against the provider/cache limit.
     *
     * @param path file to read
     * @return parsed JSON tree; empty input may produce no usable root
     * @throws IOException if the file is oversized, unreadable or contains malformed JSON
     */
    private static JsonNode readJson(Path path) throws IOException {
        if (Files.size(path) > MAX_FEED_BYTES) throw new IOException("JSON file exceeds size limit");
        return MAPPER.readTree(Files.readAllBytes(path));
    }

    /**
     * Serializes a feed or private cache as formatted UTF-8 JSON within the shared size limit.
     *
     * @param object JSON tree or cache record to serialize
     * @return encoded JSON bytes
     * @throws IOException if serialization fails or the resulting JSON exceeds the shared size limit
     */
    private static byte[] serialize(Object object) throws IOException {
        byte[] bytes = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(object);
        if (bytes.length > MAX_FEED_BYTES) throw new IOException("Generated JSON exceeds size limit");
        return bytes;
    }

    /**
     * Stages all supplied bytes in sibling temporary files, then atomically replaces each target.
     *
     * <p>Parent directories are created as needed. Staging failure leaves all targets unchanged,
     * but replacement is atomic only per file: a later move failure does not roll back files
     * already replaced. Remaining temporary files are removed in the cleanup block.</p>
     *
     * @param files destination paths and complete contents, processed in map iteration order
     * @throws IOException if directory creation, staging, atomic replacement or temporary-file cleanup fails
     */
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

    /**
     * Stores a provider post before localization, together with its upstream and cached preview references.
     *
     * @param id stable provider-prefixed identity shared by all language feeds
     * @param source provider name, {@code youtube} or {@code facebook}
     * @param title unescaped source heading
     * @param perex captured source description, before public-summary cleanup and truncation
     * @param link validated provider permalink
     * @param publishedAt normalized UTC publication timestamp
     * @param remoteImage upstream image URL, which may be empty or invalid and requires validation before downloading
     * @param image local preview path, or {@code null} when unavailable; older caches may contain an absolute URL
     */
    record Post(String id, String source, String title, String perex, String link, String publishedAt, String remoteImage, String image) { }

    /**
     * Associates an accepted translation with the source content used to produce it.
     *
     * @param sourceHash SHA-256 hash of the untranslated field used to detect content changes
     * @param text translated field value, before final summary cleanup and HTML escaping
     */
    record Translation(String sourceHash, String text) { }

    /**
     * Persists provider fallback data and reusable translations outside the public feed directory.
     *
     * @param sources post lists by provider, including the fourth YouTube video reserved for fallback
     * @param translations entries keyed as {@code postId:language:field}
     */
    record FeedCache(Map<String, List<Post>> sources, Map<String, Translation> translations) { }

    /** Supplies bounded upstream response bytes independently of feed parsing and image conversion. */
    @FunctionalInterface
    interface Downloader {
        /**
         * Fetches a resource within the supplied byte budget.
         *
         * @param uri requested upstream resource
         * @param token optional Facebook Page token, or {@code null} for unauthenticated resources
         * @param limit maximum permitted response size in bytes
         * @return downloaded response bytes
         * @throws IOException if downloading fails or the resource or response violates the transport policy
         */
        byte[] download(URI uri, String token, int limit) throws IOException;
    }

    /** Adapts translation of Slovak source text to a configured service or an isolated test implementation. */
    @FunctionalInterface
    interface Translator {
        /**
         * Translates one title or description into the requested target language.
         *
         * @param text source text treated as Slovak
         * @param language target language code, {@code cs} or {@code en}
         * @return translated text; unchanged, blank or null results are treated as unavailable by the generator
         */
        String translate(String text, String language);
    }

    /**
     * Holds filesystem destinations and provider configuration for one generation run.
     *
     * <p>Production values come from {@link #fromConfiguration()}; the token is kept only for
     * upstream authentication and is deliberately redacted from the record's string representation.</p>
     *
     * @param outputDirectory filesystem directory for public feeds and the relative image subdirectory
     * @param cacheFile filesystem location of the private source and translation cache
     * @param youtubeChannelId full YouTube channel ID including the {@code UC} prefix
     * @param facebookPageId numeric Graph API Page ID
     * @param facebookToken runtime Page token, or an empty string when unavailable or not stored encrypted
     * @param facebookApiVersion Graph API version in {@code vN.N} form
     */
    record Settings(Path outputDirectory, Path cacheFile,
        String youtubeChannelId, String facebookPageId, String facebookToken, String facebookApiVersion) {

        /**
         * Resolves WebJET configuration and web-application-relative paths into run settings.
         *
         * <p>Missing account settings use the central WebJET defaults. The private cache has a
         * fixed location under {@code /WEB-INF/social-media/}; only the public output directory
         * is configurable. Token verification failure disables Facebook fetching without blocking YouTube.</p>
         *
         * @return settings with syntactically validated provider IDs and API version
         * @throws IOException if a provider ID or the API version has an invalid format
         */
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

        /**
         * Accepts the runtime token only when its persisted configuration value appears encrypted.
         *
         * <p>The runtime value must already be decrypted by WebJET configuration loading. The
         * stored value is checked separately so a plaintext or unverified token is not used.</p>
         *
         * @return runtime Page token, or an empty string when absent, unverified or still encrypted
         */
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

        /**
         * Recognizes the expected encrypted-configuration marker and minimum stored-value length.
         *
         * @param value persisted configuration value, possibly {@code null}
         * @return whether the value has the expected envelope shape; this does not decrypt or authenticate it
         */
        static boolean isEncryptedToken(String value) {
            return value != null && value.startsWith("encrypted:") && value.length() > 32;
        }

        /**
         * Describes the output destination without exposing the runtime Page token.
         *
         * @return diagnostic representation with the token replaced by {@code REDACTED}
         */
        @Override
        public String toString() {
            return "SocialMediaFeedGenerator.Settings[outputDirectory=" + outputDirectory + ", facebookToken=REDACTED]";
        }

    }
}
