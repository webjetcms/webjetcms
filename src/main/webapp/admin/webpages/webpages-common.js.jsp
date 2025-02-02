<%@page import="java.util.List"%>/*
<%@ page pageEncoding="utf-8" contentType="text/javascript" %><%@
page import="sk.iway.iwcm.Constants"%><%@
page import="sk.iway.iwcm.FileTools"%><%@
page import="sk.iway.iwcm.Identity"%><%@
page import="sk.iway.iwcm.doc.AtrDB"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
sk.iway.iwcm.PathFilter.setStaticContentHeaders("/admin/ckeditor/webpages.js", null, request, response);%><%@
page import="sk.iway.iwcm.doc.groups.GroupsController" %><%@
page import="sk.iway.iwcm.form.FormDB"%><%@
page import="sk.iway.iwcm.i18n.Prop"%><%@
page import="sk.iway.iwcm.tags.CombineTag"%><%@
page import="sk.iway.iwcm.users.SettingsAdminBean"%><%@ page import="sk.iway.iwcm.users.SettingsAdminDB"%><%@ page import="sk.iway.iwcm.users.UsersDB"%><%@ page import="java.util.ArrayList"%><%@ page import="java.util.Map"%><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true"/>
*/
<%
    Prop prop2 = Prop.getInstance(request);
    int maxSize = Constants.getInt("webpagesTreePagesMaxSize");
	Identity currUser = UsersDB.getCurrentUser(request);
	if(currUser==null) {
		return;
	}
	Map<String,SettingsAdminBean> userSettings = SettingsAdminDB.getSettings(currUser.getUserId());
%>

function createNewWebpage()
{
	editWebPage(-1, -1, lastGroupId);
}

function editWebPage(docId, historyId, groupId)
{
	window.open("/admin/v9/webpages/web-pages-list/?groupid="+groupId+"&docid="+docId);
}

function loadGroupRefreshed(refreshedGroupId)
{
	if (lastGroupId == refreshedGroupId && $("#editorMainDiv").css("display")=="none") {
		groupslistTable.loadData(refreshedGroupId);
	}
}

function jstreeAddNewGroup(obj)
{
	var postData = { parent: obj.id, text: "<iwcm:text key="editor.dir.new_dir"/>" };

  	$.ajax({
		  type: "POST",
		  url: "/admin/webpages/ajax_jstreeoperations.jsp?act=addGroup",
		  data: postData,
		  context: obj,
		  success: function(data)
		  {
		     //test
		     //console.log("RESPONSE FROM SERVER: "+data);
		     //console.log("Originalny node: ");
		     //console.log(this);
		     //console.log("data");
		     //console.log(data);

		     //this odkazuje na obj co je parent node
              var self = this;
              showErrorOrCallback(data, function(data){
                  var inst = $.jstree.reference(self.id);
                  inst.create_node(self, { id: data.newGroupId, text: data.text }, "last", function (new_node) {
                      setTimeout(function () { inst.edit(new_node); });
                  });
              });
		  }
	});
}

function showErrorOrCallback(data, okCallback, errorCallback) {

	 if (data.forceReloadTree === true) {
		 setTimeout(function() {
			 reloadWebpagesTree();
		 }, 500);
	 }

    if (data.status === false) {
        if (WJ.isNotEmpty(data.error)) {
            toastr["error"]("", data.error);
        }
        else {
            toastr["error"]("", "<iwcm:text key="dmail.subscribe.db_error"/>");
        }

        if (typeof okCallback == 'function') {
            errorCallback(data);
        }

        return;
    }

    if (typeof okCallback == 'function') {
        okCallback(data);
    }
}

function jstreeAddNewRootGroup()
{
	var postData = { parent: 0, text: "<iwcm:text key="editor.dir.new_dir"/>" };

	$("#treeScrollDiv").slimScroll({ scrollToY: $("#treeScrollDiv")[0].scrollHeight+'px' });

  	$.ajax({
		  type: "POST",
		  url: "/admin/webpages/ajax_jstreeoperations.jsp?act=addGroup",
		  data: postData,
		  context: obj,
		  success: function(data)
		  {
		     //test
		     //console.log("RESPONSE FROM SERVER: "+data);
		     //console.log("Originalny node: ");
		     //console.log(this);
		     //console.log("data");
		     //console.log(data);

		     //this odkazuje na obj co je parent node
                showErrorOrCallback(data, function(data){
                    var inst = $.jstree.reference("#groupslist-jstree");
                    inst.create_node(null, { id: data.newGroupId, text: data.text }, "last", function (new_node) {
                        setTimeout(function () { inst.edit(new_node); },0);
                    });
                });

		  }
	});
}

function jstreeAddNewDoc(obj)
{
	var postData = { parent: obj.id, text: "<iwcm:text key="editor.newDocumentName"/>" };

  	$.ajax({
		  type: "POST",
		  url: "/admin/webpages/ajax_jstreeoperations.jsp?act=addPage",
		  data: postData,
		  context: obj,
		  success: function(data)
		  {
		     //test
		     //console.log("RESPONSE FROM SERVER: "+data);
		     //console.log("Originalny node: ");
		     //console.log(this);
		     //console.log("data");
		     //console.log(data);

		     //this odkazuje na obj co je parent node
              var self = this;
              showErrorOrCallback(data, function(data){
                  var inst = $.jstree.reference(self.id);
                  inst.create_node(self, { id: data.newDocId, text: data.text, icon: "fa fa-file icon-state-success", children: false }, "last", function (new_node) {
                      setTimeout(function () { inst.edit(new_node); },0);
                  });
              });
		  }
	});
}

function clearNodeText(text) {
	var textArr = text.split("</span>");

	for (var i = 0; i < textArr.length; i++) {
		var val = $.trim(textArr[i]);
		if (val.indexOf("<"+"span") != 0) {
			if (val.indexOf("<"+"span") > -1) {
				text = val.substring(0, val.indexOf("<"+"span"));
				break;
			}
			else {
				text = val;
				break;
			}
		}
	}

	text = $.trim(text);

	return text;

}

function jstreeDeleteDocByDocId(groupId, docId)
{
	var inst = $.jstree.reference("#groupslist-jstree");
	var nodeId = groupId+"_"+docId;
	var node = false;
	if(inst != undefined){
		node = inst.get_node(nodeId);
	}

	if (false == node)
	{
		//nod neexistuje v strome, asi vypnute zobrazovanie stranok, su len adresare
		node = { id: nodeId };
	}

	jstreeDeleteDoc(node);
}

function jstreeDeleteDoc(node)
{
	if (window.confirm("<iwcm:text key="groupslist.do_you_really_want_to_delete"/>"))
	{
		var postData = { id: node.id };

	  	$.ajax({
			  type: "POST",
			  url: "/admin/webpages/ajax_jstreeoperations.jsp?act=delete",
			  data: postData,
			  context: node,
			  success: function(response)
			  {
			      var self = this;
                  showErrorOrCallback(response, function(response){
                      var inst = $.jstree.reference(self.id);
                      if (inst != null)
                      {
                          inst.delete_node(self);
                      }

                      loadGroupRefreshed(response.refreshGroupId);
                  });
			  }
		});
	}
}

function jstreeDeleteGroup(node)
{
	if (window.confirm("<iwcm:text key="groupedit.dirDeleteConfirm"/>"))
	{
		var postData = { id: node.id };

	  	$.ajax({
			  type: "POST",
			  url: "/admin/webpages/ajax_jstreeoperations.jsp?act=delete",
			  data: postData,
			  context: node,
			  success: function(response)
			  {
			     //test
			     /*
			     console.log("RESPONSE FROM SERVER: "+data);
			     console.log("Originalne data: ");
			     console.log(this);
			     console.log("response from server");
			     console.log(response);
			     */
			      var self = this;
                  showErrorOrCallback(response, function(response){
                      var inst = $.jstree.reference(self.id);
                      inst.delete_node(self);
                  });
			  }
		});
	}
}

function getJsTreeNodeLabel(type, node)
{
	var data = {};
	if (type == "page") {
		data.contextDocId = ['<iwcm:text key="webpages.doc_id" />', node.original.docId, 'col-xs-3'];
		data.contextSortPriority = ['<iwcm:text key="webpages.sort_priority" />', node.original.sortPriority, 'col-xs-4'];
		data.contextEditDate = ['<iwcm:text key="webpages.last_update" />', node.original.dateCreated, 'col-xs-5'];
	}
	else {
		data.contextGroupId = ['<iwcm:text key="webpages.group_id" />', node.original.groupId, 'col-xs-5'];
		data.contextSortPriority = ['<iwcm:text key="webpages.sort_priority" />', node.original.sortPriority, 'col-xs-7'];
	}

	var html = '<div class="container-fluid contextItemInfo"><div class="row">';

	$.each(data, function(k,v){
		if (typeof v[1] != 'undefined') {
			html += '<div class="' + v[2] + '">';
			html += '<label id="'+ k +'">' + v[0] + '</label>';
			html += '<div id="'+ k +'">' + v[1] + '</div>';
			html += '</div>';
		}
	});

	html += '</div></div>';

	return html;
}

function initJsTree()
{
	$('#groupslist-jstree')
	.on('select_node.jstree', function (e, data)
	{
    	//console.log("select_node 1: "+data.node.id);
    	//console.log(data);
		//toto je ked sa vola refresh z funkcie reloadWebpagesTree
		if (data.event == undefined) return;
		//ked sme klikli z kontextmenu nesprav refresh
    	if (data.event && data.event.type == "contextmenu") return;

    	if (data.node.id.indexOf("_")!=-1)
    	{
    		var docId = data.node.id.substring(data.node.id.indexOf("_")+1);
    		//console.log("Loading doc: "+docId+" display="+$("#editorMainDiv").css("display"));

    		// klik na ikonu - web stranka
	    	if (typeof data.event != 'undefined' && $(data.event.target).hasClass('jstree-icon')) {
	    		window.open('/showdoc.do?docid=' + docId);
	    	}
	    	// klik na text
	    	else {
    			editWebPage(docId, -1);
    		}

    	}
    	else
    	{
    		if (checkDirtyEditor()==false) return;
			resetDirtyEditor();

    		//klik na ikonu adresar
	    	if (typeof data.event != 'undefined' && $(data.event.target).hasClass('jstree-icon')) {
				 //over ci je zobrazeny button Editovat adresar a len vtedy otvor editor (kontrola prav)
				if ($("#btnEditDir:hidden").length==0) {
					popup('<%=GroupsController.BASE_URL%>'+data.node.id+'/?Edit=Edituj', 650, 500);
				}
	    		return;
	    	}

    		groupslistTable.loadData(data.node.id);
    		//po kliknuti rovno nod aj otvor (lepsie na pouzivanie)
    		$('#groupslist-jstree').jstree("open_node", $("#"+data.node.id));

    		if ($("#groupslistTableDiv").css("display")=="none")
    		{
    			$("#editorMainDiv").hide();
				$("#groupslistTableDiv").show();
				$(".title.webstrankyTitle .form-group").show();
    		}
    	}
   })
   .on('after_open.jstree', function (e, data) {
   		changeNodeText(data.instance, data.node);
		<% if (maxSize > -1) { %>
    		setTimeout(function(){
    			hidePagesMaxSize(<%= maxSize %>, data);
    		}, 1);
		<% } %>
        fixSlimScrollScrollers();
   })
   .on('keydown.jstree', function (e)
   {
	   // e.which
	   if(e.target.tagName === "INPUT") { return true; }
		var o = null;

		//console.log(e);

		switch(e.which) {
			case 113:
				e.preventDefault();
				//console.log("F2");
				var inst = $.jstree.reference("#groupslist-jstree");
				var hoveredID = $($('#groupslist-jstree .jstree-hovered')[0]).parent().attr("id");
				var o = inst.get_node(hoveredID);
				//console.log(o);
				if(o!=null && o.id && o.id !== '#')
				{
					//obj = inst.get_node(selected);
					//ziskaj nod ktory je oznaceny focusom
					o.text = clearNodeText(o.text);
					inst.edit(o);
				}
				break;
			case 78:
				//CTRL+n / N
				if (e.ctrlKey || e.altKey)
				{
					e.preventDefault();
					var inst = $.jstree.reference("#groupslist-jstree");
					var selected = inst.get_selected();
					//console.log(selected);
					var o = null;
					if (selected != null && selected.length > 0) o = document.getElementById(selected[0]);
					//console.log(o);
					var hoveredID = $($('#groupslist-jstree .jstree-hovered')[0]).parent().attr("id");
					if (hoveredID != undefined)
					{
						if (hoveredID.indexOf("_")!=-1) hoveredID = hoveredID.substring(0, hoveredID.indexOf("_"));
						//console.log(hoveredID);
						o = inst.get_node(hoveredID);
					}
					if(o!=null && o.id && o.id !== '#')
					{
						if (e.shiftKey) jstreeAddNewDoc(o);
						else jstreeAddNewGroup(o);
					}
				}

				break;
			case 46:
				//CTRL+DEL
				if (e.ctrlKey || e.altKey)
				{
					e.preventDefault();
					var inst = $.jstree.reference("#groupslist-jstree");
					var selected = inst.get_selected();
					var o = null;
					if (selected != null && selected.length > 0) o = document.getElementById(selected[0]);
					if(o!=null && o.id && o.id !== '#')
					{
						//zmaz nod ktory je oznaceny focusom
						var hoveredID = $($('#groupslist-jstree .jstree-hovered')[0]).parent().attr("id");
						//console.log(hoveredID);
						{
						   var node = inst.get_node(hoveredID);
							if (hoveredID.indexOf("_")!=-1) jstreeDeleteDoc(node);
							else jstreeDeleteGroup(node);
							setTimeout(function()
						   {
				            $("#"+node.parent).attr("aria-selected", true);
				            $("#"+node.parent+" a").focus();
						   }, 500);
						}
					}
				}

				break;
			default:
				//console.log("jstree.keydown"+e.which);
				//console.log(e);
				break;
		}
	})
   .on('rename_node.jstree', function (e, data)
   {
		//console.log("Rename node, data:");
		//console.log(data);

		var postData = { id: data.node.id, text: data.node.text };

		$.ajax({
			  type: "POST",
			  url: "/admin/webpages/ajax_jstreeoperations.jsp?act=rename",
			  data: postData,
			  context: data,
			  success: function(response)
			  {
                  showErrorOrCallback(response, function(response){
							try {
								//uspesne premenovane
								data.node.text = response.text;
								//console.log("data.node:", data.node);
								//data.instance.refresh_node(data.node);
								var parentNode = data.instance.get_node(data.node.parent);
								//console.log("parentNode=", parentNode, " parentNode.parent: ", parentNode.parent);
								if (parentNode.parent != null && parentNode.parent != "#")
								{
									parentNode = data.instance.get_node(parentNode.parent);
								}

								var selectedPath = data.node.id;

								//ak je to rename web stranky reloadni a selectni parent node
								if (data.node.id.indexOf("_")!=-1)
								{
									selectedPath = data.node.parent;
								}

								//console.log(parentNode);
								data.instance.refresh_node(parentNode);
								data.instance.redraw(true);
							} catch (e) {
								console.log(e);
								reloadWebpagesTree();
							}

							if (response.refreshGroupId)
							{
								if (data.node.parent=="#") reloadWebpagesTree();
								else loadGroupRefreshed(response.refreshGroupId);
							}

                      setTimeout(function()
                      {
                          //console.log("timeout: "+selectedPath);
                          //console.log(data.node);
                          //sprav focus
                          $("#"+selectedPath).attr("aria-selected", true);
                          $("#"+selectedPath+">a").focus();
                      }, 500);
                  });
			  }
		});
   })
   .on('move_node.jstree', function (e, data)
   {
		//console.log("Move node, data:");
		//console.log(data);

		var postData = { id: data.node.id, parent: data.parent, oldparent: data.old_parent, position: data.position, oldposition: data.old_position };

		$.ajax({
			  type: "POST",
			  url: "/admin/webpages/ajax_jstreeoperations.jsp?act=move",
			  data: postData,
			  context: data,
			  success: function(response)
			  {
			     /*
			     console.log("RESPONSE FROM SERVER: "+data);
			     console.log("Originalne data: ");
			     console.log(this);
			     console.log("response from server");
			     console.log(response);
			     */

                  showErrorOrCallback(response, function(response){
                      //uspesne premenovane
                      data.instance.refresh_node(data.instance.get_node(data.node.parent));
                      data.instance.redraw(true);

                      //sprav focus
                      $("#"+data.node.id).attr("aria-selected", true);
                      $("#"+data.node.id+" a").focus();
                  }, function(){
                      data.instance.refresh(false);
                  });
			  }
		});
   })
   .on('redraw.jstree', function(nodes, data)
   {
        var instance = $('#groupslist-jstree').jstree(true);

        $.each(data.nodes, function(i, v){
            var node = instance.get_node(v);
            changeNodeText(instance, node);
        });
   })
   .on('loaded.jstree', function(event, data)
	{
		$('#groupslist-jstree .jstree-container-ul > li').each(function(){
			var id = $(this).prop('id');
			var instance = data.instance;
			var node = instance.get_node(id);

			changeNodeText(instance, node);
		});
		jsTreeInitialized = true;
	})
	.jstree(
	{
		"plugins" : [ "contextmenu", "dnd" ],
		'sort' :  function (a, b) {
			//console.log(a);
			//console.log(b);
		    return this.get_node(a).original.px > this.get_node(b).original.px ? 1 : -1;
		},

		contextmenu: {items: jsTreeContextMenu},

		'core' : {
		  'data' : {
		    'url' : function (node) {
		      return node.id === '#' ?
		        'ajax_roots.jsp' :
		        'ajax_children.jsp';
		     },
		     'data' : function (node) {
		     	//console.log(node);
		      	return { 'id' : node.id };
		     }
		  },
		  'check_callback' : function (operation, node, node_parent, node_position, more) {
		 		// operation can be 'create_node', 'rename_node', 'delete_node', 'move_node' or 'copy_node'
		 		// in case of 'rename_node' node_position is filled with the new node name
		 		if (operation == 'create_node' || operation == 'rename_node' || operation == 'delete_node') return true;

                if (operation == 'move_node')
		 		{
                    //if (more.ref) console.log(more.ref.id+" "+more.pos+" "+node_position);
                    //more.ref.id
                    /*
                    console.log("---");
                    console.log(node);
                    console.log(more);
                    */
                    if (more && more.ref && more.ref.id)
                    {
                        if (more.ref.id.indexOf("_")!=-1)
                        {
                            //web stranka
                            //console.log(node);
                            if (node && node.id && node.id.indexOf("_")!=-1 && ( more.pos=='b' || more.pos=='a'))
                            {
                                //console.log("TRUE 1");
                                return true;
                            }
                            //console.log("FALSE");
                            return false;
                        }
                        else
                        {
                        //adresar
                            if (node && node.id && node.id.indexOf("_")==-1 && ( more.pos=='b' || more.pos=='a'))
                            {
                                //console.log("TRUE 2");
                                return true;
                            }
                        }
                    }

                    if (node_position == 0 && more.ref) return true;

                    //finalne zavolanie / potvrdenie
                    if (!more.ref && window.confirm("<%= prop2.getText("grouptree.drag_and_drop.do_you_want_to_move") %>")) {
                        //console.log("TRUE 3");
                        return true;
                    }
                }

		 		//console.log("RET FALSE 2, operation="+operation+" node_position="+node_position);
		 		//console.log(more)
		 		return false;
		  }
		}
	}
	);
}

//TODO: toto nam na nieco treba?
function showDoc(event, ui){
		window.location.href = "/admin/editor.do?docid="+ui.item.doc_id
}

$(document).ready(function()
{
	$('#docIdInputBox').keypress(function(e) {
	    if (e.which == 13)
	    {
	        editWebPage($.trim($('#docIdInputBox').val()));
	    }
	});

	$('#groupIdInputBox').keypress(function(e) {
	    if (e.which == 13) {
	    	loadGroup($.trim($('#groupIdInputBox').val()));
	    }
	});

    $('input[name="title"]').on("input",function() {
        var maxLength = $(this).data("length");
        if (maxLength <= $(this).val().length) {
            toastr.remove();
            toastr["warning"]("<iwcm:text key="editor.title.maxLength"/>");
        } else {
            toastr.remove();
		}
    });

	window.setInterval(function()
	{
		//Layout.fixContentHeight();
		fixSlimScrollScrollers();
	}, 5500);
});

function loadGroup(id)
{
	groupslistTable.loadData(id);
}

function popupLinkAddGroupId(url, width, height)
{
    if(url.indexOf("admin/groups/") == -1) {
        if (url.indexOf("?")==-1) url = url + "?groupId="+lastGroupId;
        else if (url.indexOf("Edit=Edituj")!=-1) url = url.split("?")[0] + lastGroupId;
        else url = url + "&myParentGroupId="+lastGroupId;
	} else {
    	if (url.indexOf("Add=Pridaj")!=-1) {
            url = url + "&myParentGroupId="+lastGroupId;
		}
		var trimmedUrl = url.replace(/^\/|\/$/g, '');

        if (trimmedUrl == "admin/groups/edit") {
            url = "/" + trimmedUrl + "/" + lastGroupId;
        }
	}

	popup(url, width, height);
}

//===================== WEBJET_FUNCTIONS.JS.JSP ===================
	var perexEditing = false;


	function docTitleFocus()
	{
		if (document.editorForm.title.value == "<iwcm:text key="editor.newDocumentName"/>")
		{
			document.editorForm.title.value = "";
		}
	}

	function docTitleBlur()
	{
		if (document.editorForm.navbar.value == "")
		{

		}
		document.editorForm.navbar.value = document.editorForm.title.value;
	}

	function showProps()
	{
		elProps = document.getElementById("propLayer");
		difSize = 205;
		if (elProps.style.display=="block")
		{
			elProps.style.display="none"
			//document.all['tbContentElementBoxiwcm'].style.pixelHeight=document.all['tbContentElementBoxiwcm'].style.pixelHeight + difSize;
		}
		else
		{
			elProps.style.display="block";
			//document.all['tbContentElementBoxiwcm'].style.pixelHeight=document.all['tbContentElementBoxiwcm'].style.pixelHeight - difSize;
		}
	}

	function checkDateTimePublish()
	{
		if (document.editorForm.publishStart.value=="")
		{
			window.alert("<iwcm:text key="editor.publish.date_check"/>");
			document.editorForm.publicable.checked=false;
			return(false);
		}
		if (document.editorForm.publishStartTime.value=="")
		{
			window.alert("<iwcm:text key="editor.publish.time_check"/>");
			document.editorForm.publicable.checked=false;
			return(false);
		}
		return(true);
	}

	function checkDateTimePublishEnd()
	{
		if (document.editorForm.publishEnd.value=="")
		{
			window.alert("<iwcm:text key="editor.publish.date_check"/>");
			document.editorForm.disableAfterEnd.checked=false;
			return(false);
		}
		if (document.editorForm.publishEndTime.value=="")
		{
			window.alert("<iwcm:text key="editor.publish.time_check"/>");
			document.editorForm.disableAfterEnd.checked=false;
			return(false);
		}
		return(true);
	}


function openComponent(name)
{
   var leftPos = 400;
   var topPos = 250;
   if (screen && screen.availWidth)
   {
		leftPos = (screen.availWidth-400) / 2;
		topPos = (screen.availHeight-200) / 2;
	}


	//window.open("/components/"+name+"/editor_component.jsp",'','width=400,height=200,toolbar=no,scrollbars=no,resizable=yes,titlebar=0,top=' + topPos + ',left=' + leftPos);
}

//========== nove funkcie pre FCK =================

function setEditorMode(isHtml)
{
	var editor = CKEDITOR.instances.data;
	if (isHtml) editor.setMode('source');
	else editor.setMode('wysiwyg');
}

//============ WebJET 6 funkcie prenesene ==========

/**
 * Funkcia cez ajax GET request zavola jsp subor, ktory na zaklade parametrov v requeste a v session vygeneruje div panel s upozorneniami
 *
 */
function reloadValidationDiv()
{
	$.get('/admin/FCKeditor/editor_validationDiv.jsp?time=' + <%=System.currentTimeMillis()%>, function(data)
	{
		$("#errorWarningDiv").html(data);
		if ($("#errorWarningDiv").html().toLowerCase().indexOf("<"+"div") != -1)
		{
			//zobrazi zatvaraciu ikonku, ked sa zobrazi informacny panel
			$("#closeLinkValidationDivId").show();
		}
	});
}

/**
 *	Reloadne validationDiv, v ktorom su zobrazovane jednotlive chyby alebo upozornenia
 *	Po skonceni realodnutia nacita formular EditorForm.
 */
function ajaxReloadEditor(jsonEditorForm)
{
	//kvoli chrome musi ist asynchronne, inak sa po prepnuti do html editor zrutil
	//ajaxFillEditorForm(jsonEditorForm);
	setTimeout(function() { ajaxFillEditorForm(jsonEditorForm); }, 500);

	//reloadne div s upozorneniami a chybami po ulozeni dokumentu
	  $.post('/admin/FCKeditor/editor_validationDiv.jsp?time=' + <%=System.currentTimeMillis()%>, function(data) {
		  $("#errorWarningDiv").fadeOut('fast', function() {
			  $("#errorWarningDiv").html(data).fadeIn('fast', function()
			  {
			  	  //console.log("Warning div content 3: "+ data);
			  	  //console.log("index:"+$("#errorWarningDiv").html().toLowerCase().indexOf('<'+'div'));
				  if ($("#errorWarningDiv").html().toLowerCase().indexOf('<'+'div') != -1)
						$("#closeLinkValidationDivId").show();//zobrazi zatvaraciu ikonku, ked sa zobrazi informacny panel
			  });
		  });
		});

	 // vrat informaciu o tom, ze sa nalodovala nova stranka do editora
	 return true;
}

/**
 * Pomocou Javascriptu vyplni cely formular editora bez potreby je reloadnutia cez ajax request
 *
 * @param jsonEditorForm cely java object editorForm ziskany zo servera serializovany v notacii json
 */
function ajaxFillEditorForm(jsonEditorForm)
{
	//console.log("Perex group 1: "+jsonEditorForm.perexGroup);

	//presunute do editu dtStart("ajaxFillEditorForm");
	dtDiff("ajaxFillEditorForm start");

	//alert("group: " + $('[name="atrGroupSelect"] option:selected').text());
	//ak vypadne spojenie zo serverom
	if (jsonEditorForm == null)
	{
		alert("<iwcm:text key="editor.ajax.load.error.server"/>");
		$("#errorWarningDiv").html("<div class=\"topNotify\"><div class=\"notifyText\"><iwcm:text key='editor.ajax.load.error.server'/></div></div>");
		return;
	}

	var oEditor = CKEDITOR.instances.data;
	var oForm = document.editorForm;

	//ak sa zmeni css styl FCK Editora, tak ho musime jednoducho cely reloadnut
	//	kvoli tomu, ze si nacitava konfiguraciu pri prvom nacitani
	//pretypovanie na string
	var editorCss = ""+oEditor.config.contentsCss;
	var pluginsCss = ","+"/admin/skins/webjet8/ckeditor/plugins/";
	var pluginsCssIndex = editorCss.indexOf(pluginsCss);
	//window.alert(pluginsCss+" i="+pluginsCssIndex);
	if (pluginsCssIndex > 0) editorCss = editorCss.substring(0, pluginsCssIndex);
	var jsonCss = jsonEditorForm.cssStyle;

	try
	{
		//window.alert("Editor CSS:"+editorCss+" jsonCSS:"+ jsonCss);
		//nastav CSS pre editor

		var appendStyles = "";
		var i = 0;
		for (i=0; i < ckEditorInstance.config.contentsCss.length; i++)
		{
			var style = ckEditorInstance.config.contentsCss[i];
			if (style.indexOf("/admin/")!=-1)
			{
				appendStyles += ","+style;
			}
		}

		//console.log(appendStyles);

		var newStyles = (jsonCss + appendStyles).split(",");
		dtDiff("3. Content CSS:"+ckEditorInstance.config.contentsCss+"; newStyles:"+newStyles+";");
		//console.log("Editor contentsCss=", ckEditorInstance.config.contentsCss, " new=", newStyles);
		//console.log(newStyles);
		if ((""+ckEditorInstance.config.contentsCss) != (""+newStyles))
		{
			dtDiff("CHANGING STYLES to:"+newStyles);
			ckEditorInstance.config.contentsCss = newStyles;
		}
	}
	catch (e) { console.log(e); }

	dtDiff("newStyles");

	//nastavi editoru ziskany html kod a resetne stav IsDirty
	//console.log("Setting data");
	//console.log(oEditor);
	//console.log(jsonEditorForm.data);
	oEditor.setData(jsonEditorForm.data);
	setTimeout(function() { try { ckEditorInstance.resetUndo(); } catch (e) {} }, 500);
	//console.log("data set, current: "+oEditor.getData());
	needToSave = false;

	// kazdu premennu v objekte zapise do rovnomenneho pola vo formulari editorForm
	for(var member in jsonEditorForm)
	{
		if (oForm.elements[member]==null) continue;

		// ak je to checkbox
		if ($(oForm.elements[member]).attr('type') == "checkbox")
		{
			if(member == "passwordProtected")	//multicheckbox
			{
				$("input[name='passwordProtected']:checked").each(function() { // odcheckni vsetky, ktore boli checknute
				   $(this).removeAttr("checked");});

				if (jsonEditorForm.passwordProtected != null && jsonEditorForm.passwordProtected.length > 0)	// checkni vsetky skupiny, ktore maju byt
					for (i = 0; i < jsonEditorForm.passwordProtected.length; i++)
						$("input[name='passwordProtected'][value='" + jsonEditorForm.passwordProtected[i] + "']").prop("checked", "checked");
			}
			else
			{
				$(oForm.elements[member]).prop('checked', jsonEditorForm[member]);

				//console.log("member="+member+" "+jsonEditorForm[member]+" set: "+$(oForm.elements[member])+" attr:"+$(oForm.elements[member]).prop('checked'));

				if (member=="available")
				{
					//console.log("html.ajaxSaveFormAvailable 1="+jsonEditorForm[member]);
		  			$(editorForm.available).bootstrapSwitch('state', jsonEditorForm[member]);
				}
			}
		}
		else
		{
			if(member == "passwordProtected")
			{
				try
				{
					var ppSelect = oForm.passwordProtected;
					for (i=0; i < ppSelect.options.length; i++)
					{
						ppSelect.options[i].selected = false;
						ppSelect.options[i].defaultSelected = false;
					}
					if (jsonEditorForm.passwordProtected != null && jsonEditorForm.passwordProtected.length > 0)	// checkni vsetky skupiny, ktore maju byt
					{
						for (i = 0; i < jsonEditorForm.passwordProtected.length; i++)
						{
							for (j=0; j < ppSelect.options.length; j++)
							{
								if (ppSelect.options[j].value == jsonEditorForm.passwordProtected[i])
								{
									ppSelect.options[j].selected = true;
									ppSelect.options[j].defaultSelected = true;
								}
							}
						}
					}
				} catch (e) {}
			}
			else
			{
				//$(oForm[name=member]).val(jsonEditorForm[member]);
				//jeeff: to hore robilo problem v IE
				oForm.elements[member].value=jsonEditorForm[member];
			}
		}
	};

	dtDiff("after member");

    try {
        $("#pageTitle").text(jsonEditorForm.title);
    } catch (e) { console.log(e); }

	if (jsonEditorForm.templates.length > 0)
	{
		var tempSelect = oForm.tempId;
		//vymaz aktualne sablony
		var failsafe = 0;
		while (tempSelect.length>0 && failsafe++<100)
		{
			tempSelect.options[0] = null;
		}

		if (tempSelect.options)
		{
			for (i=0; i < jsonEditorForm.templates.length; i++)
			{
			    var template = jsonEditorForm.templates[i];
				var tempId = template.id;
				var selected = false;
				//window.alert("porovnavam: "+tempId+" vs "+jsonEditorForm.tempId);
				if (tempId == jsonEditorForm.tempId)
				{
					//window.alert("mam: tempId="+tempId);
					selected = true;
				}
				var option = new Option(template.name, tempId, selected, selected);
				tempSelect.options[tempSelect.length] = option;
			}
		}
	}

	//console.log(jsonEditorForm);

	dtDiff("after temps");

	//ak sa nepodari nastavit ID sablony v zozname sprav plny reload (asi su sablony popriradzovane per adresar)
	if (oForm.tempId.value != jsonEditorForm.tempId)
	{
		//window.alert("NEMAM SABLONU, "+oForm.tempId.value+"; "+jsonEditorForm.tempId);
		//location.href="/admin/webpages/?docid=" + jsonEditorForm.docId;
		//return;
	}

	oForm.virtualPathOriginal.value = oForm.virtualPath.value;

	//nacitanie Slave adresarov
	$("#otherGroupsDiv").load("/admin/FCKeditor/ajaxAddOtherGroup.jsp?reload=1&docId="+jsonEditorForm.docId, function(){
		otherGroups = [];
		$('input[name="otherGroups"]', oForm).each(function(i, v){
			var id = $(this).val();
			otherGroups.push(id);
		});
	});

	dtDiff("after slaves");

	//vymaze tabulku s historiou stranky
	$("#pageHistoryResults").text("");

	//console.log("Perex group 2: "+jsonEditorForm.perexGroup);
    //nacitanie perex skupin pre docId
	<% if (Constants.getBoolean("perexGroupsFastLoad")==false) { %>
		//ak je definovanych velmi vela perex skupin ale nepouziva sa zaradenie podla adresara nemusime toto loadovat a nastavovat
		$.post("/admin/FCKeditor/perex_groups_load.jsp?time="+<%=System.currentTimeMillis()%>+"&docId="+jsonEditorForm.docId+"&groupId="+jsonEditorForm.groupId, function(data)
		{
			dtDiff("perex_groups_load, loaded");

			$("select[name='perexGroup']").html(data);

			dtDiff("perex_groups_load, html set");

			//multiselect pre zalozku publikovanie a fieldset zobrazit na -- tzv.	perex skupiny
			if (jsonEditorForm.perexGroupString == null)
                jsonEditorForm.perexGroupString = "";
			if (jsonEditorForm.perexGroupString.length > 0)
			{
				var perexGroupsRightArray = jsonEditorForm.perexGroupString.split(",");
				if(perexGroupsRightArray != null && perexGroupsRightArray.length > 0)
				{
					for (i = 0; i < perexGroupsRightArray.length; i++)
					{
						if(perexGroupsRightArray[i] != '')
							$("select[name='perexGroup'] option[value='" + perexGroupsRightArray[i] + "']").prop("selected", "selected");
					}
				}
			}
			else
			{
				$("select[name='perexGroup'] option:selected").each(function(){
		            $(this).removeAttr("selected"); });
			}

			dtDiff("perex_groups_load, after set select");

			if (oForm.perexGroupSearch) oForm.perexGroupSearch.value = '';
			try
			{
				initializeDisabledItems(oForm, 'perexGroup', 'disabledItems');
			} catch (e) {}

			dtDiff("perex_groups_load, finished");
		});
	<% } else { %>
		//console.log("Perex group 3: "+jsonEditorForm.perexGroup);

		//multiselect pre zalozku publikovanie a fieldset zobrazit na -- tzv.	perex skupiny
		if (jsonEditorForm.perexGroupString == null)
			jsonEditorForm.perexGroupString = "";

		//console.log("jsonEditorForm.perexGroup="+jsonEditorForm.perexGroup);
		if (jsonEditorForm.perexGroupString.length > 0)
		{
			//console.log("actually selected: "+document.editorForm.perexGroup.selectedIndex);
			/*
			$("select[name='perexGroup'] option:selected").each(function(){
		            console.log($(this));
		    });
		   */

		   //sprav najskor clear
		   //console.log("clear");
		   if (document.editorForm.perexGroup.selectedIndex != -1)
			{
				$("select[name='perexGroup'] option:selected").each(function(){
		            $(this).removeAttr("selected"); });
	      }
	      //console.log("clear done");

			var perexGroupsRightArray = jsonEditorForm.perexGroupString.split(",");
			if(perexGroupsRightArray != null && perexGroupsRightArray.length > 0)
			{
				for (i = 0; i < perexGroupsRightArray.length; i++)
				{
					if(perexGroupsRightArray[i] != '')
					{
						$("select[name='perexGroup'] option[value='" + perexGroupsRightArray[i] + "']").prop("selected", "selected");
					}
				}
			}
		}
		else
		{
			dtDiff("before clear");

			dtDiff(document.editorForm.perexGroup.selectedIndex);
			if (document.editorForm.perexGroup.selectedIndex != -1)
			{
				$("select[name='perexGroup'] option:selected").each(function(){
		            $(this).removeAttr("selected"); });
	      }
	      dtDiff("after clear");
		}

		dtDiff("perex_groups_load, after set select");

		if (oForm.perexGroupSearch) oForm.perexGroupSearch.value = '';
		try
		{
			initializeDisabledItems(oForm, 'perexGroup', 'disabledItems');
		} catch (e) {}

		dtDiff("perex_groups_load, finished");
	<% } %>

	dtDiff("after perex");

	<% if (AtrDB.getAtrGroups(request).size()>0) { %>
		dtDiff("loading atrs");
		setTimeout(function() {
			/*
			$.ajax({
			   type: "POST",
			   dataType: "script",
			   url: "/admin/FCKeditor/attributes_load_ajax.jsp?docId="+jsonEditorForm.docId+"&group="+$('[name="atrGroupSelect"] option:selected').text()
			});
			*/
			dtDiff("attrs loaded");
		}, 500);
	<% } %>

	dtDiff("after attrs");

	<%
		if (Constants.getBoolean("passwordProtectedRenderMulti")) //multiselect pre zalozku skupiny
		{
	%>
			if (jsonEditorForm.passwordProtected != null && jsonEditorForm.passwordProtected.length > 0)
				for (i = 0; i < jsonEditorForm.passwordProtected.length; i++)
					$("select[name='passwordProtected'] option[value='" + jsonEditorForm.passwordProtected[i] + "']").prop("selected", "selected");

			try
			{
				initializeDisabledItems(oForm, 'passwordProtected', 'passwordProtected');
			} catch (e) {}
	<%
		}
	%>

	dtDiff("after render multi");

	//reloadne media zalozku
	setTimeout(function() { $('#mediaIFrameId').attr("src", "/components/media/admin_media_list.jsp?docid=" + jsonEditorForm.docId); }, 1000);

    if (typeof fillMedia == "function") {
        fillMedia(jsonEditorForm.docId, jsonEditorForm.media);
    }

	dtDiff("after media");

	setStylesDef(jsonEditorForm.sessionCssParsed, ckEditorInstance);

	dtDiff("after css");

	try
	{
		$('#showInMenuId').prop("checked", jsonEditorForm.showInMenu).uniform().trigger('change');

		dtDiff("after uniform");

		setTemplatePages(jsonEditorForm);

		dtDiff("after templates");

		dtDiff("after setPerexGroups");

		if (jsonEditorForm.virtualPath.indexOf("<%=Constants.getString("ABTestingName")%>")==-1) $("#abtestingOption").show();
		else $("#abtestingOption").hide();

	} catch (e) { console.log(e); }

	fillFields(jsonEditorForm);

	dtDiff("after abtest");

	setDocAtr(jsonEditorForm);

	dtDiff("after setDocAtr");

	$.uniform.update();

	try
	{
		$('#docIdInputBox').val(jsonEditorForm.docId);

	} catch (e){}

	loadNoteText();

	//nastav domenu
	if (typeof jsonEditorForm.domainName != "undefined") {
		$("#actualDomainNameSelectId").val(jsonEditorForm.domainName);
	}

	dtDiff("done");

	resetDirtyEditor();
	setTimeout(function() {resetDirtyEditor();}, 500);
	setTimeout(function() {resetDirtyEditor();}, 2000);

	setTimeout(function() { try { ckEditorInstance.focus(); } catch (e) {} }, 300);
	setTimeout(function() { try { if(typeof afterFormFill === "function") { afterFormFill(); } } catch (e) {} }, 300);
}

function loadNoteText()
{
	var noteText = document.getElementById("noteId").value;
	if(!noteText || 0 === noteText.length)
	{
		document.getElementById('noteTextId').style.display = 'none';
	}
	else
	{
		document.getElementById('noteTextId').style.display = 'block';
		document.getElementById('noteTextId').innerHTML = '<i class="fa fa-bell-o"></i> <span><iwcm:text key="editor.tab.note"/>:</span> ' + noteText;
	}
}

function setDocAtr(jsonEditorForm)
{
	try
	{
        var docAtrs = jsonEditorForm.docAtrs;

        $.each(docAtrs, function(i, v){
			//console.log("TESTING: ");
			//console.log(v);

			if (v.type == <%=AtrDB.TYPE_BOOL%>)
			{
                $("input[name=atr_"+v.atrId+"][value="+v.value+"]").prop("checked", true);
			}
			else
			{
                $("input[name=atr_"+v.atrId+"],select[name=atr_"+v.atrId+"],textarea[name=atr_"+v.atrId+"]").val(v.value);
			}
        });
	}
	catch (e) { console.log(e); }
}

function setStylesDef(sessionCssParsed, ckEditorInstance)
{
	//nastav CSS styly
	try
	{
		// elementy podla dokumentacia ckeditora: 'address', 'div', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'p', 'pre', 'a', 'embed', 'hr', 'img', 'li', 'object', 'ol', 'table', 'td', 'tr', 'ul'
		var allElements = ['a', 'b', 'i', 'u', 'blockquote', 'dd', 'div', 'dl', 'dt', 'hr', 'img', 'input', 'select', 'textarea', 'label', 'ul', 'ol', 'li', 'table', 'th', 'td', 'p', 'span', 'strong', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6'];
		var cssData = [
			{name: '<iwcm:text key="editor.paragraph"/>', element: 'p' },
			{name: '<iwcm:text key="editor.h1"/>', element: 'h1'},
			{name: '<iwcm:text key="editor.h2"/>', element: 'h2'},
			{name: '<iwcm:text key="editor.h3"/>', element: 'h3'},
			{name: '<iwcm:text key="editor.h4"/>', element: 'h4'},
			{name: '<iwcm:text key="editor.h5"/>', element: 'h5'},
			{name: '<iwcm:text key="editor.h6"/>', element: 'h6'}
		];

		//console.log("setStylesDef, adding styles=", sessionCssParsed);

		$.each(sessionCssParsed, function(i,v){
			var result = {};
			var name = "";
			var nameClass = v.class;
			if (v.tag != "*") {
				name += "["+v.tag;
				var space = nameClass.indexOf(" ");
				//zmen format z [div] container div01 na [div.container] div01
				if (space > 0) {
					name += "."+nameClass.substring(0, space);
					nameClass = nameClass.substring(space+1);
				}
				name += "] ";
			}
			name += nameClass;

			result.name = name;
			result.element = v.tag == "*" ? allElements : v.tag;
			result.attributes = { 'class': v.class };

			cssData.push(result);
		});

		//console.log(JSON.stringify(ckEditorInstance.config.stylesSet) + " vs " + JSON.stringify(cssData));
		//console.log(cssData);

		if (JSON.stringify(ckEditorInstance.config.stylesSet) != JSON.stringify(cssData)) {
			ckEditorInstance.ui.get("Styles").reset();
			ckEditorInstance.config.stylesSet = cssData;
			ckEditorInstance._.stylesDefinitions = null;

			ckEditorInstance.getStylesSet( function( styles ) {
				ckEditorInstance.fire('stylesSet', {styles: styles});
			});

			try
			{
				ckEditorInstance.ui.get("Styles").__proto__.showAll()
				ckEditorInstance.ui.get("Styles")._.panel = null;
			} catch (e) {}
		}

	} catch (e) { console.log(e); }

}

function setTemplatePages(editorForm)
{
	setTemplateSelect($('#headerDocId1, #footerDocId1'), editorForm.docList);
	setTemplateSelect($('#menuDocId1, #rightMenuDocId1'), editorForm.menuList);

	$("#headerDocId1").val(editorForm.headerDocId);
	$("#footerDocId1").val(editorForm.footerDocId);
	$("#menuDocId1").val(editorForm.menuDocId);
	$("#rightMenuDocId1").val(editorForm.rightMenuDocId);
}

function setTemplateSelect(selects, data)
{
	selects.each(function(){
		var select = $(this);
		var html = "";
		$.each(data, function(i, v){
			var obj = parseTemplateSelectData(v);
			html += '<option value="' + obj.id + '">' + obj.title + '</option>';
		});
		select.empty().append(html);
	});
}

function parseTemplateSelectData(dirty)
{
	var clean = {};
	var cleaner = dirty.split("|");

	clean.id = cleaner[0];
	clean.title = cleaner[1];

	return clean;
}

//posledne vybrata verzia ulozenej a nepublikovanej stranky na zobrazenie
var lastHistoryRow;

/**
 * Funkcia, ktora obnovi editor a ajaxom natiahne stranku ulozenu, ale nepublikovanu
 *
 * @param docId - identifikator stranky - docId
 * @param historyId - ulozena verzia stranky, ktora ale este nebola publikovana
 */
function historyPageClick(docId, historyId)
{
	if (docId == 0)
		return;

	$("#editHistoryImg" + historyId).attr("src","/admin/images/ajax-loader.gif");

	$.ajax({
		type: 'GET',
		url: "/admin/rest/document/" + docId + "/" + historyId,
		dataType: 'json',
		timeout: 30000,
		success: function(json)
		{
			ajaxFillEditorForm(json);

			if (lastHistoryRow != undefined)
				$("#historyRow" + lastHistoryRow).css("background-color", "");//odfarbi posledne vybratu stranku

			$("#historyRow" + historyId).css("background-color", "yellow"); //zafarbi aktualne vybratu stranku
			lastHistoryRow = historyId;

			try { $("#editorMainDiv #tabLink1").tab("show"); } catch (e) {}
		},
		error:function(xmlHttpRequest, textStatus, errorThrown)
		{
			try
			{
				if (xmlHttpRequest.status == 200)
				{
					alert("<iwcm:text key="editor.ajax.load.error.login"/>");
					$("#errorWarningDiv").html("<div class=\"topNotify\"><img src=\"/admin/images/warning.gif\" align=\"absmiddle\" alt=\"\"/><iwcm:text key='editor.ajax.load.error.login'/><br /></div>");
				}
				else
				{
					alert("<iwcm:text key="editor.ajax.save.error.default"/>");
					$("#errorWarningDiv").html("<div class=\"topNotify\"><img src=\"/admin/images/warning.gif\" align=\"absmiddle\" alt=\"\"/><iwcm:text key='editor.ajax.save.error.default'/><br /></div>");
				}
			}
			catch (ex)
			{}
		},
		complete: function(xmlRequest, textStatus)
		{
			$("#editHistoryImg" + historyId).attr("src","images/icon_edit.gif");
		}
	});
}

//============ WebJET 8 funkcie ====================

    <%
    boolean unpublishBeforeSaveAs = Constants.getBoolean("editor.unpublishBeforeSaveAs");
    %>

    function saveEditorAs(button)
    {


        var oForm = window.editorForm;

        /* toto je v saveEditor, tu asi netreba
       if (window.FCKBeforePublish)
          {
              var canSubmit = window.FCKBeforePublish(oForm);
              if (canSubmit == false) return;
          }
          */

        <% if(unpublishBeforeSaveAs) { %>
        var availableTemp = oForm.available.checked;
        oForm.available.checked = false;
        <% } %>

        oForm.copyMediaFrom.value = oForm.docId.value;
        oForm.docId.value = -1;
        if (oForm.virtualPath.value==oForm.virtualPathOriginal.value)
        {
            oForm.virtualPath.value="";
        }

        var newTitle = window.prompt("<iwcm:text key="editor.enterNewTitle"/>", oForm.title.value);
        if (newTitle == null) return;
        oForm.title.value = newTitle;
        oForm.title.onblur();

        saveEditor(button, true);

        <% if(unpublishBeforeSaveAs) { %>
        oForm.available.checked = availableTemp;
        <% } %>


        //console.log($(oForm.groupId).val());

        refreshOtherGroups();
        refreshGroup($(oForm.groupId).val());
    }

function saveEditorAsTest(button)
{
    var oForm = window.editorForm;
	var virtualPath = oForm.virtualPath.value;
	if (virtualPath.indexOf("<%=Constants.getString("ABTestingName") %>")==-1)
	{
		var i = virtualPath.indexOf(".");
		var variantName = "<%=Constants.getString("ABTestingName")%>b";
		if (i != -1)
		{
			virtualPath = virtualPath.substring(0, i) + "-" + variantName + virtualPath.substring(i);
		}
		else
		{
			virtualPath = virtualPath + variantName + ".html";
		}

		oForm.virtualPath.value = virtualPath;
		$('#showInMenuId').prop("checked", false).uniform().trigger('change');
		//oForm.sortPriority.value = parseInt(oForm.sortPriority.value) + 1;

		saveEditorAs(button);
	}
}

function saveEditor(button, publish)
{

    try {
        $.isFunction(GridEditorDeinit());
    } catch (e) {}

	var editorForm = window.editorForm;

	var editor = CKEDITOR.instances.data;

	if (window.FCKBeforePublish)
  	{
  		var canSubmit = window.FCKBeforePublish(editorForm);
  		if (canSubmit == false) return;
  	}

	var savingHtml = "<div class=\"topWaiting\"><iwcm:text key='editor.ajax.save.info'/></div>";

	$("#errorWarningDiv").html(savingHtml);
	$("#errorWarningDiv").show();

	needToSave = false;

	if (publish == false && editorForm.publicable.checked)
		editorForm.publicable.checked = false;

	//alert na publikovanie stranky
	if (publish && editorForm.publicable.checked)
		window.alert("<iwcm:text key="editor.publish.note"/> "+editorForm.publishStart.value+" "+editorForm.publishStartTime.value);

	editorForm.saveAsNew = "0";
	if (publish)
		$(editorForm.publish).val("1")	// ak sa ma publikovat, nastavi sa flag publish na true
	else
		$(editorForm.publish).val("0")

	//TODO: treba dorobit standardny save bez ajaxu (ak je vela dat v editore)

	//textarei vo forme, ktory sa posiela sa nastavi kod z FCK editora
	var htmlCode = editor.getData();

	//window.alert("html in wjef="+htmlCode)
	$(editorForm.data).val(htmlCode);

	//console.log($(editorForm).serialize())

	$.ajax({
	  type: 'POST',
	  url: "/admin/rest/document/save?&rnd=" + (new Date()).getTime(),
	  data: $(editorForm).serialize(),
	  dataType: 'json',
	  cache: false,
	  timeout: 300000,
	  success: function(html)
	  {
		  if (html == null)
		  {
			  alert("<iwcm:text key="editor.ajax.save.error.server"/>");
			  $("#errorWarningDiv").html("<div class=\"topNotify\"><div class=\"notifyText\"><iwcm:text key='editor.ajax.save.error.server'/></div></div>");
			  return;
		  }

		  setWarnings(html.ajaxSaveFormWarnings);

		  if(html.ajaxSaveFormPermDenied)
		  {
		  	  alert("<iwcm:text key="editor.permsDenied"/>");
			  $("#errorWarningDiv").html("<div class=\"topNotify\"><div class=\"notifyText\"><iwcm:text key='editor.permsDenied'/></div></div>");
			  return;
		  }

		  //reloadne div s upozorneniami a chybami po ulozeni dokumentu
		  setTimeout(reloadValidationDiv, 100);

		  //ak sa ma refreshnut strom vlavo, tak ho refreshne a vysvieti aktualne ulozenu stranku, ktora je aj otvorena
		  if(html.ajaxSaveFormRefreshLeft == true)
		  {
			  try
			  {
					setTimeout(function() {
						//toto nefungovalo dobre, trebalo reloadnut parenta
						//var groupId = $(editorForm.groupId).val();
						//refreshGroup(groupId);
						reloadWebpagesTree();
					}, 1000);

					refreshOtherGroups();
			  } catch (e) {}
		  }
		  //nastavi virtualnu cestu podla toho, aku mu nastavil SaveDoc
		  $(editorForm.virtualPath).val(html.ajaxSaveFormVirtualPath);
		  $(editorForm.virtualPathOriginal).val(html.ajaxSaveFormVirtualPath);

		  //nastavi docId podla toho, ake mu nastavil SaveDoc
		  $(editorForm.docId).val(html.ajaxSaveFormDocId);
		  $("#docIdInputBox").val(html.ajaxSaveFormDocId);

		  //nastavi lastDocId podla toho, ake mu nastavil SaveDoc
		  $(editorForm.lastDocId).val(html.ajaxSaveFormLastDocId);

		  //nastavi flag zobrazovat podla toho, ake mu nastavil SaveDoc
		  $(editorForm.available).prop('checked', html.ajaxSaveFormAvailable);
		  //console.log("html.ajaxSaveFormAvailable="+html.ajaxSaveFormAvailable);
		  $(editorForm.available).bootstrapSwitch('state' , html.ajaxSaveFormAvailable);

		  //reset editora, uz ho nebude povazovat za zmeneny, kedze vsetky zmeny sa ulozili
		  needToSave = false;
		  //reloadne media zalozku
		  $('#mediaIFrameId').attr("src", "/components/media/admin_media_list.jsp?docid=" + html.ajaxSaveFormLastDocId+"&rnd="+(new Date()).getTime());

		  try
		  {

		  	//toto sa deje predsa hore s testom if(html.ajaxSaveFormRefreshLeft == true) refreshOtherGroups();

		  	//tu sa refreshne povodny adresar
			//toto sa deje predsa hore s testom if(html.ajaxSaveFormRefreshLeft == true) refreshGroup($(editorForm.groupId).val())

			//v groupId mame povodny adresar (pri zmene) a v groupidString mame novy (ak nastala zmena)
		  	var groupIdNew = parseInt(editorForm.groupIdString.value);
		  	if (editorForm.groupId.value != groupIdNew)
		  	{
		  		//refreshni novy adresar
				editorForm.groupId.value = groupIdNew;
		   	refreshGroup($(editorForm.groupId).val())
		   }
		  }
		  catch (e) { console.log(e); }

		  try
		  {
			  if (publish && window.parent && window.parent.parent && window.parent.parent.WJInlineDialog != undefined)
			  {
			     window.setTimeout(inlineReloadParentWindow, 1000);
			  }
			  if (publish && window.parent && window.parent.opener && window.parent.opener.WJInlineDialog != undefined)
			  {
				  window.setTimeout(inlineReloadParentWindow, 1000);
			  }
		  } catch (e) {}

		  try
		  {
		  	  var callFunction = getParameterByName("afterSaveCall");
		  	  //console.log("callFunction1="+callFunction);
		  	  var failsafeCount = 0;
		  	  var currentWindow = window;
		  	  while (failsafeCount++ < 10)
		  	  {
		  	  	  //console.log("Testing window: "+currentWindow.location.href);
		  	     var fn = currentWindow[callFunction];
		  	     //console.log(fn);
		  	     if(typeof fn === 'function')
		  	     {
		  	     		//console.log("Executing fn");
					    fn(editorForm);
					    break;
				  }

				  currentWindow = currentWindow.parent;
		  	  }
		  }
		  catch (e) { console.log("ERROR: "+e); }

		  try
		  {
			  //TODO: FCK.ResetIsDirty();
			  resetDirtyEditor();
			  setTimeout(function() { resetDirtyEditor() }, 1000);
		  }
		  catch (ex) {}
	  },
		error:function(xmlHttpRequest, textStatus, errorThrown)
		{
			try
			{
				if (xmlHttpRequest.status == 200 || xmlHttpRequest.status == 302 || xmlHttpRequest.status == 0)
				{
					alert("<iwcm:text key="editor.ajax.save.error.login"/>");
					$("#errorWarningDiv").html("<div class=\"topNotify\"><div class=\"notifyText\"><iwcm:text key='editor.ajax.save.error.login'/></div></div>");
				}
				else
				{
					$("#errorWarningDiv").html("<div class=\"topNotify\"><div class=\"notifyText\"><iwcm:text key='editor.ajax.save.error'/></div></div>");
				}
			}
			catch (ex) {}
		}
	});
}

function setWarnings(warnings) {
    if (typeof warnings == 'undefined' || warnings.length == 0) {
        return;
	}

    toastr["warning"](warnings.join("<br>"), "WebJET");
}

function refreshGroup(id)
{
	if (id != "" && $('#groupslist-jstree').size()>0)
	{
		setTimeout(function()
		{
			try
			{
				//console.log("refreshGroup=", id);
 				$('#groupslist-jstree').jstree(true).refresh_node(id);
			}
			catch (e) { }
		});
	}
}

function refreshOtherGroups() {
	$('input[name="otherGroups"]', editorForm).each(function(i, v){
		var id = $(this).val();
		otherGroups.push(id);
	});

	var counter = 0;
	$.each(otherGroups, function(i,v){
		//console.log("refreshOtherGroups, group=", v);
		setTimeout(function() {
			refreshGroup(v);
		}, (counter*1000)+200);
		counter++;
	});
}

function previewPage()
{
	var editorForm = window.editorForm;
	var editor = CKEDITOR.instances.data;

	//textarei vo forme, ktory sa posiela sa nastavi kod z FCK editora
	var htmlCode = editor.getData();
	//window.alert("html in wjef="+htmlCode)
	$(editorForm.data).val(htmlCode);

	//document.editorForm.submit();
	previewWindow=window.open("/admin/skins/webjet8/ckeditor/preview.jsp","preview","location=yes,menubar=yes,status=yes,toolbar=yes,scrollbars=yes,resizable=yes,width=790,height=550;");
	if (window.focus) {previewWindow.focus()}
}

function changeNodeText(instance, node) {
	if (typeof node.original != "undefined" && typeof node.original.sortPriority != "undefined") {
		var text = node.text;

		if (text.indexOf('<span class="docId">') == -1 &&
			text.indexOf('<span class="sortPriority">') == -1 &&
			text.indexOf('<span class="dateCreated">') == -1)
		{
			node.original.text = text;
		}

		var before = []
		var after = []

		var id = node.original.docId ? node.original.docId : node.original.groupId;
		var sortPriority = node.original.sortPriority;
		var dateCreated = node.original.dateCreated;

		if (userSettingsJson.docId && typeof id != "undefined") {
			before.push('<span class="docId">[' + id + ']</span>');
		}

		if (userSettingsJson.sortPriority && typeof sortPriority != "undefined") {
			before.push('<span class="sortPriority">[' + node.original.sortPriority + ']</span>');
		}

		if (userSettingsJson.dateCreated && typeof dateCreated != "undefined") {
			after.push('<span class="dateCreated">[' + node.original.dateCreated + ']</span>');
		}

		node.text = before.join(" ") + " " + node.original.text + " " + after.join(" ");
		instance.redraw_node(node.id);

		if (node.children != null && node.children.length > 0) {
			$.each(node.children ,function(i, v){
				changeNodeText(instance, instance.get_node(v));
			});
		}
	}
}

function fillFields(jsonEditorForm) {
    var textTemplate = '<input name="field{pismeno}" maxlength="{maxlength}" data-warningLength="{warninglength}" data-warningMessage="{warningMessage}" value="{value}" id="fieldInput{pismeno}" class="form-control" type="text">';
	var autocompleteTemplate = '<div class="input-icon input-icon-lg right"><i class="fa fa-search"></i><input type="text" class="form-control" name="field{pismeno}" value="{value}" id="fieldAutocompleteId{pismeno}"/></div>';
	var selectTemplate = '<select name="field{pismeno}" id="fieldInput{pismeno}" class="form-control">{options}</select>';
	//JICH - add
    var hiddenTemplate = '<input name="field{pismeno}" value="{value}" id="fieldInput{pismeno}" class="form-control" type="hidden"><div id="fieldDisplay{pismeno}"></div>';
    //JICH - add end
	var fields = jsonEditorForm.fields;

	$.each(fields, function(i, v){
		var keyUpper = v.key.toUpperCase(),
			tr = $('#fieldTr' + keyUpper),
			label = tr.find('label'),
			input = tr.find('input, select'),
            inputBox = input.closest('.input-group');

		var value = v.value;
		value = value.replace(/"/gi, "&quot;");

        var maxlength = v.maxlength;
        var warninglength;
        var warningMessage;

        if (v.warninglength <= 0) {
            warninglength = "";
		} else {
            warninglength = v.warninglength;
		}
		if (v.warningMessage == null) {
            warningMessage = "";
        } else {
            warningMessage = v.warningMessage;
        }

        var template = textTemplate.replace(new RegExp('{pismeno}', 'g'), keyUpper).replace(new RegExp('{value}', 'g'), value).replace(new RegExp('{maxlength}', 'g'), maxlength).replace(new RegExp('{warninglength}', 'g'), warninglength).replace(new RegExp('{warningMessage}', 'g'), warningMessage);

        $('#fieldTr' + keyUpper).show();

		if (v.type == 'select') {
			var options = '<option value=""></option>';
			$.each(v.typeValues, function(it, val){
                var selected = val.value == value;
				options += '<option ' + (selected ? ' selected="true"' : "") + ' value="' + val.value + '">' + val.label + '</option>';
			});

			template = selectTemplate.replace('{options}', options).replace(new RegExp('{pismeno}', 'g'), keyUpper);
		}
		else if (v.type == 'autocomplete') {
            if (typeof AutoCompleter == "undefined") {
			     $('head').append('<script type="text/javascript" src="/components/_common/javascript/ajax_support.js"></script>');
            }

            if (typeof $.ui == "undefined") {
			     $('head').append('<script type="text/javascript" src="/components/_common/javascript/jqui/jquery-ui-core.js" ></script>');
            }

            if (typeof $.ui.autocomplete == "undefined") {
			     $('head').append('<script type="text/javascript" src="/components/_common/javascript/jqui/jquery-ui-autocomplete.js" ></script>');
            }

			template = autocompleteTemplate.replace(new RegExp('{pismeno}', 'g'), keyUpper).replace(new RegExp('{value}', 'g'), value);
		}
		else if (v.type == 'image') {
			template += '<span class="input-group-addon btn green"><i onclick="openImageDialogWindow(\'editorForm\', \'field' + keyUpper + '\', \'\')" class="fa fa-image"></i></span>';
		}
		else if (v.type == 'link') {
			template += '<span class="input-group-addon btn green"><i onclick="openLinkDialogWindow(\'editorForm\', \'field' + keyUpper + '\')" class="fa fa-link"></i></span>';
		}
        else if (v.type == 'file_archiv_link_insert_new') {
            template += '<span class="input-group-addon btn green"><i onclick=" WJDialog.OpenDialog( \'editorForm\' , \'Image\',\'/components/file_archiv/file_archiv_upload.jsp?form=editorForm&amp;field=field' + keyUpper + '\')" class="fa fa-link"></i></span>';
        }
        // LPA
        else if (v.type == 'none') {
			$('#fieldTr' + keyUpper).hide();
		}
		//JICH - add
		else if (v.type == 'hidden') {
		    var button = "";
			if (v.typeValues[0] != null) {
				var dialogScript = v.typeValues[0].label;
				if (dialogScript.indexOf("?") > 0) {
					dialogScript += "&";
				} else {
					dialogScript += "?";
				}
			    var dialogScript2 = dialogScript + "fieldName=fieldInput" + keyUpper + "&fieldValue=" + value;

			    if (v.typeValues[0].value == null || v.typeValues[0].value == "") {
			    	//kdyz nemame displayScript prepneme na textTemplate
					button = "<span type=\"button\" id=\"fieldButton" + keyUpper + "\" class=\"btn green input-group-addon\"><i class=\"fa fa-pencil-square-o\"></i></span>";
			    	hiddenTemplate = textTemplate;
					hiddenTemplate = hiddenTemplate + button;
				} else {
					button = "<span type=\"button\" id=\"fieldButton" + keyUpper + "\" class=\"pull-right btn green\"><i class=\"fa fa-pencil-square-o\"></i></span>";
					hiddenTemplate = button + hiddenTemplate;
				}
			}

			template = hiddenTemplate.replace(new RegExp('{pismeno}', 'g'), keyUpper).replace(new RegExp('{value}', 'g'), value);
		}
		//JICH - add end

		inputBox.html(template);
		label.text(v.label + ":");

		if (v.type == 'autocomplete') {
			new AutoCompleter('#fieldAutocompleteId' + keyUpper).setUrl('/admin/FCKeditor/_editor_autocomplete.jsp?template=' + jsonEditorForm.tempId + '&field=' + keyUpper).transform();
		}

		//JICH - add
		//musime AJAXem zavolat skript pro zobrazeni custom obsahu
		if (v.type == 'hidden') {
			if (v.typeValues[0] != null) {
			    var displayScript = v.typeValues[0].value
				if (displayScript.indexOf("?") > 0) {
					displayScript += "&";
				} else {
					displayScript += "?";
				}
			    displayScript += "fieldName=" + keyUpper + "&fieldValue=" + value;
				$.ajax({
					type : "GET",
					url : displayScript,
					success : function(data) {
						$("#fieldDisplay" + keyUpper).html($.trim(data));
						//upravime tlacitku click udalost
						$("#fieldButton" + keyUpper).click(function() {
							var dialogScript2 = dialogScript + "fieldName=fieldInput" + keyUpper + "&fieldValue=" + $("#fieldInput" + keyUpper).val();
							popup(dialogScript2, 800, 500);
						});
					},
					async : false
				});
			} else {
				$("#fieldButton" + keyUpper).click(function() {
					var dialogScript2 = dialogScript + "fieldName=fieldInput" + keyUpper + "&fieldValue=" + $("#fieldInput" + keyUpper).val();
					popup(dialogScript2, 800, 500);
				});
			}
		}
		//JICH - add end

	});

    $('#sidebarFields input').each(function(){
        var input = $(this);
        var dataWarningLength = $(this).attr('data-warninglength');
        var dataWarningMessage = $(this).attr('data-warningmessage');
        if (dataWarningLength > 0) {
            input.on('input', function(){
                if (dataWarningLength <= input.val().length) {
                    toastr.remove();
                    toastr["warning"](dataWarningMessage);
                } else {
                    toastr.remove();
                }
            })
        }
    })
}


var ckEditorInstance = null;

function getCkEditorInstance()
{
	return ckEditorInstance;
}

var lastEditorHeight = 0;
var webjetInlineInitialized = false;
function resizeEditor()
{
	var height = 0;
	if ( window.parent && window.parent != window.self && window.parent.document.getElementById("inlineEditorToolbarTop")!=null)
	{
		//inline editacia, nastavime velkost parent iframe
		height = $(ckEditorInstance.window.$.document).height();
		var positionTop = $("#trEditor").position().top;
		if (positionTop < 1) return;
		height = height + positionTop + $("#cke_1_contents").position().top + $("#cke_1_bottom").outerHeight() + $("#cke_1_top").outerHeight() + 5;
		if (window.frameElement.height < (height - 10))
		{
			window.frameElement.height = height;
		}

		//nastav poziciu toolbaru
		var scrollTop = $(window.parent).scrollTop() - document.getElementById("cke_data").offsetTop - $(window.parent.document.getElementById("webjetEditorIframe")).offset().top + $(window.parent.document.getElementById("inlineEditorToolbarTop")).height();
		if (scrollTop < 1) scrollTop = 0;
		var toolbarElement = document.getElementById("cke_1_top");
		//toolbarElement.style.cssText = "position: relative; top:"+scrollTop+"px";

		if (webjetInlineInitialized == false)
		{
			webjetInlineInitialized = true;
			toolbarElement.style.cssText = "position: absolute; width: 98%; overflow: visible; top:"+scrollTop+"px";
			$("#cke_1_contents").css("border-top", $("#cke_1_top").outerHeight()+"px solid #f5f5f5");
			//aby sa nezobrazili scrollbary
			$("body").css("overflow", "hidden");
			ckEditorInstance.document.$.body.style.overflowY="auto";
		}

		//toolbarElement.style.cssText = "position: absolute; width: 100%; overflow: visible; top:"+scrollTop+"px";
		//console.log("scrollTop: "+scrollTop);
		toolbarElement.style.top = scrollTop+"px";
	}

	if ($(".page-content").size()==0 || (window.parent && window.parent != window.self))
	{
		//sme v okne alebo iframe
		height = $(window).innerHeight();
		var positionTop = $("#trEditor").position().top;
		if (positionTop < 1) return;
		height = height - positionTop - 2;
		//console.log("PARENT WIN HEIGHT:"+height+" positionTop="+positionTop);
	}
	else
	{
	  height = $("#groupslistTableDiv").innerHeight();
	  var positionTop = $("#trEditor").position().top;
	  if (positionTop < 1) return;

      var paddingBottom = parseInt($(".websiteBgWrapper").css("padding-bottom"));
      if (isNaN(paddingBottom))
	  {
         //sme v aplikacii novinky
		 paddingBottom = $("#wjTopButtons").height() + $("#editorFormId ul.nav").outerHeight() + 25;
         height = $(window).height();
  	  }

      var mainDivTop = parseInt($("#editorMainDiv").css("top"));

	  height = Math.floor( height - positionTop - paddingBottom - mainDivTop - 2 );
      //console.log("PARENT WIN HEIGHT:"+height+" positionTop="+positionTop+" paddingBottom="+paddingBottom+" mainDivTop="+mainDivTop);
	}

	//window.alert(height);
     if (height > 300 && ckEditorInstance.container.$.clientHeight != height && $("#editorFormId #tab1").is(":visible"))
     {
     	//console.log("Setting editor height: "+height+" contentHeight="+$(".page-content").innerHeight()+" offsetTop="+positionTop+" warningDivH="+$("#errorWarningDiv").outerHeight()+" page-content padding:"+ parseInt($(".page-content").css("padding-top")));
     	lastEditorHeight = height;
     	ckEditorInstance.resize("99%", height);

     	$("div.editorTabAutoheight").height(height);
     }

     //console.log("SETTING WIDTH: "+$("#editorMainDiv").width());
     if ($("#editorMainDiv").width() <= 776)
   	{
   	    if (document.editorForm.title.size != 27) document.editorForm.title.size = 27;
   	}

   	try
   	{
   		initSidebarSize();
   	} catch (e) {}

	try
	{
		if (CKEDITOR.currentInstance == null && ckEditorInstance != null)
		{
			//fix, po prepnuti do HTML kodu sa CKEDITOR.currentInstance nastavilo na null, opravme
			CKEDITOR.currentInstance = ckEditorInstance;
		}
	} catch (e) {}
}

var ckEditorAtLeastOneInitialized = false;

function initializeCkEditor()
{
	<%
	String ckEditorConfig = "/admin/skins/webjet8/ckeditor/config.jsp";
	String customConfig = "/components/"+Constants.getInstallName()+"/admin/ckeditor/config.jsp";
	if (FileTools.isFile(customConfig))
	{
		ckEditorConfig = customConfig;
	}
	%>
	var configLink = '<%=ckEditorConfig%>?v=<%=CombineTag.getVersion()%>';

	initializeCkEditorImpl("data", CKEDITOR.replace, configLink)
}

function ckeditorFixIframeDialog(iframeElement) {
	setTimeout(function() {
		//console.log("iframeElement=", iframeElement, " tr height=", $(iframeElement).parent().parent().height());
		//chrome bug - nastavime vysku TD elementu rovnako ako TR elementu, height 100% na td to neberie
		$(iframeElement).parent().css("padding-bottom", "0px");
		$(iframeElement).closest("td.cke_dialog_contents_body").css("padding-bottom", "0px");
		/*$(iframeElement).parent().css("height", ($(iframeElement).parent().parent().height()-4)+"px");
		setTimeout(function() {
			//height po rendri musime vratit nazad na 100%, aby sa mohol zvacsit podla contentu
			$(iframeElement).parent().css("height", "100%");
		}, 5000);*/
		//toto pomohlo, okno sa totiz zvacsuje a nemoze mat nastaveny presny rozmer, min-height tiez nefungoval
		if (navigator.userAgent.indexOf("Firefox")==-1) $(iframeElement).parent().css("display", "block");
	}, 100);
}

function initializeCkEditorImpl(ckEditorElementId, ckEditorInitFunction, configLink)
{
	var editorElem = document.getElementById("trEditor");

	CKEDITOR.config.justifyClasses = [ 'align-left', 'align-center', 'align-right', 'align-justify' ];
	var imageAlignClasses = ['image-left', 'image-right'];

	<% if (Constants.getBoolean("bootstrapEnabled")) { %>
		CKEDITOR.config.justifyClasses = [ 'text-left', 'text-center', 'text-right', 'text-justify' ];
		imageAlignClasses = ['pull-left image-left', 'pull-right image-right'];
	<% } %>

	//CKEDITOR.config.image_alignClasses = [ 'align-left', 'align-center', 'align-right' ];
	/*
	 * @example An iframe-based dialog with custom button handling logics.
	 */

	if (ckEditorAtLeastOneInitialized == false)
	{
		CKEDITOR.on( 'dialogDefinition', function( ev )
		{
			var dialogName = ev.data.name;
			var dialogDefinition = ev.data.definition;

			/*
			console.log("On dialog definition, name="+dialogName);
			console.log("THIS:");
			console.log(this);
			console.log("EV:");
			console.log(ev);
			*/

			//console.log("dialogName: "+dialogName);

			if ( dialogName == 'tableProperties' || dialogName == 'table')
			{
				//console.log("tableProperties");

				var infoTab = dialogDefinition.getContents('info');
		        infoTab.get('selHeaders')['default'] = 'row';
			    infoTab.get('txtWidth')['default'] = '100%';
				var advancedTab = dialogDefinition.getContents('advanced');
				if (typeof window.bootstrapVersion == "undefined" || window.bootstrapVersion.indexOf("3")==0) {
					advancedTab.get('advCSSClasses')['default'] = 'tabulkaStandard';
				} else {
					advancedTab.get('advCSSClasses')['default'] = 'table table-sm tabulkaStandard';
				}
	      	dialogDefinition.dialog.on( 'show', function()
				{
					//this.getContentElement("info", "txtSummary");
					//this.getContentElement("info", "txtSummary").getElement().hide();
				});
				dialogDefinition.onOk = CKEDITOR.tools.override(dialogDefinition.onOk, function(original) {
					return function()
					{
						var wrapTable = false;
						if (typeof window.bootstrapVersion != "undefined" && window.bootstrapVersion.indexOf("3")!=0) wrapTable = true;

						var id = null;
						var originalId = null;
						if (wrapTable) {
							//nastav ID na nasu hodnotu
							var advField = this.getContentElement("advanced", "advId");
							id = "table"+(new Date()).getTime();
							originalId = advField.getValue();
							advField.setValue(id);
						}

						original.call(this);

						if (wrapTable) {
							var table = CKEDITOR.currentInstance.document.$.getElementById(id);
							var $table = $(table);
							var parent = $table.parent("div.table-responsive");

							//console.log("table=", table, "$table=", $table);
							//console.log("this=", this, "parent=", parent);

							if (parent.length == 0)	$table.wrap('<div class="table-responsive"></div>');

							if (originalId != "") $table.attr("id", originalId);
							else $table.removeAttr("id");
						}

						return;
					}
				});
			}

			if ( dialogName == 'image')
			{

				dialogDefinition.minWidth = 800;
				dialogDefinition.minHeight = 460;

				dialogDefinition.addContents(
				{
					id: 'wjImage',
			    	label: '<iwcm:text key="admin.fck_image.images"/>',
			    	elements: [
			    	   {
			    		   type: 'html',
			    		   id: 'wjImageIframe',
			    		   html: '<div><iframe id="wjImageIframeElement" style="width: 800px; height: 460px;" src="/admin/skins/webjet8/ckeditor/dist/plugins/webjet/wj_image.jsp" border="0"/></div>'
			    	   }
			    	]
				}, 'info');

				var linkTab = dialogDefinition.getContents('Link');
				linkTab.add({
					id: 'txtRel',
					type: 'text',
					requiredContent: 'a[rel]',
					label: "<iwcm:text key="editor.image.rel"/>",
					'default': '',
					setup: function( type, element ) {
						if ( type == 2 )
							this.setValue( element.getAttribute( 'rel' ) || '' );
					},
					commit: function( type, element ) {
						if ( type == 2 ) {
							if ( this.getValue() || this.isChanged() )
								element.setAttribute( 'rel', this.getValue() );
						}
					}
				});

				<%
				boolean pixabay = Constants.getBoolean("pixabayEnabled");
				if (pixabay) {
				%>
				dialogDefinition.addContents(
				{
					id: 'wjImagePixabay',
			    	label: '<iwcm:text key="editor.photobank"/>',
			    	elements: [
			    	   {
			    		   type: 'html',
			    		   id: 'wjImageIframe',
			    		   html: '<div><iframe id="wjImagePixabayIframeElement" style="width: 800px; height: 455px;" src="/admin/skins/webjet8/ckeditor/dist/plugins/webjet/wj_pixabay.jsp" border="0"/></div>'
			    	   }
			    	]
				}, 'info');
				<% } %>

				dialogDefinition.dialog.on( 'show', function()
				{
					this.getContentElement("info", "txtUrl").getElement().hide();
					this.getContentElement("info", "txtAlt").getElement().hide();
					//this.getContentElement("info", "dataName").getElement().hide();
					//this.getContentElement("info", "txtWidth").getElement().hide();
					//this.getContentElement("info", "txtHeight").getElement().hide();
					this.getContentElement("info", "ratioLock").getElement().hide();
					this.getContentElement("info", "txtBorder").getElement().hide();
					this.getContentElement("info", "cmbAlign").getElement().hide();
					//TODO: this.getContentElement("Link").getElement().hide();

					// image alignment classes
					var dialog = this;
					this.getContentElement("info", "cmbAlign").on( 'change', function( changeEvent ) {
						var align = $.trim(this.getValue());
						var classes = dialog.getContentElement("advanced", "txtGenClass").getValue();
						classes = classes.split(" ");

						var imageAlignClassesGrep = imageAlignClasses.join(" ").split(" ");
						//console.log("imageAlignClassesGrep"); console.log(imageAlignClassesGrep);

						classes = jQuery.grep(classes, function( v ) {
						  	return $.inArray(v, imageAlignClassesGrep) == -1;
						});

						if (align != "") {
							classes.push("image-" + align);
							classes.push("pull-" + align);
						}

						dialog.getContentElement("advanced", "txtGenClass").setValue($.trim(classes.join(" ")));
					});

					this.getContentElement("info", "txtWidth").getElement().on( 'keydown', function( changeEvent ) {
						//ked napise cislo do sirky tak zrusme img-responsive, inak sa hodnota nepouzije
						var classes = dialog.getContentElement("advanced", "txtGenClass").getValue();
						classes = classes.replace("img-responsive", "");
						classes = classes.replace("fixedSize", "");
                        classes = classes.replace("img-fluid", "");
                        classes = classes.replace("w-100", "");
						dialog.getContentElement("advanced", "txtGenClass").setValue(classes);
					});

					this.getContentElement("info", "txtHeight").getElement().on( 'keydown', function( changeEvent ) {
						//ked napise cislo do sirky tak zrusme img-responsive, inak sa hodnota nepouzije
						var classes = dialog.getContentElement("advanced", "txtGenClass").getValue();
						classes = classes.replace("img-responsive", "");
						classes = classes.replace("fixedSize", "");
                        classes = classes.replace("img-fluid", "");
                        classes = classes.replace("w-100", "");
						dialog.getContentElement("advanced", "txtGenClass").setValue(classes);
					});

					setTimeout( function()
					{
						try
						{
							var wjImageIframe = CKEDITOR.document.getById( 'wjImageIframeElement' );
							if (wjImageIframe && wjImageIframe.$)
							{
								//console.log("INITIALIZED: "+wjImageIframe.$.contentWindow.elFinderInitialized);

								wjImageIframe.$.contentWindow.refreshValuesFromCk();

								if (wjImageIframe.$.contentWindow.elFinderInitialized == true) wjImageIframe.$.contentWindow.updateElfinderCustomData();
							}
						}
						catch (e)
						{
							//toto moze nastat pri prvom nacitani, vtedy sa ale refresh zavola priamo v iframe kode
							//console.log("Error image dialog show, e="+e);
						}
					}, 200);

				});

				dialogDefinition.onOk = CKEDITOR.tools.override(dialogDefinition.onOk, function(original) {
					return function()
					{
						var video = ['.mp3', '.mp4', '.flv', 'youtube.com', 'youtu.be', 'vimeo.com', 'facebook.com/facebook/videos/', 'facebook.com/watch'];
						var txtUrl = this.getContentElement("info", "txtUrl").getValue();

						var videoFile = "";
						$.each(video, function(i,v)
						{
							if (txtUrl.indexOf(v) != -1 || (txtUrl.indexOf("facebook.com")!=-1 && txtUrl.indexOf("/videos/")!=-1) ) {
								videoFile = txtUrl;
								return false;
							}
						});

						if (videoFile != "") {
							var editor = CKEDITOR.currentInstance;
							editor.wjInsertUpdateComponent("!INCLUDE(/components/video/video_player.jsp, file="+videoFile+")!");
							return;
						}

						//vybral maly obrazok z galerie, treba nastavit odkaz
						if (txtUrl.indexOf("/images")!=-1 && txtUrl.indexOf("/s_")!=-1)
						{
							this.getContentElement("Link", "txtUrl").setValue(txtUrl.replace("/s_", "/"));
							this.getContentElement("Link", "txtRel").setValue("wjimageviewer");
						}

						//pre fixedSize nesmieme nastavit width a height
						var classNames = this.getContentElement("advanced", "txtGenClass").getValue();
						if (classNames != undefined && classNames != null)
						{
						    //img-fluid a w-100, w-75 atd su z Bootstrap 4
							if (classNames.indexOf("fixedSize")!=-1 || classNames.indexOf("img-responsive")!=-1 || classNames.indexOf("img-fluid")!=-1 || classNames.indexOf("w-")!=-1 || classNames.indexOf("card-img-top")!=-1)
							{
								if (txtUrl.indexOf(".svg")>0 && classNames.indexOf("fixedSize")!=-1)
								{
									var fixedSizeRegex = /fixedSize-(\d+)-(\d+)(?:-(\d+))?/gi;
									var matched = fixedSizeRegex.exec(classNames);
									//console.log(matched);
									if (matched != null && matched.length == 4)
									{
										var actualWidth = matched[1];
										var actualHeight = matched[2];

										var ip = 0;
										if (matched[3]!=undefined) ip = matched[3];
										//console.log("ip=", ip);
										if ("1"==ip || 1==ip) actualHeight = "";
										else if ("2"==ip || 2==ip) actualWidth = "";

										this.getContentElement("info", "txtWidth").setValue(actualWidth);
										if ("0"==actualWidth || 0==actualWidth) this.getContentElement("info", "txtWidth").setValue("");
										this.getContentElement("info", "txtHeight").setValue(actualHeight);
										if ("0"==actualHeight || 0==actualHeight) this.getContentElement("info", "txtHeight").setValue("");
									}
								}
								else
								{
									this.getContentElement("info", "txtWidth").setValue("");
									this.getContentElement("info", "txtHeight").setValue("");
								}
							}
						}

						var setAutomaticTitle = <%=Constants.getBoolean("editorImageAutoTitle")%>;
						if(setAutomaticTitle)
						{
							var txtGenTitle = this.getContentElement("advanced", "txtGenTitle").getValue();
							if((txtUrl!=undefined && txtUrl != null) && (txtGenTitle===null || txtGenTitle==""))
							{
								//odpazime cestu k suobru, potom jeho priponu a potom nahradime podciarniky a pomlcky za medzery
								var imageName = txtUrl.replace(/^.*[\\\/]/, '').replace(/\.[^/.]+$/, "").replace(/_|-/g, " ");
								var projectName = "<%=prop2.getText("default.project.name")%>";
								if(projectName.length>0)
									projectName = " | " + projectName;
								this.getContentElement("advanced", "txtGenTitle").setValue(imageName + projectName);
							}
						}

						return original.call(this);
					};
				});

			}
			else if ( dialogName == 'link')
			{
				//console.log("link dialog open OLD version");

				dialogDefinition.minWidth = 800;
				dialogDefinition.minHeight = 445;

				dialogDefinition.dialog.on( 'show', function()
				{
					this.getContentElement("info", "url").getElement().hide();
					this.getContentElement("info", "protocol").getElement().hide();
					this.getContentElement("info", "linkType").getElement().hide();

					//console.log("ON SHOW 1");
					var urlElement = this.getContentElement("info", "url").getElement();
					var wjLinkIframeId = "wjLinkIframe-"+getCkEditorInstance().name;
					//console.log("wjLinkIframeId=", wjLinkIframeId, "instance=", getCkEditorInstance());
					if (document.getElementById(wjLinkIframeId) == null)
					{
						var iframeElement = new CKEDITOR.dom.element("IFRAME");
						iframeElement.setAttribute("src", "/admin/skins/webjet8/ckeditor/dist/plugins/webjet/wj_link.jsp");
						iframeElement.setAttribute("id", wjLinkIframeId);
						//iframeElement.setAttribute("width", 580);
						iframeElement.setStyle("width", 800+"px");
						//iframeElement.setAttribute("height", 445);
						iframeElement.setStyle("height", 455+"px");
						iframeElement.setStyle("margin-left", "-12px");
						iframeElement.setStyle("margin-right", "-12px");

						//urlElement.$.insertBefore(iframeElement, .$);
						urlElement.getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().insertBeforeMe(iframeElement);
						//schovaj celu tabulku lebo ma paddingy a pod URL fieldom sa zobrazuje sedy pas
						urlElement.getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().getParent().hide();
					}

					//console.log("SHOW EVENT: "+urlElement);

					setTimeout( function()
					{
						try
						{
							//console.log("ON SHOW");
							var wjLinkIframeId = "wjLinkIframe-"+getCkEditorInstance().name;
							//console.log("wjLinkIframeId=", wjLinkIframeId, "instance=", getCkEditorInstance());
							var wjLinkIframe = CKEDITOR.document.getById( wjLinkIframeId );
							if (wjLinkIframe && wjLinkIframe.$)
							{
								//console.log("Idem updatnut");

								wjLinkIframe.$.contentWindow.refreshValuesFromCk();

								if (wjLinkIframe.$.contentWindow.elFinderInitialized == true) wjLinkIframe.$.contentWindow.updateElfinderCustomData();

								//console.log("Updatnute");
							}
						}
						catch (e)
						{
							//toto moze nastat pri prvom nacitani, vtedy sa ale refresh zavola priamo v iframe kode
							//console.log(e);
						}
					}, 200);

				});

				dialogDefinition.onOk = CKEDITOR.tools.override(dialogDefinition.onOk, function(original) {
					return function()
					{
						//prepneme na URL typ inak nam to nesetne dobre z kotvy
						this.getContentElement("info", "linkType").setValue("url");
						var url = this.getContentElement("info", "url").getValue();
						if (url.indexOf("@")!=-1 && url.indexOf("mailto:")==-1)
						{
							url = "mailto:"+url;
							this.getContentElement("info", "url").setValue(url);
						}

						//ak ako URL je zadane #nazov vytvori sa kotva
						if (url.indexOf("#")==0)
						{
							var editor = ckEditorInstance;
							var sel = editor.getSelection(),
							range = sel && sel.getRanges()[ 0 ];

							var name = url.substring(1);
							var attributes = {
								id: name,
								name: name,
								'data-cke-saved-name': name
							};

							// Empty anchor
							if ( range.collapsed )
							{
								var anchor = editor.createFakeElement( editor.document.createElement( 'a', {
									attributes: attributes
								} ), 'cke_anchor', 'anchor' );
								range.insertNode( anchor );
								return;
							}
						}

						return original.call(this);
					}
				});

			}
			else if ( dialogName == 'form')
			{
				dialogDefinition.minWidth = 600;
				dialogDefinition.minHeight = 445;

				dialogDefinition.addContents(
				{
					id: 'wjFormAttributes',
			    	label: ev.editor.lang.webjetcomponents.forms.formAttributes,
			    	elements: [
			    		{
			    		   type: 'html',
			    		   id: 'wjFormAttributesIframe',
			    		   //html: '<div><iframe id="wjFormAttributesIframeElement" style="width: 600px; height: 455px;" src="/components/form/admin_form_attributes.jsp" border="0"/></div>'
			    		   html: '<div><div id="attributesContent" style="height: 425px; overflow: auto;"><iwcm:text key="divpopup-blank.wait_please"/></div></div>'
			    	   }
			    	]
				});

				dialogDefinition.addContents(
				{
					id: 'wjFormRestrictions',
			    	label: ev.editor.lang.webjetcomponents.forms.fileLimits,
			    	elements: [
			    		{
			    		   type: 'html',
			    		   id: 'wjFormRestrictionsIframe',
			    		   //html: '<div><iframe id="wjFormAttributesIframeElement" style="width: 600px; height: 455px;" src="/components/form/admin_form_attributes.jsp" border="0"/></div>'
			    		   html: '<div><div id="restrictionsContent" style="height: 150px;"><iwcm:text key="divpopup-blank.wait_please"/></div></div>'
			    	   }
			    	]
				});

				var infoTab = dialogDefinition.getContents( 'info' );
				infoTab.add( {
					type: 'text',
					label: ev.editor.lang.common.name,
					id: 'wjFormName'
				}, "txtName");

				dialogDefinition.dialog.on( 'show', function()
				{
					//console.log("ON SHOW 2");
					//console.log(this.getContentElement("info", "action").getElement());
					//console.log(this.getContentElement("info", "action").getValue());

					this.getContentElement("info", "txtName").getElement().hide();
					this.getContentElement("info", "action").getElement().hide();

					var path = this.getParentEditor().elementPath();
					var form = path.contains( 'form', 1 );

					//console.log(path);
					//console.log(form);

					if (this.getContentElement("info", "action").getValue() == "")
					{
						this.getContentElement("info", "action").setValue("/formmail.do");
					}
					if (this.getContentElement("info", "txtName").getValue() == "")
					{
						this.getContentElement("info", "txtName").setValue("formMailForm");
					}
					if (this.getContentElement("info", "method").getValue() == "")
					{
						this.getContentElement("info", "method").setValue("post");
					}

					var wjFormName = ev.editor.element.$.form.title.value;

					if (form != null)
					{
						var formAction = form.getAttribute("action");
						var formMethod = form.getAttribute("method");

						if (formAction.indexOf("formmail.do")!=-1)
					   {
							var index = formAction.indexOf("?");
							if (index>0)
							{
								var params = formAction.substring(index+1);
								var paramsArray = params.split("&");
								var i;
								for (i=0; i < paramsArray.length; i++)
								{
									index = paramsArray[i].indexOf("=");
									if (index!=-1)
									{
									   var name = paramsArray[i].substring(0, index);
									   var value = paramsArray[i].substring(index+1);
									   value = unencodeValue(value);

									   if (name=="savedb")
									   {
										   wjFormName = value;
									   }
									}
								}
							}
					   }
					}

					//odstran diakritiku a medzery z wjformname
					wjFormName = removeChars(internationalToEnglish(wjFormName));

					this.getContentElement("info", "wjFormName").setValue( wjFormName );

					try
					{

                        var input = {hasUserApproved:'true',formName:encodeValue(wjFormName)}
                        $.ajax({
                            type: "POST",
                            url: "/components/form/admin_gdpr_check-ajax.jsp",
                            data: input,
                            success: function(data){
                                data = $.trim(data);
                                if(data.indexOf('true') == 0)
                                {
                                    $("#attributesContent").load("/components/form/admin_form_attributes.jsp?formname="+ encodeValue(wjFormName));
                                    $("#restrictionsContent").load("/components/form/admin_file_restriction_form.jsp?formname="+encodeValue(wjFormName));
                                }
                                else
                                {
                                    if(confirm('<%=prop2.getText("components.forms.alert.gdpr")%>'))//pratest
                                    {
                                        $.post( "/components/form/admin_gdpr_check-ajax.jsp?addUserApprove=true&formName="+encodeValue(wjFormName), function( data ) {
                                            data = $.trim(data);
                                            if(data.indexOf('true') == 0)
                                            {
                                                //alert('ok');
                                            }
                                        });
                                        $("#attributesContent").load("/components/form/admin_form_attributes.jsp?formname="+ encodeValue(wjFormName));
                                        $("#restrictionsContent").load("/components/form/admin_file_restriction_form.jsp?formname="+encodeValue(wjFormName));

                                    }
                                    else
                                    {
                                        setTimeout(function(){
                                        dialogDefinition.dialog.hide()
                                        }, 1);
                                    }
                                }
                            }
                        });
					}
					catch (e)
					{
						//toto moze nastat pri prvom nacitani, vtedy sa ale refresh zavola priamo v iframe kode
						console.log(e);
					}

					//this.getContentElement("info", "txtName").getElement().hide();
				});

				dialogDefinition.onOk = CKEDITOR.tools.override(dialogDefinition.onOk, function(original) {
					return function()
					{
						var formName = this.getContentElement("info", "wjFormName").getValue();
						formName = removeChars(internationalToEnglish(formName));
						this.getContentElement("info", "wjFormName").setValue(formName);

						if (this.getContentElement("info", "method").getValue() == "")
						{
							this.getContentElement("info", "method").setValue("post");
						}
						var attribues = this;

						var input = {hasUserApproved:'true',formName:formName}

						var thisDialog = this;

                        $.ajax({
                            type: "POST",
                            url: "/components/form/admin_gdpr_check-ajax.jsp",
                            data: input,
                            success: function(data){
                                data = $.trim(data);
                                if(data.indexOf('true') == 0)
                                {
                                    //return true;
									saveAttributes(attribues);//this

									var formAction = thisDialog.getContentElement("info", "action").getValue();
									if (formAction.indexOf("/formmail.do")!=-1)
									{
										formAction = "/formmail.do?savedb=" + formName;
                                        thisDialog.getContentElement("info", "action").setValue(formAction);

                                        original.call(thisDialog);

                                        dialogDefinition.dialog.hide()
									}
                                }
                                else
                                {
                                    if(confirm('<%=prop2.getText("components.forms.alert.gdpr")%>'))//pratest
                                    {
                                        $.post( "/components/form/admin_gdpr_check-ajax.jsp?addUserApprove=true&formName="+formName, function( data ) {
                                            data = $.trim(data);
                                            if(data.indexOf('true') == 0)
                                            {
                                                //alert('ok');
												saveAttributes(attribues);//this

												var formAction = thisDialog.getContentElement("info", "action").getValue();
												if (formAction.indexOf("/formmail.do")!=-1)
												{
													formAction = "/formmail.do?savedb=" + formName;
                                                    thisDialog.getContentElement("info", "action").setValue(formAction);
												}

                                                original.call(thisDialog);

                                                dialogDefinition.dialog.hide()
                                            }
                                        });
                                    }
                                    else
                                    {
                                        setTimeout(function(){
                                        	dialogDefinition.dialog.hide()
                                        }, 1);
                                    }
                                }
                            }
                        });

						return false;
					};
				});
			}
			else if ( dialogName == 'textfield' || dialogName == 'textarea' || dialogName == 'radio' || dialogName == 'checkbox' || dialogName == 'select')
			{
				var infoTab = dialogDefinition.getContents( 'info' );

				if (infoTab != null)
				{
					var infoTab = dialogDefinition.getContents( 'info' );
					infoTab.add( {
						type: 'text',
						label: '<iwcm:text key="calendar_edit.title" />',
						id: 'data-name'
					}, '_cke_saved_name');

					/**/
					infoTab.add({
						type: 'text',
						//label: '<iwcm:text key="calendar_edit.title" />',
						id: 'id'
					});
					/**/

					infoTab.remove( 'required' );

					infoTab.add( {
						id: 'wjrequired',
						type: 'checkbox',
						label: ev.editor.lang.webjetcomponents.forms.required,
						'default': '',
						accessKey: 'Q',
						value: 'required',
						setup: function( element )
						{
							if (element.getAttribute && element.getAttribute( 'class' ) != null && element.getAttribute( 'class' ).indexOf("required")!=-1 ) this.setValue("required");
							else this.setValue("");
						},
						commit: function( data )
						{
							//console.log("COMMIT na checkboxe");

							var element = data.element;
							//toto plati pre textareu
							if (typeof element === 'undefined')
							{
								element = data.$;
							}

							var dialog = CKEDITOR.dialog.getCurrent();
							var required = this.getValue();
							var requiredType = "";
							try
							{
								requiredType = dialog.getValueOf("info", "requiredType");
							} catch (e) {}

							var classValue = requiredType;
							if (required == true)
							{
								if (classValue != "") classValue += " ";
								classValue += "required";
							}

							//console.log("commit required, classValue="+classValue+" element="+element+" current:"+element.getAttribute("class"));
							var currentClasses = element.getAttribute("class");
							if (currentClasses!=null && currentClasses!="")
							{
								var currentArray = currentClasses.split(" ");
								//console.log(currentArray);

								var checkFormNames = new Array();
								checkFormNames.push("required");
								<%
									FormDB myFormDB = FormDB.getInstance();
									List<String[]> regularExpr = myFormDB.getAllRegularExpression();
									for(String[] regExp: regularExpr)
									{
										out.println("checkFormNames.push(\""+regExp[1]+"\");");
									}
								%>

                                //console.log("checkFormNames:");
								//console.log(checkFormNames);

								var isCheckFormClass = false;
                                for (var i=0; i<currentArray.length; i++)
								{
								    //odstranime len CSS triedy checkformu, ostatne ponechame
                                    isCheckFormClass = false;
									for (var j=0; j<checkFormNames.length; j++) {
                                        //console.log(currentArray[i]+":"+checkFormNames[j]+";");
                                        if (currentArray[i] == checkFormNames[j]) {
                                            //console.log("================== isCheckFormClass --------")
                                    		isCheckFormClass = true;
                                    		break;
                                        }
                                    }
                                    if (isCheckFormClass == false)
									{
									    //console.log("Pridavam classValue "+currentArray[i]);
                                        classValue = classValue + " " + currentArray[i];
									}
								}
							}
							//console.log("SETTING classValue:"+classValue);
							if (element) element.setAttribute("class", classValue);
						}
					});

					//vyber typu povinneho pola je len pre text polia
					//console.log("dialogName="+dialogName);
					if ( dialogName == 'textfield' || dialogName == 'textarea')
					{
						infoTab.add( {
							id: 'requiredType',
							type: 'select',
							label: ev.editor.lang.webjetcomponents.forms.requiredType,
							'default': 'text',
							accessKey: 'M',
							items: [
								[ '',	'' ],
								<%
								//FormDB myFormDB = FormDB.getInstance();
								//List<String[]> regularExpr = myFormDB.getAllRegularExpression();
								int counter = 0;
								Prop prop = Prop.getInstance(Constants.getServletContext(), request);
								for(String[] regExp: regularExpr)
								{
									counter++;
									out.print("					[ \""+regExp[1] + " - " + prop.getText(regExp[0]) +"\", \""+regExp[1]+"\"]");
									if (counter < regularExpr.size()) out.print(",");
									out.println();
								}
								%>
							],
							setup: function( element )
							{
								//this.setValue( element.getAttribute( 'type' ) );
								if (element.getAttribute && element.getAttribute( 'class' ) != null)
								{
								    //console.log(this.items);
									var elClass = element.getAttribute( 'class' ).split(" ");
									var i = 0;
									for (i=0; i< elClass.length; i++)
									{
										if (elClass[i]=="required") continue;
										for (var j=0; j<this.items.length; j++)
										{
										    //console.log("comparing "+elClass[i]+":"+this.items[j][1]+";");
											if (elClass[i]==this.items[j][1]) this.setValue(elClass[i]);
										}

									}
								}
							}
						});
					}

					dialogDefinition.dialog.on('show', function(event)
					{
						var savedNameElement = this.getContentElement("info", "_cke_saved_name");
						var idElement = this.getContentElement("info", "id");

						if (typeof savedNameElement != "undefined") {
								$(savedNameElement.getElement().$).parent('td').hide()
						}

						if (typeof idElement != "undefined") {
							$(idElement.getElement().$).parent('td').hide()
						}

						var $this = this;

						// hodnotu z '_cke_saved_name' sa nedarilo nacitat bez timeoutu
						setTimeout(function(){
							setName($this);
						}, 1);

						function setName($this)
						{
							var value = $this.getContentElement("info", "data-name").getValue();

							if (value == "")
							{
								//skus najst element podla ID a z neho data element, lebo CK nam pre select vracia clear namiesto elementu
								var nameCleared = $this.getContentElement("info", "_cke_saved_name");
								if (nameCleared != "")
								{
									var editor = ckEditorInstance;
									//toto je element na ktory sa doubleclicklo (je selectnuty)
									var element = editor.getSelection().getStartElement();

									//console.log("MAM ELEMENT: ")
									//console.log(element);
									if (element)
									{
										value = element.data("name");

										//lebo select sa zle renderuje
										if (element.hasClass("required"))
										{
											$this.getContentElement("info", "wjrequired").setValue("true");
										}
									}
								}
							}

							//console.log("setName, value 1="+value);

							var savedNameElement = $this.getContentElement("info", "_cke_saved_name");

							//console.log(savedNameElement);

							if ((value == null || value == "") && typeof savedNameElement != "undefined") {
								value = savedNameElement.getValue();
							}

							//console.log("setName, value="+value);

							$this.getContentElement("info", "data-name").setValue(value);
						}
					});
				}

				dialogDefinition.onOk = CKEDITOR.tools.override(dialogDefinition.onOk, function(original) {
					return function()
					{
						var savedNameElement = this.getContentElement("info", "_cke_saved_name");
						var oldElement = this.getParentEditor().getSelection().getSelectedElement();
						//console.log("oldElement="+oldElement);

						var prevName = "";
						if (oldElement != null)
						{
						   prevName = $(oldElement.$).prop("id");
						   //console.log("prevName 1="+prevName);
						}

						if (typeof savedNameElement == "undefined")
						{
							var result = original.call(this);
							return result;
						}

						var name = this.getContentElement("info", "data-name").getValue();

						//console.log(name);

						if (name == "") {
							name = this.getContentElement("info", "name").getValue();
						}

						var nameCleared = internationalToEnglish(name).toLowerCase();
						var idElement = this.getContentElement("info", "id");

						if (typeof savedNameElement != "undefined") {
							savedNameElement.setValue(nameCleared);
						}

						if (typeof idElement != "undefined") {
							idElement.setValue(nameCleared);
						}

						//----- zavolajme vytvorenie input pola ------
						var result = original.call(this);

						var editor = ckEditorInstance;
						var startElement = editor.getSelection().getStartElement();

						//console.log(startElement);
						//console.log("prevName="+prevName);

						var tagName = "input";
						if (dialogName == 'select') tagName = "select";
						if (dialogName == 'textarea') tagName = "textarea";
						var element = $(startElement.$).find(tagName+'[name="' + nameCleared + '"]');
						if (element.length == 0) element = $(startElement.$).find(tagName+'[id="' + nameCleared + '"]');
						if (element.length == 0) element = $(startElement.$).find(tagName+'[data-cke-saved-name="' + nameCleared + '"]');

						//console.log("element.length="+element.length);
						//console.log(element);

						if (element.length == 0) {
							element = $(startElement.$);
						}

						//console.log("ID="+element.attr("id"));
						element.attr("id", nameCleared);
						//console.log("Setting data-name to: "+name);
						element.attr("data-name", name);

						if (dialogName == 'select')
						{
							//selectu sa zle nastavuje required
							var required = this.getContentElement("info", "wjrequired").getValue();
							//console.log("required="+required);
							var classValue = "";
							if (required == true)
							{
								if (classValue != "") classValue += " ";
								classValue += "required";
							}
							//console.log("commit required, classValue="+classValue+" element="+element);
							element.attr("class", classValue);
						}


						if (element.is(':radio, :checkbox')) {
							name = element.val();
						}

						if (oldElement==null) bootstrapWrapField(element, name);
						insertLabel(element, prevName, name);

						return result;
					};
				});

				function bootstrapWrapField(element, text) {
					if (typeof window.bootstrapVersion == "undefined" || window.bootstrapVersion.indexOf("3")==0) return;

					//console.log("bootstrapWrapField, element=", element);
					window.bsElement = element;
					var $el = $(element);
					var id = element.prop('id');

					//ak je parent TD, tak neries bs wrapping
					var parents = $el.closest("TD,TH,FORM");
					//console.log("parents=", parents);
					if (parents.length>0 && (parents[0].tagName=="TD" || parents[0].tagName=="TH")) return;

					var wrapperClassName = "form-group";
					var elementClassName = "form-control";
					var inputType = $el.prop("type");
					var label = null;
					if (typeof inputType!="undefined") {
						inputType = inputType.toLowerCase();
						if ("radio"==inputType || "checkbox"==inputType) {
							wrapperClassName = "form-check";
							elementClassName = "form-check-input";

							//priprav label za element
							label = $('<label>' + text + '</label>');
							label.prop("for", id);
							label.addClass("form-check-label");
						}
					}
					var wrapperHtml = '<div class="'+wrapperClassName+'"></div>';
					if ("FORM" == $el.parent().prop("tagName")) {
						//console.log("wrapping element");
						$el.wrap(wrapperHtml);
					} else if ("P" == $el.parent().prop("tagName")) {
						//console.log("changing from P to DIV");
						$el.unwrap().wrap(wrapperHtml);
					}
					$el.addClass(elementClassName);
					if (label != null) label.insertAfter(element);
				}

				function getCurrentLabel(element, prevName, id)
				{
					//console.log("Searching for label id=", id, " closest=", element.closest('form').html(), "find=", element.closest('form').find("label[for='" + id + "']"));
					var label = null;
					element.closest('form').find("label").each(function() {
						var forId = this.getAttribute("for");
						//console.log("Comparing label forId=", forId, "prevName=", prevName, "id=", id);
						if (typeof forId != "undefined" && forId != null && forId!="" && (id==forId || prevName==forId)) {
							//console.log("Exists, returning");
							label = $(this);
							return false;
						}
					});
					return label;
				}

				function insertLabel(element, prevName, text)
				{
					if (element.length == 0) {
						console.warn("Element can not be empty");
					}

					//console.log(element.attr("placeholder"));

					//ak mame placeholder, tak setni ten, nevytvaraj label
					var placeholder = element.attr("placeholder");
					if (placeholder)
					{
						element.attr("placeholder", text);
						return;
					}

					var id = element.prop('id');
					var label = getCurrentLabel(element, prevName, id);
					//console.log("Current label: "+label);
					var hasPrevTd = element.parent('td').length > 0 && element.parent('td').prev('td').length > 0;
					var hasNextTd = element.parent('td').length > 0 && element.parent('td').next('td').length > 0;

					//console.log("label="+label+" id="+id);

					if (label == null) {
						label = $('<label>' + text + '</label>', {'for': id});

						if (hasPrevTd) {
							label.prependTo(element.parent('td').prev('td'));
						}
						else if (hasNextTd) {
							label.insertBefore(element);
							element.detach().appendTo(label.parent('td').next('td'));
							element.prev("br").remove();
						}
						else {
							//console.log("Inserting label before element", element);
							label.insertBefore(element);
						}
					}

					label.prop("for", id);
					//console.log("Setting label text to: "+text);
					label.text(text);
				}

				function CreateElement(tag, atrs)
				{
					var element = CKEDITOR.document.createElement(tag);

					$.each(atrs, function(k,v){
						if (k == "value") {
							element.setHtml(v);
						}
						else {
							element.setAttribute(k,v);
						}
					});

					return element;
				}
			}
			else if (dialogName == 'button')
			{
				var label = "<iwcm:text key="editor.form.btn.send_by_ajax" />";

				var infoTab = dialogDefinition.getContents( 'info' );
				infoTab.add( {
					type: 'checkbox',
					label: label,
					id: 'sendAjax'
				});

				var typeSelectBox = infoTab.get('type');
				if (typeSelectBox.items.length==3)
				{
					typeSelectBox.items[3] = ["<iwcm:text key="help.print_page"/>", "button"];
				}

				var onclickAjax = "return invokeWJAjax('formMailForm', 'ajaxFormResultContainer', 'bSubmit', '/FormMailAjax.action');";
				var onclickPrint = "window.print();"
				dialogDefinition.dialog.on('show', function(event)
				{
					var element = this.getSelectedElement();
					if (element != null && element.getAttribute('data-cke-pa-onclick') != null) {
						var hasOnclick = element.getAttribute('data-cke-pa-onclick').indexOf(onclickAjax) != -1;
						this.getContentElement("info", "sendAjax").setValue(hasOnclick);

						if (element.getAttribute('data-cke-pa-onclick').indexOf(onclickPrint) != -1)
						{
							var infoType = document.getElementById(this.getContentElement("info", "type")._.inputId);
							setTimeout(function() { infoType.selectedIndex = 3; }, 300);
						}
					}

					var dialog = dialogDefinition.dialog,
					nameElement = dialog.getContentElement('info', 'name'),
               typeElement = dialog.getContentElement('info', 'type'),
					textElement = dialog.getContentElement('info', 'value');

				   if (textElement.getValue() == '') {
						textElement.setValue('<iwcm:text key="editor.form.btn.submit"/>');
						typeElement.setValue("submit");
						nameElement.setValue("bSubmit");
				   }
				});

				dialogDefinition.onOk = CKEDITOR.tools.override(dialogDefinition.onOk, function(original) {
					return function()
					{
						//console.log("button on ok");

						var value = this.getContentElement("info", "sendAjax").getValue();
						var element = this.getSelectedElement();

						var onclick = onclickAjax;
						var typeSelectedIndex = document.getElementById(this.getContentElement("info", "type")._.inputId).selectedIndex;
						if (typeSelectedIndex==3)
						{
							onclick = onclickPrint;
							value = true;
							if (this.getContentElement('info', 'name').getValue()=="bSubmit") this.getContentElement('info', 'name').setValue("bPrint");
							if (this.getContentElement('info', 'value').getValue()=="<iwcm:text key="editor.form.btn.submit"/>") this.getContentElement('info', 'value').setValue("<iwcm:text key="help.print_page"/>");
						}

						if (value && element != null)
						{
							element.setAttribute('data-cke-pa-onclick', onclick);
						}
						else
						{
							if (element != null)
							{
								var onclickValue = element.getAttribute('data-cke-pa-onclick');

								if (onclickValue != null) {
									value = $.trim(onclickValue.replace(onclick, ''));

									if (value != "") {
										element.setAttribute('data-cke-pa-onclick', value);
									}
									else {
										element.removeAttribute('data-cke-pa-onclick');
									}
								}
							}
						}

						var editor = this.getParentEditor();
						element = this.button;
						var isInsertMode = !element;

						var fake = element ? CKEDITOR.htmlParser.fragment.fromHtml( element.getOuterHtml() ).children[ 0 ] : new CKEDITOR.htmlParser.element( 'input' );
						this.commitContent( fake );

						var writer = new CKEDITOR.htmlParser.basicWriter();
						fake.writeHtml( writer );
						var newElement = CKEDITOR.dom.element.createFromHtml( writer.getHtml(), editor.document );

						if ( isInsertMode )
							editor.insertElement( newElement );
						else {
							newElement.replace( element );
							editor.getSelection().selectElement( newElement );
						}

						if (value && newElement != null)
						{
							newElement.setAttribute('data-cke-pa-onclick', onclick);
						}

						newElement.setAttribute("class", "btn btn-primary")
					};
				});
			}
			else if ( dialogName == 'bulletedListStyle')
			{
				dialogDefinition.onOk = CKEDITOR.tools.override(dialogDefinition.onOk, function(original) {
					return function()
					{
						//console.log("List style DONE");

						var editor = this.getParentEditor();

						var element = null; // this.getListElement( editor, 'ul' );
						var range;
						try {
							range = editor.getSelection().getRanges()[ 0 ];

							range.shrink( CKEDITOR.SHRINK_TEXT );
							element = editor.elementPath( range.getCommonAncestor() ).contains( 'ul', 1 );

							element && this.commitContent( element );

							//console.log(element);
						} catch ( e ) {

						}

						if (element != null)
						{
							//nastav class podla zvoleneho typu, aby sa dalo lepsie stylovat
							element.removeClass("circle");
							element.removeClass("disc");
							element.removeClass("square");
							var type = this.getContentElement("info", "type").getValue();
							if (type != "") element.addClass(type);
						}

						return original.call(this);
					};
				});
			}

			/*
				//toto uz netreba, bolo potrebne len pre iframe verziu
				dialogDefinition.dialog.on('show', function(event) {
					expandIframeOnShow();
				});

				dialogDefinition.dialog.on('hide', function(event) {
					compressIframeOnHide();
				});

				dialogDefinition.dialog.on('close', function(event) {
					compressIframeOnHide();
				});

				if (typeof dialogDefinition.onOk != "undefined") {
					dialogDefinition.onOk = CKEDITOR.tools.override(dialogDefinition.onOk, function(original) {
						return function(){
							//console.log('onOk');
							compressIframeOnHide();
							return original.call(this);
						}
					});
				}

				if (typeof dialogDefinition.onHide != "undefined") {
					dialogDefinition.onHide = CKEDITOR.tools.override(dialogDefinition.onHide, function(original) {
						return function(){
							//console.log('onHide');
							compressIframeOnHide();
							return original.call(this);
						}
					});
				}

				if (typeof dialogDefinition.onCancel != "undefined") {
					dialogDefinition.onCancel = CKEDITOR.tools.override(dialogDefinition.onCancel, function(original) {
						return function(){
							//console.log('onCancel');
							compressIframeOnHide();
							return original.call(this);
						}
					});
				}
			*/
		});
	}
	ckEditorAtLeastOneInitialized = true;

	var expanded = false;
	var zIndex;
	function expandIframeOnShow() {
		if (expanded) {
			return;
		}

		var iframe = window.parent.$('#webjetEditorIframe');
		iframe.after('<div class="webjetEditorIframePlaceholder"></div>');

		if (iframe.length > 0) {

			var parent = iframe.offsetParent();
			var offset = iframe.offset();

			if (parent.length == 0) {
				offset = parent.offset();
			}

			var placeholderCss = {
				position: 'static',
				height: iframe.height(),
				width: iframe.width()
			}

			var css = {
				position: 'absolute',
				height: $(window.parent.document).height(),
				width: $(window.parent.window).width(),
				zIndex: 1000
			};

			var bodyCss = {
				height: iframe.height(),
				width: iframe.width()
			}

			if (offset.top > 0) {
				css.marginTop = "-" + offset.top + "px";
				bodyCss.marginTop = offset.top + "px";
				//bodyCss.height += offset.top;
			}

			if (offset.left > 0) {
				css.marginLeft = "-" + offset.left + "px";
				bodyCss.marginLeft = offset.left + "px";
			}

			/*
			console.log('css');
			console.log(css);
			console.log('bodyCss');
			console.log(bodyCss);
			console.log('placeholderCss');
			console.log(placeholderCss);
			console.log('z-index: ' + zIndex);
			*/
			zIndex = window.parent.$('#inlineEditorToolbarTop').css('z-index');

			iframe.css(css);
			iframe.contents().find('#editorFormId').css(bodyCss);
			iframe.contents().find('#editorFormId .tab-content').css('height', bodyCss.height - 30);
			window.parent.$('.webjetEditorIframePlaceholder').css(placeholderCss);
			window.parent.$('#inlineEditorToolbarTop').css('z-index', 1000);

			expanded = true;
		}
	}

	function compressIframeOnHide() {
		if (!expanded) {
			return;
		}

		var iframe = window.parent.$('#webjetEditorIframe');

		if (iframe.length > 0) {
			iframe.removeAttr('style');

			var body = iframe.contents().find('#editorFormId');
			if (body.length > 0) {
				body.removeAttr('style');
			}

			window.parent.$('.webjetEditorIframePlaceholder').remove();
		}

		iframe.contents().find('#editorFormId .tab-content').css('height', 'auto');
		window.parent.$('#inlineEditorToolbarTop').css('z-index', zIndex);
		expanded = false;
	}

	CKEDITOR.dtd.$removeEmpty['i'] = false;
	CKEDITOR.dtd.$removeEmpty['span'] = false;
    CKEDITOR.dtd.$removeEmpty['div'] = false;
    CKEDITOR.dtd.$removeEmpty['section'] = false;
	CKEDITOR.dtd.a.div = 1;
	CKEDITOR.dtd.a.h1 = 1;
	CKEDITOR.dtd.a.h2 = 1;
	CKEDITOR.dtd.a.h3 = 1;
	CKEDITOR.dtd.a.h4 = 1;
	CKEDITOR.dtd.a.h5 = 1;
	CKEDITOR.dtd.a.h6 = 1;
	CKEDITOR.dtd.a.ul = 1;

	//window.alert(editorElem.clientHeight);

	//sem nam to nastavi editor.jsp pre popup okno
	var webjetContentsCss = window.webjetContentsCss;
	if (webjetContentsCss == undefined)
	{
		webjetContentsCss = ['/css/page.css', '/admin/skins/webjet8/ckeditor/dist/plugins/webjetcomponents/samples/contents.css']
	}

	var ckEditorInstanceInitialized = ckEditorInitFunction(ckEditorElementId, {

			uploadUrl: '<iwcm:cp/>/admin/web-pages/upload/?__sfu=0&uploadType=ckeditor',

			contentsCss: webjetContentsCss,

			allowedContent: true,

			floatSpacePinnedOffsetY: 50,

			customConfig: configLink,

			language: "<%=sk.iway.iwcm.i18n.Prop.getLngForJavascript(request)%>",

			on :
			{
	         // maximize the editor on startup
	         'instanceReady' : function( evt )
	         {
		         FCKeditor_OnComplete();
	         },

	         'getData' : function(e)
	         {
	        	   var data = e.editor.getData(true);
	        	   data = data.replace(/<article>/gi, '');
	        	   data = data.replace(/<\/article>/gi, '');
	        	   data = data.replace(/&lt;article&gt;/gi, '');
	        	   data = data.replace(/&lt;\/article&gt;/gi, '');
	        	   e.data.dataValue = data;
	        	},

	        	'setData' : function(e)
	         {
	        	   var data = e.data.dataValue;
	        	   data = data.replace(/(!INCLUDE\((.*?)\)!)/gi, '<article>$1</article>');
	        	   e.data.dataValue = data;
	        	},

				afterPasteFromWord: function( evt ) {
					var filter = new CKEDITOR.filter({
						$1: {
							// Use the ability to specify elements as an object.
							elements: CKEDITOR.dtd,
							attributes: true,
							styles: false,
							classes: true
						}
					}),
					fragment = CKEDITOR.htmlParser.fragment.fromHtml( evt.data.dataValue ),
					writer = new CKEDITOR.htmlParser.basicWriter();

					//console.log("html1=", evt.data.dataValue);
					//console.log("fragment 2: ", fragment);

					//filter.allow("*{*}");

					filter.disallow( 'table[width]' );
					filter.disallow( 'table[height]' );
					filter.disallow( 'table[border]' );
					filter.disallow( 'span' );
					filter.disabled = false;

					filter.addTransformations([
						[
							{
								element: 'table',
								left: function( el ) {
									return true;
								},
								right: function( el, tools ) {
									el.classes = ["table", "table-sm", "tabulkaStandard"];
								}
							}
						]
					]);

					//console.log("filter=", filter);

					filter.applyTo( fragment );
					fragment.writeHtml( writer );

					var html = writer.getHtml();
					html = html.replace(/<table/gi, "<div class=\"table-responsive\"><table style=\"width: 100%\" ");
					html = html.replace(/<\/table>/gi, "</table></div>");

					html = html.replace(/<b>/gi, "<strong>");
					html = html.replace(/<\/b>/gi, "</strong>");

					html = html.replace(/<i>/gi, "<em>");
					html = html.replace(/<\/i>/gi, "</em>");

					evt.data.dataValue = html;

					//console.log("html2=", evt.data.dataValue);
				}

	      }
	   });

	 ckEditorInstance = ckEditorInstanceInitialized;

	 ckEditorInstanceInitialized.on('afterPaste', function(e) {
		//console.log("After paste, e=", e);
		if (typeof window.bootstrapVersion != "undefined" && window.bootstrapVersion.indexOf("3")!=0) {
			//najdi tabulky a wrapni ich do table-responsive
			$(CKEDITOR.currentInstance.document.$).find("table.tabulkaStandard").each(function() {
				var $table = $(this);
					if ($table.hasClass("tabulkaStandard")) {
					//console.log("table=", this);
					$table.attr("class", "table table-sm tabulkaStandard");
					//aby to bolo rovnake ako standardne vytvorenie tabulky
					$table.attr("border", "1");

					var parent = $table.parent("div.table-responsive");
					if (parent.length == 0)	$table.wrap('<div class="table-responsive"></div>');
				}

			});
		}
	 });
	 ckEditorInstanceInitialized.on('afterCommandExec', function(e) {
		if (typeof window.bootstrapVersion != "undefined" && window.bootstrapVersion.indexOf("3")!=0) {
			//console.log("afterCommandExec, e=", e.data.name);
			if ("tableDelete"==e.data.name || "deleteTable"==e.data.name) {
				//odstran prazdne table-responsive div elementy
				$(CKEDITOR.currentInstance.document.$).find("div.table-responsive:empty").remove();
			}
		}
	 });

    ckEditorInstanceInitialized.on('fileUploadRequest', function(e)
	{
   		var fileLoader = e.data.fileLoader;
   		var formData = new FormData();
   		var xhr = fileLoader.xhr;

   		fileLoader.uploadUrl = "<iwcm:cp/>/admin/web-pages/upload/?__sfu=0&uploadType=ckeditor&groupId="+e.editor.element.$.form.groupId.value+"&docId="+e.editor.element.$.form.docId.value;

   		fileLoader.xhr.open( 'POST', fileLoader.uploadUrl, true );
   		formData.append( 'uploadFile', fileLoader.file, fileLoader.fileName );
   		fileLoader.xhr.send( formData );

   		e.stop();
   	}, null, null, 4);

    ckEditorInstanceInitialized.on("focus", function(e){
        //console.log("FOCUS");
        //console.log(e);
        ckEditorInstance = e.editor;
    });

	// image alignment classes
	CKEDITOR.on('instanceReady', function()
	{
		var commands = ['justifyleft', 'justifyright'];

		$.each(commands, function(i, v){
			var command = ckEditorInstance.getCommand(v);
            if (typeof command !== 'undefined') {
                command.exec = CKEDITOR.tools.override(command.exec, function (original) {
                    return function () {
                        var element = this.editor.getSelection().getSelectedElement();
                        if (element != null && element.$.tagName == "IMG") {
                            var wasAligned = hasAlign($(element.$), v.replace('justify', ''));
                            var hadClass = $(element.$).hasClass(imageAlignClasses[i]);

                            $(element.$).removeClass(imageAlignClasses.join(" "));

                            if (!hadClass && !wasAligned) {
                                $(element.$).addClass(imageAlignClasses[i]);
                            }

                            if (i == 0) $(element.$).css("float", "left");
                            if (i == 1) $(element.$).css("float", "right");

                            //odskrtnutie zarovnania - zrusime float atribut
                            if (wasAligned) $(element.$).css("float", "");

                            //console.log($(element.$));
                            //console.log("i="+i+ " " + $(element.$).css("float")+" wasAligned="+wasAligned);

                            return;
                        }

                        return original.call(this);
                    }
                });
            }
		});

		if (sessionCssParsed != null)
		{
			setStylesDef(sessionCssParsed, ckEditorInstance);
		}
	});

	return ckEditorInstanceInitialized;
}

function hasAlign(element, side) {
	var style = $.trim(element.attr('style'));
	var styles = {};

	if (typeof style == "undefined" || style == null) {
		return false;
	}

	while(style.indexOf(": ") != -1) {
		style = style.replace(": ", ":");
	}

	$.each(style.split(" "), function(i, v) {
		var propertyValue = $.trim(v.replace(";", "")).split(":");
		styles[propertyValue[0]] = propertyValue[1];
	});

	return (typeof styles.float != "undefined") ? styles.float == side : false;
}


function fillKategoriaHeureka()
{
	document.getElementById("fieldQId").value=document.getElementById("kategoriaHeurekaId").value;
}

function reloadWebpagesTree()
{
	try { $('#groupslist-jstree').jstree("refresh"); } catch (e) {}
	try { $('#SomStromcek').jstree(true).refresh(); } catch(e) {}
}

var editorLastResetDirty = null;
function resetDirtyEditor()
{
	try
	{
		if (ckEditorInstance != null)
		{
			//console.log("Reseting dirty");
			ckEditorInstance.resetDirty();
			editorLastResetDirty = new Date().getTime();
		}
	}
	catch (e) { console.log(e); }
}

function checkDirtyEditor()
{
	try
	{
		var now = new Date().getTime();
		var timeDiff = now - editorLastResetDirty;
		//console.log("now="+now+" editorLastResetDirty="+editorLastResetDirty+" rozdiel="+timeDiff);
		if (editorLastResetDirty == null || (timeDiff < 2000))
		{
			return true;
		}

		if (ckEditorInstance != null)
		{
			//console.log("checkDirtyEditor="+ckEditorInstance.checkDirty());
			if (ckEditorInstance.checkDirty())
			{
				if (window.confirm("<iwcm:text key="editor.confirmExitMessage"/>"))
				{
					resetDirtyEditor();
				}
				else
				{
					return false;
				}
			}
		}
	}
	catch (e) { console.log(e); }
	return true;
}

function setDefaultDocId(groupId, docId)
{
    var postData = { id: groupId+"_"+docId };
    //takto sa nam to poslalo z kontextoveho menu kde priamo v groupId je groupId_docId ako ID nodu
    if (""==docId || docId==null) postData = { id: groupId };

    $.ajax({
        type: "POST",
        url: "/admin/webpages/ajax_jstreeoperations.jsp?act=setAsDefaultPage",
        data: postData,
        success: function(response)
        {
            showErrorOrCallback(response, function(response){
                loadGroupRefreshed(response.refreshGroupId);
            });
        }
    });
}

/*
function isGdprValidFormName(formName)
{
	var input = {hasUserApproved:'true',formName:formName};
	$.ajax({
		type: "POST",
		url: "/components/form/admin_gdpr_check-ajax.jsp",
		data: input,
		success: function(data){
			data = $.trim(data);
			if(data.indexOf('true') == 0)
			{
				return true;
			}
			else
			{

				if(confirm('<%=prop2.getText("components.forms.alert.gdpr")%>'))
				{

				}
				else
				{

					setTimeout(function(){
					dialogDefinition.dialog.hide()
					}, 1);
				}
			}
		}
	});
}

function validateFormName(formName)
{
	$.post( "/components/form/admin_gdpr_check-ajax.jsp?addUserApprove=true&formName="+formName, function( data ) {
		data = $.trim(data);
		if(data.indexOf('true') == 0)
		{
			return true;
		}
	});
	return false;
}*/
