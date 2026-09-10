package sk.iway.iwcm.components.basket.rest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity;

/**
 * Calculates rounded gross unit prices and allocates VAT rounded per tax rate.
 */
public final class BasketPricingService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private BasketPricingService() {}

    public static boolean isEnabled() {
        return Constants.getBoolean("basketRoundPrices");
    }

    /** Creates a validated currency formatter shared by pricing and presentation. */
    public static DecimalFormat createPriceFormat() {
        String format = Constants.getString("currencyFormat");
        if (format == null || format.isBlank() || format.length() > 255) throw new IllegalArgumentException("Currency format must contain 1 to 255 characters.");
        DecimalFormat formatter = new DecimalFormat(format);
        if (formatter.getMultiplier() != 1) throw new IllegalArgumentException("Currency format must not multiply basket prices.");
        AttributedCharacterIterator output = formatter.formatToCharacterIterator(BigDecimal.ONE);
        for (char character = output.first(); character != CharacterIterator.DONE; character = output.next()) {
            if (output.getAttribute(NumberFormat.Field.EXPONENT) != null) throw new IllegalArgumentException("Currency format must not use scientific notation for basket prices.");
        }
        int scale = formatter.getMaximumFractionDigits();
        if (scale > 18) throw new IllegalArgumentException("Currency format supports at most 18 fraction digits for basket prices.");
        return formatter;
    }

    /** Rounds a selling unit price to the current display format's maximum precision. */
    public static BigDecimal roundGross(BigDecimal amount) {
        return amount.setScale(createPriceFormat().getMaximumFractionDigits(), RoundingMode.HALF_UP);
    }

    /** Applies VAT and the active selling-price policy to catalog or basket net prices. */
    public static BigDecimal sellingPriceWithVat(BigDecimal net, BigDecimal rate) {
        BigDecimal gross = net.multiply(rate.divide(HUNDRED).add(BigDecimal.ONE));
        return isEnabled() ? roundGross(gross) : gross;
    }

    /** Settles a line in EUR or CZK after multiplying its rounded unit price by quantity. */
    public static BigDecimal roundLineGross(BigDecimal grossUnit, int quantity) {
        return grossUnit.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Calculates an item's gross unit price in memory after a net price or VAT change. */
    public static void recalculateLinePrice(BasketInvoiceItemEntity item) {
        recalculateLinePrice(item, createPriceFormat().getMaximumFractionDigits());
    }

    public static void recalculateLinePrice(BasketInvoiceItemEntity item, int scale) {
        item.setRoundedUnitPriceVat(item.getItemPrice().multiply(HUNDRED.add(BigDecimal.valueOf(item.getItemVat())))
            .movePointLeft(2).setScale(scale, RoundingMode.HALF_UP));
        item.setLineVatAmount(null);
    }

    /** Stores the net equivalent of the agreed gross unit in the existing price column. */
    public static void prepareForSave(BasketInvoiceItemEntity item) {
        BigDecimal gross = item.getRoundedUnitPriceVat();
        item.setItemPrice(gross.multiply(HUNDRED).divide(HUNDRED.add(BigDecimal.valueOf(item.getItemVat())),
            Math.max(12, gross.scale() + 8), RoundingMode.HALF_UP));
    }

    /** Allocates each rate's rounded VAT across its lines using largest remainders. */
    public static void allocateVat(List<BasketInvoiceItemEntity> items) {
        Map<Integer, List<VatShare>> groups = new LinkedHashMap<>();
        for (BasketInvoiceItemEntity item : items) {
            if (!item.hasRoundedPrice()) recalculateLinePrice(item);
            int rate = item.getItemVat();
            if (rate < 0) throw new IllegalArgumentException("VAT rate must not be negative.");
            BigDecimal numerator = item.getItemPriceVatQty().multiply(BigDecimal.valueOf(rate));
            BigDecimal denominator = BigDecimal.valueOf(100L + rate);
            BigDecimal floorTax = numerator.divide(denominator, 2, RoundingMode.FLOOR);
            BigDecimal remainder = numerator.subtract(floorTax.multiply(denominator));
            groups.computeIfAbsent(rate, ignored -> new ArrayList<>())
                .add(new VatShare(item, numerator, floorTax, remainder));
        }

        Comparator<VatShare> allocationOrder = Comparator.comparing(VatShare::remainder).reversed()
            .thenComparing(share -> share.item().getId(), Comparator.nullsLast(Comparator.naturalOrder()));
        for (Map.Entry<Integer, List<VatShare>> group : groups.entrySet()) {
            List<VatShare> shares = group.getValue();
            BigDecimal totalNumerator = BigDecimal.ZERO;
            BigDecimal allocatedTax = BigDecimal.ZERO;
            for (VatShare share : shares) {
                totalNumerator = totalNumerator.add(share.numerator());
                allocatedTax = allocatedTax.add(share.floorTax());
            }
            BigDecimal targetTax = totalNumerator.divide(BigDecimal.valueOf(100L + group.getKey()), 2, RoundingMode.HALF_UP);
            int remainingCents = targetTax.subtract(allocatedTax).movePointRight(2).intValueExact();
            shares.sort(allocationOrder);
            for (int index = 0; index < shares.size(); index++) {
                VatShare share = shares.get(index);
                BigDecimal tax = share.floorTax();
                if (index < remainingCents) tax = tax.add(new BigDecimal("0.01"));
                share.item().setLineVatAmount(tax);
            }
        }
    }

    private record VatShare(BasketInvoiceItemEntity item, BigDecimal numerator, BigDecimal floorTax, BigDecimal remainder) {}
}
