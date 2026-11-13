package sk.iway.iwcm.components.ai.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.handler.MappedInterceptor;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletRegistration;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.rest.AiToolsController;
import sk.iway.iwcm.components.quiz.rest.QuizRestController;
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

    @EnableWebMvc
    @EnableMethodSecurity(proxyTargetClass = true, securedEnabled = true)
    @Import({AiToolsController.class, AiToolRegistry.class, AiToolService.class, AiToolAuthorization.class, AiToolCallFilter.class})
    static class Config {
        @Bean(name = "WebjetSecurityService")
        WebjetSecurityService security() { return mock(WebjetSecurityService.class); }
        @Bean AssistantDefinitionRepository assistants() { return mock(AssistantDefinitionRepository.class); }
        @Bean QuizRestController quizzes() { return new QuizRestController(null, null, null); }
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
        assistant.setAction("live_chat");
        assistant.setToolNames("ai_tools_preview,ai_tools_details");
        when(context.getBean(AssistantDefinitionRepository.class).findByIdAndDomainId(eq(7L), anyInt()))
                .thenReturn(Optional.of(assistant));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        context.close();
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
