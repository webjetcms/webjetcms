package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import net.sourceforge.stripes.action.ActionBeanContext;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.components.news.NewsActionBean;
import sk.iway.iwcm.components.news.templates.jpa.NewsTemplatesEntity;
import sk.iway.iwcm.components.news.templates.NewsTemplatesService;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.i18n.Prop;

/** Existing News templates keep their read helpers without exposing arbitrary LIVE context classes. */
class BranchNewsContextTest {
    /** Runs the real Velocity template path with shared file icons, discussion state and WORK news. */
    @Test void rendersSharedMetadataHelpersAlongsideWorkingNews() {
        try (var contexts = mockStatic(BranchContexts.class); var templates = mockStatic(NewsTemplatesService.class);
             var icons = mockStatic(sk.iway.iwcm.FileTools.class); var forums = mockStatic(sk.iway.iwcm.forum.ForumDB.class);
             var languages = mockStatic(PageLng.class); var properties = mockStatic(Prop.class)) {
            contexts.when(BranchContexts::current).thenReturn(new BranchContext(1, "example.sk",1,BranchContext.Mode.ADMIN));
            languages.when(() -> PageLng.getUserLng(any())).thenReturn("sk");
            properties.when(() -> Prop.getInstance("sk")).thenReturn(mock(Prop.class));
            icons.when(() -> sk.iway.iwcm.FileTools.getFileIcon("xls")).thenReturn("/components/_common/mime/xls.gif");
            forums.when(() -> sk.iway.iwcm.forum.ForumDB.isActive(10)).thenReturn(true);
            NewsTemplatesEntity template = new NewsTemplatesEntity();
            template.setContextClasses("sk.iway.iwcm.FileTools\nsk.iway.iwcm.forum.ForumDB");
            template.setTemplateCode("$FileTools.getFileIcon('xls')|$ForumDB.isActive(10)|#foreach($item in $news)$item.title#end");
            templates.when(() -> NewsTemplatesService.getTemplateByName("preview")).thenReturn(template);
            NewsActionBean action = new NewsActionBean();
            ActionBeanContext context = new ActionBeanContext();
            context.setRequest(new MockHttpServletRequest());context.setResponse(new MockHttpServletResponse());
            action.setContext(context);action.setTemplate("preview");
            DocDetails work = new DocDetails();work.setTitle("WORK title");
            ReflectionTestUtils.setField(action, "newsList", List.of(work));
            ReflectionTestUtils.invokeMethod(action, "fillTemplate");
            assertEquals("/components/_common/mime/xls.gif|true|WORK title", action.getHtmlOut());

            template.setContextClasses("sk.iway.iwcm.DBPool");
            assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(action, "fillTemplate"));
        }
    }

    /** The adapters expose only the shared metadata reads, never the original mutation APIs. */
    @Test void excludesLiveMutationMethodsFromSharedHelpers() {
        assertThrows(NoSuchMethodException.class, () -> BranchNewsContext.resolve("sk.iway.iwcm.FileTools")
            .getMethod("saveFileContent", String.class, String.class));
        assertThrows(NoSuchMethodException.class, () -> BranchNewsContext.resolve("sk.iway.iwcm.forum.ForumDB")
            .getMethod("setForumCountForDocIds", int.class, int.class));
    }
}
