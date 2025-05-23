package sk.iway.iwcm.components.basket.jpa;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class BasketInvoicePaymentEditorFields extends BaseEditorFields {
    
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title="components.invoice_payment.save_as_refund",
        hidden = true,
        sortAfter = "confirmed"
    )
    private Boolean saveAsRefund;

    public void fromBasketInvoicePayment(BasketInvoicePaymentEntity originalEntity, BigDecimal minPayedPrice, ProcessItemAction action) {

        if(ProcessItemAction.GETONE.equals(action)) {
            if(originalEntity.getPayedPrice() != null && originalEntity.getPayedPrice().compareTo(minPayedPrice) < 0)
                saveAsRefund = true;
            
            //While creating new payment, pre set confirmed to true
            if(originalEntity.getId() == null || originalEntity.getId() < 1)
                originalEntity.setConfirmed(true);
        }   
        
        if(Tools.isFalse(originalEntity.getConfirmed())) {
            addRowClass("payment-not-confirmed");
        }

        originalEntity.setEditorFields(this);
    }

    public void toBasketInvoicePayment(BasketInvoicePaymentEntity originalEntity, long requestBasketInvoiceId, ProcessItemAction action) {
        if(ProcessItemAction.CREATE.equals(action)) {
            //Set date of creation
            if(originalEntity.getCreateDate() == null) originalEntity.setCreateDate(new Date());

            //Its inner table, invoiceId is send as request param
            originalEntity.setInvoiceId(requestBasketInvoiceId);
        }

        //If its confirmed payment, set also close date
        if(Tools.isTrue(originalEntity.getConfirmed()))
            originalEntity.setClosedDate(new Date());

        //
        setPaymentInfo(originalEntity);
    }

    private void setPaymentInfo(BasketInvoicePaymentEntity entity) {
        if(Tools.isTrue(entity.getEditorFields().getSaveAsRefund())) {
            //Paymnet of type REFUND
            entity.setPaymentStatus( Tools.isTrue(entity.getConfirmed()) ? InvoicePaymentStatus.REFUND_SUCCESS.getCode() : InvoicePaymentStatus.UNKNOWN.getCode());
        } else {
            //Basic payment
            entity.setPaymentStatus( Tools.isTrue(entity.getConfirmed()) ? InvoicePaymentStatus.SUCCESS.getCode() : InvoicePaymentStatus.UNKNOWN.getCode());
        }

        if(Tools.isTrue(entity.getConfirmed())) {
            entity.setClosedDate(new Date());
        } else {
            entity.setClosedDate(null);
        }
    }
}