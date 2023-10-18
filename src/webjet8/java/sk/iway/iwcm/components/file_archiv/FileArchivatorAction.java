package sk.iway.iwcm.components.file_archiv;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import sk.iway.iwcm.*;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.stripes.WebJETActionBean;
import sk.iway.iwcm.users.UsersDB;

/**
 * FileArchivatorAction.java
 *
 *
 *  Title       webjet7
 *  Company     Interway s.r.o. (www.interway.sk)
 *  Copyright   Interway s.r.o. (c) 2001-2015
 *  @author     $Author: jeeff $(prau)
 *  @version    Revision: 1.3 14.2.2015
 *  created     Date: 14.2.2015 9:21:33
 *  modified    Date: 14.2.2015 9:21:33
 *  Ticket Number: #17263
 */
public class FileArchivatorAction extends WebJETActionBean
{
	private FileArchivatorBean fab;

	@Override
	public void setContext(ActionBeanContext context)
	{
		super.setContext(context);
		Identity user = UsersDB.getCurrentUser(context.getRequest());
		if (user == null || !user.isAdmin())
		{
			Logger.debug(FileArchivatorAction.class, "User not loged, or user is not admin");
			return;
		}
		int id = Tools.getIntValue(getRequest().getParameter("fab.rpackageId"), -1); //manazer kategorii
		if(id < 1) id = Tools.getIntValue(getRequest().getParameter("edit"), -1); //editacia zaznamu
		// Logger.debug(FileArchivatorBean.class,"id from reqest: "+ id);
		if (id > 0)
		{
			fab = FileArchivatorDB.getInstance().getById(id);
			if(fab != null && fab.getId() > 0)
				fab.setNote(Tools.replace(Tools.replace(Tools.replace(Tools.replace(fab.getNote(),"&lt;","<"),"&gt;",">"),"&quot;","\""),"&nbsp;"," "));
		}
	}

	@HandlesEvent("save")
	public Resolution save()
	{
		if(!isAdminLogged())
		{
			return (new ForwardResolution("/admin/logon.jsp"));
		}

		FileArchivValidator fav = (FileArchivValidator)getRequest().getAttribute("validator");
		if(fav == null)
            fav = new FileArchivDefaultValidator();

		if(!fav.validateActionSave(fab,getRequest()))
            return (new ForwardResolution("/components/maybeError.jsp"));

		FileArchivatorBean oldFab = FileArchivatorDB.getInstance().getById(fab.getId());
		if(oldFab != null)
		{
			fab.setFileName(oldFab.getFileName());
			fab.setFilePath(oldFab.getFilePath());
			fab.setOrderId(oldFab.getOrderId());
			fab.setReferenceId(oldFab.getReferenceId());
			fab.setUserId(oldFab.getUserId());
		    fab.setDateInsert(oldFab.getDateInsert());
		    if(getRequest().getParameter("setMd5") != null) fab.setMd5(oldFab.getMd5());
		    fab.setUploaded(oldFab.getUploaded());
		    fab.setDateUploadLater(oldFab.getDateUploadLater());
		    fab.setEmails(oldFab.getEmails());
		    fab.setGlobalId(oldFab.getGlobalId());
		    fab.setFileSize(oldFab.getFileSize());
		    fab.setReferenceToMain(oldFab.getReferenceToMain());
			fab.setDomainId(CloudToolsForCore.getDomainId());
		}
		String changes = FileArchivatorKit.getPojoZmeny(fab,oldFab);
		boolean saved = fab.save();
		if(saved)
        {
            Adminlog.add(Adminlog.TYPE_FILE_EDIT,"File Archiv zmeny:"+changes,-1,-1);
        }
		return (new ForwardResolution("/components/reloadParentClose.jsp"));
	}

	public FileArchivatorBean getFab()
	{
		return fab;
	}

	public void setFab(FileArchivatorBean fab)
	{
		this.fab = fab;
	}
}
