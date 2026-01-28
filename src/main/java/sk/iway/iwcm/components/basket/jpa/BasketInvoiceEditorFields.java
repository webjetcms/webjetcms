package sk.iway.iwcm.components.basket.jpa;

import java.math.BigDecimal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;
import sk.iway.iwcm.users.UsersDB;

@Getter
@Setter
public class BasketInvoiceEditorFields extends BaseEditorFields {

    //USED just for export
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN, title="components.basket.invoice.price")
    private BigDecimal exportTotalPriceWithVat;
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN, title="components.basket.invoice.items")
    private Integer exportTotalItemsCount;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title="components.basket.sendNotificationToClient",
		tab = "basic",
        hidden = true,
        sortAfter = "userNote",
        className = "not-export"
    )
	private Boolean sendNotification;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.qa.add_action.sender",
		tab = "notify",
        hidden = true,
        className = "not-export"
    )
	private String sender;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.qa.add_action.subject",
		tab = "notify",
        sortAfter = "contactEmail",
        hidden = true,
        className = "not-export"
    )
	private String subject;

    @DataTableColumn(
        inputType = DataTableColumnType.QUILL,
        title="components.basket.invoice.body",
		tab = "notify",
        hidden = true,
        className = "not-export",
        editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
					@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{components.basket.invoice.notify_body}]]")
				}
			)
		}
    )
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
	private String body;

    @DataTableColumn(inputType = DataTableColumnType.HIDDEN, className = "not-export")
    private String orderRecapHead;

    @DataTableColumn(inputType = DataTableColumnType.HIDDEN, className = "not-export")
    private String orderRecapBody;

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
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "celledit,import")
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
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,celledit,duplicate,import"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-tabs", value = "[{ 'id': 'basic', 'title': '[[#{datatable.tab.basic}]]', 'selected': true },{ 'id': 'itemPreview', 'title': '[[#{editor.preview}]]', 'content': '<div class=\\\"previewContainer\\\"><iframe id=\\\"itemPreviewIframe\\\" src=\\\"about:blank\\\" width=\\\"100%\\\" height=\\\"500\\\"></iframe></div>' }]")
            }
        )
    })
    private List<BasketInvoiceItemEntity> items;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.basket.invoice_email.surname",
		hiddenEditor = true,
        sortAfter = "id",
        orderable = true,
        orderProperty = "contactLastName,deliverySurName"
    )
    private String lastName;

    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.basket.invoice.name",
		hiddenEditor = true,
        sortAfter = "id",
        orderable = true,
        orderProperty = "contactFirstName,deliveryName"
    )
    private String firstName;

    public void fromBasketInvoice(BasketInvoiceEntity originalEntity, HttpServletRequest request) {

        Prop prop = Prop.getInstance(request);

        sender = UsersDB.getCurrentUser(request).getEmail();
        subject = prop.getText("components.basket.invoiceDetail.subject", originalEntity.getBasketInvoiceId() + "");
        body = prop.getText("components.basket.invoiceDetail.body") + " <p>&nbsp;</p> {ORDER_DETAILS}";
        authToken = originalEntity.getAuthorizationToken();

        exportTotalItemsCount = originalEntity.getItemQty();
        exportTotalPriceWithVat = originalEntity.getPriceToPayVat();

        firstName = originalEntity.getContactFirstName();
        if(Tools.isNotEmpty( originalEntity.getDeliveryName() )) firstName += " (" + originalEntity.getDeliveryName() + ")";

        lastName = originalEntity.getContactLastName();
        if(Tools.isNotEmpty( originalEntity.getDeliverySurName() )) lastName += " (" + originalEntity.getDeliverySurName() + ")";

        originalEntity.setEditorFields(this);
    }
}
