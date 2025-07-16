package sk.iway.iwcm.components.reservation.jpa;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_RESERVATION)
public class ReservationEntity implements Serializable {

    @Id
    @Column(name = "reservation_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_reservation")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "name")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.reservation.reservation_list.name",
        tab = "personalInfo"
    )
    @Size(max = 150)
    private String name;

    @Column(name = "surname")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.reservation.reservation_list.surname",
        tab = "personalInfo"
    )
    @Size(max = 155)
    private String surname;

    @Column(name = "email")
    @NotBlank
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.reservation.reservation_list.email",
        tab = "personalInfo"
    )
    @Size(max = 100)
    private String email;

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="components.reservation.reservationObjectList",
        visible = false,
        sortAfter = "editorFields.selectedReservation"
    )
    @Column(name = "reservation_object_id")
    private Long reservationObjectId;

    @Column(name = "date_from")
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="components.reservation.reservation_list.date_from2",
        tab = "basic"
    )
	private Date dateFrom;

    @Column(name = "date_to")
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="components.reservation.reservation_list.date_to2",
        tab = "basic"
    )
	private Date dateTo;

    @Lob
    @Column(name = "purpose")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="components.reservation.reservation_list.purpose",
        visible = false,
        tab = "basic"
    )
    private String purpose;

    @Column(name = "accepted")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="reservation.reservations.status",
        className = "disabled hide-on-create",
        editor = {
			@DataTableColumnEditor(
				options = {
                    @DataTableColumnEditorAttr(key = "reservation.reservations.status.waiting", value = "null"),
					@DataTableColumnEditorAttr(key = "reservation.reservations.status.accepted", value = "true"),
					@DataTableColumnEditorAttr(key = "reservation.reservations.status.denied", value = "false")
				},
                attr = {
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
                }
			)
		}
    )
    private Boolean accepted;

    @Column(name = "phone_number")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.formsimple.label.telefon",
        visible = false,
        tab = "personalInfo"
    )
    @Size(max = 255)
    private String phoneNumber;

    @Column(name = "hash_value")
    @Size(max = 60)
    private String hashValue;

    @Column(name = "price")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        renderFormat = "dt-format-number--decimal",
        title="components.reservation.reservations.price",
        tab = "basic",
        sortAfter = "editorFields.reservationTimeTo",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private BigDecimal price;

    @DataTableColumn(inputType = DataTableColumnType.HIDDEN, className = "not-export")
    @Column(name = "domain_id")
    private Integer domainId;

    @Column(name = "user_id")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Integer userId;

    @Transient
    @DataTableColumnNested
    private transient ReservationEditorFields editorFields = null;

    @ManyToOne
    @JsonBackReference(value="reservationObjectForReservation")
    @JoinColumn(name="reservation_object_id", insertable = false, updatable = false)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private ReservationObjectEntity reservationObjectForReservation;

    public String getReservationObjectName() {
        return reservationObjectForReservation != null ? reservationObjectForReservation.getName() : "";
    }

    //For date-book-app
    @Transient
    @JsonIgnore
    private String actualDate;
}
