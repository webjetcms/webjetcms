package sk.iway.iwcm.components.forms;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormItemsRepository;
import sk.iway.iwcm.components.multistep_form.jpa.FormStepsRepository;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.users.UserDetails;

class FormsServiceTest {

    @Test
    void isFormAccessibleReturnsFalseForRestrictedUserWhenSubmittedFormDoesNotExist() {
        int domainId = 42;
        String formName = "new-form";

        FormsRepository formsRepository = mock(FormsRepository.class);
        FormsService<FormsRepository, FormsEntity> formsService = new FormsService<>(
            formsRepository,
            mock(FormSettingsRepository.class),
            mock(FormStepsRepository.class),
            mock(FormItemsRepository.class)
        );

        UserDetails user = mock(UserDetails.class);
        when(user.getEditableGroups()).thenReturn("");
        when(user.getEditablePages()).thenReturn("123");

        GroupsDB groupsDB = mock(GroupsDB.class);
        when(groupsDB.expandGroupIdsToChilds(any(int[].class), eq(true))).thenReturn(new int[0]);
        DocDB docDB = mock(DocDB.class);
        when(formsRepository.findTopByFormNameAndDomainIdAndCreateDateNotNullOrderByCreateDateDesc(formName, domainId))
            .thenReturn(null);

        try (MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
                MockedStatic<GroupsDB> groups = mockStatic(GroupsDB.class);
                MockedStatic<DocDB> docs = mockStatic(DocDB.class)) {
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(domainId);
            groups.when(GroupsDB::getInstance).thenReturn(groupsDB);
            docs.when(DocDB::getInstance).thenReturn(docDB);

            assertFalse(formsService.isFormAccessible(formName, user));
        }

        verify(formsRepository).findTopByFormNameAndDomainIdAndCreateDateNotNullOrderByCreateDateDesc(formName, domainId);
        verifyNoInteractions(docDB);
    }
}
