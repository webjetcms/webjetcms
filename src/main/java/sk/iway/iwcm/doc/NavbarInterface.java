package sk.iway.iwcm.doc;

import javax.servlet.http.HttpServletRequest;

/**
 * Interface for custom navbar implementations.
 * Clients can implement this interface to provide custom navbar formatting.
 * The implementation class name should be set in Constants.getString("navbarDefaultType").
 */
public interface NavbarInterface {

    /**
     * Generates navbar in RDF format
     * @param groupId - ID of the directory/group
     * @param docId - ID of current web page
     * @param request - HTTP request
     * @return HTML code for breadcrumb navigation in RDF format
     */
    String getNavbarRDF(int groupId, int docId, HttpServletRequest request);

    /**
     * Generates navbar in schema.org format
     * @param groupId - ID of the directory/group
     * @param docId - ID of current web page
     * @param request - HTTP request
     * @return HTML code for breadcrumb navigation in schema.org format
     */
    String getNavbarSchema(int groupId, int docId, HttpServletRequest request);

    /**
     * Generates navbar in standard format
     * @param groupId - ID of the directory/group
     * @param docId - ID of current web page
     * @param request - HTTP request
     * @return HTML code for breadcrumb navigation
     */
    String getNavbar(int groupId, int docId, HttpServletRequest request);
}
