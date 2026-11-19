package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEditorFields;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.jpa.SupportedActions;
import sk.iway.iwcm.components.ai.providers.AiInterface;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.DataTableAi;

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

    /** Global discovery accepts either exact binding and requires the current domain and an active provider. */
    @Test
    void standaloneChatUsesCurrentDomainAndOnlyConfiguredActiveChats() {
        AssistantDefinitionRepository repository = mock(AssistantDefinitionRepository.class);
        AiInterface configured = mock(AiInterface.class);
        when(configured.getProviderId()).thenReturn("test");
        when(configured.isInit()).thenReturn(true);
        AiService service = new AiService(List.of(configured), null, null);

        AssistantDefinitionEntity chat = chat(1L);
        chat.setInstructions("Server-only instructions");
        chat.setDescription("{model}");
        chat.setModel("gpt-6.1-sol");
        AssistantDefinitionEntity disabled = chat(2L);
        disabled.setActive(false);
        AssistantDefinitionEntity generation = chat(3L);
        generation.setClassName("*");
        generation.setFieldTo("top-search-field");
        generation.setAction(SupportedActions.GENERATE_TEXT.getAction());
        AssistantDefinitionEntity missing = chat(4L);
        missing.setProvider("missing");
        AssistantDefinitionEntity editor = chat(5L);
        editor.setClassName("sk.iway.iwcm.doc.DocDetails");
        editor.setFieldFrom("top-search-field");
        editor.setFieldTo("dt-format-text,%top-search-field");
        AssistantDefinitionEntity wildcard = chat(6L);
        wildcard.setClassName("*");
        wildcard.setFieldTo("*");
        AssistantDefinitionEntity shared = chat(7L);
        shared.setClassName("sk.iway.iwcm.doc.DocDetails, CHAT_SEARCH");
        AssistantDefinitionEntity universal = chat(8L);
        universal.setClassName("*");
        universal.setFieldTo("dt-format-text,dt-format-text-wrap,dt-format-wysiwyg, top-search-field ");
        when(repository.findAllByDomainId(42)).thenReturn(List.of(chat, disabled, generation, missing, editor, wildcard, shared, universal));

        try (MockedStatic<CloudToolsForCore> domains = mockStatic(CloudToolsForCore.class);
                MockedStatic<Cache> caches = mockStatic(Cache.class)) {
            domains.when(CloudToolsForCore::getDomainId).thenReturn(42);
            caches.when(Cache::getInstance).thenReturn(mock(Cache.class));

            Prop prop = mock(Prop.class);
            when(prop.getText("{model}", false)).thenReturn("{model}");
            List<DataTableAi> result = service.getChatAssistants(repository, prop);

            assertEquals(List.of(1L, 7L, 8L), result.stream().map(DataTableAi::getAssistantId).toList());
            assertEquals("gpt-6.1-sol", result.get(0).getDescription());
            assertNull(result.get(0).getInstructions());
        }
    }

    /** Model names support literal prefixes, translated prefixes and placeholders inside translated values. */
    @Test
    void resolvesAssistantModelNamesWithoutChangingStoredConfiguration() {
        Prop prop = mock(Prop.class);
        when(prop.getText(anyString(), eq(false))).thenAnswer(invocation -> invocation.getArgument(0));
        when(prop.getText("chat.prefix", false)).thenReturn("Čet");
        when(prop.getText("chat.full", false)).thenReturn("Čet {model}");
        when(prop.getText("chat.original", false)).thenReturn("Pôvodný asistent");
        AssistantDefinitionEntity assistant = chat(1L);
        assistant.setModel("gpt-6.1-sol");

        for (String description : List.of("{model}", "Čet {model}", "chat.prefix {model}", "chat.full", "chat.original")) {
            assistant.setDescription(description);
            String expected = "Čet gpt-6.1-sol";
            if ("{model}".equals(description)) expected = "gpt-6.1-sol";
            if ("chat.original".equals(description)) expected = "Pôvodný asistent";
            assertEquals(expected, assistant.getDisplayName(prop), description);
            AssistantDefinitionEditorFields fields = new AssistantDefinitionEditorFields();
            fields.fromAssistantDefinition(assistant, true, prop);
            assertEquals(expected, fields.getTranslatedDescription(), description);
            assertEquals(description, assistant.getDescription());
        }

        assistant.setDescription("{model}");
        assistant.setModel("gemini-3.8-flash");
        assertEquals("gemini-3.8-flash", assistant.getDisplayName(prop));
        assistant.setDescription("");
        assertEquals("Chat 1", assistant.getDisplayName(prop));
    }

    private static AssistantDefinitionEntity chat(Long id) {
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setId(id);
        assistant.setProvider("test");
        assistant.setName("Chat " + id);
        assistant.setAction(SupportedActions.LIVE_CHAT.getAction());
        assistant.setClassName(AiAssistantsService.CHAT_TARGET);
        assistant.setActive(true);
        return assistant;
    }

    /** Files are allowed only for OpenAI/Gemini text chats, including temporary chats. */
    @Test
    void validatesPageChatAttachmentSupport() {
        InputDataDTO input = new InputDataDTO();
        input.setInputValueType(InputDataDTO.InputValueType.TEXT);
        input.setChatAttachments(AiChatAttachments.read(List.of(
            new MockMultipartFile("files", "notes.txt", "text/plain", new byte[] {65}))));
        AssistantDefinitionEntity assistant = chat(12L);
        Prop prop = mock(Prop.class);
        for (String provider : List.of("openai", "gemini")) {
            assistant.setProvider(provider);
            for (boolean temporal : List.of(true, false)) {
                assistant.setUseTemporal(temporal);
                AiService.validateChatAttachments(assistant, input, prop);
            }
        }
        assistant.setProvider("browser");
        assertThrows(IllegalStateException.class, () -> AiService.validateChatAttachments(assistant, input, prop));
        assistant.setProvider("openai");
        assistant.setAction(SupportedActions.GENERATE_TEXT.getAction());
        assertThrows(IllegalStateException.class, () -> AiService.validateChatAttachments(assistant, input, prop));
        assistant.setAction(SupportedActions.LIVE_CHAT.getAction());
        input.setInputValueType(InputDataDTO.InputValueType.IMAGE);
        assertThrows(IllegalStateException.class, () -> AiService.validateChatAttachments(assistant, input, prop));
    }
}
