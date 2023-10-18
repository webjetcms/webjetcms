<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway"%>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"%>

<%=Tools.insertJQueryUI(pageContext, "dialog")%>
<%
if (request.getAttribute("/common/dialog.jsp-inserted")!=null) return;
request.setAttribute("/common/dialog.jsp-inserted", "1");
%>

<script type="text/javascript">
	<!--

		var link = document.createElement("link");
		link.href = "/components/dialog-style.css";
		link.type = "text/css";
		link.rel = "stylesheet";
		var head = document.getElementsByTagName('HEAD')[0];
		head.appendChild(link);

		var currentComponent;
		// otvorenie dialogoveho okna s formularom
		// parametre width, title a buttons nie su povinne
		function openWJDialog(component, url, width, title, buttons)
		{
			currentComponent = component;

			//window.alert($("div#" + component).length);

			if ($("div#" + component).length == 0) {
				$("body").append('<div id="' + component + '" class="' + component + '"><div id="' + component + 'ContentHolder"></div></div>');
			}

			var urlData = url.split('?');

			url 	= urlData[0];
			var params 	= urlData[1];
			width 	= typeof(width) != 'undefined' ? width : 600;
			width 	= parseInt(width);
			title 	= typeof(title) != 'undefined' ? title : typeof $("div#" + component).attr('title') != "undefined" ? $("div#" + component).attr('title') : '';
			buttons = typeof(buttons) == 'undefined' ? true : false;

			if (component == 'sendLink' && title == '') {
				title = '<iwcm:text key="components.send_link.title" />'
			}
			else if (component == 'sendLinkGallery' && title == '') {
				title = '<iwcm:text key="components.send_link_gallery.title" />'
			}
			else if (component == 'forum' && title == '') {
				title = '<iwcm:text key="components.forum.title" />'
			}
			else if (title == '') {
				title = '<iwcm:text key="components.dialog.title" />'
			}

			$("div#" + component).dialog({
				autoOpen: false,
				resizable: false,
				width: width,
				title: title,
				modal: true,
				close: function( event, ui ) {
					closeDialog();
				}
			});

			if(buttons)
			{
				$("div#" + component).dialog( "option", "buttons", {
					'close': {
						click: function() {
							closeDialog();
						},
						text: '<iwcm:text key="components.send_link.buttons.close" />',
						class: 'btn btn-secondary'
					},
					'send': {
						click: function() {
							sendForm();
						},
						text: '<iwcm:text key="components.send_link.buttons.send" />',
						class: "btn btn-primary"
					}
				});
			}

			if (component == 'sendLinkGallery') {
				$.prettyPhoto.close();
				$("div#" + component).dialog("open");
			}

			$.ajax({
				type: "POST",
				url: url,
				data: params,
				success: function(msg){
					$('#' + component).html(msg);
					if (msg.indexOf("<"+"form")==-1)
					{
						//ak tam nie je form tag zobrazime len close button
						$("div#" + component).dialog('option', 'buttons', {
							'close': {
								click: function() {
									closeDialog();
								},
								text: '<iwcm:text key="components.send_link.buttons.close" />',
								class: "btn btn-primary"
							}
						});
					}
					$('#' + component).dialog('open');
				},
				error: function(msg){
					$("div#" + component).html(msg);
					$("div#" + component).dialog('option', 'buttons', {
						'close': {
							click: function() {
								clearTimeout(timeSet);
								closeDialog();
							},
							text: '<iwcm:text key="components.send_link.buttons.close" />',
							class: "btn btn-primary"
						}
					});
					$("div#" + component).dialog('open');
					var timeSet = setTimeout('closeDialog()', 10000);
				}
			});
		}

		// zatvorenie a odstranenie dilogu
		function closeDialog()
		{
			//console.log("closeDialog, currentComponent=", currentComponent);
			//window.alert("close, currentComponent="+currentComponent);
			$("div#" + currentComponent).dialog( "destroy" );
			$("div#" + currentComponent).remove();
			$("div.cleditorPopup").remove();
		}

		function sendForm()
		{
			var check = checkForm.recheckAjax(document.getElementById(currentComponent).getElementsByTagName('form')[0]);
			if (check==false)
			{
				return false;
			}
			else
			{
				//po odoslani zmenim text z Odoslat na Cakajte
				$(".ui-dialog-buttonpane .btnSubmit:eq(1)").val('<iwcm:text key="components.send_link.buttons.wait" />');

				if (currentComponent ==  'sendLinkGallery') {
					appendEmails();
				}

				//textareaUpdate(document.getElementById('wysiwyg'));

				var values = new Array();
				var valuesCounter = 0;

				//encodeURIComponent;
			    $('.ui-dialog form input, .ui-dialog form select, .ui-dialog form textarea').each(function(i, el)
				 {
					 if (el.type!="checkbox" || el.checked == true)
					 {
				        values[valuesCounter] = el.name + "=" +encodeURIComponent($(el).val());
				        valuesCounter++;
					 }
			    });

			    var data = values.join("&"); // rozparsovany string POST data
				var formAction = $(".ui-dialog form").attr("action");

			   <% if ("utf-8".equals(sk.iway.iwcm.SetCharacterEncodingFilter.getEncoding())==false) { %>
				//aby filter vedel, ze ideme cez AJAX a sme UTF-8
				if (formAction.indexOf("?")==-1) formAction = formAction + "?ajax_utf-8=1";
				else formAction = formAction + "&ajax_utf-8=1";
				<% } %>

				$.ajax({
					url: formAction,
					type: "POST",
					data: data,
					success: function(msg)
					{
						if (msg.indexOf("Zadaný text z obrázku nie je zhodný") != -1)
						{
							$("td.captcha img").attr("src", "/captcha.jpg?rnd="+(new Date().getTime()));
							if ($("div.error").length == 0) {
								var error = '<div class="error">'+msg+'</div>';
								$("div#" + currentComponent).append(error);
							}
						}
						else
						{
							//console.log($("div#" + currentComponent).html());
							//console.log(msg);
							$("div#" + currentComponent).html(msg);
							try
							{
								$("div#" + currentComponent).dialog('option', 'buttons', {
									'close': {
										click: function()
										{
											closeDialog();
											try { clearTimeout(timeSet); } catch (e) {}
										},
										text: '<iwcm:text key="components.send_link.buttons.close" />',
										class: 'btn btn-primary'
									}
								});
							}
							catch (e2) {}
							var timeSet = setTimeout(closeDialog, 3000);
						}

					},
					error: function(msg){
						$("div#" + currentComponent).html(msg);
						$("div#" + currentComponent).dialog('option', 'buttons', {
							'close': {
								click: function() {
									closeDialog();
								},
								text: '<iwcm:text key="components.send_link.buttons.close" />',
								class: 'btn btn-primary'
							}
						});
					}
				});
			}
		}
	//-->
</script>