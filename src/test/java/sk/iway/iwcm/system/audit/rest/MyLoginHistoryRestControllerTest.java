package sk.iway.iwcm.system.audit.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.system.audit.jpa.AuditRepository;
import sk.iway.iwcm.users.UsersDB;

/** Verifies the authenticated ownership and fixed time/type boundaries of personal login history. */
class MyLoginHistoryRestControllerTest {
    private final AuditRepository repository = mock(AuditRepository.class);
    private final MyLoginHistoryRestController controller = new MyLoginHistoryRestController(repository);

    /** An ordinary signed-in user can read only their own type-80 records from a server-derived thirty-day period. */
    @Test
    void scopesHistoryToTheSessionUserAndBoundsPagination() throws Exception {
        Identity user = mock(Identity.class);
        when(user.getUserId()).thenReturn(7);
        when(repository.findAllByLogTypeAndUserIdAndCreateDateBetween(anyInt(), anyInt(), any(), any(), any())).thenReturn(Page.empty(org.springframework.data.domain.PageRequest.of(0, 20)));
        try (var users = mockStatic(UsersDB.class)) {
            users.when(() -> UsersDB.getCurrentUser(any(jakarta.servlet.http.HttpServletRequest.class))).thenReturn(user);
            MockMvcBuilders.standaloneSetup(controller).build().perform(get("/rest/audit/my-login-history")
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
        assertEquals("@WebjetSecurityService.isLogged()", MyLoginHistoryRestController.class.getAnnotation(PreAuthorize.class).value());
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
