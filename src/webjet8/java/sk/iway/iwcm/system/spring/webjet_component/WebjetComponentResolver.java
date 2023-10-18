package sk.iway.iwcm.system.spring.webjet_component;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.WebjetComponentInterface;
import sk.iway.iwcm.editor.rest.ComponentsRestController;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.annotations.DefaultHandler;


@Component
public class WebjetComponentResolver {

    // handluje metodu z triedy, vklada do argumentov hodnoty a vykona navratovy typ, hodnotu vrati v response
    private final WebjetRequestMappingHandlerAdapter handlerAdapter;

    private final WebjetViewResolver viewResolver;

    @Autowired
    public WebjetComponentResolver(WebjetRequestMappingHandlerAdapter handlerAdapter, WebjetViewResolver viewResolver) {
        this.handlerAdapter = handlerAdapter;
        this.viewResolver = viewResolver;
    }

    // renderuje html kod z triedy a page params
    public String render(HttpServletRequest request, HttpServletResponse response, WebjetComponentInterface component, PageParams pageParams) throws Exception {
        try {
            // naplnenie page params do triedy komponenty
            ComponentsRestController.setParametersToBean(component, pageParams);

            // inicializacna metoda pre potreby komponenty
            component.init();
            component.init(request, response);

            // mock response, aby bolo mozne vybrat content
            MockHttpServletResponse mockResponse = new MockHttpServletResponse();

            // vygeneruje html kod komponenty do responsu
            handleRequest(request, component, mockResponse, pageParams);

            // ak response obsahuje redirect, tak ho odosleme a vratime prazdny retazec
            if (Tools.isNotEmpty(mockResponse.getRedirectedUrl())) {
                Logger.debug(WebjetComponentResolver.class,
                        String.format(
                                "Redirect from component %s to URL: %s",
                                component.getClass().getSimpleName(),
                                mockResponse.getRedirectedUrl()
                        )
                );
                response.sendRedirect(mockResponse.getRedirectedUrl());
                return "";
            }

            // ziska content responsu
            return mockResponse.getContentAsString();
        } catch (IOException e) {
            sk.iway.iwcm.Logger.error(e);
        }

        return "";
    }

    // vykona kod z metody komponenty ktora je anotovana @Render
    void handleRequest(HttpServletRequest request, WebjetComponentInterface component, HttpServletResponse response, PageParams pageParams) throws Exception {

        // vyhlada metodu s anotaciou @Render
        Method method = findHandlerMethod(request, component, pageParams);
        if (method == null) {
            response.getWriter().println(Prop.getInstance(request).getText("writetag.error_not_exists", null, null, component.getClass().getSimpleName()));
            return;
        }
        HandlerMethod handlerMethod = new HandlerMethod(component, method);

        try {
            // ziska ModelAndView z akejkolvek navratovej hodnoty metody
            ModelAndView mv = handlerAdapter.handle(request, response, handlerMethod);
            WebjetDispatcherServlet dispatcherServlet = new WebjetDispatcherServlet();

            if (Tools.isNotEmpty(component.getViewFolder())) {
                viewResolver.setViewFolder(component.getViewFolder());
            }

            mv.addObject("request", request);
            mv.addObject("session", request.getSession());

            // nastavi html kod komponenty do responsu
            dispatcherServlet.render(mv, viewResolver, request, response);
        }
        catch (AccessDeniedException ex) {
            response.getWriter().println(ex.getMessage());
        }
    }

    // vyhlada prislusnu metodu v triede
    Method findHandlerMethod(HttpServletRequest request, WebjetComponentInterface bean, PageParams pageParams) {
        @SuppressWarnings("rawtypes")
        Class c = bean.getClass();

        if (c != null && c.getSimpleName().contains("EnhancerBySpring")) {
            c = ClassUtils.getUserClass(c);
        }
        if (c==null) return null;

        Method[] methods = c.getMethods();
        Set<String> parameterNames = request.getParameterMap().keySet();
        if(!parameterNames.isEmpty()) {
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (parameterNames.contains(method.getName())) {
                    return method;
                }
            }
        }

        if (pageParams != null) {
            String defaultHandler = pageParams.getValue("defaultHandler", null);
            if (Tools.isNotEmpty(defaultHandler)) {
                for( int i = 0; i < methods.length; i++ ) {
                    Method method = methods[i];
                    if (defaultHandler.equals(method.getName())) {
                        return method;
                    }
                }
            }
        }

        // hladanie metody s anotaciou @DefaultHandler
        for( int i = 0; i < methods.length; i++ ) {
            Method method = methods[i];
            if (method.isAnnotationPresent(DefaultHandler.class)) {
                return method;
            }
        }

        // ak nenajde anotovanu metodu hlada metodu s nazvom render
        for( int i = 0; i < methods.length; i++ ) {
            Method method = methods[i];
            if(method.getName().equals("render")) {
                return method;
            }
        }

        return null;
    }
}