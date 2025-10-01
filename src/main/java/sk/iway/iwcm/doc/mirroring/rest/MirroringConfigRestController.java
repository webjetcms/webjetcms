package sk.iway.iwcm.doc.mirroring.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.configuration.AbstractConfigurationController;
import sk.iway.iwcm.components.configuration.ConfDetailsMapper;
import sk.iway.iwcm.system.datatable.Datatable;

@RestController
@Datatable
@RequestMapping("/admin/rest/webpages/mirroring/config")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_mirroring')")
public class MirroringConfigRestController extends AbstractConfigurationController {

    @Autowired
    public MirroringConfigRestController(ConfDetailsMapper confDetailsMapper) {
        super("structureMirroring", confDetailsMapper);
    }
}