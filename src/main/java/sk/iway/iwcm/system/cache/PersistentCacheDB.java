package sk.iway.iwcm.system.cache;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;
import sk.iway.iwcm.*;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;

import jakarta.persistence.Query;
import java.util.*;

/**
 * PersistentCacheDB.java
 * <p>
 * DAO class for manipulating with PersistentCacheBean
 *
 * @author      $Author: jeeff $
 * @version     $Revision: 1.3 $
 * @Title       webjet7
 * @Company     Interway s.r.o. (www.interway.sk)
 * @Copyright   Interway s.r.o. (c) 2001-2010
 * @created     Date: 20.01.2012 11:08:14
 * @modified    $Date: 2004/08/16 06:26:11 $
 */
public class PersistentCacheDB extends JpaDB<PersistentCacheBean> {
    public static final int TYPE_URLDOWNLOAD = 1;

    public PersistentCacheDB() {
        super(PersistentCacheBean.class);
    }

    public PersistentCacheBean findByCacheId(int cacheId) {
        return JpaTools.findFirstByMatchingProperty(PersistentCacheBean.class, "cacheId", cacheId);
    }

    public List<PersistentCacheBean> findByDataType(int dataType) {
        return JpaTools.findByMatchingProperty(PersistentCacheBean.class, "dataType", dataType);
    }

    public PersistentCacheBean findFirstByDataValue(String dataValue) {
        return JpaTools.findFirstByMatchingProperty(PersistentCacheBean.class, "dataValue", dataValue);
    }

    /**
     * Najde zaznamy ktore maju byt vykonane (je prekroceny next refresh cas)
     *
     * @return
     */
    private List<PersistentCacheBean> findNext() {
        JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
        try {
            ExpressionBuilder builder = new ExpressionBuilder();
            ReadAllQuery dbQuery = new ReadAllQuery(PersistentCacheBean.class, builder);
            Expression expr = builder.get("nextRefresh").lessThan(new Date());
            dbQuery.setSelectionCriteria(expr);

            Query query = em.createQuery(dbQuery);
            List<PersistentCacheBean> records = JpaDB.getResultList(query);
            return records;
        } catch (Exception e) {
            sk.iway.iwcm.Logger.error(e);
        } finally {
            em.close();
        }
        return Collections.<PersistentCacheBean>emptyList();
    }

    /**
     * Main metoda volana z CRONu
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            Logger.debug(PersistentCacheDB.class, "main");
            //ziskaj zoznam objektov ktore uz maju byt vykonane
            PersistentCacheDB cdb = new PersistentCacheDB();
            List<PersistentCacheBean> nextData = cdb.findNext();
            for (PersistentCacheBean c : nextData) {
                try {
                    Logger.println(PersistentCacheDB.class, "main: type=" + c.getDataType() + " value=" + c.getDataValue() + " next=" + Tools.formatDateTime(c.getNextRefresh()));

                    cdb.updateData(c);
                } catch (Exception e) {
                    sk.iway.iwcm.Logger.error(e);
                }
            }
        } catch (Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        }
    }

    /**
     * Nastavi limity pre connection timeout
     */
    private void setConnectionTimeouts() {
        System.setProperty("sun.net.client.defaultConnectTimeout", String.valueOf(10 * 60 * 1000));
        System.setProperty("sun.net.client.defaultReadTimeout", String.valueOf(10 * 60 * 1000));
    }


    /**
     * Nasilne vyvola aktualizaciu databazy cache hodnot
     *
     * @param cacheId
     */
    public void updateData(int cacheId) {
        PersistentCacheBean c = getById(cacheId);
        if (c != null) {
            updateData(c);
            Cache cache = Cache.getInstance();
            cache.removeObject("PersistentCache." + c.getDataValue());
        }
    }

    /**
     * Interne aktualizuje databazu cache hodnot
     *
     * @param c
     */
    public void updateData(PersistentCacheBean c) {
        if (c.getDataType() == PersistentCacheDB.TYPE_URLDOWNLOAD) {
            setConnectionTimeouts();
            String htmlCode = Tools.downloadUrl(c.getDataValue(), SetCharacterEncodingFilter.getEncoding(), null, 10 * 60 * 1000);
            c.setDataResult(htmlCode);
        }

        //neukladame
        if (c.getDataResult() == null) return;

        //uloz vysledok
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, c.getRefreshMinutes());
        c.setNextRefresh(cal.getTime());

        Logger.debug(PersistentCacheDB.class, "Ukladam data, value=" + c.getDataValue() + " next=" + Tools.formatDateTime(c.getNextRefresh()));
        save(c);
    }

    /**
     * Metoda pre stiahnutie URL adresy a ulozenie do persistentnej cache
     *
     * @param url            -  URL adresa pre download
     * @param cacheInMinutes -  pocet minut pre cachovanie
     * @return
     */
    public String downloadUrl(String url, int cacheInMinutes) {
        url = Tools.replace(url, "+", "%2B");
        url = Tools.replace(url, " ", "%20");

        String htmlCode = getUrlDownloadValue(url, cacheInMinutes);
        if (htmlCode != null) return htmlCode;

        //v cache neexistuje, zapiseme docasnu hodnotu
        setUrlDownloadValue(url, "<!-- PERSISTENT_TEMP_VALUE: downloading, please try again later -->", cacheInMinutes);

        //v cache neexistuje, musime stiahnut
        setConnectionTimeouts();

        Adminlog.add(Adminlog.TYPE_DATA_DELETING, "PersistentCache: adding new link, url=" + url, -1, -1);

        htmlCode = Tools.downloadUrl(url, SetCharacterEncodingFilter.getEncoding(), null, 10 * 60 * 1000);
        if (htmlCode != null) //null je kvoli tomu, ze to nastane pri chybe downloadu a niekedy moze byt platny aj prazdny retazec
        {
            setUrlDownloadValue(url, htmlCode, cacheInMinutes);
        }

        return htmlCode;
    }

    private String getUrlDownloadValue(String dataValue, int localCacheMinutes) {
        //lokalna cache
        String localCacheName = "PersistentCache." + dataValue;
        Cache c = Cache.getInstance();
        if (localCacheMinutes > 0) {
            Object o = c.getObject(localCacheName);
            if (o != null && o instanceof String) {
                return (String) o;
            }
        }

        //persistent cache
        PersistentCacheBean cb = findFirstByDataValue(dataValue);

        if (cb != null) {
            if (localCacheMinutes > 0) {
                //ak sa nejedna o docasnu hodnotu
                if (cb.getDataResult() != null && cb.getDataResult().indexOf("PERSISTENT_TEMP_VALUE") == -1) {
                    Date nextRefresh = cb.getNextRefresh();
                    long expiryTime = nextRefresh.getTime();
                    if (expiryTime < Tools.getNow()) {
                        //nastav na 5 minut
                        expiryTime = Tools.getNow() + (1000 * 60 * 5);
                    }

                    //ulozime do cache (ak to nie je len docasna hodnota)
                    c.setObjectByExpiry(localCacheName, cb.getDataResult(), expiryTime, true);
                }
            }
            return cb.getDataResult();
        }

        return null;
    }

    private void setUrlDownloadValue(String dataValue, String dataResult, int refreshMinutes) {
        PersistentCacheBean cb = findFirstByDataValue(dataValue);
        if (cb == null) cb = new PersistentCacheBean();

        cb.setDataType(TYPE_URLDOWNLOAD);
        cb.setDataValue(dataValue);
        cb.setDataResult(dataResult);
        cb.setRefreshMinutes(refreshMinutes);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, refreshMinutes);
        cb.setNextRefresh(cal.getTime());

        //null neukladame
        if (cb.getDataResult() == null) return;

        save(cb);
    }
}