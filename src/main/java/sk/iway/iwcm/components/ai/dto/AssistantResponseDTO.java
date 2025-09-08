package sk.iway.iwcm.components.ai.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;

@Getter
@Setter
public class AssistantResponseDTO {
    private String response;
    private String error;
    private int totalTokens;
    private List<String> tempFiles = new ArrayList<>();
    private String generatedFileName;

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
