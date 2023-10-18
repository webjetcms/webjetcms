/*
<%@ page pageEncoding="utf-8" contentType="text/javascript" %><%@page import="sk.iway.iwcm.i18n.Prop"%><%@page import="java.util.ArrayList"%><%@page import="sk.iway.iwcm.form.FormDB"%><%@page import="sk.iway.iwcm.doc.AtrDB"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");

sk.iway.iwcm.PathFilter.setStaticContentHeaders("/admin/ckeditor/webpages.js", null, request, response);%><%@
page import="sk.iway.iwcm.*" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true"/><%
    Prop prop2 = Prop.getInstance(request);

    int maxSize = Constants.getInt("webpagesTreePagesMaxSize");
%>
*/

var lastGroupId = <%=Constants.getInt("rootGroupId") %>;
function setLastGroupId(groupId)
{
	//console.log("setLastGroupId, groupId="+groupId);
	lastGroupId = groupId;
	try
	{
		$("#searchFormGroupId").val(groupId);
		$("#groupIdInputBox").val(groupId);
	}
	catch (e) {}
}

var openedDirectories = [];
function hidePagesMaxSize(maxSize, data) {
	var id = data.node.id;
    //console.log('openedDirectories');
    //console.log(openedDirectories);

	if ($.inArray(id, openedDirectories) == -1) {
		var list = data.node.children;

		list = $.grep(list, function(v, i){
			return v.indexOf("_") != -1;
		});

        //console.log(list);

		if (list.length > maxSize) {
			var counter = 0,
			   half = Math.ceil(maxSize / 2);

			$.each(list, function(i, v) {
				if (i < half || i >= list.length - half) {
					return true;
				}

				$("#" + v).hide().addClass("hiddenLi");
                //console.log($("#" + v))
				counter++;
			});

			$('#' + data.node.id).find('li.hiddenLi:last').after('<li class="expand"><a class="btn btn-primary" href="javascript:void(\'Expand\')"><%= prop2.getText("formslist.showAll") %> (' + counter + ')</a></li>');
			$('.jstree-container-ul').on('click', '.expand', function(){
				var el = $(this);
				el.siblings('.hiddenLi').fadeIn(function(){
					$(this).removeClass('hiddenLi').show();
				});
				el.remove();
			});
		}
		openedDirectories.push(id);
	}
}

function fixSlimScrollScrollers()
{
	try
	{
        if ($("#treeScrollDiv").size()<1) return;

		$("#treeScrollDiv").mouseenter();
		setTimeout(function()
		{
			$("#treeScrollDiv").slimScroll({ scrollByY: '0px', width: $("#treeScrollDiv").width() });
		}, 500);
	}
	catch (e) {}
}

var jsTreeInitialized = false;


var groupslistTable = function () {
	var groupsListTableData = [];
    var initPickers = function () {
        //init date pickers
        /*$('.date-picker').datepicker({
            rtl: Metronic.isRTL(),
            autoclose: true
        });
        */
    }

    var grid = null;

	 function loadGroupsListTableData(groupId)
	 {
	    $.ajax({
			  type: "POST",
		<%

		if("news".equals(Tools.getRequestParameter(request, "type"))){
			out.print("url: \"/components/news/ajax_docstable.jsp?newsListConstantName="+Tools.getStringValue(Tools.getRequestParameter(request, "newsListConstantName"),"")+"&id=\"+groupId,");
		}else{
			out.print("url: \"ajax_docstable.jsp?id=\"+groupId,");
		}
		%>

			  success: function(data)
			  {
			     //console.log("loadGroupsListTableData data.data="+data.data);
			     //console.log(data.data);
			     //console.log(groupsListTableData);

			     groupsListTableData = data.data;
			     handleRecords(groupId, groupsListTableData);

			     if (typeof data.parents != "undefined")
			     {
			    	var pathForTree = data.parents.split(",");
					if (pathForTree.length > 0 && jsTreeInitialized==true)
					{
						//console.log("Data loaded, opening treem path=")
						//console.log(pathForTree);

						for(var i=0; i < pathForTree.length; i++) { pathForTree[i] = +pathForTree[i]; }
						openNodes = pathForTree;
						openNodeCounter = 0;
						openNextNode();
					}
				  }

				  if (typeof data.domainName != "undefined")
				  {
				     $("#actualDomainNameSelectId").val(data.domainName);
				  }
			  }
		 });
	 }

    var handleRecords = function (groupId, tableData)
    {
    	  setLastGroupId(groupId);

    	  groupsListTableData = tableData;

		  if (grid != null)
		  {

		     grid.getDataTable().clear();

		     var DT = grid.getDataTable();
		     for (var i=0; i < groupsListTableData.length; i++)
		     {

		        DT.row.add(groupsListTableData[i]);
		     }

		     grid.getDataTable().draw();

		     return;
		  }

        grid = new Datatable();

		var isNewsEditing = window.location.href.indexOf("admin_news_list.jsp")!=-1;

        grid.init({
            src: $("#groupslistTableAjax"),
            onSuccess: function (grid)
            {
                // execute some code after table records loaded
                setTimeout(function() {
                	fixSlimScrollScrollers();
                }, 300);
            },
            onError: function (grid) {
                // execute some code on network or other general error
            },
            onDataLoad: function(grid) {
                // execute some code on ajax data load
            },
            loadingMessage: 'Loading...',
            dataTable: { // here you can define a typical datatable settings from http://datatables.net/usage/options
                dom: datatables_globalConfig.dom,
                "bStateSave": true,
			       "stateSaveParams": function (settings, data) {
			        	data = datatableClearSaveParams(settings, data);
			       },

                language: {
		                "url": "/admin/skins/webjet8/assets/global/plugins/datatables/i18n/<%=sk.iway.iwcm.i18n.Prop.getLngForJavascript(request)%>.json"
		          },

                columns: [
                	{//1
                		data: "docId",
                		sortable: false,
                		render: function ( data, type, full, meta ) {
					         return '<input type="checkbox" name="docId" class="group-checkable-in-td" value="'+data+'"/>';
					    }
					},
                	{//2
                		data: "sortPriority",
                		sortable: true,
                		render: function ( data, type, full, meta ) {
					         return data+'.';
					      }
                	},
                 	{//3
                 		data: "docId",
                 		sortable: false,
                 		render: function ( data, type, full, meta )
                 		{
							if (isNewsEditing)
							{
							var tools = '<a href="/showdoc.do?docid='+data+'" target="_blank" title="<iwcm:text key="components.qa.show_page"/>"><span class="glyphicon glyphicon-orange glyphicon-eye-open" aria-hidden="true"></span></a>';
							tools += '   <a href="javascript:editWebPage('+data+', -1, -1)" title="<iwcm:text key="components.wiki.editPage"/>"><span class="glyphicon glyphicon-pencil" aria-hidden="true"></span></a>';
							tools += '   <a href="javascript:void(0)" onclick="jstreeDeleteDocByDocId('+full.groupId+', '+data+')" title="<iwcm:text key="components.user.perms.deletePage"/>"><span class="glyphicon glyphicon-remove" aria-hidden="true"></span></a>';
							return tools;
							}
							else
							{
							   var title = full.title;
								//tools
								var tools = '<a href="/showdoc.do?docid='+data+'" target="_blank"><span class="glyphicon glyphicon-eye-open" aria-hidden="true" title="<iwcm:text key="components.qa.show_page"/>"></span></a>';
								tools += '<div class="task-config webSiteSettings"><div class="task-config-btn btn-group"><a class="btn btn-xs default" href="#" data-toggle="dropdown" data-hover="dropdown" data-close-others="true"><i class="fa fa-cog"></i><i class="fa fa-angle-down"></i></a><ul class="dropdown-menu pull-left" style="min-width: 160px;">';
								tools += '   <li><a href="/apps/stat/admin/top-details/?docId='+data+'&title='+title+'" title="<iwcm:text key="menu.stat"/>"><i class="fa fa-bar-chart-o"></i></a></li>';
								tools += '   <li><a href="javascript:editWebPage('+data+', -1, -1)" title="<iwcm:text key="components.wiki.editPage"/>"><i class="fa fa-file-text"></i></a></li>';
								tools += '   <li><a href="javascript:void(0)" onclick="window.open(\'/admin/dochistory.jsp?docid='+data+'\',\''+title+'\',\'width=600,height=400\')"  title="<iwcm:text key="components.data.deleting.documentHistory"/>"><i class="fa fa-clock-o"></i></a></li>';
								tools += '   <li><a href="javascript:void(0)" onclick="jstreeDeleteDocByDocId('+full.groupId+', '+data+')" title="<iwcm:text key="components.user.perms.deletePage"/>"><i class="fa fa-trash-o"></i></a></li>';
								tools += '</ul></div></div>';
								  return tools;
							}
						}
                 	},

						{//4
                		data: "title",
                		sortable: true,
                		render: function ( data, type, full, meta ) {
					         var html = "";
							if (isNewsEditing && full.perexImage!=null && full.perexImage.indexOf("/images")==0)
							{
								html+= "<img src=\"/thumb"+full.perexImage+"\" class='pull-left' style='margin-right: 6px; width: 96px;'/>"
							}

					         if (full.defaultDoc == true) html += '<strong><span class="glyphicon glyphicon-certificate" aria-hidden="true" title="<iwcm:text key="editor.main_site"/>"></span>';

					         var classes = 'onHoverShowMenu';

					         if (full.properties != null && !full.properties.isAvailable) {
					         	classes += ' notAvailable';
					         }

					         html += '<a class="' + classes + '" href="javascript:editWebPage('+full.docId+', -1, -1)" title="<iwcm:text key="components.wiki.editPage"/>">'+data+'</a>';
					         if (full.defaultDoc == true) html += "</strong>";

							if (isNewsEditing)
							{
								html += "<br/><p>"+full.perex+"</p>";
							}

					         return html;
						}
					},

					{///5
                		data: "properties",
                		sortable: false,
                		render: function ( data, type, full, meta ) {

                			var data = "";

                			if (full.properties != null) {
                				if (full.properties.isAvailable == false) {
                					data += ' <img class="vlastnostiImg" src="/admin/skins/webjet8/assets/global/img/icon-nezobrazovat.png" title="<iwcm:text key="components.news.img_none"/>"/> ';
                				}
                			}

                			if (full.properties != null) {
                				if (full.properties.isShowInMenu == false) {
                					data += "  <img class=\"vlastnostiImg\" src=\"/admin/skins/webjet8/assets/global/img/icon-menu.png\" title=\"<iwcm:text key="editor.show_in_menu_disabled"/>\"/> ";
                				}
                			}

                			if (full.properties != null) {
                				if (full.properties.isCacheable == true) {
                					data += ' <img class="vlastnostiImg" src="/admin/skins/webjet8/assets/global/img/icon-cache.png" title="<iwcm:text key="editor.cache"/>"/> ';
                				}
                			}

                			if (full.properties != null) {
                				if (full.properties.isSearchable == false) {
                					data += ' <img class="vlastnostiImg" src="/admin/skins/webjet8/assets/global/img/icon-neprehladavatelne.png" title="<iwcm:text key="editor.groupslist.neprehladavatelne"/>"/> ';
                				}
                			}


                			if (full.properties != null) {
                				if (full.properties.isRequireSsl == true) {
                					data += ' <img class="vlastnostiImg" src="/admin/skins/webjet8/assets/global/img/icon-ssl.png" title="<iwcm:text key="editor.requireSsl"/>"/> ';
                				}
                			}

								if (full.properties != null) {
                				if (full.properties.isForum == 1) {
                					data += ' <a href="javascript:popup(\'/components/forum/admin_forum_open.jsp?docid='+full.docId+'\', 600, 530)" title="<iwcm:text key="groupslist.open_forum"/>" class="iconForum">&nbsp;</a> ';
                				}
                				else if (full.properties.isForum == -1) {
                					data += ' <a href="javascript:popup(\'/components/forum/admin_forum_open.jsp?docid='+full.docId+'\', 600, 530)"><img src="/admin/skins/webjet6/images/icon_forum_disabled.gif" border="0" align="absbottom" title="<iwcm:text key="groupslist.open_forum"/>"></a>';
                				}
                			}


					         return data;
					    }
                	},

                	{//6
                	    data: "authorName", sortable: true,
                		render: function ( data, type, full, meta ) {
					         return data;
						}
                	},
                	{ //7
                	    data: { _ : "dateCreated", sort: "dateCreatedTimestamp" }, sortable: true
                	},
                	{//8
                	    data: { _ : "publicStartWithTime", sort: "publicStartTimestamp" }, sortable: true
                	},
                	{ //9
                	    data: { _ : "publicEndWithTime", sort: "publicEndTimestamp" }, sortable: true
                	},
                	{ //10
                	    data: { _ : "publicEventWithTime", sort: "publicEventTimestamp" }, sortable: true
                	},
                	{//11
                		data: "perexGroup",
                		sortable: true,
                		render: function ( data, type, full, meta ) {
					         return data;
					    }
                	},
                	{//12
                		data: "perex",
                		sortable: true,
                		render: function ( data, type, full, meta ) {
					         return data;
					    }
                	},
                	{//13
                		data: "perexPlace",
                		sortable: true,
                		render: function ( data, type, full, meta ) {
					         return data;
					    }
                	},
                	{//14
                		data: "perexImage",
                		sortable: true,
                		render: function ( data, type, full, meta ) {
							return data;
					    }
                	},
                	{//15
                		data: "perexPasswordProtected",
                		sortable: true,
                		render: function ( data, type, full, meta ) {
					         return data;
					    }
                	},
                	{//16
                	data: "templateName", sortable: true,
                		render: function ( data, type, full, meta ) {
					         return data;
						}
					},

                	{//17
                		data: "link",
                		sortable: false,
                		render: function ( data, type, full, meta ) {
					         return data;
					    }
                	},
                	{//18
                		data: "externalLink",
                		sortable: false,
                		render: function ( data, type, full, meta ) {
					         return data;
					    }
                	},
                	{//19
                		data: "fieldA", sortable: false, render: function ( data, type, full, meta ) {   return data;  }
                	},
                	{//20
                		data: "fieldB", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//21
                		data: "fieldC", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//22
                		data: "fieldD", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//23
                		data: "fieldE", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//24
                		data: "fieldF", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//25
                		data: "fieldG", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//26
                		data: "fieldH", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//27
                		data: "fieldI", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//28
                		data: "fieldJ", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//29
                		data: "fieldK", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//30
                		data: "fieldL", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//31
                		data: "fieldM", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//32
                		data: "fieldN", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//33
                		data: "fieldO", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//34
                		data: "fieldP", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//35
                		data: "fieldQ", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//36
                		data: "fieldR", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//37
                		data: "fieldS", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//38
                		data: "fieldT", sortable: false, render: function ( data, type, full, meta ) {  return data;  }
                	},
                	{//39
                		data: "empty", sortable: false, render: function ( data, type, full, meta ) { return data;  }
                	},
                	{//40
                		data: "title",
                		sortable: false,
                		render: function ( data, type, full, meta ) {
					         var html = "";
					         html += '<div class="divFixedNazov">';

					         if (full.defaultDoc == true) html += '<strong><span class="glyphicon glyphicon-certificate" aria-hidden="true" title="<iwcm:text key="editor.main_site"/>"></span>';

					         html += '<a href="javascript:editWebPage('+full.docId+', -1, -1)" title="'+data+'">'+data+'</a></div>';
					         return html;
					         }
					},

					{ //41
					data: "properties",
					sortable: true,
						render: function ( data, type, full, meta ){
					if(data.isAvailable == true){
							var html = '<i class="fa fa-check-circle availableLabel" aria-hidden="true"> <span>true</span></i>';
					}else{
						var html = '<i class="fa fa-ban notAvailable availableLabel" aria-hidden="true"> <span>false</span> </i>';

					}
						return html;
						}
					},
					{ //42
						data: "subcategory",
						sortable: true,
						render: function ( data, type, full, meta ) {
							return data;
						}
					},
					{ //43
					data: "category",
					sortable: true,
					render: function ( data, type, full, meta ) {
					return data;
					}
					},
					{ // 44
					data: "lang",
					sortable: true,
					render: function ( data, type, full, meta ) {
					return data;
					}
					},
					{ //45
					data: "properties",
					sortable: true,
						render: function ( data, type, full, meta ) {
						if(data.isShowOnHomepage == true){
							var html = '<i class="fa fa-check-circle showOnHomepage" aria-hidden="true"> <span></span></i>';
						}else{
							var html = '<i class="fa fa-ban notAvailable showOnHomepage" aria-hidden="true"> <span></span> </i>';

						}
						return html;
						}
					},
					{ // 46
					data: "domain",
					sortable: true,
					render: function ( data, type, full, meta ) {
					return data;
					}
					}
                ],

                "lengthMenu": [
                    [10, 20, 50, 100, 150, -1],
                    [10, 20, 50, 100, 150, "All"] // change per page values here
                ],
                "paging": true,
                "pageLength": 20,
                "info": true,
                "orderCellsTop": true,
                "order": [[ 1, "asc" ]],
                "ajax": null,
                "data": groupsListTableData,
                "serverSide": false,
                "searching": true,
                "fnDrawCallback": function( oSettings ) {

			   	 		$(".divFixedNazov").each(function()
			   	 		{
			   	 			tableTdheight = $(this).parents("td").height();
				   	 		$(this).height(tableTdheight);
			   	 		});

			   	 		$('input.group-checkable').prop('checked', false).uniform().trigger('change');
			   	 		$("input.group-checkable-in-td").prop('checked', false).uniform();
			   	 		checkAll();

			   	 		$(".divFixedNazov_nadpis").show();
			    },
			    "fnInitComplete": function(oSettings, json) {},
				 "columnDefs": [
				            {
				                "targets": [ 1, 2, 3, 4,7 ],
				                "visible": true
				            },
				            {
				                "targets": [ 0, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 39, 40 ],
				                "visible": false,
				                "searchable": true
				            }
				        ],
                "bFilter": true,
                buttons: datatables_globalConfig.buttons//buttons
                /*
                ,
                "order": [
                    [1, "asc"]
                ]// set first column as a default sort by asc
                */

            }

        });

		$("#dtNewsForm").on( 'submit', function () {

			grid.getDataTable().column(3).search( $("#dtFilterTitleClick").val() );
			grid.getDataTable().column(5).search( $("#dtFilterAuthorNameClick").val() );
			grid.getDataTable().column(43).search( $("#dtFilterLangClick").val() );
			grid.getDataTable().column(40).search( $("#dtFilterAvailableClick").val() );
			grid.getDataTable().column(41).search( $("#dtFilterGroupNameClick").val() );
			grid.getDataTable().column(42).search( $("#dtFilterCategoryClick").val() );
			grid.getDataTable().column(45).search( $("#dtFilterDomainClick").val() );

			//datumy
			var d = $("#dtFilterPublishStartMinClick").val().split('.');
			if (d.length == 3) minPublishDateFilter = new Date(d[2]+"-"+d[1]+"-"+d[0]).getTime();
			else minPublishDateFilter = "";

			d = $("#dtFilterPublishStartMaxClick").val().split('.');
			if (d.length == 3) maxPublishDateFilter = new Date(d[2]+"-"+d[1]+"-"+d[0]).getTime();
			else maxPublishDateFilter = "";

			grid.getDataTable().draw();

			return false;
		});


        $("#dtFilterTitle").on( 'keyup change', function () { grid.getDataTable().column(3).search( this.value ).draw(); });

		var minPublishDateFilter ="";
		var maxPublishDateFilter ="";

		var minPublishEndDateFilter ="";
		var maxPublishEndDateFilter ="";

		var otherGroups = [];
		$(document).ready(function() {

		    function showHideColumns() {
                var table = grid.getDataTable();
                var TableWidth;

                if ($("input.checkbox_check:checked").length > 0) {
                    var TableWidth = 800;
                }
                else {
                    var TableWidth = 100;
                }

                if ($('input.checkbox_check.showProperties').is(':checked')) {
                    var column = table.columns([4]);
                    column.visible(true, true);
                } else {
                    var column = table.columns([4]);
                    column.visible(false, true);

                }
                if ($('input.checkbox_check.showLangVersion').is(':checked')) {
                    var column = table.columns([43]);
                    column.visible(true, true);
                    $("#dtFilterLang").on('keyup change', function () {
                        grid.getDataTable().column(43).search(this.value).draw();
                    });
                } else {
                    var column = table.columns([43]);
                    column.visible(false, true);
                }

                if ($('input.checkbox_check.showSubcategory').is(':checked')) {
                    var column = table.columns([41]);
                    column.visible(true, true);

                    $("#dtFilterGroupName").on('keyup change', function () {
                        grid.getDataTable().column(41).search(this.value).draw();
                    });

                } else {
                    var column = table.columns([41]);
                    column.visible(false, true);
                }

                if ($('input.checkbox_check.showCategory').is(':checked')) {

                    var column = table.columns([42]);
                    column.visible(true, true);
                                $("#dtFilterCategory").on('keyup change', function () {
                        grid.getDataTable().column(42).search(this.value).draw();
                    });
                } else {
                    var column = table.columns([42]);
                    column.visible(false, true);
                }


                if ($('input.checkbox_check.showDomain').is(':checked')) {
                    var column = table.columns([45]);
                    column.visible(true, true);
                    $("#dtFilterDomain").on('keyup change', function () {
                        grid.getDataTable().column(45).search(this.value).draw();
                    });
                } else {
                    var column = table.columns([45]);
                    column.visible(false, true);
                }


                if ($('input.checkbox_check.showAvailable').is(':checked')) {
                    var column = table.columns([40]);
                    column.visible(true, true);
                    var column = table.columns([38]);
                    column.visible(false, true);

                    $("#dtFilterAvailable").on('keyup change', function () {
                        grid.getDataTable().column(40).search(this.value).draw();
                    });
                } else {
                    var column = table.columns([40]);
                    column.visible(false, true);
                }


                if ($('input.checkbox_check.publikovanie').is(':checked')) {
                    var column = table.columns([5, 6, 7, 8, 9, 10]);
                    column.visible(true, true);
                    var TableWidthPublikovanie = 860;
                    $("#dtFilterAuthorName").on( 'keyup change', function () { grid.getDataTable().column(5).search( this.value ).draw(); });
                    $("#dtFilterLastChange").on( 'keyup change', function () { grid.getDataTable().column(6).search( this.value ).draw(); });

                }else {
                    var column = table.columns([5, 6, 7, 8, 9, 10]);
                    column.visible(false, true);
                    var TableWidthPublikovanie = 0;
                }

                if ($('input.checkbox_check.publikovanie').is(':checked')) {

                    //console.log("TADA");

                    $("#dtFilterPublishStartMin").on('keyup change', function () {
                        var d = this.value.split('.');

                        if (this.value != "") {
                            $(this).addClass('expand');
                            $("#dtFilterPublishStartMax").addClass('expand');
                        } else {
                            if ($("#dtFilterPublishStartMax").val() == "") {
                                $(this).removeClass('expand');
                                $("#dtFilterPublishStartMax").removeClass("expand");
                            }
                        }


                        minPublishDateFilter = new Date(d[2] + "-" + d[1] + "-" + d[0]).getTime();
                        grid.getDataTable().column(7).draw();

                    });

                    $("#dtFilterPublishStartMax").on('keyup change', function () {
                        var d = this.value.split('.');

                        if (this.value != "") {
                            $(this).addClass('expand');
                            $("#dtFilterPublishStartMin").addClass('expand');
                        } else {
                            if ($("#dtFilterPublishStartMin").val() == "") {
                                $(this).removeClass('expand');
                                $("#dtFilterPublishStartMin").removeClass("expand");
                            }

                        }

                        maxPublishDateFilter = new Date(d[2] + "-" + d[1] + "-" + d[0]).getTime();
                        grid.getDataTable().column(7).draw();
                    });

                    $("#dtFilterPublishEndMin").on('keyup change', function () {
                        var d = this.value.split('.');

                        if (this.value != "") {
                            $(this).addClass('expand');
                            $("#dtFilterPublishEndMax").addClass('expand');
                        } else {

                            if ($("#dtFilterPublishEndMax").val() == "") {
                                $(this).removeClass('expand');
                                $("#dtFilterPublishEndMax").removeClass("expand");
                            }
                        }

                        minPublishEndDateFilter = new Date(d[2] + "-" + d[1] + "-" + d[0]).getTime();
                        grid.getDataTable().column(8).draw();

                    });

                    $("#dtFilterPublishEndMax").on('keyup change', function () {
                        var d = this.value.split('.');
                        if (this.value != "") {
                            $(this).addClass('expand');
                            $("#dtFilterPublishEndMin").addClass('expand');

                        } else {

                            if ($("#dtFilterPublishEndMin").val() == "") {
                                $(this).removeClass('expand');
                                $("#dtFilterPublishEndMin").removeClass("expand");
                            }
                        }
                        maxPublishEndDateFilter = new Date(d[2] + "-" + d[1] + "-" + d[0]).getTime();
                        grid.getDataTable().column(8).draw();
                    });


                    $.fn.dataTable.ext.search.push(
                        function (oSettings, aData, iDataIndex) {
                            //console.log(aData)
                            var datePublishStart = aData[7].substr(0, aData[7].indexOf(' '));
                            datePublishStart = datePublishStart.split('.');
                            datePublishStart = new Date(datePublishStart[2] + "-" + datePublishStart[1] + "-" + datePublishStart[0]);

                            if (typeof aData._datePS == 'undefined') {
                                aData._datePS = datePublishStart.getTime();
                            }

                            var datePublishEnd = aData[8].substr(0, aData[8].indexOf(' '));
                            datePublishEnd = datePublishEnd.split('.');
                            datePublishEnd = new Date(datePublishEnd[2] + "-" + datePublishEnd[1] + "-" + datePublishEnd[0]);

                            if (typeof aData._datePE == 'undefined') {
                                aData._datePE = datePublishEnd.getTime();
                            }

                            if (minPublishDateFilter && !isNaN(minPublishDateFilter)) {
                                if (aData._datePS < minPublishDateFilter || isNaN(aData._datePS)) {
                                    return false;
                                }
                            }

                            if (maxPublishDateFilter && !isNaN(maxPublishDateFilter)) {
                                if (aData._datePS > maxPublishDateFilter || isNaN(aData._datePS)) {
                                    return false;
                                }
                            }

                            if (maxPublishEndDateFilter && !isNaN(maxPublishEndDateFilter)) {
                                if (aData._datePE > maxPublishEndDateFilter || isNaN(aData._datePE)) {
                                    return false;
                                }
                            }
                            if (minPublishEndDateFilter && !isNaN(minPublishEndDateFilter)) {
                                if (aData._datePE < minPublishEndDateFilter || isNaN(aData._datePE)) {
                                    return false;
                                }
                            }


                            return true;
                        }
                    );

                    $("#dtFilterPublishEnd").on('keyup change', function () {
                        grid.getDataTable().column(8).search(this.value).draw();
                    });
                    $("#dtFilterGroup").on('keyup change', function () {
                        grid.getDataTable().column(10).search(this.value).draw();
                    });

                }

                if ($('input.checkbox_check.showPublishStart').is(':checked')) {
                    var column = table.columns([7]);
                    column.visible(true, true);
                }
                if ($('input.checkbox_check.showPublishEnd').is(':checked')) {
                    var column = table.columns([8]);
                    column.visible(true, true);
                }
                if ($('input.checkbox_check.showShowOnHomepage').is(':checked')) {
                    var column = table.columns([44]);
                    column.visible(true, true);
                } else {
                    var column = table.columns([44]);
                    column.visible(false, true);

                }

		        if ($('input.checkbox_check.perex').is(':checked')) {
		            var column = table.columns( [ 11, 12, 13 ] ); column.visible(true, true);
		            var TableWidthPerex= 660;
		            $("#dtFilterPerex").on( 'keyup change', function () { grid.getDataTable().column(11).search( this.value ).draw(); });
		            $("#dtFilterPerexPlace").on( 'keyup change', function () { grid.getDataTable().column(12).search( this.value ).draw(); });
		            $("#dtFilterPerexPicture").on( 'keyup change', function () { grid.getDataTable().column(13).search( this.value ).draw(); });
		        } else {
		            var column = table.columns( [ 11, 12, 13 ] ); column.visible(false, true);
		            var TableWidthPerex= 0;
		        }

		        if ($('input.checkbox_check.prava').is(':checked')) {
		            var column = table.columns( [ 14 ] ); column.visible(true, true);
		            var TableWidthPrava= 80;
		            $("#dtFilterPrava").on( 'keyup change', function () { grid.getDataTable().column(14).search( this.value ).draw(); });
		        } else {
		            var column = table.columns( [ 14 ] ); column.visible(false, true);
		            var TableWidthPrava= 0;
		        }

		        if ($('input.checkbox_check.sablona').is(':checked')) {
		            var column = table.columns( [ 15 ] ); column.visible(true, true);
		            var TableWidthSablona = 170;
		            $("#dtFilterTemplateName").on( 'keyup change', function () { grid.getDataTable().column(15).search( this.value ).draw(); });

		        } else {
		            var column = table.columns( [ 15 ] ); column.visible(false, true);
		            var TableWidthSablona = 0;
		        }

		        if ($('input.checkbox_check.url').is(':checked')) {
		            var column = table.columns( [ 16, 17 ] ); column.visible(true, true);
		            var TableWidthUrl = 500;
		        } else {
		            var column = table.columns( [ 16, 17 ] ); column.visible(false, true);
		            var TableWidthUrl = 0;
		        }

		        if ($('input.checkbox_check.polia').is(':checked')) {
		            var column = table.columns( [ 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37 ] ); column.visible(true, true);
		            var TableWidthPolia = 4450;
		            $("#dtFilterPoleA").on( 'keyup change', function () { grid.getDataTable().column(18).search( this.value ).draw(); });
		            $("#dtFilterPoleB").on( 'keyup change', function () { grid.getDataTable().column(19).search( this.value ).draw(); });
		            $("#dtFilterPoleC").on( 'keyup change', function () { grid.getDataTable().column(20).search( this.value ).draw(); });
		            $("#dtFilterPoleD").on( 'keyup change', function () { grid.getDataTable().column(21).search( this.value ).draw(); });
		            $("#dtFilterPoleE").on( 'keyup change', function () { grid.getDataTable().column(22).search( this.value ).draw(); });
		            $("#dtFilterPoleF").on( 'keyup change', function () { grid.getDataTable().column(23).search( this.value ).draw(); });
		            $("#dtFilterPoleG").on( 'keyup change', function () { grid.getDataTable().column(24).search( this.value ).draw(); });
		            $("#dtFilterPoleH").on( 'keyup change', function () { grid.getDataTable().column(25).search( this.value ).draw(); });
		            $("#dtFilterPoleI").on( 'keyup change', function () { grid.getDataTable().column(26).search( this.value ).draw(); });
		            $("#dtFilterPoleJ").on( 'keyup change', function () { grid.getDataTable().column(27).search( this.value ).draw(); });
		            $("#dtFilterPoleK").on( 'keyup change', function () { grid.getDataTable().column(28).search( this.value ).draw(); });
		            $("#dtFilterPoleL").on( 'keyup change', function () { grid.getDataTable().column(29).search( this.value ).draw(); });
		            $("#dtFilterPoleM").on( 'keyup change', function () { grid.getDataTable().column(30).search( this.value ).draw(); });
		            $("#dtFilterPoleN").on( 'keyup change', function () { grid.getDataTable().column(31).search( this.value ).draw(); });
		            $("#dtFilterPoleP").on( 'keyup change', function () { grid.getDataTable().column(32).search( this.value ).draw(); });
		            $("#dtFilterPoleP").on( 'keyup change', function () { grid.getDataTable().column(33).search( this.value ).draw(); });
		            $("#dtFilterPoleQ").on( 'keyup change', function () { grid.getDataTable().column(34).search( this.value ).draw(); });
		            $("#dtFilterPoleR").on( 'keyup change', function () { grid.getDataTable().column(35).search( this.value ).draw(); });
		            $("#dtFilterPoleS").on( 'keyup change', function () { grid.getDataTable().column(36).search( this.value ).draw(); });
		            $("#dtFilterPoleT").on( 'keyup change', function () { grid.getDataTable().column(37).search( this.value ).draw(); });
		        } else {
		            var column = table.columns( [ 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37 ] ); column.visible(false, true);
		            var TableWidthPolia = 0;
		        }


		        if ($('input.checkbox_check.fixnutNazov').is(':checked')) {

		            var column = table.columns( [ 39 ] );
		            column.visible(true, true);
		            $(".table-container").addClass("FixedNameMargin");

		            $(".divFixedNazov").each(function()
		            {
		                tableTdheight = $(this).parents("td").height();
		                $(this).height(tableTdheight);
		            });

		            $(".divFixedNazov_nadpis").show();

		        } else {
		            var column = table.columns( [ 39 ] );
		            column.visible(false, true);
		            $(".table-container").removeClass("FixedNameMargin");

		            $(".divFixedNazov_nadpis").hide();
		        }


		        if (TableWidth == 100) {

		            $("table.dataTable").attr('style', 'width: 99.5% !important');

		        } else {

		            var setTableWidth = TableWidth + TableWidthPublikovanie + TableWidthPerex + TableWidthPrava + TableWidthSablona + TableWidthUrl + TableWidthPolia;
		            $("table.dataTable").attr('style', 'width: '+setTableWidth+' !important');
		        }

				if ($('input.checkbox_check.showNewsEditing').is(':checked'))
				{
					var column = table.columns( [ 4, 10 ] ); column.visible(false, true);
				}
		    }

		    showHideColumns();
		    $('input.checkbox_check').change(function() {
		        function saveProperty(element) {
		            var propertyValue = 0;
		            var propertyName = $(element).attr('data-property');
		            if($(element).is(':checked')) {
		                propertyValue = 1;
		            }
		            $.get('/admin/skins/webjet8/ajax_set_column_property.jsp?property=' + propertyName + '&value=' + propertyValue);
		        }

		        saveProperty($(this));
		        showHideColumns();
		    });

		});


        $('#searchFormId').on( 'keyup', function ()
        {
			    grid.getDataTable().search( this.value ).draw();
		  });


			/*
        // handle group actionsubmit button click
        grid.getTableWrapper().on('click', '.table-group-action-submit', function (e) {
            e.preventDefault();
            var action = $(".table-group-action-input", grid.getTableWrapper());
            if (action.val() != "" && grid.getSelectedRowsCount() > 0) {
                grid.setAjaxParam("customActionType", "group_action");
                grid.setAjaxParam("customActionName", action.val());
                grid.setAjaxParam("id", grid.getSelectedRows());
                grid.getDataTable().ajax.reload();
                grid.clearAjaxParams();
            } else if (action.val() == "") {
                Metronic.alert({
                    type: 'danger',
                    icon: 'warning',
                    message: 'Please select an action',
                    container: grid.getTableWrapper(),
                    place: 'prepend'
                });
            } else if (grid.getSelectedRowsCount() === 0) {
                Metronic.alert({
                    type: 'danger',
                    icon: 'warning',
                    message: 'No record selected',
                    container: grid.getTableWrapper(),
                    place: 'prepend'
                });
            }
        });
        */
    }

    return {

        //main function to initiate the module
        init: function () {
				//console.log("Groupslisttable INIT");
            initPickers();
            //handleRecords(1, groupsListTableData);
        },

        loadData: function(groupId)
        {
           loadGroupsListTableData(groupId);
				setTimeout(function() { Metronic.runResizeHandlers(); }, 100);
        }

    };

}();

<jsp:include page="webpages-common.js.jsp"/>
