package sk.iway.iwcm.components.reservation.jpa;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;
import sk.iway.iwcm.system.jpa.DefaultTimeValueConverter;

@Entity
@Table(name = "reservation_object")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_RESERVATION_OBJECT)
public class ReservationObjectEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_reservation_object")
    @Column(name = "reservation_object_id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "name")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="components.reservation.admin_addObject.name",
        tab = "basic"
    )
    @Size(max = 100)
    @NotBlank
    private String name;

    @Column(name = "max_reservations")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.reservation.admin_addObject.max_pocet_rezeracii",
        visible = false,
        tab = "basic"
    )
    @Min(value = 1)
    private Integer maxReservations;

    @Column(name = "cancel_time_befor")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="reservation.reservation_object.cancel_time_before",
        visible = false,
        tab = "basic"
    )
    private Integer cancelTimeBefor;

    @Column(name = "reservation_for_all_day")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="components.reservation.admin_addObject.reservation_for_all_day",
        visible = false,
        tab = "basic"
    )
    private Boolean reservationForAllDay;

    @Column(name = "price_for_day")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="reservation.reservation_object.price_for_day",
        visible = false,
        tab = "basic"
    )
    private BigDecimal priceForDay;

    @Column(name = "time_unit")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="reservation.reservation_object.time_unit",
        visible = false,
        tab = "basic"
    )
    private Integer timeUnit;

    @Column(name = "price_for_hour")
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.reservation.admin_addObject.price_for_hour",
        visible = false,
        tab = "basic"
    )
    private BigDecimal priceForHour;

    @Column(name = "photo_link")
    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        title="components.reservation.admin_addObject.photo_link",
        visible = false,
        tab = "basic"
    )
    @Size(max = 255)
    private String photoLink;

    @Column(name = "description")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="components.reservation.admin_addObject.description",
        tab = "basic"
    )
    @Size(max = 2000)
    @NotBlank
    private String description;

    @Column(name = "reservation_time_from")
    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_from",
        visible = false,
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "components.reservation.admin_addObject.kedy_mozne_rezervovat"),
                    @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
                }
            )
        }
    )
    @Convert(converter = DefaultTimeValueConverter.class)
    private Date reservationTimeFrom;

    @Column(name = "reservation_time_to")
    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_to",
        visible = false,
        tab = "basic"
    )
    @Convert(converter = DefaultTimeValueConverter.class)
    private Date reservationTimeTo;

    @Column(name = "must_accepted")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="components.reservation.admin_addObject.accept",
        tab = "advanced"
    )
    private Boolean mustAccepted;

    @Column(name = "email_accepter")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.reservation.admin_addObject.email",
        tab = "advanced"
    )
    @Size(max = 150)
    private String emailAccepter;

    @Column(name = "notif_emails")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="components.reservation.reservation_object.notifEmails",
        tab = "advanced",
        sortAfter = "emailAccepter"
    )
    private String notifEmails;

    @Column(name = "passwd")
    private String password;

    @Column(name = "domain_id")
    private Integer domainId;

    //Used in cases when we need included reservationObjectTimes values in variable
    //Key (Integer) is day of week 1,2 ... 7
    //Value (String) is combination timeFrom + "-" + timeTo
    @Transient
    private HashMap<Integer, String> objectTimesInfo;

    @Transient
    @DataTableColumnNested
	private ReservationObjectEditorFields editorFields = null;

	@JsonManagedReference(value="reservationObjectForTime")
    @OneToMany(mappedBy="reservationObjectForTime", fetch=FetchType.LAZY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<ReservationObjectTimesEntity> reservationObjectTimeEntities;

    @JsonManagedReference(value="reservationObjectForPrice")
    @OneToMany(mappedBy="reservationObjectForPrice", fetch=FetchType.LAZY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<ReservationObjectPriceEntity> reservationObjectPriceEntities;

    public void setPassword(String password) {
        this.password = getHashPassword(password);
    }

    private String getHashPassword(String passwd) {
        String hashPasswd = "";
		try {
            if(passwd == null || passwd.isEmpty()) return "";
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(passwd.getBytes());
			BigInteger hash = new BigInteger(1, md5.digest());
			hashPasswd = hash.toString(16);
		}
		catch (NoSuchAlgorithmException nsae) {
			Logger.error(nsae);
		}
        return hashPasswd;
    }

    public boolean checkPasswordAndHashEquality(String passwd, String passwdHash) {
        return getHashPassword(passwd).equals(passwdHash) ? true : false;
    }
}
