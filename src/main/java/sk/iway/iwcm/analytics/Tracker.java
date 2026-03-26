package sk.iway.iwcm.analytics;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Interface pre implementaciu vlastnej trackovacej triedy
 */
public interface Tracker {
    void track(String path, HttpServletRequest request) throws Exception;
}
