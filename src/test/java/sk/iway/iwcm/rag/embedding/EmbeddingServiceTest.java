package sk.iway.iwcm.rag.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiProviderConfig;
import com.webjetcms.ai.EmbeddingRequest;
import com.webjetcms.ai.EmbeddingResponse;
import com.webjetcms.ai.EmbeddingVector;
import com.webjetcms.ai.TokenUsage;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.providers.AiInterface;
import sk.iway.iwcm.components.ai.providers.ProviderCallException;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.test.BaseWebjetTest;

class EmbeddingServiceTest extends BaseWebjetTest {

    private int originalDimensions;

    @BeforeEach
    void rememberConfiguration() {
        originalDimensions = Constants.getInt("ragEmbeddingDimensions");
    }

    @AfterEach
    void restoreConfiguration() {
        Constants.setInt("ragEmbeddingDimensions", originalDimensions);
    }

    @Test
    void passesConfiguredDimensionsAndRejectsReachedTokenLimit() throws Exception {
        Constants.setInt("ragEmbeddingDimensions", 1536);
        AiClient aiClient = mock(AiClient.class);
        WebjetAiConfigurationService configurationService = mock(WebjetAiConfigurationService.class);
        AiInterface provider = mock(AiInterface.class);
        when(provider.getProviderId()).thenReturn("gemini");
        AiProviderConfig providerConfig = AiProviderConfig.builder("secret-key").build();
        when(configurationService.resolveForDomain(provider, "customer.example"))
            .thenReturn(providerConfig);
        when(aiClient.embed(eq("gemini"), any(EmbeddingRequest.class), eq(providerConfig)))
            .thenReturn(new EmbeddingResponse(
                List.of(new EmbeddingVector(new float[1536])),
                new TokenUsage(7, 0, 7, null)
            ));

        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setId(42L);
        assistant.setDomainId(17);
        assistant.setProvider("gemini");
        assistant.setModel("gemini-embedding-001");
        AssistantDefinitionRepository repository = mock(AssistantDefinitionRepository.class);
        EmbeddingService service = new EmbeddingService(aiClient, configurationService, List.of(provider), repository);

        service.embedWithUsage(List.of("Text to embed"), assistant, "customer.example");

        ArgumentCaptor<EmbeddingRequest> requestCaptor = ArgumentCaptor.forClass(EmbeddingRequest.class);
        verify(aiClient).embed(eq("gemini"), requestCaptor.capture(), eq(providerConfig));
        assertEquals("gemini-embedding-001", requestCaptor.getValue().model());
        assertEquals(1536, requestCaptor.getValue().options().dimensions());

        when(repository.countWithReachedTokenLimit(42L, 17)).thenReturn(1L);
        ProviderCallException exception = assertThrows(ProviderCallException.class, () ->
            service.embedWithUsage(List.of("Text to embed"), assistant, "customer.example"));
        assertEquals(Prop.getInstance().getText("components.ai_assistants.token_limit.reached"), exception.getMessage());
        verifyNoMoreInteractions(aiClient);
    }
}
