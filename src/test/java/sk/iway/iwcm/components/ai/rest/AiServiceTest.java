package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
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

    /** Standalone discovery scopes the database lookup and excludes disabled or unavailable assistants. */
    @Test
    void standaloneChatUsesCurrentDomainAndOnlyConfiguredActiveChats() {
        AssistantDefinitionRepository repository = mock(AssistantDefinitionRepository.class);
        AiInterface configured = mock(AiInterface.class);
        when(configured.getProviderId()).thenReturn("test");
        when(configured.isInit()).thenReturn(true);
        AiService service = new AiService(List.of(configured), null, null);

        AssistantDefinitionEntity chat = chat(1L);
        chat.setInstructions("Server-only instructions");
        AssistantDefinitionEntity disabled = chat(2L);
        disabled.setActive(false);
        AssistantDefinitionEntity generation = chat(3L);
        generation.setAction(SupportedActions.GENERATE_TEXT.getAction());
        AssistantDefinitionEntity missing = chat(4L);
        missing.setProvider("missing");
        when(repository.findAllByDomainId(42)).thenReturn(List.of(chat, disabled, generation, missing));

        try (MockedStatic<CloudToolsForCore> domains = mockStatic(CloudToolsForCore.class);
                MockedStatic<Cache> caches = mockStatic(Cache.class)) {
            domains.when(CloudToolsForCore::getDomainId).thenReturn(42);
            caches.when(Cache::getInstance).thenReturn(mock(Cache.class));

            List<DataTableAi> result = service.getChatAssistants(repository, mock(Prop.class));

            assertEquals(List.of(1L), result.stream().map(DataTableAi::getAssistantId).toList());
            assertNull(result.get(0).getInstructions());
        }
    }

    private static AssistantDefinitionEntity chat(Long id) {
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setId(id);
        assistant.setProvider("test");
        assistant.setName("Chat " + id);
        assistant.setAction(SupportedActions.LIVE_CHAT.getAction());
        assistant.setActive(true);
        return assistant;
    }
}
