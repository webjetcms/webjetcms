package sk.iway.iwcm.system.jpa;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AllowSafeHtmlAttributeConverterTest {

    @ParameterizedTest
    @MethodSource("provideHtmlForTesting")
    void testSanitize(String unsafeHtml, String expectedSafeHtml) {
        String actualSafeHtml = AllowSafeHtmlAttributeConverter.sanitize(unsafeHtml);
        assertEquals(expectedSafeHtml, actualSafeHtml);
    }

    private static Stream<Arguments> provideHtmlForTesting() {
        return Stream.of(
            Arguments.of(null, ""),
            Arguments.of("", ""),
            Arguments.of("<p>This is a safe paragraph.</p>", "<p>This is a safe paragraph.</p>"),
            Arguments.of("<p>This is&lt;a safe&nbsp;paragraph.</p>", "<p>This is&lt;a safe&nbsp;paragraph.</p>"),
            Arguments.of("<p style='color: red' class=\"some-class\" onclick='test();'>This is a safe paragraph <img src='/images/logo.png' align='right'>.</p>", "<p style=\"color:red\">This is a safe paragraph <img src=\"/images/logo.png\" />.</p>"),
            Arguments.of("<script>alert('XSS attack');</script>", ""),
            Arguments.of("<a href=\"https://example.com\" target='_blank'>Link</a>", "<a href=\"https://example.com\">Link</a>")
        );
    }
}