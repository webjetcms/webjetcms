package sk.iway.iwcm.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import net.sourceforge.stripes.mock.MockHttpServletResponse;

class WJResponseWrapperTest {

    @Test
    void captureWrapperCollectsContentAndRedirectWithoutClientResponse() throws Exception {
        WJResponseWrapper response = WJResponseWrapper.forCapture(new MockHttpServletRequest());

        response.getWriter().write("rendered content");
        response.sendRedirect("/target");

        assertEquals("rendered content", response.getOutputOfStreamAsString());
        assertEquals("/target", response.getRedirectURL());
        assertInstanceOf(MockHttpServletResponse.class, response.origResponse);
    }
}
