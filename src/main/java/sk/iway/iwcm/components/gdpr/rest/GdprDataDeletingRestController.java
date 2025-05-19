package sk.iway.iwcm.components.gdpr.rest;


import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.gdpr.GdprDataDeleting;
import sk.iway.iwcm.components.gdpr.GdprDataDeletingEntity;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

@RestController
@RequestMapping("/admin/rest/gdpr/data_deleting")
@PreAuthorize("@WebjetSecurityService.hasPermission('menuGDPRDelete')")
@Datatable
public class GdprDataDeletingRestController extends DatatableRestControllerV2<GdprDataDeletingEntity, Long> {

    private final HttpServletRequest request;
    private static final String DELETE_USER_AFTER_DAYS = "gdprDeleteUserAfterDays";
    private static final String DELETE_EMAILS_AFTER_DAYS = "gdprDeleteEmailsAfterDays";
    private static final String DELETE_FORM_DATA_AFTER_DAYS = "gdprDeleteFormDataAfterDays";
    private static final String DELETE_USER_BASKET_ORDERS_AFTER_YEARS = "gdprDeleteUserBasketOrdersAfterYears";
    private static final String DAYS_KEY = "welcome.statBackTime.days";

    @Autowired
    public GdprDataDeletingRestController(HttpServletRequest request) {
        super(null);
        this.request = request;
    }

    //This page doesnt use repository or DB class, we will create 4 entities static (only two columns are from DB)
    @Override
    public Page<GdprDataDeletingEntity> getAllItems(Pageable pageable) {

        List<GdprDataDeletingEntity> items =  new ArrayList<>();

        //Fill items list with 4 fix created items

        items.add(getDataDeleteEntityById(1));
        items.add(getDataDeleteEntityById(2));
        items.add(getDataDeleteEntityById(3));
        items.add(getDataDeleteEntityById(4));

        return new DatatablePageImpl<>(items);
    }

    @Override
    public GdprDataDeletingEntity getOneItem(long id) {
        return getDataDeleteEntityById((int) id);
    }

    //Only statTime column can be changed (every entity use different method to update value)
    @Override
    public GdprDataDeletingEntity editItem(GdprDataDeletingEntity entity, long id) {

        String newStatTimeString = "";
        Prop prop = getProp();

        if(id == 1) {

            ConfDB.setName(DELETE_USER_AFTER_DAYS, entity.getStatTime() + "");
            newStatTimeString = entity.getStatTime() + " " + prop.getText(DAYS_KEY);
        } else if(id == 2) {

            ConfDB.setName(DELETE_EMAILS_AFTER_DAYS, entity.getStatTime() + "");
            newStatTimeString = entity.getStatTime() + " " + prop.getText(DAYS_KEY);
        } else if(id == 3) {

            ConfDB.setName(DELETE_FORM_DATA_AFTER_DAYS, entity.getStatTime() + "");
            newStatTimeString = entity.getStatTime() + " " + prop.getText(DAYS_KEY);
        } else if(id == 4) {

            ConfDB.setName(DELETE_USER_BASKET_ORDERS_AFTER_YEARS, entity.getStatTime() + "");
            newStatTimeString = entity.getStatTime() + " " + prop.getText("components.gdpr.rokov");
        }

        entity = getDataDeleteEntityById((int) id);
        entity.setStatTimeString(newStatTimeString);

        return entity;
    }

    @Override
    public boolean processAction(GdprDataDeletingEntity entity, String action) {

        //Custom delete (on front end its look like normal delete button)
        if(action.equals("customDataDelete")) {

            Long id = entity.getId();
            GdprDataDeleting gdprdd = GdprDataDeleting.getInstance(request);

            if(id == 1) {

                gdprdd.deleteUnusedUsers();
            } else if(id == 2) {

                gdprdd.deleteSendedEmails();
            }else if(id == 3) {

                gdprdd.deleteOldFormData();
            } else if(id == 4) {

                gdprdd.deleteOldBasketOrders();
            }
        }

        return true;
    }

    //Depending on id from 1 to 4 will create entity, fill its column and return it
    GdprDataDeletingEntity getDataDeleteEntityById(int id) {

        GdprDataDeletingEntity entity = new GdprDataDeletingEntity();
        GdprDataDeleting gdprdd = GdprDataDeleting.getInstance(request);
        Prop prop = getProp();

        if(id == 1) {

            entity.setId(Long.valueOf(1));
            entity.setType(prop.getText("components.gdpr.type.users"));
            entity.setStatTime(Constants.getInt(DELETE_USER_AFTER_DAYS));
            entity.setStatTimeString(Constants.getInt(DELETE_USER_AFTER_DAYS) + " " + prop.getText(DAYS_KEY));
            entity.setRecordCnt(GdprDataDeleting.getUnusedUsers().size());
            entity.setAction(prop.getText("components.gdpr.admin_gdpr_data_deleting.zmazat_uzivatelov_za_dane_obdobie"));
        } else if(id == 2) {

            entity.setId(Long.valueOf(2));
            entity.setType(prop.getText("components.gdpr.type.emails"));
            entity.setStatTime(Constants.getInt(DELETE_EMAILS_AFTER_DAYS));
            entity.setStatTimeString(Constants.getInt(DELETE_EMAILS_AFTER_DAYS) + " " + prop.getText(DAYS_KEY));
            entity.setRecordCnt(gdprdd.getSendedEmailsCount());
            entity.setAction(prop.getText("components.gdpr.admin_gdpr_data_deleting.zmazat_vsetky_odoslane_emaily"));
        } else if(id == 3) {

            entity.setId(Long.valueOf(3));
            entity.setType(prop.getText("components.gdpr.type.forms"));
            entity.setStatTime(Constants.getInt(DELETE_FORM_DATA_AFTER_DAYS));
            entity.setStatTimeString(Constants.getInt(DELETE_FORM_DATA_AFTER_DAYS) + " " + prop.getText(DAYS_KEY));
            entity.setRecordCnt(gdprdd.getOldFormDataCount());
            entity.setAction(prop.getText("components.gdpr.admin_gdpr_data_deleting.zmazat_data_vo_formularoch_za_dane_obdobie"));
        } else if(id == 4) {

            entity.setId(Long.valueOf(4));
            entity.setType(prop.getText("components.gdpr.type.eshop"));
            entity.setStatTime(Constants.getInt(DELETE_USER_BASKET_ORDERS_AFTER_YEARS));
            entity.setStatTimeString(Constants.getInt(DELETE_USER_BASKET_ORDERS_AFTER_YEARS) + " " + prop.getText("components.gdpr.rokov"));
            entity.setRecordCnt((int)gdprdd.getOldBasketOrdersCount());
            entity.setAction(prop.getText("components.gdpr.admin_gdpr_data_deleting.zmazat_objednavky_z_modulu_elektorincky_obchod_za_dane_obdobie"));
        }

        return entity;
    }
}
