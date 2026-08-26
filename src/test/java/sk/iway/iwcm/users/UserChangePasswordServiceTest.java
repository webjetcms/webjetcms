package sk.iway.iwcm.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.AdminlogBean;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.system.ConfDB;

class UserChangePasswordServiceTest {

    private static final String RESET_LOGIN = "reset-request-owner";
    private static final String AUTH = "123456";
    private static final int AUTH_VALUE = Integer.parseInt(AUTH);
    private static final int RESET_ADMINLOG_MARKER = 0;
    private static final String RESET_ADMINLOG_SELECT_SQL = "SELECT * FROM " + ConfDB.ADMINLOG_TABLE_NAME
            + " WHERE log_type=? AND user_id = ? AND sub_id1 = ? AND sub_id2 = ?";
    private static final String RESET_ADMINLOG_DELETE_SQL = "DELETE FROM " + ConfDB.ADMINLOG_TABLE_NAME
            + " WHERE log_type=? AND sub_id1=? AND sub_id2=?";

    @Test
    void createChangePasswordAdminlogBeansShouldUseSameTokenForEveryIssuedUser() {
        UserDetailsEntity firstUser = mockUserEntity(11L);
        UserDetailsEntity secondUser = mockUserEntity(22L);

        try (MockedStatic<Adminlog> adminlog = mockStatic(Adminlog.class)) {
            UserChangePasswordService.createChangePasswordAdminlogBeans(
                    List.of(firstUser, secondUser),
                    AUTH_VALUE);

            adminlog.verify(() -> Adminlog.add(
                    eq(Adminlog.TYPE_USER_CHANGE_PASSWORD),
                    eq(11),
                    anyString(),
                    eq(AUTH_VALUE),
                    eq(RESET_ADMINLOG_MARKER)));
            adminlog.verify(() -> Adminlog.add(
                    eq(Adminlog.TYPE_USER_CHANGE_PASSWORD),
                    eq(22),
                    anyString(),
                    eq(AUTH_VALUE),
                    eq(RESET_ADMINLOG_MARKER)));
            adminlog.verifyNoMoreInteractions();
        }
    }

    @Test
    void deleteChangePasswordAdminlogBeanShouldConsumeAllRecordsForAuthInSingleStatement() {
        assertAllResetRecordsConsumed(() -> UserChangePasswordService.deleteChangePasswordAdminlogBean(
                RESET_LOGIN + UserChangePasswordService.LOGINS_SEPARATOR + "second-login",
                AUTH));
    }

    @Test
    void deleteChangePasswordAdminlogBeanForUserShouldConsumeAllRecordsForAuthInSingleStatement() {
        UserDetails user = mock(UserDetails.class);
        assertAllResetRecordsConsumed(() -> UserChangePasswordService.deleteChangePasswordAdminlogBean(user, AUTH));
    }

    @Test
    void verifyLoginValueShouldRejectSingleLoginWhenResetRecordDoesNotExist() {
        try (MockedStatic<Constants> constants = mockPasswordResetValidity();
                MockedStatic<UsersDB> users = mockUser(RESET_LOGIN);
                MockedConstruction<ComplexQuery> queries = mockAdminlogQuery(1, List.of())) {
            assertFalse(UserChangePasswordService.verifyLoginValue(RESET_LOGIN, RESET_LOGIN, AUTH, null),
                    "A password reset without a matching audit record must be rejected");

            assertResetQueryTargetsUser(queries, 1);
        }
    }

    @Test
    void verifyLoginValueShouldRejectDifferentSelectedLoginForSingleLoginReset() {
        try (MockedStatic<Constants> constants = mockPasswordResetValidity()) {
            assertFalse(UserChangePasswordService.verifyLoginValue(RESET_LOGIN, "admin", AUTH, null),
                    "A reset record must not authorize changing a different login");
        }
    }

    @Test
    void verifyLoginValueShouldAcceptMatchingLoginWithValidResetRecord() {
        AdminlogBean resetRecord = createResetRecord(0);

        try (MockedStatic<Constants> constants = mockPasswordResetValidity();
                MockedStatic<UsersDB> users = mockUser(RESET_LOGIN);
                MockedConstruction<ComplexQuery> queries = mockAdminlogQuery(1, List.of(resetRecord))) {
            assertTrue(UserChangePasswordService.verifyLoginValue(RESET_LOGIN, RESET_LOGIN, AUTH, null),
                    "A fresh reset record must authorize its matching login");

            assertResetQueryTargetsUser(queries, 1);
        }
    }

    @Test
    void verifyLoginValueShouldNotTrustCustomSendPasswordMethodWithoutResetRecord() {
        try (MockedStatic<Constants> constants = mockPasswordResetValidity();
                MockedStatic<UsersDB> users = mockUser(RESET_LOGIN);
                MockedConstruction<ComplexQuery> queries = mockAdminlogQuery(1, List.of())) {
            constants.when(() -> Constants.getString("sendPasswordMethod")).thenReturn("custom.PasswordReset.send");

            assertFalse(UserChangePasswordService.verifyLoginValue(RESET_LOGIN, RESET_LOGIN, AUTH, null),
                    "A custom password sender must not bypass reset-token verification");

            assertResetQueryTargetsUser(queries, 1);
        }
    }

    @Test
    void verifyLoginValueShouldRejectNonResetPasswordAuditRecord() {
        AdminlogBean passwordChangeRecord = createResetRecord(0);
        passwordChangeRecord.setSubId2(-1);

        try (MockedStatic<Constants> constants = mockPasswordResetValidity();
                MockedStatic<UsersDB> users = mockUser(RESET_LOGIN);
                MockedConstruction<ComplexQuery> queries = mockAdminlogQuery(1, List.of(passwordChangeRecord))) {
            assertFalse(UserChangePasswordService.verifyLoginValue(RESET_LOGIN, RESET_LOGIN, AUTH, null),
                    "A regular password-change audit record must not be accepted as a reset token");

            assertResetQueryTargetsUser(queries, 1);
        }
    }

    @Test
    void verifyLoginValueShouldRejectExpiredResetRecord() {
        AdminlogBean expiredResetRecord = createResetRecord(31);

        try (MockedStatic<Constants> constants = mockPasswordResetValidity();
                MockedStatic<UsersDB> users = mockUser(RESET_LOGIN);
                MockedConstruction<ComplexQuery> queries = mockAdminlogQuery(1, List.of(expiredResetRecord))) {
            assertFalse(UserChangePasswordService.verifyLoginValue(RESET_LOGIN, RESET_LOGIN, AUTH, null),
                    "An expired reset record must be rejected on form submission");

            assertResetQueryTargetsUser(queries, 1);
        }
    }

    @Test
    void verifyLoginValueShouldRequireTokenForSelectedLoginInMultiLoginFlow() {
        String selectedLogin = "admin";

        try (MockedStatic<Constants> constants = mockPasswordResetValidity();
                MockedStatic<UsersDB> users = mockUser(selectedLogin, 2);
                MockedConstruction<ComplexQuery> queries = mockAdminlogQuery(2, List.of())) {
            assertFalse(UserChangePasswordService.verifyLoginValue(
                    RESET_LOGIN + UserChangePasswordService.LOGINS_SEPARATOR + selectedLogin,
                    selectedLogin,
                    AUTH,
                    null),
                    "A token issued only for the first login must not authorize an injected login");

            assertResetQueryTargetsUser(queries, 2);
        }
    }

    @Test
    void verifyLoginValueShouldAcceptIssuedTokenForSecondLogin() {
        String selectedLogin = "second-login";
        AdminlogBean resetRecord = createResetRecord(0);

        try (MockedStatic<Constants> constants = mockPasswordResetValidity();
                MockedStatic<UsersDB> users = mockUser(selectedLogin, 2);
                MockedConstruction<ComplexQuery> queries = mockAdminlogQuery(2, List.of(resetRecord))) {
            assertTrue(UserChangePasswordService.verifyLoginValue(
                    RESET_LOGIN + UserChangePasswordService.LOGINS_SEPARATOR + selectedLogin,
                    selectedLogin,
                    AUTH,
                    null),
                    "A reset record issued for the selected login must remain valid in a multi-login flow");

            assertResetQueryTargetsUser(queries, 2);
        }
    }

    @Test
    void verifyLoginValueShouldRejectSelectedLoginMissingFromIssuedList() {
        try (MockedStatic<Constants> constants = mockPasswordResetValidity()) {
            assertFalse(UserChangePasswordService.verifyLoginValue(RESET_LOGIN, "admin", AUTH, null),
                    "The selected login must be present in the login list issued with the reset link");
        }
    }

    private static AdminlogBean createResetRecord(int ageInMinutes) {
        AdminlogBean record = new AdminlogBean();
        record.setSubId2(RESET_ADMINLOG_MARKER);
        record.setCreateDate(new Timestamp(System.currentTimeMillis() - ageInMinutes * 60L * 1000L));
        return record;
    }

    private static MockedStatic<Constants> mockPasswordResetValidity() {
        MockedStatic<Constants> constants = mockStatic(Constants.class);
        constants.when(() -> Constants.getInt("passwordResetValidityInMinutes")).thenReturn(30);
        return constants;
    }

    private static MockedStatic<UsersDB> mockUser(String login) {
        return mockUser(login, 1);
    }

    private static MockedStatic<UsersDB> mockUser(String login, int userId) {
        UserDetails user = mock(UserDetails.class);
        when(user.getUserId()).thenReturn(userId);

        MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
        users.when(() -> UsersDB.getUser(login)).thenReturn(user);
        return users;
    }

    private static UserDetailsEntity mockUserEntity(long userId) {
        UserDetailsEntity user = mock(UserDetailsEntity.class);
        when(user.getId()).thenReturn(userId);
        return user;
    }

    private static void assertAllResetRecordsConsumed(Runnable deleteAction) {
        try (MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class)) {
            deleteAction.run();

            assertEquals(1, queries.constructed().size());
            SimpleQuery query = queries.constructed().get(0);
            verify(query).execute(
                    RESET_ADMINLOG_DELETE_SQL,
                    Adminlog.TYPE_USER_CHANGE_PASSWORD,
                    AUTH_VALUE,
                    RESET_ADMINLOG_MARKER);
            verifyNoMoreInteractions(query);
        }
    }

    @SuppressWarnings({ "unchecked" })
    private static void assertResetQueryTargetsUser(MockedConstruction<ComplexQuery> queries, int userId) {
        assertEquals(1, queries.constructed().size());
        ComplexQuery query = queries.constructed().get(0);
        verify(query).setSql(RESET_ADMINLOG_SELECT_SQL);
        verify(query).setParams(
                Adminlog.TYPE_USER_CHANGE_PASSWORD,
                userId,
                AUTH_VALUE,
                RESET_ADMINLOG_MARKER);
        verify(query).list(any(Mapper.class));
        verifyNoMoreInteractions(query);
    }

    @SuppressWarnings({ "unchecked" })
    private static MockedConstruction<ComplexQuery> mockAdminlogQuery(
            int expectedUserId,
            List<AdminlogBean> records) {
        return mockConstruction(ComplexQuery.class, (query, context) -> {
            when(query.setSql(RESET_ADMINLOG_SELECT_SQL)).thenReturn(query);
            when(query.setParams(
                    Adminlog.TYPE_USER_CHANGE_PASSWORD,
                    expectedUserId,
                    AUTH_VALUE,
                    RESET_ADMINLOG_MARKER)).thenReturn(query);
            doReturn(records).when(query).list(any(Mapper.class));
        });
    }
}
