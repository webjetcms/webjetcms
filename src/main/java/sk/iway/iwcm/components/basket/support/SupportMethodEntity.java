package sk.iway.iwcm.components.basket.support;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.jpa.AllowSafeHtmlAttributeConverter;

@Getter
@Setter
@MappedSuperclass
public class SupportMethodEntity {

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

    @Column(name = "field_i")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String fieldI;

    @Column(name = "field_j")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String fieldJ;

    @Column(name = "field_k")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String fieldK;

    @Column(name = "field_l")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, hidden = true)
    @Size(max = 255)
    @javax.persistence.Convert(converter = AllowSafeHtmlAttributeConverter.class)
    private String fieldL;
}