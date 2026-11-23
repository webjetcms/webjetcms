package sk.iway.iwcm.components.configuration;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.configuration.model.ConfPreparedEntity;
import sk.iway.iwcm.components.configuration.model.ConfPreparedRepository;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.jpa.JpaTools;

@RestController
@Datatable
@RequestMapping(value = "/admin/rest/settings/prepared")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('menuConfig')")
@ComponentScan("configuration")
public class ConfigurationPreparedController extends DatatableRestControllerV2<ConfPreparedEntity, Long> {

    private final ConfPreparedRepository confPreparedRepository;

    @Autowired
    public ConfigurationPreparedController(ConfPreparedRepository confPreparedRepository) {
        super(confPreparedRepository);
        this.confPreparedRepository = confPreparedRepository;
    }

    @Override
    public Page<ConfPreparedEntity> getAllItems(Pageable pageable) {
        if (Tools.isEmpty(getRequest().getParameter("name"))) return Page.empty(pageable);
        Specification<ConfPreparedEntity> filters = getSearchConditions(Map.of(), getParamsMap(getRequest()), new ConfPreparedEntity());
        return confPreparedRepository.findAll(filters, pageable);
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<ConfPreparedEntity> root, CriteriaBuilder builder) {
        super.addSpecSearch(params, predicates, root, builder);
        if (Tools.isNotEmpty(params.get("name"))) predicates.add(builder.equal(root.get("name"), params.get("name")));
        if ("history".equals(params.get("mode"))) predicates.add(builder.isNull(root.get("datePrepared")));
        else predicates.add(builder.isNotNull(root.get("datePrepared")));
    }

    @Override
    protected Specification<ConfPreparedEntity> getSearchConditions(Map<String, String> properties, Map<String, String> params, ConfPreparedEntity entity) {
        Identity user = getUser();
        if (user == null) return (root, query, builder) -> builder.disjunction();
        Specification<ConfPreparedEntity> filters = super.getSearchConditions(properties, params, entity);
        String enabledKeys = Constants.getStringExecuteMacro("configEnabledKeys");
        if (ConfDB.canViewAllKeys(user, enabledKeys)) return filters;

        String[] prefixes = Tools.getTokens(enabledKeys, ",");
        String name = params.get("name");
        if (Tools.isNotEmpty(name) && !ConfDB.isKeyVisibleToUser(user, prefixes, name)) {
            return (root, query, builder) -> builder.disjunction();
        }
        // Database collations may treat different names as equal. Apply WebJET's case-sensitive
        // prefix policy to stored names before the data and count queries are paginated.
        List<Long> visibleIds = confPreparedRepository.findBy(filters, query -> query.as(StoredKey.class).all()).stream()
            .filter(key -> Tools.isNotEmpty(key.name()) && ConfDB.isKeyVisibleToUser(user, prefixes, key.name()))
            .map(StoredKey::id).toList();
        return filters.and((root, query, builder) -> JpaTools.in(root.get("id"), visibleIds, builder));
    }

    public record StoredKey(Long id, String name) {}

    @Override
    public boolean checkItemPerms(ConfPreparedEntity entity, Long id) {
        Identity user = getUser();
        if (user == null || id == null || id <= 0) return false;
        // Delete receives an editor row. Authorize the name stored under the target ID.
        return confPreparedRepository.findById(id).map(ConfPreparedEntity::getName)
            .filter(Tools::isNotEmpty).filter(name -> ConfDB.isKeyVisibleToUser(user, name)).isPresent();
    }

    @Override
    public JSONObject sumItems(ConfPreparedEntity entity, String[] columns) {
        // The inherited summary aggregates the whole table.
        if (!canViewAllKeys()) return null;
        return super.sumItems(entity, columns);
    }

    @Override
    public Map<Long, ConfPreparedEntity> preImportDataEdit(HttpServletRequest request, Map<Long, ConfPreparedEntity> data, String importMode) {
        // Import matching would look up rows before the read-only save hook runs.
        if (!canViewAllKeys()) throwError("datatables.error.recordIsNotEditable");
        return super.preImportDataEdit(request, data, importMode);
    }

    private boolean canViewAllKeys() {
        Identity user = getUser();
        return user != null && ConfDB.canViewAllKeys(user, Constants.getStringExecuteMacro("configEnabledKeys"));
    }

	@Override
	public void beforeSave(ConfPreparedEntity entity) {
		throwError("datatables.error.recordIsNotEditable");
	}

	@Override
    public boolean beforeDelete(ConfPreparedEntity entity) {
		//zmazat je mozne len planovany zaznam v buducnosti
		if (entity.getId()>0 && entity.getDatePrepared()!=null && entity.getDatePrepared().getTime()>Tools.getNow()) return true;

		throwError("admin.cong_editor.youCanOnlyDeleteFutureRecords");
		return false;
    }
}
