package sk.iway.iwcm.components.ai.jpa;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Enum of supported actions for AI assistants
 */
@Getter
public enum SupportedActions {
    GENERATE_TEXT("generate_text"),
    GENERATE_IMAGE("generate_image"),
    EDIT_IMAGE("edit_image"),
    LIVE_CHAT("live_chat");

    private final String action;

    private static final String ACTION_PREFIX = "components.ai_assistants.supported_actions.";

    SupportedActions(String action) {
        this.action = action;
    }

    public static SupportedActions getSupportedAction(String action) {
        if(Tools.isEmpty(action)) return null;

        if(GENERATE_TEXT.getAction().equalsIgnoreCase(action)) return GENERATE_TEXT;
        if(GENERATE_IMAGE.getAction().equalsIgnoreCase(action)) return GENERATE_IMAGE;
        if(EDIT_IMAGE.getAction().equalsIgnoreCase(action)) return EDIT_IMAGE;
        if(LIVE_CHAT.getAction().equalsIgnoreCase(action)) return LIVE_CHAT;

        return null;
    }

    public static List<LabelValue> getSupportedActions(Prop prop) {
        List<LabelValue> labels = new ArrayList<>();
        for (SupportedActions supportedAction : SupportedActions.values()) {
            labels.add(new LabelValue(prop.getText(ACTION_PREFIX + supportedAction.getAction()), supportedAction.getAction()));
        }
        return labels;
    }

    public static boolean doesSupportAction(AssistantDefinitionEntity assistant, SupportedActions action) throws IllegalStateException {
        if(assistant == null || action == null) return false;

        SupportedActions assistantAction = getSupportedAction(assistant.getAction());

        if(assistantAction == null || assistantAction.equals(action) == false) return false;

        return true;
    }
}
