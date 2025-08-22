package sk.iway.iwcm.components.ai.dto;

import java.io.File;

import org.json.JSONObject;

import lombok.Getter;
import lombok.Setter;
import sk.iway.Html2Text;
import sk.iway.iwcm.Tools;

@Getter
@Setter
public class InputDataDTO {

    public enum InputDataType {
        TEXT,
        IMAGE;

        public static InputDataType getInputDataType(String value) {
            if(Tools.isEmpty(value) == false && "image".equalsIgnoreCase(value)) return IMAGE;
            return TEXT;
        }
}

    String inputText = null;
    String userPrompt = null;
    File inputFile = null;
    InputDataType inputDataType;

    public void removeHtml() {
        if(Tools.isEmpty(inputText)) return;
        Html2Text html2Text = new Html2Text(inputText);
        inputText = html2Text.getText();
    }


    public InputDataDTO(String inputData) {
        JSONObject json = new JSONObject(inputData);

        inputDataType = InputDataType.getInputDataType(json.optString("type", ""));

        if(inputDataType == InputDataType.TEXT) {
            userPrompt = json.optString("userPrompt", "");
            inputText = json.optString("value", "");
        } else {
            String imagePath = json.optString("value", "");

            if(Tools.isEmpty(imagePath)) throw new IllegalStateException("No imagePath provided.");

            String realPath = Tools.getRealPath(imagePath);
            File fileImage = new File(realPath);

            if (fileImage.isFile() == false) throw new IllegalStateException("Not a image");

            inputFile = fileImage;
        }
    }
}
