package sk.iway.iwcm.components.basket.payment_methods.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentState {
    /**
     * Enum representing payment status after payment is processed.
     *
     * <p> SUCCESS - Payment is paid successfully - all good
     * <p> FAIL    - Payment failed on BANK side
     * <p> ERROR   - Payment failed on OUR side
     */
    public enum PaymentStatus {
        SUCCESS,
        FAIL,
        ERROR
    }

    private String paymentId;
    private String paymentDescription;
    private PaymentStatus status;

    private Long invoiceId;
    private Date paymentDateTime;
}