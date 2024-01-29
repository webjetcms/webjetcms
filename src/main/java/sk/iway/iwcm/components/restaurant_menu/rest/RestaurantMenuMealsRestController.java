package sk.iway.iwcm.components.restaurant_menu.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.restaurant_menu.jpa.RestaurantMenuMealsEditorFields;
import sk.iway.iwcm.components.restaurant_menu.jpa.RestaurantMenuMealsEntity;
import sk.iway.iwcm.components.restaurant_menu.jpa.RestaurantMenuMealsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.SpecSearch;


@RestController
@RequestMapping("/admin/rest/restaurant-menu/meals")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_restaurant_menu')")
@Datatable
public class RestaurantMenuMealsRestController extends DatatableRestControllerV2<RestaurantMenuMealsEntity, Long> {

    private final RestaurantMenuMealsRepository rmmr;

    @Autowired
    public RestaurantMenuMealsRestController(RestaurantMenuMealsRepository rmmr) {
        super(rmmr);
        this.rmmr = rmmr;
    }

    @Override
    public Page<RestaurantMenuMealsEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<RestaurantMenuMealsEntity> page = new DatatablePageImpl<>( rmmr.findAllByDomainId(CloudToolsForCore.getDomainId(), pageable) );
        page.addOptions("editorFields.alergensArr", RestaurantMenuService.getAlergenBeans( getRequest() ), "alergenName", "alergenNumber", false);
        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public RestaurantMenuMealsEntity processFromEntity(RestaurantMenuMealsEntity entity, ProcessItemAction action) {
        RestaurantMenuMealsEditorFields rmmef = entity.getEditorFields();
        if(rmmef == null) rmmef = new RestaurantMenuMealsEditorFields();
        rmmef.fromRestaurantMenuMealsEntity(entity);
        return entity;
    }

    @Override
    public RestaurantMenuMealsEntity processToEntity(RestaurantMenuMealsEntity entity, ProcessItemAction action) {
        if(entity != null) {
            RestaurantMenuMealsEditorFields rmmef = entity.getEditorFields();
            rmmef.toRestaurantMenuMealsEntity(entity);
        }
        return entity;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<RestaurantMenuMealsEntity> root, CriteriaBuilder builder) {
        SpecSearch<RestaurantMenuMealsEntity> specSearch = new SpecSearch<>();
        String alergen = params.get("searchEditorFields.alergensArr");
        if (alergen != null)
            specSearch.addSpecSearchPasswordProtected(Tools.getIntValue(alergen, -1), "alergens", predicates, root, builder);

        super.addSpecSearch(params, predicates, root, builder);
    }

    @Override
	public void beforeSave(RestaurantMenuMealsEntity entity) {
        //Set domain id
        if(entity.getId() == null || entity.getId() == -1)
            entity.setDomainId(CloudToolsForCore.getDomainId());

        //Arr of id's alergens to string
        processToEntity(entity, ProcessItemAction.EDIT);
	}

    @RequestMapping(value = "/getSelectValues", params=("cathegory"))
    @ResponseBody
    public List<Long> getSelectMealByCathegory(@RequestParam("cathegory") String cathegory) {
        List<Long> selectValues = new ArrayList<>();
        if(Tools.isNotEmpty(cathegory)) {
            for(RestaurantMenuMealsEntity entity : rmmr.findAllByCathegoryAndDomainId(cathegory, CloudToolsForCore.getDomainId())) {
                selectValues.add(entity.getId());
            }
        }
        return selectValues;
    }
}
