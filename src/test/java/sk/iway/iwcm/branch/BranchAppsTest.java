package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Unsupported parameter editors remain separate from ordinary component preview rendering. */
class BranchAppsTest {
    @Test void permitsSupportedEditorsAndRejectsOtherOrMissingClassesInBranchContexts() {
        try (var contexts=mockStatic(BranchContexts.class)) {
            for (var mode:List.of(BranchContext.Mode.ADMIN,BranchContext.Mode.PREVIEW)) {
                contexts.when(BranchContexts::current).thenReturn(new BranchContext(1,"example.sk",1,mode));
                assertDoesNotThrow(()->BranchApps.checkEditorSupport("sk.iway.iwcm.components.menu.MenuApp"));
                assertDoesNotThrow(()->BranchApps.checkEditorSupport("sk.iway.iwcm.components.news.NewsApp"));
                var error=assertThrows(ResponseStatusException.class,()->BranchApps.checkEditorSupport("sk.iway.iwcm.components.gallery.GalleryApp"));
                assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
                assertThrows(ResponseStatusException.class,()->BranchApps.checkEditorSupport(null));
                assertThrows(ResponseStatusException.class,()->BranchApps.checkEditorSupport(""));
            }
        }
    }

    @Test void leavesLiveEditorSelectionUnrestricted() {
        try (var contexts=mockStatic(BranchContexts.class)) {
            contexts.when(BranchContexts::current).thenReturn(BranchContext.live("example.sk",1));
            assertDoesNotThrow(()->BranchApps.checkEditorSupport("customer.CustomApp"));
            assertDoesNotThrow(()->BranchApps.checkEditorSupport(null));
        }
    }

}
