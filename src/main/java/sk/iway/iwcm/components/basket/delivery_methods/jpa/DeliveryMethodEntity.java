package sk.iway.iwcm.components.basket.delivery_methods.jpa;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;

@Entity
@Table(name = "delivery_methods")
@Getter
@Setter
@EntityListeners(AuditEntityListener.class)
@EntityListenersType(Adminlog.TYPE_BASKET_UPDATE)
public class DeliveryMethodEntity {

    @Id //We do not use this ID, it's here just because it must be here
    @Column(name = "id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    //For us, this is REAL ID, in DB set as UNIQUE
    @Column(name = "delivery_method_name")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.basket.invoice_email.delivery_method",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled"),
                }
            )
        }
    )
    @Size(max = 255)
    private String deliveryMethodName;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.MULTISELECT,
        title = "apps.eshop.delivery_methods.supproted_countries",
        filter = true
    )
    private String[] supportedCountries;

    @Column(name = "supported_countries")
    private String supportedCountriesStr;

    //Price no VAT
    @Column(name="price")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title = "components.basket.price_without_DPH"
    )
    @Min(0)
    private BigDecimal price;

    @Column(name="vat")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title = "apps.product-list.VAT.js"
    )
    @Min(0)
	private Integer vat;

    @Transient
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title = "apps.product-list.price_with_DPH.js",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled"),
                }
            )
        }
    )
    private BigDecimal priceWithVat;

    @Column(name="currency")
	@DataTableColumn(inputType = DataTableColumnType.SELECT, title = "components.basket.invoice.currency")
    @Size(max = 8)
	private String currency;

    @Column(name = "sort_priority")
	@DataTableColumn(
		inputType = DataTableColumnType.NUMBER,
		title = "editor.sort_order"
	)
	private Integer sortPriority = 0;

    @Column(name = "field_a")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String fieldA;

    @Column(name = "field_b")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String fieldB;

    @Column(name = "field_c")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String fieldC;

    @Column(name = "field_d")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String fieldD;

    @Column(name = "field_e")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String fieldE;

    @Column(name = "field_f")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String fieldF;

    @Column(name = "field_g")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String fieldG;

    @Column(name = "field_h")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String fieldH;

    @Transient
	@DataTableColumnNested
	private transient BaseEditorFields editorFields = null;

    @Column(name = "domain_id")
    private Integer domainId;

    public BigDecimal getPriceWithVat() {
        if(vat == null || vat < 1) return price;
        if(price == null) return BigDecimal.ZERO;
        return price.multiply(new BigDecimal(vat));
    }
}