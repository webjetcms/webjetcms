package sk.iway.iwcm.components.domain_redirects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.domainRedirects.DomainRedirectBean;
import sk.iway.iwcm.database.SimpleQuery;

class DomainRedirectsControllerTest {

    private static final String CLOUD_DOMAIN = "www.domain-name.cz";

    private static final Object[][] DOMAIN_VARIANTS = {
        { "domain-name.cz", "www.domain-name.cz", true },
        { "any-domain.example", "http://www.domain-name.cz", true },
        { "another-domain.example", "https://www.domain-name.cz", true },
        { "domain-name.cz", "https://www.domain-name.cz/path?query=value", true },
        { "domain-name.cz", "https://www.domain-name.cz:8443/path", true },
        { "domain-name.cz", "HTTPS://WWW.DOMAIN-NAME.CZ/path", true },
        { "domain-name.cz", "https://domain-name.cz", false },
        { "domain-name.cz", "https://another-domain.cz", false },
        { "domain-name.cz", "another-domain.cz", false },
        { "domain-name.cz", "https://www.domain-name.cz.attacker.example", false },
        { "domain-name.cz", "https://attacker.example/www.domain-name.cz", false },
        { "domain-name.cz", null, false },
        { "domain-name.cz", "", false },
        { "domain-name.cz", "not a valid URL", false }
    };

    @ParameterizedTest(name = "redirectFrom={0}, redirectTo={1}, expected={2}")
    @MethodSource("domainVariants")
    void shouldValidateRedirectToHost(String redirectFrom, String redirectTo, boolean expected) {
        DomainRedirectBean entity = new DomainRedirectBean();
        entity.setRedirectFrom(redirectFrom);
        entity.setRedirectTo(redirectTo);

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            initServlet.when(InitServlet::isTypeCloud).thenReturn(true);
            cloudTools.when(CloudToolsForCore::getDomainName).thenReturn(CLOUD_DOMAIN);

            DomainRedirectsController controller = new DomainRedirectsController();

            assertEquals(expected, controller.beforeDelete(entity));
        }
    }

    @ParameterizedTest(name = "storedRedirectTo={0}, expected={1}")
    @MethodSource("storedDomainVariants")
    void shouldValidateStoredRedirectToHost(String storedRedirectTo, boolean expected) {
        DomainRedirectBean entity = new DomainRedirectBean();
        entity.setRedirectId(123);
        entity.setRedirectTo("https://" + CLOUD_DOMAIN);

        try (MockedStatic<InitServlet> initServlet = mockStatic(InitServlet.class);
                MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
                MockedConstruction<SimpleQuery> simpleQueries = mockConstruction(SimpleQuery.class,
                        (query, context) -> when(query.forString(
                                "SELECT redirect_to FROM domain_redirects WHERE redirect_id=?", 123))
                                .thenReturn(storedRedirectTo))) {
            initServlet.when(InitServlet::isTypeCloud).thenReturn(true);
            cloudTools.when(CloudToolsForCore::getDomainName).thenReturn(CLOUD_DOMAIN);

            DomainRedirectsController controller = new DomainRedirectsController();

            assertEquals(expected, controller.beforeDelete(entity));
            assertEquals(1, simpleQueries.constructed().size());
        }
    }

    private static Stream<Arguments> domainVariants() {
        return Stream.of(DOMAIN_VARIANTS).map(Arguments::of);
    }

    private static Stream<Arguments> storedDomainVariants() {
        return Stream.of(
                Arguments.of("https://" + CLOUD_DOMAIN, true),
                Arguments.of("https://another-domain.cz", false));
    }
}
