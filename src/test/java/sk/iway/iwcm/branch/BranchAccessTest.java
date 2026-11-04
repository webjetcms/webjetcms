package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.stripes.CSRF;
import sk.iway.iwcm.users.UsersDB;

/** Covers functional rights, trusted domain selection and the existing Ajax CSRF contract. */
class BranchAccessTest {
    private final BranchAccess access = new BranchAccess();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    void disabledFeatureAndNonAdminCannotReachBranchStorage() {
        try (var constants = mockStatic(Constants.class); var users = mockStatic(UsersDB.class)) {
            assertStatus(HttpStatus.NOT_FOUND, () -> access.requireUser(request));
            constants.when(() -> Constants.getBoolean("branchesEnabled")).thenReturn(true);
            assertStatus(HttpStatus.FORBIDDEN, () -> access.requireUser(request));
            Identity user = mock(Identity.class);
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(user);
            assertStatus(HttpStatus.FORBIDDEN, () -> access.requireUser(request));
            when(user.isAdmin()).thenReturn(true);
            assertStatus(HttpStatus.FORBIDDEN, () -> access.requireUser(request));
            when(user.isEnabledItem(BranchAccess.PERMISSION)).thenReturn(true);
            assertSame(user, access.requireUser(request));
        }
    }

    @Test
    void usesSessionDomainAndRejectsForeignOrStaleDomain() {
        Identity user = mock(Identity.class);
        GroupsDB groups = mock(GroupsDB.class);
        request.getSession().setAttribute("preview.editorDomainName", "a.example.sk");
        request.addParameter("docid", "999");
        try (var db = mockStatic(GroupsDB.class)) {
            db.when(GroupsDB::getInstance).thenReturn(groups);
            when(groups.getUserRootDomainNames(user)).thenReturn(List.of("a.example.sk"));
            assertEquals("a.example.sk", access.requireDomain(request, user));
            assertStatus(HttpStatus.CONFLICT, () -> access.requireExpectedDomain("a.example.sk", "b.example.sk"));
            assertStatus(HttpStatus.CONFLICT, () -> access.requireExpectedDomain("a.example.sk", null));
            request.getSession().setAttribute("preview.editorDomainName", "b.example.sk");
            assertStatus(HttpStatus.FORBIDDEN, () -> access.requireDomain(request, user));
        }
    }

    @Test
    void csrfRequiresExistingTokenUnlessExistingApiTokenFilterExemptsRequest() {
        assertStatus(HttpStatus.FORBIDDEN, () -> access.requireCsrf(request));
        request.getSession();
        request.addHeader("X-CSRF-Token", "test-token");
        try (var csrf = mockStatic(CSRF.class)) {
            assertStatus(HttpStatus.FORBIDDEN, () -> access.requireCsrf(request));
            csrf.when(() -> CSRF.verifyTokenAjax(request.getSession(), "test-token")).thenReturn(true);
            assertDoesNotThrow(() -> access.requireCsrf(request));
        }
        request.setAttribute("csrfDisabled", true);
        assertDoesNotThrow(() -> access.requireCsrf(request));
    }

    private static void assertStatus(HttpStatus status, Runnable action) {
        assertEquals(status, assertThrows(ResponseStatusException.class, action::run).getStatusCode());
    }
}
