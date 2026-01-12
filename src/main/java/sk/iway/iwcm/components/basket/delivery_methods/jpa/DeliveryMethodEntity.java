package sk.iway.iwcm.components.basket.delivery_methods.jpa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.basket.support.SupportMethodEntity;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@Entity
@Table(name = "delivery_methods")
@Getter
@Setter
@EntityListeners(AuditEntityListener.class)
@EntityListenersType(Adminlog.TYPE_BASKET_UPDATE)
public class DeliveryMethodEntity extends SupportMethodEntity {

    @Id //We do not use this ID, it's here just because it must be here
    @Column(name = "id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "delivery_method_name")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "components.basket.invoice_email.delivery_method",
        className="dt-row-edit",
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

    @Column(name = "title")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "components.basket.delivery_method.title"
    )
    private String title;

    //For frontend files, here we gonna set name from translation key, based on "title/deliveryMethodName" plus price and currency
    @Transient
    private String customerTitle;

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
    private BigDecimal priceVat;

    @Column(name = "sort_priority")
	@DataTableColumn(
		inputType = DataTableColumnType.NUMBER,
		title = "editor.sort_order"
	)
	private Integer sortPriority = 0;

    @Transient
	@DataTableColumnNested
	private transient BaseEditorFields editorFields = null;

    @Column(name = "domain_id")
    private Integer domainId;

    public BigDecimal getPriceVat() {
        if(price == null) return BigDecimal.ZERO;
        if(vat == null || vat < 1) return price;

        BigDecimal bdVat = new BigDecimal(vat);
        return price
            .multiply(BigDecimal.ONE.add(bdVat.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)))
            .setScale(2, RoundingMode.HALF_UP);
    }

    @JsonIgnore
    public String[] getSupportedCountriesArr() { return Tools.getTokens(getSupportedCountriesStr(), ",+"); }

    @JsonIgnore
    public List<String> getSupportedCountriesList() { return Arrays.asList( getSupportedCountriesArr() ); }
}