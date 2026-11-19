package sk.iway.iwcm.components.ai.chat;

/** Metadata for an attachment stored beside its owner's private conversation. */
public record AiChatAttachment(String id, String name, String mediaType, long size, String sha256) { }
