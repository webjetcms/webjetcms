package sk.iway.iwcm.editor.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.system.datatable.EditorException;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.spring.DatatableExceptionHandlerV2;

/** Tests cleanup notifications in the page editor's batch deletion error response. */
class WebpagesFileCleanupTest {
    private final EditorService editor = mock(EditorService.class);
    private final WebpagesRestController controller = mock(WebpagesRestController.class, CALLS_REAL_METHODS);
    private final NotifyBean summary = new NotifyBean("Page files", "Removed one file", NotifyType.INFO);
    private RequestBean previous;

    @BeforeEach
    void setUp() {
        previous = SetCharacterEncodingFilter.getCurrentRequestBean();
        SetCharacterEncodingFilter.setCurrentRequestBean(new RequestBean());
        ReflectionTestUtils.setField(controller, "editorService", editor);
        Set<Integer> selected = new HashSet<>();
        when(editor.selectForFileCleanup(anyInt())).thenAnswer(call -> selected.add(call.getArgument(0)));
        when(editor.finishFileCleanup(true)).thenReturn(summary);
        doReturn(true).when(controller).checkItemPerms(any(), anyLong());
        doReturn(true).when(controller).deleteItem(any(), anyLong());
    }

    @AfterEach
    void tearDown() {
        SetCharacterEncodingFilter.setCurrentRequestBean(previous);
    }

    /**
     * Verifies that a row failure stops later deletions and the response includes both the row error and the cleanup summary.
     *
     * @throws Exception if the test request cannot be processed
     */
    @Test
    void preservesTheRowErrorAndReportsCleanupWhenABatchStops() throws Exception {
        NotifyBean rowNotice = new NotifyBean("Page", "Delete failed", NotifyType.ERROR);
        EditorException error = new EditorException("Second page could not be deleted", List.of(rowNotice));
        doThrow(error).when(controller).checkItemPerms(any(), eq(2L));

        var result = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new DatatableExceptionHandlerV2()).build()
            .perform(post("/admin/rest/web-pages/editor").contentType(MediaType.APPLICATION_JSON).content("""
                {"action":"remove","data":{
                    "1":{"editorFields":{"deleteUnusedPageFiles":true}},
                    "2":{"editorFields":{"deleteUnusedPageFiles":true}},
                    "3":{"editorFields":{"deleteUnusedPageFiles":true}}
                }}
                """)).andExpect(status().isOk()).andReturn();
        assertSame(error, result.getResolvedException());
        var body = new JSONObject(result.getResponse().getContentAsString());
        assertEquals(error.getMessage(), body.getString("error"));
        var notifications = body.getJSONArray("notify");
        assertEquals(2, notifications.length());
        assertEquals(rowNotice.getText(), notifications.getJSONObject(0).getString("text"));
        assertEquals(summary.getText(), notifications.getJSONObject(1).getString("text"));
        verify(controller).deleteItem(any(), eq(1L));
        verify(controller, never()).deleteItem(any(), eq(3L));
        verify(editor).finishFileCleanup(true);
        verify(editor, never()).finishFileCleanup(false);
    }
}
