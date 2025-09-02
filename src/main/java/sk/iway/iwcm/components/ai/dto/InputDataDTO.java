package sk.iway.iwcm.components.ai.dto;

import java.io.File;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.Setter;
import sk.iway.Html2Text;
import sk.iway.iwcm.Tools;

@Getter
@Setter
public class InputDataDTO {

    public enum InputValueType {
        @JsonEnumDefaultValue TEXT("text"),
        IMAGE("image");

        private final String type;

        InputValueType(String type) { this.type = type; }

        @JsonValue
        public String getType() { return type; }

        @JsonCreator
        public static InputValueType from(String value) {
            if(Tools.isEmpty(value) == false && "image".equalsIgnoreCase(value)) return IMAGE;
            return TEXT;
        }
    }

    Integer assistantId;
    String userPrompt = null;

    String inputValue = null;
    InputValueType inputValueType;

    File inputFile = null;

    Integer imageCount;
    String imageSize;
    String imageQuality;

    public void removeHtml() {
        if(Tools.isEmpty(inputValue)) return;
        Html2Text html2Text = new Html2Text(inputValue);
        inputValue = html2Text.getText();
    }

    public InputDataDTO() {
        /* MUST BE HERE - or else @JsonCreator for enum wont work AND request fails with 400 (because Spring dont know how to map text to enum properly) */
    }

    public InputDataDTO(Map<String, String> data) {
        this.assistantId = Tools.getIntValue(data.get("assistantId"), -1);
        this.inputValue = data.get("inputValue");
        this.inputValueType = InputValueType.from( data.get("inputValueType") );
    }

    public void prepareData() throws IllegalStateException {
        if(inputValueType.equals(InputValueType.IMAGE)) {
            if(Tools.isEmpty(inputValue)) throw new IllegalStateException("No imagePath provided.");

            String realPath = Tools.getRealPath(inputValue);
            File fileImage = new File(realPath);

            if (fileImage.isFile() == false) throw new IllegalStateException("Not a image");

            inputFile = fileImage;
        }
    }
}
