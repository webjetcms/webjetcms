package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;

/** Verifies HTTP error serialization before saved chat streaming starts. */
class AiChatControllerTest {
    private final AiChatService service = mock(AiChatService.class);
    private final AiChatStore store = mock(AiChatStore.class);
    private final AiChatController controller = new AiChatController(service, store);
    private final ObjectMapper mapper = new ObjectMapper();

    /** Both response formats retain the status and JSON error code for validation, conflicts and upload limits. */
    @ParameterizedTest
    @CsvSource({
        "application/json,400,invalidInput", "application/x-ndjson,400,invalidInput",
        "application/json,409,stale", "application/x-ndjson,409,stale",
        "application/json,413,attachmentLimit", "application/x-ndjson,413,attachmentLimit"
    })
    void preStreamErrorsRemainJson(String accept, int status, String error) throws Exception {
        var interceptor = mock(HandlerInterceptor.class);
        RuntimeException failure = status == 413
            ? new MaxUploadSizeExceededException(AiChatAttachments.MAX_BYTES)
            : new ResponseStatusException(HttpStatus.valueOf(status), error);
        when(interceptor.preHandle(any(), any(), any())).thenThrow(failure);

        var response = MockMvcBuilders.standaloneSetup(controller).addInterceptors(interceptor).build()
            .perform(post("/admin/rest/ai/chat/messages")
                .contentType(MediaType.APPLICATION_JSON).content("{}").accept(accept))
            .andReturn().getResponse();

        assertEquals(status, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals(error, mapper.readTree(response.getContentAsString()).get("error").asText());
        verifyNoInteractions(service, store);
    }
}
