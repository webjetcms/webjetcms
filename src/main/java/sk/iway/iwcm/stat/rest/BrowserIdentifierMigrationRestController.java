package sk.iway.iwcm.stat.rest;

import java.sql.SQLException;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

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

    @GetMapping("/status")
    public BrowserIdentifierMigrationService.State status() {
        return service.getStatus();
    }

    @PostMapping("/start")
    public BrowserIdentifierMigrationService.State start() {
        return service.start();
    }

    @PostMapping("/stop")
    public BrowserIdentifierMigrationService.State stop() {
        return service.stop();
    }
}
