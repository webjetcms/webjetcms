package sk.iway.iwcm.components.reservation.rest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.reservation.jpa.ReservationEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectTimesEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.jpa.DefaultTimeValueConverter;

@Service
public class ReservationService {

    /**
     * !! Beware, reservation time must be already set into reservation date (date and time must be combined).
     * Check this validation requirements :
     * 1. Reservation time from is < reservation time to (interval must have 1 minute at least)
     * 2. Reservation time interval is >= minimal reservation time (in minutes) set in reservation object
     * 3. If reservation range reservationTimeFrom-reservationTimeTo fits inside of reservationObject range reservationTimeFrom-reservationTimeTo
     *    (check it separetly for every day in date range dateFrom-DateTo because reservation object range can be different for every day in week)
     * @param reservation reservation to check
     * @param reservationObejct reservation object that we trying reservate
     * @param reservationObjectTimes reservation obejct times list
     * @return If any of this validations are violated, text key with belonging error message is returned.
     *         Otherwise return null;
     */
    public String checkReservationTimeRangeValidity(ReservationEntity reservation, ReservationObjectEntity reservationObject,
        List<ReservationObjectTimesEntity> reservationObjectTimes) {

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
        //Only if reservation object cant be reservate only for whole day
        if(!reservationObject.getReservationForAllDay()) {
            long diffT = reservationTimeTo.getTime() - reservationTimeFrom.getTime();
            long reservationTimesRange = TimeUnit.MILLISECONDS.toMinutes(diffT);
            if(reservationTimesRange < reservationObject.getTimeUnit()) return "reservation.reservations.time_range_to_short.js";
        }

        for(Integer dayOfWeek : daysOfWeek) {
            //First set default reservation object times range
            Date objectTimeFrom = reservationObject.getReservationTimeFrom();
            Date objectTimeTo = reservationObject.getReservationTimeTo();

            //Loop reservationObjectTimes and check if our dayOfWeek have special reservation time
            for(ReservationObjectTimesEntity objectTime : reservationObjectTimes) {
                if(objectTime.getDay() == dayOfWeek) {
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
                continue;
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
     * 2. Reservation date from/to arent in past
     * 3. Reservation overlaping validity, reservation can overlap with other reservations BUT max number of overlaps is set
     *    by reservation object (we ccount ONLY already approved reservations)
     * @param reservation reservation to check
     * @param reservationObejct reservation object that we trying reservate
     * @param rr reservation repository (needed in proces of validation)
     * @return If any of this validations are violated, text key with belonging error message is returned.
     *         Otherwise return null;
     */
    public String checkReservationOverlapingValidity(ReservationEntity reservation, ReservationObjectEntity reservationObejct, ReservationRepository rr) {

        Date reservationDateFrom = setTimeOfDate(reservation.getDateFrom(), 0, 0, 0, 0);
        Date reservationDateTo = setTimeOfDate(reservation.getDateTo(), 0, 0, 0, 0);

        //Validate date range
        if(reservationDateTo.before(reservationDateFrom)) return "reservation.reservations.date_range_in_bad_order.js";

        //Reservation date from/to allready includes set time
        //Validate date time range (if part or whole range is in past)
        Date now = new Date();
        if(reservation.getDateFrom().before(now) || reservation.getDateTo().before(now))
            return "reservation.reservations.range_in_past.js";

        //Need set dateTo to edge of date (because we want all reservations, from whole day)
        reservationDateTo = setTimeOfDate(reservation.getDateTo(), 23, 59, 59, 0);

        //Potentially overlaping reservations, days are overlaping BUT time does not have overlap
        // !! We are finding only ACCEPTED reservations
        //We dont count reservations is they are rejected or still waiting for acceptance
        List<ReservationEntity> potentiallyOverlappingReservations =
            rr.findAllByReservationObjectIdAndDomainIdAndDateFromLessThanEqualAndDateToGreaterThanEqualAndAcceptedTrue(reservationObejct.getId().intValue(), CloudToolsForCore.getDomainId(), reservationDateTo, reservationDateFrom);

        //When we do EDIT on reservation, we must remove this reservation from list
        Long reservationId = reservation.getId();
        if(reservationId != null && reservationId > 0 && potentiallyOverlappingReservations != null)
            potentiallyOverlappingReservations.removeIf(item -> item.getId() == reservationId);

        //Now check if even time interval overlaps
        List<ReservationEntity> overlappingReservations = new ArrayList<>();

        if(potentiallyOverlappingReservations != null && potentiallyOverlappingReservations.size() > 0) {
            //Get reservation time in correct format (if we want compare times, date values must have set yyyy-mm-dd at same value "2000-01-01")
            Date reservationTimeFrom = DefaultTimeValueConverter.getValidTimeValue(reservation.getDateFrom());
            Date reservationTimeTo = DefaultTimeValueConverter.getValidTimeValue(reservation.getDateTo());

            for(ReservationEntity overlapReservation : potentiallyOverlappingReservations) {
                //Need to convert date to same format where date is set to 2000-01-01 and time is untouched
                //This way we can compare times (time saved in DB allready has this format)
                Date overlapFrom = DefaultTimeValueConverter.getValidTimeValue(overlapReservation.getDateFrom());
                Date overlapTo = DefaultTimeValueConverter.getValidTimeValue(overlapReservation.getDateTo());

                if(checkOverlap(reservationTimeFrom, reservationTimeTo, overlapFrom, overlapTo, true))
                    overlappingReservations.add(overlapReservation);
            }
        }

        /*
         * Now we know that all reservations in List "overlapReservation" are overlaping with our new reservation, but
         * we still must figure out if they are overlaping each other. Reason is we need compute max number of overlaps
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

                if(checkOverlap(re1.getDateFrom(), re1.getDateTo(), re2.getDateFrom(), re2.getDateTo(), true))
                    overlapCount++;
            }

            //Save max overlap count
            maxOverlapCount = maxOverlapCount > overlapCount ? maxOverlapCount : overlapCount;
        }

        //+1 because 1 overlap means that there are 2 reservation in same time (2 overlaps means 3 reservation in same time etc)
        if(overlappingReservations.size() > 0) maxOverlapCount++;

        //+1 because we want add new reservation (+1 our new reservation)
        //Validate if still can add our reservation (due to number limitation)
        if((maxOverlapCount + 1) > reservationObejct.getMaxReservations())
            return "reservation.reservations.max_reservations_error.js";

        //No problem found so return null
        return null;
    }

    /**
     * Prepare reservation to validation. First check if needed values arent null. Then set reservation date based on input value "isReservationForAllDay".
     * If isReservationForAllDay is true, date is same but hh:mm:ss:ms are set to 0 (because reservation upon reservation object for whole day cant set other time then default).
     * If isReservationForAllDay is false, date is set as combination of date from reservation and time from reservationEditorFields.
     * @param reservation
     * @param isReservationForAllDay
     */
    public void  prepareReservationToValidation(ReservationEntity reservation, Boolean isReservationForAllDay) {
        //Check of values
        if(reservation.getDateFrom() == null || reservation.getDateTo() == null || reservation.getEditorFields() == null)
            throwError("html_area.insert_image.error_occured");

        //If reservation object is set for all day (it means we do not select reservation time)
        if(isReservationForAllDay) {
            //In case of "for all day", use only date range and time params set to 0
            reservation.setDateFrom(setTimeOfDate(reservation.getDateFrom(), 0, 0, 0, 0));
            reservation.setDateTo(setTimeOfDate(reservation.getDateTo(), 0, 0, 0, 0));
        } else {
            //Check of values
            if(reservation.getEditorFields().getReservationTimeFrom() == null || reservation.getEditorFields().getReservationTimeTo() == null)
                throwError("html_area.insert_image.error_occured");

            //If reservation ISNT for all day, set reservation date as combination of selected date and time
            Date dateTimeFrom = DefaultTimeValueConverter.combineDateWithTime(reservation.getDateFrom(), reservation.getEditorFields().getReservationTimeFrom());
            Date dateTimeTo = DefaultTimeValueConverter.combineDateWithTime(reservation.getDateTo(), reservation.getEditorFields().getReservationTimeTo());

            reservation.setDateFrom(dateTimeFrom);
            reservation.setDateTo(dateTimeTo);
        }
    }

    /**
     * Get reservation object "email accepter" and send send mail to notify accpter about new waiting reservation for thi reservation object.
     * Email inludes link to this reservation wating for approve.
     * @param reservation
     * @param reservationObject
     */
    public void sendAcceptationEmail(ReservationEntity reservation, ReservationObjectEntity reservationObject, HttpServletRequest request) {
        if(reservation == null || reservationObject == null) return;
        //Validate recipient email
        String recipientEmail = reservationObject.getEmailAccepter();
        try {
            InternetAddress emailAddr = new InternetAddress(recipientEmail);
            emailAddr.validate();
        } catch (AddressException ex) {
            return;
        }

        Prop prop = Prop.getInstance();
		String senderName = notNull(reservation.getName()) + " " + notNull(reservation.getSurname());
		String senderEmail = notNull(reservation.getEmail());
		String reservationObjectName = notNull(reservationObject.getName());
		String dateFrom = Tools.formatDate(reservation.getDateFrom());
		String dateTo = Tools.formatDate(reservation.getDateTo());
        String timeFrom = Tools.formatTime(reservation.getDateFrom());
		String timeTo = Tools.formatTime(reservation.getDateTo());
		String subject = prop.getText("components.reservation.mail.title") + senderName + " - " + reservationObjectName;
		String phoneNumber = notNull(reservation.getPhoneNumber());

        String message = "";
        if(reservationObject.getReservationForAllDay()) {
            message = prop.getText("components.reservation.mail.greeting") + "<br /><br />" + senderName
                + " (" + prop.getText("user.phone") + " )" + phoneNumber + " "
                + prop.getText("components.reservation.mail.next") + " <b>" + reservationObjectName + "</b> "
                + prop.getText("reservation.reservations.email_for_all_day") + " "
                + prop.getText("reservation.reservations.email_date_from") + " " + dateFrom + " "
                + prop.getText("reservation.reservations.email_date_to") + " " + dateTo + " "
                + prop.getText("components.reservation.mail.next4") + "<br/> " + reservation.getPurpose() + " <br /><br />"
                + prop.getText("components.reservation.mail.next5") + "<a href=\""+Tools.getBaseHref(request)+"/apps/reservation/admin&id=" + reservation.getId() + "\">"
                + prop.getText("components.reservation.mail.accept") + "</a>";
        } else {
            message = prop.getText("components.reservation.mail.greeting") + "<br /><br />" + senderName
                + " (" + prop.getText("user.phone") + " )" + phoneNumber + " "
                + prop.getText("components.reservation.mail.next") + " <b>" + reservationObjectName + "</b> "
                + prop.getText("reservation.reservations.email_date_from") + " " + dateFrom + " "
                + prop.getText("reservation.reservations.email_date_to") + " " + dateTo + " "
                + prop.getText("components.reservation.mail.next2") + " " + timeFrom + " "
                + prop.getText("components.reservation.mail.next3") + " " + timeTo + " "
                + prop.getText("components.reservation.mail.next4") + "<br/> " + reservation.getPurpose() + " <br /><br />"
                + prop.getText("components.reservation.mail.next5") + "<a href=\""+Tools.getBaseHref(request)+"/apps/reservation/admin&id=" + reservation.getId() + "\">"
                + prop.getText("components.reservation.mail.accept") + "</a>";
        }

		SendMail.send(senderName, senderEmail, recipientEmail, null, null, subject, message, null);
    }

    /**
     * Send confirmation email to email adress set in reservation. Email subject and text is based on reservation "accepted" value where :
     * (true, resevation was accepted),
     * (false, reservation was rejected),
     * (null, reservation status was reset and reservation is waiting for approve)
     * @param reservation approved reservation
     * @param reservationObject reservation object that reservation is trying reservate
     * @param request HttpServletRequest instance
     * @param loggedUserName full name of actualy logged user who change reservation accepted status
     */
    public void sendConfirmationEmail(ReservationEntity reservation, ReservationObjectEntity reservationObject, HttpServletRequest request, String loggedUserName) {
        if(reservation == null || reservationObject == null) return;
        //Validate recipient email
        String recipientEmail = reservation.getEmail();
        try {
            InternetAddress emailAddr = new InternetAddress(recipientEmail);
            emailAddr.validate();
        } catch (AddressException ex) {
            return;
        }

        Prop prop = Prop.getInstance();
        String recipientName = notNull(reservation.getName() + " " + reservation.getSurname());
        String reservationObjectName = notNull(reservationObject.getName());
		String senderName = notNull(reservation.getName() + reservation.getSurname());
		String senderEmail = "no-reply@"+Tools.getBaseHref(request).replace("https://", "").replace("http://", "").replace("www.", "");
        String dateFrom = Tools.formatDate(reservation.getDateFrom());
		String dateTo = Tools.formatDate(reservation.getDateTo());
        String timeFrom = Tools.formatTime(reservation.getDateFrom());
        String timeTo = Tools.formatTime(reservation.getDateTo());
		String subject = "";
        String status = "";
        String endOfEmail = ".";
        Boolean approved = reservation.getAccepted();

        if(approved == null) {
            subject = prop.getText("reservation.reservations.email_subject_reset");
            status = prop.getText("reservation.reservations.email_was_reset");
            endOfEmail = prop.getText("reservation.reservations.reset_end_of_email");
        } else if(approved == true) {
            subject = prop.getText("reservation.reservations.email_subject_accepted");
            status = prop.getText("reservation.reservations.email_was_accepted");
        } else if(approved == false) {
            subject =  prop.getText("reservation.reservations.email_subject_rejected");
            status = prop.getText("reservation.reservations.email_was_rejected");
        }

        String message = "";
        if(reservationObject.getReservationForAllDay()) {
            message = prop.getText("reservation.reservations.email_greeting") + " " + recipientName + ",<br><br>"
                + prop.getText("reservation.reservations.email_your_reservation") + " <b>" + reservationObjectName + "</b> "
                + prop.getText("reservation.reservations.email_for_all_day") + " "
                + prop.getText("reservation.reservations.email_date_from") + " " + dateFrom + " "
                + prop.getText("reservation.reservations.email_date_to") + " " + dateTo + " ";
        } else {
            message = prop.getText("reservation.reservations.email_greeting") + " " + recipientName + ",<br><br>"
                + prop.getText("reservation.reservations.email_your_reservation") + " <b>" + reservationObjectName + "</b> "
                + prop.getText("reservation.reservations.email_date_from") + " " + dateFrom + " "
                + prop.getText("reservation.reservations.email_date_to") + " " + dateTo + " "
                + prop.getText("components.reservation.mail.next2") + " " + timeFrom + " "
                + prop.getText("components.reservation.mail.next3") + " " + timeTo + " ";
        }

        message += "<b>" + status + "<b/> : " + loggedUserName + " " + endOfEmail;

		SendMail.send(senderName, senderEmail, recipientEmail, null, null, subject, message, null);
	}

    /**
     * Take input date and set his time part hh:mm:ss:ms using input params. If input date is null, null will be returned.
     * @param date input date to be edit
     * @param hours number of hours we want to set into date
     * @param minutes number of minutes we want to set into date
     * @param seconds number of seconds we want to set into date
     * @param milliseconds number of milliseconds we want to set into date
     * @return date where date part is same as input but time part is set based on input params
     */
    private Date setTimeOfDate(Date date, int hours, int minutes, int seconds, int milliseconds) {
        if(date == null) return null;
        Calendar newDate = Calendar.getInstance();
        newDate.setTime(date);
        newDate.set(Calendar.HOUR_OF_DAY, hours);
        newDate.set(Calendar.MINUTE, minutes);
        newDate.set(Calendar.SECOND, seconds);
        newDate.set(Calendar.MILLISECOND, milliseconds);
        return newDate.getTime();
    }

    //Now validate if date range s1-e1 and range s2-e2 overlaps
    //Formula ((s1 <= e2) && (s2 <= e1)) , if true, then two dates are overlaping

    /**
     * Validate if inteval s1-e1 and interval s2-e2 are overlaping using logical formula.
     * Used formula ((s1 <= e2) && (s2 <= e1)), return true if they are overlaping.
     * Intervals are overlaping even if one start when second ends (08:00-09:00 and 09:00-10:00).
     * IF function is used to compare date values (intervals) representing TIME we want all the values to share same yyyy-mm-dd part and for this reason
     * with input param "prepareDates" set to true, every date part of value will be set to 2000-01-01, so we can compare times.
     * @param s1 date value representing START of FIRST interval
     * @param e1 date value representing END of FIRST interval
     * @param s2 date value representing START of SECOND interval
     * @param e2 date value representing END of SECOND interval
     * @param prepareDates if true set date part of intervals to 2000-01-01, false/null - do nothing
     * @return true - if interval are overlaping, otherwise false
     */
    public Boolean checkOverlap(Date s1, Date e1, Date s2, Date e2, Boolean prepareDates) {
        if(prepareDates != null && prepareDates != false) {
            s1 = DefaultTimeValueConverter.getValidTimeValue(s1);
            e1 = DefaultTimeValueConverter.getValidTimeValue(e1);
            s2 = DefaultTimeValueConverter.getValidTimeValue(s2);
            e2 = DefaultTimeValueConverter.getValidTimeValue(e2);
        }

        if((s1.getTime() <= e2.getTime()) && (s2.getTime() <= e1.getTime())) return true;
        return false;
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
        throw new RuntimeException(message);
    }
}
