package sk.iway.iwcm.components.users.sessions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.system.audit.jpa.AuditRepository;
import sk.iway.iwcm.users.UsersDB;

/** Verifies the authenticated ownership and fixed time/type boundaries of personal login history. */
class SessionLoginHistoryTest {
    private final AuditRepository repository = mock(AuditRepository.class);
    private final SessionService service = mock(SessionService.class);
    private final SessionRestController controller = new SessionRestController(service, repository);

    /** History contains only the session user's type-80 records from a server-derived thirty-day period. */
    @Test
    void scopesHistoryToTheSessionUserAndBoundsPagination() throws Exception {
        Identity user = mock(Identity.class);
        when(user.getUserId()).thenReturn(7);
        when(repository.findAllByLogTypeAndUserIdAndCreateDateBetween(anyInt(), anyInt(), any(), any(), any())).thenReturn(Page.empty(org.springframework.data.domain.PageRequest.of(0, 20)));
        try (var users = mockStatic(UsersDB.class)) {
            users.when(() -> UsersDB.getCurrentUser(any(HttpServletRequest.class))).thenReturn(user);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/admin/rest/sessions/login-history")
                .param("page", "-1").param("userId", "8").param("logType", "20").param("days", "365").param("size", "10000")
                .param("sort", "userId,asc")).andExpect(status().isOk());
        }
        var start = ArgumentCaptor.forClass(Timestamp.class);
        var end = ArgumentCaptor.forClass(Timestamp.class);
        var page = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByLogTypeAndUserIdAndCreateDateBetween(eq(Adminlog.TYPE_USER_LOGON), eq(7), start.capture(), end.capture(), page.capture());
        assertEquals(Duration.ofDays(30), Duration.between(start.getValue().toInstant(), end.getValue().toInstant()));
        assertTrue(Math.abs(System.currentTimeMillis() - end.getValue().getTime()) < 5000);
        assertEquals(0, page.getValue().getPageNumber());
        assertEquals(20, page.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "createDate", "id"), page.getValue().getSort());
        assertEquals("@WebjetSecurityService.isLogged()", SessionRestController.class
            .getMethod("history", HttpServletRequest.class, int.class).getAnnotation(PreAuthorize.class).value());
        assertEquals("@WebjetSecurityService.isAdmin()", SessionRestController.class.getAnnotation(PreAuthorize.class).value());
    }

    /** Moving history into the session controller preserves all existing administrator session routes. */
    @Test
    void preservesAdministratorSessionRoutes() throws Exception {
        Identity user = mock(Identity.class);
        when(service.loggedAdministrators(user)).thenReturn(List.of());
        when(service.logoutSession(eq(user), anyString(), eq("autotest-other"))).thenReturn(new SessionLogoutResultDto(true, false));
        when(service.logoutAdministrator(user, 8)).thenReturn(new SessionLogoutResultDto(true, true));
        try (var users = mockStatic(UsersDB.class)) {
            users.when(() -> UsersDB.getCurrentUser(any(HttpServletRequest.class))).thenReturn(user);
            var mvc = MockMvcBuilders.standaloneSetup(controller).build();
            mvc.perform(get("/admin/rest/sessions/administrators")).andExpect(status().isOk()).andExpect(content().string("[]"));
            var mapper = new ObjectMapper();
            var session = mapper.readTree(mvc.perform(post("/admin/rest/sessions/logout").param("sessionId", "autotest-other"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertTrue(session.path("success").asBoolean());
            assertFalse(session.path("pending").asBoolean());
            var administrator = mapper.readTree(mvc.perform(post("/admin/rest/sessions/logout-administrator").param("userId", "8"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertTrue(administrator.path("success").asBoolean());
            assertTrue(administrator.path("pending").asBoolean());
        }
        verifyNoInteractions(repository);
    }

    /** Missing or invalid session identities are denied before any repository access. */
    @Test
    void rejectsUnauthenticatedRequestsBeforeReadingAudit() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        try (var users = mockStatic(UsersDB.class)) {
            assertThrows(AccessDeniedException.class, () -> controller.history(request, 0));
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(new Identity());
            assertThrows(AccessDeniedException.class, () -> controller.history(request, 0));
        }
        verifyNoInteractions(repository);
    }
}
