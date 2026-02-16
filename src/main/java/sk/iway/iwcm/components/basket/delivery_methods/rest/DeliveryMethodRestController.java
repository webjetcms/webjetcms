package sk.iway.iwcm.components.basket.delivery_methods.rest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodEntity;
import sk.iway.iwcm.components.basket.delivery_methods.jpa.DeliveryMethodsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@RestController
@RequestMapping("/admin/rest/eshop/delivery-methods")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_basket')")
@Datatable
public class DeliveryMethodRestController extends DatatableRestControllerV2<DeliveryMethodEntity, Long> {

    private final DeliveryMethodsService deliveryMethodsService;

    public DeliveryMethodRestController(DeliveryMethodsService deliveryMethodsService, DeliveryMethodsRepository repo) {
        super(repo);
        this.deliveryMethodsService = deliveryMethodsService;
    }

    @Override
    public Page<DeliveryMethodEntity> getAllItems(Pageable pageable) {
        Page<DeliveryMethodEntity> page = new DatatablePageImpl<>( deliveryMethodsService.getAllDeliveryMethods(getRequest(), getProp(), false) );
        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<DeliveryMethodEntity> root, CriteriaBuilder builder) {
        super.addSpecSearch(params, predicates, root, builder);

        String country = getRequest().getParameter("searchSupportedCountries");
        if(Tools.isNotEmpty(country)) {
            predicates.add(
                builder.and(
                    builder.like(
                        builder.concat(
                            builder.concat(builder.literal("+"), root.get("supportedCountriesStr")),
                            builder.literal("+")
                        ), "%+" + country + "+%"
                    )
                )
            );
        }
    }

    @Override
    public DeliveryMethodEntity getOneItem(long id) {
        String deliveryMethodName = Tools.getStringValue(getRequest().getParameter("deliveryMethod"), "");
        DeliveryMethodEntity entity = deliveryMethodsService.getDeliveryMethod(id, deliveryMethodName, getProp());
        processFromEntity(entity, null);
        return entity;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, DeliveryMethodEntity> target, Identity user, Errors errors, Long id, DeliveryMethodEntity entity) {
        if ("remove".equals(target.getAction())) return;

        //Optional fields CAN'T validate required fields, we need to do it manually
        deliveryMethodsService.validateEditorValues(entity, errors, getProp());

        super.validateEditor(request, target, user, errors, id, entity);
    }

    @Override
    public void getOptions(DatatablePageImpl<DeliveryMethodEntity> page) {
        deliveryMethodsService.setOptions(page, getProp());
    }

    @Override
    public DeliveryMethodEntity processFromEntity(DeliveryMethodEntity entity, ProcessItemAction action) {
        entity.setSupportedCountries( entity.getSupportedCountriesArr() );
        return entity;
    }

    @Override
    public DeliveryMethodEntity processToEntity(DeliveryMethodEntity entity, ProcessItemAction action) {
        entity.setSupportedCountriesStr( String.join("+", entity.getSupportedCountries()) );

        if(entity.getPrice() == null) entity.setPrice(BigDecimal.ZERO);
        if(entity.getVat() == null) entity.setVat(0);

        return entity;
    }

    @GetMapping("/supported-deliveries")
    public List<LabelValue> getSupportedDeliveryMethods() {
        return deliveryMethodsService.getDeliveryOptionsClasses(getProp());
    }
}
