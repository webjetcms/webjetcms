package sk.iway.iwcm.branch;

import java.io.IOException;
import java.nio.file.*;
import jakarta.persistence.EntityManagerFactory;
import sk.iway.iwcm.*;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.system.cluster.ClusterDB;

/** Cache invalidation for direct JDBC and filesystem publication writes. */
final class BranchPublishEffects {
    private BranchPublishEffects() { }

    static void refreshReaders() {
        evictJpa();
        GroupsDB.getInstance(true);
        DocDB.getInstance(true);
        Cache.getInstance().clearAll();
        // GroupsDB and DocDB already notify the cluster on a forced refresh.
        ClusterDB.addRefresh(Cache.class);
    }

    private static void evictJpa() {
        EntityManagerFactory spring=Tools.getSpringBean("webjet2022EntityManager",EntityManagerFactory.class);
        if(spring!=null)spring.getCache().evictAll();
        // The legacy iwcm unit coexists with Spring's unit in this installation.
        try {
            EntityManagerFactory legacy=DBPool.getEntityManagerFactory("iwcm");
            if(legacy!=null&&legacy!=spring)legacy.getCache().evictAll();
        } catch(NullPointerException absent) {
            if(Tools.getSpringContext()!=null)throw absent;
        }
    }

    static void invalidateFile(String target) throws IOException {
        Cache.getInstance().removeObjectStartsWithName(sk.iway.iwcm.io.FileCache.FILE_CACHE_PREFIX+target,true);
        Path cached=Path.of(Tools.getRealPath(Constants.getString("thumbServletCacheDir")+target.substring(1)));
        if(!Files.isDirectory(cached.getParent()))return;
        String name=cached.getFileName().toString();int dot=name.lastIndexOf('.');if(dot<0)return;
        String expression=java.util.regex.Pattern.quote(name.substring(0,dot))+"-\\d+x\\d+[^/]*"+java.util.regex.Pattern.quote(name.substring(dot));
        try(var variants=Files.list(cached.getParent())) {
            for(Path variant:variants.filter(path->path.getFileName().toString().matches(expression)).toList())Files.deleteIfExists(variant);
        }
    }
}
