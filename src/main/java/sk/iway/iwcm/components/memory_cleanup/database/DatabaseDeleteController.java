package sk.iway.iwcm.components.memory_cleanup.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.Datatable;
import sk.iway.iwcm.system.datatable.DatatablePageImpl;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;
import sk.iway.iwcm.utils.Pair;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Trieda sluzi na cistenie pamate pre Statistiku, E-maily, Historiu stranok, Monitorovanie servera a Audit.
 *
 * @author pgajdos
 * @since 24.07.2020
 */
@Datatable
@RestController
@RequestMapping(value = "/admin/rest/settings/date-dependent-entries")
@PreAuthorize(value = "@WebjetSecurityService.hasPermission('cmp_data_deleting')")
public class DatabaseDeleteController extends DatatableRestControllerV2<DatabaseDeleteBean, Long> {

    private final DatabaseDeleteService cleanupService;

    @Autowired
    public DatabaseDeleteController(DatabaseDeleteService cleanupService) {
        super(null);
        this.cleanupService = cleanupService;
    }

    /**
     * Mapping sluziaci na ziskanie skupin zaznamov.
     *
     * @param pageable Pageable objekt obsahujuci parametre pre strankovanie.
     * @return Page<MemoryCleanupDateDependentEntity> Page objekt obsahujuci MemoryCleanupDateDependentEntity.
     */
    @Override
    public Page<DatabaseDeleteBean> getAllItems(Pageable pageable) {
        Prop prop = Prop.getInstance(getRequest());
        DatatablePageImpl<DatabaseDeleteBean> page = new DatatablePageImpl<>(cleanupService.getAllItems(prop));
        page.addOptions("groupId", cleanupService.getGroupNames(prop), "label", "value", false);

        return page;
    }

    /**
     * Metoda sluzi na filtrovanie/sortovanie skupin zaznamov. Metoda sa taktiez stara o ziskavanie poctu zaznamov na mazanie.
     * Zaznamy sa ziskaju vtedy, pokial sa filtruje podla datumu (pokial params obsahuju datum z filtru tabulky).
     *
     * @param params   Parametre podla ktorych sa vykanava filtrovanie/sortovanie.
     * @param pageable Pageable objekt obsahujuci parametre pre strankovanie.
     * @param search   Prazdny objekt MemoryCleanupDateDependentEntity (funkcionalita z ineho modulu).
     * @return Page<MemoryCleanupDateDependentEntity> Page objekt obsahujuci upraveny zoznam MemoryCleanupDateDependentEntity.
     */
    @Override
    public Page<DatabaseDeleteBean> searchItem(Map<String, String> params, Pageable pageable, DatabaseDeleteBean search) {

        DatatablePageImpl<DatabaseDeleteBean> page;
        String dateRange = params.get("searchfrom");
        if (Tools.isNotEmpty(dateRange)) {
            Pair<Date, Date> dates = getCleanStatDateValue(dateRange);
            Prop prop = Prop.getInstance(getRequest());
            page = new DatatablePageImpl<>(cleanupService.getMemoryCleanupEntities(dates.first, dates.second, prop));
        }
        else {
            Prop prop = Prop.getInstance(getRequest());
            page = new DatatablePageImpl<>(cleanupService.getAllItems(prop));
        }
        return page;
    }

    /**
     * Mapping sluziaci na mazanie zaznamov.
     *
     * @param entities DatatableRequest objekt obsahujuci datumovi ramec v ktorom sa maju zaznami vymazat.
     * @return boolean pre potvrdenie mazania.
     */
    @Override
    public boolean deleteItem(DatabaseDeleteBean entity, long id) {
        setForceReload(true);
        return cleanupService.delete(entity);
    }


    /**
     * Metoda sluzi na spracovanie prichadzajuceho casoveho ramca.
     *
     * @param statDate String obsahujuci casovi ramec.
     * @return Pair objekt s dvoma datumami od/do.
     */
    private Pair<Date, Date> getCleanStatDateValue(String statDate) {
        String filteredDate = statDate.replace("daterange:", "");
        String[] stringDateArray = new String[2];

        if (filteredDate.contains("-")) {
            if (filteredDate.startsWith("-")) {
                stringDateArray[0] = "";
                stringDateArray[1] = filteredDate.replace("-", "");
            } else {
                stringDateArray = filteredDate.split("-");
            }
        } else {
            stringDateArray[0] = filteredDate;
            stringDateArray[1] = "";
        }

        long[] longDateArray = new long[]{Tools.getLongValue(stringDateArray[0], 0), Tools.getLongValue(stringDateArray[1], new Date().getTime())};

        Date dateFrom = new Date(longDateArray[0]);
        //datum do mame 7.10.2020 ale myslime do konca dna, posunieme teda o jeden den
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(longDateArray[1]);
        cal.add(Calendar.DATE, 1);
        cal.add(Calendar.SECOND, -1);
        Date dateTo = cal.getTime();
        return new Pair<>(dateFrom, dateTo);
    }
}
