package sk.iway.webjet.v9;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import org.eclipse.persistence.sessions.SessionCustomizer;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sequencing.Sequence;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.jpa.WJGenSequence;

/**
 * SessionCustomizer modifikuje mena @Table anotacie, kedze v Oracle mame inak pomenovane DB tabulky
 * credits: https://gist.github.com/iromu/6864061
 */
public class JpaSessionCustomizer implements SessionCustomizer {

    @Override
    public void customize(Session session) throws SQLException {
        if (Constants.DB_TYPE == Constants.DB_ORACLE) {
            Logger.println(JpaSessionCustomizer.class, "JPA customize=" + session);

            for (ClassDescriptor descriptor : session.getDescriptors().values()) {
                String tableName = descriptor.getTableName();
                if ("_conf_".equals(tableName)) {
                    descriptor.setTableName("webjet_conf");
                } else if ("_conf_prepared_".equals(tableName)) {
                    descriptor.setTableName("webjet_conf_prepared");
                } else if ("_modules_".equals(tableName)) {
                    descriptor.setTableName("webjet_modules");
                } else if ("_adminlog_".equals(tableName)) {
                    descriptor.setTableName("webjet_adminlog");
                } else if ("_db_".equals(tableName)) {
                    descriptor.setTableName("webjet_db");
                } else if ("_properties_".equals(tableName)) {
                    descriptor.setTableName("webjet_properties");
                }
                Logger.println(JpaSessionCustomizer.class, "Changing JPA tableName from=" + tableName + " to=" + descriptor.getTables());
            }
        }

        if (session.getLogin().getDatasourcePlatform().getSequences() != null) {
            /**
             * Replace default EclipseLink.TableSequence with our WJGenSequence using PkeyGenerator
             */
            Set<Map.Entry<String, Sequence>> entrySet = session.getLogin().getDatasourcePlatform().getSequences().entrySet();
            for (Map.Entry<String, Sequence> entry : entrySet) {
                Logger.debug(getClass(), "sequence=" + entry.getKey() + " " + entry.getValue().getClass());

                if (entry.getValue() instanceof org.eclipse.persistence.sequencing.TableSequence) {
                    WJGenSequence wjgen = new WJGenSequence(entry.getKey());
                    session.getLogin().addSequence(wjgen);
                }
            }
        }
    }

}