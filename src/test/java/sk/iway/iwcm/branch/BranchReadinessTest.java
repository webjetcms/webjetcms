package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/** Database eligibility and filesystem checks for activating publishing packages. */
class BranchReadinessTest {
    @TempDir Path temporary;
    private Path webRoot;
    private MockedStatic<Constants> constants;
    private MockedStatic<Tools> tools;

    @BeforeEach void setup() throws IOException {
        webRoot=Files.createDirectory(temporary.resolve("web")).toRealPath();
        Files.createDirectory(webRoot.resolve("WEB-INF"));
        constants=mockStatic(Constants.class);
        tools=mockStatic(Tools.class);
        tools.when(()->Tools.getRealPath("/")).thenReturn(webRoot.toString());
        tools.when(()->Tools.getRealPath("/WEB-INF/branches")).thenReturn(webRoot.resolve("WEB-INF/branches").toString());
    }

    @AfterEach void cleanup() {
        tools.close();constants.close();
    }

    /** Blank legacy settings and the default path create storage automatically; deletion stays scoped to one branch. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings={" ","/WEB-INF/branches"})
    void createsDefaultStorageAndUsesItForWorkingFiles(String configured) throws Exception {
        constants.when(()->Constants.getString("branchStorageRoot")).thenReturn(configured);
        Path expected=webRoot.resolve("WEB-INF/branches");
        assertFalse(Files.exists(expected));
        assertEquals(expected,BranchReadiness.privateRoot());
        Path first=BranchFiles.workPath(7,9),second=BranchFiles.workPath(8,10);
        assertEquals(expected.resolve("7/9/content"),first);
        Files.createDirectories(first.getParent());Files.writeString(first,"autotest WORK bytes");
        Files.createDirectories(second.getParent());Files.writeString(second,"autotest other version");
        Path thumbnail=expected.resolve("thumbs/7/proof.png");
        Files.createDirectories(thumbnail.getParent());Files.writeString(thumbnail,"autotest cached thumbnail");
        BranchFiles.deleteBranch(7);
        assertFalse(Files.exists(first));assertFalse(Files.exists(thumbnail));
        assertEquals("autotest other version",Files.readString(second));
    }

    /** An existing custom location remains authoritative and is never migrated to the default. */
    @Test void preservesExplicitExternalStorage() throws Exception {
        Path external=Files.createDirectory(temporary.resolve("private"));
        constants.when(()->Constants.getString("branchStorageRoot")).thenReturn(external.toString());
        assertEquals(external.toRealPath(),BranchReadiness.privateRoot());
        assertEquals(external.resolve("7/9/content"),BranchFiles.workPath(7,9));
        assertFalse(Files.exists(webRoot.resolve("WEB-INF/branches")));
    }

    /** An explicit override must not put unpublished bytes in a public directory or the application's ancestor. */
    @ParameterizedTest
    @ValueSource(strings={"root","public","ancestor"})
    void rejectsPublicOrOverlappingStorage(String location) throws Exception {
        Path directory=switch(location) {
            case "root" -> webRoot;
            case "public" -> Files.createDirectory(webRoot.resolve("files"));
            default -> temporary.toRealPath();
        };
        constants.when(()->Constants.getString("branchStorageRoot")).thenReturn(directory.toString());
        assertThrows(IOException.class,BranchReadiness::privateRoot);
    }

    /** A missing explicit directory is reported rather than silently replaced with default storage. */
    @Test void rejectsMissingCustomStorage() {
        Path missing=temporary.resolve("missing");
        constants.when(()->Constants.getString("branchStorageRoot")).thenReturn(missing.toString());
        assertThrows(IOException.class,BranchReadiness::privateRoot);
        assertFalse(Files.exists(missing));
    }

    /** A symlink cannot move the protected default into a directory served as public files. */
    @Test void rejectsDefaultStorageSymlinkToPublicFiles() throws Exception {
        Path publicFiles=Files.createDirectory(webRoot.resolve("files"));
        Files.createSymbolicLink(webRoot.resolve("WEB-INF/branches"),publicFiles);
        assertThrows(IOException.class,BranchReadiness::privateRoot);
        try(var files=Files.list(publicFiles)) {assertEquals(0,files.count());}
    }

    /** A remapped WEB-INF parent is rejected before creating a storage directory through it. */
    @Test void rejectsPublicWebInfSymlinkBeforeCreatingStorage() throws Exception {
        Path publicFiles=Files.createDirectory(webRoot.resolve("files"));
        Files.delete(webRoot.resolve("WEB-INF"));
        Files.createSymbolicLink(webRoot.resolve("WEB-INF"),publicFiles);
        assertThrows(IOException.class,BranchReadiness::privateRoot);
        assertFalse(Files.exists(publicFiles.resolve("branches")));
    }

    /** Activation requires the feature and domain-separation flags without another operator confirmation. */
    @Test void enablesBranchesWithTheFeatureAndDomainSeparationFlags() {
        constants.when(()->Constants.getBoolean("branchesEnabled")).thenReturn(true);
        constants.when(()->Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(true);
        assertDoesNotThrow(BranchReadiness::requireEnabled);
        constants.when(()->Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(false);
        assertThrows(IllegalStateException.class,BranchReadiness::requireEnabled);
        constants.when(()->Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(true);
        constants.when(()->Constants.getBoolean("branchesEnabled")).thenReturn(false);
        assertThrows(IllegalStateException.class,BranchReadiness::requireEnabled);
    }

    /** Supported database versions still have to satisfy the writable-connection prerequisite. */
    @ParameterizedTest
    @CsvSource({"MariaDB,10,11,true", "MariaDB,11,4,true", "MariaDB,12,2,true",
                "MariaDB,10,10,false", "MariaDB,11,8,false", "MariaDB,12,3,false", "MySQL,11,4,false",
                "PostgreSQL,15,9,false", "PostgreSQL,16,0,true", "PostgreSQL,17,2,true"})
    void acceptsOnlySupportedDatabaseVersions(String product, int major, int minor, boolean supported) throws SQLException {
        constants.when(() -> Constants.getBoolean("branchesEnabled")).thenReturn(true);
        constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(true);
        Connection connection = mock(Connection.class);
        DatabaseMetaData database = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(database);
        when(connection.isReadOnly()).thenReturn(true);
        when(database.getDatabaseProductName()).thenReturn(product);
        when(database.getDatabaseMajorVersion()).thenReturn(major);
        when(database.getDatabaseMinorVersion()).thenReturn(minor);

        SQLException failure = assertThrows(SQLException.class, () -> BranchReadiness.verify(connection));
        assertEquals(supported, failure.getMessage().contains("writable connection"), failure.getMessage());
    }
}
