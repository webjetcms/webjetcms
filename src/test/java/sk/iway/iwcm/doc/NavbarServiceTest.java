package sk.iway.iwcm.doc;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.aceintegration.CustomNavbar;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for NavbarService and custom NavbarInterface implementations
 */
class NavbarServiceTest extends BaseWebjetTest {

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

        SetCharacterEncodingFilter.registerDataContext(request);
        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
        rb.setDomain(System.getenv("CODECEPT_DEFAULT_DOMAIN_NAME"));

        // Test standard format
        String standardNavbar = customNavbar.getNavbar(10, 20, request);
        assertEquals("Custom navbar for group 10 doc 20", standardNavbar);

        //test using Constants value and NavbarService
        NavbarService navbarService = new NavbarService();
        DocDetails docDetails = new DocDetails();
        docDetails.setTitle("Test Doc");
        docDetails.setGroupId(1);
        docDetails.setDocId(4);
        Constants.setString("navbarDefaultType", "sk.iway.aceintegration.CustomNavbar");
        String serviceNavbar = navbarService.getNavbar(docDetails, request);
        assertEquals("Custom navbar for group "+docDetails.getGroupId()+" doc "+docDetails.getDocId(), serviceNavbar);

        // Verify default navbar
        Constants.setString("navbarDefaultType", "normal");
        serviceNavbar = navbarService.getNavbar(docDetails, request);
        //doc/group doesnt exist so navbar should be just doc title
        assertEquals("<a href='/lta-href=39amp4739gtjet-portalltamp47agt/'>Jet portal 4</a>", serviceNavbar);
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
