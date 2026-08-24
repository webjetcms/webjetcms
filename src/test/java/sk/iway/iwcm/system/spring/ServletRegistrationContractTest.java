package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.filter.CharacterEncodingFilter;

import net.sourceforge.stripes.controller.DispatcherServlet;
import net.sourceforge.stripes.controller.StripesFilterIway;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.doc.GetProtectedFileServlet;
import sk.iway.iwcm.system.context.ContextFilter;

class ServletRegistrationContractTest {

    private final SpringBootStarter.ProductionServletConfiguration productionConfiguration =
        new SpringBootStarter.ProductionServletConfiguration();
    private final SpringBootStarter.ProductionServletInfrastructureConfiguration
        productionInfrastructureConfiguration =
            new SpringBootStarter.ProductionServletInfrastructureConfiguration();

    @Test
    void setupSessionCleanupRunsBeforeConfiguredSecurityFilter() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.security.filter.order", "37");

        assertFilterRegistration(
            productionInfrastructureConfiguration
                .persistedSetupAuthenticationCleanupFilterRegistration(environment),
            PersistedSetupAuthenticationCleanupFilter.class,
            "persistedSetupAuthenticationCleanupFilter",
            36,
            Set.of("/*"),
            Set.of(),
            EnumSet.allOf(DispatcherType.class)
        );
    }

    @Test
    void setupSessionCleanupFailsWhenSecurityHasNoPrecedingOrder() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty(
                "spring.security.filter.order",
                Integer.toString(Ordered.HIGHEST_PRECEDENCE)
            );

        assertThrows(IllegalStateException.class,
            () -> productionInfrastructureConfiguration
                .persistedSetupAuthenticationCleanupFilterRegistration(environment));
    }

    @Test
    void setupSessionCleanupUsesSpringBootSecurityDefaultOrder() {
        FilterRegistrationBean<?> registration = productionInfrastructureConfiguration
            .persistedSetupAuthenticationCleanupFilterRegistration(new MockEnvironment());

        assertEquals(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1, registration.getOrder());
    }

    @Test
    void embeddedFiltersKeepTheirRegistrationContract() {
        assertFilterRegistration(productionConfiguration.characterEncodingFilterRegistration(
                new CharacterEncodingFilter()),
            org.springframework.web.filter.CharacterEncodingFilter.class, "SpringEncodingFilter", 0,
            Set.of("/*"), Set.of(), EnumSet.allOf(DispatcherType.class));
        assertFilterRegistration(productionConfiguration.contextFilterRegistration(),
            ContextFilter.class, "ContextFilter", 1, Set.of("/*"), Set.of(),
            EnumSet.of(DispatcherType.REQUEST));
        assertFilterRegistration(productionConfiguration.stripesFilterRegistration(),
            StripesFilterIway.class, "StripesFilter", 3, Set.of("/*"),
            Set.of("StripesDispatcher"), EnumSet.of(DispatcherType.REQUEST));
        assertFilterRegistration(productionConfiguration.virtualPathFilterRegistration(),
            PathFilter.class, "Virtual Path Filter", 4, Set.of("/*"), Set.of(),
            EnumSet.of(DispatcherType.REQUEST));
    }

    @Test
    void embeddedLifecycleRegistrationsReplaceTheDeploymentDescriptor() throws ServletException {
        ServletListenerRegistrationBean<sk.iway.iwcm.stat.SessionListener> sessionListener =
            productionConfiguration.sessionListenerRegistration();
        assertInstanceOf(sk.iway.iwcm.stat.SessionListener.class, sessionListener.getListener());

        ServletRegistrationBean<?> iwcmInit = productionConfiguration.iwcmInitServletRegistration();
        assertServletRegistration(iwcmInit, sk.iway.iwcm.InitServlet.class, "iwcminit");

        ServletContext servletContext = mock(ServletContext.class);
        ServletRegistration.Dynamic dynamicRegistration = mock(ServletRegistration.Dynamic.class);
        when(servletContext.addServlet("iwcminit", iwcmInit.getServlet())).thenReturn(dynamicRegistration);
        iwcmInit.onStartup(servletContext);

        verify(dynamicRegistration).setLoadOnStartup(1);
        verify(dynamicRegistration, never()).addMapping(any(String[].class));
    }

    @Test
    void coreInitializationRunsBeforeEmbeddedServletRegistration() {
        ServletContextInitializer coreInitializer = new SpringAppInitializer().springAppInitializerOnStartup(
            WebjetBootstrapState.pending(WebjetBootstrapMode.PRODUCTION),
            mock(WebjetInitializationActions.class),
            mock(ApplicationContext.class)
        );
        ServletRegistrationBean<?> iwcmInit = productionConfiguration.iwcmInitServletRegistration();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("iwcmInitServletRegistration", iwcmInit);
        beanFactory.registerSingleton("springAppInitializerOnStartup", coreInitializer);

        ServletContextInitializer firstInitializer =
            new ServletContextInitializerBeans(beanFactory).iterator().next();

        assertSame(coreInitializer, firstInitializer);
        Ordered orderedInitializer = assertInstanceOf(Ordered.class, coreInitializer);
        assertEquals(Ordered.HIGHEST_PRECEDENCE, orderedInitializer.getOrder());
    }

    @Test
    void updaterDescriptorSkipsTheSpringBootApplication() throws ServletException {
        ServletContext servletContext = mock(ServletContext.class);
        ServletRegistration updaterRegistration = mock(ServletRegistration.class);
        when(servletContext.getServletRegistration("updaterinit")).thenReturn(updaterRegistration);
        when(updaterRegistration.getClassName()).thenReturn("sk.updater.InitServlet");

        new SpringBootStarter().onStartup(servletContext);

        verify(servletContext).getServletRegistration("updaterinit");
        verify(updaterRegistration).getClassName();
        verifyNoMoreInteractions(servletContext, updaterRegistration);
    }

    @Test
    void embeddedServletsKeepTheirNamesAndMappings() throws ServletException {
        MultipartConfigElement multipartConfig = new MultipartConfigElement(
            null, 37_000_000L, 37_000_000L, 65_536
        );

        assertServletRegistration(productionConfiguration.getProtectedFileServletRegistration(),
            GetProtectedFileServlet.class, "GetProtectedFile", "/files/protected/*");
        ServletRegistrationBean<?> stripesDispatcher = productionConfiguration.stripesDispatcherRegistration();
        assertServletRegistration(stripesDispatcher, DispatcherServlet.class, "StripesDispatcher", "*.action");
        ServletContext servletContext = mock(ServletContext.class);
        ServletRegistration.Dynamic dynamicRegistration = mock(ServletRegistration.Dynamic.class);
        when(servletContext.addServlet("StripesDispatcher", stripesDispatcher.getServlet()))
            .thenReturn(dynamicRegistration);
        stripesDispatcher.onStartup(servletContext);
        verify(dynamicRegistration).setLoadOnStartup(1);

        assertServletRegistration(productionConfiguration.showDocServletRegistration(),
            sk.iway.iwcm.doc.ShowDoc.class, "ShowDoc", "/showdoc.do");
        assertServletRegistration(productionConfiguration.previewServletRegistration(),
            sk.iway.iwcm.editor.PreviewServlet.class, "previewServlet", "/preview.do");
        assertServletRegistration(productionConfiguration.formMailServletRegistration(),
            sk.iway.iwcm.form.FormMailActionServlet.class, "FormMailAction", "/formmail.do");
        assertServletRegistration(productionConfiguration.offlineServletRegistration(),
            sk.iway.iwcm.components.offline.OfflineAction.class, "offlineServlet", "/admin/offline.do");
        assertServletRegistration(productionConfiguration.deleteServletRegistration(),
            sk.iway.iwcm.doc.DeleteServlet.class, "DelDoc", "/admin/docdel.do");
        assertServletRegistration(productionConfiguration.logoffServletRegistration(),
            sk.iway.iwcm.LogoffServlet.class, "LogOff", "/logoff.do", "/admin/logoff.do");
        ServletRegistrationBean<?> multipleFileUpload =
            productionConfiguration.multipleFileUploadServletRegistration(multipartConfig);
        assertServletRegistration(multipleFileUpload,
            sk.iway.iwcm.filebrowser.MultipleFileUploadAction.class, "MultipleFileUploadAction",
            "/admin/multiplefileupload.do");
        assertSame(multipartConfig, multipleFileUpload.getMultipartConfig());
        assertServletRegistration(productionConfiguration.thumbServletRegistration(),
            sk.iway.iwcm.editor.ThumbServlet.class, "thumbServlet",
            "/admin/thumb/*", "/thumb/*", "/tumbn/*");
        assertServletRegistration(productionConfiguration.captchaServletRegistration(),
            sk.iway.iwcm.system.captcha.CaptchaServlet.class, "captchaServlet", "/captcha.jpg");
        assertServletRegistration(productionConfiguration.elfinderServletRegistration(),
            sk.iway.iwcm.system.elfinder.ElfinderServlet.class, "elfinderServlet",
            "/admin/elfinder-connector/");
        assertServletRegistration(productionConfiguration.pdfServletRegistration(),
            sk.iway.iwcm.components.pdf.PdfServlet.class, "pdfServlet", "/to.pdf/*", "/topdf/*");

        ServletRegistrationBean<?> xhrUpload =
            productionConfiguration.xhrFileUploadServletRegistration(multipartConfig);
        assertServletRegistration(xhrUpload, sk.iway.iwcm.components.upload.XhrFileUploadServlet.class,
            "XhrFileUpload", "/XhrFileUpload");
        assertSame(multipartConfig, xhrUpload.getMultipartConfig());

        ServletRegistrationBean<?> adminUpload =
            productionConfiguration.adminUploadServletRegistration(multipartConfig);
        assertServletRegistration(adminUpload, sk.iway.iwcm.admin.upload.AdminUploadServlet.class,
            "AdminUpload", "/admin/upload/chunk");
        assertSame(multipartConfig, adminUpload.getMultipartConfig());

        assertServletRegistration(productionConfiguration.exportSyncServletRegistration(),
            sk.iway.iwcm.sync.export.ExportSyncServlet.class, "exportSyncServlet", "/export.sync");
    }

    @Test
    void externalWarMultipartInitializerConfiguresContainerOwnedUploadServlets()
            throws ServletException {
        MultipartConfigElement multipartConfig = new MultipartConfigElement(
            null, 39_000_000L, 39_000_000L, 65_536
        );
        ServletRegistration.Dynamic xhrUpload = mock(ServletRegistration.Dynamic.class);
        ServletRegistration.Dynamic adminUpload = mock(ServletRegistration.Dynamic.class);
        ServletRegistration.Dynamic multipleFileUpload = mock(ServletRegistration.Dynamic.class);
        ServletRegistration.Dynamic unrelatedServlet = mock(ServletRegistration.Dynamic.class);
        when(xhrUpload.getClassName()).thenReturn(
            sk.iway.iwcm.components.upload.XhrFileUploadServlet.class.getName());
        when(adminUpload.getClassName()).thenReturn(
            sk.iway.iwcm.admin.upload.AdminUploadServlet.class.getName());
        when(multipleFileUpload.getClassName()).thenReturn(
            sk.iway.iwcm.filebrowser.MultipleFileUploadAction.class.getName());
        when(unrelatedServlet.getClassName()).thenReturn("com.example.UnrelatedServlet");

        ServletContext servletContext = mock(ServletContext.class);
        doReturn(Map.<String, ServletRegistration>of(
            "xhr", xhrUpload,
            "admin", adminUpload,
            "multiple", multipleFileUpload,
            "unrelated", unrelatedServlet
        )).when(servletContext).getServletRegistrations();
        ServletContextInitializer initializer =
            productionInfrastructureConfiguration.externalWarMultipartServletInitializer(multipartConfig);

        initializer.onStartup(servletContext);

        verify(xhrUpload).setMultipartConfig(multipartConfig);
        verify(adminUpload).setMultipartConfig(multipartConfig);
        verify(multipleFileUpload).setMultipartConfig(multipartConfig);
        verify(unrelatedServlet, never()).setMultipartConfig(any(MultipartConfigElement.class));
    }

    private void assertFilterRegistration(FilterRegistrationBean<?> registration,
            Class<?> filterClass, String filterName, int order, Set<String> urlPatterns,
            Set<String> servletNames, EnumSet<DispatcherType> dispatcherTypes) {
        assertInstanceOf(filterClass, registration.getFilter());
        assertEquals(filterName, registration.getFilterName());
        assertEquals(order, registration.getOrder());
        assertEquals(urlPatterns, Set.copyOf(registration.getUrlPatterns()));
        assertEquals(servletNames, Set.copyOf(registration.getServletNames()));
        assertEquals(dispatcherTypes, registration.determineDispatcherTypes());
    }

    private void assertServletRegistration(ServletRegistrationBean<?> registration,
            Class<? extends Servlet> servletClass, String servletName, String... urlMappings) {
        assertInstanceOf(servletClass, registration.getServlet());
        assertEquals(servletName, registration.getServletName());
        assertEquals(Set.of(urlMappings), Set.copyOf(registration.getUrlMappings()));
    }
}
