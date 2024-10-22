package sk.iway.iwcm.components.users.permgroups;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.users.PermissionGroupBean;
import sk.iway.iwcm.users.PermissionGroupEditorFields;

/**
 * Skupiny prav
 * Tu sa spravuju skupinove prava pre pouzivatela, tie sa pri jeho prihlaseni spojja s pravami nastavenymi pouzivatelov
 * Ak ma user viac skupin, prava sa pridavaju, cize postupne ziskava pravo z viacerych skupin a aj individualne nastavene prava
 */
@RestController
@Datatable
@RequestMapping("/admin/rest/users/permission-groups")
@PreAuthorize("@WebjetSecurityService.hasPermission('users.perm_groups')")
public class PermissionGroupRestController extends DatatableRestControllerV2<PermissionGroupBean, Long>{

    @Autowired
    public PermissionGroupRestController(PermissionGroupRepository permissionGroupRepository) {
        super(permissionGroupRepository);
    }

    @Override
    public PermissionGroupBean processFromEntity(PermissionGroupBean entity, ProcessItemAction action) {
        //serverSide je false, takze vzdy posielame vsetko
        boolean loadSubQueries = true;
        if (ProcessItemAction.GETONE.equals(action)) {
            if (entity == null) entity = new PermissionGroupBean();
        }

        if(entity != null) {
            PermissionGroupEditorFields pgef = new PermissionGroupEditorFields();
            pgef.fromPermissionGroupBean(entity, loadSubQueries, getRequest());
            entity.setEditorFields(pgef);
        }
        return entity;
    }

    @Override
    public PermissionGroupBean processToEntity(PermissionGroupBean entity, ProcessItemAction action) {
        if(entity != null) {
            //Call toUserDetailsEntity to set new entity values from EditorFields
            PermissionGroupEditorFields pgef = new PermissionGroupEditorFields();
            pgef.toPermissionGroupBean(entity);
        }
        return entity;
    }
}
