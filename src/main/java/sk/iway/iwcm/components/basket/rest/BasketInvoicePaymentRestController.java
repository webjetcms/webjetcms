package sk.iway.iwcm.components.basket.rest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.BasketTools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEditorFields;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentsRepository;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicesRepository;
import sk.iway.iwcm.components.basket.jpa.InvoicePaymentStatus;
import sk.iway.iwcm.components.basket.payment_methods.jpa.RefundationState;
import sk.iway.iwcm.components.basket.payment_methods.jpa.RefundationState.RefundationStatus;
import sk.iway.iwcm.components.basket.payment_methods.rest.BasePaymentMethod;
import sk.iway.iwcm.components.basket.payment_methods.rest.PaymentMethodsService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;

@RestController
@RequestMapping("/admin/rest/eshop/basket-payments")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_basket')")
@Datatable
public class BasketInvoicePaymentRestController extends DatatableRestControllerV2<BasketInvoicePaymentEntity, Long> {

    private final BasketInvoicePaymentsRepository bipr;
    private final BasketInvoicesRepository bir;
    private final BasketInvoiceItemsRepository biir;
    private static final BigDecimal MIN_PAYED_PRICE = BigDecimal.valueOf(0.01);
    private static final String DO_NOT_SUPPORT_EDIT_ERR = "components.basket.invoice_payments.do_not_support_edit_err";

    private final PaymentMethodsService paymentMethodsService;

    @Autowired
    public BasketInvoicePaymentRestController(BasketInvoicePaymentsRepository bipr, BasketInvoicesRepository bir, BasketInvoiceItemsRepository biir, PaymentMethodsService paymentMethodsService) {
        super(bipr);
        this.bipr = bipr;
        this.bir = bir;
        this.biir = biir;
        this.paymentMethodsService = paymentMethodsService;
    }

    @Override
    public Page<BasketInvoicePaymentEntity> getAllItems(Pageable pageable) {
        long invoiceId = getInvoiceId();
        if(invoiceId < 1)  return new sk.iway.iwcm.system.datatable.DatatablePageImpl<>(new ArrayList<>());
        BasketInvoiceEntity invoice = bir.findFirstByIdAndDomainId(invoiceId, CloudToolsForCore.getDomainId()).orElse(null);
        if(invoice == null) return new sk.iway.iwcm.system.datatable.DatatablePageImpl<>(new ArrayList<>());

        DatatablePageImpl<BasketInvoicePaymentEntity> page = new DatatablePageImpl<>( bipr.findAllByInvoiceId(invoiceId, pageable) );
        processFromEntity(page, ProcessItemAction.GETALL);

        //Set payment methods
        page.addOptions("paymentMethod", paymentMethodsService.getPaymentOptions(getProp()), "label", "value", false);
        page.addOptions("paymentStatus", InvoicePaymentStatus.getOptions(getProp()), "label", "value", false);
        return page;
    }

    @Override
    public BasketInvoicePaymentEntity getOneItem(long id) {
        BasketInvoicePaymentEntity entity;
        if(id < 1) {
            entity = new BasketInvoicePaymentEntity();
            entity.setCreateDate(new Date());
        } else {
            entity = bipr.getById(id);
        }

        processFromEntity(entity, ProcessItemAction.GETONE);
        return entity;
    }

    @Override
    public BasketInvoicePaymentEntity processFromEntity(BasketInvoicePaymentEntity entity, ProcessItemAction action) {
        if(entity.getEditorFields() == null) {
            BasketInvoicePaymentEditorFields bipef = new BasketInvoicePaymentEditorFields();
            bipef.fromBasketInvoicePayment(entity, MIN_PAYED_PRICE, action);
        }
        entity.setCurrency(BasketTools.getSystemCurrency());
        return entity;
    }

    @Override
    public BasketInvoicePaymentEntity processToEntity(BasketInvoicePaymentEntity entity, ProcessItemAction action) {
        if(entity != null) entity.getEditorFields().toBasketInvoicePayment(entity, getInvoiceId(), action);
        return entity;
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, BasketInvoicePaymentEntity> target, Identity currentUser, Errors errors, Long id, BasketInvoicePaymentEntity entity) {
        if("create".equals(target.getAction()) || "edit".equals(target.getAction())) {
            boolean saveAsRefund = false;
            if(entity.getEditorFields() != null)
                saveAsRefund = Tools.isTrue(entity.getEditorFields().getSaveAsRefund());

            if(saveAsRefund == false && (entity.getPayedPrice() == null || entity.getPayedPrice().compareTo(MIN_PAYED_PRICE) < 0))
                errors.rejectValue("errorField.payedPrice", "", getProp().getText("components.basket.invoice.payments.price_err"));

            if(saveAsRefund == true && (entity.getPayedPrice() == null || entity.getPayedPrice().compareTo(MIN_PAYED_PRICE) > 0))
                errors.rejectValue("errorField.payedPrice", "", getProp().getText("components.invoice_payment.refund_price_err"));

            //Check if payment method is editable
            if(paymentMethodsService.isPaymentMethodEditableByAdmin(entity) == false)
                errors.reject("", getProp().getText(DO_NOT_SUPPORT_EDIT_ERR));

            if(errors.getErrorCount() < 1) {
                //Validate payment price
                String result = InvoiceService.validatePayment(entity, getInvoiceId(), bipr, bir, biir);
                if(result != null) throwError(result);
            }
        }
    }

    @Override
    public boolean beforeDelete(BasketInvoicePaymentEntity entity) {
        //Check if payment method is editable
        if(paymentMethodsService.isPaymentMethodEditableByAdmin(entity) == false) {
            throwError(getProp().getText(DO_NOT_SUPPORT_EDIT_ERR));
            return false;
        }
        return true;
    }

    @Override
    public void afterSave(BasketInvoicePaymentEntity entity, BasketInvoicePaymentEntity saved) {
        //After save we need to update invoice status
        ProductListService.updateInvoiceStats(entity.getInvoiceId(), true);
    }

    @Override
    public void afterDelete(BasketInvoicePaymentEntity entity, long id) {
        //After delete we need to update invoice status
        ProductListService.updateInvoiceStats(entity.getInvoiceId(), true);
    }

    private final long getInvoiceId() {
        return Tools.getLongValue(getRequest().getParameter("invoiceId"), -1);
    }

    @Override
    public boolean processAction(BasketInvoicePaymentEntity entity, String action) {

        if("refundPayment".equals(action)) {
            Prop prop = getProp();
            BigDecimal refundAmount = Tools.getBigDecimalValue(getRequest().getParameter("customData"), "-1");

            //Call refund method and keep result
            RefundationState result = paymentMethodsService.refundPayment(refundAmount, entity, getRequest());
            RefundationStatus status = result.getStatus();

            if(status == RefundationStatus.SUCCESS) {
                //All GOOD
                addNotify(new NotifyBean(prop.getText(BasePaymentMethod.REFUNDATION_TITLE), prop.getText(BasePaymentMethod.REFUNDATION_SUCCESS), NotifyType.SUCCESS, 15000));
            } else {
                //ERROR was returned

                if(status == RefundationStatus.FAIL)
                    addNotify(new NotifyBean(prop.getText(BasePaymentMethod.REFUNDATION_TITLE), prop.getText(result.getMsgKey()) + " " + result.getBonusText(), NotifyType.ERROR, 15000));
                else
                    addNotify(new NotifyBean(prop.getText(BasePaymentMethod.REFUNDATION_TITLE), prop.getText(result.getMsgKey()), NotifyType.ERROR, 15000));
            }
        }

        return true;
    }
}
