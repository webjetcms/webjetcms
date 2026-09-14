package sk.iway.iwcm.components.basket.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;

/** Covers selling-unit rounding, small prices and reconciled VAT for a mixed basket. */
class BasketPricingServiceTest {

    private String originalFormat;

    @BeforeEach
    void setUp() {
        originalFormat = Constants.getString("currencyFormat");
        Constants.setString("currencyFormat", "0.00");
    }

    @AfterEach
    void tearDown() {
        Constants.setString("currencyFormat", originalFormat);
    }

    /** Half-cent ties round upwards before quantity is multiplied, without changing the source price. */
    @ParameterizedTest
    @CsvSource({ "1.594,1.59,4.77", "1.595,1.60,4.80", "1.585,1.59,4.77" })
    void chargesTheRoundedUnitPrice(String source, String unit, String total) {
        BasketInvoiceItemEntity item = item(source, 0, 3, 1);
        BasketPricingService.allocateVat(List.of(item));

        assertEquals(new BigDecimal(unit), item.getItemPriceVat());
        assertEquals(new BigDecimal(total), item.getItemPriceVatQty());
        assertEquals(new BigDecimal(total), item.getItemPriceQty());
        assertEquals(new BigDecimal(source), item.getItemPrice());
    }

    /** Optional format digits preserve tiny units; line settlement and VAT reconcile after reloading existing fields. */
    @Test
    void preservesTinyPricesThroughQuantityAndReload() {
        Constants.setString("currencyFormat", "0.00##");
        BasketInvoiceItemEntity original = item("0.00008", 23, 155, 1);
        BasketPricingService.recalculateLinePrice(original);
        assertEquals(new BigDecimal("0.0001"), original.getItemPriceVat());
        BasketPricingService.prepareForSave(original);

        BasketInvoiceItemEntity loaded = item(BigDecimal.valueOf(original.getItemPrice().doubleValue()).toPlainString(), 23, 155, 1);
        BasketPricingService.allocateVat(List.of(loaded));
        assertEquals(new BigDecimal("0.0001"), loaded.getItemPriceVat());
        assertEquals(new BigDecimal("0.02"), loaded.getItemPriceVatQty());
        assertEquals(new BigDecimal("0.00"), loaded.getLineVatAmount());
        assertEquals(new BigDecimal("0.02"), loaded.getItemPriceQty());
    }

    /** A basket with mixed rates and a credit allocates residual cents deterministically by remainder and item ID. */
    @Test
    void reconcilesMixedVatAndCreditsRegardlessOfRowOrder() {
        BasketInvoiceItemEntity first = item("0.025", 20, 1, 1);
        BasketInvoiceItemEntity second = item("0.025", 20, 1, 2);
        BasketInvoiceItemEntity credit = item("-0.02", 20, 1, 3);
        BasketInvoiceItemEntity reduced = item("10.00", 5, 2, 4);
        BasketInvoiceItemEntity zero = item("1.594", 0, 3, 5);
        List<BasketInvoiceItemEntity> items = new ArrayList<>(List.of(second, first, credit, reduced, zero));
        BasketPricingService.allocateVat(items);
        List<BigDecimal> expectedVat = List.of(new BigDecimal("0.01"), new BigDecimal("0.00"),
            new BigDecimal("0.00"), new BigDecimal("1.00"), new BigDecimal("0.00"));
        assertEquals(expectedVat, List.of(first, second, credit, reduced, zero).stream()
            .map(BasketInvoiceItemEntity::getLineVatAmount).toList());
        assertEquals(new BigDecimal("25.81"), items.stream().map(BasketInvoiceItemEntity::getItemPriceVatQty).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(new BigDecimal("24.80"), items.stream().map(BasketInvoiceItemEntity::getItemPriceQty).reduce(BigDecimal.ZERO, BigDecimal::add));

        Collections.reverse(items);
        BasketPricingService.allocateVat(items);
        assertEquals(expectedVat, List.of(first, second, credit, reduced, zero).stream()
            .map(BasketInvoiceItemEntity::getLineVatAmount).toList());
    }

    private BasketInvoiceItemEntity item(String net, int vat, int quantity, long id) {
        BasketInvoiceItemEntity item = new BasketInvoiceItemEntity();
        item.setId(id);
        item.setItemPrice(new BigDecimal(net));
        item.setItemVat(vat);
        item.setItemQty(quantity);
        return item;
    }
}
