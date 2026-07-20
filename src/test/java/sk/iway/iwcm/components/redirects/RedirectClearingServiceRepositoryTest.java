package sk.iway.iwcm.components.redirects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.components.redirects.RedirectClearingAction.ActionType;
import sk.iway.iwcm.components.redirects.RedirectClearingService.ExecutionResult;
import sk.iway.iwcm.system.RedirectsRepository;

class RedirectClearingServiceRepositoryTest extends RedirectClearingTestSupport {

    @Test
    void loadsSelectedScopesAndStoresAnalysisMetadata() {
        RedirectsRepository repository = mock(RedirectsRepository.class);
        RedirectClearingService service = new RedirectClearingService(repository, domainId -> DOMAIN);
        when(repository.findAllForRedirectClearing(DOMAIN, false)).thenReturn(List.of());

        RedirectClearingPlan analyzed = service.analyze(23, false);

        assertEquals(23, analyzed.getAnalyzedDomainId());
        assertEquals(DOMAIN, analyzed.getAnalyzedDomain());
        assertFalse(analyzed.isIncludeUnnamed());
        verify(repository).findAllForRedirectClearing(DOMAIN, false);
    }

    @Test
    void executesStoredPlan() {
        RedirectsRepository repository = mock(RedirectsRepository.class);
        RedirectClearingService service = new RedirectClearingService(repository);
        when(repository.updateNewUrlForRedirectClearing(List.of(1L), "/target", DOMAIN)).thenReturn(1);
        when(repository.deleteForRedirectClearing(List.of(2L, 3L), DOMAIN)).thenReturn(0);
        RedirectClearingPlan stored = plan(List.of(
            action(1, ActionType.UPDATE_OPTIMIZE, "/target"),
            action(2, ActionType.DELETE_DUPLICATE, null),
            action(3, ActionType.DELETE_OLD, null)
        ));

        ExecutionResult result = service.execute(stored);

        assertEquals(1, result.getUpdated());
        assertEquals(0, result.getDeleted());
        assertEquals(2, result.getSkipped());
        verify(repository).updateNewUrlForRedirectClearing(List.of(1L), "/target", DOMAIN);
        verify(repository).deleteForRedirectClearing(List.of(2L, 3L), DOMAIN);
    }
}
