package sk.iway.iwcm.components.basket.jpa;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class BasketInvoiceItemEditorFields {

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title="components.basket.price_without_DPH_complete",
		hiddenEditor = true
    )
    private BigDecimal withoutVatComplete;

	@DataTableColumn(
		inputType = DataTableColumnType.TEXT,
		title = "components.basket.DPH",
        hiddenEditor = true
	)
	private String itemVat;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title="components.basket.invoice.items.price_with_VAT_complete",
		hiddenEditor = true
    )
    private BigDecimal withVatComplete;

    public void fromBasketInvoiceItem(BasketInvoiceItemEntity originalEntity) {
        withoutVatComplete = originalEntity.getItemPrice().multiply( BigDecimal.valueOf(originalEntity.getItemQty()) );
        itemVat = originalEntity.getItemVat() + "%";
        withVatComplete = originalEntity.getItemPriceVatQty();
        originalEntity.setEditorFields(this);
    }
}
