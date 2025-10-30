package sk.iway.iwcm.doc;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.servlet.http.HttpServletRequest;

/**
 * Test for NavbarService and custom NavbarInterface implementations
 */
class NavbarServiceTest {

    /**
     * Custom test implementation of NavbarInterface
     */
    public static class CustomNavbar implements NavbarInterface {
        
        @Override
        public String getNavbarRDF(int groupId, int docId, HttpServletRequest request) {
            return "<div class=\"custom-rdf\">Custom RDF navbar for group " + groupId + " doc " + docId + "</div>";
        }

        @Override
        public String getNavbarSchema(int groupId, int docId, HttpServletRequest request) {
            return "<ol class=\"custom-schema\">Custom Schema navbar for group " + groupId + " doc " + docId + "</ol>";
        }

        @Override
        public String getNavbar(int groupId, int docId, HttpServletRequest request) {
            return "Custom navbar for group " + groupId + " doc " + docId;
        }
    }

    @Test
    void testStandardNavbarService() {
        NavbarService navbarService = new NavbarService();
        
        // Test that NavbarService implements NavbarInterface
        assertNotNull(navbarService);
        
        // Verify it's an instance of NavbarInterface
        NavbarInterface navbarInterface = navbarService;
        assertNotNull(navbarInterface);
    }

    @Test
    void testCustomNavbarImplementation() {
        CustomNavbar customNavbar = new CustomNavbar();
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Test RDF format
        String rdfNavbar = customNavbar.getNavbarRDF(10, 20, request);
        assertEquals("<div class=\"custom-rdf\">Custom RDF navbar for group 10 doc 20</div>", rdfNavbar);

        // Test Schema.org format
        String schemaNavbar = customNavbar.getNavbarSchema(10, 20, request);
        assertEquals("<ol class=\"custom-schema\">Custom Schema navbar for group 10 doc 20</ol>", schemaNavbar);

        // Test standard format
        String standardNavbar = customNavbar.getNavbar(10, 20, request);
        assertEquals("Custom navbar for group 10 doc 20", standardNavbar);
    }

    @Test
    void testNavbarInterfaceContract() {
        // Verify that NavbarService implements NavbarInterface
        NavbarService navbarService = new NavbarService();
        NavbarInterface navbarInterface = navbarService;
        assertNotNull(navbarInterface);
        
        // Verify that custom implementation implements NavbarInterface
        CustomNavbar customNavbar = new CustomNavbar();
        NavbarInterface customInterface = customNavbar;
        assertNotNull(customInterface);
    }
}
