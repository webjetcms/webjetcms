package sk.iway.iwcm.components.basket.rest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Carries aggregate invoice, revenue, product, category, and fee statistics for the basket dashboard.
 */
@Getter
@Setter
@NoArgsConstructor
public class BasketStatsDTO {

    private long invoiceCount;
    private long soldItemCount;
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal averageInvoiceValue = BigDecimal.ZERO;
    private BigDecimal averageItemsPerInvoice = BigDecimal.ZERO;
    private BigDecimal deliveryFees = BigDecimal.ZERO;
    private BigDecimal paymentFees = BigDecimal.ZERO;
    private BigDecimal netRevenue = BigDecimal.ZERO;
    private String currency;
    private List<SalesTimelinePoint> salesTimeline = new ArrayList<>();
    private List<NameCount> topProducts = new ArrayList<>();
    private CategoryNode categoryTree;
    private List<NameCount> deliveryMethods = new ArrayList<>();
    private List<NameCount> paymentMethods = new ArrayList<>();
    private List<NameCount> invoiceStatuses = new ArrayList<>();

    /**
     * Represents revenue totals for one calendar day in the sales timeline.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesTimelinePoint {

        private Date dayDate;
        private BigDecimal revenueWithVat;
        private BigDecimal revenueWithoutVat;
    }

    /**
     * Associates a display name with an aggregated occurrence or item count.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NameCount {

        private String name;
        private long count;
    }

    /**
     * Represents one node in the hierarchical product-category statistics.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryNode {

        private String name;
        private Long value;
        private List<CategoryNode> children = new ArrayList<>();
    }
}
