package sk.iway.iwcm.doc;

import javax.servlet.http.HttpServletRequest;

/**
 * Interface for custom navbar implementations.
 * Clients can implement this interface to provide custom navbar formatting.
 * The implementation class name should be set in Constants.getString("navbarDefaultType").
 */
public interface NavbarInterface {

    /**
     * Generates navbar (breadcrumb navigation)
     * @param groupId - ID of the directory/group
     * @param docId - ID of current web page
     * @param request - HTTP request
     * @return HTML code for breadcrumb navigation
     */
    String getNavbar(int groupId, int docId, HttpServletRequest request);
}
