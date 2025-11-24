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

    @Override
    public String getNavbarForNonDefaultDoc(sk.iway.iwcm.doc.DocDetails docDetails, String navbar, HttpServletRequest request) {
        return navbar + ", Custom navbar for non-default doc " + docDetails.getDocId();
    }

}
