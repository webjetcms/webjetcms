package sk.iway.iwcm.rag.service;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.system.multidomain.DomainRequestBeanScope;

/** Scheduled shared Markdown scan with optional folder and accounting-domain parameters. */
public class MarkdownIndexCronTask {

    private MarkdownIndexCronTask() {}

    /**
     * Runs a shared Markdown scan in the supplied accounting domain and logs failures.
     *
     * @param args optional configured root followed by an accounting domain name; the domain is required in cloud or MultiWeb mode
     */
    public static void main(String[] args) {
        try {
            if (args != null && args.length > 2) {
                throw new IllegalArgumentException("Markdown indexing accepts a documentation folder and an accounting domain");
            }
            String folder = args != null && args.length > 0 ? args[0] : null;
            String domainName = args != null && args.length > 1 && args[1].isBlank() == false ? args[1].trim() : null;
            boolean multiDomain = InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir");
            // Cron threads have no requesting domain; never silently charge shared indexing to a default tenant.
            if (multiDomain && (domainName == null || GroupsDB.getDomainId(domainName) < 1)) {
                throw new IllegalArgumentException("Markdown indexing requires a valid accounting domain in MultiWeb/cloud mode");
            }
            try (DomainRequestBeanScope ignored = DomainRequestBeanScope.open(domainName)) {
                MarkdownIndexService service = Tools.getSpringBean("markdownIndexService", MarkdownIndexService.class);
                if (service == null) throw new IllegalStateException("Markdown indexing service is unavailable");
                service.indexConfiguredRoots(folder);
            }
        } catch (Exception e) {
            Logger.error(MarkdownIndexCronTask.class, "Error indexing Markdown documentation: " + e.getMessage(), e);
        }
    }
}
