package sk.iway.iwcm.components.abtesting.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.configuration.AbstractConfigurationController;
import sk.iway.iwcm.components.configuration.ConfDetailsMapper;
import sk.iway.iwcm.system.datatable.Datatable;

@RestController
@Datatable
@RequestMapping("/admin/rest/abtesting/config")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_abtesting')")
public class AbTestingConfigRestController extends AbstractConfigurationController {

    @Autowired
    public AbTestingConfigRestController(ConfDetailsMapper confDetailsMapper) {
        super("ABTesting", confDetailsMapper);
    }
}
