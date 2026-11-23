package sk.iway.iwcm.users;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UsersDBFolderWritableTest {

    @Test
    void recursivePermissionRequiresFolderSegmentBoundary() {
        String writableFolders = "/images/gallery/*";

        assertTrue(UsersDB.isFolderWritable(writableFolders, "/images/gallery/photo/"));
        assertFalse(UsersDB.isFolderWritable(writableFolders, "/images/gallery-backup/"));
    }

    /** The page ID scopes storage but does not change the target folder's permissions. */
    @ParameterizedTest
    @ValueSource(strings = {"42", "1234"})
    void plannedFoldersUsePublicFolderPermissions(String docId) {
        String root = "/files/protected/planned-changes/" + docId;
        assertTrue(UsersDB.isFolderWritable("/images/products/", root + "/images/products/"));
        assertTrue(UsersDB.isFolderWritable("/images/products/", root + "/images/products"));
        assertFalse(UsersDB.isFolderWritable("/images/other/", root + "/images/products/"));
        assertTrue(UsersDB.isFolderWritable("/images/products/*", root + "/images/products/nested/"));
        assertFalse(UsersDB.isFolderWritable("/images/products/*", root + "/images/products-backup/"));
        assertTrue(UsersDB.isFolderWritable("/files/docs/", root + "/files/docs/"));
        assertTrue(UsersDB.isFolderWritable("*", root + "/files/docs/"));
    }

    /** Existing path checks still reject traversal and incomplete staging paths. */
    @ParameterizedTest
    @ValueSource(strings = {
        "/files/protected/planned-changes/", "/files/protected/planned-changes/42",
        "/files/protected/planned-changes//images/products/",
        "/files/protected/planned-changes/../images/products/",
        "/files/protected/planned-changes/42/images/products/../other/"
    })
    void rejectsInvalidPlannedFolders(String folder) {
        assertFalse(UsersDB.isFolderWritable("*", folder));
    }
}
