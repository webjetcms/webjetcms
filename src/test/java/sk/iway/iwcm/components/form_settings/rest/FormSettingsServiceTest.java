package sk.iway.iwcm.components.form_settings.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.system.cluster.ClusterDB;

/** Verifies form settings behavior that differs between administrative and public cluster nodes. */
class FormSettingsServiceTest {

    /** Public nodes do not attempt writes to statistics columns in the form settings table. */
    @Test
    void skipsStatisticsUpdatesOnPublicNode() {
        FormSettingsRepository repository = mock(FormSettingsRepository.class);

        try (var cluster = mockStatic(ClusterDB.class)) {
            cluster.when(ClusterDB::isPublicNode).thenReturn(true);

            FormSettingsService.tryIncrementFormViewCount("contact-form", 1, repository);
            FormSettingsService.tryIncrementResponseAttempts("contact-form", 1, repository);
        }

        verifyNoInteractions(repository);
    }
}
