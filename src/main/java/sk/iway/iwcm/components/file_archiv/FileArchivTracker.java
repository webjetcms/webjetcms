package sk.iway.iwcm.components.file_archiv;

import org.apache.http.client.utils.URIBuilder;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.analytics.Tracker;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Trackovanie stahovania subrov do GA z file archivu
 */
public class FileArchivTracker implements Tracker {

    /**
     * Implemetacia triedy tack - Vyplni potrebne parametre a vykona request na GA
     * @param path cesta k suboru
     * @param request HttpServletRequest
     */
    public void track(String path, HttpServletRequest request) throws Exception {

        // Load configuration
        String trackingId = Constants.getString("analyticsTrackingId");
        String trackingVersion = Constants.getString("analyticsTrackingVersion");

        // Tracking id validation
        if(Tools.isEmpty(trackingId)) {
            throw new Exception("FileArchivTracker analyticsTrackingId is empty");
        }

        // Cookies to identify user
        String cid = Tools.getCookieValue(request.getCookies(), "_ga", "").replace(trackingVersion + ".", "");
        String gid = Tools.getCookieValue(request.getCookies(), "_gid", "").replace(trackingVersion + ".", "");

        // requested file
        FileArchivatorBean file = FileArchivatorDB.getByUrl(path);

        if(Tools.isEmpty(cid)) {
            cid = "unknownClient";
        }

        if(Tools.isEmpty(gid)) {
            gid = "unknownGroup";
        }

        // assemble url
        if(file != null) {
            URIBuilder builder = new URIBuilder();
            builder
                    .setScheme("http")
                    .setHost("www.google-analytics.com")
                    .setPath("/collect")
                    .addParameter("v", "1") // API Version.
                    .addParameter("tid", trackingId) // Tracking ID / Property ID.
                    .addParameter("cid", cid) // Anonymous Client Identifier. Ideally, this should be a UUID that is associated with particular user, device, or browser instance.
                    .addParameter("_gid", gid) // Anonymous Client Identifier. Ideally, this should be a UUID that is associated with particular user, device, or browser instance.
                    .addParameter("t", "event") // Event hit type.
                    .addParameter("ec", "Stiahnuté dokumenty") // Event category.
                    .addParameter("ea", file.getVirtualFileName()) // Event action.
                    .addParameter("el", path)  // Event url.
                    .addParameter("dl", request.getHeader("referer")) // referer url.
                    .addParameter("dt", request.getHeader("referer")); // referer title.
            URI uri;

            // build url
            try {
                uri = builder.build();
            } catch (URISyntaxException e) {
                throw new ServletException("FileArchivTracker Problem building URI", e);
            }

            // track event
            Tools.downloadUrl(uri.toURL().toString());
        }

    }
}
