<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.inquiry.*, sk.iway.iwcm.i18n.*, sk.iway.iwcm.tags.*"%>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="menuInquiry"/>
<%Prop prop = Prop.getInstance(request); %>
function pridajOdpoved(odpoved)
{
	lastIndOdpoved++;
	$("tr."+odpoved+":last").after(''+
		'\n<tr class="odpoved" id="odpoved'+lastIndOdpoved+'">\n'+
	    '<td class="sort_td">\n'+
				'<input type="text" name="answers['+lastIndOdpoved+'].answerString" size="30" maxlength="255" value="" class="input" id="answerString'+lastIndOdpoved+'"/>\n'+
		 '</td>\n'+
		 '<td class="sort_td" align="center">\n'+
		 '<input id="answerImage'+lastIndOdpoved+'" type="text" name="answers['+lastIndOdpoved+'].imagePath" size="20" maxlength="255" value="" class="input" />\n'+
		 '</td>\n'+
		 '<td>\n'+
			'<a href="javascript:void(0);" onclick="openImageDialogWindow(\'inquiryAnswerForm\', \'answers['+lastIndOdpoved+'].imagePath\', null)">\n'+ 
				'<img src="/admin/images/icon-edit.png" alt="<%=prop.getText("components.inquiry.dialog.set.image") %>" title="<%=prop.getText("components.inquiry.dialog.set.image") %>" />\n'+ 
			'</a>\n'+
		 '</td>\n'+
		 '<td class="sort_td" align="center">\n'+
			 '<input type="text" id="answer'+lastIndOdpoved+'" name="answers['+lastIndOdpoved+'].url" size="20" maxlength="255" value="" class="input" />\n'+
		 '</td>\n'+
		 '<td>\n'+
			 '<a href="javascript:void(0);" onclick="openLinkDialogWindow(\'inquiryAnswerForm\', \'answers['+lastIndOdpoved+'].url\', null)">\n'+ 
				 '<img src="/admin/images/icon-edit.png" alt="<%=prop.getText("components.inquiry.dialog.set.url") %>" title="<%=prop.getText("components.inquiry.dialog.set.url") %>" />\n'+ 
			 '</a>\n'+
		 '</td>\n'+
		 '<td class="sort_td" align="center">\n'+
			 '<input type="text" id="answer'+lastIndOdpoved+'" name="answers['+lastIndOdpoved+'].answerClicks" size="4" maxlength="255" value="" class="input" />\n'+
		 '</td>\n'+
		 '<td>\n'+
			 '<a href="#" onclick="vymazOdpoved('+lastIndOdpoved+',\'<%=prop.getText("inquiry.delete_answer.confirm")%>\');" title="<%=prop.getText("button.delete") %>" class="iconDelete">&nbsp;</a>\n'+
		 '</td>\n'+
		'</tr>');            
}

function vymazOdpoved(odpovedId, text)
{
  if (window.confirm(text))
  {
  	  $("#odpoved"+odpovedId).hide();
     $("#answerString"+odpovedId).val("");	
  }
}

function htmlSelectTagAddOption(select)
{
	if (select.value == "<%=SelectTag.NEW_OPTION_VALUE%>")
	{
		var myValue = window.prompt("<%=prop.getText("html_tags_support.enter_new_value") %>", "");
		myValue = ""+myValue;
	   if (myValue!="" && myValue!="null" && myValue!="undefined")
	   {
	   	var optionName = new Option(myValue, myValue, true, true)
	      select.options[select.length] = optionName;      
	   }
	   else
	   {
	      select.selectedIndex = 0;
	   }
	}
}
