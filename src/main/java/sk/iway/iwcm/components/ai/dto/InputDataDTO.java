package sk.iway.iwcm.components.ai.dto;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.Setter;
import sk.iway.Html2Text;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.SearchTools;
import sk.iway.iwcm.i18n.Prop;

import org.apache.http.entity.ContentType;

/**
 * DTO for AI assistant input data
 */
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

    Long assistantId;
    String userPrompt = null;

    String inputValue = null;
    InputValueType inputValueType;
    InputValueType outputValueType;

    File inputFile = null;

    Integer imageCount;
    String imageSize;
    String imageQuality;

    //append or replace - relevant only for PageBuilder editor
    String replaceMode;

    // timestamp from FE when assistant was called, used as part of running assiatnt task id
    Long timestamp;

    public void removeHtml() {
        if(Tools.isEmpty(inputValue)) return;
        //remove include
        inputValue = SearchTools.removeCommands(inputValue);
        //remove HTML tags
        Html2Text html2Text = new Html2Text(inputValue);
        inputValue = html2Text.getText();
    }

    public InputDataDTO() {
        /* MUST BE HERE - or else @JsonCreator for enum wont work AND request fails with 400 (because Spring dont know how to map text to enum properly) */
    }

    public InputDataDTO(Map<String, String> data) {
        this.assistantId = Tools.getLongValue(data.get("assistantId"), -1L);
        this.inputValue = data.get("inputValue");
        this.inputValueType = InputValueType.from( data.get("inputValueType") );
        this.outputValueType = InputValueType.from( data.get("outputValueType") );
        this.timestamp = Tools.getLongValue(data.get("timestamp"), -1L);
        this.userPrompt = data.get("userPrompt");
    }

    public void prepareData(HttpServletRequest request) throws IllegalStateException {
        Prop prop = Prop.getInstance(request);

        if(inputValueType.equals(InputValueType.IMAGE)) {
            if(Tools.isEmpty(inputValue)) throw new IllegalStateException(prop.getText("components.ai_assistants.image_path.err"));

            //remove /thumb prefix if exists
            if (inputValue.startsWith("/thumb")) inputValue = inputValue.substring("/thumb".length());
            //remove any parameters
            int pos = inputValue.indexOf("?");
            if (pos != -1) inputValue = inputValue.substring(0, pos);

            String realPath = Tools.getRealPath(inputValue);
            File fileImage = new File(realPath);

            if (fileImage.isFile() == false) throw new IllegalStateException(prop.getText("components.ai_assistants.not_image.err"));

            BufferedImage image;
            try {
                image = ImageIO.read( fileImage );
            } catch (IOException ioe) {
                throw new IllegalStateException(prop.getText("components.ai_assistants.not_image.err"));
            }

            if (image == null) throw new IllegalStateException(prop.getText("components.ai_assistants.not_image.err"));

            inputFile = fileImage;
        }
    }

    public String getMimeType() {
        if(inputFile == null) return "image/png";

        String fileName = inputFile.getName();
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".webp")) {
            return "image/webp";
        } else {
            return "image/png"; // default
        }
    }

    public ContentType getContentType() {
        return ContentType.create( getMimeType() );
    }

    public String getFileAsBase64() throws IOException {
        // Read the file bytes
        byte[] fileContent = Files.readAllBytes(inputFile.toPath());
        // Encode to Base64
        return Base64.getEncoder().encodeToString(fileContent);
    }
}
