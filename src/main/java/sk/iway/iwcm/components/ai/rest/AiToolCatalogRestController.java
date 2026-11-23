package sk.iway.iwcm.components.ai.rest;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import sk.iway.iwcm.components.ai.tools.AiToolEntity;
import sk.iway.iwcm.components.ai.tools.AiToolGroup;
import sk.iway.iwcm.components.ai.tools.AiToolService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;

/** Supplies the read-only settings DataTable from the current user's tool catalog. */
@RestController
@Datatable
@RequiredArgsConstructor
@RequestMapping("/admin/rest/settings/ai-tools")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_ai_tools')")
public class AiToolCatalogRestController {

    private final AiToolService tools;

    @GetMapping("/all")
    public DatatablePageImpl<AiToolEntity> getAll(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        var rows = tools.list(request, Prop.getInstance(request)).stream().map(AiToolEntity::new).toList();
        var page = new DatatablePageImpl<>(rows);
        var methods = rows.stream().map(AiToolEntity::getHttpMethod).distinct().sorted().toList();
        page.addOptions("httpMethod", methods, "", "", false);
        return page;
    }

    @GetMapping("/controllers/all")
    public DatatablePageImpl<AiToolGroup> getControllers(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        var groups = tools.listGroups(request, Prop.getInstance(request));
        var page = new DatatablePageImpl<>(groups);
        var methods = groups.stream().flatMap(group -> group.getEndpoints().stream())
            .map(AiToolGroup.Endpoint::httpMethod).distinct().sorted().toList();
        page.addOptions("httpMethod", methods, "", "", false);
        return page;
    }

    @GetMapping("/{name}")
    public AiToolEntity getOne(@PathVariable String name, HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        Prop language = Prop.getInstance(request);
        var tool = tools.findVisible(name, request, language).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return AiToolEntity.detail(tool, language);
    }
}
