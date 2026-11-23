package sk.iway.iwcm.components.ai.rest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Date;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.FilePathTools;
import sk.iway.iwcm.components.ai.chat.AiChatPaths;
import sk.iway.iwcm.components.ai.rest.AiChatStore.Scope;

/** Optional cron task deleting inactive conversations for an explicitly selected domain. */
public final class AiChatCleanup {
    private AiChatCleanup() { }

    /**
     * Runs retention for all owners in one domain through the normal provider-aware deletion workflow.
     * @param args retention days (positive integer), domain ID and domain name
     * @throws IOException if the domain storage cannot be listed
     */
    public static void main(String[] args) throws IOException {
        if (args == null || args.length != 3) throw new IllegalArgumentException("Expected: retentionDays domainId domainName");
        int days = Integer.parseInt(args[0]);
        int domainId = Integer.parseInt(args[1]);
        if (days < 1 || domainId < 0 || args[2].isBlank()) throw new IllegalArgumentException("Invalid chat retention scope");
        AiChatService service = Tools.getSpringBean("aiChatService", AiChatService.class);
        if (service == null) throw new IllegalStateException("AI chat service is unavailable");
        Date cutoff = new Date(System.currentTimeMillis() - Duration.ofDays(days).toMillis());
        Path root = FilePathTools.isExternalDir(AiChatPaths.ROOT)
            ? new File(FilePathTools.getDomainBaseFolder(args[2]), AiChatPaths.ROOT).getCanonicalFile().toPath()
            : Scope.storageRoot();
        int deleted = cleanup(root, domainId, args[2], cutoff, service);
        Logger.println(AiChatCleanup.class, "Chat retention deleted " + deleted + " conversations in domain " + args[2]);
    }

    static int cleanup(Path root, int domainId, String domainName, Date cutoff, AiChatService service) throws IOException {
        Path domain = root.resolve(Integer.toString(domainId));
        if (!Files.isDirectory(domain, LinkOption.NOFOLLOW_LINKS)) return 0;
        int deleted = 0;
        try (var owners = Files.newDirectoryStream(domain, path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))) {
            for (Path owner : owners) {
                String name = owner.getFileName().toString();
                if (!name.matches("[1-9][0-9]*")) continue;
                try {
                    int userId = Integer.parseInt(name);
                    deleted += service.deleteOlderThan(new Scope(userId, domainId, domainName, root), cutoff);
                } catch (RuntimeException exception) {
                    Logger.error(AiChatCleanup.class, "Chat retention failed for owner " + name + " in domain " + domainName, exception);
                }
            }
        }
        return deleted;
    }
}
