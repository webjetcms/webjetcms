package sk.iway.iwcm.components.ai.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.handler.MappedInterceptor;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.dto.AiChatRequest;
import sk.iway.iwcm.components.ai.jpa.AiSkillEntity;
import sk.iway.iwcm.components.ai.jpa.AiSkillRepository;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.rest.AiChatStore;
import sk.iway.iwcm.components.ai.rest.AiToolsController;
import sk.iway.iwcm.components.configuration.ConfigurationController;
import sk.iway.iwcm.components.configuration.ConfigurationService;
import sk.iway.iwcm.components.configuration.model.ConfDetailsDto;
import sk.iway.iwcm.editor.rest.GroupsRestController;
import sk.iway.iwcm.editor.rest.WebpagesRestController;
import sk.iway.iwcm.system.adminlog.AdminlogNotifyManager;
import sk.iway.iwcm.system.datatable.events.DatatableEvent;
import sk.iway.iwcm.system.datatable.events.DatatableEventType;
import sk.iway.iwcm.system.spring.SpringAppInitializer;
import sk.iway.iwcm.system.spring.services.WebjetSecurityService;
import sk.iway.iwcm.users.UsersDB;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Exercises both discovery steps through the same named-call filter and MVC handlers used by assistants. */
class AiToolDiscoveryTest {
    private static final String BASE = "/admin/rest/ai/tools";
    private final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    private final MockHttpSession session = new MockHttpSession();
    private final JsonMapper json = JsonMapper.builder().build();
    private MockMvc mvc;
    private WebjetSecurityService security;
    private AssistantDefinitionEntity assistant;

    @RestController
    @PreAuthorize("@WebjetSecurityService.hasPermission('cmp_quiz')")
    @AiTool(include = "batch")
    static class AuditProbeController {
        @PostMapping("/admin/rest/probe/batch")
        public ResponseEntity<Void> batch() {
            new DatatableEvent<>(11L, DatatableEventType.BEFORE_SAVE).publishEvent();
            Adminlog.add(Adminlog.TYPE_QUIZ, "UPDATE:\nid: 11", 11, -1);
            new DatatableEvent<>(12L, DatatableEventType.BEFORE_DELETE).publishEvent();
            Adminlog.add(Adminlog.TYPE_QUIZ, "DELETE:\nid: 12", 12, -1);
            return ResponseEntity.status(207).build();
        }
    }

    @EnableWebMvc
    @EnableSpringDataWebSupport
    @EnableMethodSecurity(proxyTargetClass = true, securedEnabled = true)
    @Import({AiToolsController.class, AiToolRegistry.class, AiToolService.class, AiToolAuthorization.class, AiToolCallFilter.class, AiToolAudit.class, AiChatStore.class, ConfigurationToolResponseAdvice.class})
    static class Config {
        @Bean(name = "WebjetSecurityService")
        WebjetSecurityService security() { return mock(WebjetSecurityService.class); }
        @Bean AssistantDefinitionRepository assistants() { return mock(AssistantDefinitionRepository.class); }
        @Bean AiSkillRepository skills() { return mock(AiSkillRepository.class); }
        @Bean ConfigurationService configurationService() { return mock(ConfigurationService.class); }
        @Bean ConfigurationController configuration(ConfigurationService service) { return new ConfigurationController(service); }
        @Bean AuditProbeController probe() { return new AuditProbeController(); }
        @Bean WebpagesRestController webpages() { return new WebpagesRestController(null, null, null, null, null); }
        @Bean GroupsRestController groups() { return new GroupsRestController(null, null, null, null, null); }
        @Bean MappedInterceptor guard(AiToolAuthorization authorization) {
            return new MappedInterceptor(null, new AiToolInterceptor(authorization));
        }
    }

    @BeforeEach
    void setUp() {
        var dispatcher = mock(ServletRegistration.class);
        when(dispatcher.getMappings()).thenReturn(List.of("/"));
        context.setServletContext(new MockServletContext() {
            @Override public Map<String, ? extends ServletRegistration> getServletRegistrations() {
                return Map.of(SpringAppInitializer.DISPATCHER_NAME, dispatcher);
            }
            @Override public Map<String, ? extends FilterRegistration> getFilterRegistrations() { return Map.of(); }
        });
        context.register(Config.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean(AiToolCallFilter.class)).build();
        var admin = new Identity();
        admin.setAdmin(true);
        session.setAttribute(Constants.USER_KEY, admin);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated("admin", "", List.of()));
        security = context.getBean(WebjetSecurityService.class);
        when(security.isAdmin()).thenReturn(true);
        when(security.hasPermission("cmp_ai_button")).thenReturn(true);
        when(security.hasPermission("cmp_quiz")).thenReturn(true);
        when(security.checkAccessAllowedOnController(any())).thenReturn(true);
        assistant = new AssistantDefinitionEntity();
        assistant.setId(7L);
        assistant.setName("Quiz editor");
        assistant.setAction("live_chat");
        assistant.setInstructions("@tool(ai_tools_preview) @tool(ai_tools_details) @tool(audit_probe_batch)");
        when(context.getBean(AssistantDefinitionRepository.class).findByIdAndDomainId(eq(7L), anyInt()))
                .thenReturn(Optional.of(assistant));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void configurationPublishesOnlyImplementedOperationsAndValidFields() {
        var registry = context.getBean(AiToolRegistry.class);
        var names = registry.assignedEntries(Set.of(), Set.of("configuration")).stream()
                .map(entry -> entry.definition().name()).toList();
        assertEquals(List.of("configuration_add", "configuration_delete", "configuration_edit", "configuration_get_all",
                "configuration_get_autocomplete", "configuration_get_autocomplete_detail", "configuration_get_one",
                "configuration_handle_editor", "configuration_restart"), names);

        var schema = context.getBean(AiToolRegistry.class).lookup("configuration_add").definition().inputSchema();
        AiToolArgumentValidator.validate(schema, Map.of("body", Map.of("name", "example", "encrypt", true)));
        assertThrows(AiToolArgumentValidator.InvalidArguments.class, () ->
                AiToolArgumentValidator.validate(schema, Map.of("body", Map.of("name", "example", "encrypted", true))));
    }

    @Test
    void configurationToolRedactsSensitiveValuesAndPreservesOrdinaryRows() throws Exception {
        assistant.setInstructions("@group(configuration)");
        when(security.hasPermission("menuConfig")).thenReturn(true);
        var service = context.getBean(ConfigurationService.class);
        var password = configurationRow("smtpPassword", "private-stored", "private-display", "private-default");
        var port = configurationRow("smtpPort", "25", "587", "2525");
        var custom = configurationRow("tenantMailLabel", "Office", "Mail", null);
        var encrypted = configurationRow("deliverySetting", "encrypted:private-stored", "private-display", "private-default");
        when(service.getAutocompleteDetail(any(), eq("smtpPassword"))).thenReturn(password);
        when(service.getAll(any(), any(), any())).thenReturn(List.of(password, port, custom, encrypted));

        try (var constants = mockStatic(Constants.class, CALLS_REAL_METHODS)) {
            constants.when(() -> Constants.getString("smtpPassword")).thenReturn("private-runtime");
            var detail = configurationCall("get_autocomplete_detail", Map.of("name", "smtpPassword"));
            assertTrue(detail.path("configured").asBoolean());
            assertTrue(detail.path("redacted").asBoolean());
            assertFalse(detail.has("value") || detail.has("displayValue") || detail.has("oldValue"));

            var rows = configurationCall("get_all", Map.of()).path("content");
            assertEquals(detail, rows.get(0));
            assertEquals(json.valueToTree(port), rows.get(1));
            assertEquals(json.valueToTree(custom), rows.get(2));
            assertTrue(rows.get(3).required("redacted").asBoolean());

            constants.when(() -> Constants.getString("smtpPassword")).thenReturn("");
            detail = configurationCall("get_autocomplete_detail", Map.of("name", "smtpPassword"));
            assertFalse(detail.required("configured").asBoolean());
        }
        var direct = mvc.perform(get("/admin/rest/settings/configuration/autocomplete/detail").session(session)
                .param("name", "smtpPassword")).andExpect(status().isOk()).andReturn().getResponse();
        assertEquals(json.readTree(json.writeValueAsBytes(password)), json.readTree(direct.getContentAsByteArray()));
    }

    @Test
    void existenceToolsReturnJsonBooleans() throws Exception {
        assistant.setInstructions("@tool(webpages_exists) @tool(groups_exists)");
        when(security.hasPermission("menuWebpages")).thenReturn(true);
        for (String name : List.of("webpages_exists", "groups_exists")) {
            mvc.perform(toolRequest(name, "{\"id\":-1}")).andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andExpect(content().string("false"));
        }
    }

    @Test
    void inheritedEndpointsForwardDeclaredQueriesAndValidateNestedFields() {
        var registry = context.getBean(AiToolRegistry.class);
        var get = registry.lookup("webpages_get_one").inputs();
        assertEquals(Map.of("groupId", List.of("25")), get.bind(Map.of("id", -1, "groupId", 25)).query());
        assertTrue(get.bind(Map.of("id", 8)).query().isEmpty());
        var list = registry.lookup("webpages_get_all").inputs().bind(Map.of("recursive", true, "tempId", 4)).query();
        assertEquals(List.of("true"), list.get("recursive"));
        assertEquals(List.of("4"), list.get("tempId"));
        assertThrows(IllegalArgumentException.class, () -> registry.lookup("groups_get_all").inputs().bind(Map.of("recursive", true)));
        var edit = registry.lookup("webpages_edit").definition().inputSchema();
        assertFeedback(edit, Map.of("id", 1, "body", Map.of("title", "A", "navbar", "A", "editorFields", Map.of("groupDetails", Map.of("groupName", "Test", "navbarName", "Test")))),
                "arguments.body.editorFields.groupDetails.groupId is required.");
    }

    private static ConfDetailsDto configurationRow(String name, String value, String displayValue, String oldValue) {
        var row = new ConfDetailsDto();
        row.setName(name);
        row.setValue(value);
        row.setDisplayValue(displayValue);
        row.setOldValue(oldValue);
        return row;
    }

    private JsonNode configurationCall(String operation, Map<String, ?> arguments) throws Exception {
        var response = mvc.perform(toolRequest("configuration_" + operation, json.writeValueAsString(arguments)))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertFalse(response.getContentAsString().contains("private-"));
        return json.readTree(response.getContentAsByteArray());
    }

    private MockHttpServletRequest adminRequest() {
        var request = new MockHttpServletRequest(context.getServletContext());
        request.setSession(session);
        return request;
    }

    /** The picker needs visible context; execution must still recognize a preset with inaccessible writes. */
    @Test
    void serverRuntimeDistinguishesAssignedAndPermittedWrites() {
        assistant.setAction("generate_text");
        assistant.setProvider("openai");
        assistant.setInstructions("@tool(audit_probe_batch)");
        var service = context.getBean(AiToolService.class);
        for (boolean permitted : List.of(true, false)) {
            when(security.hasPermission("cmp_quiz")).thenReturn(permitted);
            var request = adminRequest();
            var runtime = service.runtimeMetadata(7, request);
            assertTrue(runtime.toolsWrite());
            assertEquals(permitted, runtime.toolsEnabled());
            assertNull(runtime.tools());
            assertEquals(permitted, service.hasInstructionContext(assistant, request));
        }
    }

    @Test
    void editorChecksSkillExistenceOnceWithoutLoadingItsInstructions() throws Exception {
        assistant.setAction("generate_text");
        assistant.setProvider("openai");
        assistant.setInstructions("@skill(Quiz) @skill(Quiz)");
        var skills = context.getBean(AiSkillRepository.class);
        int domain = CloudToolsForCore.getDomainId();
        when(skills.findLinkedNames(domain, Set.of("Quiz"))).thenReturn(List.of("Quiz"), List.of());

        var service = context.getBean(AiToolService.class);
        var request = adminRequest();
        assertTrue(service.hasInstructionContext(assistant, request));
        assertTrue(service.hasInstructionContext(assistant, request));
        verify(skills).findLinkedNames(domain, Set.of("Quiz"));
        assertFalse(service.hasInstructionContext(assistant, adminRequest()));
        assistant.setProvider("browser");
        assertFalse(service.hasInstructionContext(assistant, adminRequest()));
        verify(skills, times(2)).findLinkedNames(domain, Set.of("Quiz"));
        verifyNoMoreInteractions(skills);

        assistant.setProvider("openai");
        var skill = new AiSkillEntity();
        skill.setName("Quiz");
        skill.setDomainId(domain);
        skill.setInstructions("Quiz guidance. " + "Sample content. ".repeat(20) + "Use @tool(audit_probe_batch). Full skill ending.");
        when(skills.findByDomainIdAndNameInOrderByNameAsc(domain, Set.of("Quiz"))).thenReturn(List.of(skill));
        var runtime = service.resolve(assistant, domain, adminRequest());
        String catalog = AiToolProtocol.prepare(runtime, List.of()).instructions();
        assertTrue(catalog.contains("Quiz guidance."));
        assertFalse(catalog.contains("Full skill ending."));
        assertEquals("skill Quiz skill Quiz", runtime.assistant().getInstructions());
        assertEquals(Set.of("ai_tools_load_skill", "ai_tools_details", "audit_probe_batch"), runtime.tools().stream()
                .map(AiToolDefinition::name).collect(java.util.stream.Collectors.toSet()));
        assertTrue(runtime.hasWriteTool());

        var loaded = loadQuizSkill();
        assertFalse(loaded.has("directTools"));
        assertEquals("Quiz", loaded.path("name").asText());
        assertEquals(skill.getInstructions().replace("@tool(audit_probe_batch)", "audit_probe_batch"),
                loaded.path("instructions").asText());
        assertFalse(loaded.path("version").asText().isBlank());
        assertTrue(catalog.contains(loaded.path("version").asText()));
        var tool = context.getBean(AiToolRegistry.class).lookup("audit_probe_batch").definition();
        assertEquals(json.valueToTree(List.of(Map.of("name", tool.name(), "description", tool.description(),
                "version", tool.version(), "write", true, "inputSchema", tool.inputSchema()))), loaded.required("tools"));
        skill.setInstructions("a".repeat(1024 * 1024 - 220) + " @tool(audit_probe_batch)");
        var large = loadQuizSkill();
        assertFalse(large.has("tools"));
        assertEquals(skill.getInstructions().replace("@tool(audit_probe_batch)", "audit_probe_batch"), large.required("instructions").asText());
        var updated = service.resolve(assistant, domain, adminRequest());
        assertFalse(updated.skills().get("Quiz").version().equals(loaded.path("version").asText()));
        mvc.perform(get(BASE + "/load-skill").session(session).param("name", "Unrelated")
                .header(AiToolAuthorization.ASSISTANT_HEADER, "7")).andExpect(status().isNotFound());
        mvc.perform(get(BASE + "/load-skill").session(session).param("name", "Quiz"))
                .andExpect(status().isNotFound());
    }

    /** Linked skills expose summaries first and load only schemas available to the current user. */
    @Test
    void loadsAssignedToolSchemasOnlyOnDemand() throws Exception {
        assistant.setInstructions("@skill(Configuration)");
        when(security.hasPermission("menuConfig")).thenReturn(true);
        var skill = new AiSkillEntity();
        skill.setName("Configuration");
        skill.setInstructions("Configuration guidance. " + "Description. ".repeat(20)
                + "@tool(configuration_add) @tool(configuration_add) @group(configuration) @tool(audit_probe_batch)");
        when(context.getBean(AiSkillRepository.class).findByDomainIdAndNameInOrderByNameAsc(anyInt(), eq(Set.of("Configuration"))))
                .thenReturn(List.of(skill));
        when(security.hasPermission("cmp_quiz")).thenReturn(false);
        var runtime = context.getBean(AiToolService.class).resolve(assistant, CloudToolsForCore.getDomainId(), adminRequest());
        String prompt = AiToolProtocol.prepare(runtime, List.of()).instructions();
        var summaries = json.readTree(prompt.split("Assigned tool summaries:\n", 2)[1].split("\nLoader input schemas:", 2)[0]);
        var loaders = json.readTree(prompt.split("Loader input schemas:\n", 2)[1].split("\nLinked skills", 2)[0]);
        for (var summary : summaries) {
            assertEquals(6, summary.size());
            assertTrue(summary.has("name") && summary.has("description") && summary.has("httpMethod")
                    && summary.has("path") && summary.has("write") && summary.has("version"));
        }
        assertEquals(2, loaders.size());
        for (var loader : loaders) {
            assertTrue(Set.of(AiAssistantRuntime.LOAD_TOOLS, AiAssistantRuntime.LOAD_SKILL).contains(loader.path("name").asText()));
            assertTrue(loader.has("inputSchema"));
        }
        assertFalse(prompt.contains("\"encrypt\"") || prompt.contains("\"outputSchema\""));

        var details = call("ai_tools_details", "{\"names\":[\"configuration_add\",\"configuration_add\"]}", 200);
        assertEquals(1, details.size());
        var tool = runtime.tools().stream().filter(t -> t.name().equals("configuration_add")).findFirst().orElseThrow();
        assertEquals(json.valueToTree(tool.inputSchema()), details.get(0).get("inputSchema"));
        assertFalse(details.get(0).has("outputSchema"));
        var withOutput = call("ai_tools_details", "{\"names\":[\"configuration_add\"],\"includeOutputSchema\":true}", 200).get(0);
        assertEquals(json.valueToTree(tool.outputSchema()), withOutput.get("outputSchema"));
        assertEquals(tool.preview().version(), details.get(0).path("version").asText());
        String arguments = "{\"name\":\"Configuration\"}";
        assertEquals(json.valueToTree(List.of(tool.callSchema())), call(AiAssistantRuntime.LOAD_SKILL, arguments, 200));
        when(security.hasPermission("menuConfig")).thenReturn(false);
        assertNull(call(AiAssistantRuntime.LOAD_SKILL, arguments, 200));
    }

    /** Assistant discovery cannot expand assignments; direct administrative browsing remains permission-based. */
    @Test
    void scopesDiscoveryToTheAssistantAndRechecksPermissions() throws Exception {
        when(security.hasPermission("menuConfig")).thenReturn(true);
        var details = call("ai_tools_details", "{\"names\":[\"audit_probe_batch\",\"configuration_add\",\"missing\"]}", 200);
        assertEquals(1, details.size());
        assertEquals("audit_probe_batch", details.get(0).path("name").asText());
        verify(context.getBean(AssistantDefinitionRepository.class)).findByIdAndDomainId(7L, CloudToolsForCore.getDomainId());
        var preview = call("ai_tools_preview", "{}", 200);
        assertEquals(3, preview.size());
        assertFalse(preview.toString().contains("configuration_add"));

        var administrative = mvc.perform(get(BASE + "/details").session(session).param("names", "configuration_add"))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertEquals("configuration_add", json.readTree(administrative.getContentAsByteArray()).path("tools").get(0).path("name").asText());
        var administrativePreview = mvc.perform(get(BASE + "/preview").session(session))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertTrue(administrativePreview.getContentAsString().contains("configuration_add"));
        call("configuration_add", "{\"body\":{\"name\":\"test\"}}", 404);

        when(security.hasPermission("cmp_quiz")).thenReturn(false);
        assertEquals(0, call("ai_tools_details", "{\"names\":[\"audit_probe_batch\"]}", 200).size());
        assertEquals(2, call("ai_tools_preview", "{}", 200).size());
        call("ai_tools_details", "{}", 400);
        mvc.perform(toolRequest("ai_tools_details", "{\"names\":[],\"names\":[\"ai_tools_details\"]}"))
                .andExpect(status().isBadRequest());

        assistant.setInstructions("@group(configuration) @tool(unknown)");
        var runtime = context.getBean(AiToolAuthorization.class).assistant(7L, adminRequest()).orElseThrow();
        assertTrue(runtime.tools().stream().anyMatch(tool -> "configuration_add".equals(tool.name())));
        assertTrue(runtime.assistant().getInstructions().endsWith("[tool unavailable]"));
        verifyNoInteractions(context.getBean(AiSkillRepository.class));
    }

    @Test
    void browserRuntimeLeavesReferencesUnresolved() {
        assistant.setAction("generate_text");
        assistant.setProvider("browser");
        assistant.setInstructions("Writer: {\"sharedContext\":\"@skill(Quiz) @tool(audit_probe_batch) @group(quiz)\"}");
        var runtime = context.getBean(AiToolService.class).runtimeMetadata(7, adminRequest());
        assertEquals(assistant.getInstructions(), runtime.instructions());
        assertEquals("", runtime.skillInstructions());
        assertTrue(runtime.tools().isEmpty());
        assertFalse(runtime.toolsEnabled());
        assertFalse(runtime.toolsWrite());
        verifyNoInteractions(context.getBean(AiSkillRepository.class));
    }

    private JsonNode loadQuizSkill() throws Exception {
        byte[] bytes = mvc.perform(post(BASE + "/ai_tools_load_skill/call").session(session)
                .header(AiToolAuthorization.ASSISTANT_HEADER, "7").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Quiz\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertTrue(bytes.length <= 1024 * 1024);
        return json.readTree(bytes);
    }

    private MockHttpServletRequestBuilder toolRequest(String tool, String arguments) {
        return post(BASE + "/" + tool + "/call").session(session)
                .header(AiToolAuthorization.CHAT_HEADER, "unsaved")
                .header(AiToolAuthorization.ASSISTANT_HEADER, "7").contentType(MediaType.APPLICATION_JSON).content(arguments);
    }

    private JsonNode call(String tool, String arguments, int expectedStatus) throws Exception {
        var response = mvc.perform(toolRequest(tool, arguments)).andExpect(status().is(expectedStatus)).andReturn().getResponse();
        String type = response.getHeader(AiToolCallFilter.TOOL_RESPONSE_HEADER);
        assertEquals(Map.of(200, "json", 207, "empty", 400, "invalid-arguments").get(expectedStatus), type);
        if (expectedStatus == 400) assertEquals("arguments.names is required.", response.getContentAsString());
        if (expectedStatus == 404) assertEquals("", response.getContentAsString());
        return expectedStatus == 200 ? json.readTree(response.getContentAsByteArray()).get("tools") : null;
    }

    @Test
    void savedWriteCanDispatchOnceAndOnlyBeforeStop(@TempDir Path root) throws Exception {
        UsersDB.getCurrentUser(session).setUserId(42);
        var chats = context.getBean(AiChatStore.class);
        var write = new AiToolProtocol.Call("audit_probe_batch", Map.of(), true);
        var servlet = mock(ServletContext.class);
        when(servlet.getRealPath("/")).thenReturn(root.toString());
        when(servlet.getAttribute("springContext")).thenReturn(context);
        try (var constants = mockStatic(Constants.class, CALLS_REAL_METHODS); var audit = mockStatic(Adminlog.class)) {
            constants.when(Constants::getServletContext).thenReturn(servlet);
            var scope = new AiChatStore.Scope(42, CloudToolsForCore.getDomainId(), CloudToolsForCore.getDomainName());
            for (boolean stopFirst : List.of(true, false)) {
                var input = new AiChatRequest(null, assistant.getId(), UUID.randomUUID().toString(), null, "Save", null);
                var turn = chats.begin(input, scope, assistant, "config");
                String id = turn.thread().getId();
                String message = turn.message().getId();
                assertTrue(chats.awaitToolResult(scope, id, message, write, 0));
                if (stopFirst) chats.stop(id, scope, false);
                var post = post(BASE + "/audit_probe_batch/call").session(session)
                        .header(AiToolAuthorization.ASSISTANT_HEADER, "7").contentType(MediaType.APPLICATION_JSON).content("{}");
                mvc.perform(post).andExpect(status().isNotFound()); // Delayed request from an older tab.
                post.header(AiToolAuthorization.CHAT_HEADER, id + "/" + message);
                mvc.perform(post).andExpect(status().is(stopFirst ? 404 : 207));
                mvc.perform(post).andExpect(status().isNotFound());
                if (!stopFirst) chats.stop(id, scope, false);
                var stopped = chats.get(id, scope).messages().get(1);
                assertEquals(stopFirst ? "CANCELLED" : "TOOL_OUTCOME_UNKNOWN", stopped.getErrorCategory());
                assertEquals(stopFirst, stopped.getWriteOutcomes().isEmpty());
                assertEquals(!stopFirst, Boolean.FALSE.equals(stopped.getRetryable()));
                if (stopFirst) {
                    var retry = new AiChatRequest(id, assistant.getId(), UUID.randomUUID().toString(), 1L, "Save", message);
                    assertEquals(turn.question().getId(), chats.begin(retry, scope, assistant, "config").question().getId());
                }
            }
            audit.verify(() -> Adminlog.add(Adminlog.TYPE_QUIZ, "UPDATE:\nid: 11", 11, -1), times(1));
        }
    }

    @Test
    void auditAttributesEveryWriteAndReportsOnlyDispatchedToolCalls() throws Exception {
        var previous = SetCharacterEncodingFilter.getCurrentRequestBean();
        var request = new RequestBean();
        request.setUserId(42);
        request.setRemoteIP("127.0.0.1");
        request.setUrl("/admin/rest/probe/batch");
        request.setQueryString("value=private-query");
        request.setSpringContext(context);
        SetCharacterEncodingFilter.setCurrentRequestBean(request);
        try (var pool = mockStatic(DBPool.class); var notifications = mockStatic(AdminlogNotifyManager.class)) {
            var connection = mock(Connection.class);
            var statement = mock(PreparedStatement.class);
            pool.when(DBPool::getConnection).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(statement);

            call("audit_probe_batch", "{}", 207);
            assertFalse(request.getAuditValues(false).containsKey("AI tool"));
            assertEquals("value=private-query", request.getQueryString());
            call("ai_tools_preview", "{}", 200);
            mvc.perform(post(BASE + "/ai_tools_preview/call").session(session)
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
            mvc.perform(post("/admin/rest/probe/batch").session(session).header(AiToolAuthorization.ASSISTANT_HEADER, "7"))
                    .andExpect(status().is(207));

            var descriptions = ArgumentCaptor.forClass(String.class);
            verify(statement, times(7)).setString(eq(6), descriptions.capture());
            var rows = descriptions.getAllValues();
            String source = "AI tool: audit_probe_batch via assistant Quiz editor (#7)\nEndpoint: POST /admin/rest/probe/batch";
            assertTrue(rows.get(0).startsWith("UPDATE:\nid: 11\n" + source));
            assertTrue(rows.get(1).startsWith("DELETE:\nid: 12\n" + source));
            assertTrue(rows.get(2).startsWith("Tool call (HTTP 207)\n" + source));
            assertTrue(rows.get(3).startsWith("Tool call (HTTP 200)\nAI tool: ai_tools_preview via assistant Quiz editor (#7)"));
            assertTrue(rows.get(4).contains("AI tool: ai_tools_preview (manual call)"));
            assertTrue(rows.subList(2, 5).stream().noneMatch(value -> value.contains("private-query")));
            assertFalse(rows.get(5).contains("AI tool:"));
            assertFalse(rows.get(6).contains("AI tool:"));
            verify(statement, times(3)).setInt(1, Adminlog.TYPE_AI);
            verify(statement, times(4)).setInt(1, Adminlog.TYPE_QUIZ);
            verify(statement, times(7)).setInt(2, 42);
        } finally {
            SetCharacterEncodingFilter.setCurrentRequestBean(previous);
        }
    }

    private void assertFeedback(Map<String, Object> schema, Object input, String expected) {
        assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> AiToolArgumentValidator.validate(schema, input)).getMessage());
    }

    @Test
    void correctionsExcludeUntrustedPropertyNamesAndValues() {
        var details = context.getBean(AiToolRegistry.class).lookup("ai_tools_details").definition().inputSchema();
        assertFeedback(details, Map.of("names", List.of(), "secret", true), "arguments contains an unknown field. Allowed fields are [names, includeOutputSchema].");
        var number = Map.of("minimum", 1);
        var dynamic = Map.<String, Object>of("patternProperties", Map.of("^search", number), "additionalProperties", number);
        for (String key : List.of("search secret", "secret")) assertFeedback(dynamic, Map.of(key, -12345), "arguments.* must be at least 1.");
    }

}
