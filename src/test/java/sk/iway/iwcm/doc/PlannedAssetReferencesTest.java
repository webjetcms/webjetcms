package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests inline asset collection and URL rewriting without altering unrelated CMS markup. */
class PlannedAssetReferencesTest {
    private static final String ROOT = PlannedAssetPaths.ROOT + "42";

    @Test
    void rewritesResponsiveImagesThumbnailsAndCssAndDeduplicatesSources() {
        PlannedAssetReferences references = new PlannedAssetReferences(42, "www.example.com");
        String html = "!INCLUDE(/components/example.jsp)!<picture><source srcset=\"" + ROOT + "/images/a.webp 1x, " + ROOT + "/images/b.webp 2x\">" +
            "<img data-src='https://www.example.com/thumb" + ROOT + "/images/a.webp?w=200&amp;v=old#view' style=\"background:url('" + ROOT + "/images/b.webp')\"></picture>";
        String result = references.rewrite(html);
        assertTrue(result.startsWith("!INCLUDE(/components/example.jsp)!"));
        assertTrue(result.contains("/images/a.webp 1x, /images/b.webp 2x"));
        assertTrue(result.contains("https://www.example.com/thumb/images/a.webp?w=200&amp;v=old#view"));
        assertTrue(result.contains("url('/images/b.webp')"));
        assertEquals(2, references.getFiles().size());
        assertFalse(result.contains(PlannedAssetPaths.ROOT));
    }

    @Test
    void preservesOrdinaryLinksAndExistingParameters() {
        PlannedAssetReferences references = new PlannedAssetReferences(42, "example.com");
        assertEquals("/files/a.pdf?download=1&v=old#page=2", references.rewrite(ROOT + "/files/a.pdf?download=1&v=old#page=2"));
        assertEquals("<a href='/files/current.pdf'>Current</a>", references.rewrite("<a href='/files/current.pdf'>Current</a>"));
        assertEquals("/files/a.pdf", references.rewrite(ROOT + "/files/a.pdf"));
        assertEquals("/thumb/images/a.webp?v=1634319328&amp;w=160&amp;h=160&amp;ip=5#view",
            references.rewrite("/thumb" + ROOT + "/images/a.webp?v=1634319328&amp;w=160&amp;h=160&amp;ip=5#view"));
        assertEquals(2, references.getFiles().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/files/protected/planned-changes/43/images/a.webp",
        "/files/protected/planned-changes/42/images/../secret.pdf",
        "/files/protected/planned-changes/42/images/%2e%2e/secret.pdf",
        "/files/protected/planned-changes/42/images/%252e%252e/secret.pdf",
        "/files/protected/planned-changes/42/files/protected/secret.pdf",
        "/files/protected/planned-changes/42/files/preview.html",
        "/files/protected/planned-changes/42/images/a.jsp",
        "/files/protected/planned-changes/42/images/a.webp;extra",
        "https://other.example.com/files/protected/planned-changes/42/images/a.webp"
    })
    void rejectsUnsafeOrOutOfScopeReferences(String value) {
        assertThrows(IllegalArgumentException.class, () -> new PlannedAssetReferences(42, "example.com").rewrite(value));
    }

    @Test
    void rejectsUnsupportedComponentParametersAndSupportsUnquotedAttributes() {
        PlannedAssetReferences references = new PlannedAssetReferences(42, "example.com");
        assertThrows(IllegalArgumentException.class, () -> references.rewrite("!INCLUDE(component.jsp, image=" + ROOT + "/images/a.webp)!"));
        assertEquals("<img src=/images/a.webp>", references.rewrite("<img src=" + ROOT + "/images/a.webp>"));
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
