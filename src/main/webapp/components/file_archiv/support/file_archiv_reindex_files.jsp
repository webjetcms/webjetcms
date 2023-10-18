<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, java.sql.*,java.io.*" %>
<%@ page import="sk.iway.iwcm.common.FileIndexerTools" %>
<%@ page import="sk.iway.iwcm.users.UserDetails" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="sk.iway.iwcm.common.CloudToolsForCore" %>
<%@ page import="sk.iway.iwcm.io.IwcmFile" %>
<%@ page import="sk.iway.iwcm.doc.GroupsDB" %>
<%@ page import="sk.iway.iwcm.doc.GroupDetails" %>
<%@ page import="java.util.StringTokenizer" %>
<%@ page import="sk.iway.iwcm.doc.DocDB" %>
<%@ page import="sk.iway.iwcm.doc.DocDetails" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<iwcm:checkLogon admin="true" perms="users.edit_admins"/>
<%@ include file="/admin/layout_top.jsp" %>
<%
    if(request.getParameter("indexuj") != null)
    {
        String typeOfIndexing = Tools.getParameter(request, "typeOfIndexing");
        String faDir = Tools.getRequestParameter(request, "faDir");
        if(Tools.isNotEmpty(faDir))
        {
            if(faDir.startsWith("/")) faDir = faDir.substring(1);
            if(faDir.endsWith("/") == false) faDir += "/";
        }

        out.println(Tools.formatDateTimeSeconds(Tools.getNow())+" ZACINAM indexovat subory pre adresar "+request.getParameter("faDir")+"<br/>");
        out.flush();

        if(Tools.isEmpty(faDir) || faDir.startsWith("files/archiv/") == false)
        {
            out.println("CHYBA: Adresar musi zacinat na files/archiv/<br/>");
            return;
        }

        out.println("Mazem zaindexovany adresar<br/>");
        out.flush();

        StringTokenizer st = new StringTokenizer(faDir, "/");
        int parentDirId = CloudToolsForCore.getDomainId();
        String dirName;
        GroupsDB groupsDB = GroupsDB.getInstance();
        GroupDetails groupToRemove = null;
        while (st.hasMoreTokens())
        {
            dirName = st.nextToken();
            groupToRemove = FileIndexerTools.findGroup(groupsDB.getGroupsAll(), parentDirId, dirName);
            if (groupToRemove == null)
                break;
            parentDirId = groupToRemove.getGroupId();
        }
        if(groupToRemove == null)
        {
            out.println("WARN: WebJET adresar som nenasiel<br/>");
            return;
        }
        else
        {
            //zmazem iba stranky v danom adresari
            if("only".equals(typeOfIndexing))
            {
                for(DocDetails dd : DocDB.getInstance().getDocByGroup(groupToRemove.getGroupId()))
                    DocDB.deleteDoc(dd.getDocId(), request);
            }
            //zmazem rekurzovne vsetko
            else if("all".equals(typeOfIndexing))
                GroupsDB.deleteGroup(groupToRemove.getGroupId(), request, true, true);
        }

        Connection db_conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try
        {
            db_conn = DBPool.getConnection();
            String sql = "";
            String countSql = "";
            boolean onlyMainFiles = Constants.getBoolean("fileArchivIndexOnlyMainFiles");
            if("only".equals(typeOfIndexing))
            {
                sql = "SELECT FILE_PATH, FILE_NAME FROM FILE_ARCHIV WHERE FILE_PATH = '" + faDir + "'"+(onlyMainFiles ? " AND reference_id = -1 AND uploaded = -1 " : "")+
                        CloudToolsForCore.getDomainIdSqlWhere(true);
                countSql = "SELECT COUNT(*) FROM FILE_ARCHIV WHERE FILE_PATH = '" + faDir +  "'"+(onlyMainFiles ? " AND reference_id = -1 AND uploaded = -1 " : "")+
                        CloudToolsForCore.getDomainIdSqlWhere(true);
            }
            else if("all".equals(typeOfIndexing))
            {
                sql = "SELECT FILE_PATH, FILE_NAME FROM FILE_ARCHIV WHERE FILE_PATH LIKE '" + faDir + "%'"+(onlyMainFiles ? " AND reference_id = -1 AND uploaded = -1 " : "")+
                        CloudToolsForCore.getDomainIdSqlWhere(true);
                countSql = "SELECT COUNT(*) FROM FILE_ARCHIV WHERE FILE_PATH LIKE '" + faDir + "%'"+(onlyMainFiles ? " AND reference_id = -1 AND uploaded = -1 " : "")+
                        CloudToolsForCore.getDomainIdSqlWhere(true);
            }

            if(Tools.isEmpty(sql))
                return;

            int celkovyPocet = DB.queryForInt(countSql);

            out.println(" SQL: "+sql+"<br/>");
            out.flush();

            ps = db_conn.prepareStatement(sql);
            rs = ps.executeQuery();
            String filePath, fileName;
            UserDetails ud = UsersDB.getCurrentUser(session);
            if(ud == null || ud.getUserId() < 1)
            {
                out.println("CHYBA: Pouzivatel je null<br/>");
                return;
            }
            int i = 0;
            while (rs.next())
            {
                filePath = DB.getDbString(rs, "FILE_PATH");
                fileName = DB.getDbString(rs, "FILE_NAME");
                if (Tools.isAnyNotEmpty(filePath, fileName) && new IwcmFile(Tools.getRealPath("/" + filePath + fileName)).exists())
                    FileIndexerTools.indexFile("/" + filePath + fileName, ud);
                if(++i % 20 == 0)
                {
                    out.println(i+"/"+celkovyPocet+" zaindexovanych suborov<br/>");
                    out.flush();
                }
            }
            out.println(Tools.formatDateTimeSeconds(Tools.getNow())+" KONIEC indexovania, zaindexovanych "+i+"/"+celkovyPocet+" suborov<br/>");
            out.flush();
        } catch (Exception ex)
        {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            try
            {
                out.println("ERROR: " + sw.toString().replaceAll("\\n", "<br/>"));
                return;
            } catch (Exception ignore)
            {
            }
            sk.iway.iwcm.Logger.error(ex);
        } finally
        {
            try
            {
                if (rs != null)
                    rs.close();
                if (ps != null)
                    ps.close();
                if (db_conn != null)
                    db_conn.close();
            } catch (Exception ignore)
            {
            }
        }
    }
    else
    {
%>
<p>Preindexuje subory z File Archivu pre domenu <strong><%=CloudToolsForCore.getDomainName()%></strong></p>
<form action="<%=PathFilter.getOrigPath(request)%>" method="post">
    <label for="faDir">Adresar</label>
    <input type="text" name="faDir" id="faDir" value="files/archiv/" size="50"/><br/>
    <label for="typeOfIndexing">Typ idexovania</label>
    <select id="typeOfIndexing" name="typeOfIndexing">
        <option value="only" selected>indexujem LEN zadany adresar</option>
        <option value="all">indexujem VSETKO co zacina prefixom adresara</option>
    </select><br/>
    <input type="submit" name="indexuj" value="Spustit" />
</form>
<%
    }
%>
<%@ include file="/admin/layout_bottom.jsp" %>