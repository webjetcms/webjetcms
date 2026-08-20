package sk.iway.iwcm.setup;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class SetupActionsServiceTest {

    @Test
    void createsPostgresqlDatabaseFromTemplateZeroWithUtf8Encoding() {
        assertEquals(
            "CREATE DATABASE \"webjet_web\" WITH TEMPLATE template0 ENCODING 'UTF8'",
            SetupActionsService.getCreateDatabaseSql("org.postgresql.Driver", "webjet_web")
        );
    }

    @Test
    void createsMariaDbDatabaseWithFullUnicodeCharacterSet() {
        assertEquals(
            "CREATE DATABASE `webjet_web` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci",
            SetupActionsService.getCreateDatabaseSql("org.mariadb.jdbc.Driver", "webjet_web")
        );
    }

    @Test
    void createsMicrosoftSqlDatabaseWithSupplementaryCharacterCollation() {
        assertEquals(
            "CREATE DATABASE [webjet_web] COLLATE Latin1_General_CI_AI",
            SetupActionsService.getCreateDatabaseSql("net.sourceforge.jtds.jdbc.Driver", "webjet_web")
        );
    }

    @Test
    void quotesDatabaseNameAsIdentifier() {
        assertEquals(
            "CREATE DATABASE \"name\"\"with_quote\" WITH TEMPLATE template0 ENCODING 'UTF8'",
            SetupActionsService.getCreateDatabaseSql("org.postgresql.Driver", "name\"with_quote")
        );
        assertEquals(
            "CREATE DATABASE [name]]with_bracket] COLLATE Latin1_General_CI_AI",
            SetupActionsService.getCreateDatabaseSql("net.sourceforge.jtds.jdbc.Driver", "name]with_bracket")
        );
    }

    @Test
    void doesNotAttemptToCreateOracleDatabaseThroughApplicationConnection() {
        assertThrows(IllegalArgumentException.class, () ->
            SetupActionsService.getCreateDatabaseSql("oracle.jdbc.driver.OracleDriver", "webjet_web")
        );
    }

    @Test
    void acceptsRecommendedOracleCharacterSets() throws Exception {
        Connection connection = oracleConnection("AL32UTF8", "AL16UTF16");

        assertDoesNotThrow(() ->
            SetupActionsService.validateDatabaseCharacterSet(connection, "oracle.jdbc.driver.OracleDriver")
        );
    }

    @Test
    void rejectsLegacyOracleUtf8CharacterSet() throws Exception {
        Connection connection = oracleConnection("UTF8", "AL16UTF16");

        SQLException exception = assertThrows(SQLException.class, () ->
            SetupActionsService.validateDatabaseCharacterSet(connection, "oracle.jdbc.driver.OracleDriver")
        );
        assertEquals(
            "Oracle database must use NLS_CHARACTERSET=AL32UTF8, current value is UTF8",
            exception.getMessage()
        );
    }

    private Connection oracleConnection(String characterSet, String nationalCharacterSet) throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("parameter")).thenReturn("NLS_CHARACTERSET", "NLS_NCHAR_CHARACTERSET");
        when(resultSet.getString("value")).thenReturn(characterSet, nationalCharacterSet);
        return connection;
    }
}
