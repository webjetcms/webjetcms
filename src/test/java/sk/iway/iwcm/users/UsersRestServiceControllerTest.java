package sk.iway.iwcm.users;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.system.stripes.CSRF;

/** Verifies the existing second-factor response contract and invalidation after successful saves. */
class UsersRestServiceControllerTest {
    private final UserDetailsRepository repository = mock(UserDetailsRepository.class);
    private final TwoFactorStatusService status = mock(TwoFactorStatusService.class);
    private final UsersRestServiceController controller = new UsersRestServiceController(repository, status);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final Identity user = new Identity();

    UsersRestServiceControllerTest() {
        user.setUserId(7);
    }

    /** The setup GET retains its true-or-empty contract and delegates to the display cache. */
    @ParameterizedTest
    @CsvSource(value = {"true,true", "false,''"})
    void returnsExistingStatusContract(boolean enabled, String expected) {
        try (var users = mockStatic(UsersDB.class)) {
            users.when(() -> UsersDB.getCurrentUser(request.getSession())).thenReturn(user);
            when(status.isEnabled(7)).thenReturn(enabled);

            assertEquals(expected, controller.getGauth(request));
            verify(status).isEnabled(7);
            verifyNoInteractions(repository);
        }
    }

    /** Enabling and disabling both invalidate only after the setup has been persisted. */
    @ParameterizedTest
    @CsvSource(value = {"new-key", "''"})
    void invalidatesAfterSuccessfulSave(String secret) {
        try (var users = mockStatic(UsersDB.class); var csrf = mockStatic(CSRF.class)) {
            users.when(() -> UsersDB.getCurrentUser(request.getSession())).thenReturn(user);
            csrf.when(() -> CSRF.verifyTokenAndDeleteIt(request)).thenReturn(true);

            assertEquals("", controller.setGauth(secret, request));

            var order = inOrder(repository, status);
            order.verify(repository).updateMobileDeviceByUserId(7L, secret);
            order.verify(status).invalidate(7);
        }
    }

    /** A rejected CSRF token cannot update setup or evict the cached status. */
    @Test
    void rejectedCsrfDoesNotInvalidate() {
        try (var users = mockStatic(UsersDB.class); var csrf = mockStatic(CSRF.class)) {
            users.when(() -> UsersDB.getCurrentUser(request.getSession())).thenReturn(user);

            assertEquals("CSRF token is not valid", controller.setGauth("new-key", request));
            verifyNoInteractions(repository, status);
        }
    }

}
