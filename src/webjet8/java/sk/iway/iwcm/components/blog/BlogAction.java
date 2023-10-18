package sk.iway.iwcm.components.blog;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.BlogTools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.EditorForm;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.stripes.WebJETActionBean;
import sk.iway.iwcm.users.UsersDB;

/**
 *  BlogAction.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 29.9.2011 16:09:58
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class BlogAction extends WebJETActionBean
{
	private EditorForm ef;
	
	/**
	 * Test ci je mozne aktualnu stranku editovat
	 * @param request
	 * @return
	 */
	public static boolean isEditable(HttpServletRequest request)
	{
		Identity user = UsersDB.getCurrentUser(request);
		GroupDetails actualGroup = (GroupDetails)request.getAttribute("pageGroupDetails");
		boolean canEdit = false;

		if (user != null && actualGroup != null) canEdit = BlogTools.isEditable(actualGroup.getGroupId(), user);

		return canEdit;
	}
	
	/**
	 * Vrati zoznam dostupnych perex skupin pre dany adresar, filtruje perex skupiny co nemaju
	 * nastaveny ziaden adresar aby sa zobrazili len tie, co skutocne boli priradene k blogu
	 * @param groupId
	 * @return
	 */
	public static List<PerexGroupBean> getPerexGroups(int groupId)
	{
		List<PerexGroupBean> perexGroups = DocDB.getInstance().getPerexGroups(groupId);
		//odstran vseobecne (bez priradenia)
		List<PerexGroupBean> perexGroupsFiltered = new ArrayList<PerexGroupBean>();
		for (PerexGroupBean pgb : perexGroups)
		{
			if (Tools.isNotEmpty(pgb.getAvailableGroups())) perexGroupsFiltered.add(pgb);
		}
		return perexGroupsFiltered;
	}
	
	public static String checkData(String data)
	{
		if (data == null) data = "";
		data = Tools.replace(data, "!INCLUDE(", "! INCLUDE(");
		return data;
	}
	
	public int getTopicId()
	{		
		if (ef == null) return -1;
		int topicId = -1;
		if (Tools.isNotEmpty(ef.getPerexGroupString()))
		{
			topicId = Tools.getIntValue(ef.getPerexGroup()[0], -1);
		}
		return topicId;
	}
	
	public void setTopicId(int topicId)
	{
		if (ef != null) ef.setPerexGroupString(String.valueOf(topicId));
	}
	
	public boolean isDiscussion()
	{
		if (ef.getFieldA().indexOf("NODISCUSSION")!=-1) return false;
		return true;
	}
	
	public void setDiscussion(boolean discussion)
	{
		if (ef != null)
		{
			ef.setFieldA(Tools.replace(ef.getFieldA(), "NODISCUSSION", ""));
			if (discussion == false) ef.setFieldA( ("NODISCUSSION "+ef.getFieldA()).trim() );
		}
	}
	
	/**
	 * Ulozenie stranky do databazy
	 * @return
	 */
	public Resolution btnSave()
	{
		Logger.debug(BlogAction.class, "btnSave");
		
		Prop prop = Prop.getInstance(getRequest());
		
		Identity user = UsersDB.getCurrentUser(getRequest());
		DocDetails actualDoc = (DocDetails)getRequest().getAttribute("docDetails");
		if (isEditable(getRequest())==false || actualDoc == null || user == null)
		{
			getRequest().setAttribute("errorText", prop.getText("admin.editor_dir.dontHavePermsForThisOperation"));
			return (new ForwardResolution("/components/maybeError.jsp"));
		}
		
		String data = checkData(getRequest().getParameter("data"));
		
		EditorForm origForm;
		if ("true".equals(getRequest().getParameter("isNew")))
		{
			origForm = EditorDB.getEditorForm(getRequest(), -1, -1, actualDoc.getGroupId());
			origForm.setDocId(-1);
			
		}
		else
		{
			origForm = EditorDB.getEditorForm(getRequest(), actualDoc.getDocId(), -1, actualDoc.getGroupId());
		}
		
		data = data.replace("!INCLUDE(/components/forum/forum.jsp, type=open, noPaging=true, sortAscending=true)!", "");
		data = data.replace("! INCLUDE(/components/forum/forum.jsp, type=open, noPaging=true, sortAscending=true)!", "");
		if(isDiscussion()) 
		{
			data += "\n\n!INCLUDE(/components/forum/forum.jsp, type=open, noPaging=true, sortAscending=true)!";			
		}
		
		
		origForm.setAuthorId(user.getUserId());
		origForm.setTitle(ef.getTitle());
		origForm.setNavbar(ef.getTitle());
		origForm.setData(data);
		origForm.setAvailable(ef.isAvailable());
		origForm.setPublish("1");
		origForm.setPerexGroupString(ef.getPerexGroupString());
		origForm.setFieldA(ef.getFieldA());
		
		if (Tools.isEmpty(origForm.getPublishStartTime())) origForm.setPublishStartTime(Tools.formatTime(Tools.getNow()));
		if (Tools.isEmpty(origForm.getPublishStart())) origForm.setPublishStart(Tools.formatDate(Tools.getNow()));		
		
		int historyId = EditorDB.saveEditorForm(origForm, getRequest());
		if (historyId < 1) getRequest().setAttribute("errorText", prop.getText("html_area.insert_image.error_occured"));
		else
		{
			//aktualizuj actualDoc
			actualDoc.setData(origForm.getData());
			actualDoc.setTitle(origForm.getTitle());
			actualDoc.setAvailable(origForm.isAvailable());
			actualDoc.setPerexGroupString(origForm.getPerexGroupString());
			actualDoc.setFieldA(origForm.getFieldA());
			
			getRequest().setAttribute("doc_data_blog", origForm.getData());
			
			DocDB docDB = DocDB.getInstance();
			getRequest().setAttribute("errorText", "<script type='text/javascript'>window.location.href='"+Tools.addParameterToUrl(docDB.getDocLink(origForm.getDocId(), origForm.getExternalLink(), getRequest()), "blogEdit", "1")+"';</script>");

		}
		
		return (new ForwardResolution("/components/maybeError.jsp"));
		//return (new ForwardResolution("/components/reloadParentClose.jsp"));
	}
	
	public BlogAction()
	{
		super();
		ef = new EditorForm();		
	}

	public EditorForm getEf()
	{
		return ef;
	}

	public void setEf(EditorForm ef)
	{
		this.ef = ef;
	}
}
