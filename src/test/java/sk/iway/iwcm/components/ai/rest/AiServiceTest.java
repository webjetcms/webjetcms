package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;

class AiServiceTest {

    @Test
    void backendStructuredInputBypassesHtmlRemoval() {
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setKeepHtml(false);
        InputDataDTO inputData = new InputDataDTO();

        assertTrue(AiService.shouldRemoveHtml(assistant, inputData));

        inputData.setStructuredInput(true);
        assertFalse(AiService.shouldRemoveHtml(assistant, inputData));

        inputData.setStructuredInput(false);
        assistant.setKeepHtml(true);
        assertFalse(AiService.shouldRemoveHtml(assistant, inputData));
    }
}
