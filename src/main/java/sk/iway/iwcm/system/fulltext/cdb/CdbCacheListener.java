package sk.iway.iwcm.system.fulltext.cdb;

import io.github.duckasteroid.cdb.Cdb;
import sk.iway.iwcm.Cache;
import sk.iway.iwcm.CacheBean;
import sk.iway.iwcm.CacheListener;
import sk.iway.iwcm.Logger;

public class CdbCacheListener implements CacheListener {

    static {
        CdbCacheListener listener = new CdbCacheListener();
        Cache.subscribe(listener);
    }

    public static void init() {
        //just to make sure the class is loaded
    }


    @Override
    public void objectAdded(CacheBean theObject) {
        //not needed
    }

    @Override
    public void objectRemoved(CacheBean cacheBean) {
        try {
            if (cacheBean.getName().startsWith("Lucene.") && cacheBean.getObject() instanceof Cdb) {
                Logger.debug(CdbCacheListener.class, "Closing CDB object "+cacheBean.getName());
                Cdb cdb = (Cdb) cacheBean.getObject();
                cdb.close();
            }
        } catch (Exception e) {
            Logger.error(CdbCacheListener.class, e);
        }
    }

}
