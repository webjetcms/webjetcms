
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true" perms="menuWebpages"/>
<!--
 * FCKeditor - The text editor for internet
 * Copyright (C) 2003-2004 Frederico Caldeira Knabben
 * 
 * Licensed under the terms of the GNU Lesser General Public License:
 * 		http://www.opensource.org/licenses/lgpl-license.php
 * 
 * For further information visit:
 * 		http://www.fckeditor.net/
 * 
 * File Name: fckdialog.html
 * 	This page is used by all dialog box as the container.
 * 
 * Version:  2.0 RC1
 * Modified: 2004-11-30 10:26:36
 * 
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
-->   
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
	<head>
	   <meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">
		<meta name="robots" content="noindex, nofollow" />		
		
	   <script type="text/javascript">
	   
	      //window.alert("dialog test");
	      var FCKConfig = new Object();
	      var FCKLang = new Object();
	      FCKConfig.LinkUploadAllowedExtensions = "jpg,gif,pdf";
	      
	      var FCKLanguageManager = new Object() ;
	      FCKLanguageManager.AvailableLanguages = 
			{
				'ar'		: 'Arabic',
				'bg'		: 'Bulgarian',
				'bs'		: 'Bosnian',
				'ca'		: 'Catalan',
				'cs'		: 'Czech',
				'da'		: 'Danish',
				'de'		: 'German',
				'en'		: 'English',
				'eo'		: 'Esperanto',
				'es'		: 'Spanish',
				'et'		: 'Estonian',
				'fa'		: 'Persian',
				'fi'		: 'Finnish',
				'fr'		: 'French',
				'gl'		: 'Galician',
				'gr'		: 'Greek',
				'he'		: 'Hebrew',
				'hr'		: 'Croatian',
				'hu'		: 'Hungarian',
				'it'		: 'Italian',
				'ja'		: 'Japanese',
				'ko'		: 'Korean',
				'lt'		: 'Lithuanian',
				'nl'		: 'Dutch',
				'no'		: 'Norwegian',
				'pl'		: 'Polish',
				'pt'		: 'Portuguese (Portugal)',
				'pt-br'		: 'Portuguese (Brazil)',
				'ro'		: 'Romanian',
				'ru'		: 'Russian',
				'sk'		: 'Slovak',
				'sl'		: 'Slovenian',
				'sr'		: 'Serbian (Cyrillic)',
				'sr-latn'	: 'Serbian (Latin)',
				'sv'		: 'Swedish',
				'th'		: 'Thai',
				'tr'		: 'Turkish',
				'zh'		: 'Chinese Traditional',
				'zh-cn'		: 'Chinese Simplified'
			}
			
			FCKLanguageManager.TranslateElements = function( targetDocument, tag, propertyToSet )
			{
				var aInputs = targetDocument.getElementsByTagName(tag) ;
			
				for ( var i = 0 ; i < aInputs.length ; i++ )
				{
					var sKey = aInputs[i].getAttribute( 'fckLang' ) ;
					
					if ( sKey )
					{
						var s = FCKLang[ sKey ] ;
						if ( s ) 
							eval( 'aInputs[i].' + propertyToSet + ' = s' ) ;
					}
				}
			}
			
			FCKLanguageManager.TranslatePage = function( targetDocument )
			{
				this.TranslateElements( targetDocument, 'INPUT', 'value' ) ;
				this.TranslateElements( targetDocument, 'SPAN', 'innerHTML' ) ;
				this.TranslateElements( targetDocument, 'LABEL', 'innerHTML' ) ;
				this.TranslateElements( targetDocument, 'OPTION', 'innerHTML' ) ;
			}
			<%
			String lng = (String)session.getAttribute(Prop.SESSION_I18N_PROP_LNG);
			if (Tools.isEmpty(lng)) lng = "sk";
			%>
			FCKLanguageManager.DefaultLanguage = '<%=lng%>' ;
			
			FCKLanguageManager.ActiveLanguage = new Object() ;
			FCKLanguageManager.ActiveLanguage.Code = "<%=lng%>";
			FCKLanguageManager.ActiveLanguage.Name = "<%=lng%>";
			
			var FCKBrowserInfo ;
		   var NS;
		   if (!(NS=window.parent.__FCKeditorNS)) NS=window.parent.__FCKeditorNS=new Object();
			if ( !( FCKBrowserInfo = NS.FCKBrowserInfo ) )
			{
				FCKBrowserInfo = NS.FCKBrowserInfo = new Object() ;
			
				var sAgent = navigator.userAgent.toLowerCase() ;
			
				FCKBrowserInfo.IsIE			= sAgent.indexOf("msie") != -1 ;
				FCKBrowserInfo.IsGecko		= !FCKBrowserInfo.IsIE ;
				FCKBrowserInfo.IsNetscape	= sAgent.indexOf("netscape") != -1 ;
			}
		
	   </script>
		<script type="text/javascript" src="<iwcm:cp/>/admin/FCKeditor/editor/lang/<%=lng%>.js"></script>
		<%=Tools.insertJQuery(request) %>
		<script type="text/javascript">

			
			var sAgent = navigator.userAgent.toLowerCase() ;
			var IsIE	= sAgent.indexOf("msie") != -1 ;
			var isInlineEditingWindow = true;
			
			try
			{
				if (!IsIE)
				{
					dialogArguments = window.opener.WJInlineDialogArguments;
				}
				
				var sTitle = dialogArguments.Title ;
				document.write( '<title>' + sTitle + '</title>' ) ;
			}
			catch (e) { console.log(e); }
			
			function LoadInnerDialog()
			{
				try
				{
					if ( window.onresize )
						window.onresize() ;
				
					window.frames["frmMain"].document.location.href = dialogArguments.Page ;
				} catch (e) {console.log(e);}
			}
			
			function InnerDialogLoaded()
			{
				var oInnerDoc = document.getElementById('frmMain').contentWindow.document ;
							
				SetOnKeyDown( oInnerDoc ) ;
				DisableContextMenu( oInnerDoc ) ;
				
				//window.alert("FCKLanguageManager="+FCKLanguageManager);
				
				if (FCKLang) dialogArguments.Editor.FCKLang = FCKLang;
				if (FCKLanguageManager) dialogArguments.Editor.FCKLanguageManager = FCKLanguageManager;
				if (FCKBrowserInfo) dialogArguments.Editor.FCKBrowserInfo = FCKBrowserInfo;
				
				try
				{				
					if ( window.onresize )
						window.onresize() ;
				} catch (e) {}	
												
				return this ;
			}
			
			function SetOkButton( showIt )
			{
				document.getElementById('btnOk').style.visibility = ( showIt ? '' : 'hidden' ) ;
	
				window.setTimeout(switchToTab2, 50);
			}
			
			function switchToTab2()
			{
				try
				{
					var oInnerDoc = document.getElementById('frmMain').contentWindow.document ;
					var tabLink2 = oInnerDoc.getElementById("tabLink2");
					if (tabLink2 != null) tabLink2.click();
				} catch (e) {}
			}
			
			var bAutoSize = false ;
			function SetAutoSize( autoSize )
			{
				bAutoSize = autoSize ;
				RefreshSize() ;
			}

			function RefreshSize()
			{
				if (!IsIE && window.onresize) window.setTimeout(window.onresize, 100);
				RefreshSizeImpl();
			}
			
			function RefreshSizeImpl()
			{
				if ( bAutoSize )
				{
					
					
					var oInnerDoc = document.getElementById('frmMain').contentWindow.document ;

					var iFrameHeight ;
					if ( window.dialogHeight )
						iFrameHeight = oInnerDoc.body.offsetHeight ;
					else
						iFrameHeight = document.getElementById('frmMain').contentWindow.innerHeight ;

					//window.alert(document.getElementById('frmMain').contentWindow.innerHeight);
					
					var iInnerHeight = oInnerDoc.body.scrollHeight ;

					var iDiff = iInnerHeight - iFrameHeight ;
					
					//window.alert("iDiff="+iDiff+" iInnerHeight="+iInnerHeight+" iFrameHeight="+iFrameHeight);
					
					if ( iDiff > 0 )
					{
						if ( window.dialogHeight )
							window.dialogHeight = ( parseInt( window.dialogHeight, 10 ) + iDiff ) + 'px' ;
						else
							window.resizeBy( 0, iDiff ) ;
					}
					
					//width
					var iFrameWidth = 0;
					if ( window.dialogWidth )
						iFrameWidth	= oInnerDoc.body.offsetWidth ;
					else
						iFrameWidth	= document.getElementById('frmMain').contentWindow.innerWidth ;
					
					var iInnerWidth	= oInnerDoc.body.scrollWidth ;
					iDiff = iInnerWidth - iFrameWidth ;
				
					//WebJET - pre FF3 to musime spravit sirsie	
					if (navigator.userAgent.indexOf("Firefox/3")!=-1)
					{
						iDiff = iDiff + 0;
					}

					if ( iDiff > 0 )
					{
						if ( window.dialogWidth )
						{
							window.dialogWidth = ( parseInt( window.dialogWidth ) + iDiff ) + 'px' ;
						}
						else
						{
							window.resizeBy( iDiff, 0 ) ;
						}
					}
					
				}
			}			
			
			function Ok()
			{
				if ( window.frames["frmMain"].Ok)
				{
					window.frames["frmMain"].Ok();	
				}				
			}
			
			function reloadParentWindow()
			{
				try
				{
					var href = dialogArguments.Editor.location.href;
					if (href.indexOf("?")==-1) href += "?";
					else href += "&";
					
					var start = href.indexOf("dirnd");
					if (start!=-1)
					{
					   var end = href.indexOf("&", start);
					   if (end > start) href = href.substring(0, start) + href.substring(end+1);
					   else href = href.substring(0, start);
					}
					
					href += "dirnd="+(new Date()).valueOf();
					
					dialogArguments.Editor.location.href = href;
				} catch (e) {}
			}
			
			function Cancel()
			{			
				try 
				{
					if ( window.frames["frmMain"].Cancel)
					{
						if (window.frames["frmMain"].Cancel()==false) return;	
					}	
				}
				catch (e) {	}
				
				reloadParentWindow();
				
				window.close() ;
			}
			
			//fix pre IE10, ktoremu vadilo meno Cancel
			function CancelClick()
			{
				Cancel();
			}
			
			function windowOnUnload()
			{
				reloadParentWindow();
			}
			
			//Object that holds all available tabs.
			var oTabs = new Object() ;
			
			function TabDiv_OnClick()
			{
				children = document.getElementById('Tabs').children;
				
				for (i = 0; i < children.length; i++)
				{
					child = children[i];
					child.className = child.className.replace("openFirst", "").replace("openLast", "").replace("openOnlyFirst", "").replace("open", "");
				}
				if(children.length == 1){
					this.parentNode.className = "openOnlyFirst";			
				}else{
					if (this.parentNode.className.indexOf("last") != -1){
						this.parentNode.className += " openLast";
					}else{
						if(this.parentNode.className.indexOf("first") != -1){
							this.parentNode.className += " openFirst";
						}else{
							this.parentNode.className = "open";
						}
					}
				}
			
				SetSelectedTab( this.TabCode ) ;
			}
			
			var liLast;
			
			function AddTab( tabCode, tabText, startHidden){
				
				if ( typeof( oTabs[ tabCode ] ) != 'undefined' ){
					return ;
				}
			
				var eUl = document.getElementById( 'Tabs' ) ;
				var li = document.createElement("LI");
				var oCell = eUl.appendChild(li) ;
				
				if(liLast)
					liLast.className = liLast.className.replace("last", " ").replace("openOnlyFirst", " ");
				
				if(eUl.children.length == 1)
					li.className = 'first openFirst openOnlyFirst';
			
				var oDiv = document.createElement( 'A' ) ;
				oDiv.className = 'PopupTab' ;
				oDiv.innerHTML = tabText ;
				oDiv.TabCode = tabCode ;
				oDiv.onclick = TabDiv_OnClick ;
			
				if(startHidden)
					oDiv.style.display = 'none' ;
			
				eTabsRow = document.getElementById( 'TabsRow' ) ;
			
				oCell.appendChild( oDiv ) ;
			
				if ( eTabsRow.style.display == 'none' )
				{
					var eTitleArea = document.getElementById( 'TitleArea' ) ;
					eTitleArea.className = 'PopupTitle' ;
				
					oDiv.className = 'PopupTabSelected' ;
					eTabsRow.style.display = '' ;
			
					if ( ! IsIE )
						window.onresize() ;
				}
				
				oTabs[ tabCode ] = oDiv ;
				
				liLast = li;
				eUl.lastChild.className += ' last';
			}
			
			function SetSelectedTab( tabCode )
			{
				for ( var sCode in oTabs )
				{
					if ( sCode == tabCode )
						oTabs[sCode].className = 'PopupTabSelected' ;
					else
						oTabs[sCode].className = 'PopupTab' ;
				}

				if ( typeof( window.frames["frmMain"].OnDialogTabChange ) == 'function' )
					window.frames["frmMain"].OnDialogTabChange( tabCode ) ;
			}
			
			function SetTabVisibility( tabCode, isVisible )
			{
				var oTab = oTabs[ tabCode ] ;
				oTab.style.display = isVisible ? '' : 'none' ;
				
				if ( ! isVisible && oTab.className == 'PopupTabSelected' )
				{
					for ( var sCode in oTabs )
					{
						if ( oTabs[sCode].style.display != 'none' )
						{
							SetSelectedTab( sCode ) ;
							break ;
						}
					}
				}
			}
			
			function SetOnKeyDown( targetDocument )
			{
				targetDocument.onkeydown = function ( e )
				{
					var e = e || event || this.parentWindow.event ;
					switch ( e.keyCode )
					{
						case 13 :		// ENTER
							var oTarget = e.srcElement || e.target ;
							if ( oTarget.tagName == 'TEXTAREA' ) return ;
							Ok() ;
							return false ;
						case 27 :		// ESC
							Cancel() ;
							return false ;
							break ;
					}
				}
			}
			SetOnKeyDown( document ) ;
			
			function DisableContextMenu( targetDocument )
			{
				if ( IsIE ) return ;
			
				// Disable Right-Click
				var oOnContextMenu = function( e )
				{
					var sTagName = e.target.tagName ;
					if ( ! ( ( sTagName == "INPUT" && e.target.type == "text" ) || sTagName == "TEXTAREA" ) )
						e.preventDefault() ;
				}
				targetDocument.addEventListener( 'contextmenu', oOnContextMenu, true ) ;
			}
			//DisableContextMenu( document ) ;
			
			if ( ! IsIE )
			{
				window.onresize = function()
				{
					var oFrame = document.getElementById("frmMain") ;
			
					if ( ! oFrame )
					return ;
			
					oFrame.height = 0 ;
			
					var oCell = document.getElementById("FrameCell") ;
					var iHeight = oCell.offsetHeight ;
			
					oFrame.height = iHeight - 2 ;
				}
			}
			
			if ( IsIE )
			{
				function Window_OnBeforeUnload()
				{
					for ( var t in oTabs )
						oTabs[t] = null ;
			
					window.dialogArguments.Editor = null ;
				}
				window.attachEvent( "onbeforeunload", Window_OnBeforeUnload ) ;
			}
			
			function Window_OnClose()
			{
				
			}
			
			if ( window.addEventListener )
				window.addEventListener( 'unload', Window_OnClose, false ) ;
			
			function m_click_help()
			{
			   var helpLink = "";
			   
			   try
				{
				   var url = document.getElementById("frmMain").contentWindow.location.pathname;
					var start = url.indexOf("/components/");
					if (start != -1)
					{
						start += "/components".length;
						var end = url.indexOf("/", start+1)
						if (end != -1)
						{
							var cmpName = url.substring(start+1, end);
							//window.alert(cmpName);
							helpLink = "components/"+cmpName+".jsp&book=components";
						}
						
					}
				}
				catch(e)
				{
				}	
			
				if (""==helpLink)
				{
					try
					{
						helpLink = document.getElementById("frmMain").contentWindow.helpLink;
					}
					catch (e) {}
				}
			
				if (""==helpLink)
				{
					helpLink = "editor/editor_intro.jsp&book=editor";
				}
				
				showHelpWindow(helpLink);
			}
			
			function showHelpWindow(helpLink)
			{
				var url = "/admin/help/index.jsp";
				if (helpLink != "")
				{
					url = url + "?link="+helpLink;
				}
			   var options = "menubar=no,toolbar=no,scrollbars=yes,resizable=yes,width=880,height=540;"
			   popWindow=window.open(url,"_blank",options);
			}						
			
			function setTopTitle(text, iconUrl)
			{
				var el = document.getElementById("topTitleTr");
				try { el.style.display = "table-row"; } catch (e) { el.style.display = "block"; }
				document.getElementById("topTitleTdText").innerHTML = text;				
				if ( document.all )
					window.dialogHeight = ( parseInt( window.dialogHeight, 10 ) + 36 ) + 'px' ;
				else
					window.resizeBy( 0, 36 ) ;
			}
			
			var FCK =
			{
			   InsertHtml : function(html)
			   {
				   var oldIncludeText = dialogArguments.CustomValue+")!";
				   if (oldIncludeText != html)
					{
					   //window.alert("DA cv changed=\n"+dialogArguments.CustomValue+" new=\n"+html);
					   //sprav ajax dotaz na zmenu daneho textu
					   var form = document.inlineComponentEditForm;
					   form.docid.value = dialogArguments.DocId;
					   form.oldText.value = dialogArguments.CustomValue;
					   form.newText.value = html;
					   
					   $.ajax({
							  type: 'POST',
							  url: "/admin/inline/component_save_ajax.jsp?&rnd=" + (new Date()).getTime(),
							  data: $(form).serialize(),
							  dataType: 'html',
							  cache: false,
							  timeout: 300000,
							  success: function(html)
							  {								  								  
								  if (html.indexOf("SAVEOK")!=-1)
								  {
									  Cancel();
								  }
								  else
								  {
								  	  window.alert(html);
								  	  reloadParentWindow();
								  }
								  //$("#statusIframe").html(html);
								  //Cancel();
							  },
								error:function(xmlHttpRequest, textStatus, errorThrown)
								{
									try
									{
										if (xmlHttpRequest.status == 200)
										{
											alert("<iwcm:text key="editor.ajax.save.error.login"/>");											
										}
										else
										{
											alert("<iwcm:text key='editor.ajax.save.error'/>");
										}
									}
									catch (ex) {}
								}
							});					   
					}
				   else
					{
					   Cancel();
					}
			   }
			   		
			};

		</script>
		<!-- mho edit - výmena za webjet8/css/fck_dialog.css
		<link href="/admin/FCKeditor/editor/skins/webjet/fck_dialog.css" type="text/css" rel="stylesheet"/>
		-->
		<link rel="stylesheet" href="/admin/skins/webjet8/css/fck_dialog.css" />
	</head>
	<body onload="LoadInnerDialog();" class="PopupBody" onunload="windowOnUnload();">
		<table height="100%" cellspacing="0" cellpadding="0" width="100%" border="0">
			<tr id="TabsRow" style="display: none;">
				<td class="PopupTabArea">
					<ul id="Tabs" class="tab_menu">
					</ul>
				</td>
			</tr>
			<tr>
				<td id="FrameCell" height="100%" valign="top">
					<div id="statusIframe"></div>
					<iframe id="frmMain"name="frmMain" frameborder="0" height="100%" width="100%" scrolling="auto"></iframe>
				</td>
			</tr>
			<tr>
				<td class="PopupButtons">
					<form style="display: none;" name="inlineComponentEditForm">
						<input type="hidden" name="docid" value=""/>
						<input type="hidden" name="oldText" value=""/>
						<input type="hidden" name="newText" value=""/>
					</form>
				
					<table border="0" cellpadding="0" cellspacing="0">
						<tr>
							<td width="100%"><input id="btnHelp" type="button" value="<iwcm:text key="menu.top.help"/>" onClick="m_click_help();" />&nbsp;</td>
							<td nowrap="nowrap">
								<input id="btnOk" type="button" value="<iwcm:text key="button.ok"/>" class="Button" onclick="Ok();" />&nbsp; 
								<input type="button" value="<iwcm:text key="button.close"/>" class="Button" onclick="CancelClick();" <iwcm:text key="button.cancel"/> />
							</td>
						</tr>
					</table>
				</td>
			</tr>
		</table>
	</body>
</html>