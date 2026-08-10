package sk.iway.iwcm.components.ai.providers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.webjetcms.ai.AiOperation;
import com.webjetcms.ai.AiRequest;

import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;

class LibrarySupportLogicTest {

    @TempDir
    Path tempDirectory;

    @Test
    void mapsTextInputWithoutMedia() throws Exception {
        AssistantDefinitionEntity assistant = assistant();
        InputDataDTO input = new InputDataDTO();
        input.setInputValueType(InputDataDTO.InputValueType.TEXT);
        input.setInputValue("Text to process");
        input.setUserPrompt("Make it shorter");

        AiRequest request = LibrarySupportLogic.buildRequest(
            AiOperation.TEXT,
            assistant,
            input,
            "System instructions"
        );

        assertEquals("Text to process", request.inputText());
        assertNull(request.inputMedia());
        assertFalse(request.store());
    }

    @Test
    void imageOperationMapsTextToGenerateAndImageToEdit() {
        InputDataDTO textInput = new InputDataDTO();
        textInput.setInputValueType(InputDataDTO.InputValueType.TEXT);

        InputDataDTO imageInput = new InputDataDTO();
        imageInput.setInputValueType(InputDataDTO.InputValueType.IMAGE);

        assertEquals(AiOperation.GENERATE_IMAGE, LibrarySupportLogic.imageOperation(textInput));
        assertEquals(AiOperation.EDIT_IMAGE, LibrarySupportLogic.imageOperation(imageInput));
    }

    @Test
    void mapsImageInputToEditWithoutLeakingCmsPathAsText() throws Exception {
        byte[] imageBytes = { 1, 2, 3, 4 };
        Path image = Files.write(tempDirectory.resolve("input.png"), imageBytes);

        AssistantDefinitionEntity assistant = assistant();
        InputDataDTO input = new InputDataDTO();
        input.setInputValueType(InputDataDTO.InputValueType.IMAGE);
        input.setInputValue("/images/gallery/input.png");
        input.setInputFile(image.toFile());

        AiRequest request = LibrarySupportLogic.buildRequest(
            LibrarySupportLogic.imageOperation(input),
            assistant,
            input,
            "System instructions"
        );

        assertEquals(AiOperation.EDIT_IMAGE, request.operation());
        assertNull(request.inputText());
        assertEquals("image/png", request.inputMedia().mediaType());
        assertEquals("input.png", request.inputMedia().fileName());
        assertArrayEquals(imageBytes, request.inputMedia().data());
    }

    private AssistantDefinitionEntity assistant() {
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setModel("test-model");
        assistant.setUseTemporal(true);
        return assistant;
    }
}
