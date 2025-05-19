package sk.iway.iwcm.components.reservation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.components.reservation.jpa.ReservationEditorFields;
import sk.iway.iwcm.components.reservation.jpa.ReservationEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectEntity;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectPriceRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationObjectRepository;
import sk.iway.iwcm.components.reservation.jpa.ReservationRepository;
import sk.iway.iwcm.components.reservation.rest.ReservationService;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.annotations.DefaultHandler;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.ProcessItemAction;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.jpa.DefaultTimeValueConverter;
import sk.iway.iwcm.users.UsersDB;

@WebjetComponent("sk.iway.iwcm.components.reservation.TimeBookApp")
@WebjetAppStore(nameKey = "components.reservation.time_book.title", descKey="components.reservation.time_book.desc", imagePath = "ti ti-calendar-check text-success bg-light", galleryImages = "/apps/reservation/mvc/app-page.png", commonSettings = true)
@Getter
@Setter
public class TimeBookApp extends WebjetComponentAbstract {

    @JsonIgnore
    private ReservationObjectRepository ror;

    @JsonIgnore
    private ReservationRepository rr;

    @JsonIgnore
    private ReservationObjectPriceRepository ropr;

    private static final String VIEW_PATH = "/apps/reservation/mvc/time-book"; //NOSONAR
    private static final String ERROR_MSG = "components.reservation.reservation_app.save_error";

    @Autowired
    public TimeBookApp(ReservationObjectRepository ror, ReservationRepository rr, ReservationObjectPriceRepository ropr) {
        this.ror = ror;
        this.rr = rr;
        this.ropr = ropr;
    }

    //Choose reservation object
    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, title = "components.reservation.time_book.reservation_object_ids", tab = "basic", editor = {
        @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "method:sk.iway.iwcm.components.reservation.rest.ReservationService.getReservationObjectHoursSelectList", value = "label:value")
            }
        )
    })
    private String reservationObjectIds;

    @Override
    public void init(HttpServletRequest request, HttpServletResponse response) {
        Logger.debug(TimeBookApp.class, "Init of TimeBookApp app");
    }

    @DefaultHandler
	public String view(Model model, HttpServletRequest request)
	{
        prepareTimeBookApp(model, request, null);
        return VIEW_PATH;
	}

    public String saveForm(@Valid @ModelAttribute("entity") ReservationEntity entity, BindingResult result, Model model, HttpServletRequest request) {
        //Remove errors about timeFrom-timeTo this fields are sent in separe param
        List<ObjectError> realErrors = new ArrayList<>();
        for(ObjectError err : result.getAllErrors()) {
            FieldError ferr = (FieldError) err;
            if(ferr.getField().equals("dateFrom") || ferr.getField().equals("dateTo")) {
                continue;
            }
            realErrors.add(err);
        }

        if(Tools.isEmail(entity.getEmail()) == false) {
            realErrors.add(new FieldError("entity", "email", Prop.getInstance(request).getText("javax.validation.constraints.Email.message")));
        }

        if(Tools.isEmpty(realErrors)) {
            Prop prop = Prop.getInstance(request);

            Long reservationObjectId = Tools.getLongValue(request.getParameter("reservationObjectId"), -1);
            if(reservationObjectId == -1) {
                return returnError(prop.getText(ERROR_MSG), model, request);
            }

            String reservationDateString = request.getParameter("reservationDateHidden");
            Date reservationDate;
            if(reservationDateString.matches(ReservationService.REGEX_YYYY_MM_DD)) {
                reservationDate = Tools.getDateFromString(reservationDateString, ReservationService.FE_DATEPICKER_FORMAT);
            } else {
                return returnError(prop.getText(ERROR_MSG), model, request);
            }

            String[] timeRange = Tools.getTokens(request.getParameter("timeRange"), "-");
            if(timeRange.length != 2) {
                return returnError(prop.getText(ERROR_MSG), model, request);
            }

            //Remove ":00" postfix
            timeRange[0] = timeRange[0].substring(0, timeRange[0].length()-3);
            timeRange[1] = timeRange[1].substring(0, timeRange[1].length()-3);

            Date timeFrom = DefaultTimeValueConverter.getValidTimeValue(Integer.valueOf(timeRange[0]), 0, 1);
            Date timeTo = DefaultTimeValueConverter.getValidTimeValue(Integer.valueOf(timeRange[1]), 0);
            entity.setReservationObjectId(reservationObjectId);
            entity.setDateFrom( DefaultTimeValueConverter.combineDateWithTime(reservationDate, timeFrom) );
            entity.setDateTo( DefaultTimeValueConverter.combineDateWithTime(reservationDate, timeTo) );

            try {
                ReservationObjectEntity roe = ror.findById(entity.getReservationObjectId()).orElse(null);
                if(roe == null) {
                    return returnError(prop.getText(ERROR_MSG), model, request);
                }

                entity.setReservationObjectForReservation(roe);
                ReservationEditorFields ref = new ReservationEditorFields();
                ref.toReservationEntity(entity, rr, request, true, false, ProcessItemAction.CREATE);
            } catch (Exception e) {
                return returnError( e.getLocalizedMessage(), model, request);
            }

            //Its CREATE, reservationId is by default -1
            int userId = ReservationService.getUserToPay(entity.getEmail(), Long.valueOf(-1), rr, request);
            entity.setUserId(userId);
            entity.setPrice( ReservationService.calculateReservationPrice(reservationDate, reservationDate, timeFrom, timeTo, reservationObjectId, userId, ror, ropr) );

            boolean isAccepted = ReservationService.acceptation(entity, request);
            rr.save(entity);

            //After save send mail
            ReservationService reservationService = new ReservationService(prop);
            reservationService.sendCreatedReservationEmail(entity, request);

            //Add suitable message
            if(isAccepted) {
                model.addAttribute("saveMsg", prop.getText("components.reservation.reservation_app.save_msg"));
            } else {
                model.addAttribute("saveMsg", prop.getText("components.reservation.reservation_app.save_msg_acceptation"));
            }

            prepareTimeBookApp(model, request, entity.getDateFrom());
            return VIEW_PATH;
        }

        prepareTimeBookApp(model, request, entity.getDateFrom());
        model.addAttribute("errors", realErrors);
        model.addAttribute("reservationEntity", entity);
        return VIEW_PATH;
    }

    private void prepareTimeBookApp(Model model, HttpServletRequest request, Date setReservationDate) {
        if(Tools.isEmpty(reservationObjectIds)) return;
        Integer[] ids = Arrays.stream( Tools.getTokensInt(reservationObjectIds, "+") ).boxed().toArray( Integer[]::new );
        List<ReservationObjectEntity> reservationObjectList = ror.findAllByIdIn(ids);

        Date reservationDate;
        if(setReservationDate != null) {
            reservationDate = setReservationDate;
        } else {
            reservationDate = ReservationService.getReservationDate(request.getParameter("reservation-date"), ReservationService.FE_DATEPICKER_FORMAT);
        }

        //datepicker require yyyy-MM-dd string as value
        SimpleDateFormat format = new SimpleDateFormat(ReservationService.FE_DATEPICKER_FORMAT);
        model.addAttribute("reservationDate", format.format(reservationDate));

        /* Prepare list with hour that represent reservation range (joined by all reservation objects range) */
        Long minTimeStart = Long.MAX_VALUE;
        Long maxTimeEnd = Long.MIN_VALUE;
        Map<Long, Long[]> timeRanges = new HashMap<>();
        for(ReservationObjectEntity roe : reservationObjectList) {
            Long[] timeRange = ReservationService.getReservationTimeRange(reservationDate, roe);
            timeRanges.put(roe.getId(), timeRange);
            if(timeRange[0] < minTimeStart) minTimeStart = timeRange[0];
            if(timeRange[1] > maxTimeEnd) maxTimeEnd = timeRange[1];
        }
        List<String> hours = ReservationService.getHoursForTable(minTimeStart, maxTimeEnd);
        model.addAttribute("hours", hours);

        /* Figure out, how many reservations for current hour and reservation object are already used */
        Map<String, List<ReservationService.ReservationTableCell>> tableLines = new HashMap<>();
        for(ReservationObjectEntity roe : reservationObjectList) {
            tableLines.put(roe.getName(), ReservationService.computeReservationUsageByHours(roe, rr, minTimeStart, maxTimeEnd, timeRanges.get(roe.getId())));
        }
        model.addAttribute("tableLines", tableLines);

        ReservationEntity reservation = new ReservationEntity();
        Identity user = UsersDB.getCurrentUser(request);
        if(user != null) {
            reservation.setName(user.getFirstName());
            reservation.setSurname(user.getLastName());
            reservation.setEmail(user.getEmail());
        }
        model.addAttribute("reservationEntity", reservation);
    }

    public String returnError(String errorMsg, Date reservationDate, Model model, HttpServletRequest request) {
        request.setAttribute("reservationDate", reservationDate);
        return returnError(errorMsg, model, request);
    }

    public String returnError(String errorMsg, Model model, HttpServletRequest request) {
        model.addAttribute("customError", errorMsg);
        prepareTimeBookApp(model, request, null);
        return VIEW_PATH;
    }
}