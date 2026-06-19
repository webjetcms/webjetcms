package sk.iway.iwcm.components.reservation.jpa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.DateTools;
import sk.iway.iwcm.Tools;

@Getter
@Setter
public class MyReservationDTO {

    private static final long HOUR_MILLIS = 60L * 60L * 1000L;

    private Long reservationId;
    private String reservationObjectName;
    private String reservationRange;
    private String reservationRangeFrom;
    private String reservationRangeTo;
    private BigDecimal price;
    private Boolean accepted;
    private Boolean allDay;
    private Boolean canDelete;
    private Boolean deletePasswordRequired;

    public MyReservationDTO(Long reservationId, Date dateFrom, Date dateTo, BigDecimal price, Boolean accepted, String reservationObjectName, Boolean allDay, String reservationObjectPasswd, Integer cancelTimeBefore) {
        this.reservationId = reservationId;
        this.reservationObjectName = reservationObjectName == null ? "" : reservationObjectName;
        this.allDay = allDay;
        setReservationRange(dateFrom, dateTo);
        this.price = price;
        this.accepted = accepted;
        this.canDelete = canDeleteReservation(dateFrom, accepted, allDay, cancelTimeBefore);
        this.deletePasswordRequired = isDeletePasswordRequired(reservationObjectPasswd);
    }

    private void setReservationRange(Date dateFrom, Date dateTo) {
        if(dateFrom == null || dateTo == null) {
            reservationRangeFrom = "X";
            reservationRangeTo = "";
            reservationRange = reservationRangeFrom;
            return;
        }

        try {
            long between = DateTools.getDaysBetween(dateFrom, dateTo);
            if(Tools.isTrue(allDay)) {
                reservationRangeFrom = Tools.formatDate(dateFrom);
                reservationRangeTo = between < 1 ? "" : Tools.formatDate(dateTo);
            } else if(between < 1) {
                reservationRangeFrom = Tools.formatDateTime(dateFrom);
                reservationRangeTo = Tools.formatTime(dateTo);
            } else {
                reservationRangeFrom = Tools.formatDateTime(dateFrom);
                reservationRangeTo = Tools.formatDateTime(dateTo);
            }
        } catch(Exception e) {
            reservationRangeFrom = "X";
            reservationRangeTo = "";
        }

        if(Tools.isEmpty(reservationRangeTo)) {
            reservationRange = reservationRangeFrom;
        } else {
            reservationRange = reservationRangeFrom + " - " + reservationRangeTo;
        }
    }

    private boolean isDeletePasswordRequired(String reservationObjectPasswd) {
        return Tools.isNotEmpty(reservationObjectPasswd);
    }

    private boolean canDeleteReservation(Date dateFrom, Boolean accepted, Boolean allDay, Integer cancelTimeBefore) {
        if(Tools.isTrue(accepted) == false || dateFrom == null) return false;
        if(isReservationStartInFuture(dateFrom, allDay) == false) return false;

        if(cancelTimeBefore != null && cancelTimeBefore > 0) {
            long lastDeleteTime = dateFrom.getTime() - cancelTimeBefore * HOUR_MILLIS;
            return Tools.getNow() <= lastDeleteTime;
        }

        return true;
    }

    private boolean isReservationStartInFuture(Date dateFrom, Boolean allDay) {
        if(Tools.isTrue(allDay)) {
            LocalDate reservationDate = dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return reservationDate.isAfter(LocalDate.now());
        }

        return dateFrom.getTime() > Tools.getNow();
    }
}
