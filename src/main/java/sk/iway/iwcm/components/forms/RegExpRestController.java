package sk.iway.iwcm.components.forms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.form.FormDB;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/forms_regexp")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_form')")
@Datatable
public class RegExpRestController extends DatatableRestControllerV2<RegExpEntity, Long>{

    @Autowired
    public RegExpRestController(RegExpRepository regExpRepository) {
        super(regExpRepository);
    }

    @Override
    public void afterSave(RegExpEntity entity, RegExpEntity saved) {
        //obnov cache
        FormDB.getInstance(true);
    }
}
