package sk.iway.iwcm.components.ai.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.handler.MappedInterceptor;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletRegistration;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.rest.AiToolsController;
import sk.iway.iwcm.components.quiz.rest.QuizQuestionRestController;
import sk.iway.iwcm.components.quiz.rest.QuizRestController;
import sk.iway.iwcm.system.adminlog.AdminlogNotifyManager;
import sk.iway.iwcm.system.datatable.events.DatatableEvent;
import sk.iway.iwcm.system.datatable.events.DatatableEventType;
import sk.iway.iwcm.system.spring.SpringAppInitializer;
import sk.iway.iwcm.system.spring.services.WebjetSecurityService;
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
    @EnableMethodSecurity(proxyTargetClass = true, securedEnabled = true)
    @Import({AiToolsController.class, AiToolRegistry.class, AiToolService.class, AiToolAuthorization.class, AiToolCallFilter.class, AiToolAudit.class})
    static class Config {
        @Bean(name = "WebjetSecurityService")
        WebjetSecurityService security() { return mock(WebjetSecurityService.class); }
        @Bean AssistantDefinitionRepository assistants() { return mock(AssistantDefinitionRepository.class); }
        @Bean QuizRestController quizzes() { return new QuizRestController(null, null, null); }
        @Bean QuizQuestionRestController questions() { return new QuizQuestionRestController(null); }
        @Bean AuditProbeController probe() { return new AuditProbeController(); }
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
        var assistant = new AssistantDefinitionEntity();
        assistant.setId(7L);
        assistant.setName("Quiz editor");
        assistant.setAction("live_chat");
        assistant.setToolNames("ai_tools_preview,ai_tools_details,audit_probe_batch");
        when(context.getBean(AssistantDefinitionRepository.class).findByIdAndDomainId(eq(7L), anyInt()))
                .thenReturn(Optional.of(assistant));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void controllerGroupPrefixEndsAtACompletePathSegment() {
        var registry = context.getBean(AiToolRegistry.class);
        var add = registry.lookup("quiz_add");
        var action = registry.lookup("quiz_action");
        assertEquals("/admin/rest/quiz", new AiToolGroup(QuizRestController.class, List.of(add, action), null).getPathPrefix());
        assertEquals("/admin/rest/quiz/add", new AiToolGroup(QuizRestController.class, List.of(add), null).getPathPrefix());
    }

    @Test
    void assignmentPickerGroupsByComponentPackageAndFiltersByPermission() {
        var request = new MockHttpServletRequest(context.getServletContext());
        request.setSession(session);
        var service = context.getBean(AiToolService.class);
        var tools = service.assignmentTools(request, null);
        assertTrue(tools.stream().anyMatch(tool -> tool.group().equals("quiz") && tool.name().equals("quiz_get_all")));
        assertTrue(tools.stream().anyMatch(tool -> tool.group().equals("quiz") && tool.name().equals("quiz_question_add")
                && tool.httpMethod().equals("POST") && tool.path().equals("/admin/rest/quiz/question/add")));
        assertTrue(tools.stream().anyMatch(tool -> tool.group().equals("ai") && tool.name().equals("ai_tools_preview")));
        assertTrue(tools.stream().anyMatch(tool -> tool.group().equals("ai") && tool.name().equals("audit_probe_batch")));

        when(security.hasPermission("cmp_quiz")).thenReturn(false);
        var visible = service.assignmentTools(request, null);
        assertFalse(visible.stream().anyMatch(tool -> tool.group().equals("quiz") || tool.name().equals("audit_probe_batch")));
        assertTrue(visible.stream().anyMatch(tool -> tool.name().equals("ai_tools_preview")));
    }

    private JsonNode call(String tool, String arguments, int expectedStatus) throws Exception {
        var response = mvc.perform(post(BASE + "/" + tool + "/call").session(session)
                .header(AiToolAuthorization.ASSISTANT_HEADER, "7").contentType(MediaType.APPLICATION_JSON).content(arguments))
                .andExpect(status().is(expectedStatus)).andReturn().getResponse();
        return expectedStatus == 200 ? json.readTree(response.getContentAsByteArray()).get("tools") : null;
    }

    @Test
    void assistantCanPreviewThenInspectOnlySelectedTools() throws Exception {
        var preview = call("ai_tools_preview", "{}", 200);
        for (var tool : preview) assertEquals(Set.of("name", "description"), Set.copyOf(tool.propertyNames()));
        var names = preview.valueStream().map(tool -> tool.get("name").asString()).toList();
        assertEquals(List.of("ai_tools_details", "ai_tools_preview"), names.stream().filter(name -> name.startsWith("ai_tools_")).toList());
        assertTrue(names.contains("quiz_get_quiz_type"));

        var details = call("ai_tools_details", """
                {"names":["quiz_get_quiz_type","unknown","quiz_get_all","quiz_get_quiz_type"]}
                """, 200);
        assertEquals(List.of("quiz_get_quiz_type", "quiz_get_all"), details.valueStream().map(tool -> tool.get("name").asString()).toList());
        var quiz = details.get(0);
        assertEquals("/admin/rest/quiz/quizInfo", quiz.get("path").asString());
        assertEquals("GET", quiz.get("httpMethod").asString());
        assertTrue(quiz.at("/inputSchema/properties/quizId").isObject());
        assertTrue(quiz.get("outputSchema").isObject());
        call("quiz_get_quiz_type", "{\"quizId\":42}", 404);
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
            call("quiz_get_quiz_type", "{\"quizId\":42}", 404);
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

    @Test
    void toolInputUsesStoredChoiceValuesFromEditorMetadata() {
        var schema = context.getBean(AiToolRegistry.class).lookup("quiz_add").definition().inputSchema();
        var jsonSchema = json.valueToTree(schema);
        var bodyRef = jsonSchema.at("/properties/body/$ref").asString();
        var choice = jsonSchema.at(bodyRef.substring(1) + "/properties/quizType");
        assertEquals(List.of("0", "1"), choice.get("enum").valueStream().map(JsonNode::asString).toList());
        assertTrue(choice.get("description").asString().contains("1 = "));
        AiToolArgumentValidator.validate(schema, Map.of("body", Map.of("name", "autotest", "quizType", "1")));
        assertThrows(IllegalArgumentException.class, () -> AiToolArgumentValidator.validate(schema,
                Map.of("body", Map.of("name", "autotest", "quizType", "bodovaný"))));
        var numeric = Map.<String, Object>of("type", "integer", "enum", List.of(java.math.BigInteger.ONE));
        AiToolArgumentValidator.validate(numeric, 1);
        assertThrows(IllegalArgumentException.class, () -> AiToolArgumentValidator.validate(numeric, "1"));
    }

    @Test
    void bothStepsRespectCurrentPermissionsAndDetailsRequireNames() throws Exception {
        when(security.hasPermission("cmp_quiz")).thenReturn(false);
        var preview = call("ai_tools_preview", "{}", 200);
        assertEquals(2, preview.size());
        assertTrue(call("ai_tools_details", "{\"names\":[\"quiz_get_quiz_type\",\"unknown\"]}", 200).isEmpty());
        assertTrue(call("ai_tools_details", "{\"names\":[]}", 200).isEmpty());
        mvc.perform(get(BASE + "/details").session(session)).andExpect(status().isBadRequest());
    }
}
