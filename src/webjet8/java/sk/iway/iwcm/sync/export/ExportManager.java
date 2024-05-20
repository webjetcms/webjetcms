package sk.iway.iwcm.sync.export;

import java.beans.XMLEncoder;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.FileBrowserTools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.editor.DocNoteBean;
import sk.iway.iwcm.editor.DocNoteDB;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.system.zip.ZipEntry;
import sk.iway.iwcm.system.zip.ZipOutputStream;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.spirit.MediaDB;
import sk.iway.spirit.model.Media;

/**
 * Export stranok.
 * Pouzitie:
 * <pre>
 * ExportManager export = ExportManager.create(request, session);
 * export.exportGroup(groupId); // linku na vystup ulozi do ${zipfile}
 * </pre>
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 8.6.2012 19:13:50
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ExportManager
{

	private HttpServletRequest _request; //NOSONAR
	private HttpSession _session; //NOSONAR

	private String virtualBase;  // virtualny adresar na export
	private String realBase;  // realny adresar na export

	private Identity user;

	private GroupsDB groupsDB = GroupsDB.getInstance();

	private List<ArchiveEntry> entries = new ArrayList<>();

	private JspWriter out = null;

	public static ExportManager create(HttpServletRequest request, HttpSession session)
	{
		ExportManager manager = new ExportManager();
		manager.init(request, session);
		return manager;
	}

	private ExportManager(){
		// nepouzivat priamo; instancie vytvarame cez ExportManazer.create
	}

	private void init(HttpServletRequest request, HttpSession session)
	{
		_request = request;
		_session = session;

		user = (Identity) _session.getAttribute(Constants.USER_KEY);

		initFolders(-1);
	}

	private void initFolders(int rootGroupId)
	{
		String groupIdAppend = "";
		if (rootGroupId > 0)
		{
			groupIdAppend = rootGroupId+"/";
		}

		virtualBase = Constants.getBoolean("multiDomainEnabled")
		? ("/files/" + MultiDomainFilter.getDomainAlias(DocDB.getDomain(_request)) + "/protected/backup/"+groupIdAppend)
		: "/files/protected/backup/"+groupIdAppend;

		virtualBase = Tools.replace(virtualBase, "//", "/");

		realBase = realPath(virtualBase);
		if (!new IwcmFile(realBase).exists())
		{
			new IwcmFile(realBase).mkdirs();
		}
	}

	/**
	 * Exportuje vsetko v danom adresari.
	 *
	 * @param groupId       identifikator adresara
	 * @throws IOException  chyba pri zapisovani na disk alebo zipovani
	 */
	public void exportGroup(int groupId) throws IOException
	{
		if (null == user) return;  // neprihlaseny pouzivatel nemoze exportovat

		writeOut("Export id: "+groupId);

		initFolders(groupId);

		String zipFilename = DocDB.getDomain(_request) + "-" + new SimpleDateFormat("yyyy-MM-dd-HHmm").format(new Date()) + ".zip";

		//pre cloud cachujeme uz raz exportovane ZIP subory za dany den
		if ("cloud".equals(Constants.getInstallName()))
		{
			zipFilename = Constants.getInstallName() + "-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "-"+groupId+".zip";
			if (FileTools.isFile(virtualBase+zipFilename))
			{
				Logger.debug(ExportManager.class, "Returning ZIP from cache: "+zipFilename);
				_request.setAttribute("zipfile", virtualBase + zipFilename);
				return;
			}
		}

		writeOut("Zip: "+virtualBase+"/"+zipFilename);

		// exportuj uzivatelske skupiny
		List<UserGroupDetails> userGroups = UserGroupsDB.getInstance().getUserGroups();
		if (null != userGroups)
		{
			write("usergroups.xml", userGroups);
		}

		writeOut("usergroups.xml");

		// exportuj perex skupiny
		List<PerexGroupBean> perexGroups = DocDB.getInstance().getPerexGroups();
		if (null != perexGroups)
		{
			write("perexgroups.xml", perexGroups);
		}

		writeOut("perexgroups.xml");

		// exportuj sablony
		List<TemplateDetails> templates = TemplatesDB.getInstance().getTemplates();
		if (null != templates)
		{
			write("temps.xml", templates);
		}

		writeOut("temps.xml");

		// exportuj adresare
		List<GroupDetails> groups = groupsDB.getGroupsTree(groupId, true, true);
		if (null != groups)
		{
			write("groups.xml", groups);
		}

		writeOut("groups.xml");

		//exportuj poznamky pre redaktorov
		List<DocNoteBean> docNotes = DocNoteDB.getInstance().getCurrentDocNotes();
		if (null != docNotes)
		{
			write("notes.xml", docNotes);
		}

		writeOut("notes.xml");

		// exportuj stranky
		List<DocDetails> docs = getAllDocs(groupId);
		if (!docs.isEmpty()) {
			write("docs.xml", docs);
		}

		writeOut("docs.xml");

		Content content = new Content();
		ContentBuilder callback = new ContentBuilder(content, _request);
		int counter = 0;
		for (DocDetails doc : docs)
		{
			counter++;
			DocExporter.export(doc, callback);
			if (counter % 10 == 0) writeOut("doc "+counter+"/"+docs.size());
		}

		writeOut("content.xml");

		//pridaj subory ak je definovany adresar
		String[] addDirs = _request.getParameterValues("adddir");
		if (addDirs != null && addDirs.length>0)
		{
			for (String dir : addDirs)
			{
				if (FileBrowserTools.hasForbiddenSymbol(dir)) continue;

				addCustomFiles(dir, callback, docs, 0);
			}
		}

		writeOut("customfiles");

		List<Content.File> contentFiles = content.getFiles();
		counter = 0;
		for (Content.File contentFile : contentFiles)
		{
			counter++;
			Logger.debug(ExportManager.class, "Adding file:"+contentFile.getVirtualPath());
			String realFile = realPath(contentFile.getVirtualPath());
			String zipFile = "content/" + contentFile.getZipPath();
			IwcmFile iFile = new IwcmFile(realFile);
			contentFile.setSize(iFile.length());
			contentFile.setTime(iFile.lastModified());
			entries.add(new ArchiveEntry(zipFile, realFile));

			if (counter % 10 == 0) writeOut("files "+counter+"/"+contentFiles.size());
		}
		write("content.xml", content);

		writeOut("content.xml");

		//exportuj media
		List<Media> media = MediaDB.getMedia(_session, "documents", -1, null, 0, false);
		write("media.xml", media);

		writeOut("media.xml");

		IwcmFile exportFile = new IwcmFile(realBase, zipFilename);

		writeOut("Export ZIP: "+exportFile.getVirtualPath());

		IwcmOutputStream output = new IwcmOutputStream(exportFile);
		ZipOutputStream zipOut = new ZipOutputStream(output);
		counter = 0;
		for (ArchiveEntry entry : entries)
		{
			counter++;
			addToZipArchive(zipOut, entry.entryPath, entry.filePath);
			if (counter % 10 == 0) writeOut("zipping "+counter+"/"+entries.size());
		}
		zipOut.close();

		writeOut("ZIP OK");

		for (String filename : new String[]{ "docs.xml", "groups.xml", "temps.xml", "usergroups.xml", "content.xml" })
		{
			new IwcmFile(filename).delete();
		}

		//zapis do auditu cestu k suboru
		Adminlog.add(Adminlog.TYPE_EXPORT_WEBJET, "Exporting web pages, group="+groupId+" exported ZIP="+exportFile.getVirtualPath(), groupId, -1);

		_request.setAttribute("zipfile", virtualBase + zipFilename);
	}

	/**
	 * Prida do exportu custom adresare (subory) volane parametrom adddir (pouziva sa v cloude)
	 * @param dir - URL adresa adresara, napr. /images/template/common, alebo /images/template/temp48
	 * @param callback
	 * @param docs
	 * @param failsafeCounter - failsafe counter, na zaciatku 0, potom sa pri rekurzii zvysuje, max hlbka je 10
	 */
	private void addCustomFiles(String dir, ContentBuilder callback, List<DocDetails> docs, int failsafeCounter)
	{
		if (failsafeCounter > 10) return;

		if (dir.startsWith("/images") || dir.startsWith("/files"))
		{
			IwcmFile fDir = new IwcmFile(Tools.getRealPath(dir));
			if (fDir.isDirectory())
			{
				IwcmFile[] files = fDir.listFiles();
				for (IwcmFile file : files)
				{
					if (file.isDirectory())
					{
						addCustomFiles(dir + "/" + file.getName(), callback, docs, failsafeCounter+1);
					}
					else if (file.canRead())
					{
						callback.setDoc(docs.get(0));
						Logger.debug(ExportManager.class, "Adddir, file="+file.getVirtualPath());
						callback.addLink(file.getVirtualPath());
					}
				}
			}
		}
	}

	private List<DocDetails> getAllDocs(int rootGroupId)
	{
		List<DocDetails> docs = new ArrayList<>();
		List<GroupDetails> groups = groupsDB.getGroupsTree(rootGroupId, true, true);
		for (GroupDetails group : groups)
		{
			List<DocDetails> groupDocs = getDocByGroup(group.getGroupId()); //DocDB.getInstance().getDocByGroup(group.getGroupId());
			docs.addAll(groupDocs);
		}
		return docs;
	}

	/**
	 * Pouzite priamo tu citanie z DB pretoze sa maju exportovat aj not available stranky, co standarde DocDB nevie kvoli tomu aby to nahodou niekto nepouzil ;-)
	 * @param groupId
	 * @return
	 */
	private List<DocDetails> getDocByGroup(int groupId)
	{
		List<DocDetails> ret = new ArrayList<>();
		java.sql.Connection db_conn = null;
		java.sql.PreparedStatement ps = null;
		ResultSet rs = null;
		try
		{
			db_conn = DBPool.getConnectionReadUncommited();

			//read user permissions for every org struct

			StringBuilder order = new StringBuilder("d.title");
			order.append(" ASC");

			String sql;
			DocDetails doc;
			String usersTablePrefix = Constants.getString("usersTablePrefix");
			if (Constants.DB_TYPE == Constants.DB_ORACLE)
			{
				sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, d.* FROM documents d, "+usersTablePrefix+"users u WHERE d.author_id=u.user_id(+) AND group_id=" + groupId + " ORDER BY " + order.toString();
			}
			else
			{
				sql = "SELECT u.title as u_title, u.first_name, u.last_name, u.email, u.photo, d.* FROM documents d LEFT JOIN "+usersTablePrefix+"users u ON d.author_id=u.user_id WHERE group_id=" + groupId + " ORDER BY " + order.toString();
			}
			ps = db_conn.prepareStatement(sql);
			rs = ps.executeQuery();

			while (rs.next())
			{
				doc = DocDB.getDocDetails(rs, false);

				ret.add(doc);
			}
			rs.close();
			ps.close();
			db_conn.close();
			db_conn = null;
			ps = null;
			rs = null;
		}
		catch (Exception ex){sk.iway.iwcm.Logger.error(ex);}
		finally{
			try{
				if (rs != null) rs.close();
				if (ps != null) ps.close();
				if (db_conn != null) db_conn.close();
			}catch (Exception e) {sk.iway.iwcm.Logger.error(e);}
		}
		return (ret);
	}

	/**
	 * Ulozi objekt do suboru vo formate XML.
	 *
	 * @param filename  retazec s nazvom suboru
	 * @param object    objekt, ktory ideme serializovat
	 * @throws IOException
	 */
	private void write(String filename, Object object) throws IOException
	{
		IwcmOutputStream output = new IwcmOutputStream(new IwcmFile(realBase, filename));
		XMLEncoder encoder = new XMLEncoder(output);
		encoder.writeObject(object);
		encoder.close();
		output.close();
		entries.add(new ArchiveEntry(filename, realBase + File.separator + filename));
	}

	static void addToZipArchive(ZipOutputStream zipOut, String entryPath, String filePath)
	{
		try
		{
			ZipEntry entry = new ZipEntry(entryPath);
			IwcmFile inFile = new IwcmFile(filePath);
			entry.setSize(inFile.length());
			entry.setTime(inFile.lastModified());

			zipOut.putNextEntry(entry);
			IwcmInputStream in = new IwcmInputStream(inFile);
			int bytesRead = -1;
			byte[] buffer = new byte[1024];
			while ((bytesRead = in.read(buffer)) != -1)
			{
				zipOut.write(buffer, 0, bytesRead);
			}
			in.close();

			zipOut.closeEntry();
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
	}

	private static String realPath(String virtualPath)
	{
		return Tools.getRealPath(virtualPath);
	}

	static class ArchiveEntry
	{

		public final String entryPath;
		public final String filePath;  // real

		public ArchiveEntry(String entryPath, String filePath)
		{
			this.entryPath = entryPath;
			this.filePath = filePath;
		}

	}

	public void setOut(JspWriter out) {
		this.out = out;
	}

	private void writeOut(String text) {
		if (out == null) return;
		try {
			out.println("<p>"+Tools.formatDateTimeSeconds(Tools.getNow())+" "+text+"</p>");
			out.println("<script type='text/javascript'>window.scrollBy(0,1000);</script>");
			out.flush();
		}
		catch (Exception ex) {

		}
	}
}
