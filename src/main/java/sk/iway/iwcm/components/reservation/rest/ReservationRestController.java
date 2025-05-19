package sk.iway.iwcm.components.reservation.rest;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.reservation.jpa.ReservationEditorFields;
import sk.iway.iwcm.components.reservation.jpa.ReservationEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectPriceRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectTimesEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectTimesRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationRepository;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRequest;
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
    private final ReservationObjectPriceRepository ropr;
    private static final String SESSION_ATRIBUTE_NAME = "reservationDeletePasswords";
    private static final String RESERVATION_OBJECT_ID = "reservationObjectId";

    @Autowired
    public ReservationRestController(ReservationRepository reservationRepository, ReservationObjectRepository ror, ReservationObjectTimesRepository rotr, ReservationObjectPriceRepository ropr) {
        super(reservationRepository);
        this.reservationRepository = reservationRepository;
        this.ror = ror;
        this.rotr = rotr;
        this.ropr = ropr;
    }

    @Override
    public Page<ReservationEntity> getAllItems(Pageable pageable) {
        DatatablePageImpl<ReservationEntity> page = new DatatablePageImpl<>( reservationRepository.findAllByDomainId(CloudToolsForCore.getDomainId(), pageable) );

        List<ReservationObjectEntity> reservationObjects = ror.findAllByDomainId(CloudToolsForCore.getDomainId());
        page.addOptions(RESERVATION_OBJECT_ID, reservationObjects, "name", "id", false);

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
        ref.fromReservationEntity(entity, action, getRequest());
        return entity;
    }

    @Override
    public ReservationEntity processToEntity(ReservationEntity entity, ProcessItemAction action) {
        if(entity.getEditorFields() != null) {
            //Check if reservation object ID is set
            if(entity.getReservationObjectId() != null) {
                entity.setReservationObjectForReservation( ror.findFirstByIdAndDomainId(entity.getReservationObjectId(), CloudToolsForCore.getDomainId()).orElse(null) );
                entity.getEditorFields().toReservationEntity(entity, reservationRepository, getRequest(), false, isImporting(), action);
            } else throwError("");
        }

        return entity;
    }

    @Override
    public boolean processAction(ReservationEntity entity, String action) {

        String unexpectedError = getProp().getText("html_area.insert_image.error_occured");
        String errorTitle = getProp().getText("reservation.reservations.password_for_delete.error_title");
        String acceptanceTitle = getProp().getText("reservation.reservations.acceptance_notification");


        Identity loggedUser = UsersDB.getCurrentUser(getRequest());
        Long objectId = entity.getReservationObjectId();

        //We are doing new delete process so clean passwords from session
        if(action.equals("prepareVerify")) {
            getRequest().getSession().removeAttribute(SESSION_ATRIBUTE_NAME);
            return true;
        }

        //Save combination password - reservationObjectId into session
        if(action.equals("verify")) {
            String customData = getRequest().getParameter("customData");
            if(customData != null && !customData.isEmpty()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<Long, String> deletePasswords = (Map<Long, String>)getRequest().getSession().getAttribute(SESSION_ATRIBUTE_NAME);
                    if(deletePasswords == null) deletePasswords = new HashMap<>();

                    JSONObject jsonObject = new JSONObject(customData);
                    deletePasswords.put(jsonObject.getLong(RESERVATION_OBJECT_ID), (String)jsonObject.get("password"));

                    getRequest().getSession().setAttribute(SESSION_ATRIBUTE_NAME, deletePasswords);
                } catch (Exception err){
                    addNotify(new NotifyBean(errorTitle, unexpectedError, NotifyType.ERROR, 15000));
                    return true;
                }
            }
            return true;
        }

        //Check id
        if(objectId == null) {
            addNotify(new NotifyBean(acceptanceTitle, unexpectedError, NotifyType.ERROR, 15000));
            return true;
        }

        ReservationObjectEntity reservationObject = ror.findFirstByIdAndDomainId(entity.getReservationObjectId(), CloudToolsForCore.getDomainId()).orElse(null);
        String objectAccepterEmail = reservationObject.getEmailAccepter();

        //Check if reservation needs acceptation
        if(Tools.isTrue(reservationObject.getMustAccepted())) {
            //This reservation needs acceptation, check if accepter email is set
            if(objectAccepterEmail == null || objectAccepterEmail.isEmpty()) {
                //Error because there is no accepter email
                addNotify(new NotifyBean(acceptanceTitle, unexpectedError, NotifyType.ERROR, 15000));
                return true;
            }

            //Check if logged user have right to approve/reject reservation upon reservationObject
            if(!loggedUser.getEmail().equals(reservationObject.getEmailAccepter())) {
                //Error because logged user can't accept/reject reservation
                addNotify(new NotifyBean(acceptanceTitle, getProp().getText("reservation.reservations.no_right") + " <strong>" +  reservationObject.getName() + "</strong>", NotifyType.ERROR, 15000));
                return true;
            }
        }

        ReservationService reservationService = new ReservationService(getProp());
        if("approve".equals(action)) {
            //Is this status already set ?
            if(Tools.isTrue(entity.getAccepted())) {
                addNotify(new NotifyBean(acceptanceTitle, getProp().getText("reservation.reservations.reservation_accepted_succ"), NotifyType.SUCCESS, 15000));
                return true;
            }

            //FIRST check if reservation is still valid !!

            if(Tools.isFalse(reservationObject.getReservationForAllDay())) {
                String error = reservationService.checkReservationTimeRangeValidity(entity, reservationObject);
                if(error != null) {
                    //REJECT reservation auto
                    entity.setAccepted(Boolean.FALSE);

                    //Send email
                    reservationService.sendConfirmationEmail(entity, getRequest(), loggedUser);

                    //Save changes entity
                    reservationRepository.save(entity);
                    addNotify(new NotifyBean(acceptanceTitle, getProp().getText(error), NotifyType.ERROR, 15000));
                    return true;
                }
            }

            String error2 = reservationService.checkReservationOverlappingValidity(entity, reservationObject, reservationRepository, false);
            if(error2 != null) {
                //REJECT entity auto
                entity.setAccepted(Boolean.FALSE);

                //Send email
                reservationService.sendConfirmationEmail(entity, getRequest(), loggedUser);

                //Save changes entity
                reservationRepository.save(entity);
                addNotify(new NotifyBean(acceptanceTitle, getProp().getText(error2), NotifyType.ERROR, 15000));
                return true;
            }

            //Reservation was approved
            entity.setAccepted(Boolean.TRUE);

            //Send email
            reservationService.sendConfirmationEmail(entity, getRequest(), loggedUser);
            addNotify(new NotifyBean(acceptanceTitle, getProp().getText("reservation.reservations.reservation_accepted_succ"), NotifyType.SUCCESS, 15000));
        } else if("reject".equals(action)) {
            //Is this status already set ?
            if(Tools.isFalse(entity.getAccepted())) {
                addNotify(new NotifyBean(acceptanceTitle, getProp().getText("reservation.reservations.reservation_rejected_succ"), NotifyType.SUCCESS, 15000));
                return true;
            }
            //Reservation was rejected
            entity.setAccepted(Boolean.FALSE);

            //Send email
            reservationService.sendConfirmationEmail(entity, getRequest(), loggedUser);
            addNotify(new NotifyBean(acceptanceTitle, getProp().getText("reservation.reservations.reservation_rejected_succ"), NotifyType.SUCCESS, 15000));
        } else if("reset".equals(action)) {
            //Is this status already set ?
            if(entity.getAccepted() == null) {
                addNotify(new NotifyBean(acceptanceTitle, getProp().getText("reservation.reservations.reservation_reset_succ"), NotifyType.SUCCESS, 15000));
                return true;
            }

            //Reservation now will waiting for acceptation
            entity.setAccepted(null);

            //Send email
            reservationService.sendConfirmationEmail(entity, getRequest(), loggedUser);
            addNotify(new NotifyBean(acceptanceTitle, getProp().getText("reservation.reservations.reservation_reset_succ"), NotifyType.SUCCESS, 15000));
        }

        //Save changes entity
        reservationRepository.save(entity);

        return true;
    }

    @Override
	public void beforeSave(ReservationEntity entity) {
        //Email is NULL, because this is ADMIN section, so admin must be logged in
        int userId = ReservationService.getUserToPay(null, entity.getId(), reservationRepository, getRequest());

        //INSERT action
        if(entity.getId() == null || entity.getId() == -1) {
            entity.setDomainId(CloudToolsForCore.getDomainId());
            entity.setUserId(userId);
            entity.setId(-1L);
        }

        //Set price of reservation
        entity.setPrice( ReservationService.calculateReservationPrice(entity, userId, ror, ropr) );
	}

    @Override
    public void addSpecSearch(Map<String, String> params, List<Predicate> predicates, Root<ReservationEntity> root, CriteriaBuilder builder) {
        //Search based on reservationObjectName, we find inside DB then in columns reservationObjectId
        String searchReservationObjectName = params.get("searchEditorFields.selectedReservation");
        if (searchReservationObjectName != null)
            addSpecSearchReservationObjectName(searchReservationObjectName, RESERVATION_OBJECT_ID, predicates, root, builder);

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
	private static void addSpecSearchReservationObjectName(String paramValue, String jpaProperty, List<Predicate> predicates, Root<ReservationEntity> root, CriteriaBuilder builder) {
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

		List<Integer> reservationObejctIds = (new SimpleQuery()).forListInteger("SELECT DISTINCT reservation_object_id FROM reservation_object WHERE name " + operator + " ?", prepend + valueClean + append);
		if(reservationObejctIds.isEmpty() == false) predicates.add(root.get(jpaProperty).in(reservationObejctIds));
		else predicates.add(builder.equal(root.get(jpaProperty), Integer.MAX_VALUE));
	}

    @Override
    public boolean deleteItem(ReservationEntity entity, long id) {
        String unexpectedError = getProp().getText("html_area.insert_image.error_occured");
        String errorTitle = getProp().getText("reservation.reservations.password_for_delete.error_title");

        //Check if reservation object need password
        if(Tools.isTrue(entity.getEditorFields().getNeedPasswordToDelete())) {
            Prop prop = getProp();
            Optional<ReservationObjectEntity> optReservationObject = ror.findFirstByIdAndDomainId(entity.getReservationObjectId(), CloudToolsForCore.getDomainId());

            if(!optReservationObject.isPresent()) {
                addNotify(new NotifyBean(errorTitle, getProp().getText("reservation.reservations.password_for_delete.error_entity_not_found"), NotifyType.ERROR, 15000));
                return false;
            }

            @SuppressWarnings("unchecked")
            Map<Long, String> deletePasswords = (Map<Long, String>)getRequest().getSession().getAttribute(SESSION_ATRIBUTE_NAME);

            String password = deletePasswords.get(entity.getReservationObjectId());

            if(password == null) {
                addNotify(new NotifyBean(errorTitle, unexpectedError, NotifyType.ERROR, 15000));
                return false;
            }

            //Verify password
            if(optReservationObject.get().checkPasswordAndHashEquality(password, optReservationObject.get().getPassword())) reservationRepository.delete(entity);
            else {
                String errorText = prop.getText("reservation.reservations.password_for_delete.error_bad_password_1") + " <b>" + optReservationObject.get().getName() + " </b> ";
                errorText += prop.getText("reservation.reservations.password_for_delete.error_bad_password_2") + " <b>" + id + " </b> ";
                errorText += prop.getText("reservation.reservations.password_for_delete.error_bad_password_3");
                addNotify(new NotifyBean(errorTitle, errorText, NotifyType.ERROR, 15000));
            }
        } else reservationRepository.delete(entity);

        return true;
    }

    @Override
    public void afterSave(ReservationEntity entity, ReservationEntity saved) {
        if(!isImporting()) {
            //Check if we must send Acceptation email
            ReservationObjectEntity reservationObject = entity.getReservationObjectForReservation();
            ReservationService reservationService = new ReservationService(getProp());

            if(reservationObject != null && Tools.isTrue(reservationObject.getMustAccepted()) && entity.getAccepted() == null) {
                //for some reason time part is lost even when in DB its saved good
                entity.setDateFrom( DefaultTimeValueConverter.combineDateWithTime(entity.getDateFrom(), entity.getEditorFields().getReservationTimeFrom()) );
                entity.setDateTo( DefaultTimeValueConverter.combineDateWithTime(entity.getDateTo(), entity.getEditorFields().getReservationTimeTo()) );
                reservationService.sendAcceptationEmail(entity, getRequest());
            }

            //Send email only if reservation was created - new only
            if(entity.getId() == -1)
                reservationService.sendCreatedReservationEmail(saved, getRequest());
        }
    }

    @Override
    public void validateEditor(HttpServletRequest request, DatatableRequest<Long, ReservationEntity> target, Identity user, Errors errors, Long id, ReservationEntity entity) {
        if(target.getAction().equals("create") || target.getAction().equals("edit") && entity.getEditorFields() != null) {
            //Is object set as ALL DAY reservation ? - if yes, we do not need to check time
            Boolean allDay = false;
            if (entity.getReservationObjectId()!=null) allDay = ror.isReservationForAllDay(entity.getReservationObjectId());

            if(Tools.isTrue(allDay)) {
                if(entity.getEditorFields().getArrivingTime() == null) {
                    //errors.rejectValue("errorField.editorFields.arrivingTime", null, getProp().getText("javax.validation.constraints.NotBlank.message"));
                    if (entity.getEditorFields().getReservationTimeFrom() != null) entity.getEditorFields().setArrivingTime(entity.getEditorFields().getReservationTimeFrom());
                    else entity.getEditorFields().setArrivingTime(ReservationService.getArrivalTime(entity));
                }
                if(entity.getEditorFields().getDepartureTime() == null) {
                    //errors.rejectValue("errorField.editorFields.departureTime", null, getProp().getText("javax.validation.constraints.NotBlank.message"));
                    if (entity.getEditorFields().getReservationTimeTo() != null) entity.getEditorFields().setDepartureTime(entity.getEditorFields().getReservationTimeTo());
                    else entity.getEditorFields().setDepartureTime(ReservationService.getDepartureTime(entity));
                }
            } else {
                if(entity.getEditorFields().getReservationTimeFrom() == null)
                    errors.rejectValue("errorField.editorFields.reservationTimeFrom", null, getProp().getText("javax.validation.constraints.NotBlank.message"));
                if(entity.getEditorFields().getReservationTimeTo() == null)
                    errors.rejectValue("errorField.editorFields.reservationTimeTo", null, getProp().getText("javax.validation.constraints.NotBlank.message"));
            }

            if(Tools.isNotEmpty(entity.getEmail()) && Tools.isEmail(entity.getEmail())==false) {
                errors.rejectValue("errorField.email", null, getProp().getText("javax.validation.constraints.Email.message"));
            }
        }

        super.validateEditor(request, target, user, errors, id, entity);
    }

    @RequestMapping(
        value="/compute-reservation-price",
        params={"date-from", "date-to", "time-from", "time-to", "object-id"})
    public BigDecimal computeReservationPrice(
        @RequestParam("date-from") Long dateFrom,
        @RequestParam("date-to") Long dateTo,
        @RequestParam("time-from") Long timeFrom,
        @RequestParam("time-to") Long timeTo,
        @RequestParam("object-id") Long objectId,
        @RequestParam("reservation-id") Long reservationId) {
            //Email is NULL, because this is ADMIN section, so admin must be logged in
            int userId = ReservationService.getUserToPay(null, reservationId, reservationRepository, getRequest());
            return ReservationService.calculateReservationPrice(dateFrom, dateTo, timeFrom, timeTo, objectId, userId, ror, ropr);
        }

    @RequestMapping(path={"/reservation-object/{objectId}"})
    public ReservationObjectEntity getReservationObject(@PathVariable Long objectId) {
        if(objectId != null) {
            //Get reservation object
            ReservationObjectEntity reservationObject = ror.findFirstByIdAndDomainId(objectId, CloudToolsForCore.getDomainId()).orElse(null);
            if(reservationObject == null) {
                throwError("Reservation object was not found.");
                return null;
            }

            //First set default values
            String defaultTimeRangeString = getTimeStringRange(reservationObject.getReservationTimeFrom(), reservationObject.getReservationTimeTo());
            HashMap<Integer, String> objectTimesInfo = new HashMap<>();
            for(int day = 1; day <= 7; day++)
                objectTimesInfo.put(day, defaultTimeRangeString);

            for(ReservationObjectTimesEntity objectTime : reservationObject.getReservationObjectTimeEntities()) {
                //Key (Integer) is day of week 1,2 ... 7
                //Value (String) is combination timeFrom + "-" + timeTo (HH:mm format)
                String timeRangeString = getTimeStringRange(objectTime.getTimeFrom(), objectTime.getTimeTo());
                objectTimesInfo.put(objectTime.getDay(), timeRangeString);
            }

            reservationObject.setObjectTimesInfo(objectTimesInfo);

            return reservationObject;
        } else throwError("");
        return null;
    }

    @RequestMapping(
        value="/check-reservation-validity",
        params={"date-from", "date-to", "time-from", "time-to", "object-id", "reservation-id", "allow-history-save", "allow-overbooking", "isDuplicate"})
    public String checkReservationValidity(
        @RequestParam("date-from") Long dateFrom,
        @RequestParam("date-to") Long dateTo,
        @RequestParam("time-from") Long timeFrom, //or aka arrivalTime if it's all day reservation
        @RequestParam("time-to") Long timeTo,     //or aka departureTime if it's all day reservation
        @RequestParam("object-id") Long objectId,
        @RequestParam("reservation-id") Long reservationId,
        @RequestParam("allow-history-save") boolean allowHistorySave,
        @RequestParam("allow-overbooking") boolean allowOverbooking,
        @RequestParam("isDuplicate") boolean isDuplicate) {

        String unexpectedError = getProp().getText("html_area.insert_image.error_occured");

        if(dateFrom == null || dateTo == null || timeFrom == null || timeTo == null || objectId == null)
            return unexpectedError;

        //Get reservation object
        ReservationObjectEntity reservationObject = ror.findFirstByIdAndDomainId(objectId, CloudToolsForCore.getDomainId()).orElse(null);
        if(reservationObject == null) {
            throwError("Reservation object was not found.");
            return null;
        }

        //We need keep data as fresh for validation, so get joined object times from DB
        reservationObject.setReservationObjectTimeEntities( rotr.findAllByObjectIdAndDomainId(reservationObject.getId(), CloudToolsForCore.getDomainId()) );

        //Create reservation entity (just for test purpose)
        ReservationEntity reservation = new ReservationEntity();

        //Set reservation
        reservation.setDateFrom(new Date(dateFrom));
        reservation.setDateTo(new Date(dateTo));

        if(isDuplicate) {
            reservation.setId(Long.valueOf(-1));
        } else {
            reservation.setId(reservationId);
        }

        ReservationEditorFields ef = new ReservationEditorFields();

        if(Tools.isTrue(reservationObject.getReservationForAllDay())) {
            ef.setArrivingTime(DefaultTimeValueConverter.getValidTimeValue(new Date(timeFrom)));
            ef.setDepartureTime(DefaultTimeValueConverter.getValidTimeValue(new Date(timeTo)));
        } else {
            ef.setReservationTimeFrom(DefaultTimeValueConverter.getValidTimeValue(new Date(timeFrom)));
            ef.setReservationTimeTo(DefaultTimeValueConverter.getValidTimeValue(new Date(timeTo)));
        }

        ef.setAllowHistorySave(allowHistorySave);
        ef.setAllowOverbooking(allowOverbooking);

        reservation.setEditorFields(ef);

        ReservationService reservationService = new ReservationService(getProp());

        //Prepare entity
        try{
            reservationService.prepareReservationToValidation(reservation, Tools.isTrue(reservationObject.getReservationForAllDay()));
        } catch(IllegalArgumentException e) {
            return unexpectedError;
        }

        String error = null;
        if(Tools.isFalse(reservationObject.getReservationForAllDay())) {
            error = reservationService.checkReservationTimeRangeValidity(reservation, reservationObject);
            if(error != null) return error;
        }

        error = reservationService.checkReservationOverlappingValidity(reservation, reservationObject, reservationRepository, false);
        if(error != null) return error;

        return null;
    }

    private String getTimeStringRange(Date start, Date end) {
        if(start == null || end == null) return "";
        return new SimpleDateFormat("HH:mm").format(start) + " - " + new SimpleDateFormat("HH:mm").format(end);
    }
}