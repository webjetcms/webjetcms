package sk.iway.iwcm.editor.rest;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.PlannedAccess;
import sk.iway.iwcm.editor.service.EditorService;
import sk.iway.iwcm.doc.PlannedChangesService;
import sk.iway.iwcm.users.UsersDB;

/** Explicit lifecycle actions; unchecking the editor switch never destroys a planned set. */
@RestController
@RequestMapping("/admin/rest/web-pages/planned")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuWebpages')")
public class PlannedChangesController {
    private final PlannedChangesService service;

    private final EditorService editorService;

    public PlannedChangesController(PlannedChangesService service, EditorService editorService) {
        this.service = service;
        this.editorService = editorService;
    }

    @PostMapping("/{docId}/initialize")
    public Map<String, Long> initialize(@PathVariable int docId, HttpServletRequest request) {
        DocDetails doc = editorService.getDoc(PlannedAccess.masterId(docId), -1);
        editorService.checkPermissions(UsersDB.getCurrentUser(request), doc, false);
        Long history = service.initialize(doc.getDocId());
        return Map.of("historyId", history == null ? -1L : history);
    }

    @PostMapping("/{docId}/cancel")
    public Map<String, Boolean> cancel(@PathVariable int docId, HttpServletRequest request) {
        DocDetails doc = editorService.getDoc(PlannedAccess.masterId(docId), -1);
        editorService.checkPermissions(UsersDB.getCurrentUser(request), doc, false);
        service.cancel(doc.getDocId());
        DocDB.getInstance().readPagesToPublic();
        return Map.of("success", true);
    }
}
