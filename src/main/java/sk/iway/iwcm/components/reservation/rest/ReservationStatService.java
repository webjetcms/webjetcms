package sk.iway.iwcm.components.reservation.rest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.DateTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.calendar.Month;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.reservation.jpa.ReservationEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationStatDTO;

@Service
public class ReservationStatService {

    private ReservationStatService() {}

    private static final BigDecimal HOUR_IN_MILLIS = new BigDecimal(1000 * 60 * 60);

    public enum ReservationType {
        HOURS,
        DAYS;

        public static ReservationType getReservationType(String reservationType) {
            if("typeDays".equals(reservationType))
                return ReservationType.DAYS;
            return ReservationType.HOURS;
        }
    }

    @Getter
    @Setter
    public static class DoublePieChartData {
        private Integer valueA;
        private BigDecimal valueB;
        @SuppressWarnings("unused")
        private String category;

        public DoublePieChartData(Integer valueA, BigDecimal valueB, String category) {
            this.valueA = valueA;
            this.valueB = valueB;
            this.category = category;
        }

        public void incrementA() { this.valueA++; }
        public void addToB(BigDecimal value) { this.valueB = this.valueB.add(value); }
    }

    @Getter
    @Setter
    public static class LineChartData {
        BigDecimal value;
        Date dayDate;

        public LineChartData(BigDecimal value, Date dayDate) {
            this.value = value;
            this.dayDate = dayDate;
        }

        public void addToValue(BigDecimal addValue) { this.value = this.value.add(addValue); }
    }

    /**
     * Return table data for the given month and reservation type -> for reservation stat page
     * @param serachDate - text in format yyyy-MM
     * @param reservationTypeSting - supported values -> typeDays, typeHours
     * @param reservationRepository
     * @return
     */
    public static List<ReservationStatDTO> getTableData(String serachDate, String reservationTypeSting, ReservationRepository reservationRepository) {
        ReservationType reservationType = ReservationType.getReservationType(reservationTypeSting);
        Date[] dateRange = getDateRange(serachDate);

        //Get all reservations for the given month, domain and they MUST be accepted
        List<ReservationEntity> filteredReservations = reservationRepository.findByDateAndType(dateRange[1], dateRange[0], ReservationType.DAYS.equals(reservationType), CloudToolsForCore.getDomainId());

        //Group reservations by reservation object id AND creatorId
        // !! if creatorId is null or -1 (no logged user) -> group by reservation object id AND email
        Map<String, ReservationStatDTO> reservationsStatMap = new HashMap<>();

        for(ReservationEntity re : filteredReservations) {
            String key = "";
            if(re.getUserId() != null && re.getUserId() > 0) {
                key = re.getReservationObjectId() + "_" + re.getUserId();
            } else {
                key = re.getReservationObjectId() + "_" + re.getEmail();
            }

            ReservationStatDTO rs = reservationsStatMap.get(key);
            if(rs == null) {
                //Combination not in map yet
                rs = new ReservationStatDTO();
                rs.setUserName(re.getName() + " " + re.getSurname());
                rs.setReservationObjectName( re.getReservationObjectForReservation().getName() );
                rs.setTotalPrice( BigDecimal.ZERO );
                rs.setTotalReservedHours( BigDecimal.ZERO );
                rs.setNumberOfReservedDays(0);
                rs.setNumberOfReservations(0);
            }

            //Combination already in map
            rs.setTotalPrice( rs.getTotalPrice().add( re.getPrice() ) );

            //Number of reservations -> count +1 every time
            rs.setNumberOfReservations( rs.getNumberOfReservations() + 1 );

            //Number of reserved days -> one reservation can be for multiple days
            int daysInterval = getDayDiff(re.getDateFrom(), re.getDateTo());
            rs.setNumberOfReservedDays( rs.getNumberOfReservedDays() + daysInterval );

            if(ReservationType.HOURS.equals(reservationType)) {
                //Number of total reserved hours -> it's time range * reserved days
                rs.setTotalReservedHours( rs.getTotalReservedHours().add( computeHoursInterval(re, daysInterval) ) );
            }

            reservationsStatMap.put(key, rs);
        }

        for(ReservationStatDTO rs : reservationsStatMap.values()) {
            if(ReservationType.HOURS.equals(reservationType))
                rs.setAverageTimePerDay( divide(rs.getTotalReservedHours(), rs.getNumberOfReservedDays(), 2) );

            rs.setAverageIntervalInDays( divide(rs.getNumberOfReservedDays(), rs.getNumberOfReservations(), 2) );
        }

        return new ArrayList<>(reservationsStatMap.values());
    }

    /**
     * Return pie chart data for the given month and reservation type -> for reservation stat page
     * @param serachDate - text in format yyyy-MM
     * @param reservationTypeSting - supported values -> typeDays, typeHours
     * @param wantedValue - supported values -> users (count reservations by user), objects (count reservations by reservation objects)
     * @param reservationRepository
     * @return
     */
    public static List<DoublePieChartData> getPieChartData(String serachDate, String reservationTypeSting, String wantedValue, ReservationRepository reservationRepository) {
        ReservationType reservationType = ReservationType.getReservationType(reservationTypeSting);
        Date[] dateRange = getDateRange(serachDate);

        //Get all reservations for the given month, domain and they MUST be accepted
        List<ReservationEntity> filteredReservations = reservationRepository.findByDateAndType(dateRange[1], dateRange[0], ReservationType.DAYS.equals(reservationType), CloudToolsForCore.getDomainId());

        //Supported values for wantedValue -> users, objects
        if("users".equals(wantedValue) == false && "objects".equals(wantedValue) == false) return new ArrayList<>();

        Map<String, DoublePieChartData> map = new HashMap<>();
        for(ReservationEntity re : filteredReservations) {
            String key = "";
            DoublePieChartData cd = new DoublePieChartData(0, BigDecimal.ZERO, "");
            if("users".equals(wantedValue)) {
                key = re.getUserId() > 0 ? re.getUserId().toString() : re.getEmail();
                cd = map.get(key);

                if(cd == null) {
                    String label = re.getUserId() > 0 ? re.getName() + " " + re.getSurname() + " (id:" + re.getUserId() + ")" : re.getName() + " " + re.getSurname() + " (email:" + re.getEmail() + ")";
                    cd = new DoublePieChartData(1, BigDecimal.ZERO, label);
                } else cd.incrementA();


            } else if("objects".equals(wantedValue)) {
                key = re.getReservationObjectId().toString();
                cd = map.get(key);

                if(cd == null) {
                    cd = new DoublePieChartData(1, BigDecimal.ZERO, re.getReservationObjectForReservation().getName());
                } else cd.incrementA();
            }

            //Set value B -> HOURS
            int daysInterval = getDayDiff(re.getDateFrom(), re.getDateTo());
            if(ReservationType.HOURS.equals(reservationType))
                cd.addToB( computeHoursInterval(re, daysInterval) );
            else cd.addToB( new BigDecimal(daysInterval) );

            map.put(key, cd);
        }

        return new ArrayList<>(map.values());
    }

    /**
     * Return line chart data for the given month and reservation type -> for reservation stat page
     * @param serachDate - text in format yyyy-MM
     * @param reservationTypeSting - supported values -> typeDays, typeHours
     * @param reservationRepository
     * @return
     */
    public static Map<String, List<LineChartData>> getLineChartData(String serachDate, String reservationTypeSting, ReservationRepository reservationRepository) {
        ReservationType reservationType = ReservationType.getReservationType(reservationTypeSting);
        Date[] dateRange = getDateRange(serachDate);
        int dayDiff = getDayDiff(dateRange[0], dateRange[1]);

        //Get all reservations for the given month, domain and they MUST be accepted
        List<ReservationEntity> filteredReservations = reservationRepository.findByDateAndType(dateRange[1], dateRange[0], ReservationType.DAYS.equals(reservationType), CloudToolsForCore.getDomainId());

        //Prepare empty map for each reservation object
        Map<String, List<LineChartData>> map = new HashMap<>();
        for(ReservationEntity re : filteredReservations) {
            String key = re.getReservationObjectName();
            if(map.get(key) == null) {
                map.put(key, getInitializeList(dateRange[0], dayDiff));
            }
        }

        //Fill map with data
        for(ReservationEntity re : filteredReservations) {
            String key = re.getReservationObjectName();
            List<LineChartData> reservationData = map.get(key);

            long timeDiff = DateTools.timePartDiff(re.getDateFrom(), re.getDateTo());
            BigDecimal hours = divide(timeDiff, HOUR_IN_MILLIS, 2);

            int firstReservationDay = getDayOfMonth(re.getDateFrom());
            int reservationRangeDiff = getDayDiff(re.getDateFrom(), re.getDateTo());
            for(int i = firstReservationDay; i < (firstReservationDay + reservationRangeDiff); i++) {
                if(ReservationType.HOURS.equals(reservationType))
                    reservationData.get(i-1).addToValue(hours);
                else reservationData.get(i-1).addToValue(BigDecimal.ONE);
            }

            map.put(key, reservationData);
        }

        return map;
    }

    /**
     * Return total hours interval for the given reservation and days interval
     * First compute time diff in milliseconds for ONE DAY and then multiply by days interval
     * @param re
     * @param daysInterval
     * @return
     */
    private static BigDecimal computeHoursInterval(ReservationEntity re, int daysInterval) {
        //Time diff in milliseconds for ONE DAY
        long timeDiff = DateTools.timePartDiff(re.getDateFrom(), re.getDateTo());

        //Time diff in milliseconds for WHOLE reservation -> timeDiff * daysInterval
        long wholeDiff = timeDiff * daysInterval;

        //Divide by 1000 * 60 * 60 to get hours -> round to 2 decimal places
        return divide(wholeDiff, HOUR_IN_MILLIS, 2);
    }

    /**
     * Return BigDecimal result of division of two numbers with given scale and rounding mode set to HALF_EVEN
     * @param dividend
     * @param divisor
     * @param scale
     * @return
     */
    private static BigDecimal divide(Number dividend, Number divisor, int scale) {
        return new BigDecimal(dividend.toString()).divide(new BigDecimal(divisor.toString()), scale, RoundingMode.HALF_EVEN);
    }

    /**
     * Return date range for whole MONTH from the given string of format yyyy-MM.
     * If text or format is not correct, return range for current month
     * @param searchDate
     * @return
     */
    private static Date[] getDateRange(String searchDate) {
        int year;
        int month;
        if(Tools.isNotEmpty(searchDate) && searchDate.matches(ReservationService.REGEX_YYYY_MM)) {
            String[] dateParts = searchDate.split("-");
            year = Integer.parseInt(dateParts[0]);
            month = Integer.parseInt(dateParts[1]) - 1; //-1 because Calendar month starts  with 0
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            year = cal.get(Calendar.YEAR);
            month = cal.get(Calendar.MONTH);
        }

        //Get datetime range for the given month and year
        Month monthInstance = new Month(year, month);

        return new Date[]{monthInstance.getStartDate(), monthInstance.getEndDate()};
    }

    /**
     * Return number of days between two dates
     * @param starDate
     * @param endDate
     * @return
     */
    private static int getDayDiff(Date starDate, Date endDate) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(starDate);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(endDate);
        return cal2.get(Calendar.DAY_OF_MONTH) - cal1.get(Calendar.DAY_OF_MONTH) + 1;
    }

    /**
     * Return day of month for the given date
     * @param date
     * @return
     */
    private static int getDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    private static List<LineChartData> getInitializeList(Date startDate, int dayDiff) {
        List<LineChartData> lineChartData = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        for(int i = 0; i < dayDiff; i++) {
            lineChartData.add(new LineChartData(BigDecimal.ZERO, cal.getTime()));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return lineChartData;
    }
}