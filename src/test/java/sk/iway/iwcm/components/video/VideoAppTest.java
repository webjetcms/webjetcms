package sk.iway.iwcm.components.video;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class VideoAppTest {

    private static final String PLAYER_PARAMETERS = "enablejsapi=1&showinfo=0&autoplay=0&modestbranding=0&controls=1&rel=1&origin=https%3A%2F%2Fexample.com";

    @Test
    void sharedUrlPreservesStartTimeAndBuildsOneQuery() throws URISyntaxException {
        String result = VideoApp.getYoutubeEmbedUrl(
            "https://youtu.be/q8xs3qDq-G4?si=6uc7EwqSIvciV14s&t=115", PLAYER_PARAMETERS);

        URI uri = URI.create(result);
        assertEquals("www.youtube.com", uri.getHost());
        assertEquals("/embed/q8xs3qDq-G4", uri.getPath());
        assertEquals(List.of("115"), values(result, "start"));
        assertEquals(List.of("6uc7EwqSIvciV14s"), values(result, "si"));
        assertEquals(List.of("https://example.com"), values(result, "origin"));
        assertEquals(List.of(), values(result, "t"));
        assertFalse(uri.getRawQuery().contains("?"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://www.youtube.com/watch?v=q8xs3qDq-G4",
        "https://youtu.be/q8xs3qDq-G4",
        "http://www.youtube.com/v/q8xs3qDq-G4",
        "http://www.youtu.be/watch?v=q8xs3qDq-G4",
        "http://www.youtu.be/v/q8xs3qDq-G4",
        "https://www.youtube.com/shorts/q8xs3qDq-G4",
        "https://www.youtube.com/embed/q8xs3qDq-G4",
        "//www.youtube.com/watch?v=q8xs3qDq-G4",
        " https://youtu.be/q8xs3qDq-G4 ",
        "https://youtu.be/q8xs3qDq-G4#details"
    })
    void supportedUrlsKeepTheVideoAndDefaultSettings(String url) {
        assertEquals("//www.youtube.com/embed/q8xs3qDq-G4?" + PLAYER_PARAMETERS,
            VideoApp.getYoutubeEmbedUrl(url, PLAYER_PARAMETERS));
    }

    @ParameterizedTest
    @CsvSource({
        "t=115, 115",
        "start=123, 123",
        "t=115s, 115",
        "t=1m55s, 115",
        "t=2m, 120",
        "t=1h2m3s, 3723",
        "t=1h, 3600",
        "t=0, 0",
        "t=115&start=123, 123",
        "start=0&t=115, 0",
        "start=invalid&t=115, 115",
        "t=%31%31%35, 115"
    })
    void playbackTimeIsNormalizedToSeconds(String query, String expected) throws URISyntaxException {
        String result = VideoApp.getYoutubeEmbedUrl("https://youtu.be/q8xs3qDq-G4?" + query, PLAYER_PARAMETERS);

        assertEquals(List.of(expected), values(result, "start"));
        assertEquals(List.of(), values(result, "t"));
    }

    @Test
    void watchUrlHandlesReorderedAndHtmlEncodedParameters() throws URISyntaxException {
        String result = VideoApp.getYoutubeEmbedUrl(
            "https://www.youtube.com/watch?si=share&amp;t=115&amp;v=q8xs3qDq-G4&amp;ab_channel=WebJET#details",
            PLAYER_PARAMETERS);

        assertEquals("/embed/q8xs3qDq-G4", URI.create(result).getPath());
        assertEquals(List.of("115"), values(result, "start"));
        assertEquals(List.of("WebJET"), values(result, "ab_channel"));
        assertEquals(List.of(), values(result, "v"));
        assertFalse(result.contains("#"));
    }

    @Test
    void componentSettingsOverrideDuplicatesAndKeepOtherParameters() throws URISyntaxException {
        String result = VideoApp.getYoutubeEmbedUrl(
            "https://youtu.be/q8xs3qDq-G4?autoplay=1&controls=0&controls=0&enablejsapi=0&origin=https%3A%2F%2Fother.example&start=123&end=150",
            PLAYER_PARAMETERS);

        assertEquals(List.of("0"), values(result, "autoplay"));
        assertEquals(List.of("1"), values(result, "controls"));
        assertEquals(List.of("1"), values(result, "enablejsapi"));
        assertEquals(List.of("https://example.com"), values(result, "origin"));
        assertEquals(List.of("123"), values(result, "start"));
        assertEquals(List.of("150"), values(result, "end"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-1", "1.5", "invalid", "1m55", "2147483648", "999999999999999999999", "2147483647h", "%22onload%3D%22alert(1)"})
    void invalidTimeDoesNotBreakThePlayer(String time) throws URISyntaxException {
        String result = VideoApp.getYoutubeEmbedUrl("https://youtu.be/q8xs3qDq-G4?t=" + time, PLAYER_PARAMETERS);

        assertEquals("/embed/q8xs3qDq-G4", URI.create(result).getPath());
        assertEquals(List.of(), values(result, "start"));
        assertEquals(List.of(), values(result, "t"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "https://youtu.be/", "https://youtu.be/%22onload%3D%22alert(1)", "https://youtu.be/q8xs3qDq-G4?t=%"})
    void invalidUrlsDoNotProduceAnEmbed(String url) {
        assertEquals("", VideoApp.getYoutubeEmbedUrl(url, PLAYER_PARAMETERS));
    }

    private List<String> values(String url, String name) throws URISyntaxException {
        return new URIBuilder(url).getQueryParams().stream()
            .filter(parameter -> name.equals(parameter.getName()))
            .map(NameValuePair::getValue)
            .toList();
    }
}
