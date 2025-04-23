package sk.iway.iwcm.components.basket.rest;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEditorFields;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/eshop/basket-items")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_basket')")
@Datatable
public class BasketInvoiceItemRestController extends DatatableRestControllerV2<BasketInvoiceItemEntity, Long> {

    private final BasketInvoiceItemsRepository basketInvoiceItemsRepository;

    @Autowired
    public BasketInvoiceItemRestController(BasketInvoiceItemsRepository basketInvoiceItemsRepository) {
        super(basketInvoiceItemsRepository);
        this.basketInvoiceItemsRepository = basketInvoiceItemsRepository;
    }

    @Override
    public Page<BasketInvoiceItemEntity> getAllItems(Pageable pageable) {
        long invoiceId = getInvoiceId();
        if(invoiceId < 1)  return new PageImpl<>(new ArrayList<>());

        Page<BasketInvoiceItemEntity> page = basketInvoiceItemsRepository.findAllByInvoiceIdAndDomainId(invoiceId, CloudToolsForCore.getDomainId(), pageable);
        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public BasketInvoiceItemEntity processFromEntity(BasketInvoiceItemEntity entity, ProcessItemAction action) {
        if(entity.getEditorFields() == null) {
           BasketInvoiceItemEditorFields biief = new BasketInvoiceItemEditorFields();
           biief.fromBasketInvoiceItem(entity);
        }
        return entity;
    }

    @Override
    public BasketInvoiceItemEntity insertItem(BasketInvoiceItemEntity entity) {
        throwError(getProp().getText("config.not_permitted_action_err"));
        return null;
    }

    @Override
    public void beforeDuplicate(BasketInvoiceItemEntity entity) {
        throwError(getProp().getText("config.not_permitted_action_err"));
    }

    @Override
    public void afterSave(BasketInvoiceItemEntity entity, BasketInvoiceItemEntity saved) {
        //Update invoice stats
        ProductListService.updateInvoiceStats(entity.getInvoiceId());
    }

    private final long getInvoiceId() {
        return Tools.getLongValue(getRequest().getParameter("invoiceId"), -1);
    }
}
