package sk.iway.iwcm.users;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UsersDBFolderWritableTest {

    @Test
    void recursivePermissionRequiresFolderSegmentBoundary() {
        String writableFolders = "/images/gallery/*";

        assertTrue(UsersDB.isFolderWritable(writableFolders, "/images/gallery/photo/"));
        assertFalse(UsersDB.isFolderWritable(writableFolders, "/images/gallery-backup/"));
    }
}
