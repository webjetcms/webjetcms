package cn.bluejoe.elfinder.controller.executors;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.util.FsServiceUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.system.elfinder.IwcmFsVolume;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LsCommandExecutorUnicodeNormalizationTest {

    @Test
    void shouldMatchExistingFileInFilesPathWhenIntersectIsNfdAndStoredNameIsNfc() throws Exception {
        String nfcName = "á.txt";
        String nfdName = "a\u0301.txt";

        FsItemEx fsi = mock(FsItemEx.class);

        sk.iway.iwcm.system.elfinder.FsService selectedTypeService = mock(sk.iway.iwcm.system.elfinder.FsService.class);
        when(selectedTypeService.getSelectedType()).thenReturn(sk.iway.iwcm.system.elfinder.FsService.TYPE_FILES);

        when(fsi.getPath()).thenReturn("/files/uploads");
        when(fsi.getService()).thenReturn(selectedTypeService);

        FsItemEx child = mock(FsItemEx.class);
        when(child.getHash()).thenReturn("hash-1");
        when(child.getName()).thenReturn(nfcName);

        when(fsi.listChildren()).thenReturn(List.of(child));

        LsCommandExecutor executor = new LsCommandExecutor();
        FsService fsService = mock(FsService.class);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("target", "targetHash");
        request.addParameter("intersect[]", nfdName);

        // Keep permission table empty so fbrowser_allow_diacritics is treated as enabled.
        Identity user = new Identity();
        request.getSession(true).setAttribute(Constants.USER_KEY, user);

        JSONObject json = new JSONObject();
        try (MockedStatic<FsServiceUtils> fsServiceUtils = org.mockito.Mockito.mockStatic(FsServiceUtils.class)) {
            fsServiceUtils.when(() -> FsServiceUtils.findItem(fsService, "targetHash")).thenReturn(fsi);
            executor.execute(fsService, request, null, json);
        }

        assertTrue(json.has("list"));
        Object[] list = (Object[]) json.get("list");
        assertEquals(1, list.length);
        assertEquals(nfdName, list[0]);
    }

    @Test
    void shouldMatchExistingFileOutsideFilesPathUsingNfcNormalization() throws Exception {
        String nfcName = "á.txt";
        String nfdName = "a\u0301.txt";

        FsItemEx fsi = mock(FsItemEx.class);
        when(fsi.getPath()).thenReturn("/templates/custom");

        FsItemEx child = mock(FsItemEx.class);
        when(child.getHash()).thenReturn("hash-2");
        when(child.getName()).thenReturn(nfcName);

        when(fsi.listChildren()).thenReturn(List.of(child));

        LsCommandExecutor executor = new LsCommandExecutor();
        FsService fsService = mock(FsService.class);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("target", "targetHash");
        request.addParameter("intersect[]", nfdName);

        Identity user = new Identity();
        request.getSession(true).setAttribute(Constants.USER_KEY, user);

        JSONObject json = new JSONObject();
        try (MockedStatic<FsServiceUtils> fsServiceUtils = org.mockito.Mockito.mockStatic(FsServiceUtils.class)) {
            fsServiceUtils.when(() -> FsServiceUtils.findItem(fsService, "targetHash")).thenReturn(fsi);
            executor.execute(fsService, request, null, json);
        }

        assertTrue(json.has("list"));
        Object[] list = (Object[]) json.get("list");
        assertEquals(1, list.length);
        assertEquals(nfcName, list[0]);
    }

    @Test
    void normalizeUnicodeShouldConvertNfdToNfc() {
        String nfdName = "a\u0301.txt";
        assertEquals("á.txt", IwcmFsVolume.normalizeUnicode(nfdName));
    }
}
