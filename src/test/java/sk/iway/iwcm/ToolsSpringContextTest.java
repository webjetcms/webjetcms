package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockServletContext;

import sk.iway.iwcm.components.file_archiv.FileArchiveRepository;

/**
 * Verifies Spring bean lookup for HTTP requests and background jobs without a request Spring context.
 */
class ToolsSpringContextTest {

    /**
     * Resolves the archive repository from the application context both without a request bean
     * and with an empty request bean, as created by the scheduled archive publisher.
     *
     * @param hasRequestBean whether the background job has registered an empty request bean
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void resolvesArchiveRepositoryWithoutRequestSpringContext(boolean hasRequestBean) {
        FileArchiveRepository repository = mock(FileArchiveRepository.class);
        MockServletContext servletContext = new MockServletContext();

        try (StaticApplicationContext applicationContext = new StaticApplicationContext();
                MockedStatic<Constants> constants = mockStatic(Constants.class, CALLS_REAL_METHODS);
                MockedStatic<SetCharacterEncodingFilter> requests = mockStatic(SetCharacterEncodingFilter.class)) {
            applicationContext.getBeanFactory().registerSingleton("fileArchiveRepository", repository);
            servletContext.setAttribute("springContext", applicationContext);
            constants.when(Constants::getServletContext).thenReturn(servletContext);
            requests.when(SetCharacterEncodingFilter::getCurrentRequestBean)
                .thenReturn(hasRequestBean ? new RequestBean() : null);

            assertSame(applicationContext, Tools.getSpringContext());
            assertSame(repository, Tools.getSpringBean("fileArchiveRepository", FileArchiveRepository.class));
            assertNull(Tools.getSpringBean("missingRepository", FileArchiveRepository.class));
        }
    }

    /**
     * Preserves the context associated with the current HTTP request when one is available.
     */
    @Test
    void prefersRequestSpringContext() {
        ApplicationContext requestContext = mock(ApplicationContext.class);
        RequestBean requestBean = new RequestBean();
        requestBean.setSpringContext(requestContext);

        try (MockedStatic<Constants> constants = mockStatic(Constants.class, CALLS_REAL_METHODS);
                MockedStatic<SetCharacterEncodingFilter> requests = mockStatic(SetCharacterEncodingFilter.class)) {
            requests.when(SetCharacterEncodingFilter::getCurrentRequestBean).thenReturn(requestBean);

            assertSame(requestContext, Tools.getSpringContext());
            constants.verify(Constants::getServletContext, never());
        }
    }
}
