package sk.iway.iwcm.components.restaurant_menu.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.restaurant_menu.jpa.RestaurantMenuEditorFields;
import sk.iway.iwcm.components.restaurant_menu.jpa.RestaurantMenuEntity;
import sk.iway.iwcm.components.restaurant_menu.jpa.RestaurantMenuMealsRepository;
import sk.iway.iwcm.components.restaurant_menu.jpa.RestaurantMenuRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/restaurant-menu/menu")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_restaurant_menu')")
@Datatable
public class RestaurantMenuRestController extends DatatableRestControllerV2<RestaurantMenuEntity, Long> {

    private final RestaurantMenuRepository rmr;
    private final RestaurantMenuMealsRepository rmmr;

    @Autowired
    public RestaurantMenuRestController(RestaurantMenuRepository rmr,  RestaurantMenuMealsRepository rmmr) {
        super(rmr);
        this.rmr = rmr;
        this.rmmr = rmmr;
    }

    @Override
    public Page<RestaurantMenuEntity> getAllItems(Pageable pageable) {
        List<RestaurantMenuEntity> entities = rmr.findAllByDayDateAndDomainId( RestaurantMenuService.getMenuDate( getRequest().getParameter("searchDay") ), CloudToolsForCore.getDomainId() );

        //sort entities by dayDate, meal.cathegory, priority
        entities.sort( (e1, e2) -> {
            int result = e1.getDayDate().compareTo(e2.getDayDate());
            if(result == 0) result = e1.getMeal().getCathegory().compareTo(e2.getMeal().getCathegory());
            if(result == 0) result = e1.getPriority().compareTo(e2.getPriority());
            return result;
        });

        processFromEntity(entities, ProcessItemAction.GETALL);
        DatatablePageImpl<RestaurantMenuEntity> page = new DatatablePageImpl<>( RestaurantMenuService.sortMenu( entities, true ) );
        page.addOptions("editorFields.mealAlergens", RestaurantMenuService.getAlergenBeans( getRequest() ), "alergenName", "alergenNumber", false);
        page.addOptions("editorFields.selectedMealId", rmmr.findAll(), "name", "id", false);
        return page;
    }

    @Override
    public Page<RestaurantMenuEntity> searchItem(Map<String, String> params, Pageable pageable, RestaurantMenuEntity search) {
        RestaurantMenuService.processParams(params);

        return new PageImpl<>(  //Return new page 4.
            RestaurantMenuService.sortMenu( //Sort this list 3.
                new ArrayList<RestaurantMenuEntity>( //Create new list, because Page.getContent() returns unmodifiable list 2.
                    super.searchItem(params, null, search).getContent() //Call super to get data 1.
                ), true
            )
        );
    }

    @Override
    public RestaurantMenuEntity processFromEntity(RestaurantMenuEntity entity, ProcessItemAction action) {
        RestaurantMenuEditorFields rmef = entity.getEditorFields();
        if(rmef == null) rmef = new RestaurantMenuEditorFields();
        rmef.fromRestaurantMenuEntity(entity, getProp());
        return entity;
    }

    @Override
    public RestaurantMenuEntity processToEntity(RestaurantMenuEntity entity, ProcessItemAction action) {
        if(entity != null) {
            RestaurantMenuEditorFields rmef = entity.getEditorFields();
            rmef.toRestaurantMenuEntity(entity, rmmr);
        }
        return entity;
    }

    @Override
	public void beforeSave(RestaurantMenuEntity entity) {
        //Set domain id
        if(entity.getId() == null || entity.getId() == -1)
            entity.setDomainId(CloudToolsForCore.getDomainId());

        //Setd default priority
        if(entity.getPriority() == null) entity.setPriority(0);
    }

    @RequestMapping(value = "/getMaxPriority", params={"cathegory","dayDate"})
    @ResponseBody
    public Integer getMaxPriority(@RequestParam("cathegory") String cathegory, @RequestParam("dayDate") Date dayDate) {
        if(Tools.isEmpty(cathegory) || dayDate == null) return 10;
        Optional<Integer> maxPriority = rmr.findMaxPriorityByCathegory(cathegory, RestaurantMenuService.getMenuDate(dayDate), CloudToolsForCore.getDomainId());
        return maxPriority.isPresent() ? maxPriority.get() + 10 : 10;
    }
}
