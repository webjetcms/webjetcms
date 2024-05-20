package sk.iway.spirit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.Query;
import javax.servlet.http.HttpSession;

import org.eclipse.persistence.expressions.Expression;
import org.eclipse.persistence.expressions.ExpressionBuilder;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.ReadAllQuery;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.users.UsersDB;
import sk.iway.iwcm.utils.Pair;
import sk.iway.spirit.model.Media;
import sk.iway.spirit.model.MediaGroupBean;

/**
 *  MediaDB.java - praca s mediami k roznym tabulkam
 *
 *@Title        WebJET - SpiritDesign
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.7 $
 *@created      Date: 6.9.2005 12:57:18
 *@modified     $Date: 2010/02/17 11:37:51 $
 */
public class MediaDB extends JpaDB<Media>
{
	public MediaDB()
	{
		super(Media.class);
	}

	/**
	 * Ziska zadane medium
	 * @deprecated - pouzite verziu bez session
	 */
	@Deprecated
	public static Media getMedia(HttpSession session, int mediaId)
	{
		return getMedia(mediaId);
	}

	/**
	 * Ziska zadane medium
	 */
	public static Media getMedia(int mediaId)
	{
		return JpaTools.findFirstByMatchingProperty(Media.class, "id", mediaId);
	}

	/**
	 * Ziska zoznam medii pre danu tabulku, kluc a skupinu
	 * @param tableName - nazov tabulky
	 * @param pkId - primarny kluc, podla ktoreho sa vybera (alebo -1)
	 * @param mediaGroup - skupina pre ktoru data vyberame (alebo null)
	 */
	public static List<Media> getMedia(HttpSession session, String tableName, int pkId, String mediaGroup)
	{
		return getMedia(session, tableName, pkId, mediaGroup, 0);
	}

	public static List<Media> getMedia(HttpSession session, String tableName, int pkId, String mediaGroup, long lastUpdate)
	{
		return getMedia(session, tableName, pkId, mediaGroup, lastUpdate, true);
	}

	public static List<Media> getMedia(HttpSession session, String tableName, int pkIdParam, String mediaGroup, long lastUpdate, boolean checkAvailability)
	{
		List<MediaGroupBean> selectedGroups = new ArrayList<>();

		try
		{
			if (Tools.isNotEmpty(mediaGroup))
			{
				if(Tools.isNotEmpty(mediaGroup)){
					String[] groupsArr = mediaGroup.split(",");
					for(String g : groupsArr){
						MediaGroupBean group = null;
						if(Tools.getIntValue(g, -1) > -1){
							group = MediaDB.getGroup(Tools.getIntValue(g, -1));
						}
						else{
							group = MediaDB.getGroup(g);
						}
						if(group!=null){
							selectedGroups.add(group);
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
		    sk.iway.iwcm.Logger.error(ex);
		}


		return getMediaByMediaGroups(session, tableName, pkIdParam, selectedGroups, lastUpdate, checkAvailability);
	}

	/**
	 * Vrati zoznam medii podla listu skupin
	 * @param session
	 * @param tableName
	 * @param pkIdParam
	 * @param mediaGroups
	 * @param lastUpdate
	 * @param checkAvailability
	 * @return
	 */
	public static List<Media> getMediaByMediaGroups(HttpSession session, String tableName, int pkIdParam, List<MediaGroupBean> mediaGroups, long lastUpdate, boolean checkAvailability)
	{
	    int pkId = pkIdParam;
        if ("documents".equals(tableName))
        {
            //podpora pre stranky vo viacerych adresaroch
            int masterId = MultigroupMappingDB.getMasterDocId(pkId);
            if (masterId>0) pkId = masterId;
        }

		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		try
		{
			ExpressionBuilder builder = new ExpressionBuilder();
			ReadAllQuery dbQuery = new ReadAllQuery(Media.class, builder);

			Expression expr = builder.get("mediaFkTableName").equal(tableName);
			//fast loading preto, aby na pluske nemohli nacitat celu DB pri preview neulozeneho clanku
			if (pkId > 0 || Constants.getBoolean("galleryUseFastLoading")==true) expr = expr.and(builder.get("mediaFkId").equal(Integer.valueOf(pkId)));

			if (mediaGroups!=null && mediaGroups.size()>0) expr = expr.and(builder.anyOf("groups").in(mediaGroups));

			dbQuery.setSelectionCriteria(expr);
			//dbQuery.addAscendingOrdering("mediaGroup");
			dbQuery.addAscendingOrdering("mediaSortOrder");
			dbQuery.dontUseDistinct();

			Query query = em.createQuery(dbQuery);

			List<Media> records = JpaDB.getResultList(query);

			if (lastUpdate > 0) records = filterByLastUpdateDate(records, lastUpdate);
			if (checkAvailability && "documents".equals(tableName)) records = filterByAvailability(records, session);

			return records;

		}catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}finally{
			em.close();
		}

		return Collections.emptyList();

	}

	/**
	 * Odfiltruje media starsie ako zadany datum
	 * @param mediaAll - list medii
	 * @param lastUpdate - datum zmeny web stranky, vratia sa len novsie ako tento datum
	 */
	public static List<Media> filterByLastUpdateDate(List<Media> mediaAll, long lastUpdate)
	{
		List<Media> filtered = new ArrayList<>();

		for (Media m : mediaAll)
		{
			if (m.getLastUpdate()==null || m.getLastUpdate().getTime()<=lastUpdate)
			{
				filtered.add(m);
			}
		}

		return filtered;
	}

	/**
	 * Skontroluje existenciu a prava na jednotlive linky (interne, externe ponecha ako su)
	 * @param mediaAll
	 * @param session
	 * @return
	 */
	public static List<Media> filterByAvailability(List<Media> mediaAll, HttpSession session)
	{
		String mode = Constants.getString("mediaCheckAvailabilityMode");
		if (Tools.isEmpty(mode) || "none".equals(mode)) return mediaAll;

		DebugTimer dt = new DebugTimer("MediaDB.filterByAvailability");

		List<Media> filtered = new ArrayList<>();

		DocDB docDB = DocDB.getInstance();
		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		Identity user = UsersDB.getCurrentUser(session);

		for (Media m : mediaAll)
		{
			dt.diff("checking "+m.getMediaLink());

			//externe, alebo nedefinovane linky pridame automaticky
			if (Tools.isEmpty(m.getMediaLink()) || m.getMediaLink().toLowerCase().startsWith("http") || m.getMediaLink().startsWith("/images") || m.getMediaLink().startsWith("javascript:"))
			{
				filtered.add(m);
				continue;
			}

			if (m.getMediaLink().startsWith("/files") || m.getMediaLink().startsWith("/archiv"))
			{
				IwcmFile f = new IwcmFile(Tools.getRealPath(m.getMediaLink()));
				if (!f.exists()) continue;

				if ("fast".equals(mode))
				{
					filtered.add(m);
					continue;
				}

				EditForm protectedForm = PathFilter.isPasswordProtected(m.getMediaLink(), null, session);
				if (protectedForm == null || protectedForm.isAccessibleFor(user))
				{
					filtered.add(m);
				}
				continue;
			}

			if (rb != null)
			{
				//asi ide o web stranku, skontrolujme jej existenciu a prava
				int accessDocId = docDB.getVirtualPathDocId(m.getMediaLink(), rb.getDomain());

				//stranka neexistuje (je zmazana)
				if (accessDocId < 1) continue;

				if ("fast".equals(mode))
				{
					filtered.add(m);
					continue;
				}

				DocDetails accessDoc = docDB.getBasicDocDetails(accessDocId, false);
				if (accessDoc == null) continue;

				if (DocDB.canAccess(accessDoc, user))
				{
					filtered.add(m);
				}
				continue;
			}

			//je to nejaky iny podivny subor
			filtered.add(m);
		}

		dt.diff("done");

		return filtered;
	}

	/**
	 * Kontrola na existenciu suboru pre vypis v editore
	 */
	public static boolean isExists(Media m)
	{
		if (Tools.isEmpty(m.getMediaLink()) || m.getMediaLink().toLowerCase().contains("://") || m.getMediaLink().startsWith("/images") || m.getMediaLink().startsWith("javascript:"))
		{
			return true;
		}

		try
		{
			if (m.getMediaLink().startsWith("/files") || m.getMediaLink().startsWith("/archiv") || m.getMediaLink().startsWith("/images"))
			{
				IwcmFile f = new IwcmFile(Tools.getRealPath(m.getMediaLink()));
				return f.exists();
			}
		}
		catch (Exception e)
		{
			return false;
		}

		RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (rb != null)
		{
			DocDB docDB = DocDB.getInstance();

			//asi ide o web stranku, skontrolujme jej existenciu a prava
			int accessDocId = docDB.getVirtualPathDocId(m.getMediaLink(), rb.getDomain());

			//stranka neexistuje (je zmazana)
			if (accessDocId < 1) return false;
		}

		return true;
	}

	/**
	 * Aktualizuje media, ktore obsahuje linku povodneho URL na nove URL
	 */
	public static void updateLink(String origUrl, String newUrl)
	{
		List<Media> mediaLink = JpaTools.findByMatchingProperty(Media.class, "mediaLink", origUrl);
		if (mediaLink != null)
		{
			for (Media m : mediaLink)
			{
				if (m.getMediaLink().equalsIgnoreCase(origUrl))
				{
					m.setMediaLink(newUrl);
					m.save();
				}
			}
		}


	}

	/**
	 * Aktualizuje media, ktore obsahuje thumb obrazok povodneho URL na nove URL
	 */
	public static void updateThumb(String origUrl, String newUrl)
	{
		List<Media> mediaLink = JpaTools.findByMatchingProperty(Media.class, "mediaThumbLink", origUrl);
		if (mediaLink != null)
		{
			for (Media m : mediaLink)
			{
				if (m.getMediaThumbLink().equalsIgnoreCase(origUrl))
				{
					m.setMediaThumbLink(newUrl);
					m.save();
				}
			}
		}
	}

	public static List<MediaGroupBean> getGroups()
	{
		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		List<MediaGroupBean> groups = new ArrayList<>();
		try {
			ReadAllQuery readAllQuery = new ReadAllQuery(MediaGroupBean.class);
			Expression e = readAllQuery.getExpressionBuilder();
			readAllQuery.setSelectionCriteria(e);
			Query query = em.createQuery(readAllQuery);
			groups = JpaDB.getResultList(query);
		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		} finally {
			em.close();
		}
		return filterDomainId(groups, true);
	}


	/**
	 * Returns media groups for given WEBPAGES group
	 * @param groupId - webpages group id
	 * @return
	 */
	public static List<MediaGroupBean> getGroups(int groupId){
		List<MediaGroupBean> groups = new ArrayList<>();
		GroupsDB groupsDB = GroupsDB.getInstance();

		for(MediaGroupBean mediaGroup : getGroups()){
			String availableGroups =  mediaGroup.getAvailableGroups();
			if(availableGroups == null || "".equals(availableGroups)){
				groups.add(mediaGroup);
			}else{

				boolean contains = false;
				for(String group : availableGroups.split(",")){
					int id = Tools.getIntValue(group, -1);
					List<GroupDetails> subgroups = groupsDB.getGroupsTree(id, true, true, false);
					for(GroupDetails subgroup: subgroups){
						if (subgroup.getGroupId() == groupId){
							contains = true;
							break;
						}
					}
					if (contains) break;
				}
				if(contains){
					groups.add(mediaGroup);
				}
			}
		}

		return groups;
	}

	/**
	 * Filter Media Groups by domain in multi domain environment.
	 * In MultiWeb returns only groups with availableGroups=null or availableGroups with domainId.
	 * @param all
	 * @param addEmpty - true if add grups with empty perms even in MultiWeb (it it automatically added for controller domain)
	 * @return
	 */
	public static List<MediaGroupBean> filterDomainId(List<MediaGroupBean> all, boolean addEmpty) {
        List<MediaGroupBean> filtered = new ArrayList<>();

        if (InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) {
            for (MediaGroupBean mediaGroupBean : all) {
                if (Tools.isEmpty(mediaGroupBean.getAvailableGroups())) {
					//show empty on multidomain or in MultiWeb controller domain
                    if (addEmpty || InitServlet.isTypeCloud()==false || CloudToolsForCore.isControllerDomain()) filtered.add(mediaGroupBean);
                }
                else {
                    int[] groupIds = Tools.getTokensInt(mediaGroupBean.getAvailableGroups(), ",");
                    GroupsDB groupsDB = GroupsDB.getInstance();
                    String domain = CloudToolsForCore.getDomainName();
                    for (int groupId : groupIds) {
                        GroupDetails group = groupsDB.getGroup(groupId);
                        if (group!=null && domain.equals(group.getDomainName())) {
                            filtered.add(mediaGroupBean);
                            break;
                        }
                    }
                }
            }
        } else {
            filtered = all;
        }
		return filtered;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static MediaGroupBean getGroup(int mediaGroupId)
	{
		Pair[] properties = new Pair[]{
				new Pair<>("id", mediaGroupId)
		};
		MediaGroupBean group = JpaTools.findFirstByProperties(MediaGroupBean.class, properties);
		return group;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static MediaGroupBean getGroup(String mediaGroupName)
	{
		Pair[] properties = new Pair[]{
				new Pair<>("mediaGroupName", mediaGroupName)
		};
		MediaGroupBean group = JpaTools.findFirstByProperties(MediaGroupBean.class, properties);
		return group;
	}

	public static boolean saveMediaGroup(int mediaGroupId, String mediaGroupName, String availableGroups){
		//edit
		if(mediaGroupId > -1){
			MediaGroupBean mediaGroup = MediaDB.getGroup(mediaGroupId);
			if(mediaGroup!=null){
				mediaGroup.setAvailableGroups(availableGroups);
				mediaGroup.setMediaGroupName(mediaGroupName);
				return mediaGroup.save();
		} else return false;
		}else{
			MediaGroupBean newMediaGroup = new MediaGroupBean();
			newMediaGroup.setMediaGroupName(mediaGroupName);
			newMediaGroup.setAvailableGroups(availableGroups);
			return newMediaGroup.save();
		}


	}

	public static boolean deleteMediaGroup(int mediaGroupId){
		MediaGroupBean mediaGroup = getGroup(mediaGroupId);
		if(mediaGroup!=null){
			//mediaGroup.setMedias(null);
			mediaGroup.save();
			return   mediaGroup.delete();
		}
		else return false;
	}
	public static Media duplicateMedia(int mediaId){
		Media media = getMedia(mediaId);
		if(media!=null){
			media.setId(-1L);
			media.setMediaSortOrder(MediaDB.getLastOrder(media.getMediaFkId(), "documents")+10);
			//media.save(); // chcem iba vratit duplikat
			return media;
		}
		return null;
	}

	public static void importGropus(){
		List<Media> media = new ArrayList<>();

		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		try {
			ReadAllQuery readAllQuery = new ReadAllQuery(Media.class);
			Expression e = readAllQuery.getExpressionBuilder();
			readAllQuery.setSelectionCriteria(e);
			Query query = em.createQuery(readAllQuery);
			media = JpaDB.getResultList(query);
		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		} finally {
			em.close();
		}

		Logger.info(MediaDB.class, "Size: "+media.size());
		for (Media m: media){
			Logger.info(MediaDB.class, "Media ID: "+m.getMediaId());
			String oldGroups = m.getMediaGroup();
			if(oldGroups == null) continue;
			String[] oldGroupsArray = oldGroups.split(",");

			for(String g: oldGroupsArray){
				MediaGroupBean newGroup = MediaDB.getGroup(g);
				if(newGroup == null){
					newGroup = new MediaGroupBean();
					newGroup.setMediaGroupName(g);
					Logger.info(MediaDB.class, "---> Vytvaram novu media skupinu: "+newGroup.getMediaGroupName()+"<br>");

				}
				m.addGroup(newGroup);
				Logger.info(MediaDB.class, "Priradujem media skupinu: "+newGroup.getMediaGroupName()+" ku mediu "+m.getMediaTitleSk()+"<br>");
				m.save();
			}
		}
	}

	/**
	 *
	 * @param mediaFkId required
	 * @return najvyssi sortOrder medii v stranke
	 */
	public static int getLastOrder(int mediaFkId, String mediaFkTableName){
		int sortOrder = 0;
		if(mediaFkId == -1) return  sortOrder;

		JpaEntityManager em = JpaTools.getEclipseLinkEntityManager();
		Media medium = null;

		try {
			ExpressionBuilder builder = new ExpressionBuilder();

			ReadAllQuery dbQuery = new ReadAllQuery(Media.class, builder);
			Expression expr = builder.get("mediaFkId").equal(mediaFkId);
			expr = expr.and(builder.get("mediaFkTableName").equal(mediaFkTableName));

			dbQuery.setSelectionCriteria(expr);
			dbQuery.addOrdering(builder.get("mediaSortOrder").descending());

			Query query = em.createQuery(dbQuery);
			if (query.getResultList().size()>0)	medium = (Media) query.getResultList().get(0);

		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		} finally {
			em.close();
		}

		if(medium != null) sortOrder = medium.getMediaSortOrder();

		return sortOrder;
	}

    public static List<Media> getMedia(DocDetails doc, String groups) {
        int docId = doc.getDocId();
        List<Media> files = new ArrayList<>();
        files = MediaDB.getMedia(null, "documents", docId, groups, doc.getDateCreated());
        return files;
    }

	public static JSONObject toJsonObject(List<Media> mediaList)
	{
		JSONObject output = new JSONObject();

		try
		{
			JSONArray outputJsonArray = toJsonArray(mediaList);
			output.put("data", outputJsonArray);
		} catch (JSONException e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return output;
	}

	public static JSONArray toJsonArray(List<Media> mediaList)
	{
		JSONArray outputJsonArray = new JSONArray();
		mediaList.forEach(medium->{
			JSONObject j = new JSONObject();
			JSONArray groupArray = new JSONArray();
			medium.getGroups().forEach(group->{
				JSONObject g = new JSONObject();
				try {
					g.put("label", group.getMediaGroupName());
				} catch (JSONException e) {
					sk.iway.iwcm.Logger.error(e);
				}
				try {
					g.put("value", group.getMediaGroupId());
				} catch (JSONException e) {
					sk.iway.iwcm.Logger.error(e);
				}
				groupArray.put(g);
			});
			try {
				j.put("id", medium.getMediaId());
				j.put("order", medium.getMediaSortOrder());
				j.put("title", medium.getMediaTitleSk() != null ? medium.getMediaTitleSk() : "");
				j.put("thumbLink", medium.getMediaThumbLink());
				j.put("link_url", medium.getMediaLink());
				j.put("link_exist", (MediaDB.isExists(medium) && Tools.isNotEmpty(medium.getMediaLink()))?"true": "false");

				j.put("group", medium.getGroupsToString());
				j.put("groupsArray",groupArray);

				outputJsonArray.put(j);
			} catch (JSONException e) {
				sk.iway.iwcm.Logger.error(e);
			}
		});


		return outputJsonArray;
	}
}
