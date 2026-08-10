package sk.iway.iwcm.components.ai.security;

import java.util.Set;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;

/**
 * WebJET audit adapter for prompt-injection detections reported by {@code webjet-ai}.
 */
public final class PromptInjectionDefense {

    private PromptInjectionDefense() { }

    public static void auditDetections(
        Set<com.webjetcms.ai.security.PromptInjectionDefense.UntrustedSource> suspiciousSources,
        Long assistantId
    ) {
        if (suspiciousSources == null || suspiciousSources.isEmpty()) return;

        for (com.webjetcms.ai.security.PromptInjectionDefense.UntrustedSource source : suspiciousSources) {
            auditDetection(source, assistantId);
        }
    }

    private static void auditDetection(
        com.webjetcms.ai.security.PromptInjectionDefense.UntrustedSource source,
        Long assistantId
    ) {
        String message = "Detected possible prompt-injection patterns from source " + source.name()
            + " for assistant " + assistantId;
        Logger.warn(PromptInjectionDefense.class, message);
        Adminlog.add(Adminlog.TYPE_AI, message, null, null);
    }

}
