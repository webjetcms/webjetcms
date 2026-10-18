package sk.iway.aceintegration.socialmedia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Verifies central feed generation without network calls, database access or translation credentials. */
class SocialMediaFeedGeneratorTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CHANNEL = "UCtS48g2dGJ5WVeh89PAOeLA";
    private static final String PAGE = "100066653358802";
    @TempDir
    Path directory;

    /** Atom parsing accepts namespaces, takes original publication dates and keeps four IDs for the Facebook fallback. */
    @Test
    void parsesLatestYoutubePosts() throws Exception {
        List<SocialMediaFeedGenerator.Post> posts = SocialMediaFeedGenerator.parseYoutube(youtube(5, "Video", "Text", ""), CHANNEL);
        assertEquals(4, posts.size());
        assertEquals("youtube:video000005", posts.get(0).id());
        assertEquals("2026-10-05T10:00:00Z", posts.get(0).publishedAt());
        assertEquals("https://www.youtube.com/watch?v=video000005", posts.get(0).link());
        assertThrows(IOException.class, () -> SocialMediaFeedGenerator.parseYoutube(youtube(1, "Video", "Text", ""), "UCanotherChannel"));
    }

    /** External entities and feeds with an unexpected root cannot be read as trusted social content. */
    @Test
    void rejectsXmlEntitiesAndInvalidRoots() {
        String entity = "<!DOCTYPE feed [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]>"
            + "<feed xmlns='http://www.w3.org/2005/Atom'>&xxe;</feed>";
        assertThrows(Exception.class, () -> SocialMediaFeedGenerator.parseYoutube(bytes(entity), CHANNEL));
        assertThrows(IOException.class, () -> SocialMediaFeedGenerator.parseYoutube(bytes("<html/>"), CHANNEL));
    }

    /** Facebook dates are normalized and items belonging to another Page or hostile links are ignored. */
    @Test
    void parsesOnlyValidFacebookPosts() throws Exception {
        ObjectNode feed = MAPPER.createObjectNode();
        ArrayNode data = feed.putArray("data");
        data.add(facebookPost("1", "Správa\nĎalší text", "2026-10-05T11:00:00+0200"));
        data.add(facebookPost("2", "No", "2026-10-06T09:00:00Z").put("id", "123_2"));
        data.add(facebookPost("3", "No", "2026-10-07T09:00:00Z").put("permalink_url", "https://www.facebook.com.evil.invalid/post"));
        List<SocialMediaFeedGenerator.Post> posts = SocialMediaFeedGenerator.parseFacebook(MAPPER.writeValueAsBytes(feed), PAGE);
        assertEquals(1, posts.size());
        assertEquals("Správa", posts.get(0).title());
        assertEquals("2026-10-05T09:00:00Z", posts.get(0).publishedAt());
        assertThrows(IOException.class, () -> SocialMediaFeedGenerator.parseFacebook(bytes("{\"error\":{}}"), PAGE));
        assertThrows(IOException.class, () -> SocialMediaFeedGenerator.parseFacebook(bytes("{\"data\":[{\"id\":\"bad\"}]}"), PAGE));
    }

    /** Facebook message titles take precedence over attachments, and image-only posts have localized fallback titles. */
    @Test
    void choosesFacebookTitlesAndLocalizesTheFallback() throws Exception {
        ObjectNode post = facebookPost("1", "Our message\nMore text", "2026-10-05T09:00:00Z");
        post.putObject("attachments").putArray("data").addObject().put("title", "Attachment title").put("description", "Attachment description");
        ObjectNode feed = MAPPER.createObjectNode();
        feed.putArray("data").add(post);
        assertEquals("Our message", SocialMediaFeedGenerator.parseFacebook(MAPPER.writeValueAsBytes(feed), PAGE).get(0).title());
        post.remove("message");
        assertEquals("Attachment title", SocialMediaFeedGenerator.parseFacebook(MAPPER.writeValueAsBytes(feed), PAGE).get(0).title());
        post.remove("attachments");
        generator(new AtomicReference<>(youtube(0, "", "", "")), new AtomicReference<>(MAPPER.writeValueAsBytes(feed)),
            (text, language) -> text).generate();
        assertEquals("Nový príspevok na Facebooku", output("sk").path("news").get(0).path("title").asText());
        assertEquals("Nový příspěvek na Facebooku", output("cs").path("news").get(0).path("title").asText());
        assertEquals("New Facebook post", output("en").path("news").get(0).path("title").asText());
    }

    /** Localized output contains only social posts, replaces old release feeds and reuses persistent translations. */
    @Test
    void mergesLocalizedFeedsAndReusesPersistentTranslations() throws Exception {
        Files.createDirectories(directory.resolve("output"));
        for (String language : List.of("sk", "cs", "en")) {
            Files.writeString(directory.resolve("output/wjnews." + language + ".json"),
                "{\"news\":[{\"title\":\"Old release\",\"perex\":\"Release text\",\"link\":\"https://docs.webjetcms.sk/CHANGELOG\"}],\"oldField\":true}");
        }
        AtomicInteger calls = new AtomicInteger();
        SocialMediaFeedGenerator.Translator translator = (text, language) -> {
            calls.incrementAndGet();
            return language + ": " + text;
        };
        AtomicReference<byte[]> youtube = new AtomicReference<>(youtube(1, "Video", "Popis", ""));
        AtomicReference<byte[]> facebook = new AtomicReference<>(facebook(1, "Príspevok"));
        SocialMediaFeedGenerator generator = generator(youtube, facebook, translator);
        generator.generate();
        JsonNode sk = output("sk");
        assertEquals(2, sk.path("news").size());
        assertEquals("facebook", sk.path("news").get(0).path("source").asText());
        assertEquals("youtube", sk.path("news").get(1).path("source").asText());
        assertEquals(1, sk.size());
        assertFalse(sk.has("oldField"));
        assertEquals("cs: Príspevok", output("cs").path("news").get(0).path("title").asText());
        int initialCalls = calls.get();
        generator.generate();
        assertEquals(initialCalls, calls.get());
        youtube.set(youtube(1, "Zmenený názov", "Popis", ""));
        generator.generate();
        assertEquals(initialCalls + 2, calls.get());
        assertEquals("en: Zmenený názov 1", output("en").path("news").get(1).path("title").asText());
    }

    /** One provider failing retains its cache while the other provider still publishes new content. */
    @Test
    void retainsFailedProviderButAcceptsSuccessfulEmptyResponse() throws Exception {
        AtomicReference<byte[]> youtube = new AtomicReference<>(youtube(1, "Video", "Popis", ""));
        AtomicReference<byte[]> facebook = new AtomicReference<>(facebook(1, "Facebook"));
        SocialMediaFeedGenerator generator = generator(youtube, facebook, (text, language) -> text);
        generator.generate();
        facebook.set(null);
        youtube.set(youtube(2, "Nové", "Popis", ""));
        generator.generate();
        JsonNode news = output("sk").path("news");
        assertEquals(3, news.size());
        assertEquals("facebook", news.get(0).path("source").asText());
        assertEquals("youtube:video000002", news.get(1).path("id").asText());
        facebook.set(bytes("{\"data\":[]}"));
        generator.generate();
        assertEquals(2, output("sk").path("news").size());
        assertEquals("youtube", output("sk").path("news").get(0).path("source").asText());
    }

    /** Translation failures use source text or the previous translation and are retried on the next run. */
    @Test
    void doesNotPersistFailedTranslationAsSuccessful() throws Exception {
        AtomicReference<byte[]> youtube = new AtomicReference<>(youtube(1, "Video", "Popis", ""));
        AtomicReference<byte[]> facebook = new AtomicReference<>(bytes("{\"data\":[]}"));
        AtomicInteger calls = new AtomicInteger();
        generator(youtube, facebook, (text, language) -> {
            calls.incrementAndGet();
            return text;
        }).generate();
        assertEquals("Video 1", output("en").path("news").get(0).path("title").asText());
        SocialMediaFeedGenerator successful = generator(youtube, facebook, (text, language) -> {
            calls.incrementAndGet();
            return language + ": " + text;
        });
        successful.generate();
        assertEquals(8, calls.get());
        youtube.set(youtube(1, "Changed", "Popis", ""));
        generator(youtube, facebook, (text, language) -> { throw new IllegalStateException("unavailable"); }).generate();
        assertEquals("en: Video 1", output("en").path("news").get(0).path("title").asText());
        successful.generate();
        assertEquals("en: Changed 1", output("en").path("news").get(0).path("title").asText());
    }

    /** The legacy parser must not receive executable HTML or Markdown image/link delimiters from a social post. */
    @Test
    void escapesPerexWithoutChangingThePlainTitle() throws Exception {
        String payload = "<img src=x onerror='alert(1)'> ![x](javascript:alert(1)) [link](\" onclick=\"alert(1)) & &#91;";
        String escaped = SocialMediaFeedGenerator.escapePerex(payload);
        assertFalse(escaped.contains("<"));
        assertFalse(escaped.contains("["));
        assertFalse(escaped.contains("]"));
        assertFalse(escaped.contains("\""));
        assertFalse(escaped.contains("'"));
        assertTrue(escaped.endsWith("&amp; &amp;#91;"));
        assertEquals(payload, org.jsoup.parser.Parser.unescapeEntities(escaped, false));
        generator(new AtomicReference<>(youtube(1, "Title &lt;safe&gt;", "&lt;img/&gt;", "")),
            new AtomicReference<>(bytes("{\"data\":[]}")), (text, language) -> text).generate();
        assertEquals("Title <safe> 1", output("sk").path("news").get(0).path("title").asText());
        assertEquals("&lt;img/&gt;", output("sk").path("news").get(0).path("perex").asText());
    }

    /** Preview bytes are re-encoded and published under the central origin instead of exposing CDN URLs. */
    @Test
    void cachesThumbnailsOnTheCentralHost() throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB), "png", png);
        AtomicInteger imageRequests = new AtomicInteger();
        SocialMediaFeedGenerator generator = new SocialMediaFeedGenerator(settings(), (uri, token, limit) -> {
            if (uri.getHost().equals("www.youtube.com")) return youtube(1, "Video", "Popis", "https://i2.ytimg.com/vi/video000001/hqdefault.jpg");
            if (uri.getHost().equals("graph.facebook.com")) return bytes("{\"data\":[]}");
            imageRequests.incrementAndGet();
            assertEquals(null, token);
            return png.toByteArray();
        }, (text, language) -> text);
        generator.generate();
        String image = output("sk").path("news").get(0).path("image").asText();
        assertTrue(image.startsWith("https://docs.webjetcms.sk/json/social-media/images/"));
        assertTrue(image.endsWith(".jpg"));
        Path imageFile = directory.resolve("output/social-media/images/" + image.substring(image.lastIndexOf('/') + 1));
        BufferedImage decoded = ImageIO.read(imageFile.toFile());
        assertEquals(640, decoded.getWidth());
        assertEquals(480, decoded.getHeight());
        generator.generate();
        assertEquals(1, imageRequests.get());
        assertThrows(IOException.class, () -> SocialMediaFeedGenerator.thumbnail(bytes("<svg onload='x'/>")));
    }

    /** Unsafe upstream image URLs cannot redirect the generator to arbitrary or local services. */
    @Test
    void rejectsUnapprovedImageHostsAndSchemes() {
        for (String url : List.of("http://i.ytimg.com/x", "https://ytimg.com.evil.invalid/x", "https://evil.invalid/x",
            "https://localhost/x", "https://user:password@i.ytimg.com/x", "https://i.ytimg.com:8443/x")) {
            assertFalse(SocialMediaHttpClient.isImageUrl(URI.create(url)), url);
        }
        assertTrue(SocialMediaHttpClient.isImageUrl(URI.create("https://scontent.fbcdn.net/image.jpg?signature=test")));
        assertTrue(SocialMediaHttpClient.isImageUrl(URI.create("https://i1.ytimg.com/image.jpg")));
    }

    /** A first run with both providers unavailable must preserve an existing publication and create no cache. */
    @Test
    void retainsPreviousPublicationOnInitialProviderFailures() throws Exception {
        Path target = directory.resolve("output/wjnews.sk.json");
        Files.createDirectories(target.getParent());
        Files.writeString(target, "previous");
        SocialMediaFeedGenerator generator = new SocialMediaFeedGenerator(settings(), (uri, token, limit) -> {
            throw new IOException("offline");
        }, (text, language) -> text);
        assertThrows(IOException.class, generator::generate);
        assertEquals("previous", Files.readString(target));
        assertFalse(Files.exists(directory.resolve("private/feed-cache.json")));
    }

    /** Successful empty feeds remove deleted posts without adding release announcements or duplicate placeholders. */
    @Test
    void publishesOnlyAvailablePostsIncludingAnEmptyFeed() throws Exception {
        AtomicReference<byte[]> youtube = new AtomicReference<>(youtube(1, "Video", "Popis", ""));
        AtomicReference<byte[]> facebook = new AtomicReference<>(null);
        SocialMediaFeedGenerator generator = generator(youtube, facebook, (text, language) -> text);
        generator.generate();
        for (String language : List.of("sk", "cs", "en")) {
            assertEquals(1, output(language).path("news").size());
            assertEquals("youtube", output(language).path("news").get(0).path("source").asText());
        }
        youtube.set(youtube(0, "", "", ""));
        facebook.set(bytes("{\"data\":[]}"));
        generator.generate();
        for (String language : List.of("sk", "cs", "en")) {
            assertEquals(0, output(language).path("news").size());
            assertEquals(1, output(language).size());
        }
    }

    /** The fourth video survives provider failures and Facebook visibility changes without repeating its translations. */
    @Test
    void usesFourYoutubeVideosOnlyWhenFacebookHasNoPosts() throws Exception {
        AtomicReference<byte[]> youtube = new AtomicReference<>(youtube(5, "Video", "Popis", ""));
        AtomicReference<byte[]> facebook = new AtomicReference<>(null);
        AtomicInteger calls = new AtomicInteger();
        SocialMediaFeedGenerator generator = generator(youtube, facebook, (text, language) -> {
            calls.incrementAndGet();
            return language + ": " + text;
        });
        generator.generate();
        assertEquals(4, output("sk").path("news").size());
        assertEquals("youtube:video000002", output("sk").path("news").get(3).path("id").asText());
        assertEquals(16, calls.get());
        youtube.set(null);
        generator.generate();
        assertEquals(4, output("sk").path("news").size());
        assertEquals("youtube:video000002", output("sk").path("news").get(3).path("id").asText());
        assertEquals(16, calls.get());
        facebook.set(facebook(4, "Facebook"));
        generator.generate();
        JsonNode news = output("sk").path("news");
        assertEquals(6, news.size());
        int youtubeCount = 0;
        int facebookCount = 0;
        for (JsonNode post : news) {
            if ("youtube".equals(post.path("source").asText())) youtubeCount++;
            if ("facebook".equals(post.path("source").asText())) facebookCount++;
            assertFalse("youtube:video000002".equals(post.path("id").asText()));
        }
        assertEquals(3, youtubeCount);
        assertEquals(3, facebookCount);
        assertEquals(28, calls.get());
        facebook.set(bytes("{\"data\":[]}"));
        generator.generate();
        assertEquals(4, output("sk").path("news").size());
        assertEquals("youtube:video000002", output("sk").path("news").get(3).path("id").asText());
        assertEquals(28, calls.get());
        assertEquals(16, MAPPER.readTree(Files.readString(directory.resolve("private/feed-cache.json"))).path("translations").size());
        youtube.set(youtube(0, "", "", ""));
        generator.generate();
        assertEquals(0, MAPPER.readTree(Files.readString(directory.resolve("private/feed-cache.json"))).path("translations").size());
    }

    /** An oversized generated public feed cannot replace output that the browser can already consume. */
    @Test
    void rejectsPublicFeedsAboveTheBrowserLimit() throws Exception {
        String previous = "{\"news\":[]}";
        SocialMediaFeedGenerator.Post oversized = new SocialMediaFeedGenerator.Post("youtube:video000001", "youtube", "Video",
            "x".repeat(256 * 1024), "https://www.youtube.com/watch?v=video000001", "2026-10-05T09:00:00Z", "", null);
        Path cacheFile = directory.resolve("private/feed-cache.json");
        Files.createDirectories(cacheFile.getParent());
        byte[] cache = MAPPER.writeValueAsBytes(new SocialMediaFeedGenerator.FeedCache(Map.of("youtube", List.of(oversized)), Map.of()));
        Files.write(cacheFile, cache);
        Path output = directory.resolve("output/wjnews.sk.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, previous);
        SocialMediaFeedGenerator generator = generator(new AtomicReference<>(null),
            new AtomicReference<>(null), (text, language) -> text);
        assertThrows(IOException.class, generator::generate);
        assertEquals(previous, Files.readString(output));
        assertEquals(new String(cache, StandardCharsets.UTF_8), Files.readString(cacheFile));
    }

    /** Failure while staging a later file preserves an already existing first publication. */
    @Test
    void stagesEveryFileBeforeReplacingTargets() throws Exception {
        Path first = directory.resolve("feed.json");
        Path invalidParent = directory.resolve("not-a-directory");
        Files.writeString(first, "old");
        Files.writeString(invalidParent, "file");
        Map<Path, byte[]> files = new LinkedHashMap<>();
        files.put(first, bytes("new"));
        files.put(invalidParent.resolve("other.json"), bytes("other"));
        assertThrows(IOException.class, () -> SocialMediaFeedGenerator.publishFiles(files));
        assertEquals("old", Files.readString(first));
        try (var paths = Files.list(directory)) {
            assertFalse(paths.anyMatch(path -> path.getFileName().toString().startsWith(".social-media-")));
        }
        SocialMediaFeedGenerator.publishFiles(Map.of(first, bytes("published")));
        assertEquals("published", Files.readString(first));
    }

    /** A plaintext token fails the encryption guard and settings never disclose the runtime token in their representation. */
    @Test
    void requiresEncryptedTokenStorage() {
        assertFalse(SocialMediaFeedGenerator.Settings.isEncryptedToken(null));
        assertFalse(SocialMediaFeedGenerator.Settings.isEncryptedToken("plain-secret"));
        assertFalse(SocialMediaFeedGenerator.Settings.isEncryptedToken("encrypted:"));
        assertTrue(SocialMediaFeedGenerator.Settings.isEncryptedToken("encrypted:" + "ciphertext".repeat(4)));
        assertFalse(settings().toString().contains("secret-token"));
    }

    private SocialMediaFeedGenerator generator(AtomicReference<byte[]> youtube, AtomicReference<byte[]> facebook,
        SocialMediaFeedGenerator.Translator translator) {
        return new SocialMediaFeedGenerator(settings(), (uri, token, limit) -> {
            byte[] response;
            if (uri.getHost().equals("www.youtube.com")) {
                assertEquals(null, token);
                response = youtube.get();
            } else {
                assertEquals("/v26.0/" + PAGE + "/published_posts", uri.getPath());
                assertEquals("secret-token", token);
                assertFalse(uri.toString().contains("secret-token"));
                response = facebook.get();
            }
            if (response == null) throw new IOException("provider unavailable");
            return response;
        }, translator);
    }

    private SocialMediaFeedGenerator.Settings settings() {
        return new SocialMediaFeedGenerator.Settings(directory.resolve("output"),
            directory.resolve("private/feed-cache.json"), URI.create("https://docs.webjetcms.sk/json/"), CHANNEL, PAGE, "secret-token", "v26.0");
    }

    private JsonNode output(String language) throws IOException {
        return MAPPER.readTree(Files.readAllBytes(directory.resolve("output/wjnews." + language + ".json")));
    }

    private static byte[] youtube(int count, String title, String description, String image) {
        StringBuilder xml = new StringBuilder("<feed xmlns='http://www.w3.org/2005/Atom' xmlns:yt='http://www.youtube.com/xml/schemas/2015' xmlns:media='http://search.yahoo.com/mrss/'>")
            .append("<yt:channelId>").append(CHANNEL).append("</yt:channelId>");
        for (int index = 1; index <= count; index++) {
            xml.append("<entry><yt:videoId>video00000").append(index).append("</yt:videoId><yt:channelId>")
                .append(CHANNEL).append("</yt:channelId><title>").append(title).append(' ').append(index)
                .append("</title><published>2026-10-0").append(index).append("T10:00:00Z</published>")
                .append("<updated>2026-10-18T10:00:00Z</updated><media:group><media:description>").append(description)
                .append("</media:description><media:thumbnail url='").append(image).append("'/></media:group></entry>");
        }
        return bytes(xml.append("</feed>").toString());
    }

    private static byte[] facebook(int count, String text) throws IOException {
        ObjectNode feed = MAPPER.createObjectNode();
        ArrayNode data = feed.putArray("data");
        for (int index = 1; index <= count; index++) data.add(facebookPost(Integer.toString(index), text, "2026-10-05T09:00:00+0000"));
        return MAPPER.writeValueAsBytes(feed);
    }

    private static ObjectNode facebookPost(String id, String text, String date) {
        return MAPPER.createObjectNode().put("id", PAGE + "_" + id).put("message", text).put("created_time", date)
            .put("permalink_url", "https://www.facebook.com/" + PAGE + "/posts/" + id);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
