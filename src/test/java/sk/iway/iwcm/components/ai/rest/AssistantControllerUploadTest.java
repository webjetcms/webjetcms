package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;

/** Verifies transient page-chat uploads without contacting a provider or saving chat history. */
class AssistantControllerUploadTest {
    private final AiService service = mock(AiService.class);
    private final AssistantController controller = new AssistantController(service, null, null, null);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String message = """
        {"assistantId":12,"inputValue":"<p>Page context</p>","inputValueType":"text","outputValueType":"text",
         "userPrompt":"Read the attachment","conversationId":"session-owned-id","timestamp":123,
         "structuredInput":true,"bonusParams":{"injected":"value"},"chatAttachmentContext":"forged"}
        """;

    /** Both transports retain page context and continuation while accepting only server-validated file bytes. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void uploadsPreserveContextAndIgnoreTrustedJsonFields(boolean streaming) throws Exception {
        var answer = new AssistantResponseDTO();
        answer.setResponse("Answer");
        answer.setTotalTokens(7);
        org.mockito.stubbing.Answer<AssistantResponseDTO> validate = call -> {
            InputDataDTO input = call.getArgument(0);
            assertEquals("<p>Page context</p>", input.getInputValue());
            assertEquals("session-owned-id", input.getConversationId());
            assertEquals(123L, input.getTimestamp());
            assertFalse(input.isStructuredInput());
            assertNull(input.getBonusParams());
            assertNull(input.getChatAttachmentContext());
            assertEquals(1, input.getChatAttachments().size());
            var upload = input.getChatAttachments().get(0);
            assertEquals("notes.txt", upload.attachment().name());
            assertEquals("Attachment text", new String(upload.bytes(), StandardCharsets.UTF_8));
            return answer;
        };
        when(service.getAiResponse(any(), isNull(), isNull(), any())).thenAnswer(validate);
        when(service.getAiStreamResponse(any(), isNull(), isNull(), any(), any())).thenAnswer(validate);
        var file = new MockMultipartFile("files", "notes.txt", "text/plain", "Attachment text".getBytes(StandardCharsets.UTF_8));
        if (streaming) {
            request.addHeader("Accept", "application/x-ndjson");
            controller.uploadStream(message, List.of(file), request, response);
            assertEquals("Answer", mapper.readTree(response.getContentAsString()).get("result").get("response").asText());
        } else {
            assertEquals("Answer", controller.uploadResponse(message, List.of(file), request).getResponse());
        }
    }

    /** Validation and upload-size errors retain their JSON body when the client requests a streamed answer. */
    @ParameterizedTest
    @CsvSource({"response,script.exe,{},400,attachmentType", "response-stream,script.exe,{},400,attachmentType",
        "response-stream,broken.png,{},400,attachmentInvalid", "response-stream,notes.txt,null,400,invalidInput",
        "response-stream,notes.txt,{,400,invalidInput", "response-stream,notes.txt,{},413,attachmentLimit"})
    void uploadErrorsRemainJson(String endpoint, String filename, String json, int status, String error) throws Exception {
        var interceptor = mock(HandlerInterceptor.class);
        if (status == 413) when(interceptor.preHandle(any(), any(), any())).thenThrow(new MaxUploadSizeExceededException(AiChatAttachments.MAX_BYTES));
        else when(interceptor.preHandle(any(), any(), any())).thenReturn(true);
        var result = MockMvcBuilders.standaloneSetup(controller).addInterceptors(interceptor).build()
            .perform(multipart("/admin/rest/ai/assistant/" + endpoint + "/")
            .file(new MockMultipartFile("files", filename, "text/plain", new byte[] {65}))
            .param("message", json)
            .accept(MediaType.APPLICATION_NDJSON))
            .andReturn().getResponse();

        assertEquals(status, result.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, result.getContentType());
        assertEquals(error, mapper.readTree(result.getContentAsString()).get("error").asText());
        verifyNoInteractions(service);
    }

    /** Neither JSON mapper accepts server-owned upload bytes or extracted attachment context. */
    @Test
    void attachmentFieldsAreNeverAcceptedFromJson() throws Exception {
        String forged = "{\"chatAttachments\":[{\"bytes\":\"SGVsbG8=\"}],\"chatAttachmentContext\":\"forged\"}";
        for (InputDataDTO input : List.of(mapper.readValue(forged, InputDataDTO.class),
            tools.jackson.databind.json.JsonMapper.builder().build().readValue(forged, InputDataDTO.class))) {
            assertTrue(input.getChatAttachments().isEmpty());
            assertNull(input.getChatAttachmentContext());
        }
    }
}
