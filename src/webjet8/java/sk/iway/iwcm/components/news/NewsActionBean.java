package sk.iway.iwcm.components.news;

import static sk.iway.iwcm.components.news.FieldEnum.PUBLISH_END;
import static sk.iway.iwcm.components.news.FieldEnum.PUBLISH_START;
import static sk.iway.iwcm.components.news.criteria.DatabaseCriteria.and;
import static sk.iway.iwcm.components.news.criteria.DatabaseCriteria.greaterEqual;
import static sk.iway.iwcm.components.news.criteria.DatabaseCriteria.isEmpty;
import static sk.iway.iwcm.components.news.criteria.DatabaseCriteria.isNotNull;
import static sk.iway.iwcm.components.news.criteria.DatabaseCriteria.isNull;
import static sk.iway.iwcm.components.news.criteria.DatabaseCriteria.lessEqual;
import static sk.iway.iwcm.components.news.criteria.DatabaseCriteria.or;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.DateTool;
import org.json.JSONObject;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.controller.LifecycleStage;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.LabelValueDetails;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.news.NewsQuery.OrderEnum;
import sk.iway.iwcm.components.news.NewsQuery.SortEnum;
import sk.iway.iwcm.components.news.criteria.Criteria;
import sk.iway.iwcm.components.news.criteria.DatabaseCriteria;
import sk.iway.iwcm.components.news.criteria.DatabaseCriteria.CriteriaType;
import sk.iway.iwcm.components.news.templates.NewsTemplatesService;
import sk.iway.iwcm.components.news.templates.jpa.NewsTemplatesEntity;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.helpers.RequestHelper;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.json.JsonObjectGenerator;
import sk.iway.iwcm.system.json.ObjectFormaterFactory;
import sk.iway.iwcm.system.stripes.BindPageParams;
import sk.iway.iwcm.system.stripes.IncludeRequestWrapperInterface;
import sk.iway.iwcm.system.stripes.PageParamOnly;
import sk.iway.iwcm.system.stripes.WebJETActionBean;
import sk.iway.iwcm.tags.WriteTag;
import sk.iway.iwcm.tags.support.ResponseUtils;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 * Include actionBean pre nove news komponenty
 *
 * Title webjet7
 * Company Interway s.r.o. (www.interway.sk)
 * Copyright Interway s.r.o. (c) 2001-2015
 * @author Author: mbocko
 * @version Revision:
 * created Date: 12.11.2014
 * modified Date: 12.11.2014 8:54:56
 */
@BindPageParams
public class NewsActionBean extends NewsApp implements ActionBean, IncludeRequestWrapperInterface
{
	@PageParamOnly
	private String requestPerexGroupsName = "";

	@PageParamOnly
	private String newsName = "";

	@PageParamOnly
	private boolean perex = false;

	@PageParamOnly
	private boolean date = false;

	@PageParamOnly
	private boolean place = false;

	@PageParamOnly
	private boolean includeActualDoc = false;

	@PageParamOnly
	private boolean returnDocsWithAtributes = false;

	@PageParamOnly
	private int perexCrop = 0;

	@PageParamOnly
	private String tagClickLink;

	private String htmlOut;

	private int page = 1;

	private int totalPages = 1;

	@PageParamOnly
	private boolean searchAlsoProtectedPages = false;

	private NewsQuery newsQuery = new NewsQuery();

	private List<? extends DocDetails> newsList;

	private Map<String, Object> paginator = null;

	@PageParamOnly
	private Map<String, List<String>> filter = new HashMap<>();

	private Map<String, List<String>> search = new HashMap<>();

	@PageParamOnly
	private int cacheMinutes = 10;
	//private static final String CACHE_KEY = "news-list";

	private DocDetails doc;
	private Identity user;

	private String tag;

	//tu bude drzany zoznam expandnutych ID adresarov, naplni sa v addGroups
	private List<Integer> groupIdsExpanded = null;

	private String author;

	@DefaultHandler
	public Resolution news()
	{
		//Cache c = Cache.getInstance();

		doc = new RequestHelper(getRequest()).getDocument();
		user = getCurrentUser();

		if (user!=null && user.isAdmin() && Constants.getBoolean("cacheStaticContentForAdmin")==false)
		{
			//ak je user admin necachuj
			newsList = null;
		}

		availableOnly();
		addFilter();
		addSearch();
		addDocId();
		addLoadData();
		addPaging();
		addPasswordProtected();
		addGroups();
		addDuplicityFilter();

		addPerexGroups();
		addPerexNotRequired();
		addPublishType();
		addRequestCriteria();
		addOrder();

		if (newsList == null) {
			// newsQuery.addOrder(OrderEnum.ORDER_TITLE,
			// SortEnum.SORT_ASC).addOrder(OrderEnum.ORDER_PRIORITY,
			// SortEnum.SORT_ASC).addOrder(OrderEnum.ORDER_ID, SortEnum.SORT_DESC);
			newsList = returnDocsWithAtributes ? newsQuery.getNewsListWithAtributes() : newsQuery.getNewsList();
		}

		if (isCheckDuplicity())
		{
			//pridajme do zoznamu IDecka nacitanych web stranok vratane ich child stranok

			LinkedList<Integer> duplicityList = getDuplicityList();
			DocDB docDB = DocDB.getInstance();
			Map<Integer, Integer> slavesMaster = docDB.getSlavesMasterMappings();

			for (DocDetails docDetails: newsList)
			{
				duplicityList.add(docDetails.getDocId());

				if (slavesMaster != null) {
					//musime pridat aj child stranky
					Integer masterId = slavesMaster.get(Integer.valueOf(docDetails.getDocId()));
					//docid nie je slave, moze to byt ale realne master
					if (masterId == null) masterId = Integer.valueOf(docDetails.getDocId());
					Integer[] slaves = docDB.getMasterMappings().get(masterId);
					if (slaves != null) {
						duplicityList.addAll(Arrays.asList(slaves));
					}
				}
			}
		}

		cropPerex();
		fillTemplate();

		return new ForwardResolution(WebJETActionBean.RESOLUTION_CONTINUE);
	}




	/* KOKOS */
	private ActionBeanContext context;

	@Override
	public ActionBeanContext getContext()
	{
		return context;
	}
	@Override
	public void setContext(ActionBeanContext context)
	{
		this.context = context;
	}

	public HttpServletRequest getRequest()
	{
		return context.getRequest();
	}

	public Identity getCurrentUser()
	{
		return UsersDB.getCurrentUser(context);
	}



	/**
	 * Metoda pre odfiltrovanie duplicitnych noviniek.
	 * Vyuzite pre viacero instancii news velocity na tej istej stranke.
	 */
	private void addDuplicityFilter()
	{
		if (isCheckDuplicity())
		{
			LinkedList<Integer> duplicityList = getDuplicityList();
			DocDB docDB = DocDB.getInstance();

			//pridaj do zoznamu vylucenie child stranok z podadresarov, aby sme nemali 2x ten isty dokument v zozname
			if(docDB.getSlavesMasterMappings() != null)
			{
				//vytvor si zoznam stranok v podadresaroch
				List<DocDetails> docsInGroups = new ArrayList<>();
				for(int groupId : groupIdsExpanded)
				{
					docsInGroups.addAll(docDB.getBasicDocDetailsByGroup(groupId, 0));
				}

				if(docsInGroups.size() > 0)
				{
					HashMap<Integer, Boolean> docsInGroupsTable = new HashMap<>();
					for(DocDetails dd : docsInGroups)
					{
						//TRUE hovori, ze treba kontrolovat, kvoli performance, ked najdeme childy tak ich nastavime na FALSE
						//dalo by sa to aj vyhadzovat zo zoznamu, to by nam ale robilo problem v iteracii atd.
						docsInGroupsTable.put(Integer.valueOf(dd.getDocId()), Boolean.TRUE);
					}

					for (Map.Entry<Integer, Boolean> entry : docsInGroupsTable.entrySet())
					{
						if(Boolean.TRUE.equals(entry.getValue()))
						{
							Integer docId = entry.getKey();
							//ziskam zoznam docId, ktore su rovnake k danej stranke
							List<Integer> slavesMasterForDoc = new ArrayList<>();

							//najdi masterId
							Integer masterId = docDB.getSlavesMasterMappings().get(docId);
							if(masterId != null)
							{
								//ak sa master nachadza v zozname stranok, tak ponecham master miesto slave a vymazem vsetky slave stranky danej master stranky
								//v opacnom pripade vymazem vsetky ostatne slaves
								boolean containsMaster = Boolean.TRUE.equals(docsInGroupsTable.get(masterId));
								Integer[] slaves = docDB.getMasterMappings().get(masterId);
								if(slaves != null)
								{
									if(containsMaster) //aby preskocilo pri iteracii master
										docsInGroupsTable.put(masterId, Boolean.FALSE);
									for(Integer slave : slaves)
									{
										if(containsMaster || (!containsMaster && slave.intValue() != docId.intValue()))
											slavesMasterForDoc.add(slave);
									}
								}
							}
							else
							{
								//ak docId je master, tak pridam do duplicitnych vsetky jeho slaves
								Integer[] slaves = docDB.getMasterMappings().get(docId);
								if(slaves != null)
								{
									slavesMasterForDoc.addAll(Arrays.asList(slaves));
								}
							}
							//odstranim ich zo zonamu stranok
							if(slavesMasterForDoc != null && slavesMasterForDoc.size() > 0)
							{
								for(Integer sm: slavesMasterForDoc)
								{
									if(docsInGroupsTable.get(sm) != null)
									{
										docsInGroupsTable.put(sm, Boolean.FALSE);
										duplicityList.add(sm);
									}
								}
							}
						}
					}
				}
			}

			if (!duplicityList.isEmpty())	newsQuery.addCriteria(DatabaseCriteria.notIn(FieldEnum.DOC_ID, getDuplicityList()));
		}
	}

	/**
	 * Metoda vrati zoznam doc_id noviniek, ktore zobrazuju predchadzajuce news komponenty.
	 * @return
	 */
	private LinkedList<Integer> getDuplicityList()
	{
		final String REQUEST_KEY = "sk.iway.iwcm.components.news.duplicityList";
		@SuppressWarnings("unchecked")
		LinkedList<Integer> duplicityList = (LinkedList<Integer>)getRequest().getAttribute(REQUEST_KEY);
		if (duplicityList == null)
			duplicityList = new LinkedList<>();
		getRequest().setAttribute(REQUEST_KEY, duplicityList);
		return duplicityList;
	}

	private void availableOnly()
	{
		newsQuery.addCriteria(DatabaseCriteria.equal(FieldEnum.AVAILABLE, Boolean.TRUE));
	}

	private void addFilter()
	{
		if (!filter.isEmpty())
		{
			for (Map.Entry<String, List<String>> entry : filter.entrySet())
			{
				String key = entry.getKey();
				List<String> value = entry.getValue();
				// filter[title_eq]=kjbnkj, filter[title_eq]=hadghja
				String[] fieldOperator = Tools.getTokens(key, "_");
				Criteria cr = parseCriteria(fieldOperator[0], fieldOperator[1], value);
				if (cr != null) newsQuery.addCriteria(cr);
			}
		}
	}

	private void addSearch() {
		if (!search.isEmpty())
		{
			for (Map.Entry<String, List<String>> entry : search.entrySet())
			{
				String key = entry.getKey();
				List<String> value = entry.getValue();
				String[] fieldOperator = Tools.getTokens(key, "_");
				if(fieldOperator != null && fieldOperator.length == 2)
				{
					if (filter.containsKey(key))
					{
						Logger.debug(getClass(), "Nieje mozne vyhladavat pre vyraz '" + key + "' = '" + value + "'");
					}
					Criteria cr = parseCriteria(fieldOperator[0], fieldOperator[1], value);
					if (cr != null) newsQuery.addCriteria(cr);
				}
			}
		}
	}

	private void addDocId() {
		if (doc != null && !includeActualDoc)
		{
			newsQuery.addCriteria(DatabaseCriteria.notEqual(FieldEnum.DOC_ID, doc.getDocId()));
		}
	}

	private void addLoadData() {
		newsQuery.setLoadData(isLoadData());
	}

	private void addPaging()
	{
		if (isPaging()) {
			newsQuery.setPageSize(pageSize).setPage(page);
			if (offset>0) newsQuery.setInitialOffset(offset);
		}
		else if (pageSize > 0)
		{
			newsQuery.setPageSize(pageSize);
			if (offset>0) newsQuery.setInitialOffset(offset);
		}
	}

	private void addPasswordProtected()
	{
		if (user == null && searchAlsoProtectedPages == false) {
			newsQuery.addCriteria(isEmpty(FieldEnum.PASSWORD_PROTECTED));
		}
		else if (user != null && searchAlsoProtectedPages == false) {
			DatabaseCriteria andCriteria = null;// DatabaseCriteria.and();
			StringTokenizer stu = new StringTokenizer(user.getUserGroupsIds(), ",");
			while (stu.hasMoreTokens())
			{
				int groupId = Tools.getIntValue(stu.nextToken(), -1);
				if (groupId > 0)
				{
					if (andCriteria == null) andCriteria = DatabaseCriteria.tokenizedIds(FieldEnum.PASSWORD_PROTECTED, groupId);
					else andCriteria.addCriteria(DatabaseCriteria.tokenizedIds(FieldEnum.PASSWORD_PROTECTED, groupId));
				}
			}
			DatabaseCriteria orCriteria = (andCriteria != null ? or(isEmpty(FieldEnum.PASSWORD_PROTECTED), andCriteria) : isEmpty(FieldEnum.PASSWORD_PROTECTED));
			// orCriteria.addCriteria(andCriteria);
			newsQuery.addCriteria(orCriteria);
		}
	}

	private void addGroups()
	{
		if (groupIds == null && doc != null) {
			GroupDetails group = doc.getGroup();
			groupIds = List.of(group);
		}

		List<Integer> defaultDocs = new ArrayList<>();

		if (groupIds != null)
		{
			groupIdsExpanded = new LinkedList<>();
			GroupsDB gdb = GroupsDB.getInstance();
			for (GroupDetails group : groupIds)
			{
				groupIdsExpanded.add(group.getGroupId());
				if (isAlsoSubGroups())
				{
					//All subgroups
					if(subGroupsDepth < 1) {
						List<GroupDetails> groupList = gdb.getGroupsTree(group.getGroupId(), false, false);
						for (GroupDetails g : groupList) {
							groupIdsExpanded.add(g.getGroupId());
							defaultDocs.add(g.getDefaultDocId());
						}
					} else {
						//Subgroups to specific depth
						List<GroupDetails> groupList = gdb.getGroupsTree(group.getGroupId(), false, false, false, subGroupsDepth);
						for (GroupDetails g : groupList) {
							groupIdsExpanded.add(g.getGroupId());
							defaultDocs.add(g.getDefaultDocId());
						}
					}
				}
			}
			newsQuery.addCriteria(DatabaseCriteria.in(FieldEnum.GROUP_ID, groupIdsExpanded));
		}

		//Tricky part, if we want only "defaultDocs" and there are no "defaultDocs" we must add un existing docId, so the query will return nothing
		if(docMode == 1) {
			defaultDocs.add(-666);
			newsQuery.addCriteria(DatabaseCriteria.in(FieldEnum.DOC_ID, defaultDocs));
		} else if(!defaultDocs.isEmpty() && docMode == 2) {
			newsQuery.addCriteria(DatabaseCriteria.notIn(FieldEnum.DOC_ID, defaultDocs));
		}
	}

	private Optional<PerexGroupBean> getPerexGroup(String title) {
		List<PerexGroupBean> perexGroups = DocDB.getInstance().getPerexGroups();
		Optional<PerexGroupBean> result = perexGroups
				.stream()
				.filter(group -> {
					String perexGroupName = DB.internationalToEnglish(group.getPerexGroupName()).trim();
					if (perexGroupName.contains("|")) {
						perexGroupName = perexGroupName.substring(0, perexGroupName.indexOf("|")).trim();
					}
					return perexGroupName.equalsIgnoreCase(DB.internationalToEnglish(title));
				})
				.findFirst();

		return result;
	}

	private void addPerexGroups()
	{
		if (Tools.isNotEmpty(tag)) {
			try {
				tag = URLDecoder.decode(tag, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				sk.iway.iwcm.Logger.error(e);
			}

			Optional<PerexGroupBean> perexGroupOptional = getPerexGroup("#" + tag);
			if (perexGroupOptional.isPresent()) {
				int perexGroupId = perexGroupOptional.get().getPerexGroupId();
				Logger.debug(NewsActionBean.class, "" + perexGroupId);
				newsQuery.addCriteria(DatabaseCriteria.tokenizedIds(FieldEnum.PEREX_GROUP, perexGroupId));
			}
		}

		if (Tools.isNotEmpty(author)) {
			try {
				author = URLDecoder.decode(author, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				sk.iway.iwcm.Logger.error(e);
			}

			Optional<PerexGroupBean> perexGroupOptional = getPerexGroup("@" + author);
			if (perexGroupOptional.isPresent()) {
				int perexGroupId = perexGroupOptional.get().getPerexGroupId();
				Logger.debug(NewsActionBean.class, "" + perexGroupId);
				newsQuery.addCriteria(DatabaseCriteria.tokenizedIds(FieldEnum.PEREX_GROUP, perexGroupId));
			}
		}

		// setne perexgroupy z requestu
		if(Tools.isNotEmpty("requestPerexGroupsName")) {
			String[] tmpPerexGroups = getRequest().getParameterValues(requestPerexGroupsName);
			if(tmpPerexGroups != null) {
				int[] perexGroups = Arrays.asList(tmpPerexGroups).stream().mapToInt(Integer::parseInt).toArray();
				if(perexGroups.length > 0) {
					perexGroup = Arrays.stream(perexGroups).boxed().toArray(Integer[]::new);
				}
			}
		}


		if (perexGroup!=null&& perexGroup.length>0)
		{
			DatabaseCriteria orPerexGroups = null;
			for (int i=0;i<perexGroup.length;i++)
			{
				if (orPerexGroups==null)
				{
					orPerexGroups = DatabaseCriteria.tokenizedIds(FieldEnum.PEREX_GROUP, perexGroup[i]);
				}
				else
				{
					orPerexGroups = DatabaseCriteria.or(orPerexGroups, DatabaseCriteria.tokenizedIds(FieldEnum.PEREX_GROUP, perexGroup[i]));
				}

			}
			newsQuery.addCriteria(orPerexGroups);
		}
		if (perexGroupNot!=null&& perexGroupNot.length>0)
		{
			for (int i=0;i<perexGroupNot.length;i++)
			{
				newsQuery.addCriteria(
						DatabaseCriteria.or(
								DatabaseCriteria.isNull(FieldEnum.PEREX_GROUP),
								DatabaseCriteria.not(
										DatabaseCriteria.or(
												DatabaseCriteria.contains(FieldEnum.PEREX_GROUP, ","+perexGroupNot[i]+","),
												DatabaseCriteria.startsWith(FieldEnum.PEREX_GROUP, perexGroupNot[i] + ",%"),
												DatabaseCriteria.endsWith(FieldEnum.PEREX_GROUP, "%," + perexGroupNot[i]),
												DatabaseCriteria.contains(FieldEnum.PEREX_GROUP, "%," + perexGroupNot[i] + ",%")
										)
								)

						)
				);

			}
		}
	}


	private void addPerexNotRequired()
	{
		if (!isPerexNotRequired())
		{
			newsQuery.addCriteria(DatabaseCriteria.isNotEmptyText(FieldEnum.HTML_DATA));
		}
	}

	private void addPublishType() {
		// typ noviniek
		PublishType publishTypeEnum = PublishType.fromString(publishType);
		if(publishTypeEnum == null) return;
		switch (publishTypeEnum)
		{
			case NEW:
				newsQuery.addCriteria(
						and(
								or(
										isNull(PUBLISH_START),
										lessEqual(PUBLISH_START, new Timestamp(Tools.getNow()))
								),
								or(
										isNull(PUBLISH_END),
										greaterEqual(PUBLISH_END, new Timestamp(Tools.getNow()))
								)
						)
				);
				break;
			case VALID:
				newsQuery.addCriteria(
						and(
								and(
										isNotNull(PUBLISH_START),
										lessEqual(PUBLISH_START, new Timestamp(Tools.getNow()))
								),
								or(
										isNull(PUBLISH_END),
										greaterEqual(PUBLISH_END, new Timestamp(Tools.getNow()))
								)
						)
				);
				break;
			case NEXT:
				newsQuery.addCriteria(
						or(
								and(
										isNotNull(PUBLISH_END),
										greaterEqual(PUBLISH_END, new Timestamp(Tools.getNow()))
								),
								and(
										isNull(PUBLISH_END),
										isNotNull(PUBLISH_START),
										greaterEqual(PUBLISH_START, new Timestamp(Tools.getNow()))
								)
						)
				);
				break;
			case OLD:
				newsQuery.addCriteria(
						and(
								isNotNull(PUBLISH_END),
								lessEqual(PUBLISH_END, new Timestamp(Tools.getNow()))
						)
				);
				break;
			default:
		}
	}

	private void addRequestCriteria()
	{
		/* Zoberieme Criteria ulozene v requeste (obdoba stareho whereSql) */
		int hash = 0;
		if (getRequest().getAttribute(REQUEST_CRITERIA_KEY + String.valueOf(hash)) != null)
		{
			@SuppressWarnings("unchecked")
			List<Criteria> reqCriteria = (List<Criteria>) getRequest().getAttribute(REQUEST_CRITERIA_KEY + String.valueOf(hash));
			for (Criteria c : reqCriteria)
			{
				newsQuery.addCriteria(c);
			}
			getRequest().removeAttribute(REQUEST_CRITERIA_KEY + String.valueOf(hash));
		}
	}

	private void addOrder()
	{
		OrderEnum orderEnum = OrderEnum.fromString(order);
		if (orderEnum != null)
		{
			newsQuery.addOrder(orderEnum, isAscending() ? SortEnum.ASC : SortEnum.DESC);
		}
		else
		{
			newsQuery.addOrder(OrderEnum.DATE, SortEnum.ASC);
		}
	}

	private void cropPerex() {
		if (perexCrop > 0 && newsList != null && newsList.size() > 0)
		{
			for (DocDetails d : newsList)
			{
				if (Tools.isNotEmpty(d.getHtmlData()) && d.getHtmlData().length() > perexCrop)
				{
					d.setHtmlData(d.getHtmlData().substring(0, perexCrop - 3) + "...");
				}
			}
		}
	}

	private void fillTemplate() {

		if (template != null)
		{
			NewsTemplatesEntity newsEntity = NewsTemplatesService.getTemplateByName(template);
			if (newsEntity == null) return;

			String templateCode = newsEntity.getTemplateCode();
			if (Tools.isNotEmpty(templateCode))
			{
				VelocityEngine ve = new VelocityEngine();

//				ve.evaluate(vc, swOut, "Generate comment", prop.getText("helpdesk.ticket.commentTemplate"));
				StringWriter swOut = new StringWriter();
				VelocityContext vc = new VelocityContext();

				Prop prop = Prop.getInstance(PageLng.getUserLng(getRequest()));

				vc.put("news", newsList);
				vc.put("actionBean", this);
				vc.put("context", this);
				vc.put("prop", prop);
				vc.put("Tools", Tools.class);
				vc.put("DocDB", sk.iway.iwcm.doc.DocDB.class);
				vc.put("GroupsDB", sk.iway.iwcm.doc.GroupsDB.class);
				vc.put("MediaDB", sk.iway.spirit.MediaDB.class);
				vc.put("pageParams", new PageParams(getRequest()));
				vc.put("dateTool", new DateTool());

				String[] contextClassesArray = getContextClassesArr();
				if (contextClassesArray != null && contextClassesArray.length > 0)
				{
					for (String clazzName : Tools.getTokens(String.join("+", contextClasses), ",;+|"))
					{
						//uz pridavame implicitne
						if ("sk.iway.iwcm.doc.DocDB".equals(clazzName)) continue;
						if ("sk.iway.iwcm.doc.GroupsDB".equals(clazzName)) continue;

						try
						{
							Class<?> clazz = Class.forName(clazzName);
							if (!vc.containsKey(clazz.getSimpleName()))
							{
								vc.put(clazz.getSimpleName(), clazz);
							}
							else
							{
								Logger.debug(getClass(), "FillTeplate classObject with name "+clazzName+" already contained.");
							}
						}
						catch (ClassNotFoundException e)
						{
							Logger.debug(getClass(), "FillTeplate classObject to context failed, class "+clazzName+" not found.");
						}
					}
				}

				try
				{
//					VelocityEngine ve = new VelocityEngine();
					ve.init();
					ve.evaluate(vc, swOut, "Template evaluate", VelocityTools.upgradeTemplate(templateCode));
				}
				catch (Exception e)
				{
					swOut.append(WriteTag.getErrorMessage(prop, "writetag.error", "news"));

					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));

					user = getCurrentUser();
					if (user != null && user.isAdmin() && getRequest().getAttribute("writeTagDontShowError")==null)
					{
						swOut.append("<div style='border:2px solid red; background-color: white; color: black; margin: 5px; white-space: pre;'>"+ ResponseUtils.filter(e.getMessage())+"<br>");
						String stackTrace = ResponseUtils.filter(sw.toString());
						swOut.append(stackTrace + "</div>");
					}

					sk.iway.iwcm.Logger.error(e);
				}

				htmlOut = swOut.toString();
			}

			String pagingCode = newsEntity.getPagingCode();
			if (isPaging() && Tools.isNotEmpty(pagingCode))
			{
				String pagingHtml = getPaging(pagingCode);
				if (Tools.isNotEmpty(pagingHtml))
				{
					if (newsEntity.getPagingPosition() == NewsTemplatesEntity.PagingPosition.BEFORE.ordinal() || newsEntity.getPagingPosition() == NewsTemplatesEntity.PagingPosition.BEFORE_AND_AFTER.ordinal()) {
						htmlOut = pagingHtml + htmlOut;
					}

					if (newsEntity.getPagingPosition() == NewsTemplatesEntity.PagingPosition.AFTER.ordinal() || newsEntity.getPagingPosition() == NewsTemplatesEntity.PagingPosition.BEFORE_AND_AFTER.ordinal()) {
						htmlOut += pagingHtml;
					}
				}
			}
		}
	}

	private String getPaging(String pagingText) {
		Prop prop = Prop.getInstance(getRequest());
		String result = "";
		Map<String, Object> paginatorMap = getPaginator(prop);
		if (paginatorMap != null)
		{
			StringWriter swOut = new StringWriter();
			VelocityContext vc = new VelocityContext();

			vc.put("firstPage", paginatorMap.get("first"));
			vc.put("prevPage", paginatorMap.get("prev"));
			vc.put("nextPage", paginatorMap.get("next"));
			vc.put("lastPage", paginatorMap.get("last"));
			vc.put("pages", paginatorMap.get("pages"));
			vc.put("pagesAll", paginatorMap.get("pagesAll"));
			vc.put("prop", prop);
			vc.put("dateTool", new DateTool());
			vc.put("totalCount", paginatorMap.get("totalCount"));
			vc.put("totalPages", paginatorMap.get("totalPages"));

			//ak tam je len jedna stranka nema zmysel zobrazit paginator, vratme prazdne
			@SuppressWarnings("unchecked")
			List<PaginationInfo> pagesAll = (List<PaginationInfo>)paginatorMap.get("pagesAll");
			if (pagesAll == null || pagesAll.isEmpty()) return "";

			try
			{
				VelocityEngine ve = new VelocityEngine();
//				VelocityEngine ve = new VelocityEngine();
				ve.init();
				ve.evaluate(vc, swOut, "Paging evaluate", VelocityTools.upgradeTemplate(pagingText));
			}
			catch (Exception e)
			{
				swOut.append(WriteTag.getErrorMessage(prop, "writetag.error", "news"));

				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));

				user = getCurrentUser();
				if (user != null && user.isAdmin() && getRequest().getAttribute("writeTagDontShowError")==null)
				{
					swOut.append("<div style='border:2px solid red; background-color: white; color: black; margin: 5px; white-space: pre;'>"+ ResponseUtils.filter(e.getMessage())+"<br>");
					String stackTrace = ResponseUtils.filter(sw.toString());
					swOut.append(stackTrace + "</div>");
				}

				sk.iway.iwcm.Logger.error(e);
			}

			result = swOut.toString();
		}

		return result;
	}

	public Resolution loadTemplate()
	{
		JSONObject result = new JSONObject();
		if (template != null)
		{
			JsonObjectGenerator jog = new JsonObjectGenerator();
			jog.setObjectFormaterFactory(new ObjectFormaterFactory());
			jog.addObject(result, "template", template, "key,keyShort,pagingKey,image,value,pagingValue,pagingPosition");
		}
		return new StreamingResolution("application/json", new StringReader(result.toString()));
	}

	public Resolution setTags() {
		RequestHelper requestHelper = new RequestHelper(getRequest());
		String[] perexGroups = requestHelper.getDocument().getPerexGroupNames();
		tags = new ArrayList<>();

		for (String group : perexGroups) {
			if (group.startsWith("#")) {
				LabelValueDetails lvd = new LabelValueDetails();
				lvd.setLabel(group.substring(1));
				lvd.setValue(DB.internationalToEnglish(group.substring(1)));
				tags.add(lvd);
			}
		}

		return new ForwardResolution(WebJETActionBean.RESOLUTION_CONTINUE);
	}

	public Resolution setAuthors() {
		RequestHelper requestHelper = new RequestHelper(getRequest());
		String[] perexGroups = requestHelper.getDocument().getPerexGroupNames();
		authors = new ArrayList<>();

		for (String group : perexGroups) {
			if (group.startsWith("@")) {
				LabelValueDetails lvd = new LabelValueDetails();
				lvd.setLabel(group.substring(1));
				lvd.setValue(DB.internationalToEnglish(group.substring(1)));
				authors.add(lvd);
			}
		}

		return new ForwardResolution(WebJETActionBean.RESOLUTION_CONTINUE);
	}

	@SuppressWarnings("java:S1452")
	public List<? extends DocDetails> getNewsList()
	{
		return newsList;
	}

	private static final String REQUEST_CRITERIA_KEY = "NewsActionBean-Criteria";

	/**
	 * Umoznuje pridat dalsie kriteria, napr v JSP - obdoba whereSql v
	 * predoslych newskach
	 *
	 * @param request
	 * @param criteria
	 */
	@SuppressWarnings("unchecked")
	public static void addCriteria(HttpServletRequest request, Criteria... criteria)
	{
		int hash = 0;
		List<Criteria> cr = null;
		if (request.getAttribute(REQUEST_CRITERIA_KEY + String.valueOf(hash)) != null)
		{
			cr = (List<Criteria>) request.getAttribute(REQUEST_CRITERIA_KEY);
		}
		else
		{
			cr = new LinkedList<>();
			request.setAttribute(REQUEST_CRITERIA_KEY + String.valueOf(hash), cr);
		}
		if (criteria != null)
		{
			cr.addAll(Arrays.asList(criteria));
		}
	}

	private Criteria parseCriteria(String property, String operator, List<String> values)
	{
		FieldEnum field = FieldEnum.getField(property);
		if (field != null)
		{
			CriteriaType criteriaType = null;

			if (operator.equalsIgnoreCase("eq")) criteriaType = CriteriaType.EQUALS;
			else if (operator.equalsIgnoreCase("gt")) criteriaType = CriteriaType.GREATER_THAN;
			else if (operator.equalsIgnoreCase("ge")) criteriaType = CriteriaType.GREATER_THAN_EQUAL;
			else if (operator.equalsIgnoreCase("le")) criteriaType = CriteriaType.LESS_THAN_EQUAL;
			else if (operator.equalsIgnoreCase("lt")) criteriaType = CriteriaType.LESS_THAN;
			else if (operator.equalsIgnoreCase("sw")) criteriaType = CriteriaType.LIKE;
			else if (operator.equalsIgnoreCase("ew")) criteriaType = CriteriaType.LIKE;
			else if (operator.equalsIgnoreCase("co")) criteriaType = CriteriaType.LIKE;
			else if (operator.equalsIgnoreCase("swciai")) criteriaType = CriteriaType.LIKE;
			else if (operator.equalsIgnoreCase("ewciai")) criteriaType = CriteriaType.LIKE;
			else if (operator.equalsIgnoreCase("cociai")) criteriaType = CriteriaType.LIKE;

			if (criteriaType != null)
			{
				if (values.size() > 1)
				{
					DatabaseCriteria result = null;
					for (String val : values)
					{
						val = addLikeAtrs(operator, val);

						if (result == null) result = new DatabaseCriteria(criteriaType, field, castObject(field, val), false);
						else result = DatabaseCriteria.or(result, new DatabaseCriteria(criteriaType, field, castObject(field, val), false));
					}
					return result;
				}
				else
				{
					return new DatabaseCriteria(criteriaType, field, castObject(field, addLikeAtrs(operator, values.get(0))), false);
				}
			}
		}
		else if (property.startsWith("atr:"))
		{
			// vyhladanie podla nazvu atributu
			String atributeName = property.substring("atr:".length());
			if (operator.equalsIgnoreCase("eq"))
			{
				if (values.size() > 1)
				{
					DatabaseCriteria result = null;
					for (String val : values)
					{
						if (result == null) result = DatabaseCriteria.Atribute.equal(atributeName, val);
						else result = DatabaseCriteria.or(result, DatabaseCriteria.Atribute.equal(atributeName, val));
					}
					return result;
				}
				else
				{
					return DatabaseCriteria.Atribute.equal(atributeName, values.get(0));// (field,
				}
			}
		}
		else if (property.startsWith("atrId:"))
		{
			// vyhladanie podla ID atributu
			int atributeId = Tools.getIntValue(property.substring("atrId:".length()), 0);
			if (atributeId > 0 && operator.equalsIgnoreCase("eq"))
			{
				if (values.size() > 1)
				{
					DatabaseCriteria result = null;
					for (String val : values)
					{
						if (result == null) result = DatabaseCriteria.Atribute.equal(atributeId, val);
						else result = DatabaseCriteria.or(result, DatabaseCriteria.Atribute.equal(atributeId, val));
					}
					return result;
				}
				else
				{
					return DatabaseCriteria.Atribute.equal(atributeId, values.get(0));
				}
			}
		}
		return null;
	}

	private String addLikeAtrs(String operator, String value)
	{
		if (operator.equalsIgnoreCase("sw")) value = value+"%";
		else if (operator.equalsIgnoreCase("ew")) value = "%"+value;
		else if (operator.equalsIgnoreCase("co")) value = "%"+value+"%";
			//pouziva sa napr pri stlpci documents.data_asc v Oracle kde nefunguje CI_AI pre CLOB objekty
		else if (operator.equalsIgnoreCase("swciai")) value = DB.internationalToEnglish(value).toLowerCase()+"%";
		else if (operator.equalsIgnoreCase("ewciai")) value = "%"+ DB.internationalToEnglish(value).toLowerCase();
		else if (operator.equalsIgnoreCase("cociai")) value = "%"+ DB.internationalToEnglish(value).toLowerCase()+"%";

		return value;
	}

	private Object castObject(FieldEnum field, String value)
	{
		try
		{
			if ("Boolean".equals(field.getFieldTypeString()))
			{
				if ("true".equals(value)) return Boolean.TRUE;
				return Boolean.FALSE;
			}
			else if ("Integer".equals(field.getFieldTypeString()))
			{
				return Integer.parseInt(value);
			}
			else if ("Date".equals(field.getFieldTypeString()))
			{
				return new Date(DB.getTimestamp(value));
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return value;
	}

	public void setAscending(boolean ascending)
	{
		this.ascending = ascending;
	}

	public void setPaging(boolean paging)
	{
		this.paging = paging;
	}

	public void setPageSize(int pageSize)
	{
		this.pageSize = pageSize;
	}

	public void setNewsName(String newsName)
	{
		this.newsName = newsName;
	}

	public void setPerex(boolean perex)
	{
		this.perex = perex;
	}

	public void setDate(boolean date)
	{
		this.date = date;
	}

	public void setPlace(boolean place)
	{
		this.place = place;
	}

	public final void setGroupIds(int[] groupIds)
	{
		GroupsDB gdb = GroupsDB.getInstance();
		List<GroupDetails> groups = new ArrayList<>();
		for (int groupId : groupIds)
		{
			GroupDetails group = gdb.getGroup(groupId);
			if (group != null) groups.add(group);
		}

		this.groupIds = groups;
	}

	public void setNewsQuery(NewsQuery newsQuery)
	{
		this.newsQuery = newsQuery;
	}

	public void setSearchAlsoProtectedPages(boolean searchAlsoProtectedPages)
	{
		this.searchAlsoProtectedPages = searchAlsoProtectedPages;
	}

	public boolean isLoadData()
	{
		return Tools.isTrue(loadData);
	}

	public void setLoadData(boolean loadData)
	{
		this.loadData = loadData;
	}

	public boolean isPaging()
	{
		return Tools.isTrue(paging);
	}

	public boolean isPerex()
	{
		return perex;
	}

	public int getPage()
	{
		return page;
	}

	public void setPage(int page)
	{
		this.page = page;
	}

	public boolean isDate()
	{
		return date;
	}

	public boolean isPlace()
	{
		return place;
	}

	public String getRequestPerexGroupsName() {
		return requestPerexGroupsName;
	}

	public void setRequestPerexGroupsName(String requestPerexGroupsName) {
		this.requestPerexGroupsName = requestPerexGroupsName;
	}

	public Map<String, Object> getPaginator(Prop prop)
	{
		if (paginator == null)
		{
			String url = Tools.getBaseLink(getRequest(), new String[] { "page" });
			int totalCount = newsQuery.getNewsCount();
			if (totalCount > 0)
			{
				paginator = new HashMap<>();

				//totalPages = totalCount / pageSize + 1;
				int totalPages = (int)Math.ceil((double)totalCount / (double)pageSize); //NOSONAR

				paginator.put("first", getFirst(prop, url));
				paginator.put("prev", getPrev(prop, url));
				paginator.put("next", getNext(prop, url, totalPages));
				paginator.put("last", getLast(prop, url, totalPages));
				paginator.put("totalPages", totalPages);

				List<PaginationInfo> pages = new LinkedList<>();
				List<PaginationInfo> pagesAll = new LinkedList<>();
				boolean wasSkipped = false;
				for (int pageCnt = 1; pageCnt <= totalPages; pageCnt++)
				{
					PaginationInfo pi = new PaginationInfo();
					pi.setPageNumber(pageCnt);
					pi.setUrl(Tools.addParameterToUrlNoAmp(url, "page", String.valueOf(pageCnt)));
					pi.setLabel(String.valueOf(pageCnt));
					if (pageCnt == 1) pi.setFirst(true);
					if (pageCnt == totalPages) pi.setLast(true);
					if (pageCnt == page) pi.setActual(true);

					pagesAll.add(pi);

					if (pageCnt>2 && pageCnt<totalPages-2 && ((pageCnt<page && page-pageCnt>2) || (pageCnt>page && pageCnt-page>2)) )
					{
						wasSkipped = true;
						continue;
					}
					if (wasSkipped)
					{
						wasSkipped=false;
						PaginationInfo pi2 = new PaginationInfo();
						pi2.setLabel("...");
						pi2.setUrl("javascript:void(0);");
						pages.add(pi2);
						continue;
					}

					pages.add(pi);
				}

				paginator.put("pages", pages);
				paginator.put("pagesAll", pagesAll);
				paginator.put("totalCount", totalCount);
			}
		}
		return paginator;
	}

	private PaginationInfo getFirst(Prop prop, String url)
	{
		PaginationInfo item = new PaginationInfo();
		item.setLabel(prop.getText("components.menu.paging.firstPage"));
		item.setPageNumber(1);
		item.setUrl(Tools.addParameterToUrlNoAmp(url, "page", "1"));
		item.setActive(page > 1);

		return item;
	}

	private PaginationInfo getPrev(Prop prop, String url)
	{
		PaginationInfo item = new PaginationInfo();
		int pageNumber = page - 1 > 0 ? page - 1 : 1;

		item.setLabel(prop.getText("components.menu.paging.prevPage"));
		item.setPageNumber(pageNumber);
		item.setUrl(Tools.addParameterToUrlNoAmp(url, "page", String.valueOf(pageNumber)));
		item.setActive(page > 1);

		return item;
	}

	private PaginationInfo getNext(Prop prop, String url, int totalPages)
	{
		PaginationInfo item = new PaginationInfo();
		int pageNumber = page + 1 < totalPages ? page + 1 : totalPages;

		item.setLabel(prop.getText("components.menu.paging.nextPage"));
		item.setPageNumber(pageNumber);
		item.setUrl(Tools.addParameterToUrlNoAmp(url, "page", String.valueOf(pageNumber)));
		item.setActive(page < totalPages);

		return item;
	}

	private PaginationInfo getLast(Prop prop, String url, int totalPages)
	{
		PaginationInfo item = new PaginationInfo();
		item.setLabel(prop.getText("components.menu.paging.lastPage"));
		item.setPageNumber(totalPages);
		item.setUrl(Tools.addParameterToUrlNoAmp(url, "page", String.valueOf(totalPages)));
		item.setActive(page < totalPages);

		return item;
	}

	public static class PaginationInfo
	{
		private String label;

		private int pageNumber;

		private String url;

		private boolean active;

		private boolean actual;

		private boolean first;

		private boolean last;

		private String link;

		public String getLabel()
		{
			return label;
		}

		public void setLabel(String label)
		{
			this.label = label;
		}

		public int getPageNumber()
		{
			return pageNumber;
		}

		public void setPageNumber(int pageNumber)
		{
			this.pageNumber = pageNumber;
		}

		public boolean isActive()
		{
			return active;
		}

		public void setActive(boolean active)
		{
			this.active = active;
		}

		public boolean isFirst()
		{
			return first;
		}

		public void setFirst(boolean first)
		{
			this.first = first;
		}

		public boolean isLast()
		{
			return last;
		}

		public void setLast(boolean last)
		{
			this.last = last;
		}

		public String getUrl()
		{
			return url;
		}

		public void setUrl(String url)
		{
			this.url = url;
		}

		public boolean isActual()
		{
			return actual;
		}

		public void setActual(boolean actual)
		{
			this.actual = actual;
		}

		public String getLink()
		{
			if (link == null) {
				link = "<a href=\"" + this.getUrl() + "\" class=\"page-link\">" + this.getLabel() + "</a>";
			}
			return link;
		}

		public void setLink(String link)
		{
			this.link = link;
		}

		public String getLi()
		{
			StringBuilder li = new StringBuilder();
			li.append("<li");
			li.append(getLiClass());
			li.append(">");
			li.append("<a href=\"").append(this.getUrl()).append("\" class=\"page-link\">").append(this.getLabel()).append("</a>");
			li.append("</li>");
			return li.toString();
		}

		public String getLiClass()
		{
			StringBuilder li = new StringBuilder();
			li.append(" class=\"page-item");
			if (isActual()) li.append(" active");
			else if (getUrl().contains("javascript:void")) li.append(" disabled");
			li.append("\"");
			return li.toString();
		}
	}

	public int getTotalPages()
	{
		return totalPages;
	}

	public void setReturnDocsWithAtributes(boolean returnDocsWithAtributes)
	{
		this.returnDocsWithAtributes = returnDocsWithAtributes;
	}

	public Map<String, List<String>> getFilter()
	{
		return filter;
	}

	public void setFilter(Map<String, List<String>> filter)
	{
		this.filter = filter;
	}

	public Map<String, List<String>> getSearch()
	{
		return search;
	}

	public void setSearch(Map<String, List<String>> search)
	{
		this.search = search;
	}

	public void setAlsoSubGroups(boolean alsoSubGroups)
	{
		this.alsoSubGroups = alsoSubGroups;
	}

	public void setSubGroupsDepth(int subGroupsDepth) {
		this.subGroupsDepth = subGroupsDepth;
	}

	public void setDocMode(int docMode) {
		this.docMode = docMode;
	}

	public String getGroupIdsString()
	{
		StringBuilder result = new StringBuilder();
		if (getGroupIds() != null)
		{
			for (GroupDetails in : getGroupIds())
			{
				result.append(",").append(in.getGroupId());
			}
		}
		return result.indexOf(",") == 0 ? result.substring(1) : result.toString();
	}

	public String getPerexGroupString()
	{
		StringBuilder result = new StringBuilder();
		if (perexGroup != null)
		{
			for (Integer in : perexGroup)
			{
				result.append(",").append(in);
			}
		}
		return result.indexOf(",") == 0 ? result.substring(1) : result.toString();
	}
	public String getPerexGroupNotString()
	{
		StringBuilder result = new StringBuilder();
		if (perexGroupNot != null)
		{
			for (Integer in : perexGroupNot)
			{
				result.append(",").append(in);
			}
		}
		return result.indexOf(",") == 0 ? result.substring(1) : result.toString();
	}

	public boolean isAscending()
	{
		return Tools.isTrue(ascending);
	}

	public boolean isAlsoSubGroups()
	{
		return Tools.isTrue(alsoSubGroups);
	}

	public int getPerexCrop()
	{
		return perexCrop;
	}

	public void setPerexCrop(int perexCrop)
	{
		this.perexCrop = perexCrop;
	}

	public void setPerexNotRequired(boolean perexNotRequired)
	{
		this.perexNotRequired = perexNotRequired;
	}

	public enum PublishType
	{
		NEW,
		OLD,
		ALL,
		NEXT,
		VALID;

		public static PublishType fromString(String value) {
			for (PublishType pt : PublishType.values()) {
				if (pt.name().equalsIgnoreCase(value)) {
					return pt;
				}
			}
			return null;
		}
	}

	public void setPublishType(PublishType publishType)
	{
		this.publishType = publishType == null ? null : publishType.name();
	}

	public void setOrder(OrderEnum order)
	{
		this.order = order == null ? null : order.name();
	}

	public String getHtmlOut()
	{
		return htmlOut;
	}

	public boolean isPerexNotRequired()
	{
		return Tools.isTrue(perexNotRequired);
	}

	public boolean isSearchAlsoProtectedPages()
	{
		return searchAlsoProtectedPages;
	}

	public String link(DocDetails doc)
	{
		return DocDB.getInstance().getDocLink(doc.getDocId(), doc.getExternalLink(), getRequest());
	}

	public void setTemplate(String template)
	{
		this.template = template;
	}

	public String getTemplate()
	{
		return template;
	}

	public List<NewsTemplatesEntity> getTemplates()
	{
		return NewsTemplatesService.getTemplates();
	}

	public List<NewsContextMenuItem> getVelocityProperties()
	{
		return NewsContextMenuItems.getVelocityProperties();
	}

	public List<NewsContextMenuItem> getDocDetailsProperties()
	{
		return NewsContextMenuItems.getDocDetailsProperties();
	}

	public List<NewsContextMenuItem> getGroupDetailsProperties()
	{
		return NewsContextMenuItems.getGroupDetailsProperties();
	}

	public List<NewsContextMenuItem> getPagingProperties()
	{
		return NewsContextMenuItems.getPagingProperties();
	}

	public boolean isSelected(NewsTemplatesEntity entity)
	{
		if (template == null || entity == null) return false;
		String templateNameFixed = template;
		if (templateNameFixed == null) templateNameFixed = "";
		if (templateNameFixed.startsWith("news.template.")) templateNameFixed = templateNameFixed.substring("news.template.".length());

		return templateNameFixed.equals(entity.getName());
	}

	/**
	 * Vrati atributy pre news komponentu na jej lahsiu editaciu, pouzije sa na kazdom news elemente v liste
	 * @param request
	 * @param doc
	 * @return
	 */
	public static String getEditAttributesPopup(HttpServletRequest request, DocDetails doc)
	{
		return getEditAttributes(request, doc, "newsPopup");
	}

	public static String getEditAttributesInline(HttpServletRequest request, DocDetails doc)
	{
		return getEditAttributes(request, doc, "newsInline");
	}

	private static String getEditAttributes(HttpServletRequest request, DocDetails doc, String appName)
	{
		StringBuilder atrs = new StringBuilder();

		Identity user = UsersDB.getCurrentUser(request);
		if (user != null)
		{
			atrs.append(" data-wjapp='").append(appName).append("' data-wjappkey='").append(doc.getDocId()).append("'");
		}

		return atrs.toString();
	}

	public static UserDetails getAuthor(DocDetails doc) {
		UserDetails authorUserDetails = UsersDB.getUser(doc.getAuthorId());
		if (authorUserDetails == null) {
			authorUserDetails = new UserDetails();
			authorUserDetails.setLastName("");
			authorUserDetails.setFirstName("");
			authorUserDetails.setPhoto("");
		}
		return authorUserDetails;
	}

	public void setCacheMinutes(int cacheMinutes)
	{
		this.cacheMinutes = cacheMinutes;
	}

	public void setOffset(int offset)
	{
		this.offset = offset;
	}

	public boolean isIncludeActualDoc() {
		return includeActualDoc;
	}

	public void setIncludeActualDoc(boolean includeActualDoc) {
		this.includeActualDoc = includeActualDoc;
	}

	public void clearList()
	{
		this.newsList=null;
	}

	public boolean isCheckDuplicity() {
		return Tools.isTrue(checkDuplicity);
	}

	public void setCheckDuplicity(boolean checkDuplicity) {
		this.checkDuplicity = checkDuplicity;
	}

	public String getTagClickLink() {
		return tagClickLink;
	}

	public void setTagClickLink(String tagClickLink) {
		this.tagClickLink = tagClickLink;
	}

	private List<LabelValueDetails> tags;

	public List<LabelValueDetails> getTags() {
		return tags;
	}

	public void setTags(List<LabelValueDetails> tags) {
		this.tags = tags;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	private List<LabelValueDetails> authors;

	public List<LabelValueDetails> getAuthors() {
		return authors;
	}

	public void setAuthors(List<LabelValueDetails> authors) {
		this.authors = authors;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	private static Map<Class<? extends ActionBean>, List<String>> includeParamsOnly = new HashMap<>();

	@Before(stages={LifecycleStage.BindingAndValidation})
	public void prepareIncludeRequestWrapper()
	{
		prepareIncludeRequestWrapper(context, includeParamsOnly, getClass(), getClass());
	}

	@After(stages={LifecycleStage.BindingAndValidation})
	public void removeIncludeRequestWrapper()
	{
		removeIncludeRequestWrapper(context);
	}
}
