package sk.iway.iwcm.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.io.IwcmFile;

/** Verifies that conditional downloads do not trigger the servlet error-page mechanism. */
class MultipartFileSenderTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void notModifiedResponseDoesNotCommitAnError(boolean useEtag) throws Exception {
        IwcmFile file = mock(IwcmFile.class);
        when(file.getPath()).thenReturn("/tmp/backport-cache-test.txt");
        when(file.exists()).thenReturn(true);
        when(file.getName()).thenReturn("backport-cache-test.txt");
        when(file.length()).thenReturn(16L);
        when(file.lastModified()).thenReturn(1_700_000_000_000L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (useEtag) {
            request.addHeader("If-None-Match", "backport-cache-test.txt");
        } else {
            request.addHeader("If-Modified-Since", 1_700_000_000_000L);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<Constants> constants = mockStatic(Constants.class)) {
            constants.when(Constants::getServletContext).thenReturn(new MockServletContext());

            MultipartFileSender.fromFile(file).with(request).with(response).serveResource();

            assertEquals(304, response.getStatus());
            assertEquals("backport-cache-test.txt", response.getHeader("ETag"));
            assertEquals(0, response.getContentAsByteArray().length);
            assertFalse(response.isCommitted());
        }
    }
}
