package sk.iway.iwcm.components.ai.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.DataTableColumn;

/** Verifies virtual global-chat bindings without registering a persistent entity. */
class AiAssistantsServiceTest {

    /** Offers global bindings in their respective inputs and preserves previously entered target fields. */
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
            assertEquals(List.of("dt-format-text,top-search-field"), service.getFieldOptions("dt-format-text, top", "*", "live_chat"));
            assertTrue(service.getFieldOptions("top", "*").isEmpty());
            assertTrue(service.getFieldOptions("top", "*", "generate_text").isEmpty());
        }
    }

    /** The universal chat matches the three text formats while its search token is reserved for global chat. */
    @Test
    void universalChatMatchesTextEditorFields() {
        AssistantDefinitionEntity chat = assistant("*");
        chat.setActive(true);
        chat.setFieldTo("dt-format-text,dt-format-text-wrap,dt-format-wysiwyg,top-search-field");
        AiAssitantsInterface provider = mock(AiAssitantsInterface.class);
        when(provider.getProviderId()).thenReturn("test");
        when(provider.isInit()).thenReturn(true);
        AiAssistantsService service = new AiAssistantsService(List.of(provider));
        DataTableColumn column = mock(DataTableColumn.class);
        String entity = "sk.iway.iwcm.doc.DocDetails";
        Cache cache = mock(Cache.class);
        when(cache.getObject(anyString(), eq(List.class))).thenReturn(List.of(chat));
        try (var tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
             var caches = mockStatic(Cache.class);
             var domains = mockStatic(CloudToolsForCore.class)) {
            tools.when(() -> Tools.getSpringBean("aiAssistantsService", AiAssistantsService.class)).thenReturn(service);
            caches.when(Cache::getInstance).thenReturn(cache);
            domains.when(CloudToolsForCore::getDomainId).thenReturn(1);

            for (String format : List.of("dt-format-text", "dt-format-text-wrap", "dt-format-wysiwyg")) {
                when(column.getRenderFormat()).thenReturn(format);
                assertEquals(List.of(chat), AiAssistantsService.getAssistantAndFieldFrom("title", column, entity), format);
            }
            when(column.getClassName()).thenReturn("ai-off");
            assertTrue(AiAssistantsService.getAssistantAndFieldFrom("title", column, entity).isEmpty());
            when(column.getClassName()).thenReturn("top-search-field");
            when(column.getRenderFormat()).thenReturn("dt-format-image");
            assertTrue(AiAssistantsService.getAssistantAndFieldFrom("image", column, entity).isEmpty());
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
