package sk.iway.iwcm.stripes;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.system.zip.ZipEntry;
import sk.iway.iwcm.system.zip.ZipInputStream;
import sk.iway.iwcm.users.UsersDB;

/**
 *  SyncArchiveActionBean.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2009
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 20.1.2009 11:50:27
 *@modified     $Date: 2009/05/04 09:26:01 $
 */
public class SyncArchiveActionBean implements ActionBean
{
	private ActionBeanContext context;
	private FileBean archive;

	public FileBean getArchive()
	{
		return archive;
	}

	public void setArchive(FileBean archive)
	{
		this.archive = archive;
	}
	@Override
	public ActionBeanContext getContext()
	{
		return context;
	}
	@Override
	public void setContext(ActionBeanContext cntxt)
	{
		context=cntxt;

	}
	@DefaultHandler
	public Resolution sync()
	{
		Identity user = UsersDB.getCurrentUser(getContext().getRequest());
		if (user == null || user.isAdmin()==false) return(new ForwardResolution("/components/maybeError.jsp"));

		try
		{
			String virtualDir="/WEB-INF/tmp/" + new SimpleDateFormat("dd.MM.yyyy-HHmm").format(new Date());
			/*if (Constants.getBoolean("multiDomainEnabled"))
			{
				virtualDir="/files/"+MultiDomainFilter.getDomainAlias(DocDB.getDomain(context.getRequest()))+"/protected/backup/"+ new SimpleDateFormat("dd.MM.yyyy-hhmm").format(new Date());
			}*/
			String backupDir = Tools.getRealPath(virtualDir);
			IwcmFile backupDirFile = new IwcmFile(backupDir);
			if (backupDirFile.exists()==false)
			{
				backupDirFile.mkdirs();
			}

			ZipInputStream zis = new ZipInputStream(archive.getInputStream());
			ZipEntry entry;
			IwcmFile outFile;

			while ((entry = zis.getNextEntry()) != null)
			{
				if (entry.getName().endsWith("/")) //je to adresar
				{
					outFile = new IwcmFile(backupDir + java.io.File.separatorChar + entry.getName());
					outFile.mkdirs();
				}
				else
				{
					if (entry.getName().contains("/"))
					{
						int index = entry.getName().lastIndexOf('/') + 1;
						String realDir = backupDir + java.io.File.separatorChar + entry.getName().substring(0, index).replace('/', java.io.File.separatorChar);
						String realPath = realDir + entry.getName().substring(index);
						IwcmFile outDir = new IwcmFile(realDir);
						outDir.mkdirs();
						outFile = new IwcmFile(realPath);
					}
					else
					{
						outFile = new IwcmFile(backupDir + java.io.File.separatorChar + entry.getName());
					}
					IwcmFsDB.writeFileToDisk(zis, new java.io.File(outFile.getPath()), false);
				}
				//new IwcmFile(outFile.getPath()).setLastModified(entry.getTime());//nastavime lastModified podla suboru zo zip archivu
			}
			zis.close();
			context.getRequest().setAttribute("syncDir",virtualDir);

			context.getRequest().setAttribute("compareBy", Tools.getParameter(context.getRequest(), "compareBy"));
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return new ForwardResolution("/components/maybeError.jsp");
	}
}
