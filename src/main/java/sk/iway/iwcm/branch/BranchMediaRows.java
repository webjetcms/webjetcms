package sk.iway.iwcm.branch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import sk.iway.iwcm.DBPool;

/** Reads content and membership together before JPA's display-only HTML escaping. */
final class BranchMediaRows {
    private BranchMediaRows() { }

    static Map<Integer,Map<String,Object>> read(int owner,int mediaId,int domainId) {
        String filter=mediaId>0?" AND m.media_id=?":owner>0?" AND m.media_fk_id=?":"";
        String sql="SELECT m.*,c.media_group_id AS branch_category_id FROM media m LEFT JOIN media_group_to_media c ON c.media_id=m.media_id"
            +" WHERE m.media_fk_table_name='documents' AND m.domain_id=?"+filter+" ORDER BY m.media_id,c.media_group_id";
        try(Connection connection=DBPool.getConnection();PreparedStatement statement=connection.prepareStatement(sql)) {
            statement.setInt(1,domainId);
            if(!filter.isEmpty()) statement.setInt(2,mediaId>0?mediaId:owner);
            Map<Integer,Map<String,Object>> rows=new LinkedHashMap<>();
            Map<Integer,List<Integer>> categories=new LinkedHashMap<>();
            try(ResultSet result=statement.executeQuery()) {
                while(result.next()) {
                    int id=result.getInt("media_id");
                    if(!rows.containsKey(id)) {
                        Map<String,Object> row=new LinkedHashMap<>();
                        for(String column:BranchFields.MEDIA.columns()) row.put(column,BranchStore.readColumn(result,result.findColumn(column),column));
                        List<Integer> ids=new ArrayList<>();categories.put(id,ids);row.put("group_ids_snapshot",ids);rows.put(id,row);
                    }
                    int category=result.getInt("branch_category_id");
                    if(!result.wasNull()) categories.get(id).add(category);
                }
            }
            return rows;
        } catch(SQLException ex) {throw new IllegalStateException("Cannot read branch media",ex);}
    }
}
