package sk.iway.iwcm.branch;

import java.sql.*;
import java.util.*;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.spirit.model.*;

/** Detached document media over the same whole-object selection used by pages and files. */
public final class BranchMedia {
    private BranchMedia() { }
    public static List<Media> list(int owner,List<MediaGroupBean> categories) {
        BranchView view=BranchView.current();
        Map<Integer,Map<String,Object>> rows=new LinkedHashMap<>();
        try(Connection connection=DBPool.getConnection()) {
            Map<Integer,MediaGroupBean> definitions=new LinkedHashMap<>();
            try(PreparedStatement statement=connection.prepareStatement("SELECT media_group_id,media_group_name,available_groups,related_pages FROM media_groups");ResultSet result=statement.executeQuery()) {
                while(result.next()) {MediaGroupBean group=new MediaGroupBean();group.setMediaGroupId(result.getInt(1));group.setMediaGroupName(result.getString(2));group.setAvailableGroups(result.getString(3));group.setRelatedPages(result.getString(4));definitions.put(group.getMediaGroupId(),group);}
            }
            try(PreparedStatement statement=connection.prepareStatement("SELECT media_id FROM media WHERE media_fk_table_name='documents'")) {
                try(ResultSet result=statement.executeQuery()) {while(result.next()) {
                    int id=result.getInt(1);rows.put(id,BranchStore.readLive(connection,BranchType.MEDIA,id).orElseThrow());
                }}
            }
            for(Map<String,Object> work:view.workRows(BranchType.MEDIA)) {
                int id=number(work,"media_id");Map<String,Object> live=rows.get(id);
                BranchSnapshot snapshot=live==null?BranchSnapshot.absent():BranchStore.snapshot(BranchType.MEDIA,live);
                if(!view.selectWork(BranchType.MEDIA,work,snapshot)) continue;
                if("DELETE".equals(work.get("operation"))) {rows.remove(id);continue;}
                DocDetails page=view.document(number(work,"media_fk_id"),true);
                if(page==null) {rows.remove(id);continue;}
                Map<String,Object> projected=new LinkedHashMap<>(BranchMediaMapper.project(work,live,BranchMediaMapper.initialValues(id,page.getDocId(),number(work,"domain_id")),java.time.Instant.ofEpochMilli(page.getDateCreated())));
                projected.put("group_ids_snapshot",work.get("group_ids_snapshot"));rows.put(id,projected);
            }
            List<Media> result=new ArrayList<>();
            for(Map<String,Object> row:rows.values()) {
                int pageId=number(row,"media_fk_id");
                if(owner>0&&pageId!=owner) continue;
                if(view.document(pageId,true)==null) continue;
                List<Integer> ids=BranchFields.normalizedIds(row.get("group_ids_snapshot"));
                if(categories!=null&&!categories.isEmpty()&&categories.stream().noneMatch(group->ids.contains(group.getMediaGroupId())))continue;
                result.add(bean(row,ids,definitions));
            }
            result.sort(Comparator.comparing(Media::getMediaSortOrder).thenComparing(Media::getMediaId));return result;
        } catch(SQLException ex) {throw new IllegalStateException("Cannot read branch media",ex);}
    }
    public static Media one(int id) {return list(-1,null).stream().filter(media->media.getMediaId()==id).findFirst().orElse(null);}
    private static int number(Map<String,?> row,String column){Object value=row.get(column);return value instanceof Number n?n.intValue():0;}
    private static String text(Map<String,?> row,String column){return (String)row.get(column);}
    private static Media bean(Map<String,Object> row,List<Integer> ids,Map<Integer,MediaGroupBean> definitions) {
        Media media=new Media();media.setMediaId(number(row,"media_id"));media.setMediaFkId(number(row,"media_fk_id"));media.setDomainId(number(row,"domain_id"));
        media.setMediaFkTableName(text(row,"media_fk_table_name"));media.setMediaLink(text(row,"media_link"));media.setMediaThumbLink(text(row,"media_thumb_link"));media.setMediaSortOrder(number(row,"media_sort_order"));media.setMediaGroup(text(row,"media_group"));
        media.setMediaTitleSk(text(row,"media_title_sk"));media.setMediaTitleCz(text(row,"media_title_cz"));media.setMediaTitleEn(text(row,"media_title_en"));media.setMediaTitleDe(text(row,"media_title_de"));
        media.setMediaInfoSk(text(row,"media_info_sk"));media.setMediaInfoCz(text(row,"media_info_cz"));media.setMediaInfoEn(text(row,"media_info_en"));media.setMediaInfoDe(text(row,"media_info_de"));
        media.setFieldA(text(row,"field_a"));media.setFieldB(text(row,"field_b"));media.setFieldC(text(row,"field_c"));media.setFieldD(text(row,"field_d"));media.setFieldE(text(row,"field_e"));media.setFieldF(text(row,"field_f"));
        media.setLastUpdate((java.util.Date)row.get("last_update"));
        List<MediaGroupBean> groups=new ArrayList<>();for(int id:ids){MediaGroupBean group=definitions.get(id);if(group==null)throw new IllegalStateException("Missing media category: "+id);groups.add(group);}media.setGroups(groups);
        return media;
    }
}
