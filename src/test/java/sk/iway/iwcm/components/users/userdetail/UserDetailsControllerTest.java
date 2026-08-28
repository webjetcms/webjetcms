package sk.iway.iwcm.components.users.userdetail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.users.groups_approve.GroupsApproveRepository;

class UserDetailsControllerTest {

    private static final long USER_ID = 123L;

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
}
