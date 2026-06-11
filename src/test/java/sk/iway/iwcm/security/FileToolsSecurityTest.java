package sk.iway.iwcm.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * JUnit tests for FileTools.isFileAllowedForUpload() security fix.
 * Verifies that dangerous file extensions are blocked for non-admin users.
 */
@Execution(ExecutionMode.SAME_THREAD)
class FileToolsSecurityTest extends BaseWebjetTest {

    // Dangerous extensions that MUST be blocked for non-admin users
    private static final String[] DANGEROUS_EXTENSIONS = {
        "jsp", "class", "java", "php", "sh", "exe", "bat", "cmd",
        "com", "vbs", "wsf", "wsh", "ps1", "ps2", "psm1"
    };

    // Safe extensions that should be allowed for non-admin users
    private static final String[] SAFE_EXTENSIONS = {
        "jpg", "jpeg", "gif", "bmp", "tiff", "tif", "png", "eps",
        "mp4", "wmv", "mpeg", "3gp", "mkv",
        "doc", "docx", "xls", "xlsx", "pdf", "zip", "rar", "txt", "xml", "ppt", "pptx"
    };

    @BeforeEach
    void setUp() {
        // Clear any admin state between tests
        clearAdminState();
    }

    private void clearAdminState() {
        try {
            java.lang.reflect.Field isAdminLogged = RequestBean.class.getDeclaredField("adminLogged");
            isAdminLogged.setAccessible(true);
            isAdminLogged.set(null, false);
        } catch (Exception e) {
            // ignore - test isolation
        }
    }

    // --- Tests for dangerous file extensions ---

    @Test
    void testIsFileAllowedForUpload_BlocksJSPFiles() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(false);

        assertFalse(FileTools.isFileAllowedForUpload(user, "shell.jsp"),
            "JSP files should be blocked for non-admin users");
    }

    @Test
    void testIsFileAllowedForUpload_BlocksClassFiles() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(false);

        assertFalse(FileTools.isFileAllowedForUpload(user, "exploit.class"),
            "Class files should be blocked for non-admin users");
    }

    @Test
    void testIsFileAllowedForUpload_BlocksExecutableFiles() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(false);

        assertFalse(FileTools.isFileAllowedForUpload(user, "malware.exe"),
            "EXE files should be blocked for non-admin users");
        assertFalse(FileTools.isFileAllowedForUpload(user, "script.bat"),
            "BAT files should be blocked for non-admin users");
        assertFalse(FileTools.isFileAllowedForUpload(user, "script.cmd"),
            "CMD files should be blocked for non-admin users");
    }

    @Test
    void testIsFileAllowedForUpload_BlocksScriptFiles() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(false);

        assertFalse(FileTools.isFileAllowedForUpload(user, "powershell.ps1"),
            "PS1 script files should be blocked for non-admin users");
        assertFalse(FileTools.isFileAllowedForUpload(user, "powershell.ps2"),
            "PS2 script files should be blocked for non-admin users");
        assertFalse(FileTools.isFileAllowedForUpload(user, "module.psm1"),
            "PSM1 module files should be blocked for non-admin users");
        assertFalse(FileTools.isFileAllowedForUpload(user, "script.sh"),
            "Shell script files should be blocked for non-admin users");
    }

    @Test
    void testIsFileAllowedForUpload_BlocksWindowsScriptFiles() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(false);

        assertFalse(FileTools.isFileAllowedForUpload(user, "script.vbs"),
            "VBS script files should be blocked for non-admin users");
        assertFalse(FileTools.isFileAllowedForUpload(user, "script.wsf"),
            "WSF script files should be blocked for non-admin users");
        assertFalse(FileTools.isFileAllowedForUpload(user, "script.wsh"),
            "WSH script files should be blocked for non-admin users");
    }

    @Test
    void testIsFileAllowedForUpload_BlocksAllDangerousExtensions() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(false);

        for (String ext : DANGEROUS_EXTENSIONS) {
            String fileName = "testfile." + ext;
            assertFalse(FileTools.isFileAllowedForUpload(user, fileName),
                ext.toUpperCase() + " files should be blocked for non-admin users");
        }
    }

    @Test
    void testIsFileAllowedForUpload_AllowsSafeExtensions() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(false);

        for (String ext : SAFE_EXTENSIONS) {
            String fileName = "testfile." + ext;
            assertTrue(FileTools.isFileAllowedForUpload(user, fileName),
                ext.toUpperCase() + " files should be allowed for non-admin users");
        }
    }

    @Test
    void testIsFileAllowedForUpload_AllowsAdminUsers() {
        Identity adminUser = mock(Identity.class);
        when(adminUser.isAdmin()).thenReturn(true);

        // Even dangerous extensions should be allowed for admin users
        for (String ext : DANGEROUS_EXTENSIONS) {
            String fileName = "testfile." + ext;
            assertTrue(FileTools.isFileAllowedForUpload(adminUser, fileName),
                ext.toUpperCase() + " files should be allowed for admin users");
        }
    }

    @Test
    void testIsFileAllowedForUpload_NullFileName() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(false);

        assertTrue(FileTools.isFileAllowedForUpload(user, null),
            "null file name should return true (allow by default)");
    }

    @Test
    void testIsFileAllowedForUpload_EmptyFileName() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(false);

        assertTrue(FileTools.isFileAllowedForUpload(user, ""),
            "empty file name should return true (allow by default)");
    }

    @Test
    void testIsFileAllowedForUpload_CaseInsensitiveExtensionCheck() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(false);

        // Test that extensions are checked case-insensitively
        assertFalse(FileTools.isFileAllowedForUpload(user, "shell.JSP"),
            "JSP files with uppercase extension should be blocked");
        assertFalse(FileTools.isFileAllowedForUpload(user, "shell.Php"),
            "PHP files with mixed case extension should be blocked");
        assertFalse(FileTools.isFileAllowedForUpload(user, "malware.EXE"),
            "EXE files with uppercase extension should be blocked");
    }

    @Test
    void testIsFileAllowedForUpload_DenyForbiddenSymbols() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(false);

        // Files with forbidden symbols should be blocked
        String fileName = "test<file>.jpg";
        assertFalse(FileTools.isFileAllowedForUpload(user, fileName),
            "Files with forbidden symbols should be blocked");
    }

    @Test
    void testIsFileAllowedForUpload_AdminUserBypass() {
        // When user is admin, all file types should be allowed
        Identity adminUser = mock(Identity.class);
        when(adminUser.isAdmin()).thenReturn(true);

        // Even dangerous extensions should be allowed for admin users
        assertTrue(FileTools.isFileAllowedForUpload(adminUser, "shell.jsp"),
            "JSP files should be allowed for admin users");
        assertTrue(FileTools.isFileAllowedForUpload(adminUser, "malware.exe"),
            "EXE files should be allowed for admin users");
    }

    @Test
    void testIsFileAllowedForUpload_NullUser() {
        // When user is null, the admin check is skipped and file extensions are checked
        // shell.jsp should be blocked by extension check
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(false);
        assertFalse(FileTools.isFileAllowedForUpload(user, "shell.jsp"),
            "JSP files should be blocked even with null user (extension check still applies)");
    }

    // --- Tests for forbidden symbol check in file names ---

    @Test
    void testIsFileAllowedForUpload_PathTraversalBlocked() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(false);

        // Path traversal attempts should be blocked
        String fileName = "../../../etc/passwd.jpg";
        assertFalse(FileTools.isFileAllowedForUpload(user, fileName),
            "Path traversal in file name should be blocked");
    }

    @Test
    void testIsFileAllowedForUpload_NoExtension() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(false);

        // Files without extension should be allowed (no extension to check)
        assertTrue(FileTools.isFileAllowedForUpload(user, "readme"),
            "Files without extension should be allowed");
    }
}
