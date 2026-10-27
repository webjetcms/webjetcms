package sk.iway.iwcm.system.jpa;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;

/** Covers title normalization shared by JPA, JSON binding and legacy JDBC hydration. */
class PageTitleAttributeConverterTest {
    /** Both persistence directions and entity setters remove executable HTML after legacy decoding. */
    @ParameterizedTest
    @MethodSource("titles")
    void sanitizesEveryTitleEntryPoint(String input, String expected) {
        PageTitleAttributeConverter converter = new PageTitleAttributeConverter();
        assertEquals(expected, converter.convertToDatabaseColumn(input));
        assertEquals(expected, converter.convertToEntityAttribute(input));
        assertEquals(expected, converter.convertToDatabaseColumn(expected), "Sanitization must be idempotent");
        if (input == null || input.isEmpty()) return;
        for (var doc : new sk.iway.iwcm.doc.DocBasic[] {new DocDetails(), new DocHistory()}) {
            doc.setTitle(input);
            doc.setNavbar(input);
            assertEquals(expected, doc.getTitle());
            assertEquals(expected, doc.getNavbar());
            assertEquals(expected, Tools.convertToHtmlTags(doc.getTitle()), "Rendering must not reactivate a payload");
            assertEquals(expected, Tools.convertToHtmlTags(doc.getNavbar()), "Navigation rendering must not reactivate a payload");
        }
    }

    private static Stream<Arguments> titles() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of("", ""),
            Arguments.of("Žluťoučký kôň + text", "Žluťoučký kôň + text"),
            Arguments.of("autotest <strong>safe</strong>", "autotest <strong>safe</strong>"),
            Arguments.of("autotest <img src=x onerror=alert('WJ14-TITLE')>", "autotest <img src=\"x\" />"),
            Arguments.of("autotest *|img src=x onerror=alert(1)|*", "autotest <img src=\"x\" />"),
            Arguments.of("autotest *|strong onclick=alert(1)|*safe*||strong|*", "autotest <strong>safe</strong>"),
            Arguments.of("autotest *|script|*alert(1)*||script|*", "autotest "),
            Arguments.of("autotest &#42;|script|&#42;alert(1)&#42;||script|&#42;", "autotest &#42;|script|&#42;alert(1)&#42;||script|&#42;"),
            Arguments.of("autotest &#x2a;|script|&#x2a;alert(1)&#x2a;||script|&#x2a;", "autotest &#42;|script|&#42;alert(1)&#42;||script|&#42;"),
            Arguments.of("autotest *&#124;script&#124;*alert(1)*&#124;&#124;script&#124;*", "autotest &#42;|script|&#42;alert(1)&#42;||script|&#42;"),
            Arguments.of("autotest &#42;|strong|&#42;literal&#42;||strong|&#42;", "autotest &#42;|strong|&#42;literal&#42;||strong|&#42;"),
            Arguments.of("autotest &lt;script&gt;literal&lt;/script&gt;", "autotest &lt;script&gt;literal&lt;/script&gt;"),
            Arguments.of("autotest &lt;b onclick=alert(1)&gt;safe&lt;&#47;b&gt;", "autotest <b>safe</b>"),
            Arguments.of("autotest <script>alert(1)</script><a href='javascript:alert(1)'>link</a>", "autotest link"),
            Arguments.of("autotest &lt;img src=x onerror=alert(1)&gt;", "autotest &lt;img src&#61;x onerror&#61;alert(1)&gt;")
        );
    }
}
