package sk.iway.iwcm.components.users.userdetail;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.system.spring.events.WebjetEventPublisher;

class UserDetailsSelfControllerTest {

    private static final long SELF_ID = 123L;
    private static final long ATTACKER_SELECTED_ID = 999L;
    private static final String NEW_PASSWORD = "attacker-selected-password";

    @Test
    void shouldRejectDifferentBodyIdBeforeChangingPassword() {
        UserDetailsSelfRepository repository = mock(UserDetailsSelfRepository.class);
        UserDetailsService service = mock(UserDetailsService.class);

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<UserDetailsService> passwordService = mockStatic(UserDetailsService.class);
                MockedStatic<WebjetEventPublisher> events = mockStatic(WebjetEventPublisher.class)) {
            initServlet.when(InitServlet::isTypeCloud).thenReturn(false);
            constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(false);

            UserDetailsSelfController controller = createController(repository, service);
            UserDetailsSelfEntity submitted = new UserDetailsSelfEntity();
            submitted.setId(ATTACKER_SELECTED_ID);
            submitted.setPassword(NEW_PASSWORD);

            assertThrows(ConstraintViolationException.class, () -> controller.edit(SELF_ID, submitted));

            passwordService.verifyNoInteractions();
            verify(repository, never()).save(any());
        }
    }

    @Test
    void shouldUseSavedIdAsPasswordTargetForPayloadWithoutId() {
        UserDetailsSelfRepository repository = mock(UserDetailsSelfRepository.class);
        UserDetailsService service = mock(UserDetailsService.class);
        UserDetailsSelfEntity stored = new UserDetailsSelfEntity();
        stored.setId(SELF_ID);
        when(repository.existsById(SELF_ID)).thenReturn(true);
        when(repository.findById(SELF_ID)).thenReturn(Optional.of(stored));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<Constants> constants = mockStatic(Constants.class);
                MockedStatic<UserDetailsService> passwordService = mockStatic(UserDetailsService.class);
                MockedStatic<WebjetEventPublisher> events = mockStatic(WebjetEventPublisher.class)) {
            initServlet.when(InitServlet::isTypeCloud).thenReturn(false);
            constants.when(() -> Constants.getBoolean("enableStaticFilesExternalDir")).thenReturn(false);
            passwordService.when(() -> UserDetailsService.savePassword(NEW_PASSWORD, (int)SELF_ID)).thenReturn(true);

            UserDetailsSelfController controller = createController(repository, service);
            UserDetailsSelfEntity submitted = new UserDetailsSelfEntity();
            submitted.setPassword(NEW_PASSWORD);

            controller.edit(SELF_ID, submitted);

            passwordService.verify(() -> UserDetailsService.savePassword(NEW_PASSWORD, (int)SELF_ID));
            passwordService.verify(() -> UserDetailsService.savePassword(NEW_PASSWORD, (int)ATTACKER_SELECTED_ID), never());
        }
    }

    private UserDetailsSelfController createController(UserDetailsSelfRepository repository, UserDetailsService service) {
        Identity currentUser = mock(Identity.class);
        when(currentUser.getUserId()).thenReturn((int)SELF_ID);

        Validator validator = mock(Validator.class);
        doReturn(Collections.emptySet()).when(validator).validate(any());

        UserDetailsSelfController controller = new UserDetailsSelfController(repository, service) {
            @Override
            public Identity getUser() {
                return currentUser;
            }

            @Override
            public void afterSave(UserDetailsSelfEntity entity, UserDetailsSelfEntity saved) {
                // Password target is asserted directly; unrelated session refresh is out of scope here.
            }

            @Override
            public void throwConstraintViolation(String errorKey) {
                throw new ConstraintViolationException(errorKey, Collections.emptySet());
            }
        };
        controller.setValidator(validator);
        return controller;
    }
}
