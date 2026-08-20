package sk.iway.iwcm.components.ai.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjetcms.ai.AiPromptTemplate;
import com.webjetcms.ai.security.PromptInjectionDefense.UntrustedSource;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;

class PromptInjectionDefenseTest {

    @Test
    void auditsPreparedRequestDetectionsOncePerSource() {
        Set<UntrustedSource> sources = EnumSet.allOf(UntrustedSource.class);

        try (MockedStatic<Adminlog> adminlogMock = mockStatic(Adminlog.class)) {
            PromptInjectionDefense.auditDetections(sources, 42L);

            String inputMessage = "Detected possible prompt-injection patterns from source INPUT_TEXT for assistant 42";
            String promptMessage = "Detected possible prompt-injection patterns from source USER_PROMPT for assistant 42";
            adminlogMock.verify(
                () -> Adminlog.add(Adminlog.TYPE_AI, inputMessage, (Long) null, (Long) null),
                times(1)
            );
            adminlogMock.verify(
                () -> Adminlog.add(Adminlog.TYPE_AI, promptMessage, (Long) null, (Long) null),
                times(1)
            );
            adminlogMock.verifyNoMoreInteractions();
        }
    }

    @Test
    void replaceModeOmitsInputTextFromExpandedInstructionsWithoutMutatingDto() {
        InputDataDTO inputData = new InputDataDTO();
        inputData.setInputValue("Page builder selection");
        inputData.setUserPrompt("Rewrite it");
        inputData.setReplaceMode("replace");

        AiPromptTemplate.ExpansionResult result = AiAssistantsService.expandPromptMacros(
            "Input: {inputText}\nPrompt: {userPrompt}",
            inputData
        );

        assertFalse(result.instructions().contains("Page builder selection"));
        assertTrue(result.instructions().contains("Rewrite it"));
        assertEquals("Page builder selection", inputData.getInputValue());
        assertEquals("Rewrite it", inputData.getUserPrompt());
    }

    @Test
    void protectedMacroValuesRemainValidJsonStringContent() throws Exception {
        InputDataDTO inputData = new InputDataDTO();
        inputData.setUserPrompt("Line one\n\"quoted value\"");

        String instructions = AiAssistantsService.expandPromptMacros(
            "{\"imageDescription\":\"{userPrompt}\"}",
            inputData
        ).instructions();
        JsonNode parsed = new ObjectMapper().readTree(instructions);

        String description = parsed.path("imageDescription").asText();
        assertTrue(description.contains("[BEGIN_UNTRUSTED_USER_PROMPT]"));
        assertTrue(description.contains("Line one \"quoted value\""));
        assertTrue(description.contains("[END_UNTRUSTED_USER_PROMPT]"));
    }
}
