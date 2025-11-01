package sk.iway.iwcm.doc;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

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
        public String getNavbar(int groupId, int docId, HttpServletRequest request) {
            return "Custom navbar for group " + groupId + " doc " + docId;
        }
    }

    @Test
    void testStandardNavbarService() {
        NavbarService navbarService = new NavbarService();
        
        // Test that NavbarService can be instantiated
        assertNotNull(navbarService);
    }

    @Test
    void testCustomNavbarImplementation() {
        CustomNavbar customNavbar = new CustomNavbar();
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Test standard format
        String standardNavbar = customNavbar.getNavbar(10, 20, request);
        assertEquals("Custom navbar for group 10 doc 20", standardNavbar);
    }

    @Test
    void testNavbarInterfaceContract() {
        // Verify that custom implementation implements NavbarInterface
        CustomNavbar customNavbar = new CustomNavbar();
        NavbarInterface navbarInterface = customNavbar;
        assertNotNull(navbarInterface);
    }

    @Test
    void testBuiltInImplementations() {
        // Test that built-in implementations can be instantiated
        NavbarStandard standard = new NavbarStandard();
        assertNotNull(standard);
        
        NavbarRDF rdf = new NavbarRDF();
        assertNotNull(rdf);
        
        NavbarSchemaOrg schemaOrg = new NavbarSchemaOrg();
        assertNotNull(schemaOrg);
        
        // Verify they implement the interface
        NavbarInterface standardInterface = standard;
        NavbarInterface rdfInterface = rdf;
        NavbarInterface schemaOrgInterface = schemaOrg;
        
        assertNotNull(standardInterface);
        assertNotNull(rdfInterface);
        assertNotNull(schemaOrgInterface);
    }
}
