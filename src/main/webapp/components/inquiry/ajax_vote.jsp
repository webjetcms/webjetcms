<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.users.*,java.io.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>

		function trim(str, chars) {
		    return ltrim(rtrim(str, chars), chars);
		}

		function ltrim(str, chars) {
		    chars = chars || "\\s";
		    return str.replace(new RegExp("^[" + chars + "]+", "g"), "");
		}

		function rtrim(str, chars) {
		    chars = chars || "\\s";
		    return str.replace(new RegExp("[" + chars + "]+$", "g"), "");
		}
	
		function anketa(url, width, height, qid, optionalRedirect){
		 	var options = "toolbar=no,scrollbars=no,resizable=no,width="+width+",height="+height+";";
		 	var returnText;
			$.ajax({
			    url: url,
			    type: 'GET',
			    dataType: 'html',
			    timeout: 1000,
			    error: function(){ alert('<iwcm:text key="inquiry.errorLoadingAnswer"/>'); },
			    success: function(html){
				    var delimiter = '<BODY class="inquiryPopup">';
					reply = html.substring ( html.indexOf(delimiter) + delimiter.length, html.indexOf("</BODY>"));
					reply = trim(reply);
					alert( reply );
					if ((optionalRedirect == undefined) || (optionalRedirect == null))
					{
						if(qid==undefined || qid==null)
				    		reloadPollDiv();
				    	else 
				    		reloadPollDivId(qid);
					}
			    	else
			    		window.location.href = optionalRedirect;
			    }
			});
		}
		
		function anketaMulti(form, qID)
		{
			var url = "<iwcm:cp/>/inquiry.answer.do?la=multipleAnswer&qID="+qID.value;
			var somethinkSelected = false;
			for (i=0; i<form.selectedAnswers.length; i++)
			{
			   if (form.selectedAnswers[i].checked)
			   {
				   url = url + "&selectedAnswers="+form.selectedAnswers[i].value;
				   somethinkSelected = true;
			   }
			}
			if (somethinkSelected) anketa(url, 200, 300, qID.value);
			
		}
		
		function reloadPollDiv(){
			$('#resultsDiv').load('/components/inquiry/voteResultsDiv.jsp?showResults=true');
		}

		function reloadPollDivId(qid)
		{						
			if (document.getElementById('resultsDiv-'+qid) != null) $('#resultsDiv-'+qid).load('/components/inquiry/voteResultsDiv.jsp?qID='+qid+'&showResults=true');
			else $('#resultsDiv').load('/components/inquiry/voteResultsDiv.jsp?qID='+qid+"&showResults=true");
		}
