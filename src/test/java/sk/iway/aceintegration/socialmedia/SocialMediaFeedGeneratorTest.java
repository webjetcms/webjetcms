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
    private static final String PAGE = "237316516345904";
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

    /** YouTube omits the UC prefix on its feed-level channel ID while entries still require the full configured ID. */
    @Test
    void acceptsUnprefixedFeedChannelIdButRequiresFullEntryIds() throws Exception {
        String fullId = "<yt:channelId>" + CHANNEL + "</yt:channelId>";
        String feedId = "<yt:channelId>" + CHANNEL.substring(2) + "</yt:channelId>";
        String feed = new String(youtube(1, "Video", "Text", ""), StandardCharsets.UTF_8).replaceFirst(fullId, feedId);
        List<SocialMediaFeedGenerator.Post> posts = SocialMediaFeedGenerator.parseYoutube(bytes(feed), CHANNEL);
        assertEquals(1, posts.size());
        assertEquals("youtube:video000001", posts.get(0).id());
        assertThrows(IOException.class, () -> SocialMediaFeedGenerator.parseYoutube(bytes(feed.replace(feedId,
            "<yt:channelId>another-channel</yt:channelId>")), CHANNEL));
        assertThrows(IOException.class, () -> SocialMediaFeedGenerator.parseYoutube(bytes(feed.replace(fullId, feedId)), CHANNEL));
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

    /** Summaries remove a repeated emoji title, social links and markup without discarding the useful next sentence. */
    @Test
    void summarizesSocialTextWithoutRepeatingItsTitle() throws Exception {
        String title = "🚀 Novinky vo WebJET CMS";
        String body = title + "\n\n## Editor prináša nové možnosti.\n- Pozrite si https://example.com/video a www.example.com. #WebJET #CMS";
        assertEquals("Editor prináša nové možnosti.", SocialMediaFeedGenerator.summarize(body, title));
        assertEquals("", SocialMediaFeedGenerator.summarize("  🚀 Novinky\tvo WebJET CMS  ", title));
        assertEquals("JavaScript changes are ready.", SocialMediaFeedGenerator.summarize("JavaScript changes are ready.", "Java"));
        assertEquals("Súbory nájdete", SocialMediaFeedGenerator.summarize("Súbory nájdete ftp://example.com/file https://example.com www.example.com #CMS", "Iný názov"));
        generator(new AtomicReference<>(youtube(0, "", "", "")), new AtomicReference<>(facebook(1, body)),
            (text, language) -> text).generate();
        JsonNode post = output("sk").path("news").get(0);
        assertEquals(title, post.path("title").asText());
        assertEquals("Editor prináša nové možnosti.", post.path("perex").asText());
        generator(new AtomicReference<>(youtube(0, "", "", "")), new AtomicReference<>(facebook(1, title)),
            (text, language) -> text).generate();
        assertEquals("", output("sk").path("news").get(0).path("perex").asText());

        String longFirstLine = "🚀 " + "Nové možnosti nastavenia textu a kontajnera vo WebJET CMS ".repeat(5).stripTrailing();
        String truncatedTitle = longFirstLine.substring(0, longFirstLine.offsetByCodePoints(0, 119)).stripTrailing() + "…";
        String longBody = longFirstLine + "\n\nNastavte si vlastný vzhľad stránky. Podrobnosti nájdete vo videu.";
        assertEquals("Nastavte si vlastný vzhľad stránky.", SocialMediaFeedGenerator.summarize(longBody, truncatedTitle));
        assertEquals("", SocialMediaFeedGenerator.summarize(longFirstLine, truncatedTitle));
        generator(new AtomicReference<>(youtube(0, "", "", "")), new AtomicReference<>(facebook(1, longBody)),
            (text, language) -> text).generate();
        post = output("sk").path("news").get(0);
        assertEquals(truncatedTitle, post.path("title").asText());
        assertEquals("Nastavte si vlastný vzhľad stránky.", post.path("perex").asText());
        generator(new AtomicReference<>(youtube(0, "", "", "")), new AtomicReference<>(facebook(1, longFirstLine)),
            (text, language) -> text).generate();
        assertEquals("", output("sk").path("news").get(0).path("perex").asText());
    }

    /** Long summaries preserve Unicode code points and end at a word boundary when one is available. */
    @Test
    void boundsSummariesWithoutSplittingUnicodeOrWords() {
        String emojiSummary = SocialMediaFeedGenerator.summarize("🚀".repeat(200), "Unrelated title");
        assertEquals(180, emojiSummary.codePointCount(0, emojiSummary.length()));
        assertTrue(emojiSummary.endsWith("…"));
        assertTrue(emojiSummary.codePoints().noneMatch(value -> value >= 0xD800 && value <= 0xDFFF));
        String longText = "Bezpečná aktualizácia systému ".repeat(15);
        String wordSummary = SocialMediaFeedGenerator.summarize(longText, "Unrelated title");
        assertTrue(wordSummary.codePointCount(0, wordSummary.length()) <= 180);
        assertTrue(wordSummary.endsWith("…"));
        assertTrue(longText.startsWith(wordSummary.substring(0, wordSummary.length() - 1).stripTrailing() + " "));
        assertFalse(wordSummary.contains("\n"));
    }

    /** Translation receives a concise description and cannot reintroduce a repeated title or links into the published summary. */
    @Test
    void summarizesBeforeAndAfterTranslationWithoutRepeatingTranslationCalls() throws Exception {
        String title = "🚀 Nové možnosti";
        AtomicInteger calls = new AtomicInteger();
        SocialMediaFeedGenerator generator = generator(new AtomicReference<>(youtube(0, "", "", "")),
            new AtomicReference<>(facebook(1, title + "\nEditor teraz podporuje zmeny. https://example.com #WebJET")),
            (text, language) -> {
                calls.incrementAndGet();
                String translatedTitle = language + " translated title";
                if (text.equals(title)) return translatedTitle;
                assertEquals("Editor teraz podporuje zmeny.", text);
                return translatedTitle + "\nThe editor now supports changes. See https://example.com #WebJET";
            });
        generator.generate();
        assertEquals(4, calls.get());
        assertEquals("The editor now supports changes.", output("en").path("news").get(0).path("perex").asText());
        generator.generate();
        assertEquals(4, calls.get());
        assertEquals("The editor now supports changes.", output("en").path("news").get(0).path("perex").asText());
    }

    /** Old cached source and translated fallback descriptions are shortened even when both providers and translation are offline. */
    @Test
    void summarizesOldCachedDescriptionsAndTranslationFallbacks() throws Exception {
        String title = "🚀 Aktualizácia WebJET";
        SocialMediaFeedGenerator.Post post = new SocialMediaFeedGenerator.Post("facebook:" + PAGE + "_1", "facebook", title,
            title + "\nZmeny sú už dostupné. " + "Dlhý text ".repeat(100) + "https://example.com #WebJET",
            "https://www.facebook.com/" + PAGE + "/posts/1", "2026-10-05T09:00:00Z", "", null);
        String translatedTitle = "🚀 WebJET update";
        Map<String, SocialMediaFeedGenerator.Translation> translations = Map.of(
            post.id() + ":en:title", new SocialMediaFeedGenerator.Translation("old-source-hash", translatedTitle),
            post.id() + ":en:perex", new SocialMediaFeedGenerator.Translation("old-source-hash",
                translatedTitle + "\nThe changes are available. " + "More details ".repeat(100) + "https://example.com"));
        writeCache(post, translations);
        SocialMediaFeedGenerator generator = generator(new AtomicReference<>(null), new AtomicReference<>(null),
            (text, language) -> { throw new IllegalStateException("translation offline"); });
        generator.generate();
        assertEquals("Zmeny sú už dostupné.", output("sk").path("news").get(0).path("perex").asText());
        assertEquals(translatedTitle, output("en").path("news").get(0).path("title").asText());
        assertEquals("The changes are available.", output("en").path("news").get(0).path("perex").asText());
        generator.generate();
        assertEquals("The changes are available.", output("en").path("news").get(0).path("perex").asText());
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

    /** Preview bytes are re-encoded and published through portable relative paths instead of exposing CDN URLs. */
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
        assertTrue(image.matches("social-media/images/[a-f0-9]{32}\\.jpg"));
        Path imageFile = directory.resolve("output").resolve(image);
        BufferedImage decoded = ImageIO.read(imageFile.toFile());
        assertEquals(640, decoded.getWidth());
        assertEquals(480, decoded.getHeight());
        generator.generate();
        assertEquals(1, imageRequests.get());
        assertThrows(IOException.class, () -> SocialMediaFeedGenerator.thumbnail(bytes("<svg onload='x'/>")));
    }

    /** Existing owned absolute image paths migrate without downloads while traversal and unrelated cached paths are omitted. */
    @Test
    void normalizesOwnedCachedImagesAndRejectsUnrelatedPaths() throws Exception {
        String remoteImage = "https://i2.ytimg.com/vi/video000001/hqdefault.jpg";
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", png);
        SocialMediaFeedGenerator initial = new SocialMediaFeedGenerator(settings(), (uri, token, limit) -> {
            if (uri.getHost().equals("www.youtube.com")) return youtube(1, "Video", "Popis", remoteImage);
            if (uri.getHost().equals("graph.facebook.com")) return bytes("{\"data\":[]}");
            return png.toByteArray();
        }, (text, language) -> text);
        initial.generate();
        String relativeImage = output("sk").path("news").get(0).path("image").asText();
        assertTrue(relativeImage.matches("social-media/images/[a-f0-9]{32}\\.jpg"));
        SocialMediaFeedGenerator.Post post = new SocialMediaFeedGenerator.Post("youtube:video000001", "youtube", "Video 1", "Popis",
            "https://www.youtube.com/watch?v=video000001", "2026-10-01T10:00:00Z", remoteImage,
            "https://docs.webjetcms.sk/json/" + relativeImage);
        writeCache(post, Map.of());
        SocialMediaFeedGenerator offline = generator(new AtomicReference<>(null), new AtomicReference<>(null), (text, language) -> text);
        offline.generate();
        assertEquals(relativeImage, output("sk").path("news").get(0).path("image").asText());
        assertEquals(relativeImage, MAPPER.readTree(Files.readString(directory.resolve("private/feed-cache.json")))
            .path("sources").path("youtube").get(0).path("image").asText());
        offline.generate();
        assertEquals(relativeImage, output("sk").path("news").get(0).path("image").asText());
        for (String invalid : List.of("social-media/images/../" + relativeImage.substring(relativeImage.lastIndexOf('/') + 1),
            "https://external.example/other-image.jpg", "social-media/images/" + "0".repeat(32) + ".jpg")) {
            writeCache(new SocialMediaFeedGenerator.Post(post.id(), post.source(), post.title(), post.perex(), post.link(),
                post.publishedAt(), post.remoteImage(), invalid), Map.of());
            offline.generate();
            assertFalse(output("sk").path("news").get(0).has("image"), invalid);
        }
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
        facebook.set(facebook(4, "Facebook\nDetaily príspevku."));
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
        SocialMediaFeedGenerator.Post oversized = new SocialMediaFeedGenerator.Post("youtube:video000001", "youtube", "x".repeat(256 * 1024),
            "Popis", "https://www.youtube.com/watch?v=video000001", "2026-10-05T09:00:00Z", "", null);
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
            directory.resolve("private/feed-cache.json"), CHANNEL, PAGE, "secret-token", "v26.0");
    }

    private void writeCache(SocialMediaFeedGenerator.Post post, Map<String, SocialMediaFeedGenerator.Translation> translations) throws IOException {
        Path cache = directory.resolve("private/feed-cache.json");
        Files.createDirectories(cache.getParent());
        Files.write(cache, MAPPER.writeValueAsBytes(new SocialMediaFeedGenerator.FeedCache(Map.of(post.source(), List.of(post)), translations)));
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
