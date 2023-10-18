package sk.iway.iwcm.components.reservation.jpa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.jpa.DefaultTimeValueConverter;

@Getter
@Setter
public class ReservationObjectEditorFields implements Serializable {
    
    public ReservationObjectEditorFields() {}

    //With this columns we inform that we want add/change password
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="reservation.reservation_object.set_password",
        tab = "advanced",
        sortAfter = "emailAccepter",
        visible = false
    )
    private Boolean addPassword;

    //This column yust inform if password is set
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="reservation.reservation_object.is_set_password",
        tab = "advanced",
        sortAfter = "emailAccepter",
        hiddenEditor = true
    )
    private Boolean isPassword;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.reservation.admin_addObject.pass",
        visible = false,
        tab = "advanced"
    )
    private String newPassword;
    
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.reservation.admin_addObject.pass.repeat",
        visible = false,
        tab = "advanced"
    )
    private String passwordCheck;

    //Reservation time for MONDAY
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="dayfull.mo",
        visible = false,
        tab = "chooseDays",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "reservation.reservation_object.choose_days_tilte"),
                }
            )
        }
    )
    private Boolean chooseDayA;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_from",
        visible = false,
        tab = "chooseDays"
    )
    private Date reservationTimeFromA;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_to",
        visible = false,
        tab = "chooseDays"
    )
    private Date reservationTimeToA;

    //Reservation time for TUESDAY
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="dayfull.tu",
        visible = false,
        tab = "chooseDays"
    )
    private Boolean chooseDayB;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_from",
        visible = false,
        tab = "chooseDays"
    )
    private Date reservationTimeFromB;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_to",
        visible = false,
        tab = "chooseDays"
    )
    private Date reservationTimeToB;

    //Reservation time for WEDNESDAY
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="dayfull.we",
        visible = false,
        tab = "chooseDays"
    )
    private Boolean chooseDayC;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_from",
        visible = false,
        tab = "chooseDays"
    )
    private Date reservationTimeFromC;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_to",
        visible = false,
        tab = "chooseDays"
    )
    private Date reservationTimeToC;

    //Reservation time for THURSDAY
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="dayfull.th",
        visible = false,
        tab = "chooseDays"
    )
    private Boolean chooseDayD;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_from",
        visible = false,
        tab = "chooseDays"
    )
    private Date reservationTimeFromD;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_to",
        visible = false,
        tab = "chooseDays"
    )
    private Date reservationTimeToD;

    //Reservation time for FRIDAY
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="dayfull.fr",
        visible = false,
        tab = "chooseDays"
    )
    private Boolean chooseDayE;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_from",
        visible = false,
        tab = "chooseDays"
    )
    private Date reservationTimeFromE;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_to",
        visible = false,
        tab = "chooseDays"
    )
    private Date reservationTimeToE;

    //Reservation time for SATURDAY
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="dayfull.sa",
        visible = false,
        tab = "chooseDays"
    )
    private Boolean chooseDayF;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_from",
        visible = false,
        tab = "chooseDays"
    )
    private Date reservationTimeFromF;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_to",
        visible = false,
        tab = "chooseDays"
    )
    private Date reservationTimeToF;

    //Reservation time for SUNDAY
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title="dayfull.su",
        visible = false,
        tab = "chooseDays"
    )
    private Boolean chooseDayG;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_from",
        visible = false,
        tab = "chooseDays"
    )
    private Date reservationTimeFromG;

    @DataTableColumn(
        inputType = DataTableColumnType.TIME_HM,
        title="components.reservation.reservation_objects.date_to",
        visible = false,
        tab = "chooseDays"
    )
    private Date reservationTimeToG;

    //special anotation, create a ReservationObjectPrice table inside specialPrice tab
    @DataTableColumn(inputType = DataTableColumnType.DATATABLE, title = "&nbsp;",
        tab = "specialPrice",
        hidden = true,
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/reservation/reservation_object_price?objectId={id}"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.components.reservation.jpa.ReservationObjectPriceEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "false")
            }
        )
    })
    private List<ReservationObjectPriceEntity> objectPrices;

    public void fromReservationObjectEntity(ReservationObjectEntity originalEntity, ProcessItemAction action, List<ReservationObjectTimesEntity> reservationObjectTimesEntities) {

        //Most of fields are visible only in editor, so we need them only if action is "EDIT/CREATE"
        if(action != ProcessItemAction.GETALL) {
            if(originalEntity.getId() == null || originalEntity.getId() == -1) { 

                //Set default values of ReservationObjectEntity for new entity
                originalEntity.setMaxReservations(1);
                originalEntity.setCancelTimeBefor(0);
                originalEntity.setReservationTimeFrom(DefaultTimeValueConverter.getValidTimeValue(8, 0));
                originalEntity.setReservationTimeTo(DefaultTimeValueConverter.getValidTimeValue(16, 0));
                originalEntity.setTimeUnit(30);
                originalEntity.setPriceForHour(0.0);
                originalEntity.setPriceForDay(0.0);
            }

            //IF reservationObjectTimesEntities is null (or empty) set default values, else set vaules from list (load values from DB)
            if(reservationObjectTimesEntities == null || reservationObjectTimesEntities.size() == 0)
                setReservationObjectTimesDefault();
            else
                setReservationObjectTimes(reservationObjectTimesEntities);
        }

        isPassword = (originalEntity.getPassword() == null || originalEntity.getPassword().isEmpty()) ? false : true;

        originalEntity.setEditorFields(this);
    }

    //Not beautiful but fastest way to set default values
    private void setReservationObjectTimesDefault() {
        Date defaultTimeFrom = DefaultTimeValueConverter.getValidTimeValue(8, 0);
        Date defaultTimeTo = DefaultTimeValueConverter.getValidTimeValue(16, 0);

        reservationTimeFromA = defaultTimeFrom;
        reservationTimeToA = defaultTimeTo;        
        
        reservationTimeFromB = defaultTimeFrom;
        reservationTimeToB = defaultTimeTo;

        reservationTimeFromC = defaultTimeFrom;
        reservationTimeToC = defaultTimeTo;

        reservationTimeFromD = defaultTimeFrom;
        reservationTimeToD = defaultTimeTo;
                
        reservationTimeFromE = defaultTimeFrom;
        reservationTimeToE = defaultTimeTo;
                
        reservationTimeFromF = defaultTimeFrom;
        reservationTimeToF = defaultTimeTo;
                
        reservationTimeFromG = defaultTimeFrom;
        reservationTimeToG = defaultTimeTo;
    }

    private void setReservationObjectTimes(List<ReservationObjectTimesEntity> entities) {
        //First set default values
        setReservationObjectTimesDefault();

        if(entities == null || entities.size() < 1) return;

        for(ReservationObjectTimesEntity entity : entities) {
            if(entity.getDay() == 1) {
                //Monday
                chooseDayA = true;
                reservationTimeFromA = entity.getTimeFrom();
                reservationTimeToA = entity.getTimeTo();
            } else if(entity.getDay() == 2) {
                //Tuesday
                chooseDayB = true;
                reservationTimeFromB = entity.getTimeFrom();
                reservationTimeToB = entity.getTimeTo();
            } else if(entity.getDay() == 3) {
                //Wednesday
                chooseDayC = true;
                reservationTimeFromC = entity.getTimeFrom();
                reservationTimeToC = entity.getTimeTo();
            } else if(entity.getDay() == 4) {
                //Thursday
                chooseDayD = true;
                reservationTimeFromD = entity.getTimeFrom();
                reservationTimeToD = entity.getTimeTo();
            } else if(entity.getDay() == 5) {
                //Friday
                chooseDayE = true;
                reservationTimeFromE = entity.getTimeFrom();
                reservationTimeToE = entity.getTimeTo();
            } else if(entity.getDay() == 6) {
                //Saturday
                chooseDayF = true;
                reservationTimeFromF = entity.getTimeFrom();
                reservationTimeToF = entity.getTimeTo();
            } else if(entity.getDay() == 7) {
                //Sunday
                chooseDayG = true;
                reservationTimeFromG = entity.getTimeFrom();
                reservationTimeToG = entity.getTimeTo();
            }
        }
    }

    public void toReservationObjectEntity(ReservationObjectEntity originalEntity, ProcessItemAction action, ReservationObjectTimesRepository rotr) {
        //Its important to set domain id
        if(originalEntity.getDomainId() == null ) originalEntity.setDomainId(CloudToolsForCore.getDomainId());

        //Update reseravtion object times (bind to this ReservationObject entity)
        if(action != ProcessItemAction.CREATE)
            updateReservationObjectTimesInDB(originalEntity, rotr);

        //Set password
        if(addPassword) 
            originalEntity.setPassword(newPassword);

        if(originalEntity.getMustAccepted() == false)
            originalEntity.setEmailAccepter("");
    }

    private List<Boolean> getChooseDayStats() {
        List<Boolean> chooseDayStatus = new ArrayList<>();
        //Monday to Sunday
        chooseDayStatus.add(0, getChooseDayA());
        chooseDayStatus.add(1, getChooseDayB());
        chooseDayStatus.add(2, getChooseDayC());
        chooseDayStatus.add(3, getChooseDayD());
        chooseDayStatus.add(4, getChooseDayE());
        chooseDayStatus.add(5, getChooseDayF());
        chooseDayStatus.add(6, getChooseDayG());
        return chooseDayStatus;
    }

    private void updateReservationObjectTimesInDB(ReservationObjectEntity originalEntity, ReservationObjectTimesRepository rotr) {
        //Status about which day has set specific time reservation (actualy from originalEntity)
        List<Boolean> chooseDaysStatus = getChooseDayStats();

        for(int i = 0; i < chooseDaysStatus.size(); i++) {
            Boolean chooseDayStatus = chooseDaysStatus.get(i);
            boolean isInDB = false;
            ReservationObjectTimesEntity timeEntity = null;

            //Try find if this day is saved in DB (e.g. i=0 its monday)
            if(originalEntity.getId() != null && originalEntity.getId() != -1) {
                for(ReservationObjectTimesEntity entity : rotr.findAllByObjectIdAndDomainId(originalEntity.getId().intValue(), CloudToolsForCore.getDomainId())) {
                    //i+1 because in list we start from 0, and in DB we start from 1
                    if(entity.getDay() == i + 1) { 
                        isInDB = true;
                        timeEntity = entity;
                        break;
                    }
                }
            }

            //Need to delete from DB
            if(chooseDayStatus == false && isInDB && timeEntity != null) rotr.deleteById(timeEntity.getId());

            //Need to add new record to DB, OR update time values
            if(chooseDayStatus == true)
                rotr.save(convertToReservationObjectTimes(i+1, originalEntity, timeEntity));
        }
    }

    private ReservationObjectTimesEntity convertToReservationObjectTimes(int day, ReservationObjectEntity reservationObject, ReservationObjectTimesEntity timeEntity) {
        Date timeFrom = null;
        Date timeTo = null;
        ReservationObjectEditorFields roef = reservationObject.getEditorFields();

        if(timeEntity == null) {
            //If match entity if null (not saved in DB), create new entity and set needed columns
            timeEntity = new ReservationObjectTimesEntity();
            timeEntity.setDay(day);
            timeEntity.setDomainId(reservationObject.getDomainId());
            timeEntity.setObjectId(reservationObject.getId().intValue());
        }

        //Update time values
        if(day == 1) { 
            timeFrom = roef.getReservationTimeFromA();
            timeTo = roef.getReservationTimeToA();
        } else if(day == 2) { 
            timeFrom = roef.getReservationTimeFromB();
            timeTo = roef.getReservationTimeToB();
        } else if(day == 3) { 
            timeFrom = roef.getReservationTimeFromC();
            timeTo = roef.getReservationTimeToC();
        } else if(day == 4) { 
            timeFrom = roef.getReservationTimeFromD();
            timeTo = roef.getReservationTimeToD();
        } else if(day == 5) { 
            timeFrom = roef.getReservationTimeFromE();
            timeTo = roef.getReservationTimeToE();
        } else if(day == 6) { 
            timeFrom = roef.getReservationTimeFromF();
            timeTo = roef.getReservationTimeToF();
        } else if(day == 7) { 
            timeFrom = roef.getReservationTimeFromG();
            timeTo = roef.getReservationTimeToG();
        }

        timeEntity.setTimeFrom(DefaultTimeValueConverter.getValidTimeValue(timeFrom));
        timeEntity.setTimeTo(DefaultTimeValueConverter.getValidTimeValue(timeTo));

        return timeEntity;
    }
}
