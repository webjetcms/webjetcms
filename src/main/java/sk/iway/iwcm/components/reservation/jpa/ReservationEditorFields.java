package sk.iway.iwcm.components.reservation.jpa;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.reservation.rest.ReservationService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.jpa.DefaultTimeValueConverter;
import sk.iway.iwcm.users.UsersDB;

@Getter
@Setter
public class ReservationEditorFields {

    public ReservationEditorFields() {
        //
    }

    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="reservation.reservation_object.selected_object",
        hiddenEditor = true,
        sortAfter = "email"
    )
    private String selectedReservation;

    //Hidden field, we just must know
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "&nbsp;",
        hiddenEditor = true,
        hidden = true
    )
    private Boolean needPasswordToDelete;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="reservation.reservations.time_from",
        sortAfter = "dateTo",
        tab = "basic"
    )
    private Date reservationTimeFrom;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="reservation.reservations.time_to",
        sortAfter = "editorFields.reservationTimeFrom",
        tab = "basic"
    )
    private Date reservationTimeTo;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "reservation.reservations.info_title",
        sortAfter = "dateTo",
        tab = "basic",
        className = "wrap",
        hidden = true
    )
    private String infoLabel1;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "reservation.reservations.validate_reservation",
        visible = false,
        sortAfter = "purpose",
        tab = "basic"
    )
    private Boolean showReservationValidity;

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "reservation.reservations.status",
        tab = "acceptation",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "reservation.reservations.accept", value = "1"),
                    @DataTableColumnEditorAttr(key = "reservation.reservations.reject", value = "0"),
                    @DataTableColumnEditorAttr(key = "reservation.reservations.reset", value = "-1")
                }
            )
        },
        visible = false
    )
    private Integer acceptation;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "reservation.reservations.info_title",
        tab = "basic",
        className = "wrap",
        hidden = true
    )
    private String infoLabel2;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="dayfull.mo",
        visible = false,
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "reservation.reservations.reservation_object_times"),
                    @DataTableColumnEditorAttr(key = "disabled", value = "disabled")
                }
            )
        }
    )
    private String reservationTimeRangeA;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="dayfull.tu",
        visible = false,
        tab = "basic",
        editor = @DataTableColumnEditor( attr = @DataTableColumnEditorAttr(key = "disabled", value = "disabled"))
    )
    private String reservationTimeRangeB;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="dayfull.we",
        visible = false,
        tab = "basic",
        editor = @DataTableColumnEditor( attr = @DataTableColumnEditorAttr(key = "disabled", value = "disabled"))
    )
    private String reservationTimeRangeC;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="dayfull.th",
        visible = false,
        tab = "basic",
        editor = @DataTableColumnEditor( attr = @DataTableColumnEditorAttr(key = "disabled", value = "disabled"))
    )
    private String reservationTimeRangeD;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="dayfull.fr",
        visible = false,
        tab = "basic",
        editor = @DataTableColumnEditor( attr = @DataTableColumnEditorAttr(key = "disabled", value = "disabled"))
    )
    private String reservationTimeRangeE;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="dayfull.sa",
        visible = false,
        tab = "basic",
        editor = @DataTableColumnEditor( attr = @DataTableColumnEditorAttr(key = "disabled", value = "disabled"))
    )
    private String reservationTimeRangeF;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="dayfull.su",
        visible = false,
        tab = "basic",
        editor = @DataTableColumnEditor( attr = @DataTableColumnEditorAttr(key = "disabled", value = "disabled"))
    )
    private String reservationTimeRangeG;

    //special anotation, create a ReservationObjectPrice table inside specialPrice tab
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "specialPrice",
        hidden = true,
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/reservation/reservation-object-price?object-id={reservationObjectId}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.reservation.jpa.ReservationObjectPriceEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-hideButtons", value = "create,edit,duplicate,import,celledit,remove"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false")
            }
        )
    })
    private List<ReservationObjectPriceEntity> objectPrices;

    public void fromReservationEntity(ReservationEntity originalEntity, ProcessItemAction action, HttpServletRequest request) {

        if(action != ProcessItemAction.GETALL) {
            //Set info label
            Prop prop = Prop.getInstance(request);
            setInfoLabel1(prop.getText("reservation.reservations.info_label_1"));
        }

        if(action == ProcessItemAction.CREATE) {
            //Logged user
            Identity user = UsersDB.getCurrentUser(request);
            originalEntity.setName(user.getFirstName());
            originalEntity.setSurname(user.getLastName());
            originalEntity.setEmail(user.getEmail());

            //-1 represent waiting for approve
            acceptation = -1;

            //Set default time range
            reservationTimeFrom = DefaultTimeValueConverter.getValidTimeValue(8, 0);
            reservationTimeTo = DefaultTimeValueConverter.getValidTimeValue(16, 0);

            //Set default reservation date on tomorrow
            Calendar cld = Calendar.getInstance();
            cld.setTime(new Date());
            cld.add(Calendar.DAY_OF_MONTH, 1);
            originalEntity.setDateFrom(cld.getTime());
            originalEntity.setDateTo(cld.getTime());
        } else {
            //Set correct time range
            reservationTimeFrom = DefaultTimeValueConverter.getValidTimeValue(originalEntity.getDateFrom());
            reservationTimeTo = DefaultTimeValueConverter.getValidTimeValue(originalEntity.getDateTo());
        }

        ReservationObjectEntity selected = originalEntity.getReservationObjectForReservation();
        if(selected != null) {
            selectedReservation = selected.getName();
            if(selected.getPassword() != null && selected.getPassword().isEmpty() == false) needPasswordToDelete = Boolean.TRUE;
            else needPasswordToDelete = Boolean.FALSE;
        }

        originalEntity.setEditorFields(this);
    }

    public void toReservationEntity(ReservationEntity originalEntity, ReservationRepository rr, HttpServletRequest request, boolean skipPrepare) {
        //Set domain id for new entity
        if(originalEntity.getDomainId() == null) originalEntity.setDomainId(CloudToolsForCore.getDomainId());

        ReservationObjectEntity reservationObject = originalEntity.getReservationObjectForReservation();

        String error = null;
        ReservationService reservationService = new ReservationService(Prop.getInstance(request));
        if(skipPrepare == false)
            reservationService.prepareReservationToValidation(originalEntity, Tools.isTrue(reservationObject.getReservationForAllDay()) );

        //If reservationObject can be reserve only for whole day, we dont need to check time range (time range is set automatically)
        if(Tools.isFalse(reservationObject.getReservationForAllDay())) {
            //In this case we can select even time, so we need check time validity
            error = reservationService.checkReservationTimeRangeValidity(originalEntity, reservationObject);
            //Check if error was returned
            if(error != null) reservationService.throwError(error);
        }

        //Validate reservation range
        error = reservationService.checkReservationOverlappingValidity(originalEntity, reservationObject, rr);
        if(error != null) reservationService.throwError(error);

        //Now decide if reservation need acceptation or not
        if(ReservationService.acceptation(originalEntity, request) == false)  {
            // !! Send mail - > in after save because we dont have ID yet
        }
    }
}