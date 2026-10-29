package sk.iway.iwcm.components.ai.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.webjetcms.ai.AiErrorCategory;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.providers.ProviderCallException;

/**
 * DTO for AI assistant response
 */
@Getter
@Setter
public class AssistantResponseDTO {
    private String response;
    private String error;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCategory;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer providerStatusCode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean retryable;
    private String conversationId;
    private int totalTokens;
    private List<String> tempFiles = new ArrayList<>();
    private String generatedFileName;

    /** Creates a client-safe failure result without serializing provider responses or exception causes. */
    public static AssistantResponseDTO fromError(Throwable cause) {
        ProviderCallException exception = cause instanceof ProviderCallException providerException
            ? providerException : new ProviderCallException(cause);
        AssistantResponseDTO response = new AssistantResponseDTO();
        response.setError(exception.getLocalizedMessage());
        if (Tools.isEmpty(response.getError())) {
            response.setError(exception.getErrorCategory() == AiErrorCategory.CANCELLED ? "AI request cancelled" : "AI request failed");
        }
        response.setErrorCategory(exception.getErrorCategory().name());
        response.setProviderStatusCode(exception.getProviderStatusCode());
        response.setRetryable(exception.getRetryable());
        return response;
    }

    public void addTempFile(String tempFile) {
        if(tempFiles == null) tempFiles = new ArrayList<>();
        if(Tools.isEmpty(tempFile)) return;
        tempFiles.add(tempFile);
    }

    public String toJsonString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json =  mapper.writeValueAsString(this);
            return json;
        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }
}
