package sk.iway.iwcm.filebrowser;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.ui.Model;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.stat.Column;
import sk.iway.iwcm.system.spring.SpringUrlMapping;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

public class FileBrowserService {

	/**
	 * Prepare model/form for elfinder dir properties (fbrowser_dirprop.jsp)
	 * @param dir
	 * @param model
	 * @param request
	 * @return
	 */
	public static String editDir(String dir, Model model, HttpServletRequest request) {
		UserDetails user = UsersDB.getCurrentUser(request);
        boolean canUpload = user.isFolderWritable("/"+dir);

        if (Tools.isEmpty(dir) || canUpload==false) {
            return SpringUrlMapping.redirectTo403();
        }

        IwcmFile f = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(dir));
        String dirName = f.getName();

        UserGroupsDB userGroupsDB = UserGroupsDB.getInstance();
        request.setAttribute("userGroupsList", userGroupsDB.getUserGroups());

        EditForm ef = new EditForm();
        ef.setDir(dirName);
        ef.setOrigDir(dir);

        FileAtrDB.fillEditForm(ef);

        model.addAttribute("fbrowserEditForm", ef);

        return "/admin/fbrowser_dirprop";
	}

	/**
	 * Save elfinder dir properties (fbrowser_dirprop.jsp)
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 */
    public static String saveDir(EditForm form, HttpServletRequest request, HttpServletResponse response) {
        //Request check
        if(request == null) return SpringUrlMapping.redirectToLogon();

        //Get session +  check
        HttpSession session = request.getSession();
		if (session == null) return SpringUrlMapping.redirectToLogon();

        //Check logged user
		Identity user = (Identity) session.getAttribute(Constants.USER_KEY);
		if (user == null || !user.isAdmin()) return SpringUrlMapping.redirectToLogon();

		if (user.isFolderWritable(form.getOrigDir() + "/") == false)
			return SpringUrlMapping.redirect( "fbrowser.browse.do?refreshLeft=true&permsDenied=true&dir=" + form.getOrigDir() );

		//Delete old one and save new one
		String dirUrl = form.getOrigDir();
		new SimpleQuery().execute("DELETE FROM dirprop WHERE dir_url = ?", dirUrl);

		new SimpleQuery().execute("INSERT INTO dirprop (dir_url, index_fulltext, password_protected, logon_doc_id) VALUES (?, ?, ?, ?)",
			dirUrl,
			form.isIndexFulltext(),
			form.getPasswordProtectedString(),
			form.getLogonDocId());

		//refreshni zoznam v PathFilter
		PathFilter.reloadProtectedDirs();

		String forward = "/admin/fbrowser/dirprop/?saveok=true&refreshLeft=true&dir=" + form.getOrigDir();

		//There is lack of /admin/ prefix
		return SpringUrlMapping.redirect( Tools.sanitizeHttpHeaderParam(forward) );
	}

	/**
	 * Prepare model/form for elfinder file properties (fbrowser_prop.jsp)
	 * @param dir
	 * @param model
	 * @param request
	 * @return
	 */
	public static String editFile(String dir, Model model, HttpServletRequest request) {
		Identity user = UsersDB.getCurrentUser(request);
		if (user != null && user.isAdmin())
		{
		}
		else
		{
			return SpringUrlMapping.redirectToLogon();
		}
		if (user.isFolderWritable(dir)==false) return "/admin/fbrowser_editor";

		String file = request.getParameter("file");
		// writeOutFile(dir,file);
		if ("".equals(file))
		{
			// mame novy subor
			file = null;
		}
		boolean alsoData = true;
		if ("yes".equals(request.getParameter("prop")))
		{
			alsoData = false;
		}
		EditForm editForm = EditorToolsForCore.fillEditFormFromFile(dir, file, alsoData, user);
		model.addAttribute("fbrowserEditForm", editForm);
		if (file != null)
		{
			// atributy stranky
			List<?> atrs = FileAtrDB.getAtributes(dir + "/" + file, request);
			if (atrs.size() > 0)
			{
				request.setAttribute("atrs", atrs);
			}
		}
		return "/admin/fbrowser_prop";
	}

	/**
	 * Save elfinder file properties (fbrowser_prop.jsp)
	 * @param editForm
	 * @param request
	 * @return
	 */
	public static String saveFile(EditForm editForm, HttpServletRequest request) {
		Identity user = UsersDB.getCurrentUser(request);
		if (user != null && user.isAdmin())
		{

		}
		else
		{
			return SpringUrlMapping.redirectToLogon();
		}

		//ak je to novy subor, alebo nazov nie je rovnaky ako povodny, skontroluj ho
		if (editForm.getOrigFile() == null || editForm.getOrigFile().compareTo(editForm.getFile()) != 0)
		{
			editForm.setFile(DB.internationalToEnglish(DocTools.removeChars(editForm.getFile())));
		}

		if (editForm.getFile() == null || editForm.getFile().length() < 1)
		{
			editForm.setFile("new.html");
		}

		boolean needRefreshParent = false;

		boolean isProp = "yes".equals(request.getParameter("prop"));

		//ak treba, presun subor
		if (editForm.getOrigFile() != null && editForm.getOrigFile().length() > 0 && isProp)
		{
			if (!editForm.getFile().equals(editForm.getOrigFile()))
			{
				Adminlog.add(Adminlog.TYPE_FILE_EDIT, "Rename file: oldUrl="+editForm.getDir()+"/"+editForm.getOrigFile()+" newUrl="+editForm.getDir()+"/"+editForm.getFile(), -1, -1);

				String dir = Tools.getRealPath(editForm.getDir());
				Tools.renameFile(dir + File.separatorChar + editForm.getOrigFile(), dir + File.separatorChar + editForm.getFile());
				needRefreshParent = true;

				//ak existuje full text zmen odkaz
				String oldUrl = editForm.getDir()+"/"+editForm.getOrigFile();
				String newUrl = editForm.getDir()+"/"+editForm.getFile();
				DocDB docDB = DocDB.getInstance();
				int docId = docDB.getDocIdFromURLImpl(oldUrl+".html", null);
				if (docId < 1) docId = docDB.getDocIdFromURLImpl(Tools.replace(oldUrl, ".", "-")+".html", null);
				if (docId > 0)
				{
					IwcmFile f = new IwcmFile(Tools.getRealPath(newUrl));
					long length = f.length();

					//existuje nam full text verzia
					EditorForm ef = EditorDB.getEditorForm(request, docId, -1, -1);
					ef.setTitle(editForm.getFile());
					ef.setNavbar(editForm.getFile() +  " ("+Tools.formatFileSize(length)+")");
					ef.setVirtualPath(newUrl+".html");
					ef.setExternalLink(newUrl);
					ef.setAuthorId(user.getUserId());
					ef.setPublish("1");

					EditorDB.saveEditorForm(ef, request);

					EditorDB.cleanSessionData(request);
				}

				String dir2 = editForm.getDir();
				System.out.println("dir2 "+dir2);
				String oldFile = dir2 + "/" +  editForm.getOrigFile();
				String newFile = dir2 + "/" +  editForm.getFile();

				if(Tools.getIntValue(request.getParameter("zmenNazov"), 0)==1)
				{
					// zmenim udaje v suboroch
					Column col;
					String stranka="";
					List<Column> zoznamUsage = FileTools.getFileUsage(oldFile);
					for(int i=0;i<zoznamUsage.size();i++)
					{
						col = zoznamUsage.get(i);
						String subor = col.getColumn2().replaceAll("\\\\", "/");
						IwcmFile f = new IwcmFile(Tools.getRealPath(subor));
						if (f.exists() && f.isFile() && f.canRead())
						{
							stranka = FileTools.readFileContent(subor);
							if(Tools.isNotEmpty(stranka))
							{
								String strankaNova = stranka.replaceAll(oldFile, newFile);
								if (strankaNova.equals(stranka)==false)
								{
									FileTools.saveFileContent(subor, strankaNova);
									System.out.println("ZMENENE "+col.getColumn2());
								}
							}
						}
					}
					//zmenim udaje na strankach
					List<DocDetails> replacedPages = docDB.replaceTextAll(oldFile, newFile);
					request.setAttribute("replacedPages", replacedPages);
				}
			}
		}


		//ak je to novy subor
		if (Tools.isEmpty(editForm.getOrigFile()))
		{
			needRefreshParent = true;
		}

		//ulozenie atributov stranky
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			String link = editForm.getDir() + "/" + editForm.getFile();
			//TODO: ulozenie do history! (+restore z history)

			//najskor vymazeme
			db_conn = DBPool.getConnection(request);
			ps = db_conn.prepareStatement("DELETE FROM file_atr WHERE link=?");
			ps.setString(1, link);
			ps.execute();
			ps.close();
			ps = null;

			//nainsertujeme
			Enumeration<String> params = request.getParameterNames();
			String name;
			String value;
			int atrId;
			FileAtrBean atr;
			while (params.hasMoreElements())
			{
				try
				{
					name = params.nextElement();
					if (name != null && name.startsWith("atr_"))
					{
						atrId = Integer.parseInt(name.substring(4));
						value = request.getParameter(name);
						atr = FileAtrDB.getAtrDef(atrId, request);
						if (Tools.isNotEmpty(value) && atr != null)
						{
							ps = db_conn.prepareStatement("INSERT INTO file_atr (file_name, link, atr_id, value_string, value_int, value_bool) VALUES (?, ?, ?, ?, ?, ?)");
							ps.setString(1, editForm.getFile());
							ps.setString(2, link);
							ps.setInt(3, atrId);
							if (atr.getAtrType() == FileAtrDB.TYPE_INT)
							{
								ps.setString(4, Integer.toString(Integer.parseInt(value)));
								ps.setInt(5, Integer.parseInt(value));
								ps.setNull(6, Types.INTEGER);
							}
							else if (atr.getAtrType() == FileAtrDB.TYPE_BOOL)
							{
								ps.setString(4, null);
								ps.setNull(5, Types.INTEGER);
								if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value))
								{
									ps.setBoolean(6, true);
								}
								else
								{
									ps.setBoolean(6, false);
								}
							}
							else
							{
								ps.setString(4, value);
								ps.setNull(5, Types.INTEGER);
								ps.setNull(6, Types.INTEGER);
							}
							ps.execute();
							ps.close();
							ps = null;
							//String sa = editForm.
							//ps = db_conn.prepareStatement("UPDATE documents SET title=? WHERE doc_id=?");
						}
					}
				}
				catch (Exception ex2)
				{
					sk.iway.iwcm.Logger.error(ex2);
				}
			}
			db_conn.close();
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
				if (ps != null)
					ps.close();
				if (db_conn != null)
					db_conn.close();
			}
			catch (Exception ex2)
			{
			}
		}

		StringBuilder url = new StringBuilder("/admin/fbrowser/fileprop/?dir=").append(Tools.URLEncode(editForm.getDir())).append("&file=").append(Tools.URLEncode(editForm.getFile()));
		if ("yes".equals(request.getParameter("prop")))
		{
			url.append("&prop=yes");
		}
		if (needRefreshParent)
		{
			url.append("&refresh=yes");
		}
		url.append("&saved=true");
		return SpringUrlMapping.redirect(url.toString());
	}
}