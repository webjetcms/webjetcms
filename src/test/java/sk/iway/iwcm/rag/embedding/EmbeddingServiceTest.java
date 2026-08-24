package sk.iway.iwcm.rag.embedding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;
import sk.iway.iwcm.test.BaseWebjetTest;

class EmbeddingServiceTest extends BaseWebjetTest {

    private int originalDimensions;
    private String originalProvider;
    private String originalModel;

    @BeforeEach
    void rememberConfiguration() {
        originalDimensions = Constants.getInt("ragEmbeddingDimensions");
        originalProvider = Constants.getString("ragEmbeddingProvider");
        originalModel = Constants.getString("ragEmbeddingModel");
    }

    @AfterEach
    void restoreConfiguration() {
        Constants.setInt("ragEmbeddingDimensions", originalDimensions);
        Constants.setString("ragEmbeddingProvider", originalProvider);
        Constants.setString("ragEmbeddingModel", originalModel);
    }

    @Test
    void passesConfiguredDimensionsToGeminiProvider() throws Exception {
        Constants.setInt("ragEmbeddingDimensions", 1536);
        Constants.setString("ragEmbeddingProvider", "openai");
        Constants.setString("ragEmbeddingModel", "text-embedding-3-small");
        AiClient aiClient = mock(AiClient.class);
        WebjetAiConfigurationService configurationService = mock(WebjetAiConfigurationService.class);
        AiProviderConfig providerConfig = AiProviderConfig.builder("secret-key").build();
        when(configurationService.resolveForDomain("gemini", "customer.example"))
            .thenReturn(providerConfig);
        when(aiClient.embed(eq("gemini"), any(EmbeddingRequest.class), eq(providerConfig)))
            .thenReturn(new EmbeddingResponse(
                List.of(new EmbeddingVector(new float[1536])),
                new TokenUsage(7, 0, 7, null)
            ));

        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setProvider("gemini");
        assistant.setModel("gemini-embedding-001");
        EmbeddingService service = new EmbeddingService(aiClient, configurationService);

        EmbeddingBatchResult result = service.embedWithUsage(
            List.of("Text to embed"),
            assistant,
            "customer.example"
        );

        ArgumentCaptor<EmbeddingRequest> requestCaptor = ArgumentCaptor.forClass(EmbeddingRequest.class);
        verify(aiClient).embed(eq("gemini"), requestCaptor.capture(), eq(providerConfig));
        assertEquals("gemini-embedding-001", requestCaptor.getValue().model());
        assertEquals(1536, requestCaptor.getValue().options().dimensions());
        assertEquals(1536, result.getEmbeddings().get(0).length);
        assertEquals(7, result.getUsedTokens());
    }
}
