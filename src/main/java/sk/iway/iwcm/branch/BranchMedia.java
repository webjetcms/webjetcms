package sk.iway.iwcm.branch;

import java.util.*;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.media.MediaGroupRepository;
import sk.iway.iwcm.components.media.MediaRepository;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.spirit.model.*;

/** Detached document media over the same whole-object selection used by pages and files. */
public final class BranchMedia {
    private final BranchView view;
    private final Map<Integer,Map<String,Object>> details=new HashMap<>();
    private Map<Integer,MediaGroupBean> definitions;

    BranchMedia(BranchView view) { this.view=view; }

    public static List<MediaGroupBean> categories(int folder) {
        List<MediaGroupBean> groups=sk.iway.spirit.MediaDB.filterDomainId(new ArrayList<>(BranchView.current().media().definitions().values()),true);
        if(folder<1) return groups;
        Set<Integer> ancestors=new HashSet<>();
        for(sk.iway.iwcm.doc.GroupDetails group=BranchView.current().group(folder);group!=null&&ancestors.add(group.getGroupId());group=BranchView.current().group(group.getParentGroupId())) { }
        return groups.stream().filter(group->group.getAvailableGroups()==null||group.getAvailableGroups().isBlank()||BranchFields.normalizedIds(group.getAvailableGroups()).stream().anyMatch(ancestors::contains)).toList();
    }
    private Map<Integer,MediaGroupBean> definitions() {
        if(definitions==null) {
            definitions=new LinkedHashMap<>();
            for(MediaGroupBean group:Tools.getSpringBean("mediaGroupRepository",MediaGroupRepository.class).findAllByOrderByIdAsc())
                definitions.put(group.getMediaGroupId(),group);
        }
        return definitions;
    }
    public static List<Media> list(int owner,List<MediaGroupBean> categories) {
        BranchMedia reader=BranchView.current().media();
        return reader.read(owner,0,categories).stream().map(reader::bean).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    /** Reuses the displayed record for permission checks within the same verified view. */
    public static Media one(int id) {
        if(id<1) return null;
        BranchMedia reader=BranchView.current().media();
        if(!reader.details.containsKey(id)) reader.details.put(id,reader.read(-1,id,null).stream().findFirst().orElse(null));
        Map<String,Object> row=reader.details.get(id);
        return row==null?null:reader.bean(row);
    }

    private List<Map<String,Object>> read(int owner,int mediaId,List<MediaGroupBean> categories) {
        Map<Integer,Map<String,Object>> rows=new LinkedHashMap<>();
        MediaRepository repository=Tools.getSpringBean("mediaRepository",MediaRepository.class);
        int domainId=view.context().domainId();
        List<Media> liveMedia;
        if(mediaId>0) liveMedia=repository.findFirstByIdAndDomainId((long)mediaId,domainId).stream().filter(media->"documents".equals(media.getMediaFkTableName())).toList();
        else if(owner>0) liveMedia=repository.findAllByMediaFkIdAndMediaFkTableNameAndDomainId(owner,"documents",domainId);
        else liveMedia=repository.findAllByMediaFkTableNameAndDomainId("documents",domainId);
        for(Media media:liveMedia) rows.put(media.getMediaId(),row(media));
        for(Map<String,Object> work:view.workRows(BranchType.MEDIA)) {
            int id=number(work,"media_id");
            if(mediaId>0&&id!=mediaId) continue;
            if(owner>0&&number(work,"media_fk_id")!=owner&&!rows.containsKey(id)) continue;
            Map<String,Object> live=rows.get(id);
            // Include a moved-in record's actual LIVE owner when resolving whole-object conflicts.
            if(owner>0&&live==null) live=repository.findFirstByIdAndDomainId((long)id,domainId)
                .filter(media->"documents".equals(media.getMediaFkTableName())).map(BranchMedia::row).orElse(null);
            BranchSnapshot snapshot=live==null?BranchSnapshot.absent():BranchStore.snapshot(BranchType.MEDIA,live);
            if(!view.selectWork(BranchType.MEDIA,work,snapshot)) continue;
            if("DELETE".equals(work.get("operation"))) {rows.remove(id);continue;}
            DocDetails page=view.document(number(work,"media_fk_id"),true);
            if(page==null) {rows.remove(id);continue;}
            // A no-op must retain LIVE's unpublished-media cutoff.
            if(live!=null && BranchStore.snapshot(BranchType.MEDIA,work).equals(snapshot)) continue;
            Map<String,Object> projected=new LinkedHashMap<>(BranchMediaMapper.project(work,live,BranchMediaMapper.initialValues(id,page.getDocId(),number(work,"domain_id")),java.time.Instant.ofEpochMilli(page.getDateCreated())));
            projected.put("group_ids_snapshot",work.get("group_ids_snapshot"));rows.put(id,projected);
        }
        List<Map<String,Object>> result=new ArrayList<>();
        Map<Integer,DocDetails> owners=new HashMap<>();
        for(Map<String,Object> row:rows.values()) {
            int pageId=number(row,"media_fk_id");
            if(owner>0&&pageId!=owner) continue;
            if(!owners.containsKey(pageId)) owners.put(pageId,view.document(pageId,true));
            if(owners.get(pageId)==null) continue;
            List<Integer> ids=BranchFields.normalizedIds(row.get("group_ids_snapshot"));
            if(categories!=null&&!categories.isEmpty()&&categories.stream().noneMatch(group->ids.contains(group.getMediaGroupId())))continue;
            result.add(row);
        }
        result.sort(Comparator.comparingInt((Map<String,Object> row)->number(row,"media_sort_order")).thenComparingInt(row->number(row,"media_id")));return result;
    }
    private Media bean(Map<String,Object> row) { return bean(row,BranchFields.normalizedIds(row.get("group_ids_snapshot")),definitions()); }
    private static Map<String,Object> row(Media media) {
        Map<String,Object> row=BranchEditorRows.media(media,BranchMediaMapper.initialValues(media.getMediaId(),media.getMediaFkId(),media.getDomainId()));
        row.put("last_update",media.getLastUpdate());
        row.put("group_ids_snapshot",media.getGroups().stream().map(MediaGroupBean::getMediaGroupId).toList());
        return row;
    }
    private static int number(Map<String,?> row,String column){Object value=row.get(column);return value instanceof Number n?n.intValue():0;}
    private static String text(Map<String,?> row,String column){return (String)row.get(column);}
    private static Media bean(Map<String,Object> row,List<Integer> ids,Map<Integer,MediaGroupBean> definitions) {
        Media media=new Media();media.setMediaId(number(row,"media_id"));media.setMediaFkId(number(row,"media_fk_id"));media.setDomainId(number(row,"domain_id"));
        media.setMediaFkTableName(text(row,"media_fk_table_name"));media.setMediaLink(text(row,"media_link"));media.setMediaThumbLink(text(row,"media_thumb_link"));media.setMediaSortOrder(number(row,"media_sort_order"));media.setMediaGroup(text(row,"media_group"));
        media.setMediaTitleSk(text(row,"media_title_sk"));media.setMediaTitleCz(text(row,"media_title_cz"));media.setMediaTitleEn(text(row,"media_title_en"));media.setMediaTitleDe(text(row,"media_title_de"));
        media.setMediaInfoSk(text(row,"media_info_sk"));media.setMediaInfoCz(text(row,"media_info_cz"));media.setMediaInfoEn(text(row,"media_info_en"));media.setMediaInfoDe(text(row,"media_info_de"));
        media.setFieldA(text(row,"field_a"));media.setFieldB(text(row,"field_b"));media.setFieldC(text(row,"field_c"));media.setFieldD(text(row,"field_d"));media.setFieldE(text(row,"field_e"));media.setFieldF(text(row,"field_f"));
        media.setLastUpdate((java.util.Date)row.get("last_update"));
        if(BranchContexts.current().mode()==BranchContext.Mode.ADMIN) {
            MediaEditorFields fields=new MediaEditorFields();
            fields.setBranchHash(BranchMediaMapper.snapshot(row,ids).hash());
            media.setEditorFields(fields);
        }
        List<MediaGroupBean> groups=new ArrayList<>();for(int id:ids){MediaGroupBean group=definitions.get(id);if(group==null)throw new IllegalStateException("Missing media category: "+id);groups.add(group);}media.setGroups(groups);
        return media;
    }
}
