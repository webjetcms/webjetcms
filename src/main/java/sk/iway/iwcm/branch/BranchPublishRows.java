package sk.iway.iwcm.branch;

import java.sql.*;
import java.util.*;
import sk.iway.iwcm.*;
import sk.iway.iwcm.i18n.Prop;

/** Fresh JDBC rows, projections and checkpoints used by the single POC publisher. */
final class BranchPublishRows {
    private BranchPublishRows() { }

    static List<Map<String,Object>> rows(Connection connection,long branchId,BranchType type) throws SQLException {
        List<Map<String,Object>> rows=new ArrayList<>();
        try(PreparedStatement statement=connection.prepareStatement("SELECT * FROM `"+type.workTable()+"` WHERE branch_id=? ORDER BY row_id")) {
            statement.setLong(1,branchId);
            try(ResultSet result=statement.executeQuery()) {
                while(result.next()) rows.add(row(result));
            }
        }
        return rows;
    }

    static Map<String,Object> row(ResultSet result) throws SQLException {
        Map<String,Object> row=new LinkedHashMap<>();
        for(int i=1;i<=result.getMetaData().getColumnCount();i++) row.put(result.getMetaData().getColumnName(i),BranchStore.readColumn(result,i));
        return row;
    }

    static Map<Integer,Map<String,Object>> groups(Connection connection) throws SQLException {
        Map<Integer,Map<String,Object>> groups=new LinkedHashMap<>();
        try(PreparedStatement statement=connection.prepareStatement("SELECT * FROM `groups`");ResultSet result=statement.executeQuery()) {
            while(result.next()) {Map<String,Object> row=row(result);groups.put(number(row,"group_id"),row);}
        }
        return groups;
    }

    static boolean inTrash(int groupId,Map<Integer,Map<String,Object>> groups) {
        String trash=Prop.getInstance(Constants.getString("defaultLanguage")).getText("config.trash_dir");
        StringBuilder path=new StringBuilder();Set<Integer> visited=new HashSet<>();
        while(groupId>0&&visited.add(groupId)) {
            Map<String,Object> row=groups.get(groupId);if(row==null)break;
            path.insert(0,"/"+Objects.toString(row.get("group_name"),""));groupId=number(row,"parent_group_id");
        }
        return DB.internationalToEnglish(path.toString()).toLowerCase(Locale.ROOT)
            .contains(DB.internationalToEnglish(trash).toLowerCase(Locale.ROOT));
    }

    static BranchSnapshot liveSnapshot(Connection connection,BranchType type,Map<String,Object> work) throws SQLException {
        if(type==BranchType.FILE)return BranchFiles.liveSnapshot((String)work.get("logical_path"));
        Map<String,Object> live=BranchStore.readLive(connection,type,number(work,type.idColumn())).orElse(null);
        if(live==null)return BranchSnapshot.absent();
        BranchSnapshot.State state=BranchSnapshot.State.ACTIVE;
        if(type==BranchType.DOCUMENT&&inTrash(number(live,"group_id"),groups(connection))
            ||type==BranchType.GROUP&&inTrash(number(live,"group_id"),groups(connection)))state=BranchSnapshot.State.TRASH;
        return switch(type) {
            case DOCUMENT->BranchDocumentMapper.snapshot(live,state);
            case GROUP->BranchGroupMapper.snapshot(live,state);
            case MEDIA->BranchMediaMapper.snapshot(live,live.get("group_ids_snapshot"));
            case FILE->throw new IllegalStateException();
        };
    }

    static void lockLive(Connection connection,BranchType type,Map<String,Object> work) throws SQLException {
        try(PreparedStatement statement=connection.prepareStatement("SELECT "+type.idColumn()+" FROM `"+type.liveTable()+"` WHERE "+type.idColumn()+"=? FOR UPDATE")) {
            statement.setInt(1,number(work,type.idColumn()));try(ResultSet ignored=statement.executeQuery()) {while(ignored.next()) { /* Lock the current physical identity. */ }}
        }
        if(type==BranchType.MEDIA)try(PreparedStatement statement=connection.prepareStatement("SELECT media_id FROM media_group_to_media WHERE media_id=? FOR UPDATE")) {
            statement.setInt(1,number(work,"media_id"));try(ResultSet ignored=statement.executeQuery()) {while(ignored.next()) { /* Lock category membership with the owner. */ }}
        }
    }

    static Map<String,Object> project(Connection connection,BranchType type,Map<String,Object> work,BranchSnapshot target,Map<String,Object> live) throws SQLException {
        Map<String,Object> values=new LinkedHashMap<>();
        Map<String,Object> stable=live==null?work:live;
        for(String name:BranchFields.forType(type).columns()) {
            Object value=target.fields().containsKey(name)?target.fields().get(name):stable.get(name);
            if(Set.of("date_created","event_date","publish_start","publish_end","last_update").contains(name)&&value instanceof Number n)value=new Timestamp(n.longValue());
            values.put(name,value);
        }
        values.put(type.idColumn(),number(work,type.idColumn()));
        if(type==BranchType.DOCUMENT) {
            values.put("perex_group",BranchFields.idTokens(target.fields().get("perex_group")));
            values.put("data_asc",DB.internationalToEnglish(org.jsoup.Jsoup.parse(Objects.toString(values.get("data"),"")).text()).toLowerCase(Locale.ROOT));
            values.put("file_name",values.get("virtual_path"));
            List<Integer> ancestors=new ArrayList<>();Set<Integer> visited=new HashSet<>();int id=number(values,"group_id");
            Map<Integer,Map<String,Object>> groups=groups(connection);
            while(id>0&&visited.add(id)) {ancestors.add(0,id);Map<String,Object> group=groups.get(id);if(group==null)break;id=number(group,"parent_group_id");}
            for(int level=1;level<=3;level++)values.put("root_group_l"+level,ancestors.size()>=level?ancestors.get(level-1):null);
        }
        if(type==BranchType.MEDIA) {
            int owner=number(values,"media_fk_id");
            Map<String,Object> page=BranchStore.readLive(connection,BranchType.DOCUMENT,owner).orElseThrow(()->new SQLException("Media owner disappeared: "+owner));
            Object saved=page.get("date_created");long millis=saved instanceof java.util.Date date?date.getTime():((Number)saved).longValue();
            values.put("last_update",new Timestamp(Math.floorDiv(millis,1000)*1000));
        }
        return values;
    }

    static void update(Connection connection,BranchType type,Map<String,Object> values) throws SQLException {
        List<String> names=new ArrayList<>(values.keySet());names.remove(type.idColumn());
        String assignments=String.join(",",names.stream().map(name->"`"+name+"`=?").toList());
        try(PreparedStatement statement=connection.prepareStatement("UPDATE `"+type.liveTable()+"` SET "+assignments+" WHERE "+type.idColumn()+"=?")) {
            int i=1;for(String name:names)statement.setObject(i++,values.get(name));statement.setObject(i,values.get(type.idColumn()));
            if(statement.executeUpdate()!=1)throw new SQLException("Publication identity disappeared: "+type+":"+values.get(type.idColumn()));
        }
    }

    static void checkpoint(Connection connection,BranchType type,Map<String,Object> work,String state,String phase,String hash,String error) throws SQLException {
        try(PreparedStatement statement=connection.prepareStatement("UPDATE `"+type.workTable()+"` SET apply_state=?,apply_phase=?,applied_hash=?,apply_error=? WHERE row_id=? AND branch_id=?")) {
            statement.setString(1,state);statement.setString(2,phase);statement.setString(3,hash);statement.setString(4,error);
            statement.setLong(5,((Number)work.get("row_id")).longValue());statement.setLong(6,((Number)work.get("branch_id")).longValue());
            if(statement.executeUpdate()!=1)throw new SQLException("Publication checkpoint disappeared");
        }
        work.put("apply_state",state);work.put("apply_phase",phase);work.put("applied_hash",hash);work.put("apply_error",error);
    }

    /** Full snapshots are required by the existing trash recovery mapper, also with regular history disabled. */
    static void documentHistory(Connection connection,Map<String,Object> values,boolean deleting) throws SQLException {
        int docId=number(values,"doc_id");
        try(PreparedStatement statement=connection.prepareStatement("UPDATE documents_history SET actual=? WHERE doc_id=? AND actual=?")) {
            statement.setBoolean(1,false);statement.setInt(2,docId);statement.setBoolean(3,true);statement.executeUpdate();
        }
        Map<String,Object> history=matchingColumns(connection,"documents_history",values,Set.of("history_id"));
        history.put("doc_id",docId);history.put("save_date",new Timestamp(System.currentTimeMillis()));history.put("actual",true);
        history.put("publicable",false);history.put("approved_by",0);history.put("disapproved_by",-1);history.put("awaiting_approve",null);
        history.put("is_delete",deleting);BranchStore.insert(connection,"documents_history",history);
    }

    static void groupHistory(Connection connection,Map<String,Object> values,int userId) throws SQLException {
        Map<String,Object> history=matchingColumns(connection,"groups_scheduler",values,Set.of("schedule_id"));
        history.put("save_date",new Timestamp(System.currentTimeMillis()));history.put("user_id",userId);history.put("when_to_publish",null);
        BranchStore.insert(connection,"groups_scheduler",history);
    }

    private static Map<String,Object> matchingColumns(Connection connection,String table,Map<String,Object> values,Set<String> excluded) throws SQLException {
        Map<String,Object> matching=new LinkedHashMap<>();
        try(PreparedStatement statement=connection.prepareStatement("SELECT * FROM `"+table+"` WHERE 1=0");ResultSet result=statement.executeQuery()) {
            for(int i=1;i<=result.getMetaData().getColumnCount();i++) {
                String column=result.getMetaData().getColumnName(i);
                if(!excluded.contains(column)&&values.containsKey(column))matching.put(column,values.get(column));
            }
        }
        return matching;
    }

    static int number(Map<String,?> row,String column) {Object value=row.get(column);return value==null?0:((Number)value).intValue();}
}
