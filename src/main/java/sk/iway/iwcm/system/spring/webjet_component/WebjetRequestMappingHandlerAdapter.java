package sk.iway.iwcm.system.spring.webjet_component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

// handluje metodu z triedy, vklada do argumentov hodnoty a vykona navratovy typ, hodnotu vrati v response
@Component
public class WebjetRequestMappingHandlerAdapter {
    private final RequestMappingHandlerAdapter handlerAdapter;

    @Autowired
    public WebjetRequestMappingHandlerAdapter(RequestMappingHandlerAdapter handlerAdapter) {
        this.handlerAdapter = handlerAdapter;
    }

    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
        return handlerAdapter.handle(request, response, handlerMethod);
    }
}