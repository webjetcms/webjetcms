package sk.iway.iwcm.components.ai.chat;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** One versioned JSON document containing a conversation and its cleanup state. */
@Getter
@Setter
public class AiChatDocument {
    private int schemaVersion = 1;
    private AiChatThread thread;
    private List<AiChatEntry> messages = new ArrayList<>();
}
