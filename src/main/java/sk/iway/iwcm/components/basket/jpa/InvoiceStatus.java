package sk.iway.iwcm.components.basket.jpa;

/**
 * Enum representing the status of an invoice.
 * <p>
 * This enum is used to represent various states of an invoice in the system.
 * Each status is associated with a specific integer value.
 * </p>
 * @param value the integer value associated with the invoice status
 * @return the integer value of the invoice status
 */
public enum InvoiceStatus {

    INVOICE_STATUS_UNKNOWN(-1),
    INVOICE_STATUS_NEW(1),
    INVOICE_STATUS_PAID(2),
    INVOICE_STATUS_CANCELLED(3),
    INVOICE_STATUS_PARTIALLY_PAID(4),
    INVOICE_STATUS_ISSUED(5),
    INVOICE_STATUS_DEPOSIT_PAID(8);

    private final int value;

    InvoiceStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static InvoiceStatus fromValue(Integer value) {
        if (value == null) {
            return INVOICE_STATUS_UNKNOWN;
        }
        int intValue = value.intValue();
        for (InvoiceStatus status : InvoiceStatus.values()) {
            if (status.value == intValue) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown InvoiceStatus value: " + value);
    }
}
