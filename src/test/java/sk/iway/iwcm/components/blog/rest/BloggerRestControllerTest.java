package sk.iway.iwcm.components.blog.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.blog.jpa.BloggerBean;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.editor.facade.EditorFacade;

@Execution(ExecutionMode.SAME_THREAD)
class BloggerRestControllerTest {

    private static final int BLOG_GROUP_ID = 73;
    private static final long BLOGGER_ID = 321L;
    private static final long NON_BLOGGER_ID = 654L;

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = { -1L, 0L })
    void shouldAllowCreateSentinelOnlyWithNonPositiveEntityId(Long entityId) {
        assertTrue(checkItemPerms(blogger(entityId), -1L, List.of()));
    }

    @Test
    void shouldRejectEditSentinelWithPositiveEntityId() {
        assertFalse(checkItemPerms(blogger(NON_BLOGGER_ID), -1L, List.of()));
    }

    @Test
    void shouldAllowMatchingBloggerInCurrentScope() {
        assertTrue(checkItemPerms(blogger(BLOGGER_ID), BLOGGER_ID, List.of(BLOGGER_ID)));
    }

    @Test
    void shouldRejectMatchingUserOutsideBloggerScope() {
        assertFalse(checkItemPerms(blogger(NON_BLOGGER_ID), NON_BLOGGER_ID, List.of(BLOGGER_ID)));
    }

    @ParameterizedTest(name = "pathId={0}, entityId={1}")
    @MethodSource("mismatchedIds")
    void shouldRejectPathAndEntityIdMismatch(long pathId, long entityId) {
        assertFalse(checkItemPerms(blogger(entityId), pathId, List.of(BLOGGER_ID)));
    }

    @ParameterizedTest(name = "pathId={0}, entity={1}")
    @MethodSource("invalidExistingEntities")
    void shouldRejectInvalidExistingEntity(Long pathId, BloggerBean entity) {
        assertFalse(checkItemPerms(entity, pathId, List.of(BLOGGER_ID)));
    }

    private static Stream<Arguments> mismatchedIds() {
        return Stream.of(
            Arguments.of(BLOGGER_ID, NON_BLOGGER_ID),
            Arguments.of(NON_BLOGGER_ID, BLOGGER_ID)
        );
    }

    private static Stream<Arguments> invalidExistingEntities() {
        return Stream.of(
            Arguments.of(null, blogger(BLOGGER_ID)),
            Arguments.of(0L, blogger(0L)),
            Arguments.of(BLOGGER_ID, null),
            Arguments.of(BLOGGER_ID, blogger(null))
        );
    }

    private static boolean checkItemPerms(BloggerBean entity, Long pathId, List<Long> scopedBloggerIds) {
        List<Integer> scopedIds = scopedBloggerIds.stream().map(Long::intValue).toList();

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<DocDB> docDB = mockStatic(DocDB.class);
                MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
                MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class,
                    (query, context) -> when(query.forListInteger(anyString())).thenReturn(scopedIds))) {
            initServlet.when(InitServlet::isTypeCloud).thenReturn(false);
            constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(false);
            docDB.when(DocDB::getBlogGroupId).thenReturn(BLOG_GROUP_ID);
            cloudTools.when(() -> CloudToolsForCore.getDomainIdSqlWhere(true)).thenReturn(" AND domain_id=1");

            BloggerRestController controller = new BloggerRestController(
                mock(UserDetailsRepository.class),
                mock(EditorFacade.class)
            );
            return controller.checkItemPerms(entity, pathId);
        }
    }

    private static BloggerBean blogger(Long id) {
        BloggerBean blogger = new BloggerBean();
        blogger.setId(id);
        return blogger;
    }
}
