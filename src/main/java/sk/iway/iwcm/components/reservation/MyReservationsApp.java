package sk.iway.iwcm.components.reservation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.components.reservation.jpa.MyReservationDTO;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.annotations.DefaultHandler;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.users.UsersDB;

@WebjetComponent("sk.iway.iwcm.components.reservation.MyReservationsApp")
@WebjetAppStore(
    nameKey = "components.reservation.my_reservations.title",
    descKey="components.reservation.my_reservations.desc",
    imagePath = "ti ti-calendar-user text-success bg-light",
    galleryImages = "/apps/reservation/mvc/my-resevations-app-page.png",
    commonSettings = true
)
@Getter
@Setter
public class MyReservationsApp extends WebjetComponentAbstract {

    private static final String VIEW_PATH = "/apps/reservation/mvc/my-reservations-app"; //NOSONAR
    private static final String PARAM_DATE_FROM = "reservation-date-from";
    private static final String PARAM_DATE_TO = "reservation-date-to";
    private static final String PARAM_RESERVATION_ID = "reservationId";
    private static final String PARAM_DELETE_PASSWORD = "deletePassword";
    private static final String DELETE_ERROR_KEY = "components.reservation.reservation_manager.deleteReservation.two";

    @Override
    public void init(HttpServletRequest request, HttpServletResponse response) {
        Logger.debug(MyReservationsApp.class, "Init of MyReservationsApp app");
    }

    @DefaultHandler
	public String view(Model model, HttpServletRequest request)
	{
        prepareView(model, request);
        return VIEW_PATH;
	}

    public String deleteReservation(Model model, HttpServletRequest request) {
        Identity user = UsersDB.getCurrentUser(request);
        Prop prop = Prop.getInstance(request);
        int reservationId = Tools.getIntValue(request.getParameter(PARAM_RESERVATION_ID), -1);
        String deletePassword = request.getParameter(PARAM_DELETE_PASSWORD);
        if(deletePassword == null) deletePassword = "";

        if("POST".equalsIgnoreCase(request.getMethod()) == false || user == null || reservationId < 1) {
            model.addAttribute("deleteError", prop.getText(DELETE_ERROR_KEY));
            prepareView(model, request);
            return VIEW_PATH;
        }

        MyReservationDTO reservation = getMyReservation(reservationId, user.getUserId(), CloudToolsForCore.getDomainId());

        if(reservation == null || Tools.isTrue(reservation.getCanDelete()) == false) {
            model.addAttribute("deleteError", prop.getText(DELETE_ERROR_KEY));
        } else if(Tools.isTrue(reservation.getDeletePasswordRequired()) && Tools.isEmpty(deletePassword)) {
            model.addAttribute("deleteError", prop.getText("reservation.reservations.password_for_delete.msg_2.js"));
        } else {
            String deleteUserEmail = Tools.isTrue(reservation.getDeletePasswordRequired()) ? "" : user.getEmail();
            int deleteResult = ReservationManager.deleteReservation(reservationId, deletePassword, deleteUserEmail);
            if(deleteResult == 0) {
                model.addAttribute("deleteMsg", prop.getText("components.reservation.reservation_manager.deleteReservation.zero"));
            } else {
                model.addAttribute("deleteError", getDeleteReservationError(deleteResult, reservationId, reservation.getDeletePasswordRequired(), prop));
            }
        }

        prepareView(model, request);
        return VIEW_PATH;
    }

    private void prepareView(Model model, HttpServletRequest request) {
        Identity user = UsersDB.getCurrentUser(request);
        String reservationDateFrom = Tools.getRequestParameter(request, PARAM_DATE_FROM);
        String reservationDateTo = Tools.getRequestParameter(request, PARAM_DATE_TO);
        List<MyReservationDTO> reservations = new ArrayList<>();

        if(user != null) {
            reservations = getMyReservations(user.getUserId(), CloudToolsForCore.getDomainId(), reservationDateFrom, reservationDateTo);
        }

        model.addAttribute("reservationDateFrom", reservationDateFrom);
        model.addAttribute("reservationDateTo", reservationDateTo);
        model.addAttribute("reservations", reservations);
    }

    private List<MyReservationDTO> getMyReservations(Integer userId, Integer domainId, String reservationDateFrom, String reservationDateTo) {
        StringBuilder sql = getReservationRowsSql();
        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(domainId);

        sql.append("WHERE r.user_id = ? AND r.domain_id = ? ");

        LocalDate dateFrom = parseDateFilter(reservationDateFrom);
        LocalDate dateTo = parseDateFilter(reservationDateTo);

        // Default range
        if(dateFrom == null && dateTo == null) {
            dateFrom = LocalDateTime.now().minusDays(30).toLocalDate();
        }

        if(dateFrom != null) {
            sql.append("AND r.date_to >= ? ");
            params.add(Timestamp.valueOf(dateFrom.atStartOfDay()));
        }

        if(dateTo != null) {
            sql.append("AND r.date_from < ? ");
            params.add(Timestamp.valueOf(dateTo.plusDays(1).atStartOfDay()));
        }

        sql.append("ORDER BY r.date_from DESC");

        return new ComplexQuery().setSql(sql.toString()).setParams(params.toArray()).list(getReservationRowMapper());
    }

    private MyReservationDTO getMyReservation(Integer reservationId, Integer userId, Integer domainId) {
        StringBuilder sql = getReservationRowsSql();
        sql.append("WHERE r.reservation_id = ? AND r.user_id = ? AND r.domain_id = ? ");

        List<MyReservationDTO> reservations = new ComplexQuery()
            .setSql(sql.toString())
            .setParams(reservationId, userId, domainId)
            .setMaxSize(1)
            .list(getReservationRowMapper());

        if(reservations.isEmpty()) return null;
        return reservations.get(0);
    }

    private StringBuilder getReservationRowsSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT r.reservation_id, r.date_from, r.date_to, r.price, r.accepted, ");
        sql.append("ro.name AS reservation_object_name, ro.reservation_for_all_day AS reservation_for_all_day, ");
        sql.append("ro.passwd AS reservation_object_passwd, ro.cancel_time_befor AS cancel_time_befor ");
        sql.append("FROM reservation r ");
        sql.append("LEFT JOIN reservation_object ro ON r.reservation_object_id = ro.reservation_object_id AND r.domain_id = ro.domain_id ");
        return sql;
    }

    private Mapper<MyReservationDTO> getReservationRowMapper() {
        return new Mapper<MyReservationDTO>() {
            @Override
            public MyReservationDTO map(ResultSet rs) throws SQLException {

                return new MyReservationDTO(
                    rs.getLong("reservation_id"),
                    DB.getDate(rs, "date_from"),
                    DB.getDate(rs, "date_to"),
                    rs.getBigDecimal("price"),
                    DB.getBoolean(rs, "accepted"),
                    DB.getDbString(rs, "reservation_object_name"),
                    DB.getBoolean(rs, "reservation_for_all_day"),
                    DB.getDbString(rs, "reservation_object_passwd"),
                    DB.getInteger(rs, "cancel_time_befor")
                );
            }
        };
    }

    private String getDeleteReservationError(int deleteResult, int reservationId, Boolean deletePasswordRequired, Prop prop) {
        if(deleteResult == 1) {
            if(Tools.isTrue(deletePasswordRequired)) return prop.getText("reservation.reservations.password_for_delete.error_title");
            return prop.getText("components.reservation.reservation_manager.deleteReservation.one");
        }
        if(deleteResult == 3) {
            ReservationObjectBean reservationObject = ReservationManager.getReservationObject(ReservationManager.getReservationObjectId(reservationId));
            if(reservationObject != null) {
                return prop.getText("components.reservation.reservation_manager.deleteReservation.three", String.valueOf(reservationObject.getCancelTimeBefor()));
            }
        }

        return prop.getText(DELETE_ERROR_KEY);
    }

    private LocalDate parseDateFilter(String date) {
        if(Tools.isEmpty(date)) return null;

        try {
            return LocalDate.parse(date);
        } catch(DateTimeParseException e) {
            return null;
        }
    }
}
