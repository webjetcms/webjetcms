package sk.iway.iwcm.users;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import sk.iway.iwcm.AdminlogBean;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;

class UserChangePasswordServiceTest {

    private static final String RESET_LOGIN = "reset-request-owner";
    private static final String AUTH = "123456";

    @Test
    void verifyLoginValueShouldRejectSingleLoginWhenResetRecordDoesNotExist() {
        try (MockedStatic<Constants> constants = mockPasswordResetValidity();
                MockedStatic<UsersDB> users = mockUser(RESET_LOGIN);
                MockedConstruction<ComplexQuery> ignored = mockAdminlogQuery(List.of())) {
            assertFalse(UserChangePasswordService.verifyLoginValue(RESET_LOGIN, RESET_LOGIN, AUTH, null),
                    "A password reset without a matching audit record must be rejected");
        }
    }

    @Test
    void verifyLoginValueShouldRejectDifferentSelectedLoginForSingleLoginReset() {
        AdminlogBean resetRecord = createResetRecord(0);

        try (MockedStatic<Constants> constants = mockPasswordResetValidity();
                MockedStatic<UsersDB> users = mockUser(RESET_LOGIN);
                MockedConstruction<ComplexQuery> ignored = mockAdminlogQuery(List.of(resetRecord))) {
            assertFalse(UserChangePasswordService.verifyLoginValue(RESET_LOGIN, "admin", AUTH, null),
                    "A reset record must not authorize changing a different login");
        }
    }

    @Test
    void verifyLoginValueShouldAcceptMatchingLoginWithValidResetRecord() {
        AdminlogBean resetRecord = createResetRecord(0);

        try (MockedStatic<Constants> constants = mockPasswordResetValidity();
                MockedStatic<UsersDB> users = mockUser(RESET_LOGIN);
                MockedConstruction<ComplexQuery> ignored = mockAdminlogQuery(List.of(resetRecord))) {
            assertTrue(UserChangePasswordService.verifyLoginValue(RESET_LOGIN, RESET_LOGIN, AUTH, null),
                    "A fresh reset record must authorize its matching login");
        }
    }

    @Test
    void verifyLoginValueShouldNotTrustCustomSendPasswordMethodWithoutResetRecord() {
        try (MockedStatic<Constants> constants = mockPasswordResetValidity();
                MockedStatic<UsersDB> users = mockUser(RESET_LOGIN);
                MockedConstruction<ComplexQuery> ignored = mockAdminlogQuery(List.of())) {
            constants.when(() -> Constants.getString("sendPasswordMethod")).thenReturn("custom.PasswordReset.send");

            assertFalse(UserChangePasswordService.verifyLoginValue(RESET_LOGIN, RESET_LOGIN, AUTH, null),
                    "A custom password sender must not bypass reset-token verification");
        }
    }

    @Test
    void verifyLoginValueShouldRejectNonResetPasswordAuditRecord() {
        AdminlogBean passwordChangeRecord = createResetRecord(0);
        passwordChangeRecord.setSubId2(-1);

        try (MockedStatic<Constants> constants = mockPasswordResetValidity();
                MockedStatic<UsersDB> users = mockUser(RESET_LOGIN);
                MockedConstruction<ComplexQuery> ignored = mockAdminlogQuery(List.of(passwordChangeRecord))) {
            assertFalse(UserChangePasswordService.verifyLoginValue(RESET_LOGIN, RESET_LOGIN, AUTH, null),
                    "A regular password-change audit record must not be accepted as a reset token");
        }
    }

    @Test
    void verifyLoginValueShouldRejectExpiredResetRecord() {
        AdminlogBean expiredResetRecord = createResetRecord(31);

        try (MockedStatic<Constants> constants = mockPasswordResetValidity();
                MockedStatic<UsersDB> users = mockUser(RESET_LOGIN);
                MockedConstruction<ComplexQuery> ignored = mockAdminlogQuery(List.of(expiredResetRecord))) {
            assertFalse(UserChangePasswordService.verifyLoginValue(RESET_LOGIN, RESET_LOGIN, AUTH, null),
                    "An expired reset record must be rejected on form submission");
        }
    }

    private static AdminlogBean createResetRecord(int ageInMinutes) {
        AdminlogBean record = new AdminlogBean();
        record.setSubId2(UsersDB.APPROVE_APPROVE);
        record.setCreateDate(new Timestamp(System.currentTimeMillis() - ageInMinutes * 60L * 1000L));
        return record;
    }

    private static MockedStatic<Constants> mockPasswordResetValidity() {
        MockedStatic<Constants> constants = mockStatic(Constants.class);
        constants.when(() -> Constants.getInt("passwordResetValidityInMinutes")).thenReturn(30);
        return constants;
    }

    private static MockedStatic<UsersDB> mockUser(String login) {
        UserDetails user = mock(UserDetails.class);
        when(user.getUserId()).thenReturn(1);

        MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
        users.when(() -> UsersDB.getUser(login)).thenReturn(user);
        return users;
    }

    @SuppressWarnings({ "unchecked" })
    private static MockedConstruction<ComplexQuery> mockAdminlogQuery(List<AdminlogBean> records) {
        return mockConstruction(ComplexQuery.class, (query, context) -> {
            when(query.setSql(anyString())).thenReturn(query);
            when(query.setParams(any(Object[].class))).thenReturn(query);
            doReturn(records).when(query).list(any(Mapper.class));
        });
    }
}
