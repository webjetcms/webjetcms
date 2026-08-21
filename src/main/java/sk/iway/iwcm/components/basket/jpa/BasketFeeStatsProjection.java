package sk.iway.iwcm.components.basket.jpa;

import java.math.BigDecimal;

/**
 * Projects invoice fee data required to calculate basket statistics.
 */
public interface BasketFeeStatsProjection {
    Integer getItemId();
    String getItemNote();
    BigDecimal getItemPrice();
    Integer getItemQty();
    Integer getItemVat();
    String getCurrency();
    String getUserLng();
}
