package sk.iway.iwcm.components.reservation.rest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.reservation.jpa.ReservationEditorFields;
import sk.iway.iwcm.components.reservation.jpa.ReservationEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectTimesEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectTimesRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationRepository;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.system.datatable.NotifyBean;
import sk.iway.iwcm.system.datatable.NotifyBean.NotifyType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.jpa.DefaultTimeValueConverter;
import sk.iway.iwcm.users.UsersDB;

@RestController
@RequestMapping("/admin/rest/reservation/reservations")
@PreAuthorize("@WebjetSecurityService.hasPermission('cmp_reservation')")
@Datatable
public class ReservationRestController extends DatatableRestControllerV2<ReservationEntity, Long> {

    private final ReservationRepository reservationRepository;
    private final ReservationObjectRepository ror;
    private final ReservationObjectTimesRepository rotr;
    private static final String SESSION_ATRIBUTE_NAME = "reservationDeletePasswords";

    @Autowired
    public ReservationRestController(ReservationRepository reservationRepository, ReservationObjectRepository ror, ReservationObjectTimesRepository rotr) {
        super(reservationRepository);
        this. reservationRepository = reservationRepository;
        this.ror = ror;
        this.rotr = rotr;
    }

    @Override
    public Page<ReservationEntity> getAllItems(Pageable pageable) {
        //Use custom select with domain id
        Page<ReservationEntity> items = reservationRepository.findAllByDomainId(CloudToolsForCore.getDomainId(), pageable);

        DatatablePageImpl<ReservationEntity> page = new DatatablePageImpl<>(items);

        List<ReservationObjectEntity> reservationObjects = ror.findAllByDomainId(CloudToolsForCore.getDomainId());
        page.addOptions("reservationObjectId", reservationObjects, "name", "id", false);

        //
        processFromEntity(page, ProcessItemAction.GETALL);
        return page;
    }

    @Override
    public ReservationEntity getOneItem(long id) {
        ReservationEntity entity;
        if(id == -1) {
            entity = new ReservationEntity();
            processFromEntity(entity, ProcessItemAction.CREATE);
        } else {
            entity = reservationRepository.findFirstByIdAndDomainId(id, CloudToolsForCore.getDomainId()).orElse(null);
            processFromEntity(entity, ProcessItemAction.EDIT);
        }

        return entity;
    }

    @Override
    public ReservationEntity processFromEntity(ReservationEntity entity, ProcessItemAction action) {
        //If editorFields if null create new
        ReservationEditorFields ref = entity.getEditorFields() == null ? new ReservationEditorFields() : entity.getEditorFields();

        ref.fromReservationEntity(entity, action, ror, getRequest());

        return entity;
    }

    @Override
    public ReservationEntity processToEntity(ReservationEntity entity, ProcessItemAction action) {
        if(entity.getEditorFields() != null) {
            //Check if reservation object ID is set
            if(entity.getReservationObjectId() != null) {
                //Get reservation obejct
                ReservationObjectEntity reservationObject = ror.findFirstByIdAndDomainId(entity.getReservationObjectId().longValue(), CloudToolsForCore.getDomainId()).orElse(null);

                //Get reservation object times values
                List<ReservationObjectTimesEntity> reservationObjectTimes = rotr.findAllByObjectIdAndDomainId(entity.getReservationObjectId(), CloudToolsForCore.getDomainId());

                //Get all other reservations for this object
                List<ReservationEntity> otherReservations = reservationRepository.findAllByReservationObjectIdAndDomainId(entity.getReservationObjectId(), CloudToolsForCore.getDomainId());

                entity.getEditorFields().toReservationEntity(entity, reservationObject, reservationRepository, reservationObjectTimes, otherReservations, getRequest());

            } else throwError("");
        }

        return entity;
    }

    @RequestMapping(path={"/reservation_object/{objectId}"})
    public ReservationObjectEntity getReservationObject(@PathVariable Integer objectId) {
        if(objectId != null) {
            //Get reservation object
            ReservationObjectEntity reservationObject = ror.findFirstByIdAndDomainId(objectId.longValue(), CloudToolsForCore.getDomainId()).orElse(null);

            //Get reservation object times values
            List<ReservationObjectTimesEntity> reservationObjectTimes = rotr.findAllByObjectIdAndDomainId(reservationObject.getId().intValue(), CloudToolsForCore.getDomainId());

            //First set default values
            String defaultTimeRangeString = getTimeStringRange(reservationObject.getReservationTimeFrom(), reservationObject.getReservationTimeTo());
            HashMap<Integer, String> objectTimesInfo = new HashMap<>();
            for(int day = 1; day <= 7; day++)
                objectTimesInfo.put(day, defaultTimeRangeString);

            if(reservationObjectTimes != null) {
                for(ReservationObjectTimesEntity objectTime : reservationObjectTimes) {
                    //Key (Integer) is day of week 1,2 ... 7
                    //Value (String) is combination timeFrom + "-" + timeTo (HH:mm format)
                    String timeRangeString = getTimeStringRange(objectTime.getTimeFrom(), objectTime.getTimeTo());
                    objectTimesInfo.put(objectTime.getDay(), timeRangeString);
                }
            }
            reservationObject.setObjectTimesInfo(objectTimesInfo);

            return reservationObject;
        } else throwError("");
        return null;
    }


    @RequestMapping(
        value="/checkReservationValidity",
        params={"dateFrom", "dateTo", "timeFrom", "timeTo", "objectId", "reservationId"})
    @ResponseBody
    public String checkReservationValidity(
        @RequestParam("dateFrom") Long dateFrom,
        @RequestParam("dateTo") Long dateTo,
        @RequestParam("timeFrom") Long timeFrom,
        @RequestParam("timeTo") Long timeTo,
        @RequestParam("objectId") Long objectId,
        @RequestParam("reservationId") Long reservationId) {

        Prop prop = Prop.getInstance();

        if(dateFrom == null || dateTo == null || timeFrom == null || timeTo == null || objectId == null)
            return prop.getText("html_area.insert_image.error_occured");

        //Get reservation object
        ReservationObjectEntity reservationObject = ror.findFirstByIdAndDomainId(objectId.longValue(), CloudToolsForCore.getDomainId()).orElse(null);

        //Get reservation object times values
        List<ReservationObjectTimesEntity> reservationObjectTimes = rotr.findAllByObjectIdAndDomainId(objectId.intValue(), CloudToolsForCore.getDomainId());

        //Create reservation entity (just for test purpose)
        ReservationEntity reservation = new ReservationEntity();

        //Set reservation
        reservation.setDateFrom(new Date(dateFrom));
        reservation.setDateTo(new Date(dateTo));
        reservation.setId(reservationId);
        ReservationEditorFields ef = new ReservationEditorFields();
        ef.setReservationTimeFrom(DefaultTimeValueConverter.getValidTimeValue(new Date(timeFrom)));
        ef.setReservationTimeTo(DefaultTimeValueConverter.getValidTimeValue(new Date(timeTo)));
        reservation.setEditorFields(ef);

        ReservationService reservationService = new ReservationService();

        //Prepare entity
        reservationService.prepareReservationToValidation(reservation, reservationObject.getReservationForAllDay());

        String error = null;
        if(!reservationObject.getReservationForAllDay()) {
            error = reservationService.checkReservationTimeRangeValidity(reservation, reservationObject, reservationObjectTimes);
            if(error != null) return error;
        }

        error = reservationService.checkReservationOverlapingValidity(reservation, reservationObject, reservationRepository);
        if(error != null) return error;

        return null;
    }

    private String getTimeStringRange(Date start, Date end) {
        if(start == null || end == null) return "";

        String timeRange = new SimpleDateFormat("HH:mm").format(start);
        timeRange += " - " + new SimpleDateFormat("HH:mm").format(end);

        return timeRange;
    }

    @Override
    public boolean processAction(ReservationEntity entity, String action) {

        Identity loggedUser = UsersDB.getCurrentUser(getRequest());
        Integer objectId = entity.getReservationObjectId();

        //We are doing new delete process so clean passwords from sessio
        if(action.equals("prepareVerify")) {
            getRequest().getSession().removeAttribute(SESSION_ATRIBUTE_NAME);
            return true;
        }

        //Save combination password - reservationObejctId into session
        if(action.equals("verify")) {
            String customData = getRequest().getParameter("customData");
            if(customData != null && !customData.isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<Integer, String> deletePasswords = (Map<Integer, String>)getRequest().getSession().getAttribute(SESSION_ATRIBUTE_NAME);
                    if(deletePasswords == null) deletePasswords = new HashMap<>();
                    JSONObject jsonObject = new JSONObject(customData);
                    deletePasswords.put((Integer)jsonObject.get("reservationObjectId"), (String)jsonObject.get("password"));
                    getRequest().getSession().setAttribute(SESSION_ATRIBUTE_NAME, deletePasswords);
                } catch (Exception err){
                    addNotify(new NotifyBean(getProp().getText("reservation.reservations.password_for_delete.error_title"), getProp().getText("html_area.insert_image.error_occured"), NotifyType.ERROR, 15000));
                    return true;
                }
            }
            return true;
        }

        //Check id
        if(objectId == null) {
            addNotify(new NotifyBean(getProp().getText("reservation.reservations.acceptance_notification"), getProp().getText("html_area.insert_image.error_occured"), NotifyType.ERROR, 15000));
            return true;
        }

        ReservationObjectEntity reservationObject = ror.findFirstByIdAndDomainId(entity.getReservationObjectId().longValue(), CloudToolsForCore.getDomainId()).orElse(null);
        String objectAccepterEmail = reservationObject.getEmailAccepter();

        //Check if reservation needs acceptation
        if(reservationObject.getMustAccepted()) {
            //This reservation needs acceptation, check if accepter email is set
            if(objectAccepterEmail == null || objectAccepterEmail.isEmpty()) {
                //Error because there is no accepter email
                addNotify(new NotifyBean(getProp().getText("reservation.reservations.acceptance_notification"), getProp().getText("html_area.insert_image.error_occured"), NotifyType.ERROR, 15000));
                return true;
            }

            //Check if logged user have right to approve/reject reservation upon reservationObject
            if(!loggedUser.getEmail().equals(reservationObject.getEmailAccepter())) {
                //Error because logged user can't accept/reject reservation
                addNotify(new NotifyBean(getProp().getText("reservation.reservations.acceptance_notification"), getProp().getText("reservation.reservations.no_right") + " <strong>" +  reservationObject.getName() + "</strong>", NotifyType.ERROR, 15000));
                return true;
            }
        }

        ReservationService reservationService = new ReservationService();
        if("approve".equals(action)) {
            //Is this status already set ?
            if(entity.getAccepted() != null && entity.getAccepted() == true) {
                addNotify(new NotifyBean(getProp().getText("reservation.reservations.acceptance_notification"), getProp().getText("reservation.reservations.reservation_accepted_succ"), NotifyType.SUCCESS, 15000));
                return true;
            }

            //FIRST check if reservation is still valid !!

            //Get reservation object times values
            List<ReservationObjectTimesEntity> reservationObjectTimes = rotr.findAllByObjectIdAndDomainId(objectId.intValue(), CloudToolsForCore.getDomainId());

            if(!reservationObject.getReservationForAllDay()) {
                String error = reservationService.checkReservationTimeRangeValidity(entity, reservationObject, reservationObjectTimes);
                if(error != null) {
                    //REJECT reservation auto
                    entity.setAccepted(false);

                    //Send email
                    reservationService.sendConfirmationEmail(entity, reservationObject, getRequest(), loggedUser.getFullName());

                    //Save changes entity
                    reservationRepository.save(entity);
                    addNotify(new NotifyBean(getProp().getText("reservation.reservations.acceptance_notification_error"), getProp().getText(error), NotifyType.ERROR, 15000));
                    return true;
                }
            }

            String error2 = reservationService.checkReservationOverlapingValidity(entity, reservationObject, reservationRepository);
            if(error2 != null) {
                //REJECT entity auto
                entity.setAccepted(false);

                //Send email
                reservationService.sendConfirmationEmail(entity, reservationObject, getRequest(), loggedUser.getFullName());

                //Save changes entity
                reservationRepository.save(entity);
                addNotify(new NotifyBean(getProp().getText("reservation.reservations.acceptance_notification_error"), getProp().getText(error2), NotifyType.ERROR, 15000));
                return true;
            }

            //Reservation was approved
            entity.setAccepted(true);

            //Send email
            reservationService.sendConfirmationEmail(entity, reservationObject, getRequest(), loggedUser.getFullName());
            addNotify(new NotifyBean(getProp().getText("reservation.reservations.acceptance_notification"), getProp().getText("reservation.reservations.reservation_accepted_succ"), NotifyType.SUCCESS, 15000));
        } else if("reject".equals(action)) {
            //Is this status already set ?
            if(entity.getAccepted() != null && entity.getAccepted() == false) {
                addNotify(new NotifyBean(getProp().getText("reservation.reservations.acceptance_notification"), getProp().getText("reservation.reservations.reservation_rejected_succ"), NotifyType.SUCCESS, 15000));
                return true;
            }
            //Reservation was rejected
            entity.setAccepted(false);

            //Send email
            reservationService.sendConfirmationEmail(entity, reservationObject, getRequest(), loggedUser.getFullName());
            addNotify(new NotifyBean(getProp().getText("reservation.reservations.acceptance_notification"), getProp().getText("reservation.reservations.reservation_rejected_succ"), NotifyType.SUCCESS, 15000));
        } else if("reset".equals(action)) {
            //Is this status already set ?
            if(entity.getAccepted() == null) {
                addNotify(new NotifyBean(getProp().getText("reservation.reservations.acceptance_notification"), getProp().getText("reservation.reservations.reservation_reset_succ"), NotifyType.SUCCESS, 15000));
                return true;
            }

            //Reservation now will waiting for acceptation
            entity.setAccepted(null);

            //Send email
            reservationService.sendConfirmationEmail(entity, reservationObject, getRequest(), loggedUser.getFullName());
            addNotify(new NotifyBean(getProp().getText("reservation.reservations.acceptance_notification"), getProp().getText("reservation.reservations.reservation_reset_succ"), NotifyType.SUCCESS, 15000));
        }

        //Save changes entity
        reservationRepository.save(entity);

        return true;
    }

    @Override
	public void beforeSave(ReservationEntity entity) {
        //Set domain id, not null
        if(entity.getId() == null || entity.getId() == -1)
            entity.setDomainId(CloudToolsForCore.getDomainId());
	}

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<ReservationEntity> root, CriteriaBuilder builder) {

        //Search based on reservationObjectName, we find inside DB then in columns reservationObjectId
        String searchReservationObjectName = params.get("searchEditorFields.selectedReservation");
        if (searchReservationObjectName != null) {
            addSpecSearchReservationObjetcName(searchReservationObjectName, "reservationObjectId", predicates, root, builder);
        }

        super.addSpecSearch(params, predicates, root, builder);
    }

    /**
	 * Special search to filter reservations based on reservationObjectName (paramValue). Because ReservationEntity do not contain column with reservationObjectName (only reservationObjectId),
	 * we will use that reservationObjectName to get list of reservationObjectIds and than use jpaProperty.in() to filter only thus reservations with reservationObvejctId in this list.
	 * @param paramValue - searched reservation obejct name
	 * @param jpaProperty - name of JPA property, that must by inside returned reservation obejct ids list
	 * @param predicates
	 * @param root
	 * @param builder
	 */
	private static void addSpecSearchReservationObjetcName(String paramValue, String jpaProperty, List<Predicate> predicates, Root<ReservationEntity> root, CriteriaBuilder builder) {
		String valueClean = DatatableRestControllerV2.getCleanValue(paramValue);

		String operator = "LIKE";
		String prepend = "%";
		String append = "%";

		if (paramValue.startsWith("^") && paramValue.endsWith("$")) {
			operator = "=";
			prepend = "";
			append = "";
		} else if (paramValue.startsWith("^")) {
			prepend = "";
		} else if (paramValue.endsWith("$")) {
			append = "";
		}

		List<Integer> reservationObejctIds = (new SimpleQuery()).forListInteger("SELECT DISTINCT reservation_object_id FROM reservation_object WHERE name " + operator +" ?", prepend + valueClean + append);
		if(reservationObejctIds.isEmpty() == false) predicates.add(root.get(jpaProperty).in(reservationObejctIds));
		else predicates.add(builder.equal(root.get(jpaProperty), Integer.MAX_VALUE));
	}

    @Override
    public boolean deleteItem(ReservationEntity entity, long id) {
        //Check if reservation obejct need password
        if(Boolean.TRUE.equals(entity.getEditorFields().getNeedPasswordToDelete())) {
            Prop prop = getProp();
            Optional<ReservationObjectEntity> optReservationObejct = ror.findFirstByIdAndDomainId(entity.getReservationObjectId().longValue(), CloudToolsForCore.getDomainId());

            if(!optReservationObejct.isPresent()) {
                addNotify(new NotifyBean(prop.getText("reservation.reservations.password_for_delete.error_title"), getProp().getText("reservation.reservations.password_for_delete.error_entity_not_found"), NotifyType.ERROR, 15000));
                return false;
            }

            @SuppressWarnings("unchecked")
            Map<Integer, String> deletePasswords = (Map<Integer, String>)getRequest().getSession().getAttribute(SESSION_ATRIBUTE_NAME);

            String password = deletePasswords.get(entity.getReservationObjectId());

            if(password == null) {
                addNotify(new NotifyBean(prop.getText("reservation.reservations.password_for_delete.error_title"), getProp().getText("html_area.insert_image.error_occured"), NotifyType.ERROR, 15000));
                return false;
            }

            //Verify password
            if(optReservationObejct.get().checkPasswordAndHashEquality(password, optReservationObejct.get().getPassword())) reservationRepository.delete(entity);
            else {
                String errorText = prop.getText("reservation.reservations.password_for_delete.error_bad_password_1") + " <b>" + optReservationObejct.get().getName() + " </b> ";
                errorText += prop.getText("reservation.reservations.password_for_delete.error_bad_password_2") + " <b>" + id + " </b> ";
                errorText += prop.getText("reservation.reservations.password_for_delete.error_bad_password_3");
                addNotify(new NotifyBean(prop.getText("reservation.reservations.password_for_delete.error_title"), errorText, NotifyType.ERROR, 15000));
            }
        } else reservationRepository.delete(entity);

        return true;
    }
}