package sk.iway.iwcm;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class SendMailTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
        "",
        "<html><body><p>No images here!</p></body></html>",
        "<img src=\"\"><img>"
    })
    void testGetInlineAttachments_emptyResults(String html) {
        List<String> result = SendMail.getInlineAttachments(html);
        assertThat(result, is(empty()));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("provideHtmlAndExpectedResults")
    void testGetInlineAttachments_parametrized(String html, List<String> expected) {
        List<String> result = SendMail.getInlineAttachments(html);
        assertThat(result, contains(expected.toArray()));
    }

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> provideHtmlAndExpectedResults() {
        return java.util.stream.Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(
                "<html><body><table background=\"bg1.png\"></table></body></html>",
                java.util.List.of("bg1.png")
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "<div><img src=\"img1.png\"></div>",
                java.util.List.of("img1.png")
            )
        );
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("provideHtmlAndExpectedResultsExtended")
    void testGetInlineAttachments_variousCases(String html, List<String> expected) {
        List<String> result = SendMail.getInlineAttachments(html);
        assertThat(result, containsInAnyOrder(expected.toArray()));
    }

    private static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> provideHtmlAndExpectedResultsExtended() {
        return java.util.stream.Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(
                "<html><body><img src=\"image1.png\"><img src='image2.jpg'></body></html>",
                java.util.List.of("image1.png", "image2.jpg")
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "<img src=\"img.png\"><table background=\"bg.png\"></table>",
                java.util.List.of("img.png", "bg.png")
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "<img src=\"img1.png\"><img src=\"\"><table background=\"bg1.jpg\"></table><table background=\"\"></table>",
                java.util.List.of("img1.png", "bg1.jpg")
            ),
            org.junit.jupiter.params.provider.Arguments.of(
                "<div><img src=\"img1.png\"><table><tr><td><img src=\"img2.jpg\"></td></tr></table></div>",
                java.util.List.of("img1.png", "img2.jpg")
            )
        );
    }
}