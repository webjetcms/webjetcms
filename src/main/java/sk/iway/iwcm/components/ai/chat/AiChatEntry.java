package sk.iway.iwcm.components.ai.chat;

import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Full message text and provider resource ownership for one generation attempt. */
@Getter
@Setter
public class AiChatEntry {
    private String id;
    private String requestId;
    private String turnId;
    private long sortOrder;
    private String role;
    private String text;
    private String inputValue;
    /** Indicates that the input is a selected rich-text fragment rather than a complete page. */
    private boolean inputIsSelectedTextInEditor;
    private String status;
    private Date created;
    private Date updated;
    private int totalTokens;
    private String errorCategory;
    private Integer providerStatus;
    private Boolean retryable;
    private String providerResponseId;
    private boolean storageUnknown;
    private boolean providerDeleted;
    private List<AiChatAttachment> attachments = List.of();
}
