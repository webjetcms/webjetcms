package sk.iway.iwcm.components.users.userdetail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.users.groups_approve.GroupsApproveRepository;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;
import sk.iway.iwcm.test.TestRequest;
import sk.iway.iwcm.users.PasswordSecurity;
import sk.iway.iwcm.users.UsersDB;

class UserDetailsControllerTest {

    private static final long USER_ID = 123L;
    private static final long TARGET_USER_ID = 1L;
    private static final long NEW_USER_ID = 456L;
    private static final int CURRENT_DOMAIN_ID = 1;
    private static final int FOREIGN_DOMAIN_ID = 2;
    private static final String ORIGINAL_PASSWORD = "original-password";
    private static final String ATTACKER_PASSWORD = "attacker-password";
    private static final String TEST_SALT = "0123456789abcdef";

    @ParameterizedTest
    @CsvSource({
        "false, true,  true,  false",
        "false, true,  false, true",
        "true,  false, true,  true",
        "true,  false, false, false",
        "true,  true,  true,  true",
        "true,  true,  false, true"
    })
    void shouldCheckPermissionAgainstStoredUserType(boolean canEditAdmins, boolean canEditPublicUsers,
            boolean storedAdmin, boolean expected) {
        assertEquals(expected, checkExistingUser(canEditAdmins, canEditPublicUsers, storedAdmin, false, 1, 1));
    }

    @ParameterizedTest
    @CsvSource({
        "false, true,  true,  false",
        "true,  false, true,  true",
        "false, true,  false, true",
        "true,  false, false, true"
    })
    void shouldCheckPermissionWhenCreatingUser(boolean canEditAdmins, boolean canEditPublicUsers,
            boolean requestedAdmin, boolean expected) {
        Identity currentUser = createIdentity(canEditAdmins, canEditPublicUsers);
        UserDetailsEntity requested = new UserDetailsEntity();
        requested.setAdmin(requestedAdmin);

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            initServlet.when(InitServlet::isTypeCloud).thenReturn(false);
            constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(false);

            UserDetailsController controller = createController(mock(UserDetailsRepository.class), currentUser);
            assertEquals(expected, controller.checkItemPerms(requested, -1L));
        }
    }

    @Test
    void shouldRejectUserFromAnotherDomain() {
        assertEquals(false, checkExistingUser(true, true, true, true, 1, 2));
    }

    @Test
    void shouldRejectMissingStoredUser() {
        UserDetailsRepository repository = mock(UserDetailsRepository.class);
        Identity currentUser = createIdentity(true, true);
        when(repository.findById(USER_ID)).thenReturn(Optional.empty());

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<UserDetailsService> userDetailsService = mockStatic(UserDetailsService.class)) {
            initServlet.when(InitServlet::isTypeCloud).thenReturn(false);
            constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(false);
            userDetailsService.when(UserDetailsService::isUsersSplitByDomain).thenReturn(false);

            UserDetailsController controller = createController(repository, currentUser);
            assertEquals(false, controller.checkItemPerms(new UserDetailsEntity(), USER_ID));
        }
    }

    @Test
    void addEndpointMustNotChangePasswordHashOfBodyIdTarget() {
        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<WebjetEventPublisher> events = mockStatic(WebjetEventPublisher.class)) {
            configureEndpointStatics(initServlet, constants);
            EndpointFixture fixture = createEndpointFixture(users);
            String originalHash = hash(ORIGINAL_PASSWORD);
            fixture.service.passwordHashes.put(TARGET_USER_ID, originalHash);
            when(fixture.repository.save(any())).thenAnswer(invocation -> {
                UserDetailsEntity entity = invocation.getArgument(0);
                assertNull(entity.getId(), "Generated body ID must be cleared before repository.save");
                entity.setId(NEW_USER_ID);
                return entity;
            });

            fixture.controller.add(submittedUser(TARGET_USER_ID));

            assertEquals(originalHash, fixture.service.passwordHashes.get(TARGET_USER_ID));
            assertEquals(hash(ATTACKER_PASSWORD), fixture.service.passwordHashes.get(NEW_USER_ID));
        }
    }

    @Test
    void editEndpointMustRejectBodyIdMismatchWithoutChangingTargetPasswordHash() {
        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<WebjetEventPublisher> events = mockStatic(WebjetEventPublisher.class)) {
            configureEndpointStatics(initServlet, constants);
            EndpointFixture fixture = createEndpointFixture(users);
            String originalHash = hash(ORIGINAL_PASSWORD);
            fixture.service.passwordHashes.put(TARGET_USER_ID, originalHash);
            mockEditablePathUser(fixture);

            assertThrows(ConstraintViolationException.class,
                    () -> fixture.controller.edit(USER_ID, submittedUser(TARGET_USER_ID)));

            assertEquals(originalHash, fixture.service.passwordHashes.get(TARGET_USER_ID));
            verify(fixture.repository, never()).save(any());
        }
    }

    @Test
    void editorEndpointMustRejectBodyIdMismatchWithoutChangingTargetPasswordHash() {
        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<WebjetEventPublisher> events = mockStatic(WebjetEventPublisher.class)) {
            configureEndpointStatics(initServlet, constants);
            EndpointFixture fixture = createEndpointFixture(users);
            String originalHash = hash(ORIGINAL_PASSWORD);
            fixture.service.passwordHashes.put(TARGET_USER_ID, originalHash);
            mockEditablePathUser(fixture);

            DatatableRequest<Long, UserDetailsEntity> request = new DatatableRequest<>();
            request.setAction("edit");
            request.setData(Map.of(USER_ID, submittedUser(TARGET_USER_ID)));

            assertThrows(ConstraintViolationException.class,
                    () -> fixture.controller.handleEditor(fixture.request, request));

            assertEquals(originalHash, fixture.service.passwordHashes.get(TARGET_USER_ID));
            verify(fixture.repository, never()).save(any());
        }
    }

    @Test
    void addEndpointMustRejectForeignDomainWithoutChangingPasswordHash() {
        withSplitDomainFixture(true, false, fixture -> {
            String originalHash = hash(ORIGINAL_PASSWORD);
            fixture.service.passwordHashes.put(TARGET_USER_ID, originalHash);

            UserDetailsEntity submitted = submittedUser(TARGET_USER_ID);
            submitted.setAdmin(true);
            submitted.setDomainId(FOREIGN_DOMAIN_ID);

            assertDomainError(() -> fixture.controller.add(submitted));

            assertEquals(originalHash, fixture.service.passwordHashes.get(TARGET_USER_ID));
            assertEquals(1, fixture.service.passwordHashes.size());
            verify(fixture.repository, never()).save(any());
        });
    }

    @Test
    void addEndpointMustAssignCurrentDomainWhenDomainIsMissing() {
        withSplitDomainFixture(false, true, fixture -> {
            when(fixture.repository.save(any())).thenAnswer(invocation -> {
                UserDetailsEntity entity = invocation.getArgument(0);
                assertEquals(CURRENT_DOMAIN_ID, entity.getDomainId());
                entity.setId(NEW_USER_ID);
                return entity;
            });

            UserDetailsEntity submitted = submittedUser(TARGET_USER_ID);
            submitted.setDomainId(null);
            fixture.controller.add(submitted);

            assertEquals(hash(ATTACKER_PASSWORD), fixture.service.passwordHashes.get(NEW_USER_ID));
        });
    }

    @Test
    void editEndpointMustRejectStoredForeignDomainWithoutChangingPasswordHash() {
        withSplitDomainFixture(false, true, fixture -> {
            String originalHash = hash(ORIGINAL_PASSWORD);
            fixture.service.passwordHashes.put(USER_ID, originalHash);
            mockPathUser(fixture, false, FOREIGN_DOMAIN_ID);

            UserDetailsEntity submitted = submittedUser(USER_ID);
            submitted.setDomainId(CURRENT_DOMAIN_ID);

            assertThrows(ConstraintViolationException.class,
                    () -> fixture.controller.edit(USER_ID, submitted));

            assertEquals(originalHash, fixture.service.passwordHashes.get(USER_ID));
            assertEquals(1, fixture.service.passwordHashes.size());
            verify(fixture.repository, never()).save(any());
        });
    }

    @Test
    void editEndpointMustRejectForeignBodyDomainWithoutChangingPasswordHash() {
        withSplitDomainFixture(false, true, fixture -> {
            String originalHash = hash(ORIGINAL_PASSWORD);
            fixture.service.passwordHashes.put(USER_ID, originalHash);
            mockPathUser(fixture, false, CURRENT_DOMAIN_ID);

            UserDetailsEntity submitted = submittedUser(USER_ID);
            submitted.setDomainId(FOREIGN_DOMAIN_ID);

            assertDomainError(() -> fixture.controller.edit(USER_ID, submitted));

            assertEquals(originalHash, fixture.service.passwordHashes.get(USER_ID));
            assertEquals(1, fixture.service.passwordHashes.size());
            verify(fixture.repository, never()).save(any());
        });
    }

    @Test
    void editorEndpointMustRejectForeignDomainWithoutChangingPasswordHash() {
        withSplitDomainFixture(true, false, fixture -> {
            String originalHash = hash(ORIGINAL_PASSWORD);
            fixture.service.passwordHashes.put(TARGET_USER_ID, originalHash);

            UserDetailsEntity submitted = submittedUser(TARGET_USER_ID);
            submitted.setAdmin(true);
            submitted.setDomainId(FOREIGN_DOMAIN_ID);

            DatatableRequest<Long, UserDetailsEntity> request = new DatatableRequest<>();
            request.setAction("create");
            request.setData(Map.of(-1L, submitted));

            assertDomainError(() -> fixture.controller.handleEditor(fixture.request, request));

            assertEquals(originalHash, fixture.service.passwordHashes.get(TARGET_USER_ID));
            assertEquals(1, fixture.service.passwordHashes.size());
            verify(fixture.repository, never()).save(any());
        });
    }

    @Test
    void editorEditEndpointMustRejectForeignBodyDomainWithoutChangingPasswordHash() {
        withSplitDomainFixture(false, true, fixture -> {
            String originalHash = hash(ORIGINAL_PASSWORD);
            fixture.service.passwordHashes.put(USER_ID, originalHash);
            mockPathUser(fixture, false, CURRENT_DOMAIN_ID);

            UserDetailsEntity submitted = submittedUser(USER_ID);
            submitted.setDomainId(FOREIGN_DOMAIN_ID);

            DatatableRequest<Long, UserDetailsEntity> request = new DatatableRequest<>();
            request.setAction("edit");
            request.setData(Map.of(USER_ID, submitted));

            assertDomainError(() -> fixture.controller.handleEditor(fixture.request, request));

            assertEquals(originalHash, fixture.service.passwordHashes.get(USER_ID));
            assertEquals(1, fixture.service.passwordHashes.size());
            verify(fixture.repository, never()).save(any());
        });
    }

    private boolean checkExistingUser(boolean canEditAdmins, boolean canEditPublicUsers, boolean storedAdmin,
            boolean usersSplitByDomain, int storedDomainId, int currentDomainId) {
        UserDetailsRepository repository = mock(UserDetailsRepository.class);
        Identity currentUser = createIdentity(canEditAdmins, canEditPublicUsers);

        UserDetailsEntity stored = new UserDetailsEntity();
        stored.setAdmin(storedAdmin);
        stored.setDomainId(storedDomainId);
        when(repository.findById(USER_ID)).thenReturn(Optional.of(stored));

        UserDetailsEntity submitted = new UserDetailsEntity();
        submitted.setAdmin(!storedAdmin);

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<UserDetailsService> userDetailsService = mockStatic(UserDetailsService.class);
                MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            initServlet.when(InitServlet::isTypeCloud).thenReturn(false);
            constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(false);
            userDetailsService.when(UserDetailsService::isUsersSplitByDomain).thenReturn(usersSplitByDomain);
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(currentDomainId);

            UserDetailsController controller = createController(repository, currentUser);
            return controller.checkItemPerms(submitted, USER_ID);
        }
    }

    private Identity createIdentity(boolean canEditAdmins, boolean canEditPublicUsers) {
        Identity currentUser = mock(Identity.class);
        when(currentUser.isEnabledItem("users.edit_admins")).thenReturn(canEditAdmins);
        when(currentUser.isEnabledItem("users.edit_public_users")).thenReturn(canEditPublicUsers);
        return currentUser;
    }

    private void configureEndpointStatics(MockedStatic<InitServlet> initServlet, MockedStatic<Constants> constants) {
        initServlet.when(InitServlet::isTypeCloud).thenReturn(false);
        constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(false);
        constants.when(() -> Constants.getBoolean("usersSplitByDomain")).thenReturn(false);
        constants.when(() -> Constants.getString("passwordHashAlgorithm")).thenReturn("sha-512");
    }

    private EndpointFixture createEndpointFixture(MockedStatic<UsersDB> users) {
        return createEndpointFixture(users, false, true);
    }

    private EndpointFixture createEndpointFixture(MockedStatic<UsersDB> users,
            boolean canEditAdmins, boolean canEditPublicUsers) {
        UserDetailsRepository repository = mock(UserDetailsRepository.class);
        PasswordHashTrackingService service = new PasswordHashTrackingService();
        GroupsApproveRepository groupsApproveRepository = mock(GroupsApproveRepository.class);
        Identity currentUser = createIdentity(canEditAdmins, canEditPublicUsers);
        TestRequest request = new TestRequest();

        Validator validator = mock(Validator.class);
        doReturn(Collections.emptySet()).when(validator).validate(any());

        UserDetailsController controller = new UserDetailsController(repository, service, groupsApproveRepository) {
            @Override
            public Identity getUser() {
                return currentUser;
            }

            @Override
            public UserDetailsEntity processFromEntity(UserDetailsEntity entity, ProcessItemAction action) {
                return entity;
            }

            @Override
            public UserDetailsEntity processToEntity(UserDetailsEntity entity, ProcessItemAction action) {
                return entity;
            }

            @Override
            public void throwConstraintViolation(String errorKey) {
                throw new ConstraintViolationException(errorKey, Collections.emptySet());
            }

            @Override
            public void throwError(String errorKey) {
                throw new IllegalArgumentException(errorKey);
            }
        };
        controller.setRequest(request);
        controller.setValidator(validator);
        users.when(() -> UsersDB.getCurrentUser(request)).thenReturn(currentUser);
        return new EndpointFixture(controller, repository, service, request);
    }

    private void mockEditablePathUser(EndpointFixture fixture) {
        mockPathUser(fixture, false, CURRENT_DOMAIN_ID);
    }

    private void mockPathUser(EndpointFixture fixture, boolean admin, int domainId) {
        UserDetailsEntity editableUser = new UserDetailsEntity();
        editableUser.setId(USER_ID);
        editableUser.setAdmin(admin);
        editableUser.setDomainId(domainId);
        when(fixture.repository.existsById(USER_ID)).thenReturn(true);
        when(fixture.repository.findById(USER_ID)).thenReturn(Optional.of(editableUser));
        when(fixture.repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void withSplitDomainFixture(boolean canEditAdmins, boolean canEditPublicUsers,
            Consumer<EndpointFixture> test) {
        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
                MockedStatic<UsersDB> users = mockStatic(UsersDB.class);
                MockedStatic<WebjetEventPublisher> events = mockStatic(WebjetEventPublisher.class)) {
            configureEndpointStatics(initServlet, constants);
            constants.when(() -> Constants.getBoolean("usersSplitByDomain")).thenReturn(true);
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(CURRENT_DOMAIN_ID);

            EndpointFixture fixture = createEndpointFixture(users, canEditAdmins, canEditPublicUsers);
            test.accept(fixture);
        }
    }

    private void assertDomainError(Executable action) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertEquals("datatables.error.domainId", exception.getMessage());
    }

    private UserDetailsEntity submittedUser(long id) {
        UserDetailsEntity submitted = new UserDetailsEntity();
        submitted.setId(id);
        submitted.setAdmin(false);
        submitted.setPassword(ATTACKER_PASSWORD);
        return submitted;
    }

    private String hash(String password) {
        return PasswordSecurity.calculateHash(password, TEST_SALT);
    }

    private UserDetailsController createController(UserDetailsRepository repository, Identity currentUser) {
        UserDetailsService service = mock(UserDetailsService.class);
        GroupsApproveRepository groupsApproveRepository = mock(GroupsApproveRepository.class);

        return new UserDetailsController(repository, service, groupsApproveRepository) {
            @Override
            public Identity getUser() {
                return currentUser;
            }
        };
    }

    private static class EndpointFixture {

        private final UserDetailsController controller;
        private final UserDetailsRepository repository;
        private final PasswordHashTrackingService service;
        private final TestRequest request;

        EndpointFixture(UserDetailsController controller, UserDetailsRepository repository,
                PasswordHashTrackingService service, TestRequest request) {
            this.controller = controller;
            this.repository = repository;
            this.service = service;
            this.request = request;
        }
    }

    private static class PasswordHashTrackingService extends UserDetailsService {

        private final Map<Long, String> passwordHashes = new HashMap<>();

        @Override
        public boolean afterSave(UserDetailsEntity entity, UserDetailsEntity saved) {
            passwordHashes.put(saved.getId(), PasswordSecurity.calculateHash(entity.getPassword(), TEST_SALT));
            return true;
        }

        @Override
        public void setBeforeSaveUserGroups(UserDetailsEntity entity) {
            // No session state is needed for password-target tests.
        }

        @Override
        public void sendUserGroupsEmails(UserDetailsEntity user, UserDetailsEntity userBeforeSave,
                Identity admin, HttpServletRequest request) {
            // No email side effects are needed for password-target tests.
        }

        @Override
        public boolean updateSelf(UserDetailsBasic form, Identity user, HttpServletRequest request) {
            return false;
        }
    }
}
