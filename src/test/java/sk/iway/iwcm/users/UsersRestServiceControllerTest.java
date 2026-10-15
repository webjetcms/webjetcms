package sk.iway.iwcm.users;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.system.stripes.CSRF;

/** Verifies fresh second-factor status, saving and CSRF protection. */
class UsersRestServiceControllerTest {
    private final UserDetailsRepository repository = mock(UserDetailsRepository.class);
    private final UsersRestServiceController controller = new UsersRestServiceController(repository);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final Identity user = new Identity();

    UsersRestServiceControllerTest() {
        user.setUserId(7);
    }

    /** The setup GET reads the current key and exposes only its true-or-empty status. */
    @ParameterizedTest
    @CsvSource(value = {"true,true", "false,''"})
    void returnsExistingStatusContract(boolean enabled, String expected) {
        try (var users = mockStatic(UsersDB.class); var tools = mockStatic(Tools.class, CALLS_REAL_METHODS)) {
            users.when(() -> UsersDB.getCurrentUser(request.getSession())).thenReturn(user);
            when(repository.getMobileDeviceByUserId(7L)).thenReturn(enabled ? "autotest-secret" : "");

            assertEquals(expected, controller.getGauth(request));
            verify(repository).getMobileDeviceByUserId(7L);
        }
    }

    /** Enabling and disabling both persist the requested setting. */
    @ParameterizedTest
    @CsvSource(value = {"new-key", "''"})
    void savesSecondFactorSetting(String secret) {
        try (var users = mockStatic(UsersDB.class); var csrf = mockStatic(CSRF.class)) {
            users.when(() -> UsersDB.getCurrentUser(request.getSession())).thenReturn(user);
            csrf.when(() -> CSRF.verifyTokenAndDeleteIt(request)).thenReturn(true);

            assertEquals("", controller.setGauth(secret, request));

            verify(repository).updateMobileDeviceByUserId(7L, secret);
        }
    }

    /** A rejected CSRF token cannot update setup. */
    @Test
    void rejectedCsrfDoesNotSave() {
        try (var users = mockStatic(UsersDB.class); var csrf = mockStatic(CSRF.class)) {
            users.when(() -> UsersDB.getCurrentUser(request.getSession())).thenReturn(user);

            assertEquals("CSRF token is not valid", controller.setGauth("new-key", request));
            verifyNoInteractions(repository);
        }
    }

}
