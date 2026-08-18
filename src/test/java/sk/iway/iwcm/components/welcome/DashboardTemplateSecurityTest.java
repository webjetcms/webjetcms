package sk.iway.iwcm.components.welcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;

class DashboardTemplateSecurityTest {

    @Test
    void dashboardBootstrapSafelyInlinesEveryJsonPayload() throws IOException {
        String template = Files.readString(
            Path.of("src/main/webapp/admin/v9/views/pages/dashboard/overview.pug")
        );

        assertTrue(template.contains("script(data-th-inline=\"javascript\")."));
        String[] modelAttributes = {
            "overviewBackData",
            "overviewAdmins",
            "overviewRecentPages",
            "overviewChangedPages",
            "overviewAdminlog",
            "overviewTodo",
            "overviewCurrentSessions"
        };
        for (String modelAttribute : modelAttributes) {
            assertTrue(template.contains("JSON.parse(/*[[${" + modelAttribute + "}]]*/"));
            assertFalse(template.contains("[(${" + modelAttribute + "})]"));
        }
    }

    @Test
    void javascriptInliningEscapesScriptTerminatorsAndLineSeparators() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        Context context = new Context();
        context.setVariable(
            "overviewRecentPages",
            "[{\"title\":\"</script><script>globalThis.dashboardXss=true</script>\",\"separators\":\"\u2028\u2029\"}]"
        );

        String rendered = templateEngine.process(
            "<script data-th-inline=\"javascript\">" +
                "window.dashboardData=JSON.parse(/*[[${overviewRecentPages}]]*/ \"[]\");" +
            "</script>",
            context
        );

        int closingScriptIndex = rendered.indexOf("</script>");
        assertTrue(closingScriptIndex > -1);
        assertEquals(closingScriptIndex, rendered.lastIndexOf("</script>"));
        assertTrue(rendered.contains("<\\/script>"));
        assertTrue(rendered.contains("globalThis.dashboardXss=true"));
        assertFalse(rendered.contains("\u2028"));
        assertFalse(rendered.contains("\u2029"));
        assertTrue(rendered.contains("\\u2028"));
        assertTrue(rendered.contains("\\u2029"));
    }
}
