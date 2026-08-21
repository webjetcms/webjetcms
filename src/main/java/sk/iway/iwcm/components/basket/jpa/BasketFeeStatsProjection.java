package sk.iway.iwcm.components.basket.jpa;

import java.math.BigDecimal;

public interface BasketFeeStatsProjection {
    Integer getItemId();
    String getItemNote();
    BigDecimal getItemPrice();
    Integer getItemQty();
    Integer getItemVat();
    String getCurrency();
    String getUserLng();
}
