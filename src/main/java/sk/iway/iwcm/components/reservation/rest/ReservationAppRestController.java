package sk.iway.iwcm.components.reservation.rest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.DateTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectPriceRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationRepository;
import sk.iway.iwcm.system.jpa.DefaultTimeValueConverter;


@RestController
@RequestMapping("/rest/reservation-app")
public class ReservationAppRestController {

    private final ReservationRepository rr;
    private final ReservationObjectRepository ror;
    private final ReservationObjectPriceRepository ropr;

    @Autowired
    public ReservationAppRestController(ReservationRepository rr, ReservationObjectRepository ror, ReservationObjectPriceRepository ropr) {
        this.rr = rr;
        this.ror = ror;
        this.ropr = ropr;
    }

    @GetMapping(value="/compute-reservation-price", params={"reservation-date", "time-range", "object-id", "email"})
    public BigDecimal computeHoursReservationPrice(
        @RequestParam("reservation-date") String reservationDateString,
        @RequestParam("time-range") String timeRangeString,
        @RequestParam("object-id") String objectId,
        @RequestParam("email") String email,
        HttpServletRequest request) {

            if(reservationDateString.matches(ReservationService.REGEX_YYYY_MM_DD) == false)
                return new BigDecimal(-1);

            Date reservationDate = Tools.getDateFromString(reservationDateString, ReservationService.FE_DATEPICKER_FORMAT);
            String[] timeRange = timeRangeString.split("-");
            Date timeFrom = DefaultTimeValueConverter.getValidTimeValue(Integer.valueOf(timeRange[0]), 0);
            Date timeTo = DefaultTimeValueConverter.getValidTimeValue(Integer.valueOf(timeRange[1]), 0);

            //Its CREATE, reservationId is by default -1
            int userId = ReservationService.getUserToPay(email, Long.valueOf(-1), rr, request);

            return ReservationService.calculateReservationPrice(reservationDate, reservationDate, timeFrom, timeTo, Long.valueOf(objectId), userId, ror, ropr);
    }

    @GetMapping(value="/compute-reservation-price", params={"reservation-date-from", "reservation-date-to", "object-id", "email"})
    public BigDecimal computeDaysReservationPrice(
        @RequestParam("reservation-date-from") String reservationDateFromString,
        @RequestParam("reservation-date-to") String reservationDateToString,
        @RequestParam("object-id") String objectId,
        @RequestParam("email") String email,
        HttpServletRequest request) {

            if(!reservationDateFromString.matches(ReservationService.REGEX_YYYY_MM_DD) || !reservationDateToString.matches(ReservationService.REGEX_YYYY_MM_DD))
                return new BigDecimal(-1);

            Date reservationDateFrom = Tools.getDateFromString(reservationDateFromString, ReservationService.FE_DATEPICKER_FORMAT);
            Date reservationDateTo = Tools.getDateFromString(reservationDateToString, ReservationService.FE_DATEPICKER_FORMAT);

            //Its CREATE, reservationId is by default -1
            int userId = ReservationService.getUserToPay(email, Long.valueOf(-1), rr, request);

            return ReservationService.calculateReservationPrice(reservationDateFrom.getTime(), reservationDateTo.getTime(), null, null, Long.valueOf(objectId), userId, ror, ropr);
    }

    @GetMapping(value="/calendar-support-values", params={"reservation-object-id", "reservation-date", "email"})
    public String getCalandarSuspportValues(
        @RequestParam("reservation-object-id") Long reservationObjectId,
        @RequestParam("reservation-date") String reservationDateString,
        @RequestParam("email") String email,
        HttpServletRequest request
    ) {
        JSONObject jsonObject = new JSONObject();
        Date reservationDate = ReservationService.getReservationDate(reservationDateString, ReservationService.FE_MONTHPICKER_FORMAT);

        Calendar cal = Calendar.getInstance();
        cal.setTime(reservationDate);
        jsonObject.put("currentYear", cal.get(Calendar.YEAR));
        jsonObject.put("currentMonth", cal.get(Calendar.MONTH));

        //Range of showed days in calendar
        Date startDate = DateTools.getFirstDateOfMonth(reservationDate, 0).getTime();
        Date endDate = DateTools.getLastDateOfMonth(reservationDate, 1).getTime();

        Map<String, Integer> reservationCountMap = ReservationService.computeReservationUsageByDays(reservationObjectId, startDate, endDate, rr);
        jsonObject.put("reservationCountMap", reservationCountMap);

        List<String> disabledDays = prepareDisabledDays(reservationObjectId, reservationCountMap, jsonObject);

        prepareCheckOutOnlyDays(jsonObject, disabledDays);

        prepareReservationPriceMap(email, startDate, endDate, reservationObjectId, jsonObject, request);

        return jsonObject.toString();
    }

    private void prepareReservationPriceMap(String email, Date startDate, Date endDate, Long reservationObjectId, JSONObject jsonObject, HttpServletRequest request) {
        //Its CREATE, reservationId is by default -1
        int userId = ReservationService.getUserToPay(email, Long.valueOf(-1), rr, request);

        // We MUST ADD 1 day, because map of prices return -1 day (because user last day do not pay, its day of leaving)
        Map<String, BigDecimal> reservationPriceMap = ReservationService.getMapOfPrices(startDate.getTime(), endDate.getTime() + (24*60*60*1000), null, null, reservationObjectId, userId, ror, ropr);
        jsonObject.put("reservationPriceMap", reservationPriceMap);
    }

    private List<String> prepareDisabledDays(Long selectedReservationId, Map<String, Integer> reservationCount, JSONObject jsonObject) {
        int maxReservationCount = ror.getMaxReservationsById(selectedReservationId);

        List<String> disabledDays = new ArrayList<>();
        for(Map.Entry<String, Integer> entry : reservationCount.entrySet()) {
            if(entry.getValue() >= maxReservationCount) {
                disabledDays.add(entry.getKey());
            }
        }

        jsonObject.put("disabledDays", disabledDays);
        jsonObject.put("maxReservationCount", maxReservationCount);

        return disabledDays;
    }

    /**
     * Prepare list of days, where user can check-out only and this days remove from list of disabled days.
     * Then FE login will allow only check-out on this days.
     * @param jsonObject
     */
    private void prepareCheckOutOnlyDays(JSONObject jsonObject, List<String> disabledDays) {
        List<String> newDisabledDays = new ArrayList<>();
        List<String> checkOutOnlyDays = new ArrayList<>();

        for(String disabledDayId : disabledDays) {
            //Prepare id of previous day
            Calendar cal = Calendar.getInstance();
            cal.setTime( Tools.getDateFromString(disabledDayId, ReservationService.FE_DATEPICKER_FORMAT) );
            cal.add(Calendar.DAY_OF_YEAR, -1);
            String previousDayId = ReservationService.getDateId(cal.getTime(), false);

            //Check that previous day is not disabled
            if(disabledDays.contains(previousDayId) == false) {
                checkOutOnlyDays.add(disabledDayId);
            } else {
                newDisabledDays.add(disabledDayId);
            }
        }

        jsonObject.remove("disabledDays");
        jsonObject.put("disabledDays", newDisabledDays);
        jsonObject.put("checkOutOnlyDays", checkOutOnlyDays);
    } 
}