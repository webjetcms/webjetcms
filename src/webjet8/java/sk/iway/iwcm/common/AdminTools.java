package sk.iway.iwcm.common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.lucene.document.Field;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.sync.FileBean;
import sk.iway.iwcm.system.elfinder.IwcmFsVolume;
import sk.iway.iwcm.system.fulltext.lucene.LuceneUtils;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.users.UsersDB;

public class AdminTools {

    private AdminTools() {

    }

    public static int createWebPage(GroupDetails group, Identity user, HttpServletRequest request, String title)
    {
        EditorForm ef;
        if (group.getDefaultDocId()>0 && title==null) ef = EditorDB.getEditorForm(request, group.getDefaultDocId(), -1, group.getGroupId());
        else ef = EditorDB.getEditorForm(request, title, group.getGroupId());

        if (Tools.isEmpty(title)) title = group.getGroupName();

        ef.setTitle(title);
        ef.setNavbar(title);
        ef.setAuthorId(user.getUserId());
        ef.setPublish("1");

        EditorDB.saveEditorForm(ef, request);
        EditorDB.cleanSessionData(request);

        return ef.getDocId();
    }

    /**
     * Vrati zoznam naposledy editovanych stranok podla tabulky documents_history. Vynecha stranky ktorych virtual_path zacina na /files/ co su zaindexovane subory
     * Prenesene z GroupsListAction koli kontrole na cloud WJ
     * @param userId
     * @param maxSize
     * @return
     */
    public static List<DocDetails> getMyRecentPages(int userId, int maxSize)
    {
        Identity user = new Identity(UsersDB.getUser(userId));
        return getMyRecentPages(user, maxSize);
    }

    /**
     * Vrati zoznam naposledy editovanych stranok podla tabulky documents_history. Vynecha stranky ktorych virtual_path zacina na /files/ co su zaindexovane subory
     * Prenesene z GroupsListAction koli kontrole na cloud WJ
     * @param user
     * @param maxSize
     * @return
     */
    public static List<DocDetails> getMyRecentPages(Identity user, int maxSize)
    {
        int userId = user.getUserId();
        List<DocDetails> pages = new ArrayList<>();

        DocDB docDB = DocDB.getInstance();

        Connection db_conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            //String sql = "SELECT d.* FROM documents d WHERE d.author_id=? AND virtual_path not like '/files/%' ORDER BY d.date_created DESC, d.title ASC";
            //vymenovane su tam kvoli rychlosti nacitania na welcome obrazovku
            String sql = "SELECT doc_id, group_id, title, virtual_path, save_date, available, password_protected, publish_start, publish_end, external_link, sort_priority FROM documents_history WHERE author_id=? AND (virtual_path is null OR virtual_path not like '/files/%') ORDER BY save_date DESC, title ASC";

            db_conn = DBPool.getConnection();
            if (Constants.DB_TYPE == Constants.DB_MYSQL) {
                ps = db_conn.prepareStatement(sql+" LIMIT "+(maxSize*3));
            } else {
                ps = db_conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                ps.setFetchSize(maxSize * 3);
            }
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            DocDetails doc;
            GroupsDB groupsDB = GroupsDB.getInstance();
            boolean linkTypeHtml = false;
            if (Constants.getInt("linkType") == Constants.LINK_TYPE_HTML)
            {
                linkTypeHtml = true;
            }
            int counter = 0;
            Map<Integer, Integer> allreadyHasTable = new Hashtable<>();

            Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
            String trashDirName = propSystem.getText("config.trash_dir");

            while (rs.next())
            {
                int docId = rs.getInt("doc_id");

                //test na zmazanie stranky
                DocDetails actualDoc = docDB.getBasicDocDetails(docId, false);
                if (actualDoc == null) continue;

                if (allreadyHasTable.get(Integer.valueOf(docId))!=null) continue;

                GroupDetails group = actualDoc.getGroup();
                //test na stranky v kosi / schovane adresare
                if(group==null || group.getFullPath().startsWith(trashDirName) || group.isHiddenInAdmin())
                    continue;

                counter++;

                if (counter > maxSize) break;

                doc = new DocDetails();
                //DocDB.getDocDetails(rs, doc, true, true);
                doc.setDocId(docId);
                doc.setGroupId(rs.getInt("group_id"));
                doc.setTitle(DB.getDbString(rs, "title"));
                doc.setVirtualPath(DB.getDbString(rs, "virtual_path"));
                doc.setDateCreated(rs.getTimestamp("save_date").getTime());
                doc.setAuthorId(userId);
                doc.setAuthorName(user.getFullName());
                doc.setAuthorEmail(user.getEmail());

                Logger.debug(DocDB.class, "testing recent page: "+docId+" "+doc.getTitle());

                if (linkTypeHtml)
                {
                    doc.setDocLink(DocDB.getURL(doc, groupsDB));
                }
                doc.setNavbar(groupsDB.getNavbarNoHref(doc.getGroupId()));
                doc.setAvailable(rs.getBoolean("available"));

                doc.setPasswordProtected(DB.getDbString(rs, "password_protected"));
                doc.setPublishStart(DB.getDbTimestamp(rs, "publish_start"));
                doc.setPublishEnd(DB.getDbTimestamp(rs, "publish_end"));

                doc.setExternalLink(DB.getDbString(rs, "external_link"));

                doc.setSortPriority(rs.getInt("sort_priority"));

                //ak stranku nemozem editovat preskoc (napr. ak bola vytvorena programovo)
                if (EditorDB.isPageEditable(user, new EditorForm(doc))==false) continue;

                pages.add(doc);

                allreadyHasTable.put(Integer.valueOf(docId), Integer.valueOf(docId));
            }
            rs.close();
            ps.close();
            db_conn.close();
            rs = null;
            ps = null;
            db_conn = null;
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }
        finally
        {
            try
            {
                if (rs != null)
                    rs.close();
                if (ps != null)
                    ps.close();
                if (db_conn != null)
                    db_conn.close();
            }
            catch (Exception ex2)
            {
                sk.iway.iwcm.Logger.error(ex2);
            }
        }

        return(pages);
    }

    public static List<FileBean> getVersionList(String virtualPath)
    {
        List<FileBean> result=new ArrayList<>();
        Connection db_conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            db_conn = DBPool.getConnection();
            ps = db_conn.prepareStatement("select * from file_fat where virtual_path=? order by last_modified desc");
            ps.setString(1, virtualPath);
            rs = ps.executeQuery();
            while (rs.next())
            {
                FileBean bean=new FileBean();
                bean.setFileSize(rs.getLong("fsize"));
                bean.setLastModified(rs.getLong("last_Modified"));
                bean.setLocalLastModified(rs.getLong("file_fat_id"));
                bean.setFilePath(Boolean.toString(rs.getBoolean("is_deleted")));
                result.add(bean);
            }
            rs.close();
            ps.close();
            db_conn.close();
            rs = null;
            ps = null;
            db_conn = null;
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }
        finally
        {
            try
            {
                if (rs != null)
                    rs.close();
                if (ps != null)
                    ps.close();
                if (db_conn != null)
                    db_conn.close();
            }
            catch (Exception ex2)
            {
                sk.iway.iwcm.Logger.error(ex2);
            }
        }
        return result;
    }




    /**
     * Vrati zoznam naposledy upravenych stranok, zobrazi len take, na ktore ma pouzivatel prava
     * @return
     */
    public static List<DocDetails> getRecentPages(int size, Identity user) {
        return getRecentPages(size, -1, user);
    }

    /**
     * Vrati zoznam naposledy upravenych stranok, zobrazi len take, na ktore ma pouzivatel prava
     * @param size - maximalny pocet vratenych zaznamov (realne vrateny moze byt nizsii, kedze sa kontroluju prava)
     * @param filterDomainId - ID domeny pre ktoru sa maju stranky odfiltrovat, alebo -1 ak sa filtrovat nema
     * @param user
     * @return
     */
    public static List<DocDetails> getRecentPages(int size, int filterDomainId, Identity user)
    {
        Connection db_conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        Prop propSystem = Prop.getInstance(Constants.getString("defaultLanguage"));
        String trashDirName = propSystem.getText("config.trash_dir");

        List<DocDetails> pages = new ArrayList<>();
        StringBuilder DOCUMENT_FIELDS_NODATA = new StringBuilder("SELECT");

        if (Constants.DB_TYPE == Constants.DB_MSSQL) DOCUMENT_FIELDS_NODATA.append(" TOP (?)"); //BHR: otaznik musi byt v zatvorkach, inak to hlasilo java.sql.SQLException: Incorrect syntax near '@P0'.

        DOCUMENT_FIELDS_NODATA.append(" u.title as u_title, u.first_name, u.last_name, u.email, u.photo, "+DocDB.getDocumentFieldsNodata()+" FROM documents d LEFT JOIN users u ON d.author_id=u.user_id");
        String WHERE = " WHERE (d.virtual_path is null OR d.virtual_path not like '/files/%') AND d.available=1 ";
        if (InitServlet.isTypeCloud()) WHERE += " AND d.root_group_l1="+ CloudToolsForCore.getDomainId();
        else if (filterDomainId > 0) {
            //ziskaj zoznam ROOT adresarov v zadanej domene
            GroupsDB groupsDB = GroupsDB.getInstance();
            GroupDetails domainGroup = groupsDB.getGroup(filterDomainId);
            if (domainGroup != null && Tools.isNotEmpty(domainGroup.getDomainName())) {
                List<GroupDetails> rootGroups = groupsDB.getGroups(0);
                StringBuilder groupIds = new StringBuilder();
                for (GroupDetails root : rootGroups) {
                    if (root.getDomainName().equalsIgnoreCase(domainGroup.getDomainName())==false) continue;

                    if (groupIds.length()>0) groupIds.append(",");
                    groupIds.append(String.valueOf(root.getGroupId()));
                }
                if (groupIds.length()>0) {
                    WHERE += " AND d.root_group_l1 IN ("+groupIds.toString()+")";
                }

                //vyluc vsetky Kos adresare
                StringBuilder groupIdsTrash = new StringBuilder();
                for (GroupDetails group : groupsDB.getGroupsAll()) {
                    if (trashDirName.equals(group.getFullPath())==false) continue;

                    if (groupIdsTrash.length()>0) groupIdsTrash.append(",");
                    groupIdsTrash.append(String.valueOf(group.getGroupId()));
                }
                if (groupIdsTrash.length()>0) {
                    WHERE += " AND d.group_id NOT IN ("+groupIdsTrash.toString()+") AND d.root_group_l2 NOT IN ("+groupIdsTrash.toString()+") ";
                }
            }
        }

        String DOCUMENT_ORDER_BY = " ORDER BY d.date_created DESC";
        try
        {
            db_conn = DBPool.getConnection();

            StringBuilder sql = null;
            if (Constants.DB_TYPE == Constants.DB_MSSQL)
            {
                sql = new StringBuilder(DOCUMENT_FIELDS_NODATA);
                sql.append(WHERE);
                sql.append(DOCUMENT_ORDER_BY);
            }
            else if (Constants.DB_TYPE == Constants.DB_MYSQL)
            {
                sql = new StringBuilder(DOCUMENT_FIELDS_NODATA);
                sql.append(WHERE);
                sql.append(DOCUMENT_ORDER_BY);
                sql.append(" LIMIT ?");
            }
            else
            {
                //oracle
                sql = new StringBuilder(DOCUMENT_FIELDS_NODATA);
                sql.append(WHERE);
                sql.append(" AND rownum < ?");
                sql.append(DOCUMENT_ORDER_BY);
            }

            ps = db_conn.prepareStatement(sql.toString());
            ps.setInt(1, size*10);
            rs = ps.executeQuery();

            DocDB docDB = DocDB.getInstance();
            int counter = 0;

            while (rs.next())
            {
                int docId = rs.getInt("doc_id");
                DocDetails actualDoc = docDB.getBasicDocDetails(docId, false);
                if (actualDoc==null) continue;

                GroupDetails docGroup = actualDoc.getGroup();
                if (docGroup==null) continue;

                //test na stranky v kosi
                if(docGroup.getFullPath().startsWith(trashDirName))
                    continue;

                //test na schovany adresar
                if(docGroup.isHiddenInAdmin())
                    continue;

                counter++;
                if (counter > size) break;

                DocDetails doc = DocDB.getDocDetails(rs, true, false);

                if (user != null)
                {
                    if (EditorDB.isPageEditable(user, new EditorForm(doc)))
                    {
                        pages.add(doc);
                    }
                }
                else
                {
                    pages.add(doc);
                }
            }
            rs.close();
            ps.close();
            db_conn.close();
            rs = null;
            ps = null;
            db_conn = null;
        }
        catch (Exception ex)
        {
            sk.iway.iwcm.Logger.error(ex);
        }
        finally
        {
            try
            {
                if (rs != null)
                    rs.close();
                if (ps != null)
                    ps.close();
                if (db_conn != null)
                    db_conn.close();
            }
            catch (Exception ex2)
            {
                sk.iway.iwcm.Logger.error(ex2);
            }
        }

        return pages;
    }

    /**
     * Ak je pre domenu definovany alias suborov vrati /alias pre jednoduche vlozenie do cesty
     * @return
     */
    public static String getDomainNameFileAliasAppend()
    {
        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (rb != null)
        {
            String domain = rb.getDomain();
            String domainAlias = MultiDomainFilter.getDomainAlias(domain);
            if (Tools.isNotEmpty(domainAlias) && Tools.isEmpty(Constants.getString("cloudStaticFilesDir")))
            {
                Logger.debug(IwcmFsVolume.class, "getDomainNameFileAliasAppend, domain:"+domain+" return: /"+domainAlias);
                return "/"+domainAlias;
            }
        }
        return "";
    }


    public static Field titleField(String title){
        return new Field("title", LuceneUtils.nonNull(title), Field.Store.YES,
                Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS_OFFSETS);
    }

    public static String defaultLanguage()
    {
        return Tools.isNotEmpty(Constants.getString("defaultLanguage"))? Constants.getString("defaultLanguage"):"sk";
    }


    public static List<DocDetails> getRecentPages(int size) {
        return getRecentPages(size,null);
    }
}
