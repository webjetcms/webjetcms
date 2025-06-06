<%@page import="sk.iway.iwcm.database.JpaDB"%>
<%@page import="java.lang.reflect.ParameterizedType"%>
<%@page import="sk.iway.iwcm.system.stripes.WebJETActionBean"%>
<%@page import="sk.iway.iwcm.components.crud.UniversalCrudAction"%>
<%@page import="sk.iway.iwcm.database.ActiveRecord"%>
<%@page import="javax.persistence.Id"%>
<%@page import="net.sourceforge.stripes.action.ActionBean"%>
<%@page import="java.lang.reflect.Field"%>
<%
  sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, sk.iway.iwcm.system.*, java.util.*, java.util.List"%><%@
taglib  prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"  uri="/WEB-INF/iway.tld"%><%@
taglib prefix="display"  uri="/WEB-INF/displaytag.tld"%><%@
taglib prefix="stripes"  uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib  prefix="c" uri="http://java.sun.com/jsp/jstl/core"%><iwcm:checkLogon admin="true"/><%

///  Ciel: kedze nemam rad opiciu robotu, chcem vytvorit univerzalne komponenty,
///  pre zaciatok pre admin rozhranie, pre jednoduch spravu roznych objektov ulozenych v db
///  Univerzalna edit komponenta, v zaklade vyzaduje do requestu nastavit nazov beanu v actionBeane,
///   triedu beanClassy a triedu actionBeany

Prop prop = Prop.getInstance(request);

String beanName = (String)request.getAttribute("universal_component_beanName");
Class<ActiveRecord> beanClass = (Class)request.getAttribute("universal_component_beanClass");
JpaDB dbInstance = (JpaDB)request.getAttribute("universal_component_dbInstance");
if (dbInstance==null)
{
	out.print("Cannot instantiate DB class.");
	return;
}

if (beanClass==null)
{
	beanClass = (Class) ((ParameterizedType) dbInstance.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	request.setAttribute("universal_component_beanClass", beanClass);
}
Class<ActionBean> stripesClass = (Class)request.getAttribute("universal_component_stripesClass");
if (beanName==null)
	beanName = "object";
if (stripesClass==null)
	stripesClass = (Class)UniversalCrudAction.class;

if (beanName==null || beanClass==null || stripesClass==null)
{
	out.print("<p>V requeste sa nenachadzaju atributy beanName, beanClass alebo stripesClass, nieje mozne pokracovat.</p>");
	return;
}
request.setAttribute("cmpName", beanClass.getSimpleName());
%><%@ include file="/admin/layout_top_dialog.jsp"%>
<%
// TODO: cmpName

%>
<script type="text/javascript" src="/components/form/check_form.js"></script>
<script type="text/javascript" src="/admin/scripts/dateTime.jsp"></script>
<%=Tools.insertJQuery(request)%>
<%=Tools.insertJQueryUI(pageContext, "datepicker")%>
<script type="text/javascript" >
jQuery(function(a)
		{
		a.datepicker.regional.webjet =
		{
		closeText:" Zavrieť",
		prevText:"&#x3cPredchádzajúci;",
		nextText:"Nasledujúci&#x3e;",
		currentText:"Dnes",
		monthNames:["Január","Február","Marec","Apríl","Máj","Jún","Júl","August","September","Október","November","December"],
		monthNamesShort:["Jan","Feb","Mar","Apr","Máj","Jún","Júl","Aug","Sep","Okt","Nov","Dec"],
		dayNames:["Nedeľa","Pondelok","Utorok","Streda","Štvrtok","Piatok","Sobota"],
		dayNamesShort:["Ned","Pon","Uto","Str","Štv","Pia","Sob"],
		dayNamesMin:["Ne","Po","Ut","St","Št","Pia","So"],
		dateFormat:"dd.mm.yy",
		firstDay:0,
		isRTL:false
		};
		a.datepicker.setDefaults(a.datepicker.regional.webjet);
		});
</script><%

//zatial takto hlupo :)
String pathToAction = "/" + DB.replace(DB.replace(DB.replace(stripesClass.getCanonicalName(), ".", "/"), "ActionBean", ".action"), "Action", ".action");

if (Tools.getRequestParameter(request, "bSave")!=null)
{
  pageContext.include(pathToAction);
}
%>
<script type="text/javascript" src="/components/form/check_form.js"></script>
<c:if test='<%=request.getAttribute("saveOk") != null%>'>
	<script type="text/javascript">
		<!--
			window.opener.location.reload();
			window.close();
		//-->
	</script>
</c:if>
<% int numOfRows = 0; %>
<stripes:useActionBean var="actionBean" beanclass="<%=stripesClass.getCanonicalName()%>" />
<iwcm:stripForm action="<%=PathFilter.getOrigPath(request)%>"  beanclass="<%=stripesClass.getCanonicalName()%>" name="editForm" method="post">
<stripes:errors />
 <div class="padding10">
	<table>
	<%
	//daj mi zoznam vsetkych deklarovanych poli v beanClasse, vyfiltruj nepotrebne (ba priam nezelane...) polia,
	// pole anotovane @Id nastav ako skryte
	// plus porob nejake usefull chujovinky typu boolean na checkbox, Date na datepicker...
	Field[] fields = beanClass.getDeclaredFields();
	for (Field field : fields)
	{
		numOfRows++;
		if (field.getName().equals("serialVersionUID"))
			continue;
		else if (field.isAnnotationPresent(Id.class))
		{%>
			<tr style="display: none;">
				<td></td>
				<td><stripes:hidden name='<%=beanName+"."+field.getName()%>' /></td>
			</tr>
		<%
		}
		else if (field.getType().getSimpleName().equalsIgnoreCase("boolean"))
		{
			//boolean polia by som chcel mat checkboxy...
			%>
			<tr>
				<td><%=prop.getText(beanClass.getSimpleName()+"."+field.getName()) %>:</td>
				<td><stripes:checkbox name='<%=beanName+"."+field.getName()%>' /></td>
			</tr>
			<%
		}
		else if (request.getAttribute("universal_component_"+field.getName()+".select")!=null)
		{
			%>
			<tr>
				<td><%=prop.getText(beanClass.getSimpleName()+"."+field.getName()) %>:</td>
				<td><stripes:select name='<%=beanName+"."+field.getName()%>' style="width: 250px;">
				<%
					Collection collection = (Collection)request.getAttribute("universal_component_"+field.getName()+".select");
					String label = Tools.getStringValue((String)request.getAttribute("universal_component_"+field.getName()+".select.label"), "");
					String value = Tools.getStringValue((String)request.getAttribute("universal_component_"+field.getName()+".select.value"), "");
					if (Tools.isNotEmpty(label) || Tools.isNotEmpty(value))
					{
						%><stripes:options-collection collection="<%=collection %>" value="<%=value %>" label="<%=label %>"/><%
					}
					else
					{
						for (Object itValue : collection)
						{
							String itLabel = prop.getText(beanClass.getSimpleName()+"."+field.getName()+".option."+String.valueOf(itValue));
							if (itLabel.equals(beanClass.getSimpleName()+"."+field.getName()+".option."+String.valueOf(itValue)))
								itLabel = String.valueOf(itValue);
							%>
							<stripes:option value="<%=String.valueOf(itValue)%>" ><%=itLabel %></stripes:option>
							<%
						}
					}%>
				</stripes:select></td>
			</tr><%
		}
		else if (field.getType().getSimpleName().equalsIgnoreCase("date"))
		{
			%>
			<tr>
				<td><%=prop.getText(beanClass.getSimpleName()+"."+field.getName()) %>:</td>
				<td><stripes:text name='<%=beanName+"."+field.getName()%>' size="20" onblur="checkDate(this);return false;" class="input datepicker"/></td>
			</tr>
			<%
		}
		else
		{
			%>
			<tr>
				<td><%=prop.getText(beanClass.getSimpleName()+"."+field.getName()) %>:</td>
				<td><stripes:text name='<%=beanName+"."+field.getName()%>' size="47"  /></td>
			</tr>
			<%
		}
	}
	%>
		<tr style="display: none;">
			<td></td>
			<td><input type="submit" name="bSave" value="<iwcm:text key="button.submit"/>" /></td>
		</tr>
	</table>
</div>




</iwcm:stripForm>
<script type="text/javascript">
<!--
	var helpLink = "";
	function Ok()
	{
		document.editForm.bSave.click();
	}
	//vypocitame si vysku dialogu, nech to dobre vyzera :)
	resizeDialog(500, <%=140+(numOfRows*20)%>);
//-->
</script>

<%@ include file="/admin/layout_bottom_dialog.jsp"%>
