package sk.iway.iwcm.components.basket.jpa;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Projects invoice values required to calculate basket statistics.
 */
public interface BasketInvoiceStatsProjection {
    Date getCreateDate();
    Integer getStatusId();
    String getDeliveryMethod();
    String getPaymentMethod();
    BigDecimal getPriceToPayVat();
    BigDecimal getPriceToPayNoVat();
    String getCurrency();
}
