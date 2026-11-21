package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.rowset.CachedRowSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import sk.iway.iwcm.DBPool;

/** Verifies raw snapshot inputs and membership aggregation at the SQL read boundary. */
class BranchMediaRowsTest {
    /** One scoped query preserves HTML and nulls and folds joined categories into one medium. */
    @ParameterizedTest
    @CsvSource({"-1,7,m.media_id", "42,0,m.media_fk_id", "-1,0,none"})
    void readsUnconvertedContentAndCategoriesTogether(int owner,int mediaId,String predicate) throws Exception {
        Map<String,Object> stored=new LinkedHashMap<>(BranchMediaMapper.initialValues(7,42,1));
        stored.put("media_title_sk","<img src=x onerror=alert(2)>");
        stored.put("media_sort_order",null); stored.put("branch_category_id",2);
        Connection connection=mock(Connection.class);
        PreparedStatement statement=mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        try(CachedRowSet result=BranchRows.result(stored);var pool=mockStatic(DBPool.class)) {
            result.moveToInsertRow();
            for(var entry:stored.entrySet()) result.updateObject(entry.getKey(),entry.getValue());
            result.updateInt("branch_category_id",9); result.insertRow();result.moveToCurrentRow();result.beforeFirst();
            when(statement.executeQuery()).thenReturn(result);
            pool.when(DBPool::getConnection).thenReturn(connection);

            Map<Integer,Map<String,Object>> rows=BranchMediaRows.read(owner,mediaId,1);

            assertEquals(1,rows.size());
            assertEquals(stored.get("media_title_sk"),rows.get(7).get("media_title_sk"));
            assertNull(rows.get(7).get("media_sort_order"));
            assertEquals(List.of(2,9),BranchFields.normalizedIds(rows.get(7).get("group_ids_snapshot")));
            verify(statement).setInt(1,1);
            if("none".equals(predicate)) verify(statement,never()).setInt(eq(2),anyInt());
            else verify(statement).setInt(2,mediaId>0?mediaId:owner);
            verify(connection).prepareStatement(argThat(sql->sql.contains("m.domain_id=?")
                &&sql.contains("m.media_fk_table_name='documents'")
                &&("none".equals(predicate)||sql.contains(predicate+"=?"))));
            verify(statement,times(1)).executeQuery();
        }
    }
}
