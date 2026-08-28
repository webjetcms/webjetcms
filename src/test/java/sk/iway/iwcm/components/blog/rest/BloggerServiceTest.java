package sk.iway.iwcm.components.blog.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.blog.jpa.BloggerBean;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;

@Execution(ExecutionMode.SAME_THREAD)
class BloggerServiceTest {

    private static final int BLOG_GROUP_ID = 73;
    private static final long BLOGGER_ID = 321L;
    private static final long NON_BLOGGER_ID = 654L;
    private static final long SUPERADMIN_ID = 1L;
    private static final String NEW_PASSWORD = "Changed-password-123";

    @ParameterizedTest(name = "{1}")
    @MethodSource("rejectedBloggers")
    void shouldRejectInvalidTargetBeforeLoadingOrUpdatingUser(BloggerBean submitted, String description) {
        UserDetailsRepository repository = mock(UserDetailsRepository.class);

        try (MockedStatic<DocDB> docDB = mockStatic(DocDB.class);
                MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
                MockedConstruction<SimpleQuery> queries = mockBloggerIds(List.of(BLOGGER_ID));
                MockedStatic<UserDetailsService> passwordService = mockStatic(UserDetailsService.class)) {
            mockScope(docDB, cloudTools);

            assertFalse(BloggerService.editBlogger(submitted, repository, mock(HttpServletRequest.class)));
            verifyNoInteractions(repository);
            passwordService.verifyNoInteractions();
        }
    }

    @Test
    void shouldUpdateBloggerInCurrentScope() {
        BloggerBean submitted = blogger(BLOGGER_ID);
        submitted.setFirstName("Updated first name");
        submitted.setLastName("Updated last name");
        submitted.setEmail("updated@example.test");
        submitted.setLogin("submitted-login-must-not-replace-stored-login");
        submitted.setPassword(NEW_PASSWORD);

        UserDetailsEntity stored = new UserDetailsEntity();
        stored.setId(BLOGGER_ID);
        stored.setFirstName("Original first name");
        stored.setLastName("Original last name");
        stored.setEmail("original@example.test");
        stored.setLogin("stored-login");

        UserDetailsRepository repository = mock(UserDetailsRepository.class);
        when(repository.getById(BLOGGER_ID)).thenReturn(stored);
        when(repository.save(stored)).thenReturn(stored);

        try (MockedStatic<DocDB> docDB = mockStatic(DocDB.class);
                MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
                MockedConstruction<SimpleQuery> queries = mockBloggerIds(List.of(BLOGGER_ID));
                MockedStatic<UserDetailsService> passwordService = mockStatic(UserDetailsService.class)) {
            mockScope(docDB, cloudTools);
            passwordService.when(() -> UserDetailsService.savePassword(NEW_PASSWORD, (int) BLOGGER_ID))
                .thenReturn(true);

            assertTrue(BloggerService.editBlogger(submitted, repository, mock(HttpServletRequest.class)));

            assertEquals("Updated first name", stored.getFirstName());
            assertEquals("Updated last name", stored.getLastName());
            assertEquals("updated@example.test", stored.getEmail());
            assertEquals("stored-login", stored.getLogin());
            assertEquals(NEW_PASSWORD, stored.getPassword());
            verify(repository).getById(BLOGGER_ID);
            verify(repository).save(stored);
            passwordService.verify(() -> UserDetailsService.savePassword(NEW_PASSWORD, (int) BLOGGER_ID));
        }
    }

    private static Stream<Arguments> rejectedBloggers() {
        return Stream.of(
            Arguments.of(null, "null entity"),
            Arguments.of(blogger(null), "null id"),
            Arguments.of(blogger(NON_BLOGGER_ID), "non-blogger"),
            Arguments.of(blogger(SUPERADMIN_ID), "superadmin")
        );
    }

    private static MockedConstruction<SimpleQuery> mockBloggerIds(List<Long> bloggerIds) {
        List<Integer> scopedIds = bloggerIds.stream().map(Long::intValue).toList();
        return mockConstruction(SimpleQuery.class,
            (query, context) -> when(query.forListInteger(anyString())).thenReturn(scopedIds));
    }

    private static void mockScope(MockedStatic<DocDB> docDB, MockedStatic<CloudToolsForCore> cloudTools) {
        docDB.when(DocDB::getBlogGroupId).thenReturn(BLOG_GROUP_ID);
        cloudTools.when(() -> CloudToolsForCore.getDomainIdSqlWhere(true)).thenReturn(" AND domain_id=1");
    }

    private static BloggerBean blogger(Long id) {
        BloggerBean blogger = new BloggerBean();
        blogger.setId(id);
        blogger.setPassword(NEW_PASSWORD);
        return blogger;
    }
}
