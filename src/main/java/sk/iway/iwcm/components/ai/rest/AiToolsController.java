package sk.iway.iwcm.components.ai.rest;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import sk.iway.iwcm.components.ai.tools.AiTool;
import sk.iway.iwcm.components.ai.tools.AiToolCallFilter;
import sk.iway.iwcm.components.ai.tools.AiToolDefinition;
import sk.iway.iwcm.components.ai.tools.AiToolProtocol;
import sk.iway.iwcm.components.ai.tools.AiToolService;

/** Administration REST interface to the current user's AI tool catalog. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/rest/ai/tools")
@PreAuthorize("@WebjetSecurityService.isAdmin()")
@AiTool(description = "components.ai.tools.ai_tools.context", readOnly = true, include = {"preview", "details"})
public class AiToolsController {

    private final AiToolService tools;

    @GetMapping(value = "/preview", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<AiToolDefinition.Preview>> preview(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("tools", tools.preview(request));
    }

    @GetMapping(value = "/details", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<AiToolDefinition>> details(@RequestParam("names") List<String> names,
            HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("tools", tools.details(names, request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<AiToolDefinition>> list(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("tools", tools.list(request));
    }

    @GetMapping(params = "assistantId", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> assigned(@RequestParam long assistantId, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        var assigned = tools.assigned(assistantId, request);
        return Map.of("tools", assigned, "instructions", AiToolProtocol.instructions(assigned));
    }

    /** Valid calls reach the target MVC handler. Unresolved calls still pass normal admin security. */
    @PostMapping("/{name}/call")
    public void unresolvedCall(HttpServletRequest request, HttpServletResponse response) {
        AiToolCallFilter.rejectUntranslated(request, response);
    }
}
