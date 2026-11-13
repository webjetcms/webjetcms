package sk.iway.iwcm.components.ai.rest;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import sk.iway.iwcm.components.ai.tools.AiToolCallFilter;
import sk.iway.iwcm.components.ai.tools.AiToolDefinition;
import sk.iway.iwcm.components.ai.tools.AiToolService;

/** Administration REST interface to the current user's AI tool catalog. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/rest/ai/tools")
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class AiToolsController {

    private final AiToolService tools;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<AiToolDefinition>> list(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("tools", tools.list(request));
    }

    /** Valid calls reach the target MVC handler. Unresolved calls still pass normal admin security. */
    @PostMapping("/{name}/call")
    public void unresolvedCall(HttpServletRequest request, HttpServletResponse response) {
        AiToolCallFilter.rejectUntranslated(request, response);
    }
}
