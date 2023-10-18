package sk.iway.iwcm.findexer;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;

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
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
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
	 */
	public static boolean indexFile(String url, List<ResultBean> indexedFiles, HttpServletRequest request)
	{
		if (!url.startsWith("/files/") || url.endsWith("/upload"))
		{
			 Logger.error(FileIndexer.class,"url musi zacinat na /files/ a nesmie koncit na /upload: "+url);
			 return false;
		}
		url = Tools.replace(url, "//", "/");
		Identity user = UsersDB.getCurrentUser(request);
	   if(user == null)
		{
			Logger.error(FileIndexer.class,"prihlaseny user je NULL");
			return false;
		}
		boolean ok = true;
		try
		{
			String realPath = Tools.getRealPath(url);
			String data = null;

			if (url.indexOf('.')==-1)
			{
				Logger.error(FileIndexer.class,"url musi obsahovat bodku: "+url);
				return(false);
			}

			String ext = url.substring(url.lastIndexOf('.')).toLowerCase();

			try
			{
				data = FileIndexerTools.getData(url, realPath, data, ext);
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}

			if (Tools.isEmpty(data) && Constants.getBoolean("fileIndexerIndexAllFiles") && url.startsWith("/files/"))
			{
				data = "<p><a href='"+url+"'>"+url+"</p>";
			}
			if (data != null && data.trim().length() > 4)
			{
				//mame text, mozeme zaindexovat

				data = FileIndexerTools.cleanText(url, data);

				//Logger.debug(FileIndexer.class, data);

				//Logger.println(FileIndexer.class,data);
				Logger.println(FileIndexer.class,"done, size="+data.length());
				ResultBean result = new ResultBean();
				result.setFile(url);
				result.setData(data);
				indexedFiles.add(result);

				//vytvor Stranku s tymto suborom
				StringTokenizer st = new StringTokenizer(FileIndexerTools.getUrlForGroupsTokenize(url), "/");
				int parentDirId = 0;
				int parentTempId = 1;
				String dirName;
				GroupsDB groupsDB = GroupsDB.getInstance();
				GroupDetails group = null;
				int level = 0;
				int sortPriority = Constants.getInt("fileIndexerSortPriority");
				while (st.hasMoreTokens())
				{
					level++;
					dirName = st.nextToken();

					if ((Constants.getBoolean("multiDomainEnabled")==false && level > 1) ||
						 (Constants.getBoolean("multiDomainEnabled")==true  && level > 2))
					{
						//automaticke navysovanie priority
						sortPriority = sortPriority * 10;
					}

					if (st.hasMoreTokens())
					{
						//je to skutocne adresar
						boolean changed = false;
						group = FileIndexerTools.findGroup(groupsDB.getGroupsAll(), parentDirId, dirName);
						if (group == null)
						{
							//vytvor adresar
							group = new GroupDetails();
							changed = true;
						}
						if (changed || group.getGroupName().equals(dirName)==false ||
							 group.getSortPriority()!=sortPriority)
						{
							if (level==1 && Constants.getBoolean("enableStaticFilesExternalDir"))
							{
								//je to root, s nim nerobime nic
							}
							else
							{
								group.setGroupName(dirName);
								group.setParentGroupId(parentDirId);
								group.setSortPriority(sortPriority);
								group.setTempId(parentTempId);
								group.setMenuType(GroupDetails.MENU_TYPE_HIDDEN);

								if (level == 2 && Constants.getBoolean("multiDomainEnabled"))
								{
									//aby sa nam multidomain subfolder nezobrazoval v ceste
									group.setNavbar("&nbsp;");
								}

								groupsDB.setGroup(group);
							}
						}

						parentDirId = group.getGroupId();
						parentTempId = group.getTempId();
					}
					else if (group != null)
					{
						IwcmFile f = new IwcmFile(realPath);
						long length = f.length();

						//uz sme na konci, dirName je uz fileName
						Logger.println(FileIndexer.class,"Vytvaram stranku: " + dirName);

						int docId = DocDB.getInstance().getDocIdFromURLImpl(url+".html", null);
						if (docId < 1) docId = FileIndexerTools.getFileDocId(dirName, group.getGroupId());

						EditorForm ef = EditorDB.getEditorForm(request, docId, -1, parentDirId);
						//suborom nastavujem rovnaku pripritu ako je priorita adresara
						ef.setGroupId(parentDirId);
						ef.setSortPriority(group.getSortPriority());
						ef.setTitle(dirName);
						ef.setData(data);
						ef.setExternalLink(url);
						ef.setVirtualPath(url+".html");
						ef.setNavbar(dirName +  " ("+Tools.formatFileSize(length)+")");

						//ak este existuje user, ktory subor povodne zaindexoval, zachovaj ho, inak zmen na aktualne prihlaseneho
						UserDetails author = UsersDB.getUser(ef.getAuthorId());
						if (author == null)	ef.setAuthorId(user.getUserId());

						ef.setSearchable(true);
						ef.setAvailable(true);
						ef.setCacheable(false);
						ef.setPublish("1");

						//ak mame v requeste nejake editor hodnoty, nastavme ich, tie z request.getAttribute sa pouzivaju v UnzipAction
						if (request.getParameter("passwordProtected")!=null) BeanUtils.setProperty(ef, "passwordProtected", request.getParameterValues("passwordProtected"));
						if (request.getAttribute("passwordProtected")!=null) ef.setPasswordProtectedString((String)request.getAttribute("passwordProtected"));

						String[] fields = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L"};
						for (String pismeno : fields)
						{
							String value = request.getParameter("field"+pismeno);
							if (value==null) value = (String)request.getAttribute("field"+pismeno);

							if (value != null) BeanUtils.setProperty(ef, "field"+pismeno, value);
						}
						if (request.getParameter("perexGroup")!=null) BeanUtils.setProperty(ef, "perexGroup", request.getParameterValues("perexGroup"));
						if(Tools.isNotEmpty(Constants.getString("beforeFileIndexerEditorSaveMathod")))
						{
							try{
								String saveMethod = Constants.getString("beforeFileIndexerEditorSaveMathod");
								String clazzName = saveMethod.substring( 0, saveMethod.lastIndexOf('.'));
								String methodName = saveMethod.substring( clazzName.length() +1 );
								Class<?> clazz = Class.forName(clazzName);
								Method method = clazz.getMethod(methodName, Class.forName("sk.iway.iwcm.editor.EditorForm"),Class.forName("javax.servlet.http.HttpServletRequest"));
								method.invoke(null, ef,request);
							}
							catch(Exception e){
								Logger.debug(FileIndexer.class, "Exception while trying to invoke "+Constants.getString("beforeFileIndexerEditorSaveMathod")+" , cause: "+e.getMessage());
							}
						}
						int saveEditOk = EditorDB.saveEditorForm(ef, request);

						EditorDB.cleanSessionData(request);

						if(saveEditOk < 1)
						{
							Logger.error(FileIndexer.class,"nepodarilo sa ulozit stranku saveEditOk "+saveEditOk);
							ok = false;
						}
						result.setDocId(ef.getDocId());
					}
				}
			}
		}
		catch (Exception ex)
		{
			ok = false;
			sk.iway.iwcm.Logger.error(ex);
		}

		return(ok);
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
			if (out != null) out.println(dirUrl+" is BACKUP, skipping</br>");
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
					out.println(dirUrl + "/" + f.getName());
					out.println("<script type='text/javascript'>window.scrollBy(0, 1000);</script>");
					out.flush();
				}

				indexFile(dirUrl + "/" + f.getName(), indexedFiles, request);

				if (out != null)
				{
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
	 */
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
				String[] filesArray = {file};
				if(filesArray != null && filesArray.length > 0) {
					for(String fileTmp : filesArray) {
						if(fileTmp.indexOf('/') == -1)
							fileTmp = dir + "/" + fileTmp;
						Logger.println(FileIndexer.class,"indexujem subor: " + fileTmp);
						out.println(fileTmp);
						out.println("<script type='text/javascript'>window.scrollBy(0, 1000);</script>");
						out.flush();
						DebugTimer dt = new DebugTimer("FileIndexer");
						FileIndexer.indexFile(fileTmp, indexedFiles, request);
						long lastDiff = dt.diff(fileTmp);
						out.println(" (+"+lastDiff+" ms)<br/>");
						out.println("<script type='text/javascript'>window.scrollBy(0, 1000);</script>");
						out.flush();
					}
				}
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
