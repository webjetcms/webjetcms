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
            Arguments.of("<p style='color: red' class=\"some-class\" onclick='test();'>This is a safe paragraph <img src='/images/logo.png' align='right'>.</p>", "<p style=\"color:red\" class=\"some-class\">This is a safe paragraph <img src=\"/images/logo.png\" />.</p>"),
            Arguments.of("<script>alert('XSS attack');</script>", ""),
            Arguments.of("<a href=\"https://example.com\" target='_blank'>Link</a>", "<a href=\"https://example.com\">Link</a>"),
            Arguments.of("<p>A + B</p>", "<p>A + B</p>"),
            Arguments.of("<p>5+3=8</p>", "<p>5+3&#61;8</p>"),
            Arguments.of(
                "<a class='btn' role='button' data-a11y-dialog-show='modal'>Open</a>",
                "<a class=\"btn\" role=\"button\" data-a11y-dialog-show=\"modal\">Open</a>"
            ),
            Arguments.of(
                "<div role='status' aria-live='polite' data-custom-name='value'><span aria-hidden='true' data-x='1'>Ready</span></div>",
                "<div role=\"status\" aria-live=\"polite\" data-custom-name=\"value\"><span aria-hidden=\"true\" data-x=\"1\">Ready</span></div>"
            ),
            Arguments.of(
                "<a role='button' aria-controls='modal' aria-expanded='false' tabindex='0'>Open</a>",
                "<a role=\"button\" aria-controls=\"modal\" aria-expanded=\"false\" tabindex=\"0\">Open</a>"
            ),
            Arguments.of(
                "<div id='modal' role='dialog' aria-labelledby='heading' tabindex='-1'><h2 id='heading' title='Details' lang='en' dir='auto'>Details</h2></div>",
                "<div id=\"modal\" role=\"dialog\" aria-labelledby=\"heading\" tabindex=\"-1\"><h2 id=\"heading\" title=\"Details\" lang=\"en\" dir=\"auto\">Details</h2></div>"
            ),
            Arguments.of(
                "<p dir='ltr'>Left</p><p dir='RTL'>Right</p>",
                "<p dir=\"ltr\">Left</p><p dir=\"rtl\">Right</p>"
            ),
            Arguments.of(
                "<p tabindex='1'>Positive</p><p tabindex='-2'>Negative</p><p tabindex='invalid' dir='invalid'>Invalid</p>",
                "<p>Positive</p><p>Negative</p><p>Invalid</p>"
            ),
            Arguments.of(
                "<img src='/images/logo.png' alt='Logo' role='img' aria-describedby='description' data-image-id='42'>",
                "<img src=\"/images/logo.png\" alt=\"Logo\" role=\"img\" aria-describedby=\"description\" data-image-id=\"42\" />"
            ),
            Arguments.of(
                "<ul data-items='2'><li data-list='bullet'>First</li><li data-list='bullet'>Second</li></ul>",
                "<ul data-items=\"2\"><li data-list=\"bullet\">First</li><li data-list=\"bullet\">Second</li></ul>"
            ),
            Arguments.of(
                "<span DATA-CUSTOM='value' ARIA-LABEL='Example' ROLE='note'>Text</span>",
                "<span data-custom=\"value\" aria-label=\"Example\" role=\"note\">Text</span>"
            ),
            // OWASP's renderer rejects attribute names containing dots even when explicitly allowed.
            Arguments.of(
                "<span data-empty='' data-custom_name='one' data-custom.value='two'>Text</span>",
                "<span data-empty=\"\" data-custom_name=\"one\">Text</span>"
            ),
            Arguments.of(
                "<p data='a' data-='b' datafoo='c' aria-='d' ariafoo='e' custom='f'>Text</p>",
                "<p>Text</p>"
            ),
            Arguments.of(
                "<a href='javascript:alert(1)' role='button' aria-label='Open' data-a11y-dialog-show='modal' onclick='alert(1)'>Open</a>",
                "<a role=\"button\" aria-label=\"Open\" data-a11y-dialog-show=\"modal\">Open</a>"
            ),
            Arguments.of(
                "<img src='javascript:alert(1)' data-image-id='42' onerror='alert(1)'>",
                "<img data-image-id=\"42\" />"
            ),
            Arguments.of(
                "<script data-x='1' aria-label='Script'>alert(1)</script><button role='button' data-a11y-dialog-show='modal'>Open</button>",
                "Open"
            ),
            Arguments.of(
                "<span data-info='&quot; onmouseover=&quot;alert(1)' aria-label='A &amp; B'>Text</span>",
                "<span data-info=\"&#34; onmouseover&#61;&#34;alert(1)\" aria-label=\"A &amp; B\">Text</span>"
            )
        );
    }
}
