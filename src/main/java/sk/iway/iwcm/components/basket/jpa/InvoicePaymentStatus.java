package sk.iway.iwcm.components.basket.jpa;

import java.util.ArrayList;
import java.util.List;

import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValue;

/**
 * Enum for invoice payment status in datatable.
 * 
 * <p> FAIL - Failed payment
 * <p> SUCCESS - Successful payment
 * <p> REFUND_SUCCESS - Successful refund
 * <p> REFUND_FAIL - Failed refund
 * <p> REFUNDED - Refunded payment (fully)
 * <p> PARTIALLY_REFUNDED - Refunded payment (partially)
 * <p> UKNOWN - Unknown status
 */
public enum InvoicePaymentStatus {
    FAIL(0),                
    SUCCESS(1),             
    REFUND_SUCCESS(2),      
    REFUND_FAIL(3),         
    REFUNDED(4),            
    PARTIALLY_REFUNDED(5),  
    UNKNOWN(-1);

    private final int code;
    private static final String KEY_PREFIX  = "components.basket.invoice_payments.status.";

    InvoicePaymentStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static InvoicePaymentStatus getInvoicePaymentStatus(int invoicePaymentStatusCode) {
        switch(invoicePaymentStatusCode) {
            case 0: return FAIL;
            case 1: return SUCCESS;
            case 2: return REFUND_SUCCESS;
            case 3: return REFUND_FAIL;
            case 4: return REFUNDED;
            case 5: return PARTIALLY_REFUNDED;
            default: return UNKNOWN;
        }
    }

    public static List<LabelValue> getOptions(Prop prop) {
        List<LabelValue> values = new ArrayList<>();

        for (InvoicePaymentStatus status : InvoicePaymentStatus.values()) {
            String label = prop.getText(KEY_PREFIX + status.name().toLowerCase());
            values.add(new LabelValue(label, String.valueOf(status.getCode())));
        }
        return values;
    }
    
}