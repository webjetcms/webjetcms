package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests planned prefix rewriting without altering unrelated CMS markup. */
class PlannedAssetPathsTest {
    private static final String ROOT = PlannedAssetPaths.ROOT + "42";

    @Test
    void rewritesResponsiveImagesThumbnailsAndCss() {
        String html = "!INCLUDE(/components/example.jsp)!<picture><source srcset=\"" + ROOT + "/images/a.webp 1x, " + ROOT + "/images/b.webp 2x\">" +
            "<img data-src='https://www.example.com/thumb" + ROOT + "/images/a.webp?w=200&amp;v=old#view' style=\"background:url('" + ROOT + "/images/b.webp')\"></picture>";
        String result = rewrite(html);
        assertTrue(result.startsWith("!INCLUDE(/components/example.jsp)!"));
        assertTrue(result.contains("/images/a.webp 1x, /images/b.webp 2x"));
        assertTrue(result.contains("https://www.example.com/thumb/images/a.webp?w=200&amp;v=old#view"));
        assertTrue(result.contains("url('/images/b.webp')"));
        assertFalse(result.contains(PlannedAssetPaths.ROOT));
    }

    @Test
    void preservesOrdinaryLinksAndExistingParameters() {
        assertEquals("/files/a.pdf?download=1&v=old#page=2", rewrite(ROOT + "/files/a.pdf?download=1&v=old#page=2"));
        assertEquals("<a href='/files/current.pdf'>Current</a>", rewrite("<a href='/files/current.pdf'>Current</a>"));
        assertEquals("/files/a.pdf", rewrite(ROOT + "/files/a.pdf"));
        assertEquals("/thumb/images/a.webp?v=1634319328&amp;w=160&amp;h=160&amp;ip=5#view",
            rewrite("/thumb" + ROOT + "/images/a.webp?v=1634319328&amp;w=160&amp;h=160&amp;ip=5#view"));
    }

    /** Replacement is independent of URL authority and preserves other pages' prefixes. */
    @Test
    void preservesUrlStructureAndMatchesOnlyTheSelectedPage() {
        assertEquals("https://other.example.com/images/a.webp", rewrite("https://other.example.com" + ROOT + "/images/a.webp"));
        assertEquals("http://example.com/images/a.webp", rewrite("http://example.com" + ROOT + "/images/a.webp"));
        assertEquals("//example.com/images/a.webp", rewrite("//example.com" + ROOT + "/images/a.webp"));
        assertEquals(ROOT + "0/images/a.webp", rewrite(ROOT + "0/images/a.webp"));
        assertEquals(PlannedAssetPaths.ROOT + "43/images/a.webp", rewrite(PlannedAssetPaths.ROOT + "43/images/a.webp"));
        assertEquals("  /images/new%20photo.webp  ", rewrite("  " + ROOT + "/images/new%20photo.webp  "));
        assertNull(rewrite(null));
    }

    /** Component parameters and unquoted HTML attributes use the same literal replacement. */
    @Test
    void rewritesComponentParametersAndUnquotedAttributes() {
        assertEquals("!INCLUDE(component.jsp, image=/images/a.webp)!", rewrite("!INCLUDE(component.jsp, image=" + ROOT + "/images/a.webp)!"));
        assertEquals("<img src=/images/a.webp>", rewrite("<img src=" + ROOT + "/images/a.webp>"));
    }

    private String rewrite(String value) {
        return PlannedAssetPaths.rewrite(value, 42);
    }

    @Test
    void mapsStagingWithoutUsingPageTitle() {
        assertEquals("/images/apple/new phone.webp", PlannedAssetPaths.target(ROOT + "/images/apple/new%20phone.webp", 42));
        assertEquals(ROOT + "/files/apple/", PlannedAssetPaths.source(42, "/files/apple/"));
    }

    /** Prefix removal supports files and folders without changing ordinary paths. */
    @Test
    void removesOnlyValidatedPlannedPrefixes() {
        assertEquals("/images/apple/", PlannedAssetPaths.removePlannedPrefix(ROOT + "/images/apple/"));
        assertEquals("/images/apple", PlannedAssetPaths.removePlannedPrefix(ROOT + "/images/apple"));
        assertEquals("/images/apple/new phone.webp", PlannedAssetPaths.removePlannedPrefix(ROOT + "/images/apple/new%20phone.webp"));
        assertEquals("/files/current.pdf", PlannedAssetPaths.removePlannedPrefix("/files/current.pdf"));
    }

    /** Removing the prefix must not accept malformed owners or unsafe public destinations. */
    @ParameterizedTest
    @ValueSource(strings = {
        "/files/protected/planned-changes/0/images/photo.jpg",
        "/files/protected/planned-changes/042/images/photo.jpg",
        "/files/protected/planned-changes/other/images/photo.jpg",
        "/files/protected/planned-changes/42/images/%2e%2e/photo.jpg",
        "/files/protected/planned-changes/42/files/protected/photo.jpg",
        "/files/protected/planned-changes/42/shared/photo.jpg",
        "/files/protected/planned-changes/42/files/script.jsp"
    })
    void rejectsInvalidPlannedPrefixesAndTargets(String path) {
        assertThrows(IllegalArgumentException.class, () -> PlannedAssetPaths.removePlannedPrefix(path));
    }
}
