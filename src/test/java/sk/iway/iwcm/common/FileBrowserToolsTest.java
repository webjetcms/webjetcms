package sk.iway.iwcm.common;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class FileBrowserToolsTest extends BaseWebjetTest {

    @Test
    public void testHasForbiddenSymbol() {
        assertFalse(FileBrowserTools.hasForbiddenSymbol("filename"));
        assertFalse(FileBrowserTools.hasForbiddenSymbol("file name.txt"));
        assertTrue(FileBrowserTools.hasForbiddenSymbol("file'name"));
        assertFalse(FileBrowserTools.hasForbiddenSymbol(null));
        assertTrue(FileBrowserTools.hasForbiddenSymbol("file.java"));
        assertTrue(FileBrowserTools.hasForbiddenSymbol("~file.java"));

        //loaded from constants
        assertTrue(FileBrowserTools.hasForbiddenSymbol("file#name"));
        assertTrue(FileBrowserTools.hasForbiddenSymbol("file@name"));
        assertTrue(FileBrowserTools.hasForbiddenSymbol("file(name"));
    }
}
