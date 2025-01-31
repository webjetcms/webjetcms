package sk.iway.iwcm.components.basket.jpa;

import java.math.BigDecimal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;
import sk.iway.iwcm.users.UsersDB;

@Getter
@Setter
public class BasketInvoiceEditorFields {

    @DataTableColumn(
        inputType = DataTableColumnType.CHECKBOX,
        title="components.basket.sendNotificationToClient",
		tab = "basic",
        hidden = true,
        sortAfter = "userNote"
    )
	private Boolean sendNotification;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.qa.add_action.sender",
		tab = "notify",
        hidden = true
    )
	private String sender;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.qa.add_action.subject",
		tab = "notify",
        sortAfter = "contactEmail",
        hidden = true
    )
	private String subject;

    @DataTableColumn(
        inputType = DataTableColumnType.QUILL,
        title="components.basket.invoice.body",
		tab = "notify",
        hidden = true,
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
					@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{components.basket.invoice.notify_body}]]")
				}
			)
		}
    )
    @javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
	private String body;

    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private String orderRecapHead;

    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private String orderRecapBody;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.basket.invoice.items",
		hiddenEditor = true
    )
	private Integer itemsCount;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title="components.basket.invoice.price",
		hiddenEditor = true
    )
    private BigDecimal totalPriceVat;

    //AUTH TOKEN  - for getting iframe of invoice_email.jsp
    @DataTableColumn(visible = false, hidden = true, hiddenEditor = true)
    private String authToken;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
    tab = "payments",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/eshop/basket-payments?invoiceId={id}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.basket.jpa.BasketInvoicePaymentEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "import"),
                @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "components.basket.invoice.payments.headline")
            }
        )
    })
    private List<BasketInvoicePaymentEntity> payments;

    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
    tab = "items",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/eshop/basket-items?invoiceId={id}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.basket.jpa.BasketInvoiceItemEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,duplicate,import"),
                @DataTableColumnEditorAttr(key = "data-dt-field-full-headline", value = "components.basket.invoice.items.headline")
            }
        )
    })
    private List<BasketInvoiceItemEntity> items;

    public void fromBasketInvoice(BasketInvoiceEntity originalEntity, HttpServletRequest request) {

        Prop prop = Prop.getInstance(request);

        sender = UsersDB.getCurrentUser(request).getEmail();
        subject = prop.getText("components.basket.invoiceDetail.subject", originalEntity.getBasketInvoiceId() + "");
        body = prop.getText("components.basket.invoiceDetail.body") + " <p>&nbsp;</p> {ORDER_DETAILS}";
        authToken = originalEntity.getAuthorizationToken();
        itemsCount = originalEntity.getTotalItems();
        totalPriceVat = originalEntity.getTotalPriceVat();

        originalEntity.setEditorFields(this);
    }
}
