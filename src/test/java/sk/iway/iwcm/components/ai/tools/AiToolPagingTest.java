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

    /** Unpaged lists omit paging for production controllers and inherited handlers on Spring proxies. */
    @Test
    void configurationAndGroupsOmitPagingAndRejectPagingArguments() throws Exception {
        var proxy = new ProxyFactory(new ClientController());
        proxy.setProxyTargetClass(true);
        for (var controller : List.of(mock(ConfigurationController.class), mock(GroupsRestController.class), proxy.getProxy())) {
            var entry = entry(controller, "getAll", "/all", Pageable.class);
            var inputs = entry.inputs();
            var properties = (Map<?, ?>) inputs.schema().get("properties");
            assertFalse(properties.containsKey("page"));
            assertFalse(properties.containsKey("size"));
            assertTrue(properties.containsKey("sort"));
            assertEquals(Map.of(), Json.mapper().readValue(AiToolExample.forInput(inputs.schema()), Map.class));
            assertQuery(inputs, Map.of(), Map.of());
            assertTrue(entry.definition().description().endsWith("Do not send page or size."));
            for (String name : List.of("page", "size")) assertRejected(inputs, Map.of(name, 1));
            assertQuery(inputs, Map.of("sort", List.of("name,asc")), Map.of("sort", List.of("name,asc")));
        }
    }

    /** Missing metadata and the default on the web pages controller retain server paging. */
    @Test
    void serverPagingKeepsItsExistingContract() throws Exception {
        for (Class<?> controller : List.of(UnannotatedController.class, WebpagesRestController.class)) {
            var entry = entry(mock(controller), "getAll", "/all", Pageable.class);
            var inputs = entry.inputs();
            var properties = (Map<?, ?>) inputs.schema().get("properties");
            assertTrue(properties.containsKey("page"));
            assertEquals(20, ((Map<?, ?>) properties.get("size")).get("default"));
            assertQuery(inputs, Map.of(), Map.of("size", List.of("20")));
            assertQuery(inputs, Map.of("page", 2, "size", 5), Map.of("page", List.of("2"), "size", List.of("5")));
            assertFalse(entry.definition().description().contains("Do not send page or size."));
        }
    }

    /** Both free-form and DataTable search filters reject paging while retaining ordinary filters. */
    @Test
    void pagingCannotBePassedThroughFilters() throws Exception {
        var custom = entry(new ClientController(), "custom", "/custom", Map.class, Pageable.class);
        var search = entry(new ClientController(), "findByColumns", "/search/findByColumns", Map.class, Pageable.class, Object.class);
        for (var entry : List.of(custom, search)) {
            var inputs = entry.inputs();
            for (String name : List.of("page", "size")) assertRejected(inputs, Map.of("filters", Map.of(name, "1")));
            assertQuery(inputs, Map.of("filters", Map.of("searchName", "Example")), Map.of("searchName", List.of("Example")));
        }
        assertTrue(custom.definition().description().startsWith("Custom list. "));
    }

    /** An unrelated size parameter on a handler without Pageable keeps its meaning and description. */
    @Test
    void handlersWithoutPageableAreUnaffected() throws Exception {
        var entry = entry(new ClientController(), "detail", "/detail", int.class);
        assertEquals("Detail.", entry.definition().description());
        assertQuery(entry.inputs(), Map.of("size", 5), Map.of("size", List.of("5")));
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
            assertEquals("Custom list. " + guidance, custom.definition(language).description());
        }
        assertEquals(custom.definition().description(), custom.definition(mock(Prop.class)).description());
    }

    private static void assertQuery(AiToolInputs inputs, Map<String, Object> arguments, Map<String, List<String>> query) {
        AiToolArgumentValidator.validate(inputs.schema(), arguments);
        assertEquals(query, inputs.bind(arguments).query());
    }

    private static void assertRejected(AiToolInputs inputs, Map<String, Object> arguments) {
        assertThrows(AiToolArgumentValidator.InvalidArguments.class, () -> AiToolArgumentValidator.validate(inputs.schema(), arguments));
        assertThrows(IllegalArgumentException.class, () -> inputs.bind(arguments));
    }

    private static AiToolRegistry.Entry entry(Object controller, String method, String path, Class<?>... parameters) throws Exception {
        var handler = new HandlerMethod(controller, controller.getClass().getMethod(method, parameters));
        var mapping = RequestMappingInfo.paths(path).methods(RequestMethod.GET).build();
        var handlers = mock(RequestMappingHandlerMapping.class);
        when(handlers.getHandlerMethods()).thenReturn(Map.of(mapping, handler));
        return new AiToolRegistry(handlers).entries().get(0);
    }

    @AiTool
    static class UnannotatedController extends DatatableRestControllerV2<PagingRow, Long> {
        UnannotatedController() { super(null); }
    }

    @AiTool
    @Datatable(clientSidePaging = true)
    static class ClientController extends UnannotatedController {
        @AiToolDescription("Custom list.")
        public List<PagingRow> custom(@RequestParam Map<String, String> filters, Pageable pageable) { return List.of(); }

        @AiToolDescription("Detail.")
        public String detail(@RequestParam("size") int size) { return "detail"; }
    }

    static class PagingRow {
        public String name;
    }
}
