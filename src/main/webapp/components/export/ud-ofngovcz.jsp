<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "application/ld+json");
	response.setHeader("Access-Control-Allow-Origin", "*");
%><%@ page pageEncoding="UTF-8"
	import="
	sk.iway.iwcm.*,
	sk.iway.iwcm.doc.*,
	java.util.Date,
	org.json.JSONArray,
	org.json.JSONObject,
	sk.iway.iwcm.components.export.ExportDatBean,
	java.util.List,
	sk.iway.iwcm.i18n.Prop,
	java.text.SimpleDateFormat,
	java.util.Locale,
	java.util.TimeZone,
	sk.iway.spirit.model.*,
	sk.iway.spirit.MediaDB"%><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%><%@
taglib
	uri="/WEB-INF/iway.tld" prefix="iway"%><%@
taglib
	uri="/WEB-INF/struts-bean.tld" prefix="bean"%><%@
taglib
	uri="/WEB-INF/struts-html.tld" prefix="html"%><%@
taglib
	uri="/WEB-INF/struts-logic.tld" prefix="logic"%><%!
	private static SimpleDateFormat sdf;
	static
	{
		sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		//sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	public synchronized static String formatDate(long date)
	{
		return sdf.format(new Date(date));
}%><%
	
	//počítám napevno pouze s češtinou, jinak by to přes export dat ani nešlo, ale když to bude v nějaké stránce, tak to vzít můžeme
	//z klíčů potřebujeme převzít "iri" + "stránka" 
	String lng = PageLng.getUserLng(request);
	String userLng = Tools.getStringValue(lng, "cz");
	Prop prop = Prop.getInstance(userLng);
	
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
	org.json.
	JSONObject jsonOutput = new JSONObject();
	JSONArray jsonNovinky = new JSONArray();
	JSONObject informace = new JSONObject();
	if (novinky != null && novinky.size() > 0)
	{
		jsonOutput.put("@context", "https://ofn.gov.cz/úřední-desky/2021-07-20/kontexty/úřední-deska.jsonld");
		jsonOutput.put("typ", "Úřední deska");
		jsonOutput.put("iri", prop.getText("components.ud-ofngovcz.url") );
		jsonOutput.put("stránka", prop.getText("components.ud-ofngovcz.url") );
		JSONObject provozovatel = new JSONObject();
		provozovatel.put("typ", "Osoba");
		provozovatel.put("ičo", prop.getText("components.ud-ofngovcz.ico") );
		jsonOutput.put("provozovatel", provozovatel);

		JSONObject jsonNovinka = null;
		JSONArray typInformace = new JSONArray();
		typInformace.put("Digitální objekt");
		typInformace.put("Informace na úřední desce");
		for (DocDetails novinka : novinky)
		{
			novinka.setData(JsonTools.prepare4Json(novinka.getData(), request));
			//jsonNovinka = new JSONObject(gson.toJson(novinka));
			jsonNovinka = new JSONObject();
			jsonNovinka.put("typ", typInformace);
			jsonNovinka.put("iri", docDB.getDocLink(novinka.getDocId(), novinka.getExternalLink(), true, request));
			jsonNovinka.put("url", docDB.getDocLink(novinka.getDocId(), novinka.getExternalLink(), true, request));
			
			JSONObject nazev = new JSONObject();
			nazev.put("cs", novinka.getTitle());
			jsonNovinka.put("název", nazev);
			
			if (Tools.isNotEmpty(novinka.getPerex())) {
				JSONObject popis = new JSONObject();
				popis.put("cs", novinka.getPerex());
				jsonNovinka.put("popis", popis);
			}
			
			JSONObject vyveseni = new JSONObject();
			vyveseni.put("typ", "Časový okamžik");
			vyveseni.put("datum", formatDate(novinka.getPublishStart()));
			jsonNovinka.put("vyvěšení", vyveseni);

			JSONObject sveseni = new JSONObject();
			sveseni.put("typ", "Časový okamžik");
			sveseni.put("datum", formatDate(novinka.getPublishEnd()));
			jsonNovinka.put("relevantní_do", sveseni);

			/*
			if (Tools.isNotEmpty(novinka.getFieldA())) {
				jsonNovinka.put("číslo_jednací", novinka.getFieldA());
			}

			if (Tools.isNotEmpty(novinka.getFieldB())) {
				jsonNovinka.put("spisová_značka", novinka.getFieldB());
			}*/
			
			//agendy bereme jako perexové skupiny
			if (novinka.getPerexGroupNames()!= null && novinka.getPerexGroupNames().length > 0) {
				JSONArray agendy = new JSONArray();
				for (String perexGroupName : novinka.getPerexGroupNames()) {
					JSONObject nazevAgendy = new JSONObject();
					nazevAgendy.put("cs", perexGroupName);
					JSONObject agenda = new JSONObject();
					agenda.put("typ", "Agenda");
					agenda.put("název", nazevAgendy);
					agendy.put(agenda);
				}
				jsonNovinka.put("agenda", agendy);
			}
			
			//prilohy v modulu Media
			List<Media> files = MediaDB.getMedia(session, "documents", novinka.getDocId(), "", novinka.getDateCreated(), false);
			if (files != null && files.size() > 0) {
				JSONArray media = new JSONArray();
				for (Media file : files) {
					JSONObject medium = new JSONObject();
					
					medium.put("typ", "Digitální objekt");
					
					JSONObject nazevMedia = new JSONObject();
					nazev.put("cs", file.getMediaTitleSk());
					medium.put("název", nazevMedia);
					
					medium.put("url", "https://" + Tools.getServerName(request) + file.getMediaLink());
					
					media.put(medium);
				}
				jsonNovinka.put("dokument", media);
			}
			
			
			//jsonNovinka.put("",);
			
			
			jsonNovinky.put(jsonNovinka);
		}
	}
	jsonOutput.put("informace", jsonNovinky);
	request.setAttribute("jsonOutput", jsonOutput);
%><% out.print(jsonOutput); %>