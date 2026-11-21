package sk.iway.iwcm.editor.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import sk.iway.iwcm.branch.BranchContext;
import sk.iway.iwcm.branch.BranchContexts;
import sk.iway.iwcm.components.WebjetComponentInterface;
import sk.iway.iwcm.system.datatable.DataTableColumnsFactory;
import sk.iway.iwcm.system.spring.components.SpringContext;

/** Registered application parameter editors follow the ordinary pipeline in working versions. */
class ComponentsServiceTest {
    /** Branches must initialize registered editors beyond the former Menu/News allowlist. */
    @ParameterizedTest
    @ValueSource(strings={"sk.iway.iwcm.components.media.MediaApp","sk.iway.iwcm.components.gallery.GalleryApp","customer.CustomApp"})
    void initializesRegisteredEditorsInBranches(String className) throws Exception {
        ApplicationContext spring=mock(ApplicationContext.class);
        WebjetComponentInterface component=mock(WebjetComponentInterface.class);
        when(spring.containsBean(className)).thenReturn(true);
        when(spring.getBean(className,WebjetComponentInterface.class)).thenReturn(component);
        ComponentRequest input=new ComponentRequest();input.setClassName(className);
        MockHttpServletRequest request=new MockHttpServletRequest();
        try(var contexts=mockStatic(BranchContexts.class);
            var application=mockStatic(SpringContext.class);
            var columns=mockConstruction(DataTableColumnsFactory.class)) {
            contexts.when(BranchContexts::current).thenReturn(new BranchContext(1,"example.sk",1,BranchContext.Mode.ADMIN));
            application.when(SpringContext::getApplicationContext).thenReturn(spring);

            var response=ComponentsService.getComponentResponse(input,request);

            assertFalse(response.containsKey("error"));
            assertSame(component,response.get("data"));
            assertEquals(1,columns.constructed().size());
            verify(component).initAppEditor(input,request);
            verify(component).getAppOptions(input,request);
        }
    }

    /** Missing or unregistered applications still return the ordinary validation error. */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings={"","unregistered.CustomApp"})
    void keepsNormalRegistrationValidation(String className) {
        ApplicationContext spring=mock(ApplicationContext.class);
        ComponentRequest input=new ComponentRequest();input.setClassName(className);
        try(var contexts=mockStatic(BranchContexts.class);var application=mockStatic(SpringContext.class)) {
            contexts.when(BranchContexts::current).thenReturn(new BranchContext(1,"example.sk",1,BranchContext.Mode.ADMIN));
            application.when(SpringContext::getApplicationContext).thenReturn(spring);

            var response=ComponentsService.getComponentResponse(input,new MockHttpServletRequest());

            assertNotNull(response.get("error"));
            assertFalse(response.containsKey("data"));
            verify(spring,never()).getBean(anyString(),eq(WebjetComponentInterface.class));
        }
    }
}
