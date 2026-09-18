package sk.iway.iwcm.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @ValueSource(strings = {
        "Name$subname.class", "Name$1.class", "Name$Nested$Inner.class",
        "/WEB-INF/classes/example/Name$subname.class", "example\\Name$subname.class"
    })
    void shouldAllowDollarSignsInClassFileNames(String path) {
        assertFalse(FileBrowserTools.hasForbiddenSymbol(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Name$subname.txt", "Name$subname.class.zip", "Name$subname.class/",
        "folder$/Name.class", "folder$/Name$subname.class", "folder$\\Name.class",
        "../Name$subname.class", "Name$subname;.class", "Name$subname.java.class",
        "Name$subname@2x.class"
    })
    void shouldKeepOtherForbiddenNamesBlocked(String path) {
        assertTrue(FileBrowserTools.hasForbiddenSymbol(path));
    }
}
