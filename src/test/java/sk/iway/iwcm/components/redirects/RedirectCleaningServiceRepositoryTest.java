package sk.iway.iwcm.components.redirects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.components.redirects.RedirectCleaningAction.ActionType;
import sk.iway.iwcm.components.redirects.RedirectCleaningService.ExecutionResult;
import sk.iway.iwcm.system.RedirectsRepository;

class RedirectCleaningServiceRepositoryTest extends RedirectCleaningTestSupport {

    @Test
    void loadsSelectedScopesAndStoresAnalysisMetadata() {
        RedirectsRepository repository = mock(RedirectsRepository.class);
        RedirectCleaningService service = new RedirectCleaningService(repository, domainId -> DOMAIN);
        when(repository.findAllForRedirectCleaning(DOMAIN, false)).thenReturn(List.of());

        RedirectCleaningPlan analyzed = service.analyze(23, false);

        assertEquals(23, analyzed.getAnalyzedDomainId());
        assertEquals(DOMAIN, analyzed.getAnalyzedDomain());
        assertFalse(analyzed.isIncludeUnnamed());
        verify(repository).findAllForRedirectCleaning(DOMAIN, false);
    }

    @Test
    void executesStoredPlan() {
        RedirectsRepository repository = mock(RedirectsRepository.class);
        RedirectCleaningService service = new RedirectCleaningService(repository);
        when(repository.updateNewUrlForRedirectCleaning(List.of(1L), "/target", DOMAIN)).thenReturn(1);
        when(repository.deleteForRedirectCleaning(List.of(2L, 3L), DOMAIN)).thenReturn(0);
        RedirectCleaningPlan stored = plan(List.of(
            action(1, ActionType.UPDATE_OPTIMIZE, "/target"),
            action(2, ActionType.DELETE_DUPLICATE, null),
            action(3, ActionType.DELETE_OLD, null)
        ));

        ExecutionResult result = service.execute(stored);

        assertEquals(1, result.getUpdated());
        assertEquals(0, result.getDeleted());
        assertEquals(2, result.getSkipped());
        verify(repository).updateNewUrlForRedirectCleaning(List.of(1L), "/target", DOMAIN);
        verify(repository).deleteForRedirectCleaning(List.of(2L, 3L), DOMAIN);
    }
}
