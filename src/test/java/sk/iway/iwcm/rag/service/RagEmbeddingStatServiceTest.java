package sk.iway.iwcm.rag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        Constants.setString("ragEmbeddingProvider", "openai");
        Constants.setString("ragEmbeddingModel", "text-embedding-3-small");
        AssistantDefinitionRepository repository = mock(AssistantDefinitionRepository.class);
        AssistantDefinitionEntity existing = new AssistantDefinitionEntity();
        existing.setProvider("gemini");
        existing.setModel("gemini-embedding-001");
        when(repository.findFirstByGroupNameAndDomainId(eq(RagEmbeddingStatService.GROUP_INDEXING), anyInt()))
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
        when(repository.findFirstByGroupNameAndDomainId(eq(RagEmbeddingStatService.GROUP_SEARCH), anyInt()))
            .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RagEmbeddingStatService service = new RagEmbeddingStatService(repository, mock(AiStatRepository.class));

        AssistantDefinitionEntity result;
        try (MockedStatic<AiAssistantsService> ignored = mockStatic(AiAssistantsService.class)) {
            result = service.getSearchAssistant();
        }

        ArgumentCaptor<AssistantDefinitionEntity> captor = ArgumentCaptor.forClass(AssistantDefinitionEntity.class);
        verify(repository).save(captor.capture());
        assertSame(captor.getValue(), result);
        assertEquals("gemini", result.getProvider());
        assertEquals("gemini-embedding-001", result.getModel());
    }
}
