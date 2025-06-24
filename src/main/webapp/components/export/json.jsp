<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "application/json");
	response.setHeader("Access-Control-Allow-Origin", "*");
%><%@ page pageEncoding="UTF-8"
	import="org.json.JSONArray, org.json.JSONObject,sk.iway.iwcm.JsonTools, sk.iway.iwcm.Tools, sk.iway.iwcm.components.export.ExportDatBean, sk.iway.iwcm.doc.DocDB, sk.iway.iwcm.doc.DocDetails, java.util.List"%><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%><%@
taglib
	uri="/WEB-INF/iway.tld" prefix="iway"%><%
	//ak komponentu nemate v stranke ale volate ju priamo
	//tak sem zadajte ID adresarov z ktorych sa maju brat clanky
	ExportDatBean bean = (ExportDatBean) request.getAttribute("exportDatBean");
	if (null == bean)
	{
		return;
	}
	String urlAddress = Tools.getStringValue(bean.getUrlAddress(), "");
	int numberItems = Tools.getIntValue(bean.getNumberItems(), 0);
	String groupIds = bean.getGroupIds();
	//mame to v takomto formate, takze to convertneme
	groupIds = groupIds.replace('+', ',');
	//ak je nastavene na true beru sa do uvahy aj podadresare
	boolean expandGroupIds = false;
	expandGroupIds = bean.getExpandGroupIds();
	String perexGroup = Tools.getStringValue(bean.getPerexGroup(), "");
	perexGroup = perexGroup.replace('+', ',');
	request.setAttribute("perexGroup", perexGroup);
	//usporiada dokumenty podla datumu vytvorenia
	int orderType = DocDB.ORDER_SAVE_DATE;
	String p_order = Tools.getStringValue(bean.getOrderType(), "priority");
	if (p_order != null)
	{
		if (p_order.compareTo("date") == 0)
		{
			orderType = DocDB.ORDER_DATE;
		}
		else if (p_order.compareTo("id") == 0)
		{
			orderType = DocDB.ORDER_ID;
		}
		else if (p_order.compareTo("priority") == 0)
		{
			orderType = DocDB.ORDER_PRIORITY;
		}
		else if (p_order.compareTo("title") == 0)
		{
			orderType = DocDB.ORDER_TITLE;
		}
		else if (p_order.compareTo("place") == 0)
		{
			orderType = DocDB.ORDER_PLACE;
		}
		else if (p_order.compareTo("eventDate") == 0)
		{
			orderType = DocDB.ORDER_EVENT_DATE;
		}
		else if (p_order.compareTo("saveDate") == 0)
		{
			orderType = DocDB.ORDER_SAVE_DATE;
		}
	}
	boolean ascending = true;
	ascending = bean.getAsc();
	int publishType = DocDB.PUBLISH_NEW;
	String p_publish = Tools.getStringValue(bean.getPublishType(), "new");
	if (p_publish != null)
	{
		if (p_publish.compareToIgnoreCase("new") == 0)
		{
			publishType = DocDB.PUBLISH_NEW;
		}
		else if (p_publish.compareToIgnoreCase("old") == 0)
		{
			publishType = DocDB.PUBLISH_OLD;
		}
		else if (p_publish.compareToIgnoreCase("all") == 0)
		{
			publishType = DocDB.PUBLISH_ALL;
		}
		else if (p_publish.compareToIgnoreCase("next") == 0)
		{
			publishType = DocDB.PUBLISH_NEXT;
		}
	}
	boolean noPerexCheck = false;
	noPerexCheck = bean.isNoPerexCheck();
	if (noPerexCheck && publishType < 100)
	{
		publishType = publishType + 100;
	}
	//pocet dokumentov, ktore sa vygeneruju v JSON
	String debugPort = "";
	//String debugPort = ":8080";
	//ziskaj DocDB
	DocDB docDB = DocDB.getInstance();
	int totalDocs = docDB.getDocPerex(groupIds, orderType, ascending, publishType, numberItems, "novinky", "pages", request);
	List<DocDetails> novinky = null;
	if (request.getAttribute("novinky") != null)
	{
		novinky = (List<DocDetails>) request.getAttribute("novinky");
	}
	request.removeAttribute("novinky");
	request.removeAttribute("perexGroup");
	JSONArray jsonNovinky = new JSONArray();
	if (novinky != null && novinky.size() > 0)
	{
		JSONObject hlavicka = new JSONObject();
		hlavicka.put("urlAddress", urlAddress);
		hlavicka.put("numberItems", numberItems);
		hlavicka.put("total", totalDocs);
		jsonNovinky.put(hlavicka);
		//Gson gson = new Gson();
		JSONObject jsonNovinka = null;
		for (DocDetails novinka : novinky)
		{
			novinka.setData(JsonTools.prepare4Json(novinka.getData(), request));
			//jsonNovinka = new JSONObject(gson.toJson(novinka));
			jsonNovinka = new JSONObject();
			jsonNovinka.put("authorId", novinka.getAuthorId());
			jsonNovinka.put("authorName", novinka.getAuthorName());
			jsonNovinka.put("authorEmail", novinka.getAuthorEmail());
			jsonNovinka.put("available", novinka.isAvailable());
			jsonNovinka.put("cacheable", novinka.isCacheable());
			jsonNovinka.put("data", novinka.getData());
			jsonNovinka.put("dateCreated", novinka.getDateCreated());
			jsonNovinka.put("disableAfterEnd", novinka.isDisableAfterEnd());
			jsonNovinka.put("docLink", novinka.getDocLink());
			jsonNovinka.put("docId", novinka.getDocId());
			jsonNovinka.put("eventDate", novinka.getEventDate());
			jsonNovinka.put("eventDateString", novinka.getEventDateString());
			jsonNovinka.put("eventTimeString", novinka.getEventTimeString());
			jsonNovinka.put("externalLink",novinka.getExternalLink());
			jsonNovinka.put("fieldA", novinka.getFieldA());
			jsonNovinka.put("fieldB", novinka.getFieldB());
			jsonNovinka.put("fieldC", novinka.getFieldC());
			jsonNovinka.put("fieldD", novinka.getFieldD());
			jsonNovinka.put("fieldE", novinka.getFieldE());
			jsonNovinka.put("fieldF", novinka.getFieldF());
			jsonNovinka.put("fieldG", novinka.getFieldG());
			jsonNovinka.put("fieldH", novinka.getFieldH());
			jsonNovinka.put("fieldI", novinka.getFieldI());
			jsonNovinka.put("fieldJ", novinka.getFieldI());
			jsonNovinka.put("fieldK", novinka.getFieldK());
			jsonNovinka.put("fieldL", novinka.getFieldL());
			jsonNovinka.put("fieldM", novinka.getFieldM());
			jsonNovinka.put("fieldN", novinka.getFieldN());
			jsonNovinka.put("fieldO", novinka.getFieldO());
			jsonNovinka.put("fieldP", novinka.getFieldP());
			jsonNovinka.put("fieldQ", novinka.getFieldQ());
			jsonNovinka.put("fieldR", novinka.getFieldR());
			jsonNovinka.put("fieldS", novinka.getFieldS());
			jsonNovinka.put("fieldT", novinka.getFieldT());
			jsonNovinka.put("fileName", novinka.getFileName());
			jsonNovinka.put("forumCount", novinka.getForumCount());
			jsonNovinka.put("footerDocId", novinka.getFooterDocId());
			jsonNovinka.put("groupId", novinka.getGroupId());
			jsonNovinka.put("headerDocId", novinka.getHeaderDocId());
			jsonNovinka.put("historyActual", novinka.isHistoryActual());
			jsonNovinka.put("historyApproveDate", novinka.getHistoryApproveDate());
			jsonNovinka.put("historyApprovedByName", novinka.getHistoryApprovedByName());
			jsonNovinka.put("historySaveDate", novinka.getHistorySaveDate());
			jsonNovinka.put("historyId", novinka.getHistoryId());
			jsonNovinka.put("historyApprovedBy", novinka.getHistoryApprovedBy());
			jsonNovinka.put("htmlHead", novinka.getHtmlHead());
			jsonNovinka.put("htmlData", novinka.getHtmlData());
			jsonNovinka.put("logonPageDocId", novinka.getLogonPageDocId());
			jsonNovinka.put("menuDocId", novinka.getMenuDocId());
			jsonNovinka.put("navbar", novinka.getNavbar());
			jsonNovinka.put("passwordProtected", novinka.getPasswordProtected());
			jsonNovinka.put("perexGroup", novinka.getPerexGroup());
			jsonNovinka.put("perexImage", novinka.getPerexImage());
			jsonNovinka.put("perexPlace", novinka.getPerexPlace());
			jsonNovinka.put("publishEndString", novinka.getPublishEndString());
			jsonNovinka.put("publishEndTimeString", novinka.getPublishEndTimeString());
			jsonNovinka.put("publishStartString", novinka.getPublishStartString());
			jsonNovinka.put("publishStartStringExtra", novinka.getPublishStartStringExtra());
			jsonNovinka.put("publishStartTimeString", novinka.getPublishStartTimeString());
			jsonNovinka.put("publicable", novinka.isPublicable());
			jsonNovinka.put("publishStart", novinka.getPublishStart());
			jsonNovinka.put("publishEnd", novinka.getPublishEnd());
			jsonNovinka.put("rightMenuDocId", novinka.getRightMenuDocId());
			jsonNovinka.put("requireSsl", novinka.isRequireSsl());
			jsonNovinka.put("searchable", novinka.isSearchable());
			jsonNovinka.put("showInMenu", novinka.isShowInMenu());
			jsonNovinka.put("sortPriority", novinka.getSortPriority());
			jsonNovinka.put("syncDefaultForGroupId", novinka.getSyncDefaultForGroupId());
			jsonNovinka.put("syncRemotePath", novinka.getSyncRemotePath());
			jsonNovinka.put("syncId", novinka.getSyncId());
			jsonNovinka.put("syncStatus", novinka.getSyncStatus());
			jsonNovinka.put("tempName", novinka.getTempName());
			jsonNovinka.put("tempId", novinka.getTempId());
			jsonNovinka.put("title", novinka.getTitle());
			jsonNovinka.put("viewsTotal", novinka.getViewsTotal());
			jsonNovinka.put("virtualPath", novinka.getVirtualPath());
			jsonNovinky.put(jsonNovinka);
		}
	}
	request.setAttribute("jsonNovinky", jsonNovinky);
%><% Tools.printJSON(out, jsonNovinky); %>
