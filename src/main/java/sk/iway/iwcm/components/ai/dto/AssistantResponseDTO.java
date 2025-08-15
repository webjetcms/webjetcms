package sk.iway.iwcm.components.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;

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
