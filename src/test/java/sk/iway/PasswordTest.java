package sk.iway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.users.PasswordsHistoryDB;

class PasswordTest {

    private static final String LAST_DATE_SQL = "SELECT max(create_date) as create_date FROM " + ConfDB.ADMINLOG_TABLE_NAME
            + " WHERE user_id=? AND log_type=?";
    private static final String LAST_DATE_BY_SUB_ID2_SQL = LAST_DATE_SQL + " AND sub_id2=?";

    String[] textArr = {"heslo",                               "03101969",                          "ucitel",                           "M@gAZloyiT3;Hes!o"};
    String[] hashArr = {"d7ed8dc6fc9b4a8c3b442c3dcc35bfe4",    "437b9363e304a0a890fa46932bc510f6",  "447144233c3cb432b945dd460f812676", "0b8e3ba673324ac9301622d92487063201e444e264b84372cbf20c600b82f181"};

    @Test
    void testDecrypt() {
        for (int i=0; i<textArr.length; i++) {
            String text = textArr[i];
            String hash = hashArr[i];
            try {
                Password password = new Password();
                String decryptedPassword = password.decrypt(hash);
                assertNotNull(decryptedPassword);
                assertFalse(decryptedPassword.isEmpty());
                assertEquals(text, decryptedPassword);
            } catch (Exception e) {
                fail("An exception occurred: " + e.getMessage());
            }
        }
    }

    @Test
    void testEncrypt() {
        for (int i=0; i<textArr.length; i++) {
            String text = textArr[i];
            String hash = hashArr[i];
            try {
                Password password = new Password();
                String encryptedPassword = password.encrypt(text);
                assertNotNull(encryptedPassword);
                assertFalse(encryptedPassword.isEmpty());
                assertEquals(hash, encryptedPassword);
            } catch (Exception e) {
                fail("An exception occurred: " + e.getMessage());
            }
        }
    }

    @Test
    void passwordExpiryShouldIgnoreNewerResetRequest() throws Exception {
        assertPasswordValidity(60, false, "A password reset request must not extend password validity");
    }

    @Test
    void passwordExpiryShouldAcceptRecentSuccessfulChange() throws Exception {
        assertPasswordValidity(2, true, "A recent successful password change must keep the password valid");
    }

    private static void assertPasswordValidity(
            int passwordChangeAgeInDays,
            boolean expected,
            String assertionMessage) throws Exception {
        int userId = 123;
        int successfulPasswordChangeMarker = -1;
        String password = "StrongPassword1!";
        String language = "sk";
        long now = System.currentTimeMillis();
        Timestamp successfulPasswordChange = new Timestamp(
                now - TimeUnit.DAYS.toMillis(passwordChangeAgeInDays));
        Timestamp newerResetRequest = new Timestamp(now - TimeUnit.DAYS.toMillis(1));

        HttpSession session = mock(HttpSession.class);
        Prop prop = mock(Prop.class);
        PasswordsHistoryDB passwordsHistory = mock(PasswordsHistoryDB.class);
        Connection connection = mock(Connection.class);
        PreparedStatement filteredStatement = mock(PreparedStatement.class);
        PreparedStatement unfilteredStatement = mock(PreparedStatement.class);
        ResultSet filteredResult = mock(ResultSet.class);
        ResultSet unfilteredResult = mock(ResultSet.class);

        when(connection.prepareStatement(LAST_DATE_BY_SUB_ID2_SQL)).thenReturn(filteredStatement);
        when(connection.prepareStatement(LAST_DATE_SQL)).thenReturn(unfilteredStatement);
        when(filteredStatement.executeQuery()).thenReturn(filteredResult);
        when(unfilteredStatement.executeQuery()).thenReturn(unfilteredResult);
        when(filteredResult.next()).thenReturn(true);
        when(unfilteredResult.next()).thenReturn(true);
        when(filteredResult.getTimestamp("create_date")).thenReturn(successfulPasswordChange);
        when(unfilteredResult.getTimestamp("create_date")).thenReturn(newerResetRequest);
        when(passwordsHistory.existsPassword(password, userId)).thenReturn(false);

        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<Prop> props = mockStatic(Prop.class);
                MockedStatic<PasswordsHistoryDB> passwordHistories = mockStatic(PasswordsHistoryDB.class);
                MockedStatic<DBPool> dbPool = mockStatic(DBPool.class)) {
            constants.when(Constants::getInstallName).thenReturn("");
            constants.when(() -> Constants.getInt("passwordAdminMinLength")).thenReturn(8);
            constants.when(() -> Constants.getInt("passwordAdminMinCountOfSpecialSigns")).thenReturn(1);
            constants.when(() -> Constants.getInt("passwordAdminMinUpperCaseLetters")).thenReturn(1);
            constants.when(() -> Constants.getInt("passwordAdminMinLowerCaseLetters")).thenReturn(1);
            constants.when(() -> Constants.getInt("passwordAdminMinCountOfDigits")).thenReturn(1);
            constants.when(() -> Constants.getInt("passwordAdminExpiryDays")).thenReturn(30);
            props.when(() -> Prop.getLng(session)).thenReturn(language);
            props.when(() -> Prop.getInstance(language)).thenReturn(prop);
            passwordHistories.when(PasswordsHistoryDB::getInstance).thenReturn(passwordsHistory);
            dbPool.when(DBPool::getConnection).thenReturn(connection);

            assertEquals(expected,
                    Password.checkPassword(true, password, true, userId, session, null),
                    assertionMessage);
        }

        verify(connection).prepareStatement(LAST_DATE_BY_SUB_ID2_SQL);
        verify(filteredStatement).setInt(1, userId);
        verify(filteredStatement).setInt(2, Adminlog.TYPE_USER_CHANGE_PASSWORD);
        verify(filteredStatement).setInt(3, successfulPasswordChangeMarker);
        verify(filteredStatement).executeQuery();
        verify(filteredResult).next();
        verify(filteredResult).getTimestamp("create_date");
        verifyNoInteractions(unfilteredStatement, unfilteredResult);
    }
}
