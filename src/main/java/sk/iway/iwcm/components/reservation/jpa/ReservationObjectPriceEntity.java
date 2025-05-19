package sk.iway.iwcm.components.reservation.jpa;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.DateTools;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Entity
@Table(name = "reservation_object_price")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_RESERVATION_PRICE)
public class ReservationObjectPriceEntity implements Serializable {

    @Id
    @GeneratedValue(generator = "WJGen_reservation_object_price")
    @TableGenerator(name = "WJGen_reservation_object_price", pkColumnValue = "reservation_object_price")
    @Column(name = "object_price_id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "object_id")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "reservation.reservation_object_price.reservation_object_id",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
                }
            )
        }
    )
    private Long objectId;

    @Column(name = "datum_od")
    @NotNull
    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="components.reservation.reservation_list.date_from2")
    private Date dateFrom;

    @Column(name = "datum_do")
    @NotNull
    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="components.reservation.reservation_list.date_to2")
    private Date dateTo;

    @Column(name = "cena")
    @NotNull
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title = "reservation.reservation_object_price.price")
    private BigDecimal price;

    @Column(name = "domain_id")
    private Integer domainId;

    @ManyToOne
	@JsonBackReference(value="reservationObjectForPrice")
	@JoinColumn(name="object_id", insertable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private ReservationObjectEntity reservationObjectForPrice;

    public Date getDateFrom() {
        return DateTools.setTimePart(this.dateFrom, 0, 0, 0, 0);
    }

    public Date getDateTo() {
        return DateTools.setTimePart(this.dateTo, 23, 59, 59, 999);
    }
}
