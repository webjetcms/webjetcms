package sk.iway.iwcm.components.basket.rest;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEditorFields;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/eshop/basket")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_basket')")
@Datatable
public class BasketInvoiceRestController extends DatatableRestControllerV2<BasketInvoiceEntity, Long> {

    private final BasketInvoicesRepository basketInvoicesRepository;
    private static final String ORDER_PLACEHOLDER = "{ORDER_DETAILS}";
    private static final String STATUS_PLACEHOLDER = "{STATUS}";

    @Autowired
    public BasketInvoiceRestController(BasketInvoicesRepository basketInvoicesRepository) {
        super(basketInvoicesRepository);
        this.basketInvoicesRepository = basketInvoicesRepository;
    }
    @Override
    public Page<BasketInvoiceEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<BasketInvoiceEntity> page = new DatatablePageImpl<>(basketInvoicesRepository.findAllByDomainId(CloudToolsForCore.getDomainId(), pageable));
        fillStatusSelect(page);
        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public void afterSave(BasketInvoiceEntity entity, BasketInvoiceEntity saved) {
        BasketInvoiceEditorFields bied = entity.getEditorFields();
        if(bied != null && Boolean.TRUE.equals(bied.getSendNotification())) {
            StringBuilder sb = new StringBuilder();
            //Styles
            sb.append(bied.getOrderRecapHead());
            //Email body AND replace "{ORDER_DETAILS}" with actual order details
            sb.append( bied.getBody().replace(ORDER_PLACEHOLDER, bied.getOrderRecapBody()) );
            //
            Integer actualStatus = basketInvoicesRepository.getStatusId(saved.getId(), CloudToolsForCore.getDomainId());
            sb.replace(sb.indexOf(STATUS_PLACEHOLDER), sb.indexOf(STATUS_PLACEHOLDER) + STATUS_PLACEHOLDER.length(), actualStatus.toString());

            boolean emailSendOK = SendMail.send(getUser().getFullName(), getUser().getEmail(), entity.getContactEmail(), bied.getSubject(), sb.toString(), getRequest());
            if(!emailSendOK) throwError("components.basket.errorSendingEmail");
        }
    }

    @Override
    public BasketInvoiceEntity processFromEntity(BasketInvoiceEntity entity, ProcessItemAction action) {
        if(entity.getEditorFields() == null) {
           BasketInvoiceEditorFields bief = new BasketInvoiceEditorFields();
           bief.fromBasketInvoice(entity, getRequest());
        }
        return entity;
    }

    @Override
    public boolean deleteItem(BasketInvoiceEntity entity, long id) {
        //DELETE action is allowed only if basketInvoiced is set as CANCELLED
        if(entity.getStatusId().equals(BasketInvoiceEntity.INVOICE_STATUS_CANCELLED) ) {
            return super.deleteItem(entity, id);
        } else {
            //Reject delete
            throwError("components.basket.invoice.delete_err");
            return false;
        }
    }

    @Override
    public BasketInvoiceEntity insertItem(BasketInvoiceEntity entity) {
        throwError(getProp().getText("config.not_permitted_action_err"));
        return null;
    }

    @Override
    public void beforeDuplicate(BasketInvoiceEntity entity) {
        throwError(getProp().getText("config.not_permitted_action_err"));
    }

    private final void fillStatusSelect(DatatablePageImpl<BasketInvoiceEntity> page) {
        String label = "statusId";
        Prop prop = getProp();

        //Add default statuses
        String defaultKeyPrefix = "components.basket.invoice.status.";
        page.addDefaultOption(label, prop.getText( defaultKeyPrefix + "1" ), "1");
        page.addDefaultOption(label, prop.getText( defaultKeyPrefix + "2" ), "2");
        page.addDefaultOption(label, prop.getText( defaultKeyPrefix + "3" ), "3");
        page.addDefaultOption(label, prop.getText( defaultKeyPrefix + "4" ), "4");
        page.addDefaultOption(label, prop.getText( defaultKeyPrefix + "5" ), "5");
        page.addDefaultOption(label, prop.getText( defaultKeyPrefix + "8" ), "8");

        //Add custom statuses
        Map<String, String> bonusStatuses = Constants.getHashtable("basketInvoiceBonusStatuses");
        for (Map.Entry<String, String> entry : bonusStatuses.entrySet()) {
            //It must be number 10 or higher, lower numbers are reserved for default statuses
            if(Integer.valueOf(entry.getKey()) >= 10)
                page.addDefaultOption(label, prop.getText( entry.getValue() ), entry.getKey());
        }
    }
}