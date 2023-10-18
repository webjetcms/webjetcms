package sk.iway.iwcm.search;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.util.ResponseUtils;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;

/**
 * vyhladavanie vo WEB strankach
 */
public class DocDetailsSearch implements Searchable
{
	protected enum SEARCH_TYPE { DOC, FILE };
	protected SEARCH_TYPE type;

	public DocDetailsSearch()
	{
		type = SEARCH_TYPE.DOC;
	}

	public List<SearchResult> search(String text, HttpServletRequest request)
	{
		List<SearchResult> result = new ArrayList<>();

		String lng = PageLng.getUserLng(request);
		Prop prop = Prop.getInstance(lng);

		Identity user = UsersDB.getCurrentUser(request);
		List<DocDetails> pages = new ArrayList<DocDetails>();
		String url = request.getParameter("url");
		DocDB docDB = DocDB.getInstance();
		GroupsDB groupsDB = GroupsDB.getInstance();

		int groupId = Tools.getIntValue(request.getParameter("groupId"), -1);
		int userEditableGroups[] = Tools.getTokensInt(user.getEditableGroups(), ",");
		if (groupId<1 && userEditableGroups!=null && userEditableGroups.length > 0)
			groupId = userEditableGroups[0];

		String whereSql = null;
		if (type == SEARCH_TYPE.DOC) whereSql = "AND (external_link IS NULL OR external_link NOT LIKE '/files/%')";
		else if (type == SEARCH_TYPE.FILE) whereSql = "AND external_link LIKE '/files/%'";

		if (Tools.isNotEmpty(url))
		{
			pages = docDB.searchTextUrl(url, groupId);
			text = url;
		}
		else
		{
			if(groupId > 0)
				pages = docDB.searchTextAll(text, groupId, whereSql);
			else
				pages = docDB.searchTextAll(text, -1, whereSql);
		}

		for(DocDetails doc:pages)
		{
			String label = "";

			if (type == SEARCH_TYPE.DOC)
         {
            if (Tools.isNotEmpty((doc).getPasswordProtected()))
               label += "<img src=\"/admin/images/lock.gif\" alt=\"<iwcm:text key=\"document.isLocked\"/>";
            label += "<a class=\"groups" + doc.isAvailable() + "\" href=\"javascript:openWebJETEditor(" + doc.getDocId() + ");\">" + ResponseUtils.filter(doc.getTitle()) + " " + "</a>";
            if (Tools.isNotEmpty((doc).getPublishStartString()))
            {
               label += "<span title=\"" + prop.getText("document.start.publication") + "\">";
               label += doc.getPublishStartString() + " " + doc.getPublishStartTimeString() + "</span>";
            }
            if (Tools.isNotEmpty(doc.getPublishEndString()))
            {
               if (Tools.isNotEmpty(doc.getPublishStartString()))
                  label += " - ";
               label += "<span title=\"" + prop.getText("document.end.publication") + "\">";
               label += doc.getPublishEndString() + " " + doc.getPublishEndTimeString() + "</span>";
            }
         }
         else if (type == SEARCH_TYPE.FILE)
         {
            label += "<a class=\"groups" + doc.isAvailable() + "\" href=\"" + ResponseUtils.filter(doc.getExternalLink()) + "\" target=\"_blank\">" + ResponseUtils.filter(doc.getTitle()) + " " + "</a>";
         }

			if (Tools.isNotEmpty(doc.getData()))
			{
				int start = doc.getData().indexOf(text);
				if (start == -1) start = 0;

            int end = start + 500;
            if (end>doc.getData().length()) end = doc.getData().length()-1;

            label += "<br/>"+ ResponseUtils.filter(doc.getData().substring(start, end));
			}

			SearchResult sr = new SearchResult();
			sr.setLabel(label);
         if (type == SEARCH_TYPE.DOC)
         {
            sr.setLocation(groupsDB.getPathLinkForward(doc.getGroupId(), null));
         }
         else if (type == SEARCH_TYPE.FILE)
         {
            String path = doc.getExternalLink();
            int lomka = path.indexOf("/");
            if (lomka>1) path = path.substring(0, lomka);
            sr.setLocation(path);
         }
			sr.setDate(doc.getDateCreatedString() + " " + doc.getTimeCreatedString());
			sr.setDocId(doc.getDocId());
			sr.setType(this.getClass().getCanonicalName());
			result.add(sr);
		}

		return result;
	}

	public static List<GroupDetails> getGroups(HttpServletRequest request)
	{
		List<GroupDetails> rootGroups = null;
		GroupsDB groupsDB = GroupsDB.getInstance();
		Identity user = UsersDB.getCurrentUser(request);
		int groupId = Tools.getIntValue(request.getParameter("groupId"), -1);
		int[] userEditableGroups = Tools.getTokensInt(user.getEditableGroups(), ",");

		if (userEditableGroups!=null && userEditableGroups.length>0)
		{
			rootGroups = new ArrayList<>();
			boolean mamSpecialny = true;
			for (int userGroupId : userEditableGroups)
			{
				GroupDetails ugd = groupsDB.getGroup(userGroupId);
				if (ugd != null) rootGroups.add(ugd);
				if (userGroupId == groupId) mamSpecialny = false;
			}
			if(mamSpecialny && groupId > 0)
			{
				GroupDetails group = groupsDB.getGroup(groupId);
				if (group != null) rootGroups.add(0, group);
			}
		}
		else
		{
			rootGroups = groupsDB.getGroups(0);
			if(groupId != -1)
			{
				GroupDetails group = groupsDB.getGroup(groupId);
				if (group != null) rootGroups.add(0, group);
			}
		}
		return rootGroups;
	}

	@Override
	public boolean canUse(HttpServletRequest request)
	{
		// TODO: Doriesit prava ci sa to ma userovi vobec zobrazovat
		return true;
	}
}
