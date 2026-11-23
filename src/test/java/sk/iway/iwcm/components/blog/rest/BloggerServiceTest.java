package sk.iway.iwcm.components.blog.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.blog.jpa.BloggerBean;
import sk.iway.iwcm.components.users.AuthorizeUserService;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@Execution(ExecutionMode.SAME_THREAD)
class BloggerServiceTest {

    private static final int BLOG_GROUP_ID = 73;
    private static final long BLOGGER_ID = 321L;
    private static final long NON_BLOGGER_ID = 654L;
    private static final long SUPERADMIN_ID = 1L;
    private static final long NEW_BLOGGER_ID = 987L;
    private static final String NEW_LOGIN = "new-blogger";
    private static final String NEW_PASSWORD = "Changed-password-123";

    @ParameterizedTest
    @MethodSource("invalidNewBloggers")
    void shouldRejectInvalidNewBloggerBeforeAnyLookupOrWrite(BloggerBean submitted, String description) {
        UserDetailsRepository repository = mock(UserDetailsRepository.class);
        EditorFacade editorFacade = mock(EditorFacade.class);

        try (MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<DocDB> docDB = mockStatic(DocDB.class);
                MockedStatic<UserDetailsService> passwordService = mockStatic(UserDetailsService.class)) {
            assertFalse(BloggerService.saveBlogger(submitted, repository, editorFacade, mock(HttpServletRequest.class)));

            verifyNoInteractions(repository, editorFacade);
            users.verifyNoInteractions();
            docDB.verifyNoInteractions();
            passwordService.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "ADMIN", "admín" })
    void shouldRejectNormalizedLoginCollisionBeforeSavingBlogger(String login) {
        BloggerBean submitted = bloggerForCreate(login);
        UserDetailsRepository repository = mock(UserDetailsRepository.class);
        EditorFacade editorFacade = mock(EditorFacade.class);

        try (MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<UserDetailsService> passwordService = mockStatic(UserDetailsService.class)) {
            users.when(() -> UsersDB.getUser(login)).thenReturn(mock(UserDetails.class));

            assertFalse(BloggerService.saveBlogger(submitted, repository, editorFacade, mock(HttpServletRequest.class)));

            verifyNoInteractions(repository, editorFacade);
            passwordService.verifyNoInteractions();
            users.verify(() -> UsersDB.getUser(login));
            users.verify(() -> UsersDB.getUserIdByLogin(login), never());
            users.verifyNoMoreInteractions();
        }
    }

    @Test
    void shouldRejectNullEntityReturnedBySaveAndFlush() {
        BloggerBean submitted = bloggerForCreate(NEW_LOGIN);
        UserDetailsRepository repository = mock(UserDetailsRepository.class);
        when(repository.saveAndFlush(any(UserDetailsEntity.class))).thenReturn(null);

        try (MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<DocDB> docDB = mockStatic(DocDB.class);
                MockedStatic<UserDetailsService> passwordService = mockStatic(UserDetailsService.class)) {
            users.when(() -> UsersDB.getUser(NEW_LOGIN)).thenReturn(null);
            docDB.when(DocDB::getBlogGroupId).thenReturn(BLOG_GROUP_ID);

            assertFalse(BloggerService.saveBlogger(
                submitted,
                repository,
                mock(EditorFacade.class),
                mock(HttpServletRequest.class)
            ));

            assertNull(submitted.getId());
            verify(repository).saveAndFlush(any(UserDetailsEntity.class));
            verifyNoMoreInteractions(repository);
            passwordService.verifyNoInteractions();
            users.verify(() -> UsersDB.getUserIdByLogin(NEW_LOGIN), never());
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = { -1L, 0L, 2147483648L })
    void shouldRejectInvalidIdReturnedBySaveAndFlush(Long savedId) {
        BloggerBean submitted = bloggerForCreate(NEW_LOGIN);
        UserDetailsEntity savedUser = new UserDetailsEntity();
        savedUser.setId(savedId);

        UserDetailsRepository repository = mock(UserDetailsRepository.class);
        when(repository.saveAndFlush(any(UserDetailsEntity.class))).thenReturn(savedUser);

        try (MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<DocDB> docDB = mockStatic(DocDB.class);
                MockedStatic<UserDetailsService> passwordService = mockStatic(UserDetailsService.class)) {
            users.when(() -> UsersDB.getUser(NEW_LOGIN)).thenReturn(null);
            docDB.when(DocDB::getBlogGroupId).thenReturn(BLOG_GROUP_ID);

            assertFalse(BloggerService.saveBlogger(
                submitted,
                repository,
                mock(EditorFacade.class),
                mock(HttpServletRequest.class)
            ));

            assertNull(submitted.getId());
            verify(repository).saveAndFlush(any(UserDetailsEntity.class));
            verifyNoMoreInteractions(repository);
            passwordService.verifyNoInteractions();
            users.verify(() -> UsersDB.getUserIdByLogin(NEW_LOGIN), never());
        }
    }

    @Test
    void shouldUseOnlyIdReturnedBySaveAndFlush() {
        BloggerBean submitted = bloggerForCreate(NEW_LOGIN);
        submitted.setId(SUPERADMIN_ID);
        UserDetailsEntity savedUser = new UserDetailsEntity();
        savedUser.setId(NEW_BLOGGER_ID);

        UserDetailsRepository repository = mock(UserDetailsRepository.class);
        when(repository.saveAndFlush(any(UserDetailsEntity.class))).thenReturn(savedUser);

        Modules modulesInstance = mock(Modules.class);
        when(modulesInstance.getModules()).thenReturn(List.of());

        try (MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<DocDB> docDB = mockStatic(DocDB.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<Modules> modules = mockStatic(Modules.class);
                MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class,
                    (query, context) -> when(query.executeInTransaction(anyList(), anyList())).thenReturn(true));
                MockedStatic<UserDetailsService> passwordService = mockStatic(UserDetailsService.class)) {
            users.when(() -> UsersDB.getUser(NEW_LOGIN)).thenReturn(null);
            docDB.when(DocDB::getBlogGroupId).thenReturn(BLOG_GROUP_ID);
            constants.when(() -> Constants.getString("bloggerAppPermissions")).thenReturn("");
            modules.when(Modules::getInstance).thenReturn(modulesInstance);
            passwordService.when(() -> UserDetailsService.savePassword(NEW_PASSWORD, (int) NEW_BLOGGER_ID))
                .thenReturn(false);

            assertFalse(BloggerService.saveBlogger(
                submitted,
                repository,
                mock(EditorFacade.class),
                mock(HttpServletRequest.class)
            ));

            ArgumentCaptor<UserDetailsEntity> newUserCaptor = ArgumentCaptor.forClass(UserDetailsEntity.class);
            verify(repository).saveAndFlush(newUserCaptor.capture());
            verify(repository, never()).save(any(UserDetailsEntity.class));
            assertNull(newUserCaptor.getValue().getId());
            assertFalse(newUserCaptor.getValue().getAdmin());
            assertFalse(newUserCaptor.getValue().getAuthorized());
            assertEquals(Long.valueOf(NEW_BLOGGER_ID), submitted.getId());
            assertEquals(NEW_PASSWORD, savedUser.getPassword());
            passwordService.verify(() -> UserDetailsService.savePassword(NEW_PASSWORD, (int) NEW_BLOGGER_ID));
            passwordService.verifyNoMoreInteractions();
            users.verify(() -> UsersDB.getUserIdByLogin(NEW_LOGIN), never());
            verify(queries.constructed().get(0)).executeInTransaction(anyList(), anyList());
        }
    }

    @Test
    void shouldKeepNewBloggerInactiveWhenRightsTransactionFails() {
        BloggerBean submitted = bloggerForCreate(NEW_LOGIN);
        UserDetailsEntity savedUser = new UserDetailsEntity();
        savedUser.setId(NEW_BLOGGER_ID);

        UserDetailsRepository repository = mock(UserDetailsRepository.class);
        when(repository.saveAndFlush(any(UserDetailsEntity.class))).thenReturn(savedUser);
        EditorFacade editorFacade = mock(EditorFacade.class);

        Modules modulesInstance = mock(Modules.class);
        when(modulesInstance.getModules()).thenReturn(List.of());

        try (MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<DocDB> docDB = mockStatic(DocDB.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<Modules> modules = mockStatic(Modules.class);
                MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class,
                    (query, context) -> when(query.executeInTransaction(anyList(), anyList()))
                        .thenThrow(new IllegalStateException("rights insert failed")));
                MockedStatic<UserDetailsService> passwordService = mockStatic(UserDetailsService.class);
                MockedStatic<AuthorizeUserService> authorizeUserService = mockStatic(AuthorizeUserService.class)) {
            users.when(() -> UsersDB.getUser(NEW_LOGIN)).thenReturn(null);
            docDB.when(DocDB::getBlogGroupId).thenReturn(BLOG_GROUP_ID);
            constants.when(() -> Constants.getString("bloggerAppPermissions")).thenReturn("");
            modules.when(Modules::getInstance).thenReturn(modulesInstance);

            assertFalse(BloggerService.saveBlogger(
                submitted,
                repository,
                editorFacade,
                mock(HttpServletRequest.class)
            ));

            ArgumentCaptor<UserDetailsEntity> newUserCaptor = ArgumentCaptor.forClass(UserDetailsEntity.class);
            verify(repository).saveAndFlush(newUserCaptor.capture());
            verify(repository, never()).activateBlogger(any(Long.class));
            verifyNoMoreInteractions(repository);
            assertFalse(newUserCaptor.getValue().getAdmin());
            assertFalse(newUserCaptor.getValue().getAuthorized());
            assertNull(newUserCaptor.getValue().getPassword());
            verify(queries.constructed().get(0)).executeInTransaction(anyList(), anyList());
            passwordService.verifyNoInteractions();
            authorizeUserService.verifyNoInteractions();
            verifyNoInteractions(editorFacade);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2 })
    void shouldActivateBloggerOnlyWhenExactlyOneStagedAccountWasUpdated(int activatedUsers) {
        BloggerBean submitted = bloggerForCreate(NEW_LOGIN);
        GroupDetails editableGroup = new GroupDetails();
        editableGroup.setGroupId(55);
        submitted.setEditableGroup(editableGroup);

        UserDetailsEntity savedUser = new UserDetailsEntity();
        savedUser.setId(NEW_BLOGGER_ID);
        savedUser.setLogin(NEW_LOGIN);
        savedUser.setAdmin(false);
        savedUser.setAuthorized(false);

        AtomicBoolean inactiveUserSaved = new AtomicBoolean();
        AtomicBoolean rightsSaved = new AtomicBoolean();
        AtomicBoolean passwordSaved = new AtomicBoolean();
        AtomicBoolean structureProcessed = new AtomicBoolean();
        AtomicBoolean activated = new AtomicBoolean();

        UserDetailsRepository repository = mock(UserDetailsRepository.class);
        when(repository.saveAndFlush(any(UserDetailsEntity.class))).thenAnswer(invocation -> {
            UserDetailsEntity entity = invocation.getArgument(0);
            assertFalse(entity.getAdmin());
            assertFalse(entity.getAuthorized());
            inactiveUserSaved.set(true);
            return savedUser;
        });
        when(repository.activateBlogger(NEW_BLOGGER_ID)).thenAnswer(invocation -> {
            assertTrue(structureProcessed.get());
            if (activatedUsers == 1) activated.set(true);
            return activatedUsers;
        });

        EditorFacade editorFacade = mock(EditorFacade.class);
        Modules modulesInstance = mock(Modules.class);
        when(modulesInstance.getModules()).thenReturn(List.of());
        GroupsDB groupsInstance = mock(GroupsDB.class);
        GroupDetails existingRootGroup = new GroupDetails();
        when(groupsInstance.getGroup(NEW_LOGIN, editableGroup.getGroupId())).thenAnswer(invocation -> {
            assertTrue(passwordSaved.get());
            structureProcessed.set(true);
            return existingRootGroup;
        });

        HttpServletRequest request = mock(HttpServletRequest.class);
        Identity currentUser = mock(Identity.class);

        try (MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<DocDB> docDB = mockStatic(DocDB.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<Modules> modules = mockStatic(Modules.class);
                MockedStatic<GroupsDB> groups = mockStatic(GroupsDB.class);
                MockedConstruction<SimpleQuery> queries = mockConstruction(SimpleQuery.class,
                    (query, context) -> when(query.executeInTransaction(anyList(), anyList())).thenAnswer(invocation -> {
                        assertTrue(inactiveUserSaved.get());
                        List<String> commands = invocation.getArgument(0);
                        List<Object[]> parameters = invocation.getArgument(1);
                        assertEquals(List.of(
                            "DELETE FROM user_disabled_items WHERE user_id=?",
                            "INSERT INTO user_disabled_items (user_id, item_name) VALUES (?, ?)"
                        ), commands);
                        assertEquals(NEW_BLOGGER_ID, ((Number) parameters.get(0)[0]).longValue());
                        assertEquals(NEW_BLOGGER_ID, ((Number) parameters.get(1)[0]).longValue());
                        assertEquals("cmp_blog_admin", parameters.get(1)[1]);
                        rightsSaved.set(true);
                        return true;
                    }));
                MockedStatic<UserDetailsService> passwordService = mockStatic(UserDetailsService.class);
                MockedStatic<AuthorizeUserService> authorizeUserService = mockStatic(AuthorizeUserService.class)) {
            users.when(() -> UsersDB.getUser(NEW_LOGIN)).thenReturn(null);
            users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(currentUser);
            docDB.when(DocDB::getBlogGroupId).thenReturn(BLOG_GROUP_ID);
            constants.when(() -> Constants.getString("bloggerAppPermissions")).thenReturn("");
            modules.when(Modules::getInstance).thenReturn(modulesInstance);
            groups.when(GroupsDB::getInstance).thenReturn(groupsInstance);
            passwordService.when(() -> UserDetailsService.savePassword(NEW_PASSWORD, (int) NEW_BLOGGER_ID))
                .thenAnswer(invocation -> {
                    assertTrue(rightsSaved.get());
                    passwordSaved.set(true);
                    return true;
                });
            authorizeUserService.when(() -> AuthorizeUserService.sendInfoEmail(savedUser, NEW_PASSWORD, currentUser, request))
                .thenAnswer(invocation -> {
                    assertTrue(activated.get());
                    return null;
                });

            assertEquals(
                activatedUsers == 1,
                BloggerService.saveBlogger(submitted, repository, editorFacade, request)
            );

            verify(repository).saveAndFlush(any(UserDetailsEntity.class));
            verify(repository).activateBlogger(NEW_BLOGGER_ID);
            verifyNoMoreInteractions(repository);
            assertEquals(activatedUsers == 1, savedUser.getAdmin());
            assertEquals(activatedUsers == 1, savedUser.getAuthorized());
            verify(queries.constructed().get(0)).executeInTransaction(anyList(), anyList());
            passwordService.verify(() -> UserDetailsService.savePassword(NEW_PASSWORD, (int) NEW_BLOGGER_ID));
            if (activatedUsers == 1) {
                authorizeUserService.verify(() -> AuthorizeUserService.sendInfoEmail(savedUser, NEW_PASSWORD, currentUser, request));
            } else {
                authorizeUserService.verifyNoInteractions();
            }
            verifyNoInteractions(editorFacade);
        }
    }

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
        when(repository.getReferenceById(BLOGGER_ID)).thenReturn(stored);
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
            verify(repository).getReferenceById(BLOGGER_ID);
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

    private static Stream<Arguments> invalidNewBloggers() {
        return Stream.of(
            Arguments.of(null, "null entity"),
            Arguments.of(bloggerForCreate(null), "null login"),
            Arguments.of(bloggerForCreate(""), "empty login"),
            Arguments.of(bloggerForCreate("   "), "blank login")
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

    private static BloggerBean bloggerForCreate(String login) {
        BloggerBean blogger = blogger(null);
        blogger.setLogin(login);
        blogger.setFirstName("New");
        blogger.setLastName("Blogger");
        blogger.setEmail("new-blogger@example.test");
        return blogger;
    }
}
