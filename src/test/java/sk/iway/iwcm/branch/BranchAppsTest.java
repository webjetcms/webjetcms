package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Branch app support is shared by class-based editor requests and JSP component previews. */
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

    @Test void permitsBothSupportedPreviewsWithOptionalParametersAndWhitespace() {
        for (String path:List.of("/components/menu/menu_ul_li.jsp","/components/news/news-velocity.jsp")) {
            assertTrue(BranchApps.isSupportedPreview("!INCLUDE("+path+")!"));
            assertTrue(BranchApps.isSupportedPreview(" \n!INCLUDE("+path+", rootGroupId=42, title=\"A (B)\")! \n"));
        }
    }

    @Test void rejectsOtherPathsMultipleIncludesAndMalformedCodes() {
        String valid="!INCLUDE(/components/menu/menu_ul_li.jsp, rootGroupId=42)!";
        for (String code:List.of("", "!INCLUDE()!", "!INCLUDE(/components/gallery/gallery.jsp)!",
                "!INCLUDE(/components/menu/menu_ul_li.jsp/other)!", "!INCLUDE(/components/menu/menu_ul_li.jsp", valid+valid,
                "<p>"+valid+"</p>", "!INCLUDE(/components/menu/menu_ul_li.jsp, value=x)!suffix)!"))
            assertFalse(BranchApps.isSupportedPreview(code),code);
        assertFalse(BranchApps.isSupportedPreview(null));
    }
}
