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

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

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

/**
 *  action ktory zavola prislusnu classu na importnutie XLS subora do databazy
 *
 *@Title        WebJET - Ceska Alzheimerova Spolocnost
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2003
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Streda, 2003, január 29
 *@modified     $Date: 2004/03/23 19:23:03 $
 */
public class ImportXLSAction extends Action
{
	/**
	 *  Description of the Method
	 *
	 *@param  mapping               Description of the Parameter
	 *@param  form                  Description of the Parameter
	 *@param  request               Description of the Parameter
	 *@param  response              Description of the Parameter
	 *@return                       Description of the Return Value
	 *@exception  IOException       Description of the Exception
	 *@exception  ServletException  Description of the Exception
	 */
	@Override
	public ActionForward execute(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
					throws IOException, ServletException
	{
		HttpSession session = request.getSession();
		if (session == null)
		{
			return (mapping.findForward("logon_admin"));
		}

		boolean setAdminLayout = true;

		String povolenyImportTrieda = (String)session.getAttribute("ImportXLSAction.allowedClass");
		if (Tools.isEmpty(povolenyImportTrieda))
		{
			Identity user = (Identity) session.getAttribute(Constants.USER_KEY);

			if (user == null || user.isAdmin()==false)
			{
				return (mapping.findForward("logon_admin"));
			}
			if (user.isAdmin())
			{
				//mohlo sa stat, ze bol na stranke, robil import a potom presiel do adminu, kde by mal mat vsetko povolene
				session.removeAttribute("ImportXLSAction.allowedClass");
				povolenyImportTrieda = null;
			}
			else
            {
                setAdminLayout = false;
            }
		}
		else
        {
            //importujem na beznej web stranku
            setAdminLayout = false;
        }

		Prop prop = Prop.getInstance(servlet.getServletContext(), request);

		ImportXLSForm f = (ImportXLSForm) form;

		sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
		PrintWriter out = response.getWriter();

		if (setAdminLayout)
        {
            request.setAttribute("closeTable", "true");
            request.setAttribute("dialogTitleKey", "components.qa.excel_import");
            request.setAttribute("dialogDesc", "&nbsp;");
            request.getRequestDispatcher("/admin/layout_top_dialog.jsp").include(request, response);
        }
		else
        {
            out.println("<html><head><LINK rel='stylesheet' href='/admin/css/style.css'></head><body style='background-color: white;'>");
            out.println("<h3>"+prop.getText("file.importing_please_wait")+"</h3>");
        }

		//out.println("<html><head><LINK rel='stylesheet' href='/admin/css/style.css'></head><body style='background-color: white;'>");
		out.println("<h3>"+prop.getText("file.importing_please_wait")+"</h3>");

		String type = Tools.getParameter(request, "type");
		String sheetName = request.getParameter("sheetName");
		if("".equals(sheetName)) sheetName = null;
		Logger.println(this,"ImportXLSAction: type="+type+", sheetName="+sheetName);

		session.removeAttribute("xlsImportForm");

		if (type.indexOf('.')!=-1)
		{
			if (Tools.isNotEmpty(povolenyImportTrieda) && type.startsWith(povolenyImportTrieda)==false)
			{
				out.println("<hr>ERROR: Permission denied");
				out.println("</body></html>");

				return(null);
			}

			Adminlog.add(Adminlog.TYPE_IMPORTXLS, "ImportXLSAction: " + type + " file="+f.getFile().getFileName()+" size="+f.getFile().getFileSize(), -1, -1);
			try
			{
				Class<?> c = Class.forName(type);
				// MBO: pre lenivcov, ktorym sa nechce vyrabat ImportImpl triedu pre JPA beany :)
				if (ActiveRecord.class.isAssignableFrom(c) && c.isAnnotationPresent(XLSImport.class))
				{
					XLSImport annotation = c.getAnnotation(XLSImport.class);
					if (DefaultEntityImporter.class.isAssignableFrom(annotation.importer()))
					{
						@SuppressWarnings("unchecked")
						Constructor<?> constructor = annotation.importer().getConstructor(new Class[] {InputStream.class, HttpServletRequest.class, PrintWriter.class, Class.class});
						DefaultEntityImporter importer = (DefaultEntityImporter)constructor.newInstance(new Object[] {f.getFile().getInputStream(), request, out, c});
						if (!f.getFile().getFileName().endsWith(".xls"))
						{
							return printError(prop, out);
						}

						importer.setFileName(f.getFile().getFileName());
						importer.doImport(prop);
					}
				}
				else
				{
					Constructor<?> constructor = c.getConstructor(new Class[] {InputStream.class, HttpServletRequest.class, PrintWriter.class});
					Object o = constructor.newInstance(new Object[] {f.getFile().getInputStream(), request, out});
					if (o instanceof ExcelImportJXL)
					{
						if (!f.getFile().getFileName().endsWith(".xls"))
						{
							return printError(prop, out);
						}
						ExcelImportJXL i = (ExcelImportJXL)o;
						i.setFileName(f.getFile().getFileName());
						i.doImport(sheetName, prop);
					}
					else if (o instanceof ExcelImport)
					{
						if (!f.getFile().getFileName().endsWith(".xls"))
						{
							return printError(prop, out);
						}
						ExcelImport i = (ExcelImport)o;
						i.doImport(sheetName, prop);
					}
					else if (o instanceof ExcelXImport)
					{
						if (!f.getFile().getFileName().endsWith(".xlsx"))
						{
							return printError(prop, out);
						}
						ExcelXImport i = (ExcelXImport)o;
						i.doImport(sheetName, prop);
					}
					else if(o instanceof CSVImport)
					{
						if (!f.getFile().getFileName().endsWith(".csv") && !f.getFile().getFileName().endsWith(".txt"))
						{
							return printError(prop, out);
						}
						CSVImport i = (CSVImport)o;
						i.doImport(request, f.getFile().getInputStream());
					}
				}

				//ak je nastavene, ulozime po uspesnom ukonceni na dane miesto napr. /files/import_xls/
				String saveExcelTo = request.getParameter("saveExcelTo");
				if(Tools.isNotEmpty(saveExcelTo))
				{
					SimpleDateFormat sdf = new SimpleDateFormat("dd_MM_yyyy-HH_mm_ss");
					Integer ind = f.getFile().getFileName().lastIndexOf("\\");
					String fileName = f.getFile().getFileName().substring(++ind);
					fileName = DocTools.removeChars(fileName, true);
					saveExcelTo = saveExcelTo.endsWith("/") ? saveExcelTo : saveExcelTo+"/";

					IwcmFile dir = new IwcmFile(Tools.getRealPath(saveExcelTo));
					if(!dir.exists())
						dir.mkdirs();

					String cesta = saveExcelTo+sdf.format(Tools.getNow())+"_"+fileName;
					IwcmFile suborFile = new IwcmFile(Tools.getRealPath(cesta));

					if (!suborFile.exists())
						suborFile.createNewFile();

					IwcmFsDB.writeFiletoDest(f.getFile().getInputStream(), new File(suborFile.getPath()), f.getFile().getFileSize());
					Logger.println(this,"ImportXLSAction: kopia XLS "+f.getFile().getFileName()+" nakopirovana do "+cesta);
					out.println("<p><strong>"+prop.getText("import_excel_action.kopia_ulozena_do", f.getFile().getFileName())+": </strong><a href=\""+cesta+"\" target=\"_blank\">"+cesta+"</a></p>");
				}
			}
			catch (Exception e)
			{
				sk.iway.iwcm.Logger.error(e);
				out.println("<hr>ERROR: "+e.getMessage());
			}
		}

		out.println("<hr><strong>"+prop.getText("file.import_done")+"</strong><br/><br/>");


		if (request.getAttribute("disableReload")==null)
        {
            out.println("<script type='text/javascript'>try { window.opener.location.reload(); } catch (e) { console.log(e); }</script>");
        }


		if (setAdminLayout)
        {
            out.println("<style type='text/css'>#btnOk {display: none; } </style>");
            request.getRequestDispatcher("/admin/layout_bottom_dialog.jsp").include(request, response);
        }
		else {
            out.println("<hr>"+prop.getText("file.import_done"));
            out.println("</body></html>");
        }

		return(null);
		//return (mapping.findForward("reloadParentClose"));
	}

	private ActionForward printError(Prop prop, PrintWriter out)
	{
		out.println("<h2>"+prop.getText("file.import.bad_file_type")+"</h2>");
		out.println("</body></html>");
		return null;
	}
}
