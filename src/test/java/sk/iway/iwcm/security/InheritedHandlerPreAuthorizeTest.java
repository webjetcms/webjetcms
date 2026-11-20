package sk.iway.iwcm.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.SecurityAnnotationScanner;
import org.springframework.security.core.annotation.SecurityAnnotationScanners;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import sk.iway.iwcm.admin.jstree.JsTreeRestController;
import sk.iway.iwcm.editor.rest.WebpagesDatatable;
import sk.iway.iwcm.editor.rest.WebpagesRestController;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/**
 * Verifies that every request handler of a controller with a class-level {@link PreAuthorize}
 * is really guarded by Spring Security method security.
 *
 * <p>Spring Security resolves {@link PreAuthorize} for a handler that has no annotation of its own
 * from the class that <em>declares</em> the method and its superclasses, never from the subclass
 * that only inherits it. A handler declared in an unannotated base class (for example
 * {@link WebpagesDatatable}) is therefore not protected by the permission of the controller that
 * exposes it (for example {@code menuWebpages} on {@link WebpagesRestController}).</p>
 *
 * <p>The test uses the same scanner as {@code PreAuthorizeExpressionAttributeRegistry}, so it
 * reports exactly the handlers that method security would let through without a permission
 * check.</p>
 */
class InheritedHandlerPreAuthorizeTest {

    /** The scanner {@code PreAuthorizeExpressionAttributeRegistry} uses to find the annotation. */
    private static final SecurityAnnotationScanner<PreAuthorize> SCANNER = SecurityAnnotationScanners.requireUnique(PreAuthorize.class);

    private static final String CHECK_ON_CONTROLLER = "@WebjetSecurityService.checkAccessAllowedOnController(this)";

    /** Handler without own annotation in a base class without class-level annotation. */
    private static class Base {
        @RequestMapping("/base")
        public String inherited() { return "base"; }
    }

    /** Subclass annotated at class level, inheriting {@link Base#inherited()}. */
    @PreAuthorize("@WebjetSecurityService.hasPermission('menuWebpages')")
    private static class AnnotatedSub extends Base {
        @RequestMapping("/own")
        public String own() { return "own"; }
    }

    /**
     * Documents the Spring Security behaviour the fix relies on: an inherited handler does not get
     * the class-level annotation of the subclass, a handler declared in the subclass does.
     */
    @Test
    void springSecurityIgnoresSubclassAnnotationForInheritedHandler() throws NoSuchMethodException {
        assertNull(SCANNER.scan(Base.class.getMethod("inherited"), AnnotatedSub.class),
            "Inherited handler must not resolve the class-level @PreAuthorize of the subclass");
        assertNotNull(SCANNER.scan(AnnotatedSub.class.getMethod("own"), AnnotatedSub.class),
            "Handler declared in the annotated class must resolve its class-level @PreAuthorize");
    }

    /**
     * The webpage preview and HTTP header handlers declared in {@link WebpagesDatatable} must check
     * the permission of the concrete controller (menuWebpages, cmp_news, cmp_blog, ...).
     */
    @Test
    void webpagesDatatableHandlersCheckControllerPermission() throws NoSuchMethodException {
        for (String name : List.of("preview", "httpHeaders")) {
            Method method = findMethod(WebpagesDatatable.class, name);
            PreAuthorize annotation = SCANNER.scan(method, WebpagesRestController.class);
            assertNotNull(annotation, "WebpagesDatatable." + name + " has no effective @PreAuthorize");
            assertEquals(CHECK_ON_CONTROLLER, annotation.value(), "WebpagesDatatable." + name);
        }
    }

    /**
     * Scans all controllers in {@code sk.iway} and fails for every request handler that its
     * controller protects with a class-level {@link PreAuthorize}, but that Spring Security
     * resolves to no annotation at all.
     */
    @Test
    void allHandlersOfAnnotatedControllersAreProtected() {
        List<Class<?>> controllers = findControllers();
        assertFalse(controllers.isEmpty(), "No controllers found on the classpath");

        TreeSet<String> unprotected = new TreeSet<>();
        int checkedHandlers = 0;
        for (Class<?> controller : controllers) {
            if (!AnnotatedElementUtils.hasAnnotation(controller, PreAuthorize.class)) continue;

            Map<Method, RequestMapping> handlers = MethodIntrospector.selectMethods(controller,
                (MethodIntrospector.MetadataLookup<RequestMapping>) m -> AnnotatedElementUtils.findMergedAnnotation(m, RequestMapping.class));
            for (Method handler : handlers.keySet()) {
                checkedHandlers++;
                if (SCANNER.scan(handler, controller) == null) {
                    unprotected.add(controller.getName() + " -> " + handler.getDeclaringClass().getSimpleName() + "." + handler.getName());
                }
            }
        }

        assertTrue(checkedHandlers > 0, "No handlers of annotated controllers were checked");
        assertTrue(unprotected.isEmpty(), "Handlers without effective @PreAuthorize although their controller has a class-level one:\n  "
            + String.join("\n  ", unprotected));
    }

    /**
     * Fails for every datatable or jstree REST controller that would let any administrator through.
     * Their inherited handlers use {@code checkAccessAllowedOnController(this)}, which allows access
     * when the controller has neither a class-level {@link PreAuthorize} nor its own
     * {@code checkAccessAllowed} override.
     */
    @Test
    void datatableAndJsTreeControllersDeclarePermission() {
        TreeSet<String> withoutPermission = new TreeSet<>();
        for (Class<?> controller : findControllers()) {
            boolean datatable = DatatableRestControllerV2.class.isAssignableFrom(controller);
            if (!datatable && !JsTreeRestController.class.isAssignableFrom(controller)) continue;
            if (AnnotatedElementUtils.hasAnnotation(controller, PreAuthorize.class)) continue;
            if (hasOwnCheckAccessAllowed(controller)) continue;
            withoutPermission.add(controller.getName());
        }

        assertTrue(withoutPermission.isEmpty(), "Controllers allowing every administrator, add class-level @PreAuthorize or override checkAccessAllowed:\n  "
            + String.join("\n  ", withoutPermission));
    }

    private static boolean hasOwnCheckAccessAllowed(Class<?> controller) {
        for (Method method : controller.getMethods()) {
            if (method.getName().equals("checkAccessAllowed") && method.getDeclaringClass() != DatatableRestControllerV2.class) return true;
        }
        return false;
    }

    private static Method findMethod(Class<?> clazz, String name) throws NoSuchMethodException {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name) && !method.isBridge()) return method;
        }
        throw new NoSuchMethodException(clazz.getName() + "." + name);
    }

    private static List<Class<?>> findControllers() {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new org.springframework.core.type.filter.AnnotationTypeFilter(Controller.class));
        ClassLoader classLoader = InheritedHandlerPreAuthorizeTest.class.getClassLoader();

        List<Class<?>> controllers = new ArrayList<>();
        for (BeanDefinition definition : provider.findCandidateComponents("sk.iway")) {
            try {
                Class<?> clazz = ClassUtils.forName(definition.getBeanClassName(), classLoader);
                if (!Modifier.isAbstract(clazz.getModifiers())) controllers.add(clazz);
            } catch (ClassNotFoundException | LinkageError e) {
                throw new IllegalStateException("Cannot load controller " + definition.getBeanClassName(), e);
            }
        }
        return controllers;
    }
}
