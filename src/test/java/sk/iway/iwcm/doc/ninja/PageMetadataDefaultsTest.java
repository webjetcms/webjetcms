package sk.iway.iwcm.doc.ninja;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.doc.DocDetails;

class PageMetadataDefaultsTest {

    private DocDetails doc;
    private Ninja ninja;
    private Temp temp;
    private TempGroup tempGroup;
    private Page page;

    @BeforeEach
    void setUp() {
        doc = new DocDetails();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("docDetails", doc);

        ninja = mock(Ninja.class);
        temp = mock(Temp.class);
        tempGroup = mock(TempGroup.class);

        when(ninja.getRequest()).thenReturn(request);
        when(ninja.getTemp()).thenReturn(temp);
        when(ninja.getConfig("defaultSeoImage")).thenReturn(null);
        when(temp.getGroup()).thenReturn(tempGroup);
        when(temp.getBasePathImg()).thenReturn("/templates/test/assets/images/");
        when(tempGroup.getDescription()).thenReturn("");
        when(tempGroup.getSeoImage()).thenReturn("");
        when(tempGroup.getSeoImageAlt()).thenReturn("");

        page = new Page(ninja);
    }

    @Test
    void seoDescriptionUsesDocumentFieldBeforeOtherValues() {
        doc.setFieldS("<strong>Page \"description\"</strong>");
        doc.setHtmlData("Perex description");
        when(tempGroup.getDescription()).thenReturn("Group description");

        assertEquals("Page description", page.getSeoDescription());
    }

    @Test
    void seoDescriptionFallsBackToPerex() {
        doc.setHtmlData("Perex \"description\"");
        when(tempGroup.getDescription()).thenReturn("Group description");

        assertEquals("Perex description", page.getSeoDescription());
    }

    @Test
    void seoDescriptionFallsBackToLocalizedTemplateGroupValue() {
        when(tempGroup.getDescription()).thenReturn("<strong>Group \"description\"</strong>");

        assertEquals("Group description", page.getSeoDescription());
    }

    @Test
    void seoImageUsesValidDocumentFieldBeforeOtherValues() {
        doc.setFieldT("/images/page.jpg");
        doc.setPerexImage("/images/perex.jpg");
        when(tempGroup.getSeoImage()).thenReturn("/images/group.jpg");
        when(ninja.getConfig("defaultSeoImage")).thenReturn("legacy.jpg");

        assertEquals("/images/page.jpg", page.getSeoImage());
    }

    @Test
    void seoImageIgnoresInvalidDocumentFieldAndFallsBackToPerex() {
        doc.setFieldT("not-an-image");
        doc.setPerexImage("/images/perex.jpg");
        when(tempGroup.getSeoImage()).thenReturn("/images/group.jpg");

        assertEquals("/images/perex.jpg", page.getSeoImage());
    }

    @Test
    void seoImageFallsBackToTemplateGroupValue() {
        when(tempGroup.getSeoImage()).thenReturn("/images/group.jpg");
        when(ninja.getConfig("defaultSeoImage")).thenReturn("legacy.jpg");

        assertEquals("/images/group.jpg", page.getSeoImage());
    }

    @Test
    void seoImageFallsBackToNonBlankLegacyConfiguration() {
        when(ninja.getConfig("defaultSeoImage")).thenReturn("legacy.jpg");

        assertEquals("/templates/test/assets/images/legacy.jpg", page.getSeoImage());
    }

    @Test
    void seoImageIgnoresInvalidTemplateGroupValue() {
        when(tempGroup.getSeoImage()).thenReturn("not-an-image");
        when(ninja.getConfig("defaultSeoImage")).thenReturn("legacy.jpg");

        assertEquals("/templates/test/assets/images/legacy.jpg", page.getSeoImage());
    }

    @Test
    void seoImageIsEmptyWhenLegacyConfigurationIsBlank() {
        when(ninja.getConfig("defaultSeoImage")).thenReturn("   ");

        assertEquals("", page.getSeoImage());
    }

    @Test
    void seoImageAltUsesDocumentFieldBeforeTemplateGroupValue() {
        doc.setFieldP("<strong>Page \"image\"</strong>");
        when(tempGroup.getSeoImageAlt()).thenReturn("Group image");

        assertEquals("Page image", page.getSeoImageAlt());
    }

    @Test
    void seoImageAltFallsBackToLocalizedTemplateGroupValue() {
        when(tempGroup.getSeoImageAlt()).thenReturn("<strong>Group \"image\"</strong>");

        assertEquals("Group image", page.getSeoImageAlt());
    }

    @Test
    void metadataGettersAreNullSafeWithoutDocumentOrTemplate() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Ninja emptyNinja = mock(Ninja.class);
        when(emptyNinja.getRequest()).thenReturn(request);
        when(emptyNinja.getTemp()).thenReturn(null);
        when(emptyNinja.getConfig("defaultSeoImage")).thenReturn(null);

        Page emptyPage = new Page(emptyNinja);

        assertEquals("", emptyPage.getSeoDescription());
        assertEquals("", emptyPage.getSeoImage());
        assertEquals("", emptyPage.getSeoImageAlt());
    }
}
