package sk.iway.iwcm.components.ai.rest;

import java.util.concurrent.ThreadLocalRandom;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.system.cluster.ClusterDB;

/**
 * Cron task that resets token usage for all AI assistants across domains.
 */
public class AiTokenUsageReset {

    private AiTokenUsageReset() {
    }

    /**
     * Resets assistant token counters and records the result in the cron audit log.
     *
     * @param args unused cron arguments
     */
    public static void main(String[] args) {
        if (ClusterDB.isPublicNode()) return;

        try {
            // Spread reset attempts across administrative cluster nodes.
            Thread.sleep(ThreadLocalRandom.current().nextInt(10000));

            AssistantDefinitionRepository repository = Tools.getSpringBean("assistantDefinitionRepository", AssistantDefinitionRepository.class);
            if (repository == null) {
                Logger.error(AiTokenUsageReset.class, "AssistantDefinitionRepository not available");
                return;
            }

            int updatedAssistants = repository.resetUsedTokens();
            Adminlog.add(Adminlog.TYPE_CRON, "Reset token usage for " + updatedAssistants + " AI assistants.", -1, -1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Logger.error(AiTokenUsageReset.class, "Error resetting AI assistant token usage: " + e.getMessage());
        }
    }
}
