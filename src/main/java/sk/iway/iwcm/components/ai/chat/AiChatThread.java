package sk.iway.iwcm.components.ai.chat;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/** Private, domain-scoped chat and its durable execution/deletion state. */
@Getter
@Setter
public class AiChatThread {
    private String id;
    private Integer userId;
    private Integer domainId;
    private String domainName;
    private String title;
    private Long assistantId;
    private String assistantName;
    private String provider;
    private String providerScope;
    private String model;
    private Long assistantVersion;
    private Date created;
    private Date updated;
    private String status = "ACTIVE";
    private long revision;
    private long nextSequence;
    private String lastResponseId;
    private String activeMessageId;
    private Date leaseUntil;
    private boolean stopRequested;
    private String deletionError;
    private String deletionLeaseId;
}
