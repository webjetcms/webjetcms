package sk.iway.iwcm.components.basket.jpa;

/**
 * Represents the persisted states of a basket invoice and their numeric identifiers.
 */
public enum InvoiceStatus {

    INVOICE_STATUS_UNKNOWN(-1),
    INVOICE_STATUS_NEW(1),
    INVOICE_STATUS_PAID(2),
    INVOICE_STATUS_CANCELLED(3),
    INVOICE_STATUS_PARTIALLY_PAID(4),
    INVOICE_STATUS_ISSUED(5),
    INVOICE_STATUS_DEPOSIT_PAID(8);

    public static final String STATUS_KEY_PREFIX = "components.basket.invoice.status.";

    private final int value;

    InvoiceStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Resolves an invoice status from its persisted numeric value.
     *
     * @param value  persisted status value; {@code null} represents an unknown status
     * @return the matching status, or {@link #INVOICE_STATUS_UNKNOWN} for {@code null}
     * @throws IllegalArgumentException if the value has no matching status
     */
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
