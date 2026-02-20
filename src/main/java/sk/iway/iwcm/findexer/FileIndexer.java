package sk.iway.iwcm.findexer;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.FileIndexerTools;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 *  FullText indexovanie suborov. Vytvari web stranku s obsahom textu daneho
 *  suboru a s presmerovanim na dany subor.
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      Utorok, 2004, január 20
 *@modified     $Date: 2004/01/25 13:31:09 $
 */
public class FileIndexer
{
	protected FileIndexer() {
		//utility class
	}

	/**
	 * Zaindexuje subor
	 * @param url - url suboru
	 * @param indexedFiles - zoznam do ktoreho sa zapise vysledok
	 * @param request
	 * @return
	 *
	 * @deprecated - use FileIndexerTools.indexFile
	 */
	@Deprecated
	public static boolean indexFile(String url, List<ResultBean> indexedFiles, HttpServletRequest request)
	{
		return FileIndexerTools.indexFile(url, indexedFiles, request);
	}


	/**
	 * Zaindexuje cely adresar
	 * @param dirUrl - url adresa
	 * @param indexedFiles - zoznam do ktoreho sa zapisu zaindexovane subory
	 * @param request
	 */
	public static void indexDir(String dirUrl, List<ResultBean> indexedFiles, HttpServletRequest request)
	{
		indexDir(dirUrl, indexedFiles, request, null);
	}

	public static void indexDir(String dirUrl, List<ResultBean> indexedFiles, HttpServletRequest request, PrintWriter out)
	{
		Logger.println(FileIndexer.class,"index dir: " + dirUrl);

		if (out != null)
		{
			out.println("<script type='text/javascript'>window.scrollBy(0, 1000);</script>");
		}

		disablePagesInDir(dirUrl);

		if (dirUrl.contains("backup")) {
			Logger.error(FileIndexer.class, dirUrl+" is BACKUP, skipping");
			return;
		}

		IwcmFile dir = new IwcmFile(Tools.getRealPath(dirUrl));
		IwcmFile[] files = dir.listFiles();
		IwcmFile f;
		int size = files.length;
		int i;

		DebugTimer dt = new DebugTimer("FileIndexer");

		for (i=0; i<size; i++)
		{
			f = files[i];

			Logger.debug(FileIndexer.class, "indexDir, i="+i+"/"+size+" f="+f.getAbsolutePath()+" dir="+f.isDirectory()+" canRead="+f.canRead());

			if (f.isDirectory())
			{
				//rekurzia
				indexDir(dirUrl + "/" + f.getName(), indexedFiles, request, out);
			}
			else if (f.canRead())
			{

				if (out != null)
				{
					out.println("<script type='text/javascript'>window.scrollBy(0, 1000);</script>");
					out.flush();
				}

				boolean indexed = FileIndexerTools.indexFile(dirUrl + "/" + f.getName(), indexedFiles, request);

				if (out != null && indexed)
				{
					out.println(dirUrl + "/" + f.getName());

					long lastDiff = dt.diff(dirUrl + "/" + f.getName());
					out.println(" (+"+lastDiff+" ms)<br/>");
					out.println("<script type='text/javascript'>window.scrollBy(0, 1000);</script>");
					out.flush();
				}
			}
		}

		deleteDisabledPagesInDir(dirUrl);
	}

	/**
	 * Pred reindexaciou adresara zakaze zobrazenie vsetkych web stranok, ktore sa nasledne vymazu (aby sa aktualizoval stav voci file systemu)
	 * @param dirUrl
	 */
	private static void disablePagesInDir(String dirUrl)
	{
		//otaguj stare web stranky aby sa vymazali neexistujuce stranky (subory)
		GroupsDB groupsDB = GroupsDB.getInstance();
		GroupDetails rootGroup = groupsDB.getGroupByPath(dirUrl);
		if (rootGroup != null)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			try
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("UPDATE documents SET available=? WHERE group_id=?");
				ps.setBoolean(1, false);
				ps.setInt(2, rootGroup.getGroupId());
				ps.execute();
				ps.close();
				db_conn.close();
				ps = null;
				db_conn = null;

				DocDB.getInstance(true);
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
			finally
			{
				try
				{
					if (ps != null)
						ps.close();
					if (db_conn != null)
						db_conn.close();
				}
				catch (Exception ex2)
				{
				}
			}
		}
	}

	/**
	 * Po reindexacii adresara vymaze neexistujuce subory (stranky)
	 * @param dirUrl
	 */
	private static void deleteDisabledPagesInDir(String dirUrl)
	{
		//otaguj stare web stranky aby sa vymazali neexistujuce stranky (subory)
		GroupsDB groupsDB = GroupsDB.getInstance();
		GroupDetails rootGroup = groupsDB.getGroupByPath(dirUrl);
		if (rootGroup != null)
		{
			Connection db_conn = null;
			PreparedStatement ps = null;
			try
			{
				db_conn = DBPool.getConnection();
				ps = db_conn.prepareStatement("DELETE FROM documents WHERE available=? AND group_id=?");
				ps.setBoolean(1, false);
				ps.setInt(2, rootGroup.getGroupId());
				ps.execute();
				ps.close();
				db_conn.close();
				ps = null;
				db_conn = null;

				DocDB.getInstance(true);
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
			finally
			{
				try
				{
					if (ps != null)
						ps.close();
					if (db_conn != null)
						db_conn.close();
				}
				catch (Exception ex2)
				{
				}
			}
		}
	}

	/**
	 * Vrati docId pre zadanu URL adresu suboru (pre multidomain uz musi byt zdekodovana na filesystem URL)
	 * @param url
	 * @param request
	 * @return
	 *
	 * @deprecated - not used anymore
	 */
	@Deprecated
	public static int getFileDocId(String url, HttpServletRequest request)
	{
		int docId = -1;
		Connection db_conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnection(DBPool.getDBName(request));
			ps = db_conn.prepareStatement("SELECT doc_id FROM documents WHERE virtual_path=? OR virtual_path=?");
			ps.setString(1, url+".html");
			ps.setString(2, Tools.replace(url, ".", "-")+".html");
			rs = ps.executeQuery();
			if (rs.next())
			{
				docId = rs.getInt("doc_id");
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
			}
		}

		return(docId);
	}


	public static boolean isFileIndexerConfigured()
	{
		if (GroupsDB.getInstance().checkExist(0, "files")) return true;
		if (Constants.getBoolean("enableStaticFilesExternalDir"))
		{
            int domainId = CloudToolsForCore.getDomainId();
			if (GroupsDB.getInstance().checkExist(domainId, "files")) return true;
		}
		return false;
	}

	/**
	 * @deprecated - use FileIndexerTools.indexFile
	 */
	@Deprecated
	public static void indexFile(String url, UserDetails user){
		FileIndexerTools.indexFile(url,user);
	}

	/**
	 * @deprecated - use FileIndexerTools.deleteIndexedFile
	 */
	@Deprecated
	public static boolean deleteIndexedFile(String url){
		return FileIndexerTools.deleteIndexedFile(url);
	}

	/**
	 * @deprecated - use FileIndexerTools.findGroup
	 */
	@Deprecated
	public static GroupDetails findGroup(List<GroupDetails> groups, int parentGroupId, String name){
		return FileIndexerTools.findGroup(groups,parentGroupId,name);
	}

	/**
	 * Index file or folder. If user is not logged redirect to logon.
	 *
	 * @param file - single file URL to index
	 * @param dir - URL of directory to index
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ServletException
	 */
	public static void indexFileOrFolder(String file, String dir,
		HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		//If user is not logged, redirect him to loggon
		Identity user = UsersDB.getCurrentUser(request);
		if (user == null || user.isAdmin() == false) {
			response.sendRedirect("/admin/logon.jsp");
			return;
		}

		if (Tools.isNotEmpty(file) && Tools.isEmpty(dir)) {
			int last = file.lastIndexOf("/");
			if (last > 0) dir = file.substring(0, last);
		}

		List<ResultBean> indexedFiles = new ArrayList<>();

		response.setContentType("text/html; charset=" + SetCharacterEncodingFilter.getEncoding());

		PrintWriter out = response.getWriter();
		Prop prop = Prop.getInstance(request);

		out.println("<html><head><LINK rel='stylesheet' href='/admin/css/style.css'></head><body>");
		out.println("<strong>" + prop.getText("findexer.indexing") + "</strong><br/>");

		if (user.isFolderWritable(dir)) {
			if (Tools.isNotEmpty(file)) {
				if(file.indexOf('/') == -1)
					file = dir + "/" + file;
				Logger.println(FileIndexer.class,"indexujem subor: " + file);
				out.println(file);
				out.println("<script type='text/javascript'>window.scrollBy(0, 1000);</script>");
				out.flush();
				DebugTimer dt = new DebugTimer("FileIndexer");
				FileIndexerTools.indexFile(file, indexedFiles, request);
				long lastDiff = dt.diff(file);
				out.println(" (+"+lastDiff+" ms)<br/>");
				out.println("<script type='text/javascript'>window.scrollBy(0, 1000);</script>");
				out.flush();
				dir = null;
			}

			if (Tools.isNotEmpty(dir) && dir != null && dir.contains("WEB-INF") == false) {
				//budeme rovno vypisovat ak sa nejedna o hromadne indexovanie
				sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

				for (int i = 0; i < 10; i++) {
					out.println("                                                                             ");
				}
				out.flush();
				FileIndexer.indexDir(dir, indexedFiles, request, out);
			}
		}

		out.println("<hr>" + prop.getText("findex.done"));

		out.println("<script type='text/javascript'>");
		out.println("if (window.parent != null && typeof window.parent.hideButtons == 'function')");
		out.println("{");
		out.println("window.parent.hideButtons();");
		out.println("}");
		out.print("window.scrollBy(0, 1000);");
		out.print("</script>");

		out.println("</body></html>");
	}
}
