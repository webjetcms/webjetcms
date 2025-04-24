package sk.iway.iwcm.components.slider.rest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.components.slider.jpa.SliderItemsEntity;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/slider/items")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_slider')")
@Datatable
public class SliderRestController extends DatatableRestControllerV2<SliderItemsEntity, Long> {

    public SliderRestController() {
        super(null); // nevyuzivame databazovy repository
    }

    @Override
    public Page<SliderItemsEntity> getAllItems(Pageable pageable) {
        List<SliderItemsEntity> emptyList = new ArrayList<>();
        return new PageImpl<>(emptyList, pageable, 0);
    }

    @Override
    public SliderItemsEntity getOneItem(long id) {
        SliderItemsEntity entity = new SliderItemsEntity();
        return processFromEntity(entity, ProcessItemAction.CREATE);
    }

    @Override
    public SliderItemsEntity processFromEntity(SliderItemsEntity entity, ProcessItemAction action) {
        return entity;
    }

    @Override
    public SliderItemsEntity insertItem(SliderItemsEntity entity) {
        return entity;
    }
}
