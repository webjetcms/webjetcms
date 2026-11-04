package sk.iway.iwcm.sync.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies that HTML image URLs resolve to original files included in page exports. */
class ContentBuilderTest extends BaseWebjetTest
{
    private static final String IMAGE = "/images/test-content/sample-image.webp";

    @TempDir
    Path webRoot;

    @ParameterizedTest
    @ValueSource(strings = {
        "/images/test-content/sample-image.webp?v=1787740915",
        "/thumb/images/test-content/sample-image.webp",
        "/thumb/images/test-content/sample-image.webp?w=880&amp;h=800&amp;ip=0&amp;v=1787740915",
        "/thumb/images/test-content/sample-image.webp?w=880&h=800&ip=0#preview",
        "http://localhost/thumb/images/test-content/sample-image.webp?w=880"
    })
    void includesOriginalImageFromHtml(String source) throws Exception
    {
        Content content = collect("<div><img alt=\"Sample image\" class=\"fixedSize-880-800-0\" src=\"" + source + "\" title=\"\"></div>");

        assertEquals(List.of(IMAGE), content.getFiles().stream().map(Content.File::getVirtualPath).toList());
        assertEquals(List.of(IMAGE), content.getDocFiles().get(123));
        assertEquals("0000_sample-image.webp", content.getFiles().get(0).getZipPath());
    }

    @Test
    void includesOriginalOnlyOnceForMultipleThumbnailSizes() throws Exception
    {
        Content content = collect("<img src=\"" + IMAGE + "\"><img src=\"/thumb" + IMAGE
            + "?w=880\"><img src=\"/thumb" + IMAGE + "?w=440\">");

        assertEquals(List.of(IMAGE), content.getFiles().stream().map(Content.File::getVirtualPath).toList());
        assertEquals(List.of(IMAGE), content.getDocFiles().get(123).stream().distinct().toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://other.example/thumb/images/test-content/sample-image.webp?w=880",
        "/thumb/images/test-content/../test-content/sample-image.webp?w=880",
        "/thumb/missing/images/test-content/sample-image.webp?w=880"
    })
    void ignoresExternalUnsafeAndMissingImages(String source) throws Exception
    {
        assertTrue(collect("<img src=\"" + source + "\">").getFiles().isEmpty());
    }

    private Content collect(String html) throws Exception
    {
        Path image = webRoot.resolve(IMAGE.substring(1));
        Files.createDirectories(image.getParent());
        Files.write(image, new byte[] {1, 2, 3});

        try (MockedStatic<Tools> tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
             MockedStatic<GalleryDB> gallery = mockStatic(GalleryDB.class))
        {
            tools.when(() -> Tools.getRealPath(anyString())).thenAnswer(call ->
                webRoot.resolve(((String) call.getArgument(0)).replaceFirst("^/+", "")).toString());

            Content content = new Content();
            ContentBuilder builder = new ContentBuilder(content, new MockHttpServletRequest());
            DocDetails doc = new DocDetails();
            doc.setDocId(123);
            builder.setDoc(doc);
            builder.addHtml(html);
            return content;
        }
    }
}
