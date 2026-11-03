package sk.iway.iwcm.setup;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.io.IwcmFile;

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
            "CREATE DATABASE [webjet_web] COLLATE Latin1_General_100_CI_AI_SC",
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
            "CREATE DATABASE [name]]with_bracket] COLLATE Latin1_General_100_CI_AI_SC",
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

    @Test
    void onlyPrefillsNonSecretDatabaseParameters() {
        assertEquals(
            "currentSchema=webjet_cms",
            SetupActionsService.getSafeDbParameters(
                "org.postgresql.Driver",
                "password=secret&currentSchema=webjet_cms&accessToken=hidden"
            )
        );
        assertEquals(
            "encoding=utf-8",
            SetupActionsService.getSafeDbParameters(
                "net.sourceforge.jtds.jdbc.Driver",
                "password=secret;encoding=utf-8"
            )
        );
        assertEquals(
            "",
            SetupActionsService.getSafeDbParameters(
                "org.mariadb.jdbc.Driver",
                "password=secret&sslKeyStorePassword=hidden"
            )
        );
    }

    @Test
    void acceptsOnlySafePostgresqlSchemaIdentifiers() {
        assertEquals("webjet_cms", SetupActionsService.getPostgresqlSchema(null));
        assertEquals(
            "customer_2026",
            SetupActionsService.getPostgresqlSchema("sslmode=require&currentSchema=customer_2026")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> SetupActionsService.getPostgresqlSchema("currentSchema=customer\";DROP SCHEMA public;--")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> SetupActionsService.getPostgresqlSchema("currentSchema=2customer")
        );
    }

    @Test
    void updatesOrInsertsConfigurationValuesWithSingleCommit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("conf_installName", "customer");
        request.addParameter("_csrf", "ignored");
        Connection connection = mock(Connection.class);
        PreparedStatement updateStatement = mock(PreparedStatement.class);
        PreparedStatement insertStatement = mock(PreparedStatement.class);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(updateStatement, insertStatement);
        when(updateStatement.executeUpdate()).thenReturn(0);

        SetupActionsService.saveConfigurationValues(request, connection);

        verify(connection).setAutoCommit(false);
        verify(updateStatement).setString(1, "customer");
        verify(updateStatement).setString(2, "installName");
        verify(insertStatement).setString(1, "installName");
        verify(insertStatement).setString(2, "customer");
        verify(insertStatement).executeUpdate();
        verify(connection).commit();
        verify(connection, never()).rollback();
        verify(connection).setAutoCommit(true);
    }

    @Test
    void updatesExistingConfigurationWithoutDeleteAndInsert() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("conf_installName", "customer");
        Connection connection = mock(Connection.class);
        PreparedStatement updateStatement = mock(PreparedStatement.class);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(updateStatement);
        when(updateStatement.executeUpdate()).thenReturn(1);

        SetupActionsService.saveConfigurationValues(request, connection);

        verify(connection, times(1)).prepareStatement(anyString());
        verify(connection).commit();
    }

    @Test
    void requestsRollbackWhenAConfigurationWriteFails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("conf_installName", "customer");
        Connection connection = mock(Connection.class);
        PreparedStatement updateStatement = mock(PreparedStatement.class);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(updateStatement);
        doThrow(new SQLException("write failed")).when(updateStatement).executeUpdate();

        SQLException exception = assertThrows(
            SQLException.class,
            () -> SetupActionsService.saveConfigurationValues(request, connection)
        );

        assertEquals("write failed", exception.getMessage());
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    void reportsPoolmanWriteFailure() {
        SetupFormBean form = new SetupFormBean();
        form.setDbDriver("org.postgresql.Driver");
        form.setDbDomain("localhost");
        form.setDbName("webjet_web");
        form.setDbParameters("currentSchema=webjet_cms");

        try (MockedStatic<FileTools> fileTools = mockStatic(FileTools.class)) {
            fileTools.when(() -> FileTools.saveFileContent(anyString(), anyString())).thenReturn(false);

            assertFalse(SetupActionsService.savePoolman(form));
        }
    }

    @Test
    void removesPoolmanGeneratedByFailedSetup() {
        IwcmFile poolmanFile = mock(IwcmFile.class);
        when(poolmanFile.exists()).thenReturn(true);
        when(poolmanFile.delete()).thenReturn(true);

        try (MockedStatic<IwcmFile> files = mockStatic(IwcmFile.class)) {
            files.when(() -> IwcmFile.fromVirtualPath("/WEB-INF/classes/poolman.xml"))
                .thenReturn(poolmanFile);

            assertEquals("setup failed", SetupActionsService.rollbackGeneratedPoolman(true, "setup failed"));
            verify(poolmanFile).delete();
        }
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
