package sk.iway.iwcm.components.reservation.rest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DateTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.reservation.jpa.ReservationEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectPriceEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectPriceRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectTimesEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;
import sk.iway.iwcm.system.jpa.DefaultTimeValueConverter;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

public class ReservationService {

    public static final String FE_DATEPICKER_FORMAT = "yyyy-MM-dd";
    public static final String FE_MONTHPICKER_FORMAT = "yyyy-MM";
    public static final String REGEX_YYYY_MM_DD = "\\d{4}-\\d{2}-\\d{2}";
    public static final String REGEX_YYYY_MM = "\\d{4}-\\d{2}";

    private final String emailAllDay;
    private final String emailDateFrom;
    private final String emailDateTo;
    private final String emailNext2;
    private final String emailNext3;

    public ReservationService() {
        this(Prop.getInstance());
    }

    public ReservationService(Prop prop) {
        emailAllDay = prop.getText("reservation.reservations.email_for_all_day");
        emailDateFrom = prop.getText("reservation.reservations.email_date_from");
        emailDateTo = prop.getText("reservation.reservations.email_date_to");
        emailNext2 = prop.getText("components.reservation.mail.next2");
        emailNext3 = prop.getText("components.reservation.mail.next3");
    }

    /**
     * !! Beware, reservation time must be already set into reservation date (date and time must be combined).
     * Check this validation requirements :
     * 1. Reservation time from is < reservation time to (interval must have 1 minute at least)
     * 2. Reservation time interval inutes) set in reservation object
     * 3. If reservation range reservationTimeFrom-reservationTimeTo fits inside of reservationObject range reservationTimeFrom-reservationTimeTo
     *    (check it separately for every day in date range dateFrom-DateTo because reservation object range can be different for every day in week)
     * @param reservation reservation to check
     * @param reservationObject reservation object that we trying reserve
     * @return If any of this validations are violated, text key with belonging error message is returned.
     *         Otherwise return null;
     */
    public String checkReservationTimeRangeValidity(ReservationEntity reservation, ReservationObjectEntity reservationObject) {

        //Compute day range (how many days we want reservate)
        long diffD = reservation.getDateTo().getTime() - reservation.getDateFrom().getTime();
        long reservationDaysRange = TimeUnit.DAYS.convert(diffD, TimeUnit.MILLISECONDS) + 1;

        //In case that date range is >= than 7, we must check every dayOfWeek
        boolean checkEveryDayOfWeek = false;
        if(reservationDaysRange >= 7) checkEveryDayOfWeek = true;

        //Get days of week during our reservation
        List<Integer> daysOfWeek = new ArrayList<>();
        if(checkEveryDayOfWeek) {
            daysOfWeek.add(1);
            daysOfWeek.add(2);
            daysOfWeek.add(3);
            daysOfWeek.add(4);
            daysOfWeek.add(5);
            daysOfWeek.add(6);
            daysOfWeek.add(7);
        } else { //Compute day o week
            for(int i = 0; i < reservationDaysRange; i++) {
                //We need day of week (but stupid Calendar start from Sunday = 1, Monday = 2 ... Saturday = 7 )
                Calendar cld = Calendar.getInstance();
                cld.setTime(reservation.getDateFrom());
                //We adding days because we need loop date range and get dayOfWeek for every of them
                cld.add(Calendar.DAY_OF_MONTH, i);
                int dayOfweek = cld.get(Calendar.DAY_OF_WEEK);
                //Format day of week to our standart where Monday = 1, Tuesday = 2 ... Sunday = 7
                dayOfweek = (dayOfweek - 1) == 0 ? 7 : (dayOfweek - 1);
                daysOfWeek.add(dayOfweek);
            }
        }

        //Validate reservation time values (if we want compare times, date values must have set yyyy-mm-dd at same value "2000-01-01")
        Date reservationTimeFrom = DefaultTimeValueConverter.getValidTimeValue(reservation.getDateFrom());
        Date reservationTimeTo = DefaultTimeValueConverter.getValidTimeValue(reservation.getDateTo());

        //Validate time range if have correct values from-to
        if(reservationTimeFrom.getTime() >= reservationTimeTo.getTime()) return "reservation.reservations.time_range_in_bad_order.js";

        //Validate if time range is big enough (because reservation object has set a minimum time range set)
        //Only if reservation object cant be reserve only for whole day
        if(Tools.isFalse(reservationObject.getReservationForAllDay())) {
            long diffT = reservationTimeTo.getTime() - reservationTimeFrom.getTime();
            long reservationTimesRange = TimeUnit.MILLISECONDS.toMinutes(diffT);
            if(reservationTimesRange < reservationObject.getTimeUnit()) return "reservation.reservations.time_range_to_short.js";
        }

        for(Integer dayOfWeek : daysOfWeek) {
            //First set default reservation object times range
            Date objectTimeFrom = reservationObject.getReservationTimeFrom();
            Date objectTimeTo = reservationObject.getReservationTimeTo();

            //Loop reservationObjectTimes and check if our dayOfWeek have special reservation time
            for(ReservationObjectTimesEntity objectTime : reservationObject.getReservationObjectTimeEntities()) {
                if(objectTime.getDay().equals(dayOfWeek)) {
                    objectTimeFrom = objectTime.getTimeFrom();
                    objectTimeTo = objectTime.getTimeTo();
                    break;
                }
            }

            //Now we have reservation object time range for specific day of week
            //We must check if "reservation" time range is inside "reservation object" time range
            if((objectTimeFrom.getTime() <= reservationTimeFrom.getTime())
            && (objectTimeTo.getTime() >= reservationTimeTo.getTime())) {
                //"reservation" time range IS inside "reservation object" time range
            } else {
                //"reservation" time range IS NOT inside "reservation object" time range
                return "reservation.reservations.time_range_validity_error.js";
            }
        }

        //No problem found so return null
        return null;
    }

    /**
     * !! Beware, reservation time must be already set into reservation date (date and time must be combined).
     * Check this validation requirements :
     * 1. Reservation date from is <= than reservation date to (if from = to reservation is for 1 day)
     * 2. Reservation date from/to are'nt in past
     * 3. Reservation overlapping validity, reservation can overlap with other reservations BUT max number of overlaps is set
     *    by reservation object (we count ONLY already approved reservations)
     * @param reservation reservation to check
     * @param reservationObject reservation object that we trying reserve
     * @param rr reservation repository (needed in proces of validation)
     * @return If any of this validations are violated, text key with belonging error message is returned.
     *         Otherwise return null;
     */
    public String checkReservationOverlappingValidity(ReservationEntity reservation, ReservationObjectEntity reservationObject, ReservationRepository rr, boolean isImporting) {
        //validate date range
        int result = DateTools.validateRange(reservation.getDateFrom(), reservation.getDateTo(), false, Tools.isFalse(reservationObject.getReservationForAllDay()));
        if(result == 1 && (isImporting || Tools.isTrue(reservation.getEditorFields().getAllowHistorySave()))) {
            //Range is in past BUT we are importing - so it's allowed
        } else {
            switch (result){
                case -1: return "datatable.error.fieldErrorMessage";
                case 1 :return "reservation.reservations.range_in_past.js";
                case 2: return "reservation.reservations.date_range_in_bad_order.js";
                default:
                    break;
            }
        }

        //Potentially overlapping reservations
        // !! We are finding only ACCEPTED reservations
        //We dont count reservations if they are rejected or still waiting for acceptance
        List<ReservationEntity> potentiallyOverlappingReservations =
            rr.findAllByReservationObjectIdAndDomainIdAndDateFromLessThanEqualAndDateToGreaterThanEqualAndAcceptedTrue(reservationObject.getId(), CloudToolsForCore.getDomainId(), reservation.getDateTo(), reservation.getDateFrom());

        //When we do EDIT on reservation, we must remove this reservation from list
        Long reservationId = reservation.getId();
        if(reservationId != null && reservationId > 0 && potentiallyOverlappingReservations != null)
            potentiallyOverlappingReservations.removeIf(item -> item.getId().equals(reservationId));

        //Now check if even time interval overlaps
        List<ReservationEntity> overlappingReservations = new ArrayList<>();

        if(potentiallyOverlappingReservations != null && potentiallyOverlappingReservations.isEmpty() == false) {
            for(ReservationEntity overlapReservation : potentiallyOverlappingReservations) {
                if(checkOverlap(reservation.getDateFrom(), reservation.getDateTo(), overlapReservation.getDateFrom(), overlapReservation.getDateTo(), Tools.isFalse(reservationObject.getReservationForAllDay())))
                    overlappingReservations.add(overlapReservation);
            }
        }

        /*
         * Now we know that all reservations in List "overlapReservation" are overlapping with our new reservation, but
         * we still must figure out if they are overlapping each other. Reason is we need compute max number of overlaps
         * in same time. Reason is because every "reservationObject" has it own set max number of reservations in same time.
        */
        int maxOverlapCount = 0;
        for(int i = 0; i < overlappingReservations.size(); i++) {
            int overlapCount = 0;
            ReservationEntity re1 = overlappingReservations.get(i);

            for(int j = 0; j < overlappingReservations.size(); j++) {
                //Do not compare with same entity
                if(i == j) continue;

                ReservationEntity re2 = overlappingReservations.get(j);

                if(checkOverlap(re1.getDateFrom(), re1.getDateTo(), re2.getDateFrom(), re2.getDateTo(), Tools.isFalse(reservationObject.getReservationForAllDay())))
                    overlapCount++;
            }

            //Save max overlap count
            maxOverlapCount = maxOverlapCount > overlapCount ? maxOverlapCount : overlapCount;
        }

        //+1 because 1 overlap means that there are 2 reservation in same time (2 overlaps means 3 reservation in same time etc)
        if(overlappingReservations.isEmpty() == false) maxOverlapCount++;

        //+1 because we want add new reservation (+1 our new reservation)
        //Validate if still can add our reservation (due to number limitation)
        if((maxOverlapCount + 1) > reservationObject.getMaxReservations()) {
            //We have too many reservations in same time
            if(isImporting || Tools.isTrue(reservation.getEditorFields().getAllowOverbooking())) {
                //IF we are importing OR user is ok with this, we can add reservation
            } else {
                //Not allowed
                return "reservation.reservations.max_reservations_error.js";
            }
        }

        //No problem found so return null
        return null;
    }

    /**
     * Prepare reservation to validation. First check if needed values are not null. Then set reservation date based on input value "isReservationForAllDay".
     * If isReservationForAllDay is true, date is same but hh:mm:ss:ms are set to 0 (because reservation upon reservation object for whole day cant set other time then default).
     * If isReservationForAllDay is false, date is set as combination of date from reservation and time from reservationEditorFields.
     * @param reservation
     * @param isReservationForAllDay
     */
    public void prepareReservationToValidation(ReservationEntity reservation, boolean isReservationForAllDay) throws IllegalArgumentException {
        //Check of values
        if(reservation.getDateFrom() == null || reservation.getDateTo() == null || reservation.getEditorFields() == null)
            throwError("html_area.insert_image.error_occured");

        if(isReservationForAllDay == false && (reservation.getEditorFields().getReservationTimeFrom() == null || reservation.getEditorFields().getReservationTimeTo() == null))
            throwError("html_area.insert_image.error_occured");

        if(isReservationForAllDay == true && (reservation.getEditorFields().getArrivingTime() == null || reservation.getEditorFields().getDepartureTime() == null))
            throwError("html_area.insert_image.error_occured");

        //Prepare dates
        ReservationService.prepareDates(reservation, isReservationForAllDay);
    }

    /**
     * Get reservation object "email accepter" and send mail to notify accepter about new waiting reservation for this reservation object.
     * Email includes link to this reservation waiting for approve.
     * @param reservation
     * @param reservationObject
     */
    public void sendAcceptationEmail(ReservationEntity reservation, HttpServletRequest request) {
        ReservationObjectEntity reservationObject = reservation.getReservationObjectForReservation();
        if(reservation == null || reservationObject == null) return;
        //Validate recipient email
        String recipientEmail = reservationObject.getEmailAccepter();
        try {
            InternetAddress emailAddr = new InternetAddress(recipientEmail);
            emailAddr.validate();
        } catch (AddressException ex) {
            return;
        }

        Prop prop = Prop.getInstance(request);
		String senderName = notNull(reservation.getName()) + " " + notNull(reservation.getSurname());
		String senderEmail = notNull(reservation.getEmail());
		String reservationObjectName = notNull(reservationObject.getName());
		String dateFrom = Tools.formatDate(reservation.getDateFrom());
		String dateTo = Tools.formatDate(reservation.getDateTo());
        String timeFrom = Tools.formatTime(reservation.getDateFrom());
		String timeTo = Tools.formatTime(reservation.getDateTo());
		String subject = prop.getText("components.reservation.mail.title") + senderName + " - " + reservationObjectName;
		String phoneNumber = notNull(reservation.getPhoneNumber());

        StringBuilder message = new StringBuilder();
        message.append(prop.getText("components.reservation.mail.greeting")).append("<br /><br />").append(senderName);
        message.append(" (").append(prop.getText("user.phone")).append(senderName).append(" )").append(phoneNumber).append(" ");
        message.append(prop.getText("components.reservation.mail.next")).append(" <b>").append(reservationObjectName).append("</b> ");

        if(Tools.isTrue(reservationObject.getReservationForAllDay())) {
            message.append(emailAllDay).append(" ");
        }

        message.append(emailDateFrom).append(" ").append(dateFrom).append(" ");
        message.append(emailDateTo).append(" ").append(dateTo).append(" ");

        if(Tools.isFalse(reservationObject.getReservationForAllDay())) {
            message.append(emailNext2).append(" ").append(timeFrom).append(" ");
            message.append(emailNext3).append(" ").append(timeTo).append(" ");
        }

        message.append(prop.getText("components.reservation.mail.next4")).append("<br/> ").append(reservation.getPurpose()).append(" <br /><br />");
        message.append(prop.getText("components.reservation.mail.next5")).append(" <a href=\"").append(Tools.getBaseHref(request)).append("/apps/reservation/admin/#dt-filter-id=").append(reservation.getId()).append("\">");
        message.append(prop.getText("components.reservation.mail.accept")).append("</a>");

		SendMail.send(senderName, senderEmail, recipientEmail, null, null, subject, message.toString(), null);
    }

    /**
     * Send confirmation email to email address set in reservation. Email subject and text is based on reservation "accepted" value where :
     * (true, reservation was accepted),
     * (false, reservation was rejected),
     * (null, reservation status was reset and reservation is waiting for approve)
     * @param reservation approved reservation
     * @param reservationObject reservation object that reservation is trying reserve
     * @param request HttpServletRequest instance
     * @param loggedUserName full name of actually logged user who change reservation accepted status
     */
    public void sendConfirmationEmail(ReservationEntity reservation, HttpServletRequest request, Identity loggedUser) {
        if(reservation == null) return;
        ReservationObjectEntity reservationObject = reservation.getReservationObjectForReservation();
        if(reservationObject == null) return;
        //Validate recipient email
        String recipientEmail = reservation.getEmail();
        try {
            InternetAddress emailAddr = new InternetAddress(recipientEmail);
            emailAddr.validate();
        } catch (AddressException ex) {
            return;
        }

        Prop prop = Prop.getInstance(request);
        String recipientName = notNull(reservation.getName() + " " + reservation.getSurname());
		String senderName = SendMail.getDefaultSenderName("reservation", loggedUser.getFullName());
		String senderEmail = SendMail.getDefaultSenderEmail("reservation", loggedUser.getEmail());
		String subject = "";
        String status = "";
        String endOfEmail = ".";
        Boolean approved = reservation.getAccepted();

        if(approved == null) {
            subject = prop.getText("reservation.reservations.email_subject_reset");
            status = prop.getText("reservation.reservations.email_was_reset");
            endOfEmail = prop.getText("reservation.reservations.reset_end_of_email");
        } else if(Tools.isTrue(approved)) {
            subject = prop.getText("reservation.reservations.email_subject_accepted");
            status = prop.getText("reservation.reservations.email_was_accepted");
        } else if(Tools.isFalse(approved)) {
            subject =  prop.getText("reservation.reservations.email_subject_rejected");
            status = prop.getText("reservation.reservations.email_was_rejected");
        }

        StringBuilder message = new StringBuilder();
        message.append(prop.getText("reservation.reservations.email_greeting")).append(" ").append(recipientName).append(",<br><br>");
        message.append(getReservationSpecForEmail(reservation, reservationObject, prop.getText("reservation.reservations.email_your_reservation")));
        message.append("<b>").append(status).append("<b/> : ").append(loggedUser.getFullName()).append(" ").append(endOfEmail);

		SendMail.send(senderName, senderEmail, recipientEmail, null, null, subject, message.toString(), null);

        if(Tools.isTrue(approved)) {
            //Send notification emails
            sendNotifEmails(reservationObject.getNotifEmails(), senderName, senderEmail, getReservationSpecForEmail(reservation, reservationObject, prop.getText("components.reservation.reservation_object.notify_body")), prop);
        }
	}

    /**
     * Send email to recipient (who created reservation) about created reservation. Email includes reservation details + info if reservation is approved or awaiting for acceptation.
     *
     * @param reservation - created reservation
     * @param request - HttpServletRequest instance
     */
    public void sendCreatedReservationEmail(ReservationEntity reservation, HttpServletRequest request) {
        if(reservation == null) return;
        ReservationObjectEntity reservationObject = reservation.getReservationObjectForReservation();
        if(reservationObject == null) return;

        //Validate recipient email
        String recipientEmail = reservation.getEmail();
        try {
            InternetAddress emailAddr = new InternetAddress(recipientEmail);
            emailAddr.validate();
        } catch (AddressException ex) {
            return;
        }

        Prop prop = Prop.getInstance(request);
        String recipientName = notNull(reservation.getName() + " " + reservation.getSurname());
		String senderName = SendMail.getDefaultSenderName("reservation", notNull(reservation.getName() + reservation.getSurname()));
		String senderEmail = SendMail.getDefaultSenderEmail("reservation", null);

        StringBuilder message = new StringBuilder();
        message.append(prop.getText("reservation.reservations.email_greeting")).append(" ").append(recipientName).append(",<br><br>");
        message.append(getReservationSpecForEmail(reservation, reservationObject, prop.getText("reservation.reservations.email_your_reservation")));

        if(Tools.isTrue(reservationObject.getMustAccepted()) && reservation.getAccepted() == null) {
            message.append(prop.getText("components.reservation.was_created_and_waiting"));
        } else {
            message.append(prop.getText("components.reservation.was_created"));

            //Send notification emails
            sendNotifEmails(reservationObject.getNotifEmails(), senderName, senderEmail, getReservationSpecForEmail(reservation, reservationObject, prop.getText("components.reservation.reservation_object.notify_body")), prop);
        }

        SendMail.send(senderName, senderEmail, recipientEmail, null, null, prop.getText("components.reservation.saved_reservation_subject"), message.toString(), null);
    }

    private void sendNotifEmails(String notifEmails, String senderName, String senderEmail, String specs, Prop prop) {
        if(Tools.isEmpty(notifEmails)) return;

        String[] emails = Tools.getTokens(notifEmails.trim(), ",");
        for(String recipientEmail : emails) {
            if(Tools.isEmail(recipientEmail))  {
                StringBuilder message = new StringBuilder();
                message.append(prop.getText("reservation.reservations.email_greeting")).append(",<br><br>");
                message.append(specs).append(".");

                SendMail.send(senderName, senderEmail, recipientEmail, null, null, prop.getText("components.reservation.saved_reservation_subject"), message.toString(), null);
            }
        }
    }

    private String getReservationSpecForEmail(ReservationEntity reservation, ReservationObjectEntity reservationObject, String startMsg) {
        String reservationObjectName = notNull(reservationObject.getName());
        String dateFrom = Tools.formatDate(reservation.getDateFrom());
        String dateTo = Tools.formatDate(reservation.getDateTo());
        String timeFrom = Tools.formatTime(reservation.getDateFrom());
        String timeTo = Tools.formatTime(reservation.getDateTo());

        StringBuilder specs = new StringBuilder();
        specs.append(startMsg).append(" <b>").append(reservationObjectName).append("</b> ");

        if(Tools.isTrue(reservationObject.getReservationForAllDay()))
            specs.append(emailAllDay).append(" ");

        specs.append(emailDateFrom).append(" ").append(dateFrom).append(" ");
        specs.append(emailDateTo).append(" ").append(dateTo).append(" ");

        if(Tools.isFalse(reservationObject.getReservationForAllDay())) {
            specs.append(emailNext2).append(" ").append(timeFrom).append(" ");
            specs.append(emailNext3).append(" ").append(timeTo).append(" ");
        }

        return specs.toString();
    }

    //Now validate if date range s1-e1 and range s2-e2 overlaps
    //Formula ((s1 <= e2) && (s2 <= e1)) , if true, then two dates are overlapping

    /**
     * Validate if interval s1-e1 and interval s2-e2 are overlapping using logical formula.
     * Used formula ((s1 <= e2) && (s2 <= e1)), return true if they are overlapping.
     * Intervals are overlapping even if one start when second ends (08:00-09:00 and 09:00-10:00).
     * IF function is used to compare date values (intervals) representing TIME we want all the values to share same yyyy-mm-dd part and for this reason
     * with input param "prepareDates" set to true, every date part of value will be set to 2000-01-01, so we can compare times.
     * @param s1 date value representing START of FIRST interval
     * @param e1 date value representing END of FIRST interval
     * @param s2 date value representing START of SECOND interval
     * @param e2 date value representing END of SECOND interval
     * @param prepareDates if true set date part of intervals to 2000-01-01, false/null - do nothing
     * @return true - if interval are overlapping, otherwise false
     */
    public boolean checkOverlap(Date s1, Date e1, Date s2, Date e2, Boolean prepareDates) {
        if(Tools.isTrue(prepareDates)) {
            s1 = DefaultTimeValueConverter.getValidTimeValue(s1);
            e1 = DefaultTimeValueConverter.getValidTimeValue(e1);
            s2 = DefaultTimeValueConverter.getValidTimeValue(s2);
            e2 = DefaultTimeValueConverter.getValidTimeValue(e2);
        }

        return ((s1.getTime() <= e2.getTime()) && (s2.getTime() <= e1.getTime()));
    }

    /**
     * Check if input string is null and if yes return empty string. Otherwise return original string.
     * @param s input string to check
     * @return not null string
     */
    private String notNull(String s) {
		if (s == null) return "";
		return s;
	}

    /**
     * Custom function to throw new RuntimeException with message.
     * @param errorTextKey translate key of error message
     */
    public void throwError(String errorTextKey) {
        Prop prop = Prop.getInstance();
        String message = prop.getText(errorTextKey);
        throw new IllegalArgumentException(message);
    }

    /**
     * Calculate price of reservation based on reservation object, reservation dateTime range and reservation object special prices.
     * @param entity - reservation entity
     * @param ror - ReservationObjectRepository instance
     * @param ropr - ReservationObjectPriceRepository instance
     * @return
     */
    public static BigDecimal calculateReservationPrice(ReservationEntity entity, int userIdToPay, ReservationObjectRepository ror, ReservationObjectPriceRepository ropr) {
        if(entity == null) return new BigDecimal(-1);
        if(entity.getEditorFields() == null) return new BigDecimal(-1);
        return calculateReservationPrice(entity.getDateFrom(), entity.getDateTo(), entity.getEditorFields().getReservationTimeFrom(), entity.getEditorFields().getReservationTimeTo(), entity.getReservationObjectId(), userIdToPay, ror, ropr);
    }

    /**
     * Calculate price of reservation based on reservation object, reservation dateTime range and reservation object special prices.
     * @param dateFrom - start date of reservation
     * @param dateTo - end date of reservation
     * @param timeFrom - start time of reservation
     * @param timeTo    - end time of reservation
     * @param objectId - id of reservation object
     * @param ror - ReservationObjectRepository instance
     * @param ropr - ReservationObjectPriceRepository instance
     * @return
     */
    public static BigDecimal calculateReservationPrice (Date dateFrom, Date dateTo, Date timeFrom, Date timeTo, Long objectId, int userIdToPay, ReservationObjectRepository ror, ReservationObjectPriceRepository ropr) {
        Long dateFromL = dateFrom == null ? null : dateFrom.getTime();
        Long dateToL = dateTo == null ? null : dateTo.getTime();
        Long timeFromL = timeFrom == null ? null : timeFrom.getTime();
        Long timeToL = timeTo == null ? null : timeTo.getTime();
        return calculateReservationPrice(dateFromL, dateToL, timeFromL, timeToL, objectId, userIdToPay, ror, ropr);
    }

    /**
     * Calculate price of reservation based on reservation object, reservation dateTime range and reservation object special prices.
     * @param dateFrom - start date of reservation
     * @param dateTo - end date of reservation
     * @param timeFrom - start time of reservation
     * @param timeTo    - end time of reservation
     * @param objectId - id of reservation object
     * @param ror - ReservationObjectRepository instance
     * @param ropr - ReservationObjectPriceRepository instance
     * @return
     */
    public static BigDecimal calculateReservationPrice (Long dateFrom, Long dateTo, Long timeFrom, Long timeTo, Long objectId, int userIdToPay, ReservationObjectRepository ror, ReservationObjectPriceRepository ropr) {
        Map<String, BigDecimal> prices = getMapOfPrices(dateFrom, dateTo, timeFrom, timeTo, objectId, userIdToPay, ror, ropr);
        if(prices == null) return new BigDecimal(-1);
        //
        return prices.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static Map<String, BigDecimal> getMapOfPrices(Long dateFrom, Long dateTo, Long timeFrom, Long timeTo, Long objectId, int userIdToPay, ReservationObjectRepository ror, ReservationObjectPriceRepository ropr) {
        if(dateFrom == null || dateTo == null || objectId == null) return null;
        ReservationObjectEntity reservationObject = ror.findFirstByIdAndDomainId(objectId, CloudToolsForCore.getDomainId()).orElse(null);
        if(Tools.isFalse(reservationObject.getReservationForAllDay()) && (timeFrom == null || timeTo == null)) return null;

        List<ReservationObjectPriceEntity> prices = ropr.findAllByObjectIdAndDomainIdAndDateFromLessThanEqualAndDateToGreaterThanEqual(objectId, CloudToolsForCore.getDomainId(), new Date(dateTo), new Date(dateFrom));

        dateTo = DateTools.setTimePart(new Date(dateTo), 23, 59, 0, 0).getTime();

        Map<String, BigDecimal> mapOfPrices = new HashMap<>();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateFrom);
        cal.set(Calendar.HOUR_OF_DAY, 12); //Set it to middle of day
        while(cal.getTimeInMillis() < dateTo) {
            BigDecimal specialPrice = null;
            for(ReservationObjectPriceEntity priceEntity : prices) {
                if(priceEntity.getDateFrom().before(cal.getTime()) && priceEntity.getDateTo().after(cal.getTime())) {
                    specialPrice = priceEntity.getPrice();
                    break;
                }
            }

            String dayId = getDateId(cal, Tools.isFalse(reservationObject.getReservationForAllDay()));

            //add day - move in range
            cal.add(Calendar.DAY_OF_MONTH, 1);

            if(Tools.isTrue(reservationObject.getReservationForAllDay())) {
                //This condition will skip 1 last day, this day user do not pay because he is leaving
                if(cal.getTimeInMillis() < dateTo) {
                    if(specialPrice != null) mapOfPrices.put(dayId, specialPrice);
                    else mapOfPrices.put(dayId, reservationObject.getPriceForDay());
                }
            } else {
                //Unit of time which is charged - IN MINUTES
                BigDecimal timeUnit = new BigDecimal( reservationObject.getTimeUnit() );
                BigDecimal defaultPriceForTimeUnit = reservationObject.getPriceForHour();
                BigDecimal reservedTimeEveryDay = new BigDecimal( (timeTo - timeFrom + 1000) / (60 * 1000) );
                if(specialPrice != null) {
                    mapOfPrices.put(dayId, (reservedTimeEveryDay.divide(timeUnit, 2, RoundingMode.HALF_EVEN)).multiply(specialPrice) );
                } else {
                    mapOfPrices.put(dayId, (reservedTimeEveryDay.divide(timeUnit, 2, RoundingMode.HALF_EVEN)).multiply(defaultPriceForTimeUnit) );
                }
            }
        }

        //There can be DISCOUNT for certain user groups
        UserGroupsDB ugdb = UserGroupsDB.getInstance();
        return ugdb.calculatePrices(mapOfPrices, UsersDB.getUser(userIdToPay));
    }

    /**
     * If reservation object needs ACCEPTATION and logged user is not accepter (or none is logged), set reservation accepted to null (waiting for acceptation) AND return false.
     *
     * ELSE set reservation accepted to true and return true.
     *
     * @param reservation
     * @param request
     * @return
     */
    public static boolean acceptation(ReservationEntity reservation, HttpServletRequest request) {
        reservation.setAccepted(Boolean.TRUE);
        ReservationObjectEntity reservationObject = reservation.getReservationObjectForReservation();
        if(Tools.isTrue(reservationObject.getMustAccepted()) && reservationObject.getEmailAccepter() != null) {
            Identity loggedUser = UsersDB.getCurrentUser(request);
            if(loggedUser == null || loggedUser.getEmail().equals(reservationObject.getEmailAccepter()) == false) {
                //Set to null, it means waiting for acceptation
                reservation.setAccepted(null);
                return false;
            }
        }
        return true;
    }

    /************* MVC METHODS ***************************/

    /**
     * Get list of reservation objects for select (as options)/
     * This reservation objects are filtered by domainId and reservationForAllDay = false.
     * @return
     */
    public static List<LabelValueInteger> getReservationObjectHoursSelectList() {
        ReservationObjectRepository reservationObjectRepository = Tools.getSpringBean("reservationObjectRepository", ReservationObjectRepository.class);
        List<ReservationObjectEntity> reservationObjects = reservationObjectRepository.findAllByDomainIdAndReservationForAllDayFalse(CloudToolsForCore.getDomainId());
        return prepareSelectList(reservationObjects);
    }

    public static List<LabelValueInteger> getReservationObjectDaysSelectList() {
        ReservationObjectRepository reservationObjectRepository = Tools.getSpringBean("reservationObjectRepository", ReservationObjectRepository.class);
        List<ReservationObjectEntity> reservationObjects = reservationObjectRepository.findAllByDomainIdAndReservationForAllDayTrue(CloudToolsForCore.getDomainId());
        return prepareSelectList(reservationObjects);
    }

    private static List<LabelValueInteger> prepareSelectList(List<ReservationObjectEntity> reservationObjects) {
        return prepareSelectList(reservationObjects, false, null);
    }

    private static List<LabelValueInteger> prepareSelectList(List<ReservationObjectEntity> reservationObjects, boolean addDefaultOption, String defaultLabelKey) {
        List<LabelValueInteger> reservationOptions = new ArrayList<>();

        if(addDefaultOption) {
            Prop prop = Prop.getInstance();
            String defaultLabel = "DEFAULT";
            if(Tools.isNotEmpty(defaultLabelKey)) defaultLabel = prop.getText(defaultLabelKey);
            reservationOptions.add(new LabelValueInteger(defaultLabel, -1));
        }

        for(ReservationObjectEntity ro : reservationObjects) {
            reservationOptions.add(new LabelValueInteger(ro.getName(), ro.getId().intValue()));
        }

        return reservationOptions;
    }

    /**
     * If this day have special time range (ReservationObjectTimesEntity) return it, otherwise return default time range (reservationObject).
     * On top of that, remove Minutes/Seconds/MiliSeconds from date values.
      *
      * @param dateToCheck - date to check (if there is special time range for this day of week)
      * @param reservationObject
      * @return
      */
    public static Long[] getReservationTimeRange(Date dateToCheck, ReservationObjectEntity reservationObject) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateToCheck);
        int dayOfweek = calendar.get(Calendar.DAY_OF_WEEK);
        //Format day of week to our standart where Monday = 1, Tuesday = 2 ... Sunday = 7
        dayOfweek = (dayOfweek - 1) == 0 ? 7 : (dayOfweek - 1);

        ReservationObjectTimesEntity rote = null;
        for(ReservationObjectTimesEntity entity : reservationObject.getReservationObjectTimeEntities()) {
            if(entity.getDay() == dayOfweek) rote = entity;
        }

        if(rote != null)
            return new Long[] {
                prepareDateHourReservation(dateToCheck, rote.getTimeFrom()),
                prepareDateHourReservation(dateToCheck, rote.getTimeTo())
            };
        else
            return new Long[] {
                prepareDateHourReservation(dateToCheck, reservationObject.getReservationTimeFrom()),
                prepareDateHourReservation(dateToCheck, reservationObject.getReservationTimeTo())
            };
    }

    /**
     * Prepare reservation fo date-hour reservation style.
     * @param date
     * @param time
     * @return
     */
    private static Long prepareDateHourReservation(Date date, Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        Calendar calTime = Calendar.getInstance();
        calTime.setTime(time);

        cal.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Get list of hours for table (reservation table) based on from-to date values.
     * @param from
     * @param to
     * @return
     */
    public static List<String> getHoursForTable(Long from, Long to) {
        List<String> hours = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(from);
        for(int i = 0; i < ((to - from) / (60 * 60 * 1000)); i++) {
            hours.add( Tools.formatTime(cal.getTimeInMillis()) );
            cal.add(Calendar.HOUR_OF_DAY, 1);
        }
        return hours;
    }

    /**
     * Used as object that is sent to FE for reservation table.
     */
    public static class ReservationTableCell {
        private String id;
        private String value;
        private String cssClass;

        public ReservationTableCell(String id, String value, String cssClass) {
            this.id = id;
            this.value = value;
            this.cssClass = cssClass;
        }

        public String getId() { return id; }
        public String getValue() { return value; }
        public String getCssClass() { return cssClass; }
    }

    /**
     * Prepare list of ReservationTableCell objects for reservation table.
     * @param roe - reservation object entity
     * @param rr - ReservationRepository
     * @param from
     * @param to
     * @param supportedRange - range in which reservation can be made
     * @return
     */
    public static List<ReservationTableCell> computeReservationUsageByHours(ReservationObjectEntity roe, ReservationRepository rr, Long from, Long to, Long[] supportedRange) {
        List<ReservationTableCell> tableCellsList = new ArrayList<>();
        int hoursDiff = (int)((to - from) / (60 * 60 * 1000));

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(from);
        cal.set(Calendar.MINUTE, 30); //middle of hour

        int isThere = 0;
        List<ReservationEntity> reservations = rr.findAllByReservationObjectIdAndDomainIdAndDateFromLessThanEqualAndDateToGreaterThanEqualAndAcceptedTrue(roe.getId(), CloudToolsForCore.getDomainId(), new Date(to), new Date(from));
        for(int i = 0; i < hoursDiff; i++) {
            //Check if can make reservation in this time
            if(cal.getTimeInMillis() < supportedRange[0] || cal.getTimeInMillis() > supportedRange[1])  {
                tableCellsList.add(
                    new ReservationTableCell(roe.getId() + "_" + cal.get(Calendar.HOUR_OF_DAY), "", "unsupported")
                );
                cal.add(Calendar.HOUR_OF_DAY, 1);
                continue;
            }

            for(ReservationEntity re : reservations) {
                if(re.getDateFrom().getTime() <= cal.getTimeInMillis() && re.getDateTo().getTime() >= cal.getTimeInMillis()) {
                    isThere++;
                }
            }

            boolean isActive = true;
            if( (cal.getTimeInMillis() - 30*60*1000) < System.currentTimeMillis() ) isActive = false; //Minus 30 minutes, so it's start of hour
            if(isThere >= roe.getMaxReservations()) isActive = false;
            tableCellsList.add(
                new ReservationTableCell(roe.getId() + "_" + cal.get(Calendar.HOUR_OF_DAY), isThere + "/" + roe.getMaxReservations(), (isActive == false ? "full" : "free"))
            );

            isThere = 0;
            cal.add(Calendar.HOUR_OF_DAY, 1);
        }

        return tableCellsList;
    }

    /**
     * Get user id of user who should pay for reservation.
     * @param email
     * @param reservationId
     * @param rr
     * @param request
     * @return
     */
    public static int getUserToPay(String email, Long reservationId, ReservationRepository rr, HttpServletRequest request) {
        if (reservationId != null && reservationId.longValue() > 0L) {
            //Check if reservation exist -> if yes return userId of reservation
            ReservationEntity reservation = rr.findFirstByIdAndDomainId(reservationId, CloudToolsForCore.getDomainId()).orElse(null);
            if(reservation != null) return reservation.getUserId();
        }

        //Get user id -> of logged user
        int userId = Tools.getUserId(request);
        if (userId > 0) return userId;

        if(Tools.isEmail(email)) {
            //If user is NOT logged, try to find user by email
            UserDetails userToPay = UsersDB.getUserByEmail(email);
            if (userToPay != null) return userToPay.getUserId();
        }

        return -1;
    }

    /**
     * Get reservation date from string. If string is empty or do not match regex, return current date.
     * @param reservationDateString
     * @param formatString
     * @return
     */
    public static Date getReservationDate(String reservationDateString, String formatString) {
        String regex = "";
        if(FE_MONTHPICKER_FORMAT.equals(formatString)) {
            regex = REGEX_YYYY_MM;
        } else if(FE_DATEPICKER_FORMAT.equals(formatString)) {
            regex = REGEX_YYYY_MM_DD;
        }

        Date reservationDate;
        if(Tools.isEmpty(reservationDateString)) {
            reservationDate = new Date();
        } else {
            if(reservationDateString.matches(regex)) {
                reservationDate = Tools.getDateFromString(reservationDateString, formatString);
            } else {
                reservationDate = new Date();
            }
        }
        return reservationDate;
    }

    /**
     * Get dateId from date object. If addHourToId is true, add hour to dateId.
     * @param date
     * @param addHourToId
     * @return yyyy-MM-dd or yyyy-MM-ddTHH
     */
    public static String getDateId(Date date, boolean addHourToId) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return getDateId(cal, addHourToId);
    }

    /**
     * Get dateId from date object. If addHourToId is true, add hour to dateId.
     * @param cal
     * @param addHourToId
     * @return yyyy-MM-dd or yyyy-MM-ddTHH
     */
    public static String getDateId(Calendar cal, boolean addHourToId) {
        String dateId = cal.get(Calendar.YEAR) + "-";

        dateId += ( cal.get(Calendar.MONTH) + 1) < 10 ? "0" + (cal.get(Calendar.MONTH) + 1) : (cal.get(Calendar.MONTH) + 1);
        dateId += "-";

        dateId += cal.get(Calendar.DAY_OF_MONTH) < 10 ? "0" + cal.get(Calendar.DAY_OF_MONTH) : cal.get(Calendar.DAY_OF_MONTH);

        if(addHourToId) {
            dateId += "T";
            dateId += cal.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + cal.get(Calendar.HOUR_OF_DAY) : cal.get(Calendar.HOUR_OF_DAY);
        }

        return dateId;
    }

    /**
     * Compute reservation usage by days aka how many reservations are exist for selected reservation object in selected date range (each day in range).
     * @param selectedReservationId
     * @param startDate
     * @param endDate
     * @param rr
     * @return Map where key is dateId and value is number of reservations in this day.
     */
    public static Map<String, Integer> computeReservationUsageByDays(Long selectedReservationId, Date startDate, Date endDate, ReservationRepository rr) {
        List<ReservationEntity> reservations = rr.findAllByReservationObjectIdAndDomainIdAndDateFromLessThanEqualAndDateToGreaterThanEqualAndAcceptedTrue(selectedReservationId, CloudToolsForCore.getDomainId(), endDate, startDate);

        Map<String, Integer> reservationCountMap = new HashMap<>();

        for(ReservationEntity re : reservations) {
            Date reservationFrom = re.getDateFrom();
            Date reservationTo = re.getDateTo();

            Calendar cal = Calendar.getInstance();
            cal.setTime(reservationFrom);

            while(cal.getTime().before(reservationTo)) {
                String dateId = ReservationService.getDateId(cal.getTime(), false);
                if(reservationCountMap.containsKey(dateId)) {
                    reservationCountMap.put(dateId, reservationCountMap.get(dateId) + 1);
                } else {
                    reservationCountMap.put(dateId, 1);
                }

                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        return reservationCountMap;
    }

    public static void prepareDates(ReservationEntity entity, boolean isReservationForAllDay) {
        if(isReservationForAllDay) {
            //Reservation for whole day
            entity.setDateFrom(DefaultTimeValueConverter.combineDateWithTime(entity.getDateFrom(), entity.getEditorFields().getArrivingTime()));
            entity.setDateTo(DefaultTimeValueConverter.combineDateWithTime(entity.getDateTo(), entity.getEditorFields().getDepartureTime()));
        } else {
            entity.setDateFrom(DefaultTimeValueConverter.combineDateWithTime(entity.getDateFrom(), entity.getEditorFields().getReservationTimeFrom()));
            entity.setDateTo(DefaultTimeValueConverter.combineDateWithTime(entity.getDateTo(), entity.getEditorFields().getReservationTimeTo()));

            //Need to add 1 second, help with time compare
            entity.setDateFrom(new Date(entity.getDateFrom().getTime() + 1000));
        }
    }

    /**
     * Get arrival time for reservation. Arrival time is obtained from constant reservationAllDayStartTime.
     * @param entity
     * @return Date with time set to reservationAllDayStartTime OR null if constant has wrong format.
     */
    public static Date getArrivalTime(ReservationEntity entity) {
        String arrivalTimeStr = Constants.getString("reservationAllDayStartTime");
        if(arrivalTimeStr.matches("\\d{2}:\\d{2}")) {
            int hour = Integer.parseInt(arrivalTimeStr.split(":")[0]);
            int minute = Integer.parseInt(arrivalTimeStr.split(":")[1]);
            return DateTools.setTimePart(entity.getDateFrom(), hour, minute, 0, 0);
        } else {
            Logger.error(ReservationService.class, "Value of constant reservationAllDayStartTime has wrong format. Must be HH:MM");
            return null;
        }
    }

    /**
     * Get departure time for reservation. Departure time is obtained from constant reservationAllDayEndTime.
     * @param entity
     * @return Date with time set to reservationAllDayEndTime OR null if constant has wrong format.
     */
    public static Date getDepartureTime(ReservationEntity entity) {
        String departureTimeStr = Constants.getString("reservationAllDayEndTime");
        if(departureTimeStr.matches("\\d{2}:\\d{2}")) {
            int hour = Integer.parseInt(departureTimeStr.split(":")[0]);
            int minute = Integer.parseInt(departureTimeStr.split(":")[1]);
            return DateTools.setTimePart(entity.getDateTo(), hour, minute, 00, 0);
        } else {
            Logger.error(ReservationService.class, "Value of constant reservationAllDayEndTime has wrong format. Must be HH:MM");
            return null;
        }
    }

    /**
     * Check if reservation was changed - compare it with DB version.
     * @param entityToCheck
     * @param reservationObject
     * @param rr
     * @return
     */
    public static boolean wasReservationChanged(ReservationEntity entityToCheck, ReservationRepository rr) {
        Long id = entityToCheck.getId();
        if(id == null || id < 1) return true;

        ReservationEntity dbVersion = rr.findById(id).orElse(null);
        if(dbVersion == null) return true;

        //now compare values
        if(entityToCheck.getReservationObjectId().equals( dbVersion.getReservationObjectId()) == false) return true;
        if(entityToCheck.getDateFrom().equals( dbVersion.getDateFrom()) == false) return true;
        if(entityToCheck.getDateTo().equals( dbVersion.getDateTo()) == false) return true;

        return false;
    }
}