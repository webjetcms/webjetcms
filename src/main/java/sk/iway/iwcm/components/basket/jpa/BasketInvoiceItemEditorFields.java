package sk.iway.iwcm.components.basket.jpa;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.json.LabelValue;

@Getter
@Setter
public class BasketInvoiceItemEditorFields extends BaseEditorFields {

    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.basket.invoice_items.items_type",
        hiddenEditor = true, hidden = false, visible = true, sortAfter = "id", className = "allow-html", orderable = false, filter = false
    )
    private String statusIcons;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title="components.basket.price_without_DPH_complete",
		hiddenEditor = true,
        sortAfter = "itemQty"
    )
    private BigDecimal withoutVatComplete;

	@DataTableColumn(
		inputType = DataTableColumnType.TEXT,
		title = "components.basket.DPH",
        hiddenEditor = true,
        sortAfter = "editorFields.withoutVatComplete"
	)
	private String itemVat;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title="components.basket.invoice.items.price_with_VAT_complete",
		hiddenEditor = true,
        sortAfter = "editorFields.itemVat"
    )
    private BigDecimal withVatComplete;

    public void fromBasketInvoiceItem(BasketInvoiceItemEntity originalEntity, List<LabelValue> paymentMethods, List<LabelValue> deliveryMethods) {
        StringBuilder iconsHtml = new StringBuilder();

        withoutVatComplete = originalEntity.getItemPrice().multiply( BigDecimal.valueOf(originalEntity.getItemQty()) );
        itemVat = originalEntity.getItemVat() + "%";
        withVatComplete = originalEntity.getItemPriceVatQty();

        if(originalEntity.getItemId() == 0 && paymentMethods.stream().anyMatch(payment -> payment.getLabel().equals(originalEntity.getItemTitle()))) {
            //This is payment method
            iconsHtml.append("<i class=\"ti ti-cash\" style=\"width: 1.25em;\"></i>");
        } else if(originalEntity.getItemId() == 0 && deliveryMethods.stream().anyMatch(delivery -> delivery.getLabel().equals(originalEntity.getItemTitle()))) {
            //This is transport method
            iconsHtml.append("<i class=\"ti ti-truck-delivery\" style=\"width: 1.25em;\"></i>");
        } else {
            //This is product
            iconsHtml.append("<i class=\"ti ti-shopping-bag\" style=\"width: 1.25em;\"></i>");
        }

        iconsHtml.append( getStatusIconsHtml() );
        statusIcons = iconsHtml.toString();

        originalEntity.setEditorFields(this);
    }
}
