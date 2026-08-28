package sk.iway.iwcm.rag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatEntity;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.test.BaseWebjetTest;

class RagEmbeddingStatServiceTest extends BaseWebjetTest {

    private String originalProvider;
    private String originalModel;

    @BeforeEach
    void rememberConfiguration() {
        originalProvider = Constants.getString("ragEmbeddingProvider");
        originalModel = Constants.getString("ragEmbeddingModel");
    }

    @AfterEach
    void restoreConfiguration() {
        Constants.setString("ragEmbeddingProvider", originalProvider);
        Constants.setString("ragEmbeddingModel", originalModel);
    }

    @Test
    void returnsExistingAssistantWithoutOverwritingProviderAndModel() {
        AssistantDefinitionRepository repository = mock(AssistantDefinitionRepository.class);
        AssistantDefinitionEntity existing = new AssistantDefinitionEntity();
        existing.setProvider("gemini");
        existing.setModel("gemini-embedding-001");
        when(repository.findFirstByNameAndDomainIdOrderByIdAsc(eq(RagEmbeddingStatService.NAME_INDEXING), anyInt()))
            .thenReturn(Optional.of(existing));

        RagEmbeddingStatService service = new RagEmbeddingStatService(repository, mock(AiStatRepository.class));

        AssistantDefinitionEntity result = service.getIndexingAssistant();

        assertSame(existing, result);
        assertEquals("gemini", result.getProvider());
        assertEquals("gemini-embedding-001", result.getModel());
        verify(repository, never()).save(any());
    }

    @Test
    void createsMissingAssistantFromConfiguredDefaults() {
        Constants.setString("ragEmbeddingProvider", "GEMINI");
        Constants.setString("ragEmbeddingModel", "gemini-embedding-001");
        AssistantDefinitionRepository repository = mock(AssistantDefinitionRepository.class);
        when(repository.findFirstByNameAndDomainIdOrderByIdAsc(eq(RagEmbeddingStatService.NAME_SEARCH), anyInt()))
            .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RagEmbeddingStatService service = new RagEmbeddingStatService(repository, mock(AiStatRepository.class));

        AssistantDefinitionEntity result;
        try (MockedStatic<AiAssistantsService> ignored = mockStatic(AiAssistantsService.class)) {
            result = service.getSearchAssistant();
        }

        verify(repository).save(result);
        assertEquals(RagEmbeddingStatService.NAME_SEARCH, result.getName());
        assertEquals(RagEmbeddingStatService.GROUP_SEARCH, result.getGroupName());
        assertEquals("gemini", result.getProvider());
        assertEquals("gemini-embedding-001", result.getModel());
    }

    @Test
    void usesExplicitDomainForAssistantLookupAndCreation() {
        int domainId = 17;
        AssistantDefinitionRepository repository = mock(AssistantDefinitionRepository.class);
        when(repository.findFirstByNameAndDomainIdOrderByIdAsc(RagEmbeddingStatService.NAME_INDEXING, domainId))
            .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RagEmbeddingStatService service = new RagEmbeddingStatService(repository, mock(AiStatRepository.class));

        AssistantDefinitionEntity result;
        try (MockedStatic<AiAssistantsService> ignored = mockStatic(AiAssistantsService.class)) {
            result = service.getIndexingAssistant(domainId);
        }

        assertEquals(Integer.valueOf(domainId), result.getDomainId());
        verify(repository).findFirstByNameAndDomainIdOrderByIdAsc(RagEmbeddingStatService.NAME_INDEXING, domainId);
        verify(repository).save(result);
    }

    @Test
    void recordsTokensForExplicitDomain() {
        int domainId = 17;
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setId(42L);
        assistant.setDomainId(domainId);
        AiStatRepository statRepository = mock(AiStatRepository.class);
        RagEmbeddingStatService service = new RagEmbeddingStatService(mock(AssistantDefinitionRepository.class), statRepository);

        service.recordIndexingTokens(assistant, 123, domainId);

        ArgumentCaptor<AiStatEntity> captor = ArgumentCaptor.forClass(AiStatEntity.class);
        verify(statRepository).save(captor.capture());
        assertEquals(42L, captor.getValue().getAssistantId());
        assertEquals(123, captor.getValue().getUsedTokens());
        assertEquals(Integer.valueOf(domainId), captor.getValue().getDomainId());
    }

    @Test
    void rejectsStatisticsForDifferentAssistantDomain() {
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setId(42L);
        assistant.setDomainId(2);
        AiStatRepository statRepository = mock(AiStatRepository.class);
        RagEmbeddingStatService service = new RagEmbeddingStatService(mock(AssistantDefinitionRepository.class), statRepository);

        assertThrows(IllegalStateException.class, () -> service.recordSearchTokens(assistant, 123, 3));

        verify(statRepository, never()).save(any());
    }
}
