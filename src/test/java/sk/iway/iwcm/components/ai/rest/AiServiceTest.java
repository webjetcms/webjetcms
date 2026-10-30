package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
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
        Cache cache = mock(Cache.class);
        AiInterface configured = mock(AiInterface.class);
        when(configured.getProviderId()).thenReturn("test");
        when(configured.isInit()).thenReturn(true);
        AiInterface unavailable = mock(AiInterface.class);
        when(unavailable.getProviderId()).thenReturn("unavailable");
        AiService service = new AiService(List.of(configured, unavailable), null, null);

        AssistantDefinitionEntity chat = chat(1L, "test", "Chat");
        chat.setInstructions("Server-only instructions");
        chat.setFieldFrom("data");
        chat.setFieldTo("data");
        AssistantDefinitionEntity disabled = chat(2L, "test", "Disabled");
        disabled.setActive(false);
        AssistantDefinitionEntity generation = chat(3L, "test", "Generate");
        generation.setAction(SupportedActions.GENERATE_TEXT.getAction());
        AssistantDefinitionEntity missing = chat(4L, "missing", "Missing");
        AssistantDefinitionEntity unconfigured = chat(5L, "unavailable", "Unconfigured");
        when(repository.findAllByDomainId(42)).thenReturn(List.of(chat, disabled, generation, missing, unconfigured));

        try (MockedStatic<CloudToolsForCore> domains = mockStatic(CloudToolsForCore.class);
                MockedStatic<Cache> caches = mockStatic(Cache.class)) {
            domains.when(CloudToolsForCore::getDomainId).thenReturn(42);
            caches.when(Cache::getInstance).thenReturn(cache);

            List<DataTableAi> result = service.getChatAssistants(repository, mock(Prop.class));

            verify(repository).findAllByDomainId(42);
            assertEquals(1, result.size());
            DataTableAi assistant = result.get(0);
            assertEquals(1L, assistant.getAssistantId());
            assertNull(assistant.getFrom());
            assertNull(assistant.getTo());
            assertNull(assistant.getToDefinition());
            assertNull(assistant.getInstructions(), "Server provider instructions must not be exposed to the browser.");
        }
    }

    private static AssistantDefinitionEntity chat(Long id, String provider, String name) {
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setId(id);
        assistant.setProvider(provider);
        assistant.setName(name);
        assistant.setAction(SupportedActions.LIVE_CHAT.getAction());
        assistant.setActive(true);
        return assistant;
    }
}
