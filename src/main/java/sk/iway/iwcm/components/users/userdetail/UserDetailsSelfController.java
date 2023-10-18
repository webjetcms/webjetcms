package sk.iway.iwcm.components.users.userdetail;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/**
 * Controller to edit user details for current user without useredit perms (show only limited fields)
 */
@RestController
@Datatable
@RequestMapping(value = "/admin/rest/user-self")
@PreAuthorize(value = "@WebjetSecurityService.isAdmin()")
public class UserDetailsSelfController extends DatatableRestControllerV2<UserDetailsSelfEntity, Long> {

    private final UserDetailsService userDetailsService;
    private final UserDetailsSelfRepository userDetailsSelfRepository;

    @Autowired
    public UserDetailsSelfController(UserDetailsSelfRepository userDetailsSelfRepository, UserDetailsService userDetailsService) {
        super(userDetailsSelfRepository);
        this.userDetailsSelfRepository = userDetailsSelfRepository;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Page<UserDetailsSelfEntity> getAllItems(Pageable pageable) {

        List<UserDetailsSelfEntity> all = new ArrayList<>();
        UserDetailsSelfEntity self = userDetailsSelfRepository.getById(Long.valueOf(getUser().getUserId()));
        all.add(self);

        DatatablePageImpl<UserDetailsSelfEntity> page = new DatatablePageImpl<>(all);

        return page;
    }

    @Override
	public UserDetailsSelfEntity insertItem(UserDetailsSelfEntity entity) {
		throwPermsDenied();
        return null;
	}

    @Override
	public UserDetailsSelfEntity editItem(UserDetailsSelfEntity entity, long id) {
		if (id != getUser().getUserId()) throwPermsDenied();

        return super.editItem(entity, id);
	}

    @Override
    public UserDetailsSelfEntity getOneItem(long id) {
        if (id != getUser().getUserId()) throwPermsDenied();

        UserDetailsSelfEntity one = super.getOneItem(id);

        //nastav heslo na Unchanged, aby presla validacia
        one.setPassword(UserTools.PASS_UNCHANGED);

        return one;
    }

    @Override
    public void afterSave(UserDetailsSelfEntity entity, UserDetailsSelfEntity saved) {
        //update current user if editing self
        userDetailsService.updateSelf(saved, getUser(), getRequest());
    }

    @SuppressWarnings("all")
    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, UserDetailsSelfEntity> target, Identity user, Errors errors, Long id, UserDetailsSelfEntity entity) {

        Prop prop = Prop.getInstance(request);

        userDetailsService.validatePassword(entity, false, true, prop, errors);

        //not empty aby pri prazdnej hlasilo v editore, ze to je povinne pole
        if (Tools.isNotEmpty(entity.getEmail()) && Tools.isEmail(entity.getEmail())==false) {
            errors.rejectValue("errorField.email", null, prop.getText("javax.validation.constraints.Email.message"));
        }

    }

    private void throwPermsDenied() {
        throwError("datatables.error.recordIsNotEditable");
    }
}