package sk.iway.iwcm.system.elfinder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.FileIndexerTools;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.filebrowser.FileAtrBean;
import sk.iway.iwcm.filebrowser.FileAtrDB;
import sk.iway.iwcm.findexer.FileIndexer;
import sk.iway.iwcm.findexer.ResultBean;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.stat.Column;

public class FilePropertiesService {
    
    private FilePropertiesService() {
        // private constructor to hide the implicit public one
    }
    
    public static FilePropertiesDTO getOneItem(HttpServletRequest request, Identity user) {
        String dirPath = request.getParameter("dirPath");
        String fileName = request.getParameter("fileName");

        if(Tools.isEmpty(dirPath) || Tools.isEmpty(fileName))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
           
        //Remove excessive spaces
        dirPath = dirPath.replaceAll("\\s+","");
        fileName = fileName.replaceAll("\\s+","");

        //Check perms
        if(user.isFolderWritable(dirPath) == false)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                
        FilePropertiesDTO filePropertiesDTO = new FilePropertiesDTO();
        filePropertiesDTO.setDirPath(dirPath);
        filePropertiesDTO.setFileName(fileName);
        filePropertiesDTO.setOriginalFileName(fileName);
        return filePropertiesDTO;
    }

    public static void indexFile(String dir, String file, HttpServletRequest request, HttpServletResponse response, Identity user) throws IOException {
        int last = file.lastIndexOf("/");
        if (last > 0) dir = file.substring(0, last);

        List<ResultBean> indexedFiles = new ArrayList<>();
        response.setContentType("text/html; charset=" + SetCharacterEncodingFilter.getEncoding());
        PrintWriter out = response.getWriter();
        Prop prop = Prop.getInstance(request);
        out.println("<html><body>");

        if(user.isFolderWritable(dir)) {

            if(Tools.isNotEmpty(file)) {
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

            if(Tools.isNotEmpty(dir) && dir != null && dir.contains("WEB-INF") == false) {
                //budeme rovno vypisovat ak sa nejedna o hromadne indexovanie
                sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
                for (int i = 0; i < 10; i++) {
                    out.println("                                                                             ");
                }

                out.flush();
                FileIndexer.indexDir(dir, indexedFiles, request, out);
            }
        }

        out.println(prop.getText("findex.done"));
        out.println("</body></html>");
    }


	// !! METHOD will remain unchanged from original form UNTIL file_atr table is implemented via SPRING
    public static FilePropertiesDTO saveFile(FilePropertiesDTO entity, HttpServletRequest request, Identity user) {

		//ak je to novy subor, alebo nazov nie je rovnaky ako povodny, skontroluj ho
		if (entity.getOriginalFileName() == null || entity.getOriginalFileName().compareTo(entity.getFileName()) != 0)
		{
			entity.setFileName(DB.internationalToEnglish(DocTools.removeChars(entity.getFileName())));
		}

		if (entity.getFileName() == null || entity.getFileName().length() < 1)
		{
			entity.setFileName("new.html");
		}

		//ak treba, presun subor
		if (entity.getOriginalFileName() != null && entity.getOriginalFileName().length() > 0)
		{
			if (!entity.getFileName().equals(entity.getOriginalFileName()))
			{
				Adminlog.add(Adminlog.TYPE_FILE_EDIT, "Rename file: oldUrl="+entity.getDirPath()+"/"+entity.getOriginalFileName()+" newUrl="+entity.getDirPath()+"/"+entity.getFileName(), -1, -1);

				String dir = Tools.getRealPath(entity.getDirPath());
				Tools.renameFile(dir + File.separatorChar + entity.getOriginalFileName(), dir + File.separatorChar + entity.getFileName());

				//ak existuje full text zmen odkaz
				String oldUrl = entity.getDirPath()+"/"+entity.getOriginalFileName();
				String newUrl = entity.getDirPath()+"/"+entity.getFileName();
				DocDB docDB = DocDB.getInstance();
				int docId = docDB.getDocIdFromURLImpl(oldUrl+".html", null);
				if (docId < 1) docId = docDB.getDocIdFromURLImpl(Tools.replace(oldUrl, ".", "-")+".html", null);
				if (docId > 0)
				{
					IwcmFile f = new IwcmFile(Tools.getRealPath(newUrl));
					long length = f.length();

					//existuje nam full text verzia
					EditorForm ef = EditorDB.getEditorForm(request, docId, -1, -1);
					ef.setTitle(entity.getFileName());
					ef.setNavbar(entity.getFileName() +  " ("+Tools.formatFileSize(length)+")");
					ef.setVirtualPath(newUrl+".html");
					ef.setExternalLink(newUrl);
					ef.setAuthorId(user.getUserId());
					ef.setPublish("1");

					EditorDB.saveEditorForm(ef, request);

					EditorDB.cleanSessionData(request);
				}

				String dir2 = entity.getDirPath();
				System.out.println("dir2 "+dir2);
				String oldFile = dir2 + "/" +  entity.getOriginalFileName();
				String newFile = dir2 + "/" +  entity.getFileName();

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

		//ulozenie atributov stranky
		Connection db_conn = null;
		PreparedStatement ps = null;
		try
		{
			String link = entity.getDirPath() + "/" + entity.getFileName();
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
							ps.setString(1, entity.getFileName());
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
							//String sa = entity.
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

        return entity;
	}
}