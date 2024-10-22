package sk.iway.iwcm.components.reservation.rest;

import java.math.BigDecimal;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    
    @RequestMapping(value="/compute-reservation-price", params={"reservation-date", "time-range", "object-id", "email"})
    public BigDecimal computeReservationPrice(
        @RequestParam("reservation-date") String reservationDateString,
        @RequestParam("time-range") String timeRangeString,
        @RequestParam("object-id") String objectId,
        @RequestParam("email") String email,
        HttpServletRequest request) {

            if(reservationDateString.matches("\\d{4}-\\d{2}-\\d{2}") == false) 
                return new BigDecimal(-1);

            Date reservationDate = Tools.getDateFromString(reservationDateString, "yyyy-MM-dd");
            String[] timeRange = timeRangeString.split("-");
            Date timeFrom = DefaultTimeValueConverter.getValidTimeValue(Integer.valueOf(timeRange[0]), 0);
            Date timeTo = DefaultTimeValueConverter.getValidTimeValue(Integer.valueOf(timeRange[1]), 0);

            //Its CREATE, reservationId is by default -1 
            int userId = ReservationService.getUserToPay(email, Long.valueOf(-1), rr, request);

            return ReservationService.calculateReservationPrice(reservationDate, reservationDate, timeFrom, timeTo, Long.valueOf(objectId), userId, ror, ropr);
    }
}