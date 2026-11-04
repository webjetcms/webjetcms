package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.i18n.Prop;

/** Verifies virtual global-chat bindings without registering a persistent entity. */
class AiAssistantsServiceTest {

    /** CHAT_SEARCH is offered alongside scanned entities, including when their metadata is already cached. */
    @Test
    void autocompleteOffersVirtualChatTarget() {
        Cache cache = mock(Cache.class);
        when(cache.getObject("AiAssistantsService_classFieldsMap"))
            .thenReturn(Map.of("sk.iway.iwcm.doc.DocDetails", List.of("data")));
        try (MockedStatic<Cache> caches = mockStatic(Cache.class)) {
            caches.when(Cache::getInstance).thenReturn(cache);
            AiAssistantsService service = new AiAssistantsService(List.of());

            assertEquals(List.of("CHAT_SEARCH"), service.getClassOptions("chat"));
            assertEquals(List.of("sk.iway.iwcm.doc.DocDetails"), service.getClassOptions("DocDetails"));
            assertTrue(service.getFieldOptions("data", "CHAT_SEARCH").isEmpty());
        }
    }

    /** Global-only assistants need no invented target field; mixed editor bindings retain their validation. */
    @Test
    void defaultsUnusedTargetFieldOnlyForGlobalChat() {
        AiAssitantsInterface provider = mock(AiAssitantsInterface.class);
        when(provider.getProviderId()).thenReturn("test");
        when(provider.isInit()).thenReturn(true);
        AiAssistantsService service = new AiAssistantsService(List.of(provider));
        Prop prop = mock(Prop.class);
        AssistantDefinitionEntity global = assistant(" CHAT_SEARCH ");
        service.prepareBeforeSave(global, prop);
        assertEquals("*", global.getFieldTo());

        AssistantDefinitionEntity mixed = assistant("CHAT_SEARCH,sk.iway.iwcm.doc.DocDetails");
        service.prepareBeforeSave(mixed, prop);
        assertNull(mixed.getFieldTo());
        mixed.setFieldTo("data");
        service.prepareBeforeSave(mixed, prop);
        assertEquals("data", mixed.getFieldTo());
    }

    private static AssistantDefinitionEntity assistant(String binding) {
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setClassName(binding);
        assistant.setAction("live_chat");
        assistant.setProvider("test");
        return assistant;
    }
}
