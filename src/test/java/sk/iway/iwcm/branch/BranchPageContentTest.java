package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.i18n.Prop;

/** Read-only page comparison authorization, snapshot checks and raw version selection. */
class BranchPageContentTest {
    private final BranchMergeService merge = mock(BranchMergeService.class);
    private final BranchAccess access = mock(BranchAccess.class);
    private final BranchPageContent renderer = mock(BranchPageContent.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final Identity user = new Identity();
    private final BranchMergeService.Item item = mock(BranchMergeService.Item.class);
    private final BranchMergeService.Report report = mock(BranchMergeService.Report.class);
    private final BranchMergeRestController controller = new BranchMergeRestController(null, merge, access, renderer);

    @BeforeEach void prepare() throws Exception {
        RequestBean bean = new RequestBean();bean.setRequest(request);SetCharacterEncodingFilter.setCurrentRequestBean(bean);
        user.setUserId(7);
        when(access.requireUser(request)).thenReturn(user);
        when(access.requireDomain(request, user)).thenReturn("example.sk");
        when(merge.detail(42, "example.sk", user)).thenReturn(report);
        when(report.items()).thenReturn(List.of(item));
        when(item.type()).thenReturn("DOCUMENT");when(item.objectKey()).thenReturn("12");
        when(item.liveHash()).thenReturn("live");when(item.workHash()).thenReturn("work");
        when(item.live()).thenReturn(new BranchMergeService.Version("ACTIVE", java.util.Map.of(), "live"));
        when(item.work()).thenReturn(new BranchMergeService.Version("ACTIVE", java.util.Map.of(), "work"));
        when(renderer.render(eq(42L), eq("example.sk"), eq(7), eq(12), any(), same(request), same(response)))
            .thenAnswer(call -> "<html>" + call.getArgument(4) + "</html>");
        when(renderer.prepareWork(anyString(), eq(42L), eq("example.sk"), eq(7), same(request))).thenAnswer(call -> call.getArgument(0));
    }

    @AfterEach void clean() { SetCharacterEncodingFilter.setCurrentRequestBean(null); }

    @Test void workingContentIgnoresSavedLiveChoiceAndRestoresContext() throws Exception {
        when(item.choice()).thenReturn("LIVE");
        var result = controller.pageContent(42, 12, BranchDecision.Source.WORK, "live", "work", false, request, response);
        assertEquals("<html>WORK</html>", result.getBody());
        assertEquals("private, no-store", result.getHeaders().getFirst("Cache-Control"));
        assertEquals("sandbox allow-same-origin", result.getHeaders().getFirst("Content-Security-Policy"));
        assertFalse(BranchContexts.current().isBranch());
        verify(renderer).render(42, "example.sk", 7, 12, BranchDecision.Source.WORK, request, response);
        verify(renderer).prepareWork("<html>WORK</html>",42,"example.sk",7,request);
        verifyNoMoreInteractions(renderer);
    }

    @Test void highlightsWorkAgainstLiveUsingTheHistoryDiff() throws Exception {
        try (var diff = mockStatic(DocTools.class)) {
            diff.when(() -> DocTools.getHtmlDiff("<html>WORK</html>", "<html>LIVE</html>",false)).thenReturn("diff");
            assertEquals("diff", controller.pageContent(42, 12, BranchDecision.Source.WORK, "live", "work", true, request, response).getBody());
        }
    }

    @Test void staleSnapshotsCannotRenderNewContentUnderAnOldComparison() {
        try (var props = mockStatic(Prop.class)) {
            Prop prop = mock(Prop.class);props.when(() -> Prop.getInstance(request)).thenReturn(prop);
            when(prop.getText("branches.merge.pageStale.js")).thenReturn("Refresh comparison");
            var error = assertThrows(ResponseStatusException.class,
                () -> controller.pageContent(42, 12, BranchDecision.Source.WORK, "outdated", "work", false, request, response));
            assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
            verifyNoInteractions(renderer);
        }
    }

    @Test void foldersNeverReachThePageRendererEvenWithHtmlFields() {
        when(item.type()).thenReturn("GROUP");
        var error = assertThrows(ResponseStatusException.class,
            () -> controller.pageContent(42, 12, BranchDecision.Source.LIVE, "live", "work", false, request, response));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());verifyNoInteractions(renderer);
    }

    @Test void deletedPagesNeverRenderProductionAsAFallback() {
        when(item.work()).thenReturn(new BranchMergeService.Version("TRASH", java.util.Map.of(), "work"));
        var error = assertThrows(ResponseStatusException.class,
            () -> controller.pageContent(42, 12, BranchDecision.Source.WORK, "live", "work", false, request, response));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());verifyNoInteractions(renderer);
    }

    @Test void domainAndItemPermissionsAreCheckedBeforeRendering() {
        when(merge.detail(42, "example.sk", user)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        assertThrows(ResponseStatusException.class,
            () -> controller.pageContent(42, 12, BranchDecision.Source.WORK, "live", "work", false, request, response));
        verifyNoInteractions(renderer);
        when(access.requireDomain(request, user)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        assertThrows(ResponseStatusException.class,
            () -> controller.pageContent(42, 12, BranchDecision.Source.LIVE, "live", "work", false, request, response));
        verifyNoInteractions(renderer);
    }
}
