package sk.iway.iwcm.system.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import sk.iway.iwcm.doc.DocBasic;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;

/** Covers safe HTML persistence and the setters used outside JPA. */
class AllowSafeHtmlAttributeConverterWithSaveTest {

    /** Writes, legacy reads and repeated saves retain formatting without executable HTML. */
    @ParameterizedTest
    @MethodSource("htmlFragments")
    void sanitizesBothDirectionsAndJdbcSetters(String input, String expected) {
        var converter = new AllowSafeHtmlAttributeConverterWithSave();
        assertEquals(expected, converter.convertToDatabaseColumn(input));
        assertEquals(expected, converter.convertToEntityAttribute(input));
        assertEquals(expected, converter.convertToDatabaseColumn(converter.convertToEntityAttribute(expected)));
        assertEquals(expected, AllowSafeHtmlAttributeConverterWithSave.sanitize(input));
        if (input == null || input.isEmpty()) return;
        for (DocBasic doc : new DocBasic[] {new DocDetails(), new DocHistory()}) {
            doc.setFileName(input);
            assertEquals(expected, doc.getFileName());
        }
    }

    private static Stream<Arguments> htmlFragments() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of("", ""),
            Arguments.of("/Priečinok/Žltá + modrá", "/Priečinok/Žltá + modrá"),
            Arguments.of("/<b>Folder</b>", "/<b>Folder</b>"),
            Arguments.of("/<b onclick='alert(1)'>Folder</b><script>alert(1)</script>", "/<b>Folder</b>"),
            Arguments.of("<img src=x onerror=alert(1)>", "<img src=\"x\" />"),
            Arguments.of("<a href='javascript:alert(1)'>Folder</a>", "Folder"),
            Arguments.of("<span data-name='folder' aria-label='Folder'>Folder</span>", "<span data-name=\"folder\" aria-label=\"Folder\">Folder</span>"),
            Arguments.of("A &amp; B &lt; C", "A &amp; B &lt; C"),
            Arguments.of("*|b|*Folder*||b|*", "*|b|*Folder*||b|*")
        );
    }
}
