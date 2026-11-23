package sk.iway.iwcm.components.basket.jpa;

/**
 * Projects aggregated product quantities for basket statistics.
 */
public interface BasketProductStatsProjection {
    Integer getItemId();
    String getItemTitle();
    Long getQuantity();
}
