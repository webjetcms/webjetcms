package cn.bluejoe.elfinder.controller.executors;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsService;
import cn.bluejoe.elfinder.util.FsServiceUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MkdirCommandExecutorReadOnlyTest extends BaseWebjetTest {

    @Test
    void shouldRejectForgedMkdirRequestForReadOnlyFolder() throws Exception {
        FsService fsService = mock(FsService.class);
        FsItemEx folder = mock(FsItemEx.class);
        when(folder.isWritable(folder)).thenReturn(false);
        when(folder.getPath()).thenReturn("/files/archiv");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("target", "archiveHash");
        request.addParameter("name", "forged-folder");
        JSONObject json = new JSONObject();

        try (MockedStatic<FsServiceUtils> finder = mockStatic(FsServiceUtils.class);
             MockedStatic<sk.iway.iwcm.system.elfinder.FsService> currentUser = mockStatic(sk.iway.iwcm.system.elfinder.FsService.class)) {
            finder.when(() -> FsServiceUtils.findItem(fsService, "archiveHash")).thenReturn(folder);
            currentUser.when(sk.iway.iwcm.system.elfinder.FsService::getCurrentUser).thenReturn(new Identity());

            new MkdirCommandExecutor().execute(fsService, request, null, json);
        }

        verify(folder).isWritable(folder);
        assertTrue(json.has("error"));
        assertEquals(0, ((Object[]) json.get("added")).length);
    }
}
