package sk.iway.iwcm.stat.rest;

import java.sql.SQLException;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sk.iway.iwcm.stat.BrowserIdentifierMigrationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/rest/settings/stat-browser-migration")
@PreAuthorize("@WebjetSecurityService.hasPermission('modUpdate')")
public class BrowserIdentifierMigrationRestController {

    private final BrowserIdentifierMigrationService service;

    @GetMapping
    public BrowserIdentifierMigrationService.Preview preview() throws SQLException {
        return service.preview();
    }

    @PostMapping
    public BrowserIdentifierMigrationService.State process(@RequestBody BrowserIdentifierMigrationService.State state) throws SQLException {
        return service.process(state);
    }
}
