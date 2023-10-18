package sk.iway.iwcm.components.memory_cleanup.database;

import org.springframework.stereotype.Service;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.components.dataDeleting.DataDeletingManager;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.utils.Pair;

import java.util.*;

/**
 * Trieda sluziaca na vykonavanie funkcionalit mazania pamate.
 *
 * @author pgajdos
 * @since 24.07.2020
 */
@Service
public class DatabaseDeleteService {

    public DatabaseDeleteService() {
        //konstruktor
    }

    private DatabaseDeleteBean buildStatBean(Prop prop, String key, String tableName, boolean tablePartitioning) {
        return DatabaseDeleteBean.builder().name(prop.getText(key)).tableName(tableName).groupId(1).tablePartitioning(tablePartitioning).build();
    }

    private DatabaseDeleteBean buildAuditBean(Prop prop, int typeId) {
        return DatabaseDeleteBean.builder().name(prop.getText("components.adminlog."+typeId)).tableName("audit").groupId(5).typeId(typeId).build();
    }

    /**
     * Vrati zoznam vsetkych moznosti mazania dat, ako pocet na zmazanie je 0, realne nevykona nacitanie poctu zaznamov z DB
     * @param prop
     * @return
     */
    public List<DatabaseDeleteBean> getAllItems(Prop prop) {
        List<DatabaseDeleteBean> items = new ArrayList<>();

        //statistika
        items.add(buildStatBean(prop, "stat_menu.searchEngines", "stat_searchengine", true));
        items.add(buildStatBean(prop, "components.data.deleting.stats.userlogon", "stat_userlogon", false));
        items.add(buildStatBean(prop, "components.data.deleting.stats.referers", "stat_from", true));
        items.add(buildStatBean(prop, "stat_menu.invalidPages", "stat_error", true));
        items.add(buildStatBean(prop, "components.data.deleting.stats.view", "stat_views", true));
        items.add(buildStatBean(prop, "components.stat.heat_map.title", "stat_clicks", true));
        items.add(buildStatBean(prop, "components.memory_cleanup.banner_clicks", "banner_stat_clicks", false));
        items.add(buildStatBean(prop, "components.memory_cleanup.banner_views", "banner_stat_views", false));
        items.add(buildStatBean(prop, "components.memory_cleanup.banner_day_views", "banner_stat_views_day", false));

        items.add(DatabaseDeleteBean.builder().name(prop.getText("components.data.deleting.emails")).groupId(2).tableName("emails").build());
        items.add(DatabaseDeleteBean.builder().name(prop.getText("components.data.deleting.documentHistory")).groupId(3).tableName("documents_history").build());
        items.add(DatabaseDeleteBean.builder().name(prop.getText("components.data.deleting.monitoring")).groupId(4).tableName("monitoring").build());

        //audit
        items.add(buildAuditBean(prop, 100)); //SE_SITEMAP
        items.add(buildAuditBean(prop, 120)); //FORMMAIL
        items.add(buildAuditBean(prop, 130)); //SENDMAIL
        items.add(buildAuditBean(prop, 140)); //JSPERROR
        items.add(buildAuditBean(prop, 150)); //SQLERROR
        items.add(buildAuditBean(prop, 170)); //RUNTIME_ERROR
        items.add(buildAuditBean(prop, 230)); //CRON
        items.add(buildAuditBean(prop, 99999)); //CLIENT_SPECIFIC

        //nastav idecka
        int counter = 1;
        for (DatabaseDeleteBean b : items) {
            b.setId(Long.valueOf(counter));
            counter++;
        }

        return items;
    }

    /**
     * nazvy skupiny a jeho id oznacenia. Vyuzitie pri selectoch v datatabulke.
     *
     * @return List obsahujuci meno a id skupiny zaznamov.
     */
    List<LabelValueDetails> getGroupNames(Prop prop) {
        return Arrays.asList(
                new LabelValueDetails(prop.getText("components.data.deleting.stats"), "1"),
                new LabelValueDetails(prop.getText("components.data.deleting.emails"), "2"),
                new LabelValueDetails(prop.getText("components.data.deleting.documentHistory"), "3"),
                new LabelValueDetails(prop.getText("components.data.deleting.monitoring"), "4"),
                new LabelValueDetails(prop.getText("components.data.deleting.audit.menu"), "5"));
    }

    /**
     * vrati zoznam moznosti mazania vratane poctu zaznamov na zmazanie za zvolene obdobie
     * @param from - datum zaciatku mazania dat
     * @param to - datum konca mazania dat
     * @param prop
     * @return
     */
    List<DatabaseDeleteBean> getMemoryCleanupEntities(Date from, Date to, Prop prop) {
        List<DatabaseDeleteBean> items = getAllItems(prop);

        if (from != null && to != null && to.getTime()>from.getTime()) {
            Pair<Date, Date> datePair = new Pair<>(from, to);
            for (DatabaseDeleteBean entity : items) {
                entity.setFrom(datePair.first);
                entity.setTo(datePair.second);
                if (entity.isTablePartitioning() && Constants.getBoolean("statEnableTablePartitioning")) {
                    entity.setNumberOfEntriesToDelete(DataDeletingManager.checkTablePartitioning(entity.getTableName(), datePair.first, datePair.second));
                } else if (entity.getTypeId()<1) {
                    if ("documents_history".equals(entity.getTableName())) {
                        entity.setNumberOfEntriesToDelete(DataDeletingManager.checkData(entity.getTableName(), datePair.first, datePair.second, true, -1));
                    } else {
                        entity.setNumberOfEntriesToDelete(DataDeletingManager.checkData(entity.getTableName(), datePair.first, datePair.second, false, -1));
                    }
                } else if ("audit".equals(entity.getTableName())) {
                    entity.setNumberOfEntriesToDelete(DataDeletingManager.checkData(entity.getTableName(), datePair.first, datePair.second, false, entity.getTypeId()));
                }
            }
        }
        return items;
    }

    /**
     * Metoda sluzi na zmazanie dat. Nemazu sa samotne MemoryCleanupDateDependentEntity, tie predstavuju len skupiny
     * pod ktorymi su ulozene realne zaznamy na mazanie.
     *
     * @param entity objekt, ktoreho data sa maju zmazat
     * @return boolean pre potvrdenie mazania.
     */
    boolean delete(DatabaseDeleteBean entity) {
        try {
            Pair<Date, Date> datePair = new Pair<>(entity.getFrom(), entity.getTo());
            if (entity.isTablePartitioning() && Constants.getBoolean("statEnableTablePartitioning")) {
                DataDeletingManager.deleteTablePartitioning(entity.getTableName(), datePair.first, datePair.second, true);
            } else if (entity.getTypeId()<1) {
                if ("documents_history".equals(entity.getTableName())) {
                    DataDeletingManager.deleteData(entity.getTableName(), datePair.first, datePair.second, true, -1, true);
                } else {
                    DataDeletingManager.deleteData(entity.getTableName(), datePair.first, datePair.second, false, -1, true);
                }
            } else if ("audit".equals(entity.getTableName())) {
                DataDeletingManager.deleteData(entity.getTableName(), datePair.first, datePair.second, false, entity.getTypeId(), true);
            }
        } catch (Exception e) {
            Logger.error(DatabaseDeleteService.class, "Error deleting entries for " + entity.getTableName() + ". Error message is: " + e.getMessage() + ".");
            return false;
        }
        return true;
    }
}
