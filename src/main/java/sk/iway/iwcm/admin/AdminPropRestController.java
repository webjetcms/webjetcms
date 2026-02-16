package sk.iway.iwcm.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.IwayProperties;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.ConfDB;

/**
 * 53128 - preklady pre admin cast (JS subory)
 */
@RestController
@PreAuthorize("@WebjetSecurityService.isAdmin()")
public class AdminPropRestController {

    @RequestMapping(path={"/admin/rest/properties/{lng}/"})
	public Map<String, String> getKeys(HttpServletRequest request, @PathVariable String lng, @RequestParam(required = false) Long since)
	{
		return getKeys(lng, null, since);
	}

    @RequestMapping(path={"/admin/rest/properties/{lng}/{prefix:.+}"})
	public Map<String, String> getKeysWithPrefix(HttpServletRequest request, @PathVariable String lng, @PathVariable String prefix, @RequestParam(required = false) Long since)
	{
		return getKeys(lng, prefix, since);
	}

    @RequestMapping(path={"/admin/rest/properties/lastmodified/{lng}/"})
	public long getLastModifiedResr(@PathVariable String lng)
	{
        return getLastModified(lng);
	}

    /**
     * Vrati datum poslednej zmeny .properties suborov alebo kluca v databaze
     * @param lng
     * @return
     */
    public static long getLastModified(String lng) {
        long lastUpdate = 0;
        if (isLngCorrect(lng)==false) return lastUpdate;

		//ziskaj datum z DB
        Date updateDateDB = (new SimpleQuery()).forDate("SELECT max(update_date) FROM "+ConfDB.PROPERTIES_TABLE_NAME+" WHERE lng=?", lng);
        if (updateDateDB != null) lastUpdate = updateDateDB.getTime();

        //over prekladove texty v properties suboroch
        String lngSuffix = "";
        if ("sk".equals(lng)==false) lngSuffix = "_"+lng;
        lastUpdate = getLastModified("/WEB-INF/classes/text"+lngSuffix+".properties", lastUpdate);
        lastUpdate = getLastModified("/WEB-INF/classes/text"+lngSuffix+"-webjet9.properties", lastUpdate);
        lastUpdate = getLastModified("/WEB-INF/classes/text"+lngSuffix+"-"+Constants.getInstallName()+".properties", lastUpdate);
        lastUpdate = getLastModified("/WEB-INF/classes/text"+lngSuffix+"-"+Constants.getLogInstallName()+".properties", lastUpdate);

        Logger.debug(AdminPropRestController.class, "getLastUpdate="+Tools.formatDateTimeSeconds(lastUpdate));

        return lastUpdate;
    }

    /**
     * Vrati mapu prekladovych klucov so suffixom .js alebo definovanych v
     * konf. premennej propertiesAdminKeys (kluce oddelene novym riadkom alebo ciarkou)
     * ak zadany kluc konci na znak * vratia sa vsetky kluce
     * @param lng
     * @param prefix
     * @param since
     * @return
     */
    private Map<String, String> getKeys(String lng, String prefix, Long since) {
        Map<String, String> result = new HashMap<>();

        if ("cs".equals(lng)) lng = "cz";

        long lastModified = getLastModified(lng);
        if (since != null && since.longValue() > 0) {
            if (lastModified <= since.longValue()) return result;
        }
        result.put("lastmodified", String.valueOf(lastModified));

        Prop prop = Prop.getInstance(lng);

        //bezpecnost, lng moze mat max. 2-3 znaky
        if (isLngCorrect(lng)==false) return result;

        List<String> prefixes = new ArrayList<>();
        if (Tools.isNotEmpty(prefix)) {
            //prefix tiez moze byt tokenizovany
            String[] keys = Tools.getTokens(prefix, ",", true);
            for (String key : keys) {
                prefixes.add(key);
            }
        }
        //pridaj vsetky kluce podla constants premennej
        String[] keys = Tools.getTokens(Constants.getString("propertiesAdminKeys"), ",\n", true);
        for (String key : keys) {
            if (key.endsWith("*") && key.length()>2) {
                prefixes.add(key.substring(0, key.length()-1));
            } else {
                result.put(key, prop.getText(key));
            }
        }

        IwayProperties properties = prop.getRes(lng);
        IwayProperties skProperties = prop.getRes("sk");
        if (properties.isEmpty()) properties = skProperties;
        //pridaj vsetky konciace na .js alebo zacinajuce na prefix
        for (String key : skProperties.keySet())
		{
            if (key.endsWith(".js")) {
                result.put(key, getText(key, properties, skProperties));
            } else {
                for (String p : prefixes) {
                    if (key.startsWith(p)) result.put(key, getText(key, properties, skProperties));
                }
            }
		}
		return result;
    }

    private String getText(String key, IwayProperties properties, IwayProperties skProperties) {
        String text = properties.get(key);
        if (Tools.isEmpty(text)) text = skProperties.get(key);

        return text;
    }

    /**
     * Ziska datum poslednej zmeny suboru na zadanej URL, vrati len cislo vacsie ako currentLastModified
     * @param url
     * @param currentLastModified
     * @return
     */
    private static long getLastModified(String url, long currentLastModified) {
        IwcmFile f = new IwcmFile(Tools.getRealPath(url));
        if (f.exists()) {
            Logger.debug(AdminPropRestController.class, "Last modified "+f.getAbsolutePath()+"="+Tools.formatDateTimeSeconds(f.lastModified()));
            if (f.lastModified()>currentLastModified) return f.lastModified();
        }
        return currentLastModified;
    }

    /**
     * Overi, ci zadany jazy vyhovuje pravidlam
     * @param lng
     * @return
     */
    private static boolean isLngCorrect(String lng) {
        if (lng == null || lng.length()<2 || lng.length()>3) return false;

        return true;
    }
}
