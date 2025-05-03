package sk.iway.iwcm.components.basket.payment_methods.jpa;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.basket.jpa.InvoicePaymentStatus;

@Getter
@Setter
public class RefundationState {

    public RefundationState(RefundationStatus status, String msgKey) {
        this.status = status;
        this.msgKey = msgKey;
        this.bonusText = "";
    }

    public RefundationState(RefundationStatus status, String msgKey, String bonusText) {
        this.status = status;
        this.msgKey = msgKey;
        this.bonusText = bonusText;
    }

    /**
     * Enum representing refundation status after refundation is processed.
     *
     * <p> SUCCESS - Refundation preceded successfully - all good
     * <p> FAIL    - Refundation failed on BANK side
     * <p> ERROR   - Refundation failed on OUR side
     */
    public enum RefundationStatus {
        SUCCESS,
        FAIL,
        ERROR
    }

    private RefundationStatus status;
    private String msgKey;
    private String bonusText;

    //Status of refunded payment after refundation
    private InvoicePaymentStatus statusAfterRefund;

    public String getBonusText() {
        if(bonusText == null) {
            return "";
        }
        return bonusText;
    }
}
