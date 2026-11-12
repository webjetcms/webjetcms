package sk.iway.iwcm.components.ai.tools;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Pins a translated call to its registered handler after MVC has matched the concrete request. */
public class AiToolInterceptor implements HandlerInterceptor {

    private final AiToolAuthorization authorization;

    public AiToolInterceptor(AiToolAuthorization authorization) {
        this.authorization = authorization;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(request.getAttribute(AiToolService.TARGET) instanceof AiToolRegistry.Entry expected)) return true;
        if (handler instanceof HandlerMethod actual && registered(actual).equals(registered(expected.handler()))
                && expected.method().name().equals(request.getMethod())
                && expected.path().equals(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))
                && expected.mapping().getMatchingCondition(request) != null && authorization.isAllowed(expected, request, true)) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return false;
    }

    private static HandlerMethod registered(HandlerMethod handler) {
        HandlerMethod registered = handler.getResolvedFromHandlerMethod();
        return registered == null ? handler : registered;
    }
}
