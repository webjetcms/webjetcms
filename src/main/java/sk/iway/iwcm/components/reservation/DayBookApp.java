package sk.iway.iwcm.components.reservation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
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
import sk.iway.iwcm.system.datatable.json.LabelValueInteger;
import sk.iway.iwcm.users.UsersDB;

/**
 * App for Booking of rooms, cars, etc. for a whole day.
 */
@WebjetComponent("sk.iway.iwcm.components.reservation.DayBookApp")
@WebjetAppStore(nameKey = "components.reservation.day_book.title", descKey="components.reservation.day_book.desc", imagePath = "ti ti-calendar-dollar text-success bg-light", galleryImages = "/apps/reservation/admin/day-book-screenshot-1.png", commonSettings = true)
@Getter
@Setter
public class DayBookApp extends WebjetComponentAbstract {

    @JsonIgnore
    private ReservationObjectRepository ror;

    @JsonIgnore
    private ReservationRepository rr;

    @JsonIgnore
    private ReservationObjectPriceRepository ropr;

    private static final String VIEW_PATH = "/apps/reservation/mvc/day-book"; //NOSONAR
    private static final String ERROR_MSG = "components.reservation.reservation_app.save_error";

    @Autowired
    public DayBookApp(ReservationObjectRepository ror, ReservationRepository rr, ReservationObjectPriceRepository ropr) {
        this.ror = ror;
        this.rr = rr;
        this.ropr = ropr;
    }

    //Choose reservation object
    @DataTableColumn(inputType = DataTableColumnType.MULTISELECT, title = "components.reservation.time_book.reservation_object_ids", tab = "basic", editor = {
        @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "method:sk.iway.iwcm.components.reservation.rest.ReservationService.getReservationObjectDaysSelectList", value = "label:value")
            }
        )
    })
    private String reservationObjectIds;

    @Override
    public void init(HttpServletRequest request, HttpServletResponse response) {
        Logger.debug(TimeBookApp.class, "Init of TimeBookApp app");
    }

    @DefaultHandler
	public String view(Model model, HttpServletRequest request) {
        prepareDayBookApp(model, request, null, false);
        return VIEW_PATH;
	}

    public String saveForm(@Valid @ModelAttribute("entity") ReservationEntity entity, BindingResult result, Model model, HttpServletRequest request) {
        //result.getAllErrors() IS unmodifiable
        List<ObjectError> errors = new ArrayList<>( result.getAllErrors() );

        if(!Tools.isEmail(entity.getEmail())) {
            errors.add(new FieldError("entity", "email", Prop.getInstance(request).getText("jakarta.validation.constraints.Email.message")));
        }

        if(Tools.isEmpty(errors)) {
            Prop prop = Prop.getInstance(request);

            // ITS GOOD
            ReservationEditorFields ref1 = new ReservationEditorFields();
            ref1.setArrivingTime( ReservationService.getArrivalTime(entity) );
            ref1.setDepartureTime( ReservationService.getDepartureTime(entity) );
            entity.setEditorFields(ref1);
            ReservationService.prepareDates(entity, true);

            try {
                ReservationObjectEntity roe = ror.findById(entity.getReservationObjectId()).orElse(null);
                if(roe == null) {
                    //Error
                    prepareDayBookApp(model, request, entity, true);
                    model.addAttribute("customError", prop.getText(ERROR_MSG));
                    return VIEW_PATH;
                }

                entity.setReservationObjectForReservation(roe);
                ReservationEditorFields ref = new ReservationEditorFields();
                ref.toReservationEntity(entity, rr, request, true, false, ProcessItemAction.CREATE);
                entity.setEditorFields(ref);
            } catch (Exception e) {
                //Error
                prepareDayBookApp(model, request, entity, true);
                model.addAttribute("customError", e.getLocalizedMessage());
                return VIEW_PATH;
            }

            //Its CREATE, reservationId is by default -1
            int userId = ReservationService.getUserToPay(entity.getEmail(), Long.valueOf(-1), rr, request);
            entity.setUserId(userId);
            entity.setPrice( ReservationService.calculateReservationPrice(entity, userId, ror, ropr) );

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

            //Successfull save
            prepareDayBookApp(model, request, entity, false);
            return VIEW_PATH;
        }

        //Error
        prepareDayBookApp(model, request, entity, true);
        model.addAttribute("errors", Tools.isEmpty(errors) ? null : errors);
        return VIEW_PATH;
    }

    private void prepareDayBookApp(Model model, HttpServletRequest request, ReservationEntity reservation, boolean keepSelection) {
        Long selectedReservationId = setSelect(model, request);

        model.addAttribute("localization", PageLng.getUserLngIso( Prop.getLng(request, false) ) );
        model.addAttribute("currency", Constants.getString("basketProductCurrency") );
        model.addAttribute("reservationAllDayStartTime", Constants.getString("reservationAllDayStartTime"));
        model.addAttribute("reservationAllDayEndTime", Constants.getString("reservationAllDayEndTime"));

        if(reservation == null) {
            reservation = new ReservationEntity();
            reservation.setReservationObjectId(selectedReservationId);

            //User pre-set infos
            Identity user = UsersDB.getCurrentUser(request);
            if(user != null) {
                reservation.setName(user.getFirstName());
                reservation.setSurname(user.getLastName());
                reservation.setEmail(user.getEmail());
            }
        }  else if(Tools.isNotEmpty(reservation.getActualDate()) || reservation.getDateFrom() != null) {
            //Retunrn to same date
            Calendar cal = Calendar.getInstance();

            //ActualDate prevents a month shift in calendar -> happens when selectwed range in only in right site of calendar (+1 month)
            //ActualDate holds allways value of left site of calendar (actual set date)
            if(Tools.isNotEmpty(reservation.getActualDate())) {
                Date reservationDate = ReservationService.getReservationDate(reservation.getActualDate(), ReservationService.FE_MONTHPICKER_FORMAT);
                cal.setTime(reservationDate);
            } else {
                cal.setTime(reservation.getDateFrom());
            }

            model.addAttribute("currentYear", cal.get(Calendar.YEAR));
            model.addAttribute("currentMonth", cal.get(Calendar.MONTH));

            if(keepSelection && reservation.getDateTo() != null) {
                //Selected range
                model.addAttribute("selectedDateFrom", ReservationService.getDateId(reservation.getDateFrom(), false));
                model.addAttribute("selectedDateTo", ReservationService.getDateId(reservation.getDateTo(), false));
            }
        }

        if(!keepSelection) {
            //Nullify selection values
            reservation.setPrice(null);
            reservation.setDateFrom(null);
            reservation.setDateTo(null);
            reservation.setPurpose(null);
        }

        model.addAttribute("reservationEntity", reservation);
    }

    private Long setSelect(Model model, HttpServletRequest request) {
        List<LabelValueInteger> reservationObjectSelectOptions = new ArrayList<>();
        List<ReservationObjectEntity> reservationObjects;

        if(Tools.isNotEmpty(reservationObjectIds)) {
            List<Integer> ids = Arrays.stream( Tools.getTokensInt(reservationObjectIds, "+") ).boxed().toList();
            reservationObjects = ror.getAllDayReservationsWhereIdsIn(ids);
        } else {
            // All reservations for full day
            reservationObjects = ror.findAllByDomainIdAndReservationForAllDayTrue(CloudToolsForCore.getDomainId());
        }

        for(ReservationObjectEntity roe : reservationObjects) {
            reservationObjectSelectOptions.add(new LabelValueInteger(roe.getName(), roe.getId().intValue()));
        }
        model.addAttribute("reservationObjects", reservationObjectSelectOptions);

        long obtainedReservationId = Tools.getLongValue(request.getParameter("reservation-object-id"), -1);

        if(obtainedReservationId != -1) {
            boolean isSupported = reservationObjects.stream().anyMatch(ro -> ro.getId().equals(obtainedReservationId));
            if(isSupported) {
                return obtainedReservationId;
            }
        }

        // Else return default reservation object id)
        return reservationObjects.get(0).getId();
    }
}
