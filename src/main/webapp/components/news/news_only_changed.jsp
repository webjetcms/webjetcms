<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.util.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%
//stranka pre includnutie noviniek
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

String groupIds = pageParams.getValue("groupIds", "");
//mame to v takomto formate, takze to convertneme
groupIds = groupIds.replace('+', ',');

String perexGroup = pageParams.getValue("perexGroup", "");
//mame to v takomto formate, takze to convertneme
perexGroup = perexGroup.replace('+', ',');

int orderType = DocDB.ORDER_PRIORITY;
String p_order = pageParams.getValue("orderType", "priority");
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

boolean ascending = pageParams.getBooleanValue("asc", true);
int publishType = DocDB.PUBLISH_NEW;
String p_publish = pageParams.getValue("publishType", "new");
if (p_publish!=null)
{
	if (p_publish.compareToIgnoreCase("new")==0)
	{
		publishType = DocDB.PUBLISH_NEW;
	}
	else if (p_publish.compareToIgnoreCase("old")==0)
	{
		publishType = DocDB.PUBLISH_OLD;
	}
	else if (p_publish.compareToIgnoreCase("all")==0)
	{
		publishType = DocDB.PUBLISH_ALL;
	}
	else if (p_publish.compareToIgnoreCase("next")==0)
	{
		publishType = DocDB.PUBLISH_NEXT;
	}
}

boolean noPerexCheck = pageParams.getBooleanValue("noPerexCheck", false);
if (noPerexCheck && publishType < 100)
{
	publishType = publishType + 100;
}

boolean paging = pageParams.getBooleanValue("paging", false);
int pageSize = pageParams.getIntValue("pageSize", 10);
int maxCols = pageParams.getIntValue("cols", 1);
String image = pageParams.getValue("image", "none");
boolean perex = pageParams.getBooleanValue("perex", false);
boolean date = pageParams.getBooleanValue("date", false);
boolean place = pageParams.getBooleanValue("place", false);

//ziskaj DocDB
DocDB docDB = DocDB.getInstance();

int actualDocId = -1;
try
{
	actualDocId = Integer.parseInt(Tools.getRequestParameter(request, "docid"));
}
catch (Exception ex)
{
	//sk.iway.iwcm.Logger.error(ex);
}
//vyradime zo zobrazenia aktualnu stranku
String whereSql = " AND doc_id NOT IN ("+actualDocId+") ";
if (perexGroup != null && perexGroup.length()>0)
{
	request.setAttribute("perexGroup", perexGroup);
}
request.setAttribute("whereSql", whereSql);

//System.out.println("groupIds="+groupIds+" perexGroup="+perexGroup);

//==================================================================
//==== TU SU ZMENY PRE KONTROLU CI NASTALA ZMENA VO WEB STRANKE ====
//==================================================================

//pocet dni pocas ktorych nesmela nastat zmena aby sa povazovalo za nove
int daysAsNew = pageParams.getIntValue("daysAsNew", 1);
//pocet dni za ktore testujeme zmenu (ak nenastala zmena za tento pocet dni, povazujeme stranku bez zmeny)
int daysAsChanged = pageParams.getIntValue("daysAsChanged", 7);

//datum posledneho odoslania dmailu
//System.out.println("REQ docid: " + Tools.getRequestParameter(request, "docid")+";");
//System.out.println("REQ: " + Tools.getRequestParameter(request, "lastDmailDate")+";");
long lastDmailDate = Tools.getLongValue(Tools.getRequestParameter(request, "lastDmailDate"), 0L);
if (lastDmailDate < 1)
{
   //System.out.println("dmail je null");
	Calendar cal = GregorianCalendar.getInstance();
	//zobereme poslednych 7 dni
	cal.add(Calendar.DAY_OF_YEAR, -7);
	lastDmailDate = cal.getTimeInMillis();
}
//System.out.println("dmail: " + Tools.formatDate(lastDmailDate)+" "+Tools.formatTime(lastDmailDate));

//nefunguje strankovanie, kedze vyhadzovanie zo zoznamu sa deje az tu
paging = false;
//musime vytiahnut dostatocne mnozstvo stranok
int pageSizeList = 3 * pageSize;

//ak je nastavene na true, budu sa prehladavat aj zadane poadresare
boolean expandGroupIds = pageParams.getBooleanValue("expandGroupIds", false);

docDB.getDocPerex(groupIds, orderType, ascending, publishType, pageSizeList, "novinkyAll", "pages", request);

List novinky = new ArrayList();
List novinkyAll = (ArrayList)request.getAttribute("novinkyAll");
Iterator iter = novinkyAll.iterator();
DocDetails docDetails;
int counter = 0;
while (iter.hasNext() && counter < pageSize)
{
	docDetails = (DocDetails)iter.next();

	if (lastDmailDate > 0)
	{
		//zaradim iba stranky, ktore boli zmenene od posledneho odoslania
		if (docDetails.getDateCreated() < lastDmailDate)
		{
			continue;
		}
		//System.out.println(docDetails.getTitle()+" "+Tools.formatDate(docDetails.getDateCreated())+" "+Tools.formatTime(docDetails.getDateCreated()));
	}

	novinky.add(docDetails);

	counter++;
}

if (novinky.size()==0)
{
	out.println("<span style='display: none'>SENDER: DO NOT SEND THIS EMAIL</span>");
	return;
}

request.setAttribute("novinky", novinky);

counter=0;
boolean hasPerex;
boolean hasLink;
int pageNewChangedStatus;
%>

<table border="0" cellspacing="2" cellpadding="5">
<iwcm:iterate id="doc" name="novinky" type="sk.iway.iwcm.doc.DocDetails">
   <tr>
      <td colspan="2">
	    <table border="0">
          <tr>
            <td><a href="/showdoc.do?docid=<jsp:getProperty name="doc" property="docId"/>"><h2><jsp:getProperty name="doc" property="title"/></h2></a></td>
            <td> <h2><%
	      pageNewChangedStatus = doc.getPageNewChangedStatus(daysAsNew, daysAsChanged);
			if (pageNewChangedStatus == 1)
			{
				//nova
				out.println(" - <span style='color:green'>Nová</span>");
			}
			else if (pageNewChangedStatus == 2)
			{
				out.println(" - <span style='color:red'>Zmenená</span>");
			}
			else if (pageNewChangedStatus == 0)
			{
				//ziadna zmena
			}
	     %>
      </h2>	</td>
          </tr>
        </table>

	       </td>
	</tr>
	 <tr> <td valign="top">
      <iwcm:notEmpty name="doc" property="perexImage">
			<img class="left" height='61' alt="" src="<jsp:getProperty name="doc" property="perexImage"/>" width='58' />
&nbsp;		</iwcm:notEmpty>      </td>
	   <td valign="top"><%
         String data = doc.getPerexPre();
         if (data == null || data.length()<10)
         {
            data = sk.iway.iwcm.doc.SearchAction.htmlToPlain(doc.getData());
            if (data.length()>310)
            {
               data = data.substring(0, 300) + "...";
            }
         }
         out.println(data);
         %>
	     <div class="right">
            <a href="/showdoc.do?docid=<jsp:getProperty name="doc" property="docId"/>">Viac info &gt;&gt;</a>         </div>      </td>
   </tr>
</iwcm:iterate>
</table>
