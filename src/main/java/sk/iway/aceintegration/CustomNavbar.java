package sk.iway.aceintegration;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.doc.NavbarInterface;

/**
 * Custom navbar implementation for testing purposes.
 */
public class CustomNavbar implements NavbarInterface {

    @Override
    public String getNavbar(int groupId, int docId, HttpServletRequest request) {
        return "Custom navbar for group " + groupId + " doc " + docId;
    }
}
