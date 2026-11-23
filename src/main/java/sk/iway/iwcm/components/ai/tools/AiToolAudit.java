package sk.iway.iwcm.components.ai.tools;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.system.datatable.events.DatatableEvent;
import sk.iway.iwcm.system.datatable.events.DatatableEventType;

/** Records dispatched tools and supplies attribution to the existing endpoint audit hooks. */
@Component
class AiToolAudit {
    private static final String ATTRIBUTE = AiToolAudit.class.getName();
    private static final String TOOL = "AI tool";

    static void begin(HttpServletRequest request, AiToolRegistry.Entry entry, AssistantDefinitionEntity assistant) {
        RequestBean context = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (context == null) return;

        String caller = assistant == null ? " (manual call)"
                : " via assistant " + assistant.getName().replaceAll("[\\p{Cntrl}\\p{Zl}\\p{Zp}]", " ") + " (#" + assistant.getId() + ")";
        String attribution = entry.definition().name() + caller + "\nEndpoint: " + entry.method() + " " + entry.path();
        request.setAttribute(ATTRIBUTE, attribution);
        markNextRow(attribution);
    }

    /** Each DataTable row consumes the audit values while recording its changes. */
    @EventListener
    public void beforeRowChange(DatatableEvent<?> event) {
        if (event.getEventType() != DatatableEventType.BEFORE_SAVE && event.getEventType() != DatatableEventType.BEFORE_DELETE) return;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                && attributes.getRequest().getAttribute(ATTRIBUTE) instanceof String attribution) {
            markNextRow(attribution);
        }
    }

    private static void markNextRow(String attribution) {
        RequestBean.removeAuditValue(TOOL);
        RequestBean.addAuditValue(TOOL, attribution);
    }

    static void finish(HttpServletRequest request, int status) {
        if (!(request.getAttribute(ATTRIBUTE) instanceof String attribution)) return;
        request.removeAttribute(ATTRIBUTE);
        RequestBean context = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (context == null) return;
        RequestBean.removeAuditValue(TOOL);
        String query = context.getQueryString();
        try {
            // The summary identifies the registered endpoint without copying tool arguments into its URI.
            context.setQueryString(null);
            Adminlog.add(Adminlog.TYPE_AI, "Tool call (HTTP " + status + ")\n" + TOOL + ": " + attribution, -1, -1);
        } catch (RuntimeException failure) {
            Logger.error(AiToolAudit.class, "Could not audit the tool call", failure);
        } finally {
            context.setQueryString(query);
        }
    }
}
