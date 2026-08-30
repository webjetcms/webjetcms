package cn.bluejoe.elfinder.controller.executors;

import java.io.ByteArrayInputStream;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.util.FsServiceUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ZipdlCommandExecutorSecurityTest {

    @Test
    void shouldRememberServerCreatedTemporaryZipInSession() throws Exception {
        FsService fsService = mock(FsService.class);
        FsItemEx zipFile = mock(FsItemEx.class);
        when(zipFile.getHash()).thenReturn("temporaryHash");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedConstruction<ArchiveCommandExecutor> archives = mockConstruction(ArchiveCommandExecutor.class,
            (archive, context) -> when(archive.executeZip(eq(fsService), eq(request), eq(null), any())).thenReturn(zipFile))) {
            new ZipdlCommandExecutor().execute(fsService, request, response, null);
        }

        assertEquals(Boolean.TRUE, request.getSession().getAttribute(
            ZipdlCommandExecutor.ZIPDL_SESSION_ATTRIBUTE_PREFIX + "temporaryHash"));
        assertTrue(response.getContentAsString().contains(ZipdlCommandExecutor.ZIPDL_HASH_PREFIX + "temporaryHash"));
    }

    @Test
    void shouldRejectZipHashThatWasNotCreatedInSession() throws Exception {
        FsService fsService = mock(FsService.class);
        MockHttpServletRequest request = downloadRequest("archiveHash");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ZipdlCommandExecutor().execute(fsService, request, response, null);

        assertEquals(403, response.getStatus());
        verifyNoInteractions(fsService);
    }

    @Test
    void shouldNotDeleteRememberedReadOnlyZip() throws Exception {
        FsService fsService = mock(FsService.class);
        FsItemEx zipFile = mock(FsItemEx.class);
        when(zipFile.isWritable(zipFile)).thenReturn(false);
        MockHttpServletRequest request = downloadRequest("temporaryHash");
        request.getSession().setAttribute(
            ZipdlCommandExecutor.ZIPDL_SESSION_ATTRIBUTE_PREFIX + "temporaryHash", Boolean.TRUE);
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class)) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "temporaryHash")).thenReturn(zipFile);

            new ZipdlCommandExecutor().execute(fsService, request, response, null);
        }

        assertEquals(403, response.getStatus());
        verify(zipFile, never()).delete();
    }

    @Test
    void shouldNotDeleteRememberedLockedZip() throws Exception {
        FsService fsService = mock(FsService.class);
        FsItemEx zipFile = mock(FsItemEx.class);
        when(zipFile.isWritable(zipFile)).thenReturn(true);
        when(zipFile.isLocked(zipFile)).thenReturn(true);
        MockHttpServletRequest request = downloadRequest("temporaryHash");
        request.getSession().setAttribute(
            ZipdlCommandExecutor.ZIPDL_SESSION_ATTRIBUTE_PREFIX + "temporaryHash", Boolean.TRUE);
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class)) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "temporaryHash")).thenReturn(zipFile);

            new ZipdlCommandExecutor().execute(fsService, request, response, null);
        }

        assertEquals(403, response.getStatus());
        verify(zipFile, never()).delete();
    }

    @Test
    void shouldDeleteRememberedWritableTemporaryZipAfterDownload() throws Exception {
        FsService fsService = mock(FsService.class);
        FsItemEx zipFile = mock(FsItemEx.class);
        when(zipFile.isWritable(zipFile)).thenReturn(true);
        when(zipFile.isLocked(zipFile)).thenReturn(false);
        when(zipFile.openInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        MockHttpServletRequest request = downloadRequest("temporaryHash");
        String sessionAttribute = ZipdlCommandExecutor.ZIPDL_SESSION_ATTRIBUTE_PREFIX + "temporaryHash";
        request.getSession().setAttribute(sessionAttribute, Boolean.TRUE);
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class)) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "temporaryHash")).thenReturn(zipFile);

            new ZipdlCommandExecutor().execute(fsService, request, response, null);
        }

        verify(zipFile).delete();
        assertNull(request.getSession().getAttribute(sessionAttribute));
    }

    private MockHttpServletRequest downloadRequest(String hash) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("download", "1");
        request.addParameter("targets[]", ZipdlCommandExecutor.ZIPDL_HASH_PREFIX + hash);
        return request;
    }
}
