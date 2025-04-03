package sk.iway.iwcm.components.users.groups_approve;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/**
 * Pouzivatelia - mapovanie schvalovania
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/groups-approve")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('users.edit_admins')")
public class GroupsApproveController extends DatatableRestControllerV2<GroupsApproveEntity, Long> {

    private final GroupsApproveRepository groupsApproveRepository;

    @Autowired
    public GroupsApproveController(GroupsApproveRepository groupsApproveRepository) {
        super(groupsApproveRepository);
        this.groupsApproveRepository = groupsApproveRepository;
    }

    @Override
    public Page<GroupsApproveEntity> getAllItems(Pageable pageable) {

        int userId = Tools.getIntValue(getRequest().getParameter("userId"), -1);

        Page<GroupsApproveEntity> page;
        if(userId != -1) {
            page = groupsApproveRepository.findByUserId(userId, pageable);
        } else {
            page = groupsApproveRepository.findAll(pageable);
        }

        return page;
    }

    @Override
    public void beforeSave(GroupsApproveEntity entity) {
        if (entity.getUserId()==null || entity.getUserId().longValue()<1) {
            //ID pouzivatela prenasame ako parameter REST volania, kedze je DT vlozena v editacii pouzivatela
            int userId = Tools.getIntValue(getRequest().getParameter("userId"), -1);
            entity.setUserId(Long.valueOf(userId));
        }
    }
}
