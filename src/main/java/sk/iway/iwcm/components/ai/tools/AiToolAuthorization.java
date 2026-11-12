package sk.iway.iwcm.components.ai.tools;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.VariableReference;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.PreAuthorizeAuthorizationManager;
import org.springframework.security.authorization.method.SecuredAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.SecurityAnnotationScanners;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.users.UsersDB;

/** Evaluates existing controller and handler permissions without executing a mapped operation. */
@Component
public class AiToolAuthorization {

    private final MethodSecurityExpressionHandler expressions;
    private final PreAuthorizeAuthorizationManager preAuthorize = new PreAuthorizeAuthorizationManager();
    private final SecuredAuthorizationManager secured = new SecuredAuthorizationManager();

    public AiToolAuthorization(ApplicationContext context) {
        expressions = context.getBeanProvider(MethodSecurityExpressionHandler.class).getIfUnique(() -> {
            DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
            handler.setApplicationContext(context);
            return handler;
        });
        preAuthorize.setExpressionHandler(expressions);
        preAuthorize.setApplicationContext(context);
    }

    static boolean hasAdminIdentity(HttpServletRequest request) {
        Identity user = UsersDB.getCurrentUser(request.getSession(false));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return user != null && user.isAdmin() && auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);
    }

    public boolean isAllowed(AiToolRegistry.Entry entry, HttpServletRequest request) {
        return isAllowed(entry, request, false);
    }

    boolean isAllowed(AiToolRegistry.Entry entry, HttpServletRequest request, boolean invoking) {
        if (!hasAdminIdentity(request)) return false;
        Object previous = request.getAttribute(AiToolRequest.CHECKING_PERMISSION);
        request.setAttribute(AiToolRequest.CHECKING_PERMISSION, Boolean.TRUE);
        try {
            Object bean = entry.handler().createWithResolvedBean().getBean();
            Class<?> type = entry.handler().getBeanType();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            PermissionInvocation invocation = new PermissionInvocation(bean, entry.handler().getMethod());
            // A handler annotation can replace Spring's type-level annotation. Tool access requires both.
            PreAuthorize classRule = AnnotatedElementUtils.findMergedAnnotation(type, PreAuthorize.class);
            if (classRule != null) {
                var expression = expressions.getExpressionParser().parseExpression(classRule.value());
                if (expression instanceof SpelExpression spel && requiresArguments(spel.getAST())) return false;
                if (!Boolean.TRUE.equals(expression.getValue(expressions.createEvaluationContext(() -> auth, invocation), Boolean.class))) return false;
            }
            Secured classRoles = AnnotatedElementUtils.findMergedAnnotation(type, Secured.class);
            if (classRoles != null && auth.getAuthorities().stream()
                    .noneMatch(a -> Arrays.asList(classRoles.value()).contains(a.getAuthority()))) return false;

            if (!((DatatableRestControllerV2<?, ?>) bean).checkAccessAllowed(request)) return false;
            if (!grants(secured.authorize(() -> auth, invocation))) return false;
            PreAuthorize handlerRule = SecurityAnnotationScanners.requireUnique(PreAuthorize.class).scan(invocation.getMethod(), type);
            if (handlerRule != null && expressions.getExpressionParser().parseExpression(handlerRule.value()) instanceof SpelExpression spel
                    && requiresArguments(spel.getAST())) {
                // Only Spring MVC has the bound Java arguments. Its secured proxy evaluates this rule when invoking.
                return invoking;
            }
            return grants(preAuthorize.authorize(() -> auth, invocation));
        } catch (RuntimeException cannotEstablishPermission) {
            // A record-dependent expression cannot grant catalog visibility without a record.
            return false;
        } finally {
            if (previous == null) request.removeAttribute(AiToolRequest.CHECKING_PERMISSION);
            else request.setAttribute(AiToolRequest.CHECKING_PERMISSION, previous);
        }
    }

    private static boolean grants(AuthorizationResult result) {
        return result == null || result.isGranted();
    }

    private static boolean requiresArguments(SpelNode node) {
        if (node instanceof VariableReference) return true;
        for (int i = 0; i < node.getChildCount(); i++) {
            if (requiresArguments(node.getChild(i))) return true;
        }
        return false;
    }

    private record PermissionInvocation(Object target, Method method) implements MethodInvocation {
        @Override public Object getThis() { return target; }
        @Override public Method getMethod() { return method; }
        @Override public AccessibleObject getStaticPart() { return method; }
        @Override public Object[] getArguments() { return new Object[method.getParameterCount()]; }
        @Override public Object proceed() { throw new UnsupportedOperationException(); }
    }
}
