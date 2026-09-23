package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import sk.iway.iwcm.admin.layout.LayoutBean;

/** Verifies that heatmap administration templates render through the server-side template engine. */
class HeatMapTemplateTest {
    /** JavaScript ordering arrays must survive HTML inlining alongside translated labels and column metadata. */
    @Test
    void pageListRendersJavaScriptOrderingAndTranslations() throws Exception {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage("stat_menu.heat_map", Locale.ENGLISH, "Click map");
        messages.addMessage("components.summary.total_title", Locale.ENGLISH, "Total");
        messages.addMessage("components.stat.heatmap.listHelp", Locale.ENGLISH, "Select a page");
        engine.setMessageSource(messages);
        LayoutBean layout = mock(LayoutBean.class);
        when(layout.getDataTableColumns("sk.iway.iwcm.stat.jpa.HeatMapPageDTO"))
                .thenReturn("[{\"data\":\"name\"},{\"data\":\"clicks\"}]");
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("layout", layout);

        String rendered = engine.process(Files.readString(Path.of("src/main/webapp/apps/stat/admin/heat-map.html")), context);

        assertTrue(rendered.contains("title: \"Click map\""));
        assertTrue(rendered.contains("title: \"Total\""));
        assertTrue(rendered.contains("const heatMapColumns = [{\"data\":\"name\"},{\"data\":\"clicks\"}];"));
        assertTrue(rendered.contains("heatMapColumns.findIndex(column => column.data === \"clicks\")"));
    }
}
