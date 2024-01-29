package sk.iway.iwcm.components.response_header.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ResponseHeaderServiceTest {
    @Test
    void testIsPathCorrect() {
        // Null path should return false
        assertFalse(ResponseHeaderService.isPathCorrect(null, "/path/subpath/"));

        // Exact match
        assertTrue(ResponseHeaderService.isPathCorrect("^/path/subpath/$", "/path/subpath/"));
        assertFalse(ResponseHeaderService.isPathCorrect("^/path/subpath/$", "/path/another/"));

        // Starts with match
        assertTrue(ResponseHeaderService.isPathCorrect("/path/subpath/", "/path/subpath/child"));
        assertFalse(ResponseHeaderService.isPathCorrect("/path/subpath/", "/path/another/child"));

        // Ends with match
        assertTrue(ResponseHeaderService.isPathCorrect("/path/subpath/*", "/path/subpath/something"));
        assertTrue(ResponseHeaderService.isPathCorrect("/path/subpath/*.pdf", "/path/subpath/file.pdf"));
        assertTrue(ResponseHeaderService.isPathCorrect("/path/subpath/*.pdf", "/path/subpath/another/file.pdf"));
        assertFalse(ResponseHeaderService.isPathCorrect("/path/subpath/*.pdf", "/path/subpath/file.xls"));
        assertFalse(ResponseHeaderService.isPathCorrect("/path/subpath/*.pdf", "/path/subpath/another/file.xls"));

        // Multiple ends with match
        assertTrue(ResponseHeaderService.isPathCorrect("/path/subpath/*.pdf,*.jpg", "/path/subpath/image.jpg"));
        assertTrue(ResponseHeaderService.isPathCorrect("/path/subpath/*.pdf,*.jpg", "/path/subpath/file.pdf"));
        assertTrue(ResponseHeaderService.isPathCorrect("/path/subpath/*.pdf,*.jpg", "/path/subpath/another/file.pdf"));
        assertFalse(ResponseHeaderService.isPathCorrect("/path/subpath/*.pdf,*.jpg", "/path/subpath/imageXpdf"));
        assertFalse(ResponseHeaderService.isPathCorrect("/path/subpath/*.pdf,*.jpg", "/path/subpath/excel.xls"));
    }
}
