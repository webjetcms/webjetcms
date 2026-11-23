# Price rounding

Rounding ensures that the price in the cart is calculated from the displayed price per piece including VAT. For example, the price **€1.594** is rounded to **€1.59** and the customer pays **€4.77** for three pieces.

## Power on

In the configuration, set:

- **`basketRoundPrices=true`** – turns on rounding. The default value `false` leaves the original calculation.
- **`currencyFormat=0.00`** – sets the price per item to two decimal places. You can use more places for cheap products.

The function supports EUR and CZK. Both the trade and the order use the set system currency.

## How is the price calculated?

1. The price per piece including VAT, after discounts and any currency conversion, is rounded to `currencyFormat`.
2. The rounded price is multiplied by the number of pieces. The resulting amount for the product is rounded to two decimal places.
3. The cart adds up the amounts for products, shipping, and payment. The fees are rounded in the same way.

Normal rounding is used: for example **1.594 → 1.59** and **1.595 → 1.60**. VAT is calculated from the sum of the VAT prices for each rate separately and divided between the items so that the totals match.

### Number of decimal places

Example for the original price **1.594 incl. VAT** and **3 pieces**:

| `currencyFormat` | Price shown per piece | Amount for 3 pieces for payment |
| --- | ---: | ---: |
| `0` | 2.00 | 6.00 |
| `0.0` | 1.60 | 4.80 |
| `0.00` | 1.59 | 4.77 |
| `0.0000` | 1.5940 | 4.78 |
| `0.00##` | 1,594 | 4.78 |

`0` means a required digit, `#` optional. The `0.00##` pattern therefore preserves up to four decimal places, but does not display unnecessary trailing zeros. Prices are always displayed to at least two places. With `0.0000`, the payment amount of 4.78 is also displayed as 4.7800.

### Very cheap products

For example, for a resistor priced at **€0.0001**, use `currencyFormat=0.0000`. Then **10,000 pieces cost €1.00**. With `0.00`, the price per piece would be rounded to zero. Even with higher precision, the resulting amount for the product is rounded to cents; an amount less than €0.005 will be zero.

For large quantities, also adjust the limit `basketMaxQty`, which is 1000 pieces by default.

## Euro and Czech crown

For **card or bank transfer**, the calculation works the same in EUR and CZK: the price per piece is governed by `currencyFormat` and the amount to be paid is accurate to two decimal places.

The difference is when withdrawing **cash**:

- **Slovakia:** the resulting cash amount is rounded to the nearest €0.05. For example, €4.77 → €4.75. If the entire payment is only €0.01 or €0.02, it is rounded to €0.05.
- **Czech Republic:** the resulting cash amount is rounded to whole crowns. For example, 4.77 CZK → 5 CZK.

**Cash rounding is not performed by this feature.** It is handled by the cashier or courier depending on the actual payment method. Cash on delivery does not have to mean cash - the courier can also be paid by card.

## Before deployment

Templates using the `iway:curr` tag will automatically adopt the new formatting. Custom price calculations must be verified separately. After enabling, clear the product list cache and try shopping. When you submit your order, product prices will be updated to the current store prices.

Choose the accuracy setting when you launch your store and change it carefully later. Changing the setting **does not change the saved total price of older orders**, but may change the display of their items and the VAT breakdown. When you edit items, the order is recalculated according to the current settings.
