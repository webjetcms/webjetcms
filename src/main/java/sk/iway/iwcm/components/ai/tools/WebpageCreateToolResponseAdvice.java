package sk.iway.iwcm.components.ai.tools;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import lombok.RequiredArgsConstructor;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.editor.rest.WebpagesRestController;

/** Gives pending webpage tools using the original full-record input the same receipt as compact creates. */
@ControllerAdvice(assignableTypes = WebpagesRestController.class)
@RequiredArgsConstructor
public class WebpageCreateToolResponseAdvice implements ResponseBodyAdvice<Object> {
    private final EditorFacade editor;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getMethod() != null && "add".equals(returnType.getMethod().getName());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof DocDetails saved && request instanceof ServletServerHttpRequest servlet
                && AiToolService.isToolRequest(servlet.getServletRequest(), WebpagesRestController.class)) {
            return WebpagesRestController.CreatedPage.from(saved, editor);
        }
        return body;
    }
}
