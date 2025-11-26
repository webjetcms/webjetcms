package sk.iway.iwcm.xls;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.multipart.MultipartFile;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.ExcelImport;
import sk.iway.iwcm.ExcelXImport;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.database.ActiveRecord;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.system.spring.SpringUrlMapping;

public class ImportService {

	public static String importFromExcel(MultipartFile file, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		HttpSession session = request.getSession();
		boolean setAdminLayout = true;
		String importActionAllowedClass = (String)session.getAttribute("ImportXLSAction.allowedClass");
		if (Tools.isEmpty(importActionAllowedClass)) {
			Identity user = (Identity) session.getAttribute(Constants.USER_KEY);

			if (user == null || !user.isAdmin()) {
				SpringUrlMapping.redirectToLogon(response);
				return null;
			}

			if (user.isAdmin()) {
				//mohlo sa stat, ze bol na stranke, robil import a potom presiel do adminu, kde by mal mat vsetko povolene
				session.removeAttribute("ImportXLSAction.allowedClass");
				importActionAllowedClass = null;
			} else
                setAdminLayout = false;
		} else {
            //importujem na beznej web stranku
            setAdminLayout = false;
        }

		Prop prop = Prop.getInstance(request);
		sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
		PrintWriter out = response.getWriter();

		if (setAdminLayout) {
            request.setAttribute("closeTable", "true");
            request.setAttribute("dialogTitleKey", "components.qa.excel_import");
            request.setAttribute("dialogDesc", "&nbsp;");
            request.getRequestDispatcher("/admin/layout_top_dialog.jsp").include(request, response);
        } else {
            out.println("<html><head><LINK rel='stylesheet' href='/admin/css/style.css'></head><body style='background-color: white;'>");
            out.println("<h3>"+prop.getText("file.importing_please_wait")+"</h3>");
        }

		//out.println("<html><head><LINK rel='stylesheet' href='/admin/css/style.css'></head><body style='background-color: white;'>");
		out.println("<h3>"+prop.getText("file.importing_please_wait")+"</h3>");

		String type = Tools.getParameter(request, "type");
		String sheetName = request.getParameter("sheetName");
		if("".equals(sheetName)) sheetName = null;
		Logger.println(ImportService.class, "ImportService: type=" + type + ", sheetName=" + sheetName);

		if (type.indexOf('.') != -1) {
			if (Tools.isNotEmpty(importActionAllowedClass) && type.startsWith(importActionAllowedClass) == false) {
				out.println("<hr>ERROR: Permission denied");
				out.println("</body></html>");

				return null;
			}

			Adminlog.add(Adminlog.TYPE_IMPORTXLS, "ImportService: " + type + " file=" + file.getOriginalFilename() + " size=" + file.getSize(), -1, -1);
			try {
				Class<?> c = Class.forName(type);
				// MBO: pre lenivcov, ktorym sa nechce vyrabat ImportImpl triedu pre JPA beany :)
				if (ActiveRecord.class.isAssignableFrom(c) && c.isAnnotationPresent(XLSImport.class)) {
					XLSImport annotation = c.getAnnotation(XLSImport.class);
					if (DefaultEntityImporter.class.isAssignableFrom(annotation.importer())) {
						@SuppressWarnings("unchecked")
						Constructor<?> constructor = annotation.importer().getConstructor(new Class[] {InputStream.class, HttpServletRequest.class, PrintWriter.class, Class.class});
						DefaultEntityImporter importer = (DefaultEntityImporter)constructor.newInstance(new Object[] {file.getInputStream(), request, out, c});
						if (!file.getOriginalFilename().endsWith(".xls"))
							return printError(prop, out);

						importer.setFileName(file.getOriginalFilename());
						importer.doImport(prop);
					}
				} else {
					Constructor<?> constructor = c.getConstructor(new Class[] {InputStream.class, HttpServletRequest.class, PrintWriter.class});
					Object o = constructor.newInstance(new Object[] {file.getInputStream(), request, out});
					if (o instanceof ExcelImportJXL) {
						if (!file.getOriginalFilename().endsWith(".xls"))
							return printError(prop, out);

						ExcelImportJXL i = (ExcelImportJXL)o;
						i.setFileName(file.getOriginalFilename());
						i.doImport(sheetName, prop);
					} else if (o instanceof ExcelImport) {
						if (!file.getOriginalFilename().endsWith(".xls"))
							return printError(prop, out);

						ExcelImport i = (ExcelImport)o;
						i.doImport(sheetName, prop);
					} else if (o instanceof ExcelXImport) {
						if (!file.getOriginalFilename().endsWith(".xlsx"))
							return printError(prop, out);

						ExcelXImport i = (ExcelXImport)o;
						i.doImport(sheetName, prop);
					} else if(o instanceof CSVImport) {
						if (!file.getOriginalFilename().endsWith(".csv") && !file.getOriginalFilename().endsWith(".txt"))
							return printError(prop, out);

						CSVImport i = (CSVImport)o;
						i.doImport(request, file.getInputStream());
					}
				}

				//ak je nastavene, ulozime po uspesnom ukonceni na dane miesto napr. /files/import_xls/
				String saveExcelTo = request.getParameter("saveExcelTo");
				if(Tools.isNotEmpty(saveExcelTo)) {
					SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy-HH_mm_ss");
					Integer ind = file.getOriginalFilename().lastIndexOf("\\");
					String fileName = file.getOriginalFilename().substring(++ind);
					fileName = DocTools.removeChars(fileName, true);
					saveExcelTo = saveExcelTo.endsWith("/") ? saveExcelTo : saveExcelTo + "/";

					IwcmFile dir = new IwcmFile(Tools.getRealPath(saveExcelTo));
					if(!dir.exists())
						dir.mkdirs();

					String path = saveExcelTo + sdf.format(Tools.getNow()) + "_" + fileName;
					IwcmFile iwcmFile = new IwcmFile(Tools.getRealPath(path));

					if (!iwcmFile.exists())
						iwcmFile.createNewFile();

					IwcmFsDB.writeFiletoDest(file.getInputStream(), new File(iwcmFile.getPath()), (int) file.getSize());
					Logger.println(ImportService.class, "ImportService: kopia XLS " + file.getOriginalFilename() + " nakopirovana do " + path);
					out.println("<p><strong>" + prop.getText("import_excel_action.kopia_ulozena_do", file.getOriginalFilename()) + ": </strong><a href=\"" + path + "\" target=\"_blank\">" + path + "</a></p>");
				}
			}
			catch (Exception e) {
				sk.iway.iwcm.Logger.error(e);
				out.println("<hr>ERROR: " + e.getMessage());
			}
		}

		out.println("<hr><strong>"+prop.getText("file.import_done")+"</strong><br/><br/>");

		if (request.getAttribute("disableReload") == null)
            out.println("<script type='text/javascript'>try { window.opener.location.reload(); } catch (e) { console.log(e); }</script>");

		if (setAdminLayout) {
            out.println("<style type='text/css'>#btnOk {display: none; } </style>");
            request.getRequestDispatcher("/admin/layout_bottom_dialog.jsp").include(request, response);
        } else {
            out.println("<hr>" + prop.getText("file.import_done"));
            out.println("</body></html>");
        }

		return null;
	}

	private static String printError(Prop prop, PrintWriter out) {
		out.println("<strong style='color: red'>" + prop.getText("file.import.bad_file_type") + "</strong>");
		out.println("</body></html>");
		return null;
	}
}
