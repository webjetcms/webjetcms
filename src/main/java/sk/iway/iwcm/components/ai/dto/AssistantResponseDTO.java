package sk.iway.iwcm.components.ai.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssistantResponseDTO {
    private String response;
    private String error;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
}
