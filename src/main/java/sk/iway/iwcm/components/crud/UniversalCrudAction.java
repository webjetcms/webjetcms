package sk.iway.iwcm.components.crud;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ActiveRecordBase;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.stripes.WebJETActionBean;

public class UniversalCrudAction extends WebJETActionBean
{
	@Override
	public void setContext(ActionBeanContext context)
	{
		super.setContext(context);
		Identity user = getCurrentUser();
		if (user!=null && user.isAdmin())
		{
			@SuppressWarnings("rawtypes")
			JpaDB dbInstance = (JpaDB)getRequest().getAttribute("universal_component_dbInstance");
			@SuppressWarnings("unchecked")
			Class<ActiveRecordBase> beanClass = (Class<ActiveRecordBase>)getRequest().getAttribute("universal_component_beanClass");
			int id = Tools.getIntValue(getRequest().getParameter("id"), 0);
			if (id>0)
				this.setObject(dbInstance.getById(id));
			else
			{
				try
				{
					this.object = beanClass.getDeclaredConstructor().newInstance();
				}
				catch (Exception ex)
				{
					Logger.debug(getClass(), "Instatiation error.");
				}
			}
		}
	}

	private ActiveRecordBase object;

	public Resolution bSave()
	{
		Identity user = getCurrentUser();
		if (user!=null && user.isAdmin())
		{
			Logger.debug(getClass(), "Teraz ukladaju objekt.");
			this.object.save();
			getRequest().setAttribute("saveOk", "ok");
		}
		return new ForwardResolution(RESOLUTION_CONTINUE);
	}

	public Resolution bDelete()
	{
		Identity user = getCurrentUser();
		if (user!=null && user.isAdmin())
		{
			Logger.debug(getClass(), "Teraz vymazava objekt data.");
			this.object.delete();
		}
		return new ForwardResolution(RESOLUTION_CONTINUE);
	}



	public ActiveRecordBase getObject() {
		return object;
	}

	public void setObject(ActiveRecordBase object) {
		this.object = object;
	}

}
