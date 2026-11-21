package sk.iway.iwcm.branch;

import java.sql.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import sk.iway.iwcm.*;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.editor.rest.GetAllItemsDocOptions;
import sk.iway.iwcm.system.datatable.DatatableRestControllerV2;

/** Small allowlisted DataTable query over the effective relation, before count and paging. */
public final class BranchDocumentQuery {
    private BranchDocumentQuery() { }
    private static final Map<String,String> COLUMNS=new LinkedHashMap<>();
    static {
        for(String column:BranchFields.DOCUMENT.columns()) {
            StringBuilder name=new StringBuilder();boolean upper=false;
            for(char c:column.toCharArray()) {if(c=='_')upper=true;else {name.append(upper?Character.toUpperCase(c):c);upper=false;}}
            COLUMNS.put(name.toString(),column);
        }
        COLUMNS.put("publishStartDate","publish_start");COLUMNS.put("publishEndDate","publish_end");COLUMNS.put("eventDateDate","event_date");
        COLUMNS.put("id","doc_id");COLUMNS.put("authorName","@author");
    }
    public static Page<DocDetails> list(GetAllItemsDocOptions options) {
        if(options.getGroupId()<0||options.isUserGroupIdRequested()||options.isTempIdRequested()||"true".equals(options.getRequestPrameter("auditVersion")))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"This special list is available only in production");
        BranchView view=BranchView.current();
        Set<Integer> groups=new LinkedHashSet<>();groups.add(options.getGroupId());
        Set<Integer> recursiveRoots=new LinkedHashSet<>();
        String requestedRoots=options.getRequestPrameter("groupIdList");
        if(requestedRoots!=null&&!requestedRoots.isBlank()) {
            groups.clear();
            for(String token:requestedRoots.split(",")) {
                boolean recursive=token.strip().endsWith("*");int id=Tools.getIntValue(token.strip().replace("*",""),-1);
                if(id>0) {groups.add(id);if(recursive)recursiveRoots.add(id);}
            }
        }
        if(options.isRecursiveSubfolders())recursiveRoots.addAll(groups);
        if(!recursiveRoots.isEmpty()) {
            boolean added;
            do {added=false;for(GroupDetails group:view.groups()) if(recursiveRoots.contains(group.getParentGroupId())) {groups.add(group.getGroupId());added|=recursiveRoots.add(group.getGroupId());}}while(added);
        }
        groups.removeIf(id->view.group(id)==null||(!GroupsDB.isGroupEditable(options.getCurrentUser(),id)&&!GroupsDB.isGroupViewable(options.getCurrentUser(),id)));
        if(groups.isEmpty()) return Page.empty(options.getPageable());
        List<Object> arguments=new ArrayList<>(groups);
        StringBuilder where=new StringBuilder(" WHERE d.group_id IN ("+String.join(",",Collections.nCopies(groups.size(),"?"))+")");
        List<Integer> editableGroups=groups.stream().filter(id->GroupsDB.isGroupEditable(options.getCurrentUser(),id)).toList();
        List<Integer> editablePages=BranchFields.normalizedIds(options.getCurrentUser().getEditablePages());
        if(editableGroups.isEmpty()&&editablePages.isEmpty())return Page.empty(options.getPageable());
        where.append(" AND (");
        if(!editableGroups.isEmpty()) {where.append("d.group_id IN (").append(String.join(",",Collections.nCopies(editableGroups.size(),"?"))).append(')');arguments.addAll(editableGroups);}
        if(!editablePages.isEmpty()) {if(!editableGroups.isEmpty())where.append(" OR ");where.append("d.doc_id IN (").append(String.join(",",Collections.nCopies(editablePages.size(),"?"))).append(')');arguments.addAll(editablePages);}
        where.append(')');
        Map<String,String> params=DatatableRestControllerV2.getParamsMap(options.getRequest());
        String status=params.remove("searchEditorFields.statusIcons");
        if(status!=null&&!status.isBlank()) {
            if("searchDefaultPage".equals(status))params.put("searchDefaultPage","true");
            else {
                String[] parts=status.split(":",2);
                String column=parts.length==2?COLUMNS.get(parts[0]):null;
                if(column==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported status filter");
                if("notEmpty".equals(parts[1]))where.append(" AND COALESCE(d.`").append(column).append("`,'')<>''");
                else if("virtual_path".equals(column)) {where.append(" AND d.virtual_path ").append(parts[1].startsWith("!")?"NOT LIKE ?":"LIKE ?");arguments.add(parts[1].startsWith("!")?parts[1].substring(1):parts[1]);}
                else params.put("search"+parts[0],parts[1]);
            }
        }
        for(var entry:params.entrySet()) {
            String raw=entry.getKey(),value=entry.getValue();if(value==null||value.isBlank())continue;
            if(!raw.startsWith("search"))continue;
            String property=DatatableRestControllerV2.getCleanKey(raw);
            if("perexGroups".equals(property)||"editorFields.permisions".equals(property)||"editorFields.emails".equals(property)) {
                List<Integer> ids=BranchFields.normalizedIds(value);
                if(!ids.isEmpty()) {
                    where.append(" AND (");boolean first=true;
                    for(int id:ids) {if(!first)where.append(" OR ");first=false;where.append("CONCAT(',',COALESCE(d.").append("perexGroups".equals(property)?"perex_group":"password_protected").append(",''),',') LIKE ?");arguments.add("%,"+id+",%");}
                    where.append(')');
                }
                continue;
            }
            if("defaultPage".equals(property)) {
                List<Integer> ids=view.groups().stream().map(GroupDetails::getDefaultDocId).filter(id->id>0).toList();
                if(!ids.isEmpty()) {where.append(" AND d.doc_id ").append("false".equals(value)?"NOT IN (":"IN (").append(String.join(",",Collections.nCopies(ids.size(),"?"))).append(")");arguments.addAll(ids);}
                else if(!"false".equals(value))where.append(" AND 1=0");continue;
            }
            String column=COLUMNS.get(property);
            if(column==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported branch filter: "+property);
            String expression=expression(column);
            if(value.startsWith("range:")||value.startsWith("daterange:")) {
                boolean date=value.startsWith("daterange:");String range=value.substring(value.indexOf(':')+1);String[] edges=range.split("-",-1);
                if(edges.length>2)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid range");
                if(!edges[0].isEmpty()) {where.append(" AND ").append(expression).append(">=?");arguments.add(date?new Timestamp(Long.parseLong(edges[0])):new java.math.BigDecimal(edges[0]));}
                if(edges.length==2&&!edges[1].isEmpty()) {where.append(" AND ").append(expression).append(date?"<?":"<=?");arguments.add(date?new Timestamp(Long.parseLong(edges[1])):new java.math.BigDecimal(edges[1]));}
            } else if("null".equals(value)) where.append(" AND ").append(expression).append(" IS NULL");
            else if(Set.of("available","searchable","cacheable","show_in_menu","require_ssl","show_in_navbar","show_in_sitemap","logged_show_in_menu","logged_show_in_navbar","logged_show_in_sitemap","url_inherit_group","generate_url_from_title","disable_after_end","publish_after_start").contains(column)) {where.append(" AND ").append(expression).append("=?");arguments.add(Boolean.parseBoolean(value));}
            else if(column.endsWith("_id")||Set.of("sort_priority","views_total","forum_count").contains(column)) {where.append(" AND ").append(expression).append("=?");arguments.add(Long.parseLong(value));}
            else {
                where.append(" AND ").append(expression).append(value.startsWith("^")&&value.endsWith("$")?"=?":" LIKE ?");
                arguments.add(value.startsWith("^")&&value.endsWith("$")?DatatableRestControllerV2.getCleanValue(value):(value.startsWith("^")?value.substring(1)+"%":value.endsWith("$")?"%"+value.substring(0,value.length()-1):"%"+value+"%"));
            }
        }
        StringBuilder order=new StringBuilder();
        for(Sort.Order item:options.getPageable().getSort()) {
            String column=COLUMNS.get(item.getProperty());if(column==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported branch sorting");
            order.append(expression(column)).append(' ').append(item.isAscending()?"ASC":"DESC").append(',');
        }
        order.append("d.doc_id ASC");
        BranchSql.Fragment source=BranchSql.documentSource("d");
        String from=source.sql()+" LEFT JOIN users u ON u.user_id=d.author_id";
        try(Connection connection=DBPool.getConnection()) {
            long count;
            try(PreparedStatement statement=connection.prepareStatement("SELECT COUNT(*) FROM "+from+where)) {
                bind(statement,source,arguments);try(ResultSet result=statement.executeQuery()){result.next();count=result.getLong(1);}
            }
            List<DocDetails> docs=new ArrayList<>();
            try(PreparedStatement statement=connection.prepareStatement("SELECT d.doc_id FROM "+from+where+" ORDER BY "+order+" LIMIT ? OFFSET ?")) {
                int index=bind(statement,source,arguments);statement.setInt(index++,options.getPageable().getPageSize());statement.setLong(index,options.getPageable().getOffset());
                try(ResultSet result=statement.executeQuery()){while(result.next())docs.add(view.document(result.getInt(1),true));}
            }
            return new PageImpl<>(docs,options.getPageable(),count);
        } catch(SQLException ex) {throw new IllegalStateException("Cannot query branch pages",ex);}
    }
    private static String expression(String column) {return "@author".equals(column)?"CONCAT(COALESCE(u.title,''),' ',COALESCE(u.first_name,''),' ',COALESCE(u.last_name,''))":"d.`"+column+"`";}
    private static int bind(PreparedStatement statement,BranchSql.Fragment source,List<Object> arguments)throws SQLException {int index=source.bind(statement,1);for(Object value:arguments)statement.setObject(index++,value);return index;}
}
