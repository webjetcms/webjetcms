package sk.iway.iwcm.admin.layout;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies environment configuration rules and foreground selection for custom colors. */
class EnvironmentBadgeTest extends BaseWebjetTest {
    private final Map<String, String> original = new HashMap<>();
    private MockedStatic<SetCharacterEncodingFilter> requests;

    @BeforeEach
    void setUp() {
        for (String key : new String[] { "Name", "Description", "Icon", "Color", "Style" }) {
            String name = "dashboardEnvironment" + key;
            original.put(name, Constants.getString(name));
            Constants.setString(name, "Description".equals(key) ? "" : "auto");
        }
        Constants.setString("dashboardEnvironmentName", "{ENVIRONMENT_NAME}");
        RequestBean request = new RequestBean();
        request.setServerName("iwcm.interway.sk");
        request.setDomain("production.example.com");
        requests = mockStatic(SetCharacterEncodingFilter.class);
        requests.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(request);
    }

    @AfterEach
    void tearDown() {
        requests.close();
        original.forEach(Constants::setString);
    }

    /** Production defaults to strong red; custom labels retain the detected server identity. */
    @Test
    void automaticStyleAndCustomLabels() {
        Constants.setString("dashboardEnvironmentName", "PROD/node-1");
        assertEquals("#C4001F", new EnvironmentBadge().getBackground());
        Constants.setString("dashboardEnvironmentName", "autotest custom environment");
        EnvironmentBadge badge = new EnvironmentBadge();
        assertEquals("AUTOTEST", badge.getName());
        assertEquals("#CFF5E4", badge.getBackground());
        assertEquals("autotest custom environment", badge.getDescription());
        Constants.setString("dashboardEnvironmentDescription", "Server {ENVIRONMENT_NAME}");
        assertEquals("autotest custom environment – Server DEV", new EnvironmentBadge().getDescription());
    }

    /** Normalizes Unicode labels and trailing slashes, preserving the empty-name opt-out. */
    @Test
    void nameNormalizationAndVisibility() {
        Constants.setString("dashboardEnvironmentName", "  dev/  ");
        assertEquals("DEV", new EnvironmentBadge().getName());
        Constants.setString("dashboardEnvironmentName", "českýserver");
        assertEquals("ČESKÝSER", new EnvironmentBadge().getName());
        Constants.setString("dashboardEnvironmentName", "  ");
        assertFalse(new EnvironmentBadge().isVisible());
    }

    /** Checks known foreground choices, including adjacent grays at the black/white crossover. */
    @ParameterizedTest
    @CsvSource({
        "'#fff', '#000000'", "'#000', '#FFFFFF'",
        "'#757575', '#FFFFFF'", "'#767676', '#000000'",
        "'#ff0000', '#000000'", "'#00ff00', '#000000'", "'#0000ff', '#FFFFFF'"
    })
    void customColorContrast(String color, String expectedForeground) {
        Constants.setString("dashboardEnvironmentColor", color);
        EnvironmentBadge badge = new EnvironmentBadge();
        assertEquals(color, badge.getBackground());
        assertEquals(expectedForeground, badge.getForeground());
    }

    /** Rejects invalid colors and passes configured icon names through without checking the font. */
    @Test
    void validatesAppearance() {
        Constants.setString("dashboardEnvironmentColor", "red;display:none");
        assertEquals("#CFF5E4", new EnvironmentBadge().getBackground());
        Constants.setString("dashboardEnvironmentIcon", "rocket");
        assertEquals("ti-rocket", new EnvironmentBadge().getIcon());
        Constants.setString("dashboardEnvironmentIcon", "ti-autotest-missing");
        assertEquals("ti-autotest-missing", new EnvironmentBadge().getIcon());
        Constants.setString("dashboardEnvironmentIcon", "none");
        assertEquals("", new EnvironmentBadge().getIcon());
    }
}
