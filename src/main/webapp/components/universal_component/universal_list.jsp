<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.components.crud.UniversalCrudAction"%>
<%@page import="java.lang.reflect.Method"%>
<%@page import="sk.iway.iwcm.utils.Pair"%>
<%@page import="javax.persistence.Id"%>
<%@page import="java.lang.reflect.Field"%>
<%@page import="sk.iway.iwcm.database.JpaDB"%>
<%@page import="net.sourceforge.stripes.action.ActionBean"%>
<%@page import="sk.iway.iwcm.database.ActiveRecord"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, java.util.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><iwcm:checkLogon admin="true"/><%@
include file="/admin/layout_top.jsp" %><%

String editDialogPath = (String)request.getAttribute("universal_component_editDialog");
String listPath = (String)request.getAttribute("universal_component_list");

Class<ActiveRecord> beanClass = (Class)request.getAttribute("universal_component_beanClass");
JpaDB dbInstance = (JpaDB)request.getAttribute("universal_component_dbInstance");
if (dbInstance==null)
{
	out.print("Cannot instantiate DB class.");
	return;
}

Class<ActionBean> stripesClass = (Class)request.getAttribute("universal_component_stripesClass");
if (stripesClass==null)
	stripesClass = (Class)UniversalCrudAction.class;
%>
<script language="JavaScript">
	var helpLink = "components/captcha.jsp&book=components";

	function edit(id)
	{
		openPopupDialogFromLeftMenu("<%=editDialogPath %>?id="+id);
	}

	function confirmDelete(id)
	{
		if(window.confirm('<iwcm:text key="calendar_edit.deleteConfirm"/>') == true)
		{
			document.actionForm.elements["id"].value=id;
			document.actionForm.submit();
		}
	}
</script>

<%

List<String> filterFields =null;
Enumeration<String> reqAttNames = (Enumeration<String>)request.getAttributeNames();
while (reqAttNames.hasMoreElements())
{
	String attributeName = reqAttNames.nextElement();
	if (attributeName.startsWith("universal_component_filter.") && !attributeName.contains("select"))
	{
		if (filterFields==null)
			filterFields = new ArrayList<String>();
		filterFields.add(attributeName);
	}
}
List<Pair<String,Object>> filterArgs=null;
%>

<div class="box_tab box_tab_thin">
	<ul class="tab_menu">
		<li class="first last">
			<div class="first">&nbsp;</div>
			<a href="javascript:void(0)" id="tabLink1">
				<iwcm:text key="components.filter"/>
			</a>
		</li>
	</ul>
</div>

<div class="box_toggle">
	<div class="toggle_content">
		<div id="tabMenu1">
			<form name="groups" action="<%=listPath %>">
	          <table>
	          <tr>
	          <%
	          if (filterFields!=null && filterFields.size()>0)
	          {
	        	  Collections.sort(filterFields);
	        	  for (String attributeName : filterFields)
	        	  {
	        	  	String filterFieldName = (String)request.getAttribute(attributeName);
	        	  	String filterValue = Tools.getStringValue(Tools.getRequestParameter(request, filterFieldName), "");


	        	  	if (Tools.isNotEmpty(filterValue))
	        	  	{
	        	  		Field field = beanClass.getDeclaredField(filterFieldName);
	        	  		if (field.getType().equals("int") || field.getType().equals("Integer"))
	        	  		{
	        	  			if (filterArgs==null)
	        	  				filterArgs = new ArrayList<Pair<String,Object>>();
	        	  			Pair<String,Object> pair = Pair.of(filterFieldName, (Object)Tools.getIntValue(filterValue, 0));
	        	  			filterArgs.add(pair);
	        	  		}
	        	  		else
	        	  		{
	        	  			if (filterArgs==null)
	        	  				filterArgs = new ArrayList<Pair<String,Object>>();
	        	  			Pair<String,Object> pair = Pair.of(filterFieldName, (Object)filterValue);
	        	  			filterArgs.add(pair);
	        	  		}
	        	  	}
	        	  	if (request.getAttribute(attributeName+".select")!=null)
	        	  	{
	        	  		Collection collection = (Collection)request.getAttribute(attributeName+".select");
						String label = Tools.getStringValue((String)request.getAttribute(attributeName+".select.label"), "");
						String value = Tools.getStringValue((String)request.getAttribute(attributeName+".select.value"), "");
						Method labelMethod = null;
						Method valueMethod = null;
	        	  		%>
						<td><label for="<%=filterFieldName %>"><iwcm:text key='<%=beanClass.getSimpleName()+"."+filterFieldName %>'/>:</label></td>
						<td>
							<select name="<%=filterFieldName %>" id="<%=filterFieldName %>">
								<option> </option>
								<% for (Object object : collection)
								{
									if (Tools.isNotEmpty(label) && labelMethod==null)
									{
										try {
											labelMethod = object.getClass().getMethod("get"+(label.substring(0, 1)).toUpperCase() + (label.substring(1)), null);

										} catch (NoSuchMethodException nsme)
										{ }
									}
									if (Tools.isNotEmpty(value) && valueMethod==null)
									{
										try {
											valueMethod = object.getClass().getMethod("get"+(value.substring(0, 1)).toUpperCase() + (value.substring(1)), null);

										} catch (NoSuchMethodException nsme)
										{ }
									}
									%>
									<option value="<%=valueMethod!=null?String.valueOf(valueMethod.invoke(object, null)):String.valueOf(object) %>" <%=filterValue.equals(valueMethod!=null?String.valueOf(valueMethod.invoke(object, null)):String.valueOf(object))?"selected=\"selected\"":"" %>><%=labelMethod!=null?String.valueOf(labelMethod.invoke(object, null)):String.valueOf(object) %></option>
									<%
								}%>
							</select></td>
		        		<%
	        	  	}
	        	  	else
	        	  	{
		        	  	%>
						<td><label for="<%=filterFieldName %>"><iwcm:text key='<%=beanClass.getSimpleName()+"."+filterFieldName %>'/>:</label></td>
						<td><input type="text" name="<%=filterFieldName %>" id="<%=filterFieldName %>" value="<%=filterValue %>" /></td>
		        		<%
	        	  	}
	        	  }

	        	  %><td><input type="submit" class="button50" value="<iwcm:text key="components.filter"/>"  /></td><%

	          }
	          %>

	          </tr>
	          </table>
        	</form>
		</div>
	</div>
</div>
<%
List items = null;
if (filterArgs!=null && filterArgs.size()>0)
{
	items = dbInstance.findByProperties(filterArgs.toArray(new Pair[filterArgs.size()]));
}
else
{
	items = dbInstance.getAll();
}

Identity user = UsersDB.getCurrentUser(request);

String pathToAction = "/" + DB.replace(DB.replace(DB.replace(stripesClass.getCanonicalName(), ".", "/"), "ActionBean", ".action"), "Action", ".action");
if (Tools.getRequestParameter(request, "bDelete")!=null)
{
	pageContext.include(pathToAction);
	%>
	<script language="JavaScript">
		window.location.href='<%=listPath %>';	//kvoli deleteId, ktore zostavalo nastavene aj po pridani novej sutazi
	</script>
	<%
}

request.setAttribute("items", items);

%>
<h1><iwcm:text key='<%=beanClass.getSimpleName() + ".items" %>'/></h1>

<form action="<%=PathFilter.getOrigPath(request)%>" name="actionForm" style="display: none;">
	<input type="hidden" name="bDelete" value="true" />
	<input type="hidden" name="id" value="" />
</form>
<%
    Field[] fields = beanClass.getDeclaredFields();

	//pole id je specificke dame si ho bokom, aby sme ho vedeli dat na zaciatok
	Field idField = null;
	for (Field field : fields)
	{
		if (field.isAnnotationPresent(Id.class))
		{
			idField = field;
			break;
		}
	}
%>
<display:table class="sort_table" export="true" cellspacing="0" cellpadding="0" name="items" uid="row" requestURI="<%=PathFilter.getOrigPath(request)%>" pagesize="30" >

	<display:column titleKey='<%=beanClass.getSimpleName()+"."+idField.getName() %>' sortable="true" sortProperty="<%=idField.getName() %>">
 		<a href="javascript:edit(<iwcm:beanWrite name="row" property="<%=idField.getName() %>"/>)"><iwcm:beanWrite name="row" property="<%=idField.getName() %>"/></a>
	</display:column>
	<%
  	for (Field field : fields)
	{
  		if (field.getName().equals("serialVersionUID"))
  		{
  			// do nothing
  		}
  		else if (field.isAnnotationPresent(Id.class))
		{
  			// do nothing
		}
  		else if (field.getType().getSimpleName().equalsIgnoreCase("boolean"))
  		{%>
  			<display:column titleKey='<%=beanClass.getSimpleName()+"."+field.getName() %>' sortable="true" property="<%=field.getName() %>"  media="xml pdf excel csv" />
  			<display:column titleKey='<%=beanClass.getSimpleName()+"."+field.getName() %>' sortable="true" property="<%=field.getName() %>" decorator="sk.iway.displaytag.BooleanDecorator" media="html" />
  		<%
  		}
		else
		{
			%><display:column titleKey='<%=beanClass.getSimpleName()+"."+field.getName() %>' sortable="true" property="<%=field.getName() %>" /><%
		}
	}
   %>
   	<display:column style="text-align:center;" media="html" titleKey="components.captcha.tools">
   		<a href="javascript:edit(<iwcm:beanWrite name="row" property="<%=idField.getName() %>"/>)" title='<iwcm:text key="components.banner.edit"/>' class="iconEdit">&nbsp;</a>
		<a href="javascript:confirmDelete(<iwcm:beanWrite name="row" property="<%=idField.getName() %>"/>);" title='<iwcm:text key="button.delete"/>' class="iconDelete">&nbsp;</a>
 	</display:column>


</display:table>

<%@ include file="/admin/layout_bottom.jsp" %>
