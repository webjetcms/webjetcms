package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjetcms.ai.AiErrorCategory;
import com.webjetcms.ai.AiProviderException;

import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.providers.ProviderCallException;

/** Verifies the CMS stream contract without calling an AI provider or a database. */
class AssistantControllerStreamTest {

    private final AiService service = mock(AiService.class);
    private final AssistantController controller = new AssistantController(service, null, null, null);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final InputDataDTO input = new InputDataDTO();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void streamsEscapedTextBeforeTheTerminalResult() throws Exception {
        request.addHeader("Accept", "application/x-ndjson");
        input.setBonusParams(Map.of("untrusted", "value"));
        String text = "Žltý kôň 🐎\n{\"totalTokens\":999,\"error\":\"generated text\"}";
        AssistantResponseDTO result = result(text);
        when(service.getAiStreamResponse(any(), isNull(), isNull(), any(), any())).thenAnswer(call -> {
            assertTrue(response.isCommitted());
            assertNull(input.getBonusParams());
            BufferedWriter writer = call.getArgument(3);
            writer.write(text);
            writer.flush();
            // Text is available while the provider is still running.
            assertEquals(text, mapper.readTree(response.getContentAsString()).get("text").asText());
            return result;
        });

        controller.streamData(input, request, response);

        assertEquals("application/x-ndjson;charset=UTF-8", response.getContentType());
        assertEquals("no-cache", response.getHeader("Cache-Control"));
        assertEquals("no", response.getHeader("X-Accel-Buffering"));
        String[] messages = response.getContentAsString().split("\n");
        assertEquals(2, messages.length);
        assertEquals("delta", mapper.readTree(messages[0]).get("type").asText());
        JsonNode terminal = mapper.readTree(messages[1]);
        assertEquals("result", terminal.get("type").asText());
        assertEquals(42, terminal.get("result").get("totalTokens").asInt());
        assertTrue(terminal.get("result").get("error").isNull());
        assertFalse(terminal.get("result").has("errorCategory"));
        assertFalse(terminal.get("result").has("providerStatusCode"));
        assertFalse(terminal.get("result").has("retryable"));
    }

    @Test
    void preservesUnicodeAcrossTheBufferedWriterBoundaryAndFlushesPendingText() throws Exception {
        request.addHeader("Accept", "application/x-ndjson; charset=UTF-8");
        String prefix = "a".repeat(8191);
        String suffix = "🐎 koniec\n";
        when(service.getAiStreamResponse(any(), isNull(), isNull(), any(), any())).thenAnswer(call -> {
            BufferedWriter writer = call.getArgument(3);
            writer.write(prefix);
            writer.write(suffix);
            // No provider flush: the controller must drain pending text before the result.
            return result(prefix + suffix);
        });

        controller.streamData(input, request, response);

        String[] messages = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8).split("\n");
        assertEquals(2, messages.length);
        assertEquals(prefix + suffix, mapper.readTree(messages[0]).get("text").asText());
        assertEquals(42, mapper.readTree(messages[1]).get("result").get("totalTokens").asInt());
    }

    /** JSON and streaming failures expose the same safe metadata, including after partial text. */
    @ParameterizedTest
    @EnumSource(AiErrorCategory.class)
    void providerFailureAfterPartialTextHasASeparateTerminalError(AiErrorCategory category) throws Exception {
        request.addHeader("Accept", "application/x-ndjson");
        String error = "Provider failed\nTry again";
        int statusCode = category == AiErrorCategory.CANCELLED ? 0 : 400;
        ProviderCallException failure = new ProviderCallException(new ProviderCallException(error,
            new AiProviderException("test-provider", statusCode, error, "private raw response", false, null, category)));
        when(service.getAiStreamResponse(any(), isNull(), isNull(), any(), any())).thenAnswer(call -> {
            BufferedWriter writer = call.getArgument(3);
            writer.write("Partial text");
            writer.flush();
            throw failure;
        });
        when(service.getAiResponse(any(), isNull(), isNull(), any())).thenThrow(failure);
        when(service.getAiImageResponse(any(), isNull(), isNull(), any())).thenThrow(failure);

        controller.streamData(input, request, response);

        String[] messages = response.getContentAsString().split("\n");
        assertEquals(2, messages.length);
        assertEquals("Partial text", mapper.readTree(messages[0]).get("text").asText());
        JsonNode result = mapper.readTree(messages[1]).get("result");
        assertEquals(error, result.get("error").asText());
        assertEquals(category.name(), result.get("errorCategory").asText());
        assertEquals(statusCode > 0 ? mapper.valueToTree(statusCode) : null, result.get("providerStatusCode"));
        assertFalse(result.get("retryable").asBoolean());
        assertEquals(0, result.get("totalTokens").asInt());
        assertFalse(result.toString().contains("private raw response"));
        assertFalse(result.has("cause"));
        assertEquals(result, mapper.valueToTree(controller.getAiReponse(input, request)));
        assertEquals(result, mapper.valueToTree(controller.getAiImageReponse(input, request)));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "*/*", "text/plain", "application/*", "application/x-ndjson;q=0, text/plain" })
    void keepsTheLegacyFormatUnlessFramingIsExplicitlyRequested(String accept) throws Exception {
        if (accept != null) request.addHeader("Accept", accept);
        AssistantResponseDTO result = result("Legacy text");
        when(service.getAiStreamResponse(any(), isNull(), isNull(), any(), any())).thenAnswer(call -> {
            BufferedWriter writer = call.getArgument(3);
            writer.write("Legacy text");
            writer.flush();
            return result;
        });

        controller.streamData(input, request, response);

        assertEquals("text/plain;charset=UTF-8", response.getContentType());
        assertEquals("Legacy text" + result.toJsonString(), response.getContentAsString());
        assertFalse(response.getContentAsString().contains("\"type\":\"delta\""));
    }

    private AssistantResponseDTO result(String text) {
        AssistantResponseDTO result = new AssistantResponseDTO();
        result.setResponse(text);
        result.setTotalTokens(42);
        return result;
    }
}
