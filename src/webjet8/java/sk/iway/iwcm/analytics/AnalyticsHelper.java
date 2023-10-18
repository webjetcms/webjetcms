package sk.iway.iwcm.analytics;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.utils.Pair;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class AnalyticsHelper {
    /**
     * Vyparsuje konfiguraciu
     * @param config String jeden alebo viac parov url:Trieda oddelenych ';' kde url je zaciatok vyhovujucej url a Trieda je cela cesta k triede implementujucej sk.iway.iwcm.analytics.Tracker, napr. '/files/filearchiv/:sk.iway.iwcm.components.file_archiv.FileArchivTracker'
     * @return vrati rozparsovanu konfiguraciu List<Pair<String, String>> alebo null
     */
    public static List<Pair<String, String>> parseConfig(String config) {
        if(Tools.isNotEmpty(config)) {
            List<Pair<String, String>> result = new ArrayList<>();
            String[] list = config.split(";");
            for(int i = 0; i < list.length; i++) {
                String[] pair = list[i].split(":");
                if(pair.length == 2) {
                    result.add(new Pair<>(pair[0], pair[1]));
                }
            }
            return result;
        }
        return null;
    }

    /**
     * Na zaklade konfiguracie vykona trackovanie
     * @param path absolutna cestna, napr. /files/sth/file.pdf
     * @param request HttpServletRequest
     */
    public static void track(String path, HttpServletRequest request) {
        String analyticsConf = Constants.getString("analyticsTrackerConf");
        if(Tools.isNotEmpty(analyticsConf)) {

            // load configuration
            List<Pair<String, String>> conf = AnalyticsHelper.parseConfig(analyticsConf);
            if(conf != null) {

                for(Pair<String, String> item : conf) {

                    String urlPattern = item.first;
                    String className = item.second;

                    // track for every type of url
                    if(path.startsWith(urlPattern)) {
                        try {
                            @SuppressWarnings("rawtypes")
                            Class c = Class.forName(className);
                            @SuppressWarnings("unchecked")
                            Tracker tracker = (Tracker)c.getDeclaredConstructor().newInstance();
                            tracker.track(path, request);
                        }
                        catch (Throwable e) {
                            try {
                                Logger.error(Class.forName(className), e.getMessage(), e);
                            } catch(ClassNotFoundException ex) {
                                Logger.error(Tracker.class,  className + ": " + e.getMessage(), e);
                            }
                        }
                    }
                }
            }
        }
    }
}
