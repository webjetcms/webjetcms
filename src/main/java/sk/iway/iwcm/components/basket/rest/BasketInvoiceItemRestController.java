package sk.iway.iwcm.components.basket.rest;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.delivery_methods.rest.DeliveryMethodsService;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEditorFields;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.payment_methods.rest.PaymentMethodsService;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@RestController
@RequestMapping("/admin/rest/eshop/basket-items")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_basket')")
@Datatable
public class BasketInvoiceItemRestController extends DatatableRestControllerV2<BasketInvoiceItemEntity, Long> {

    private final BasketInvoiceItemsRepository basketInvoiceItemsRepository;

    private final PaymentMethodsService pms;
    private final DeliveryMethodsService dms;

    private List<LabelValue> paymentMethods;
    private List<LabelValue> deliveryMethods;

    @Autowired
    public BasketInvoiceItemRestController(BasketInvoiceItemsRepository basketInvoiceItemsRepository, PaymentMethodsService pms, DeliveryMethodsService dms) {
        super(basketInvoiceItemsRepository);
        this.basketInvoiceItemsRepository = basketInvoiceItemsRepository;
        this.pms = pms;
        this.dms = dms;
    }

    @Override
    public Page<BasketInvoiceItemEntity> getAllItems(Pageable pageable) {
        long invoiceId = getInvoiceId();
        if(invoiceId < 1)  return new PageImpl<>(new ArrayList<>());

        Page<BasketInvoiceItemEntity> page = basketInvoiceItemsRepository.findAllByInvoiceIdAndDomainId(invoiceId, CloudToolsForCore.getDomainId(), pageable);

        paymentMethods = pms.getPaymentOptions(getProp());
        deliveryMethods = dms.getDeliveryOptions(getProp());

        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public BasketInvoiceItemEntity processFromEntity(BasketInvoiceItemEntity entity, ProcessItemAction action) {
        if(entity.getEditorFields() == null) {
           BasketInvoiceItemEditorFields biief = new BasketInvoiceItemEditorFields();
           biief.fromBasketInvoiceItem(entity, paymentMethods, deliveryMethods);
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
        ProductListService.updateInvoiceStats(Long.valueOf(entity.getInvoiceId()), true);
    }

    @Override
    public void afterDelete(BasketInvoiceItemEntity entity, long id) {
        //Update invoice stats
        ProductListService.updateInvoiceStats(Long.valueOf(entity.getInvoiceId()), true);
    }

    @Override
    public boolean processAction(BasketInvoiceItemEntity entity, String action) {
        if("addItem".equals(action)) {
            // !! IGNORE ENTITY - this action do not work with BasketInvoiceItemEntity
            String customData = getRequest().getParameter("customData");
            Long invoiceId;
            List<Integer> itemIdsToAdd;

            if(customData != null && !customData.isEmpty()) {
                try {
                    JSONObject jsonObject = new JSONObject(customData);
                    invoiceId = Tools.getLongValue(jsonObject.get("invoiceId").toString(), -1);
                    //String itemIdsStr = jsonObject.getJSONArray("selectedIds");

                    itemIdsToAdd = jsonObject.getJSONArray("selectedIds").toList().stream()
                        .map(obj -> Integer.valueOf(obj.toString()))
                        .toList();

                    ProductListService.addItemToInvoice(invoiceId, itemIdsToAdd, basketInvoiceItemsRepository, getUser().getUserId(), getRequest());

                } catch (Exception ex) {
                    sk.iway.iwcm.Logger.error(ex);

                }
            }
        }
        return true;
    }

    private final long getInvoiceId() {
        return Tools.getLongValue(getRequest().getParameter("invoiceId"), -1);
    }
}
