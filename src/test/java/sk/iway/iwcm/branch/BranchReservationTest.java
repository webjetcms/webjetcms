package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Explicit two-JVM restart proof for the P02 public allocator/storage implementation. */
@EnabledIfEnvironmentVariable(named="WEBJET_P02_DB",matches="true")
class BranchReservationTest extends BaseWebjetTest {
    @BeforeEach void configure() throws Exception {
        try(Connection connection=DBPool.getConnection()){assertEquals("webjet_p01",connection.getCatalog());assertTrue(connection.getMetaData().getURL().startsWith(BranchStorageTest.URL));}
        Constants.setBoolean("branchesEnabled",true);Constants.setBoolean("branchSingleWriterConfirmed",true);Constants.setBoolean("enableStaticFilesExternalDir",true);
        Files.createDirectories(Path.of("/tmp/webjet-p02-private"));Constants.setString("branchStorageRoot","/tmp/webjet-p02-private");
    }
    @AfterEach void restore(){Constants.setBoolean("branchesEnabled",false);Constants.setBoolean("branchSingleWriterConfirmed",false);}

    /** Reserves two identities per type, interleaves native/legacy LIVE generation and persists the restart fixture. */
    @Test @EnabledIfEnvironmentVariable(named="WEBJET_P02_RESTART",matches="before") void beforeDatabaseRestart() throws Exception {
        assertEquals("before",System.getenv("WEBJET_P02_RESTART"),"Run the documented before phase explicitly");
        BranchService service=new BranchService();BranchStore store=new BranchStore();
        BranchEntity branch=service.create("p02-restart.example","P02 restart fixture",null,1);
        BranchContext context=new BranchContext(branch.getId(),branch.getDomainName(),1,BranchContext.Mode.ADMIN);
        try(Connection connection=BranchStorageTest.connect()) {
            BranchStorageTest.execute(connection,"CREATE TABLE IF NOT EXISTS p02_restart_fixture (branch_id BIGINT, before_uptime BIGINT, type VARCHAR(16), lower_id INT, higher_id INT, native_id INT) ENGINE=InnoDB");
            assertEquals(0,BranchStorageTest.count(connection,"SELECT COUNT(*) FROM p02_restart_fixture"),"Complete the prior restart fixture first");
            for(BranchType type:List.of(BranchType.DOCUMENT,BranchType.GROUP,BranchType.MEDIA)) {
                Map<String,Object> initial=initial(type);
                Map<String,Object> lower=store.create(context,type,initial);
                int low=((Number)lower.get(type.idColumn())).intValue();
                int nativeId;
                if(type==BranchType.MEDIA) {
                    nativeId=PkeyGenerator.getNextValue("media");
                    PkeyGenerator.getInstance(true); // A second generator instance bootstraps without lowering the DB counter.
                } else {
                    connection.setAutoCommit(false);
                    nativeId=BranchStore.reserveNative(connection,type);connection.commit();connection.setAutoCommit(true);
                }
                Map<String,Object> higher=store.create(context,type,initial);
                int high=((Number)higher.get(type.idColumn())).intValue();
                assertTrue(high>nativeId);assertTrue(nativeId>low);
                assertTrue(BranchStore.readLive(connection,type,low).isEmpty());assertTrue(BranchStore.readLive(connection,type,high).isEmpty());
                try(PreparedStatement statement=connection.prepareStatement("INSERT INTO p02_restart_fixture VALUES (?,?,?,?,?,?)")) {
                    statement.setLong(1,branch.getId());statement.setLong(2,uptime(connection));statement.setString(3,type.name());statement.setInt(4,low);statement.setInt(5,high);statement.setInt(6,nativeId);statement.executeUpdate();
                }
                System.out.printf("P02 %s before restart: WORK=%d,%d LIVE-generator=%d%n",type,low,high,nativeId);
            }
        }
    }

    /** Reloads committed reservations in a fresh JVM after DB restart and explicitly inserts higher IDs first. */
    @Test @EnabledIfEnvironmentVariable(named="WEBJET_P02_RESTART",matches="after") void afterDatabaseRestart() throws Exception {
        assertEquals("after",System.getenv("WEBJET_P02_RESTART"),"Run the documented after phase explicitly");
        try(Connection connection=BranchStorageTest.connect();var statement=connection.createStatement();ResultSet rows=statement.executeQuery("SELECT * FROM p02_restart_fixture")) {
            int count=0;long branch=0;
            while(rows.next()) {
                count++;branch=rows.getLong("branch_id");
                assertTrue(uptime(connection)<rows.getLong("before_uptime"),"Run immediately after restarting the disposable DB");
                BranchType type=BranchType.valueOf(rows.getString("type"));int low=rows.getInt("lower_id"),high=rows.getInt("higher_id");
                int nativeId;
                connection.setAutoCommit(false);
                try {
                    nativeId=type==BranchType.MEDIA?Math.toIntExact(PkeyGenerator.getNextValueAsLongStrict("media")):BranchStore.reserveNative(connection,type);
                    assertTrue(nativeId>high);
                    Map<String,Object> higher=BranchStore.readWork(connection,branch,type,high).orElseThrow();
                    Map<String,Object> lower=BranchStore.readWork(connection,branch,type,low).orElseThrow();
                    insert(connection,type,higher);insert(connection,type,lower);
                    assertThrows(SQLException.class,()->insert(connection,type,lower));
                    assertEquals(1,BranchStorageTest.count(connection,"SELECT COUNT(*) FROM `"+type.liveTable()+"` WHERE "+type.idColumn()+"="+low));
                    assertEquals(1,BranchStorageTest.count(connection,"SELECT COUNT(*) FROM `"+type.liveTable()+"` WHERE "+type.idColumn()+"="+high));
                }finally{connection.rollback();connection.setAutoCommit(true);}
                System.out.printf("P02 %s after restart: explicit=%d then %d, native=%d%n",type,high,low,nativeId);
            }
            assertEquals(3,count);
            for(String table:List.of("branch_documents","branch_groups","branch_media","branches"))
                BranchStorageTest.execute(connection,"DELETE FROM "+table+" WHERE "+(table.equals("branches")?"id":"branch_id")+"="+branch);
            BranchStorageTest.execute(connection,"DELETE FROM p02_restart_fixture");
        }
    }

    /** Strict and legacy calls share ranges; missing rows and unavailable DB never return fallback identities. */
    @Test void strictMediaFailureAndMonotonicBootstrap() throws Exception {
        try(Connection connection=BranchStorageTest.connect()) {
            long first=PkeyGenerator.getNextValueAsLongStrict("media"),legacy=PkeyGenerator.getNextValueAsLong("media"),next=PkeyGenerator.getNextValueAsLongStrict("media");
            assertTrue(first<legacy && legacy<next);
            long counter=BranchStorageTest.count(connection,"SELECT value FROM pkey_generator WHERE name='media'");
            PkeyGenerator generator=PkeyGenerator.getInstance();
            var getBean=PkeyGenerator.class.getDeclaredMethod("getPkeyBean",String.class);getBean.setAccessible(true);Object bean=getBean.invoke(generator,"media");
            var setMax=PkeyGenerator.class.getDeclaredMethod("setMaxValue",bean.getClass(),long.class);setMax.setAccessible(true);assertEquals(true,setMax.invoke(generator,bean,1L));
            assertEquals(counter,BranchStorageTest.count(connection,"SELECT value FROM pkey_generator WHERE name='media'"));
            BranchStorageTest.execute(connection,"DELETE FROM pkey_generator WHERE name='media'");
            try{assertThrows(SQLException.class,()->PkeyGenerator.getNextValueAsLongStrict("media"));}
            finally{try(PreparedStatement statement=connection.prepareStatement("INSERT INTO pkey_generator VALUES ('media',?,'media','media_id')")){statement.setLong(1,counter);statement.executeUpdate();}}
            try(var pool=org.mockito.Mockito.mockStatic(DBPool.class)) {
                pool.when(DBPool::getConnection).thenThrow(new IllegalStateException("P02 unavailable database"));
                assertThrows(SQLException.class,()->PkeyGenerator.getNextValueAsLongStrict("media"));
            }
            // Even a cached range must reject a missing row; UPDATE must not reuse stale LAST_INSERT_ID.
            BranchStorageTest.execute(connection,"DELETE FROM pkey_generator WHERE name='media'");
            var atomic=PkeyGenerator.class.getDeclaredMethod("atomicIncrementAndGet",Connection.class,String.class,long.class);atomic.setAccessible(true);
            try{var failure=assertThrows(java.lang.reflect.InvocationTargetException.class,()->atomic.invoke(generator,connection,"media",10L));assertInstanceOf(SQLException.class,failure.getCause());}
            finally{try(PreparedStatement statement=connection.prepareStatement("INSERT INTO pkey_generator VALUES ('media',?,'media','media_id')")){statement.setLong(1,counter);statement.executeUpdate();}}
        }
    }

    private static Map<String,Object> initial(BranchType type) {
        return new LinkedHashMap<>(switch(type) {
            case DOCUMENT -> BranchDocumentMapper.initialValues(1,1,Instant.now(),false);
            case GROUP -> BranchGroupMapper.initialValues(1,"p02-restart.example");
            case MEDIA -> BranchMediaMapper.initialValues(1,900001,1);
            default -> throw new IllegalArgumentException();
        });
    }
    private static void insert(Connection connection,BranchType type,Map<String,Object> row)throws SQLException {
        BranchContext live=BranchContext.live("p02-restart.example",1);
        switch(type){
            case DOCUMENT->BranchRowWriter.insertDocumentWithId(connection,live,row);
            case GROUP->BranchRowWriter.insertGroupWithId(connection,live,row);
            case MEDIA->BranchRowWriter.insertMediaWithId(connection,live,row,List.of(2,3));
            default->throw new IllegalArgumentException();
        }
    }
    private static long uptime(Connection connection)throws SQLException{return BranchStorageTest.count(connection,"SELECT VARIABLE_VALUE FROM information_schema.GLOBAL_STATUS WHERE VARIABLE_NAME='UPTIME'");}
}
