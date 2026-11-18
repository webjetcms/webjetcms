package sk.iway.iwcm.components.ai.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import io.swagger.v3.core.util.Json;
import sk.iway.iwcm.components.configuration.ConfigurationController;
import sk.iway.iwcm.editor.rest.GroupsRestController;
import sk.iway.iwcm.editor.rest.WebpagesRestController;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/** Verifies that DataTable paging metadata controls tool descriptions, schemas and bound requests. */
class AiToolPagingTest {
    private static final String PAGING_DESCRIPTION = "components.ai.tools.client_side_paging.description";

    /** The production controllers expose their unpaged lists without advertising or injecting paging. */
    @Test
    void configurationAndGroupsOmitPagingAndRejectPagingArguments() throws Exception {
        for (Class<?> controller : List.of(ConfigurationController.class, GroupsRestController.class)) {
            var entry = entry(mock(controller), "getAll", "/all", Pageable.class);
            var inputs = entry.inputs();
            var properties = (Map<?, ?>) inputs.schema().get("properties");
            assertFalse(properties.containsKey("page"));
            assertFalse(properties.containsKey("size"));
            assertTrue(properties.containsKey("sort"));
            assertEquals(Map.of(), Json.mapper().readValue(AiToolExample.forInput(inputs.schema()), Map.class));
            assertEquals(Map.of(), inputs.bind(Map.of()).query());
            assertTrue(entry.definition().description().endsWith("Do not send page or size."));
            AiToolArgumentValidator.validate(inputs.schema(), Map.of());
            for (String name : List.of("page", "size")) {
                var arguments = Map.<String, Object>of(name, 1);
                assertThrows(AiToolArgumentValidator.InvalidArguments.class,
                        () -> AiToolArgumentValidator.validate(inputs.schema(), arguments));
                assertThrows(IllegalArgumentException.class, () -> inputs.bind(arguments));
            }
            assertEquals(Map.of("sort", List.of("name,asc")), inputs.bind(Map.of("sort", List.of("name,asc"))).query());
        }
    }

    /** Missing, default and explicit false metadata retain server paging and the DataTable list default. */
    @Test
    void serverPagingKeepsItsExistingContract() throws Exception {
        for (Class<?> controller : List.of(UnannotatedController.class, DefaultController.class,
                ServerController.class, WebpagesRestController.class)) {
            var entry = entry(mock(controller), "getAll", "/all", Pageable.class);
            var inputs = entry.inputs();
            var properties = (Map<?, ?>) inputs.schema().get("properties");
            assertTrue(properties.containsKey("page"));
            assertEquals(20, ((Map<?, ?>) properties.get("size")).get("default"));
            assertEquals(Map.of("size", List.of("20")), inputs.bind(Map.of()).query());
            var arguments = Map.<String, Object>of("page", 2, "size", 5);
            AiToolArgumentValidator.validate(inputs.schema(), arguments);
            assertEquals(Map.of("page", List.of("2"), "size", List.of("5")), inputs.bind(arguments).query());
            assertFalse(entry.definition().description().contains("Do not send page or size."));
        }
    }

    /** Controller metadata remains available for inherited methods on a Spring class proxy. */
    @Test
    void inheritedHandlersReadMetadataFromTheProxiedController() throws Exception {
        var factory = new ProxyFactory(new ClientController());
        factory.setProxyTargetClass(true);
        var entry = entry(factory.getProxy(), "getAll", "/all", Pageable.class);
        assertTrue(entry.inputs().clientSidePaging());
        assertEquals(Map.of(), entry.inputs().bind(Map.of()).query());
        assertTrue(entry.definition().description().endsWith("Do not send page or size."));
    }

    /** Both free-form and DataTable search filters reject paging while retaining ordinary filters. */
    @Test
    void pagingCannotBePassedThroughFilters() throws Exception {
        var custom = entry(new ClientController(), "custom", "/custom", Map.class, Pageable.class);
        var search = entry(new ClientController(), "findByColumns", "/search/findByColumns", Map.class, Pageable.class, Object.class);
        for (var entry : List.of(custom, search)) {
            var inputs = entry.inputs();
            for (String name : List.of("page", "size")) {
                var arguments = Map.<String, Object>of("filters", Map.of(name, "1"));
                assertThrows(AiToolArgumentValidator.InvalidArguments.class,
                        () -> AiToolArgumentValidator.validate(inputs.schema(), arguments));
                assertThrows(IllegalArgumentException.class, () -> inputs.bind(arguments));
            }
            var arguments = Map.<String, Object>of("filters", Map.of("searchName", "Example"));
            AiToolArgumentValidator.validate(inputs.schema(), arguments);
            assertEquals(Map.of("searchName", List.of("Example")), inputs.bind(arguments).query());
        }
        assertTrue(custom.definition().description().startsWith("Custom list. "));
    }

    /** An unrelated size parameter on a handler without Pageable keeps its meaning and description. */
    @Test
    void handlersWithoutPageableAreUnaffected() throws Exception {
        var entry = entry(new ClientController(), "detail", "/detail", int.class);
        assertEquals("Detail.", entry.definition().description());
        var arguments = Map.<String, Object>of("size", 5);
        AiToolArgumentValidator.validate(entry.inputs().schema(), arguments);
        assertEquals(Map.of("size", List.of("5")), entry.inputs().bind(arguments).query());
    }

    /** Localized operation text and explicit method descriptions both receive the shared paging guidance once. */
    @Test
    void localizedDescriptionsUseTheSharedBundleText() throws Exception {
        var configuration = entry(mock(ConfigurationController.class), "getAll", "/all", Pageable.class);
        var custom = entry(new ClientController(), "custom", "/custom", Map.class, Pageable.class);
        for (String suffix : List.of("", "_cz", "_en")) {
            var properties = new Properties();
            try (var reader = Files.newBufferedReader(Path.of("src/main/webapp/WEB-INF/classes/text" + suffix + "-webjet9.properties"), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            var language = mock(Prop.class);
            when(language.getTextProp(anyString())).thenAnswer(call -> properties.getProperty(call.getArgument(0)));
            String guidance = properties.getProperty(PAGING_DESCRIPTION);
            assertTrue(guidance != null && !guidance.isBlank());
            String description = configuration.definition(language).description();
            assertEquals(properties.getProperty("components.ai.tools.configuration_get_all.description") + " " + guidance, description);
            assertEquals(description.indexOf(guidance), description.lastIndexOf(guidance));
            assertEquals("Custom list. " + guidance, custom.definition(language).description());
        }
        assertEquals(custom.definition().description(), custom.definition(mock(Prop.class)).description());
    }

    private static AiToolRegistry.Entry entry(Object controller, String method, String path, Class<?>... parameters) throws Exception {
        var handler = new HandlerMethod(controller, controller.getClass().getMethod(method, parameters));
        var mapping = RequestMappingInfo.paths(path).methods(RequestMethod.GET).build();
        var handlers = mock(RequestMappingHandlerMapping.class);
        when(handlers.getHandlerMethods()).thenReturn(Map.of(mapping, handler));
        return new AiToolRegistry(handlers).entries().get(0);
    }

    static class ListController extends DatatableRestControllerV2<PagingRow, Long> {
        ListController() { super(null); }
    }

    @AiTool
    static class UnannotatedController extends ListController { }

    @AiTool
    @Datatable
    static class DefaultController extends ListController { }

    @AiTool
    @Datatable(clientSidePaging = false)
    static class ServerController extends ListController { }

    @AiTool
    @Datatable(clientSidePaging = true)
    static class ClientController extends ListController {
        @AiToolDescription("Custom list.")
        public List<PagingRow> custom(@RequestParam Map<String, String> filters, Pageable pageable) { return List.of(); }

        @AiToolDescription("Detail.")
        public String detail(@RequestParam("size") int size) { return "detail"; }
    }

    static class PagingRow {
        public String name;
    }
}
