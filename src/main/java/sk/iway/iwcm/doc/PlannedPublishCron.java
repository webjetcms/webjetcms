package sk.iway.iwcm.doc;

/** Existing WebJET cron entry point; does not depend on editor sessions or public traffic. */
public final class PlannedPublishCron {
    private PlannedPublishCron() { }

    public static void main(String[] args) {
        if (!PlannedAccess.enabled()) return;
        DocPublishService publisher = new DocPublishService();
        publisher.refreshPagesToPublish();
        publisher.checkWebpagesToPublish(DocDB.getInstance());
    }
}
