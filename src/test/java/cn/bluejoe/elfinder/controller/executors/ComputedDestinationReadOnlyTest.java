package cn.bluejoe.elfinder.controller.executors;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsItem;
import cn.bluejoe.elfinder.service.FsSecurityChecker;
import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.service.FsVolume;
import cn.bluejoe.elfinder.util.FsServiceUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComputedDestinationReadOnlyTest extends BaseWebjetTest {

    @Test
    void shouldRejectMkdirWhenComputedDestinationIsReadOnly() throws Exception {
        DestinationContext context = new DestinationContext();
        MockHttpServletRequest request = request("parentHash", "archiv/new-folder");
        JSONObject json = new JSONObject();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<sk.iway.iwcm.system.elfinder.FsService> currentUser = mockStatic(sk.iway.iwcm.system.elfinder.FsService.class)) {
            finder.when(() -> FsServiceUtils.findItem(context.fsService, "parentHash")).thenReturn(context.parent);
            currentUser.when(sk.iway.iwcm.system.elfinder.FsService::getCurrentUser).thenReturn(context.user);

            new MkdirCommandExecutor().execute(context.fsService, request, null, json);
        }

        verify(context.volume).fromPath("/files/archiv/new-folder");
        verify(context.volume, never()).createFolder(any(), any());
        assertTrue(json.has("error"));
    }

    @Test
    void shouldRejectMkfileWhenComputedDestinationIsReadOnly() throws Exception {
        DestinationContext context = new DestinationContext();
        MockHttpServletRequest request = request("parentHash", "archiv/new-file.txt");
        JSONObject json = new JSONObject();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<sk.iway.iwcm.system.elfinder.FsService> currentUser = mockStatic(sk.iway.iwcm.system.elfinder.FsService.class)) {
            finder.when(() -> FsServiceUtils.findItem(context.fsService, "parentHash")).thenReturn(context.parent);
            currentUser.when(sk.iway.iwcm.system.elfinder.FsService::getCurrentUser).thenReturn(context.user);

            new MkfileCommandExecutor().execute(context.fsService, request, null, json);
        }

        verify(context.volume).fromPath("/files/archiv/new-file.txt");
        verify(context.volume, never()).createFile(any());
        assertTrue(json.has("error"));
    }

    @Test
    void shouldRejectRenameWhenComputedDestinationIsReadOnly() throws Exception {
        DestinationContext context = new DestinationContext();
        FsItemEx source = context.source("source.txt", false);
        MockHttpServletRequest request = request("sourceHash", "archiv/renamed.txt");
        JSONObject json = new JSONObject();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<sk.iway.iwcm.system.elfinder.FsService> currentUser = mockStatic(sk.iway.iwcm.system.elfinder.FsService.class)) {
            finder.when(() -> FsServiceUtils.findItem(context.fsService, "sourceHash")).thenReturn(source);
            currentUser.when(sk.iway.iwcm.system.elfinder.FsService::getCurrentUser).thenReturn(context.user);

            new RenameCommandExecutor().execute(context.fsService, request, null, json);
        }

        verify(context.volume).fromPath("/files/archiv/renamed.txt");
        verify(context.volume, never()).rename(any(), any());
        assertTrue(json.has("error"));
    }

    @Test
    void shouldRejectPasteWhenComputedDestinationIsReadOnly() throws Exception {
        DestinationContext context = new DestinationContext();
        FsItemEx source = context.source("archiv", true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("dst", "parentHash");
        request.addParameter("targets[]", "sourceHash");
        request.addParameter("cut", "0");
        JSONObject json = new JSONObject();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<sk.iway.iwcm.system.elfinder.FsService> currentUser = mockStatic(sk.iway.iwcm.system.elfinder.FsService.class)) {
            finder.when(() -> FsServiceUtils.findItem(context.fsService, "parentHash")).thenReturn(context.parent);
            finder.when(() -> FsServiceUtils.findItem(context.fsService, "sourceHash")).thenReturn(source);
            currentUser.when(sk.iway.iwcm.system.elfinder.FsService::getCurrentUser).thenReturn(context.user);

            new PasteCommandExecutor().execute(context.fsService, request, null, json);
        }

        verify(context.volume).fromPath("/files/archiv");
        verify(context.volume, never()).createFolder(any(), any());
        verify(context.volume, never()).createFile(any());
        assertTrue(json.has("error"));
    }

    @Test
    void shouldRejectArchiveWhenComputedDestinationIsReadOnly() throws Exception {
        DestinationContext context = new DestinationContext();
        FsItemEx source = context.source("source", true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("targets[]", "sourceHash");
        request.addParameter("name", "archiv/new");
        JSONObject json = new JSONObject();

        FsItemEx result;
        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<sk.iway.iwcm.system.elfinder.FsService> currentUser = mockStatic(sk.iway.iwcm.system.elfinder.FsService.class)) {
            finder.when(() -> FsServiceUtils.findItem(context.fsService, "sourceHash")).thenReturn(source);
            currentUser.when(sk.iway.iwcm.system.elfinder.FsService::getCurrentUser).thenReturn(context.user);

            result = new ArchiveCommandExecutor().executeZip(context.fsService, request, null, json);
        }

        verify(context.volume).fromPath("/files/archiv/new.zip");
        assertNull(result);
        assertTrue(json.has("error"));
    }

    @Test
    void shouldRejectArchiveNameWithParentTraversal() throws Exception {
        DestinationContext context = new DestinationContext();
        FsItemEx source = context.source("source", true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("targets[]", "sourceHash");
        request.addParameter("name", "../archiv/new");
        JSONObject json = new JSONObject();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class)) {
            finder.when(() -> FsServiceUtils.findItem(context.fsService, "sourceHash")).thenReturn(source);

            assertNull(new ArchiveCommandExecutor().executeZip(context.fsService, request, null, json));
        }

        verify(context.volume, never()).fromPath(any());
        assertTrue(json.has("error"));
    }

    private MockHttpServletRequest request(String target, String name) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("target", target);
        request.addParameter("name", name);
        return request;
    }

    private static class DestinationContext {
        private final FsService fsService = mock(FsService.class);
        private final FsSecurityChecker security = mock(FsSecurityChecker.class);
        private final FsVolume volume = mock(FsVolume.class);
        private final FsItem parentItem = mock(FsItem.class);
        private final FsItem destinationItem = mock(FsItem.class);
        private final Identity user = mock(Identity.class);
        private final FsItemEx parent;

        private DestinationContext() throws Exception {
            when(parentItem.getVolume()).thenReturn(volume);
            when(destinationItem.getVolume()).thenReturn(volume);
            when(fsService.getSecurityChecker()).thenReturn(security);
            when(volume.getPath(parentItem)).thenReturn("/files");
            when(volume.fromPath(any())).thenReturn(destinationItem);
            when(security.isWritable(fsService, parentItem)).thenReturn(true);
            when(security.isWritable(fsService, destinationItem)).thenReturn(false);
            when(user.getWritableFolders()).thenReturn("*");
            parent = new FsItemEx(parentItem, fsService);
        }

        private FsItemEx source(String name, boolean folder) throws Exception {
            FsItem sourceItem = mock(FsItem.class);
            when(sourceItem.getVolume()).thenReturn(volume);
            when(volume.getName(sourceItem)).thenReturn(name);
            when(volume.getPath(sourceItem)).thenReturn("/files/" + name);
            when(volume.getParent(sourceItem)).thenReturn(parentItem);
            when(volume.isFolder(sourceItem)).thenReturn(folder);
            when(security.isWritable(fsService, sourceItem)).thenReturn(true);
            return new FsItemEx(sourceItem, fsService);
        }
    }
}
