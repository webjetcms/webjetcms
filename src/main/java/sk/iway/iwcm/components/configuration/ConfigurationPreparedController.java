package sk.iway.iwcm.components.configuration;

import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.configuration.model.ConfPreparedEntity;
import sk.iway.iwcm.components.configuration.model.ConfPreparedRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@RestController
@Datatable
@RequestMapping(value = "/admin/rest/settings/prepared")
@ComponentScan("configuration")
public class ConfigurationPreparedController extends DatatableRestControllerV2<ConfPreparedEntity, Long> {

    private final ConfPreparedRepository confPreparedRepository;
    @Autowired
    public ConfigurationPreparedController(ConfPreparedRepository confPreparedRepository) {
        super(confPreparedRepository);
        this.confPreparedRepository = confPreparedRepository;
    }

    @Override
	public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<ConfPreparedEntity> root, CriteriaBuilder builder) {
		//aby nam hladalo aj podla searchUserFullName musime zavolat aj super metodu
		super.addSpecSearch(params, predicates, root, builder);

		//pridaj vyhladavanie podla modu
		String mode = getRequest().getParameter("mode");
		if ("history".equals(mode)) predicates.add(builder.isNull(root.get("datePrepared")));
		else predicates.add(builder.isNotNull(root.get("datePrepared")));
	}

    @Override
    public Page<ConfPreparedEntity> getAllItems(Pageable pageable) {
        String name = getRequest().getParameter("name");
        if(name != null && !name.isEmpty()) {
			if ("history".equals(getRequest().getParameter("mode"))) return confPreparedRepository.findByNameAndDatePreparedIsNull(pageable, name);
            return confPreparedRepository.findByNameAndDatePreparedIsNotNull(pageable, name);
        }
        return confPreparedRepository.findByName(pageable, "");
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
