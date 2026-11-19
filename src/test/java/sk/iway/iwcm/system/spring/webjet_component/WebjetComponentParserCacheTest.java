package sk.iway.iwcm.system.spring.webjet_component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.components.WebjetComponentInterface;
import sk.iway.iwcm.test.BaseWebjetTest;

class WebjetComponentParserCacheTest extends BaseWebjetTest {

    @Test
    void previewRendersComponentInsteadOfCachedWriteTagPlaceholder() throws Exception {
        String include = "!INCLUDE(sk.iway.demo8.DemoComponent, cacheMinutes=&quot;5&quot;, testId=&quot;"
                + UUID.randomUUID() + "&quot;)!";
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        WebjetComponentInterface component = mock(WebjetComponentInterface.class);
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.containsBean("sk.iway.demo8.DemoComponent")).thenReturn(true);
        when(context.getBean("sk.iway.demo8.DemoComponent", WebjetComponentInterface.class)).thenReturn(component);
        WebjetComponentResolver resolver = mock(WebjetComponentResolver.class);
        when(resolver.render(eq(request), eq(response), eq(component), any(PageParams.class)))
                .thenReturn("<p>Rendered app</p>");

        WebjetComponentParser parser = new WebjetComponentParser();
        ReflectionTestUtils.setField(parser, "context", context);
        ReflectionTestUtils.setField(parser, "componentResolver", resolver);

        // WriteTag stores its temporary placeholder under this same key.
        String cacheKey = "writeTag_" + include.substring(0, include.length() - 2)
                + " ;" + PageLng.getUserLng(request);
        Cache cache = Cache.getInstance();
        String placeholder = "<!-- caching data, try reload -->";
        cache.setObjectSeconds(cacheKey, placeholder, 120, false);
        try {
            assertEquals(placeholder, parser.parse(request, response, include));
            verify(resolver, never()).render(eq(request), eq(response), eq(component), any(PageParams.class));

            request.setAttribute("isPreview", Boolean.TRUE);
            assertEquals("<p>Rendered app</p>", parser.parse(request, response, include));
            verify(resolver).render(eq(request), eq(response), eq(component), any(PageParams.class));
            assertEquals(placeholder, cache.getObject(cacheKey));
        } finally {
            cache.removeObject(cacheKey);
        }
    }
}
