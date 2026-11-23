package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import sk.iway.iwcm.admin.layout.LayoutBean;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies hostname detection and configuration macro expansion independently of the selected domain. */
class ConstantsEnvironmentTest extends BaseWebjetTest {
    private MockedStatic<SetCharacterEncodingFilter> requests;
    private RequestBean request;
    private String originalNodeName;
    private String originalEnvironmentName;

    @BeforeEach
    void setUp() {
        originalNodeName = Constants.getString("clusterMyNodeName");
        originalEnvironmentName = Constants.getString("dashboardEnvironmentName");
        request = new RequestBean();
        request.setDomain("prod.example.com");
        requests = mockStatic(SetCharacterEncodingFilter.class);
        requests.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(request);
    }

    @AfterEach
    void tearDown() {
        requests.close();
        Constants.setString("clusterMyNodeName", originalNodeName);
        Constants.setString("dashboardEnvironmentName", originalEnvironmentName);
    }

    /** Checks aliases, token boundaries, numeric suffixes and environment precedence. */
    @ParameterizedTest
    @CsvSource({
        "prod.example.com, PROD", "web-prod-02.example.com, PROD", "prd01.example.com, PROD",
        "web-production.example.com, PROD", "live.example.com, PROD", "PROD01.EXAMPLE.COM, PROD",
        "web-uat.example.com, UAT", "uat-web.example.com, UAT", "web-aut-02.example.com, UAT",
        "uat01.example.com, UAT", "web_UAT_02.example.com, UAT", "acc.example.com, UAT",
        "acceptance.example.com, UAT", "stage.example.com, UAT", "staging.example.com, UAT",
        "test.example.com, TEST", "testing.example.com, TEST", "qa01.example.com, TEST",
        "preprod.example.com, UAT", "preproduction.example.com, UAT", "pre-prod.example.com, UAT",
        "web-pre-production-01.example.com, UAT", "pre_prod.example.com, UAT", "pre.prod.example.com, UAT",
        "int.example.com, INT", "web-int-01.example.com, INT", "integration.example.com, INT", "sit01.example.com, INT",
        "dev-uat-int-prod.example.com, PROD", "dev-int-uat.example.com, UAT", "dev-int.example.com, INT",
        "prod-pre-prod.example.com, PROD", "dev.example.com, DEV", "localhost, LOCAL",
        "127.0.0.1, LOCAL", "iwcm.interway.sk, DEV", "product.example.com, DEV", "livechat.example.com, DEV",
        "cit.example.com, CIT", "web-cit02.example.com, CIT", "demo.example.com, DEMO",
        "web-demo-01.example.com, DEMO", "web-local.example.com, LOCAL", "localhost.localdomain, LOCAL",
        "127.0.1.1, LOCAL", "::1, LOCAL", "[::1], LOCAL", "0:0:0:0:0:0:0:1, LOCAL",
        "cit-int-test-demo-local.example.com, CIT", "prod-cit-demo.example.com, PROD",
        "uat-cit.example.com, UAT", "test-demo.example.com, TEST", "192.168.1.10, DEV",
        "city.example.com, DEV", "demography.example.com, DEV", "localization.example.com, DEV",
        "contest.example.com, DEV", "situation.example.com, DEV", "productional.example.com, DEV", "'', DEV"
    })
    void detectsEnvironmentFromServerName(String serverName, String expected) {
        request.setServerName(serverName);
        assertEquals(expected, Constants.getEnvironmentName());
        assertEquals(expected, new LayoutBean().getEnvironmentName());
    }

    /** Uses DEV for background work and uninitialized request hostnames. */
    @Test
    void missingRequestOrServerNameUsesDev() {
        assertEquals("DEV", Constants.getEnvironmentName());
        requests.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(null);
        assertEquals("DEV", Constants.getEnvironmentName());
        assertEquals("DEV", Constants.executeMacro("{ENVIRONMENT_NAME}"));
    }

    /** Expands the environment and the current node through the dashboard template's accessor. */
    @Test
    void expandsEnvironmentAndCurrentNodeWithoutUsingDomain() {
        request.setServerName("iwcm.interway.sk");
        Constants.setString("clusterMyNodeName", "LubosBalatProM5");
        Constants.setString("dashboardEnvironmentName", "{ENVIRONMENT_NAME}/{CLUSTER_NAME}");
        assertEquals("DEV/LubosBalatProM5", new LayoutBean().getConstantExecuteMacro("dashboardEnvironmentName"));
        request.setServerName("web-uat.example.com");
        assertEquals("UAT/LubosBalatProM5", new LayoutBean().getConstantExecuteMacro("dashboardEnvironmentName"));
    }

    /** Keeps slash cleanup confined to the badge so existing path and URL macros retain their trailing slash. */
    @Test
    void emptyNodeAndLiteralConfigurationRemainValid() {
        Constants.setString("clusterMyNodeName", "");
        assertEquals("DEV/", Constants.executeMacro("{ENVIRONMENT_NAME}/{CLUSTER_NAME}"));
        assertEquals("/uploads/", Constants.executeMacro("/uploads/"));
        Constants.setString("dashboardEnvironmentName", "Custom environment");
        assertEquals("Custom environment", new LayoutBean().getConstantExecuteMacro("dashboardEnvironmentName"));
        Constants.setString("dashboardEnvironmentName", "");
        assertEquals("", new LayoutBean().getConstantExecuteMacro("dashboardEnvironmentName"));
    }
}
