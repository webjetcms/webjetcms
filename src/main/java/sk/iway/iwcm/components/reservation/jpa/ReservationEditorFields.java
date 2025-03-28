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
        sortAfter = "email",
        orderable = false
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
        inputType = DataTableColumnType.BOOLEAN,
        title = "components.reservation.allow_history_save",
        visible = false,
        sortAfter = "accepted",
        tab = "basic"
    )
    private Boolean allowHistorySave;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "components.reservation.allow_overbooking",
        visible = false,
        sortAfter = "editorFields.allowHistorySave",
        tab = "basic"
    )
    private Boolean allowOverbooking;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "reservation.reservations.info_title",
        tab = "basic",
        className = "wrap not-export",
        hidden = true
    )
    private String infoLabel;

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

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="reservation.reservations.arrival_time",
        sortAfter = "dateFrom",
        tab = "basic"
    )
    private Date arrivingTime;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="reservation.reservations.departure_time",
        sortAfter = "dateTo",
        tab = "basic"
    )
    private Date departureTime;

    public void fromReservationEntity(ReservationEntity originalEntity, ProcessItemAction action, HttpServletRequest request) {

        ReservationObjectEntity selected = originalEntity.getReservationObjectForReservation();

        if(action == ProcessItemAction.CREATE) {
            //Logged user
            Identity user = UsersDB.getCurrentUser(request);
            originalEntity.setName(user.getFirstName());
            originalEntity.setSurname(user.getLastName());
            originalEntity.setEmail(user.getEmail());

            //Set default reservation date on tomorrow
            Calendar cld = Calendar.getInstance();
            cld.setTime(new Date());
            cld.add(Calendar.DAY_OF_YEAR, 1);
            originalEntity.setDateFrom(cld.getTime());
            originalEntity.setDateTo(cld.getTime());

            // !! CANT set time while CREATING - because we dont know if first selected reservationObject in select is for whole day or not
        } else {
            if(selected != null) {
                if(Tools.isTrue(selected.getReservationForAllDay())) {
                    arrivingTime = DefaultTimeValueConverter.getValidTimeValue(originalEntity.getDateFrom());
                    departureTime = DefaultTimeValueConverter.getValidTimeValue(originalEntity.getDateTo());
                } else {
                    //Set correct time range
                    reservationTimeFrom = DefaultTimeValueConverter.getValidTimeValue(originalEntity.getDateFrom());
                    reservationTimeTo = DefaultTimeValueConverter.getValidTimeValue(originalEntity.getDateTo());
                }
            }
        }

        if(selected != null) {
            selectedReservation = selected.getName();
            if(Tools.isNotEmpty(selected.getPassword())) needPasswordToDelete = Boolean.TRUE;
            else needPasswordToDelete = Boolean.FALSE;
        }

        originalEntity.setEditorFields(this);
    }

    public void toReservationEntity(ReservationEntity originalEntity, ReservationRepository rr, HttpServletRequest request, boolean skipPrepare, boolean isImporting, ProcessItemAction action) {
        //Set domain id for new entity
        if(originalEntity.getDomainId() == null) originalEntity.setDomainId(CloudToolsForCore.getDomainId());

        ReservationObjectEntity reservationObject = originalEntity.getReservationObjectForReservation();

        String error = null;
        ReservationService reservationService = new ReservationService(Prop.getInstance(request));
        if(skipPrepare == false) {
            reservationService.prepareReservationToValidation(originalEntity, Tools.isTrue(reservationObject.getReservationForAllDay()) );
        }

        boolean doCheck = true;
        if(isImporting == false && action == ProcessItemAction.EDIT) {
            //IF action is edit AND reservation object with reservation date/time wans NOT changed, we dont need to check it
            doCheck = ReservationService.wasReservationChanged(originalEntity, rr);
        }

        if(originalEntity.getId() != null && originalEntity.getId() > 0) {
            //Acceptation MUST be actual value from DB - because it can be changed using processAction, while editing
            Boolean acceptedDB = rr.findAcceptedByIdAndDomainId(originalEntity.getId(), originalEntity.getDomainId());
            originalEntity.setAccepted(acceptedDB);
        }

        if(doCheck) {
            //If reservationObject can be reserve only for whole day, we dont need to check time range (time range is set automatically)
            if(Tools.isFalse(reservationObject.getReservationForAllDay())) {
                //In this case we can select even time, so we need check time validity
                error = reservationService.checkReservationTimeRangeValidity(originalEntity, reservationObject);
                //Check if error was returned
                if(error != null) reservationService.throwError(error);
            }

            //Validate reservation range
            error = reservationService.checkReservationOverlappingValidity(originalEntity, reservationObject, rr, isImporting);
            if(error != null) reservationService.throwError(error);

            //Now decide if reservation need acceptation or not
            if(!ReservationService.acceptation(originalEntity, request))  {
                // !! Send mail - > in after save because we dont have ID yet
            }
        }
    }
}