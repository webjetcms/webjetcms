package sk.iway.iwcm.system.spring.webjet_component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

// extendnuty dispatcher servlet, aby spravne identifikoval view z viewResolveru pre komponenty
public class WebjetDispatcherServlet extends DispatcherServlet {
    private static final long serialVersionUID = 1L;

    protected void render(ModelAndView mv, ViewResolver viewResolver, HttpServletRequest request, HttpServletResponse response) throws Exception {
        View view = null;

        if (!mv.isReference()) {
            view = mv.getView();
        }

        if (view == null ) {
            view = viewResolver.resolveViewName(mv.getViewName(), request.getLocale());
        }

        if (view == null) {
            throw new Exception(String.format("View %s missing", mv.getViewName()));
        }

        view.render(mv.getModelMap(), request, response);
    }
}
