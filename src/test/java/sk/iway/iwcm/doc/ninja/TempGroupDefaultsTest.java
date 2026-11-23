package sk.iway.iwcm.doc.ninja;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.doc.TemplatesGroupBean;
import sk.iway.iwcm.i18n.Prop;

class TempGroupDefaultsTest {

    @Test
    void templateGroupBeanIsReadFromRequest() {
        TemplatesGroupBean bean = new TemplatesGroupBean();
        bean.setTemplatesGroupId(42L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("templatesGroupDetails", bean);

        Ninja ninja = mock(Ninja.class);
        when(ninja.getRequest()).thenReturn(request);

        TempGroup tempGroup = new TempGroup(ninja);

        assertSame(bean, tempGroup.getTemplatesGroupBean());
    }

    @Test
    void defaultsUsePersistentImageAndLocalizedTextValues() {
        TemplatesGroupBean bean = new TemplatesGroupBean();
        bean.setTemplatesGroupId(42L);
        bean.setSeoImage("/images/group.jpg");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("templatesGroupDetails", bean);

        Prop prop = mock(Prop.class);
        when(prop.getText("temp-group-42.project.description")).thenReturn("<strong>Group description</strong>");
        when(prop.getText("temp-group-42.project.seoImageAlt")).thenReturn("<strong>Group image</strong>");

        Ninja ninja = mock(Ninja.class);
        when(ninja.getRequest()).thenReturn(request);
        when(ninja.getProp()).thenReturn(prop);

        TempGroup tempGroup = new TempGroup(ninja);

        assertEquals("Group description", tempGroup.getDescription());
        assertEquals("/images/group.jpg", tempGroup.getSeoImage());
        assertEquals("Group image", tempGroup.getSeoImageAlt());
    }

    @Test
    void missingLocalizedValuesAreReturnedAsEmptyStrings() {
        TemplatesGroupBean bean = new TemplatesGroupBean();
        bean.setTemplatesGroupId(42L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("templatesGroupDetails", bean);

        Prop prop = mock(Prop.class);
        when(prop.getText("temp-group-42.project.description")).thenReturn("temp-group-42.project.description");
        when(prop.getText("temp-group-42.project.seoImageAlt")).thenReturn("temp-group-42.project.seoImageAlt");

        Ninja ninja = mock(Ninja.class);
        when(ninja.getRequest()).thenReturn(request);
        when(ninja.getProp()).thenReturn(prop);

        TempGroup tempGroup = new TempGroup(ninja);

        assertEquals("", tempGroup.getDescription());
        assertEquals("", tempGroup.getSeoImage());
        assertEquals("", tempGroup.getSeoImageAlt());
    }

    @Test
    void defaultsAreNullSafeWithoutRequestDocumentOrProperties() {
        Ninja ninja = mock(Ninja.class);
        when(ninja.getRequest()).thenReturn(null);
        when(ninja.getPage()).thenReturn(null);
        when(ninja.getProp()).thenReturn(null);

        TempGroup tempGroup = new TempGroup(ninja);

        assertNull(tempGroup.getTemplatesGroupBean());
        assertEquals("", tempGroup.getDescription());
        assertEquals("", tempGroup.getSeoImage());
        assertEquals("", tempGroup.getSeoImageAlt());
    }
}
