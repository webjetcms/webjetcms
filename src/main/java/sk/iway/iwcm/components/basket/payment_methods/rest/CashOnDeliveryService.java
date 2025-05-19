package sk.iway.iwcm.components.basket.payment_methods.rest;

import java.math.BigDecimal;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;
import sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentFieldMapAttr;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentMethodEntity;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentMethodRepository;
import sk.iway.iwcm.components.basket.payment_methods.jpa.PaymentState;
import sk.iway.iwcm.components.basket.payment_methods.jpa.RefundationState;
import sk.iway.iwcm.components.basket.payment_methods.jpa.RefundationState.RefundationStatus;
import sk.iway.iwcm.editor.FieldType;

@Service
@PaymentMethod(
    nameKey = "components.basket.editor.cash_on_delivery",
    fieldMap = {
        @PaymentFieldMapAttr(fieldAlphabet = 'A', fieldType = FieldType.NUMBER, fieldLabel = "components.basket.invoice_payments.price", isRequired = true, defaultValue = "0"),
        @PaymentFieldMapAttr(fieldAlphabet = 'B', fieldType = FieldType.NUMBER, fieldLabel = "components.basket.invoice_payments.vat", isRequired = true, defaultValue = "0"),
        @PaymentFieldMapAttr(fieldAlphabet = 'C', fieldType = FieldType.QUILL, fieldLabel = "components.payment_methods.mmoney_transfer_note", isRequired = false),
        @PaymentFieldMapAttr(fieldAlphabet = 'D', fieldType = FieldType.BOOLEAN_TEXT, fieldLabel = "components.payment_methods.allow_admin_edit", isRequired = false, defaultValue = "false"),
})
public class CashOnDeliveryService extends BasePaymentMethod {

    @Override
    protected RefundationState doPaymentRefund(BigDecimal refundAmount, BasketInvoicePaymentEntity paymentEntity, HttpServletRequest request) {
        // CANT DO REFUND
        return new RefundationState(RefundationStatus.ERROR, NON_REFUNDABLE_METHOD, null);
    }

    @Override
    protected String getPaymentResponse(Long invoiceId, String returnUrl, HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("<span> Vašu objednávku uhradíte pri prebratí zásielky. </span>");

        PaymentMethodRepository pmr = Tools.getSpringBean("paymentMethodRepository", PaymentMethodRepository.class);
        PaymentMethodEntity paymentMethod = pmr.findByPaymentMethodNameAndDomainId(this.getClass().getName(), CloudToolsForCore.getDomainId());
        if(paymentMethod == null) return adminLogError("PaymentMethodEntity " + this.getClass().getName() + " wasnt found in DB.", request);
        else {
            sb.append(paymentMethod.getFieldC());
        }

        return sb.toString();
    }

    @Override
    protected BasketInvoiceItemEntity getPaymentMethodCost(BasketInvoiceItemEntity entity, PaymentMethodEntity paymentMethod) {
        if(paymentMethod == null) return null;
        entity.setItemPrice( new BigDecimal(paymentMethod.getFieldA()) );
        entity.setItemQty(1);
        entity.setItemVat( Integer.valueOf(paymentMethod.getFieldB()) );
        return entity;
    }

    @Override
    protected PaymentState getPaymentState(HttpServletRequest request) {
        // NO STATE - PAYMENT GATE IS NOT USED
        return null;
    }

    @Override
    protected boolean isEditableByAdmin(PaymentMethodEntity paymentMethod) {
        if(paymentMethod == null) return true;
        return Tools.getBooleanValue(paymentMethod.getFieldD(), true);
    }
}
