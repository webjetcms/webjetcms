package sk.iway.iwcm.users;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.AdminlogBean;
import sk.iway.iwcm.Constants;

class UserChangePasswordServiceTest {

    @Test
    void verifyLoginValueShouldRejectSingleLoginWhenResetRecordDoesNotExist() {
        String login = "admin";
        String auth = "1";

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<UserChangePasswordService> service = mockStatic(UserChangePasswordService.class)) {
            constants.when(() -> Constants.getString("sendPasswordMethod")).thenReturn("");
            service.when(() -> UserChangePasswordService.getChangePasswordAdminlogBean(login, auth)).thenReturn(null);
            service.when(() -> UserChangePasswordService.verifyLoginValue(login, login, auth, null)).thenCallRealMethod();

            assertFalse(UserChangePasswordService.verifyLoginValue(login, login, auth, null),
                    "A password reset without a matching audit record must be rejected");
        }
    }

    @Test
    void verifyLoginValueShouldRejectDifferentSelectedLoginForSingleLoginReset() {
        String resetLogin = "reset-request-owner";
        String selectedLogin = "admin";
        String auth = "123456";
        AdminlogBean resetRecord = mock(AdminlogBean.class);

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<UserChangePasswordService> service = mockStatic(UserChangePasswordService.class)) {
            constants.when(() -> Constants.getString("sendPasswordMethod")).thenReturn("");
            service.when(() -> UserChangePasswordService.getChangePasswordAdminlogBean(resetLogin, auth)).thenReturn(resetRecord);
            service.when(() -> UserChangePasswordService.verifyLoginValue(resetLogin, selectedLogin, auth, null)).thenCallRealMethod();

            assertFalse(UserChangePasswordService.verifyLoginValue(resetLogin, selectedLogin, auth, null),
                    "A reset record must not authorize changing a different login");
        }
    }
}
