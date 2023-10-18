package sk.iway.iwcm.components.reservation.jpa;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

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
    @DataTableColumn(inputType = DataTableColumnType.ID, title="ID")
    private Long id;

    @Column(name = "name")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.reservation.reservation_list.name",
        tab = "personalInfo"
    )
    @Size(max = 150)
    private String name;

    @Column(name = "surname")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.reservation.reservation_list.surname",
        tab = "personalInfo"
    )
    @Size(max = 155)
    private String surname;

    @Column(name = "email")
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
    private Integer reservationObjectId;

    @Column(name = "date_from")
    @Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="components.reservation.reservation_list.date_from2",
        tab = "basic"
    )
	private Date dateFrom;

    @Column(name = "date_to")
    @Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="components.reservation.reservation_list.date_to2",
        tab = "basic"
    )
	private Date dateTo;

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
        hiddenEditor = true,
        editor = {
			@DataTableColumnEditor(
				options = {
                    @DataTableColumnEditorAttr(key = "reservation.reservations.status.waiting", value = "null"),
					@DataTableColumnEditorAttr(key = "reservation.reservations.status.accepted", value = "true"),
					@DataTableColumnEditorAttr(key = "reservation.reservations.status.denied", value = "false")
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

    @Column(name = "domain_id")
    private Integer domainId;

    @Transient
    @DataTableColumnNested
    private ReservationEditorFields editorFields = null;
}
