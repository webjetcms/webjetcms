package sk.iway.iwcm.system;

import sk.iway.iwcm.Adminlog;

public class UrlRedirectDeleteOldRedirectsCronJob {
    public static void main(String[] args)
    {
        Adminlog.add(Adminlog.TYPE_CRON, "UrlRedirectDeleteOldRedirectsCronJob started.", -1, -1);
        int affected = UrlRedirectDB.deleteOldRedirects();
        Adminlog.add(Adminlog.TYPE_CRON, "UrlRedirectDeleteOldRedirectsCronJob is done with " + affected + " urls affected.", -1, -1);
    }
}
