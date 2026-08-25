package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.annotation.WebServlet;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class ExternalWarRegistrationContractTest {

    private static final List<Path> DEPLOYMENT_DESCRIPTORS = List.of(
        Path.of("src/main/webapp/WEB-INF/web.xml")
    );
    private static final Map<String, String> DESCRIPTOR_FILTERS = Map.of(
        "ContextFilter", "sk.iway.iwcm.system.context.ContextFilter",
        "StripesFilter", "net.sourceforge.stripes.controller.StripesFilterIway",
        "Virtual Path Filter", "sk.iway.iwcm.PathFilter"
    );
    private static final Map<String, String> DESCRIPTOR_SERVLETS = Map.of(
        "GetProtectedFile", "sk.iway.iwcm.doc.GetProtectedFileServlet",
        "iwcminit", "sk.iway.iwcm.InitServlet",
        "StripesDispatcher", "net.sourceforge.stripes.controller.DispatcherServlet"
    );
    private static final Set<String> ANNOTATED_SERVLET_CLASSES = Set.of(
        "sk.iway.iwcm.doc.ShowDoc",
        "sk.iway.iwcm.editor.PreviewServlet",
        "sk.iway.iwcm.form.FormMailActionServlet",
        "sk.iway.iwcm.components.offline.OfflineAction",
        "sk.iway.iwcm.doc.DeleteServlet",
        "sk.iway.iwcm.LogoffServlet",
        "sk.iway.iwcm.filebrowser.MultipleFileUploadAction",
        "sk.iway.iwcm.editor.ThumbServlet",
        "sk.iway.iwcm.system.captcha.CaptchaServlet",
        "sk.iway.iwcm.system.elfinder.ElfinderServlet",
        "sk.iway.iwcm.components.pdf.PdfServlet",
        "sk.iway.iwcm.components.upload.XhrFileUploadServlet",
        "sk.iway.iwcm.admin.upload.AdminUploadServlet",
        "sk.iway.iwcm.sync.export.ExportSyncServlet"
    );

    @Test
    void deploymentDescriptorsOwnLegacyContainerRegistrations() throws Exception {
        for (Path descriptorPath : DEPLOYMENT_DESCRIPTORS) {
            Document descriptor = parseDescriptor(descriptorPath);

            assertTrue(Boolean.parseBoolean(descriptor.getDocumentElement().getAttribute("metadata-complete")) == false,
                () -> descriptorPath + " must keep servlet annotation scanning enabled");

            DESCRIPTOR_FILTERS.forEach((name, className) ->
                assertSingleDefinition(descriptor, "filter", "filter-name", "filter-class",
                    name, className, descriptorPath));
            assertMapping(descriptor, "filter-mapping", "filter-name", "ContextFilter",
                "url-pattern", "/*", null, descriptorPath);
            assertMapping(descriptor, "filter-mapping", "filter-name", "StripesFilter",
                "url-pattern", "/*", "REQUEST", descriptorPath);
            assertMapping(descriptor, "filter-mapping", "filter-name", "StripesFilter",
                "servlet-name", "StripesDispatcher", "REQUEST", descriptorPath);
            assertMapping(descriptor, "filter-mapping", "filter-name", "Virtual Path Filter",
                "url-pattern", "/*", null, descriptorPath);

            assertElementText(descriptor, "listener", "listener-class",
                "sk.iway.iwcm.stat.SessionListener", descriptorPath);

            DESCRIPTOR_SERVLETS.forEach((name, className) ->
                assertSingleDefinition(descriptor, "servlet", "servlet-name", "servlet-class",
                    name, className, descriptorPath));
            for (String annotatedServletClass : ANNOTATED_SERVLET_CLASSES) {
                assertEquals(0, countElements(descriptor, "servlet", "servlet-class", annotatedServletClass),
                    () -> annotatedServletClass + " must not also be declared in " + descriptorPath);
            }
            assertServletLoadOnStartup(descriptor, "iwcminit", descriptorPath);
            assertServletLoadOnStartup(descriptor, "StripesDispatcher", descriptorPath);
            assertEquals(0, countElements(descriptor, "servlet-mapping", "servlet-name", "iwcminit"),
                () -> "iwcminit must not receive a URL mapping in " + descriptorPath);
            assertMapping(descriptor, "servlet-mapping", "servlet-name", "GetProtectedFile",
                "url-pattern", "/files/protected/*", null, descriptorPath);
            assertMapping(descriptor, "servlet-mapping", "servlet-name", "StripesDispatcher",
                "url-pattern", "*.action", null, descriptorPath);
        }
    }

    @Test
    void descriptorOwnedComponentsDoNotAlsoUseServletAnnotations() {
        assertNull(sk.iway.iwcm.system.context.ContextFilter.class.getAnnotation(WebFilter.class));
        assertNull(net.sourceforge.stripes.controller.StripesFilterIway.class.getAnnotation(WebFilter.class));
        assertNull(sk.iway.iwcm.PathFilter.class.getAnnotation(WebFilter.class));
        assertNull(sk.iway.iwcm.stat.SessionListener.class.getAnnotation(WebListener.class));
        assertNull(sk.iway.iwcm.doc.GetProtectedFileServlet.class.getAnnotation(WebServlet.class));
        assertNull(sk.iway.iwcm.InitServlet.class.getAnnotation(WebServlet.class));
        assertNull(net.sourceforge.stripes.controller.DispatcherServlet.class.getAnnotation(WebServlet.class));
    }

    @Test
    void annotatedServletsOwnTheRemainingExternalWarMappings() {
        assertWebServlet(sk.iway.iwcm.doc.ShowDoc.class, "ShowDoc2", "/showdoc.do");
        assertWebServlet(sk.iway.iwcm.editor.PreviewServlet.class, "previewServlet", "/preview.do");
        assertWebServlet(sk.iway.iwcm.form.FormMailActionServlet.class, "FormMailAction", "/formmail.do");
        assertWebServlet(sk.iway.iwcm.components.offline.OfflineAction.class, "offlineServlet",
            "/admin/offline.do");
        assertWebServlet(sk.iway.iwcm.doc.DeleteServlet.class, "DelDoc", "/admin/docdel.do");
        assertWebServlet(sk.iway.iwcm.LogoffServlet.class, "LogOff", "/logoff.do", "/admin/logoff.do");
        assertWebServlet(sk.iway.iwcm.filebrowser.MultipleFileUploadAction.class,
            "MultipleFileUploadAction", "/admin/multiplefileupload.do");
        assertWebServlet(sk.iway.iwcm.editor.ThumbServlet.class, "thumbServlet",
            "/admin/thumb/*", "/thumb/*", "/tumbn/*");
        assertWebServlet(sk.iway.iwcm.system.captcha.CaptchaServlet.class, "captchaServlet", "/captcha.jpg");
        assertWebServlet(sk.iway.iwcm.system.elfinder.ElfinderServlet.class, "elfinderServlet",
            "/admin/elfinder-connector/");
        assertWebServlet(sk.iway.iwcm.components.pdf.PdfServlet.class, "pdfServlet",
            "/to.pdf/*", "/topdf/*");
        assertWebServlet(sk.iway.iwcm.components.upload.XhrFileUploadServlet.class, "", "/XhrFileUpload");
        assertWebServlet(sk.iway.iwcm.admin.upload.AdminUploadServlet.class, "", "/admin/upload/chunk");
        assertWebServlet(sk.iway.iwcm.sync.export.ExportSyncServlet.class, "exportSyncServlet", "/export.sync");

        assertNotNull(sk.iway.iwcm.components.upload.XhrFileUploadServlet.class
            .getAnnotation(MultipartConfig.class));
        assertNotNull(sk.iway.iwcm.admin.upload.AdminUploadServlet.class
            .getAnnotation(MultipartConfig.class));
        assertNull(sk.iway.iwcm.filebrowser.MultipleFileUploadAction.class
            .getAnnotation(MultipartConfig.class));
    }

    private Document parseDescriptor(Path descriptorPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(descriptorPath.toFile());
    }

    private void assertSingleDefinition(Document descriptor, String elementName, String nameElement,
            String classElement, String expectedName, String expectedClass, Path descriptorPath) {
        Element definition = findElement(descriptor, elementName, nameElement, expectedName);
        assertNotNull(definition, () -> expectedName + " is missing from " + descriptorPath);
        assertEquals(1, countElements(descriptor, elementName, nameElement, expectedName),
            () -> expectedName + " must be declared exactly once in " + descriptorPath);
        assertEquals(expectedClass, childText(definition, classElement),
            () -> "Unexpected class for " + expectedName + " in " + descriptorPath);
    }

    private int countElements(Document descriptor, String elementName, String childElement,
            String expectedText) {
        NodeList elements = descriptor.getElementsByTagNameNS("*", elementName);
        int count = 0;
        for (int i = 0; i < elements.getLength(); i++) {
            if (expectedText.equals(childText((Element) elements.item(i), childElement))) {
                count++;
            }
        }
        return count;
    }

    private void assertMapping(Document descriptor, String mappingElement, String nameElement,
            String expectedName, String targetElement, String expectedTarget, String expectedDispatcher,
            Path descriptorPath) {
        NodeList mappings = descriptor.getElementsByTagNameNS("*", mappingElement);
        for (int i = 0; i < mappings.getLength(); i++) {
            Element mapping = (Element) mappings.item(i);
            if (expectedName.equals(childText(mapping, nameElement))
                    && expectedTarget.equals(childText(mapping, targetElement))) {
                assertEquals(expectedDispatcher, childText(mapping, "dispatcher"),
                    () -> "Unexpected dispatcher for " + expectedName + " in " + descriptorPath);
                return;
            }
        }
        throw new AssertionError("Missing " + expectedName + " mapping to " + expectedTarget
            + " in " + descriptorPath);
    }

    private void assertElementText(Document descriptor, String elementName, String childElement,
            String expectedText, Path descriptorPath) {
        assertNotNull(findElement(descriptor, elementName, childElement, expectedText),
            () -> expectedText + " is missing from " + descriptorPath);
    }

    private void assertServletLoadOnStartup(Document descriptor, String servletName, Path descriptorPath) {
        Element servlet = findElement(descriptor, "servlet", "servlet-name", servletName);
        assertNotNull(servlet, () -> servletName + " is missing from " + descriptorPath);
        assertEquals("1", childText(servlet, "load-on-startup"),
            () -> servletName + " must load on startup in " + descriptorPath);
    }

    private Element findElement(Document descriptor, String elementName, String childElement,
            String expectedText) {
        NodeList elements = descriptor.getElementsByTagNameNS("*", elementName);
        for (int i = 0; i < elements.getLength(); i++) {
            Element element = (Element) elements.item(i);
            if (expectedText.equals(childText(element, childElement))) {
                return element;
            }
        }
        return null;
    }

    private String childText(Element parent, String childElement) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && childElement.equals(child.getLocalName())) {
                return child.getTextContent().trim();
            }
        }
        return null;
    }

    private void assertWebServlet(Class<?> servletClass, String expectedName, String... expectedMappings) {
        WebServlet annotation = servletClass.getAnnotation(WebServlet.class);
        assertNotNull(annotation, () -> servletClass.getName() + " must retain @WebServlet for external WARs");
        assertEquals(expectedName, annotation.name(),
            () -> "Unexpected servlet name for " + servletClass.getName());

        Set<String> mappings = new LinkedHashSet<>();
        mappings.addAll(Arrays.asList(annotation.value()));
        mappings.addAll(Arrays.asList(annotation.urlPatterns()));
        assertEquals(Set.of(expectedMappings), mappings,
            () -> "Unexpected servlet mappings for " + servletClass.getName());
        assertTrue(mappings.isEmpty() == false,
            () -> servletClass.getName() + " must have at least one external WAR mapping");
    }
}
