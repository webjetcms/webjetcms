package sk.iway.iwcm.system.spring.passkey;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.users.UsersDB;

/**
 * DataTable REST controller for PassKey/WebAuthn credential management.
 * Allows each logged-in admin user to view, rename and delete their own passkeys.
 * Creating new passkeys is handled by the WebAuthn browser flow - see the frontend page.
 */
@RestController
@Lazy
@RequestMapping("/admin/rest/passkey")
@PreAuthorize(value = "@WebjetSecurityService.isAdmin()")
@Datatable
public class PasskeyAdminRestController extends DatatableRestControllerV2<PasskeyCredentialEntity, Long> {

    private final UserDetailsRepository userDetailsRepository;

    @Autowired
    public PasskeyAdminRestController(PasskeyCredentialRepository passkeyCredentialRepository,
            UserDetailsRepository userDetailsRepository) {
        super(passkeyCredentialRepository);
        this.userDetailsRepository = userDetailsRepository;
    }

    @Override
    public Page<PasskeyCredentialEntity> getAllItems(Pageable pageable) {
        return super.getAllItemsIncludeSpecSearch(new PasskeyCredentialEntity(), pageable);
    }

    /**
     * Returns the current logged-in user's entity ID, or null if not found.
     */
    private Long getCurrentUserId() {
        Identity user = UsersDB.getCurrentUser(getRequest());
        if (user == null) return null;
        Optional<UserDetailsEntity> userEntityOpt = userDetailsRepository.findFirstByLoginAndDomainId(
                user.getLogin(), UsersDB.getDomainId());
        return userEntityOpt.map(UserDetailsEntity::getId).orElse(null);
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates,
            Root<PasskeyCredentialEntity> root, CriteriaBuilder builder) {
        // Always filter to only show current user's passkeys
        Long userId = getCurrentUserId();
        if (userId != null) {
            predicates.add(builder.equal(root.get("userId"), userId));
        } else {
            // If user cannot be resolved, return no results
            predicates.add(builder.disjunction());
        }
        super.addSpecSearch(params, predicates, root, builder);
    }

    @Override
    public boolean checkItemPerms(PasskeyCredentialEntity entity, Long id) {
        Long userId = getCurrentUserId();
        return userId != null && userId.equals(entity.getUserId());
    }

    @Override
    public void beforeSave(PasskeyCredentialEntity entity) {
        // Prevent userId from being changed
        Long userId = getCurrentUserId();
        if (userId != null) {
            entity.setUserId(userId);
        }
    }

    @Override
    public PasskeyCredentialEntity insertItem(PasskeyCredentialEntity entity) {
        // Registering new passkeys must be done through the WebAuthn browser flow
        throwError("passkey.manage.registerViaUI");
        return null;
    }
}
