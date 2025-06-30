<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.users.SettingsAdminBean"%>
<%@page import="sk.iway.iwcm.Identity"%>
<%@page import="java.util.Map"%>
<%@page import="sk.iway.iwcm.users.SettingsAdminDB"%><%@ page import="sk.iway.iwcm.i18n.Prop" %><%@ page import="sk.iway.iwcm.Tools" %>
<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>
<iwcm:menu notName="menuTemplates">
	<%
		response.sendRedirect("/admin/403.jsp");
		if (1==1) return;
	%>
</iwcm:menu>

<%@ include file="/admin/layout_top.jsp" %>

<div class="row title">
    <h1 class="page-title"><i class="fa icon-grid"></i><iwcm:text key="temps_list.templates"/><i class="ti ti-chevron-right"></i><iwcm:text key="admin.temp_list.zoznam_sablon"/></h1>
</div>

<div class="tabbable tabbable-custom tabbable-full-width full-height-content-ignore-block">
	<div class="tab-content">
        <div id="tabMenu01" class="tab-pane active">
            <button onclick="javascript:openPopupDialogFromLeftMenu('/admin/listtemps.do?tempid=-1');" type="button" class="btn default imageButton">
                <i class="wjIconBig wjIconBig-uploadDoc"></i>
                <iwcm:text key="temps_list.newTemplate"/>
            </button>
            <button onclick="javascript:openPopupDialogFromLeftMenu('/admin/listtemps.do?merge=1');" type="button" class="btn default imageButton">
                <i class="wjIconBig wjIconBig-uploadDoc"></i>
                <iwcm:text key="admin.temp_merge.merge_stranok"/>
            </button>
        </div>
	</div>
</div>

<%@ include file="/admin/datatables_show_columns_from_settings.jsp" %>
<div class="table-ninja-style-wrapper">
    <table class="table table-striped table-bordered nowrap table-ninja-style" style="width: 100%;" id="datatables_tempsList"></table>
</div>

<script type="text/javascript">
$(document).ready(function() {

    $("#datatables_tempsList").DataTable({
            initComplete: function(oSettings, json) {
                datatables_globalConfig.fn_initComplete(this.api());
            },
            fnDrawCallback: function(oSettings) {
                datatables_globalConfig.fn_drawCallback(this.api());
            },
            columns: [
				{
				    data: "tempId",
					name: "tempId",
                    title: 'Id',
                    className: "not-allow-hide row-controller-and-settings",
                    render: function ( data, type, full, meta ) {
                        return datatables_globalConfig.fn_createRowController(full.tempId);
                    }
				}, {
				    data: "tempName",
                    name: "tempName",
                    title: '<iwcm:text key="admin.temps_list.nazov"/>',
                    className: "not-allow-hide",
                    render: function ( data, type, full, meta ) {
                        return '<a class="doc-title-link-edit" href="javascript:openPopupDialogFromLeftMenu(\'listtemps.do?tempid='+full.tempId+'\');"><i class="ti ti-pencil"></i> '+datatables_globalConfig.renderText(data)+'</a>';
                    }
				},
                {
                    data: "templatesGroupName",
                    name: "tempGroup",
                    title: 'Skupina'
                }, {
				    data: "lng",
                    name: "lng",
                    title: '<iwcm:text key="admin.temps_list.jazyk"/>'
				}, {
				    data: { _ : "pocetPouziti", sort: "pocetPouziti" },
                    name: "pocetPouziti",
                    title: '<iwcm:text key="admin.temps_list.pocet_pouziti"/>',
                    searchType: "input",
				    render: function(data, type, full, meta) {
                        var html ="";
                        if(data==0) {
                            html = '<span class="table-warning-text"><span data-bs-toggle="tooltip" data-placement="right" title="<iwcm:text key="temps.tooltip.temp-is-not-used"/>"><i class="ti ti-info-circle" aria-hidden="true"></i></span></span>';
                        } else {
                            html = '<a href="/admin/temps_doc_list.jsp?tid='+full.tempId+'">'+datatables_globalConfig.renderText(data)+'x</a>';
                        }
                        return html;
				    }
				}, {
				    data: "HTMLSablona",
                    name: "HTMLSablona",
                    title: '<iwcm:text key="admin.temps_list.html_sablona"/>',
				    render: function(data, type, full, meta) {

				        var html = '';

				        if(full.templatesForwardExists===false){
				            html += '<span class="table-alert-text"><span data-bs-toggle="tooltip" data-placement="right" title="<iwcm:text key="temps.tooltip.file-not-exist"/>"><i class="ti ti-alert-circle" aria-hidden="true"></i>';
                        }

				        html += data;

				        if(full.templatesForwardExists===false){
                            html += '</span></span>';
                        }

				        return html;
				    }
				}, {
				    data: "header",
                    name: "header",
                    title: '<iwcm:text key="temp_edit.header"/>',
				    render: function(data, type, full, meta) {
                        var html = "";
                        if (data != null && data != "") {
                            html = '<a href="javascript:popup(\'editor.do?docid=' + full.headerId + '\', 900, 600);">' + datatables_globalConfig.renderText(data) + '</a>';
                        }
                        return html;
				    }
				}, {
				    data: "footer",
                    name: "footer",
                    title: '<iwcm:text key="temp_edit.footer"/>',
				    render: function(data, type, full, meta) {
                        var html = "";
                        if (data != null && data != "") {
                            html = '<a href="javascript:popup(\'editor.do?docid='+full.footerId+'\', 900, 600);">'+datatables_globalConfig.renderText(data)+'</a>';
                        }
                        return html;
				    }
				}, {
				    data: "menu",
                    name: "menu",
                    title: '<iwcm:text key="temp_edit.menu"/>',
				    render: function(data, type, full, meta) {
                        var html = "";
                        if (data != null && data != "") {
                            html = '<a href="javascript:popup(\'editor.do?docid='+full.menuId+'\', 1000, 800);">'+datatables_globalConfig.renderText(data)+'</a>';
                        }
                        return html;
				    }
				}, {
				    data: "rightMenu",
                    name: "rightMenu",
                    title: '<iwcm:text key="temp_edit.right_menu"/>',
				    render: function(data, type, full, meta) {
                        var html = "";
                        if (data != null && data != "") {
                            html = '<a href="javascript:popup(\'editor.do?docid='+full.rightMenuId+'\', 1000, 800);">'+datatables_globalConfig.renderText(data)+'</a>';
                        }
                        return html;
				    }
				}, {
				    data: "objectA",
                    name: "objectA",
                    title: '<iwcm:text key="temp_edit.object_a"/>',
				    render: function(data, type, full, meta) {
                        var html = "";
                        if (data != null && data != "") {
                            html = '<a href="javascript:popup(\'editor.do?docid='+full.objectAId+'\', 1000, 800);">'+datatables_globalConfig.renderText(data)+'</a>';
                        }
                        return html;
				    }
				}, {
				    data: "objectB",
                    name: "objectB",
                    title: '<iwcm:text key="temp_edit.object_b"/>',
				    render: function(data, type, full, meta) {
                        var html = "";
                        if (data != null && data != "") {
                            html = '<a href="javascript:popup(\'editor.do?docid='+full.objectBId+'\', 1000, 800);">'+datatables_globalConfig.renderText(data)+'</a>';
                        }
                        return html;
				    }
				}, {
				    data: "objectC",
                    name: "objectC",
                    title: '<iwcm:text key="temp_edit.object_c"/>',
				    render: function(data, type, full, meta) {
                        var html = "";
                        if (data != null && data != "") {
                            html = '<a href="javascript:popup(\'editor.do?docid='+full.objectCId+'\', 1000, 800);">'+datatables_globalConfig.renderText(data)+'</a>';
                        }
                        return html;
				    }
				}, {
				    data: "objectD",
                    name: "objectD",
                    title: '<iwcm:text key="temp_edit.object_d"/>',
				    render: function(data, type, full, meta) {
                        var html = "";
                        if (data != null && data != "") {
                            html = '<a href="javascript:popup(\'editor.do?docid='+full.objectDId+'\', 1000, 800);">'+datatables_globalConfig.renderText(data)+'</a>';
                        }
                        return html;
				    }
				}, {
				    data: "baseCSSPath",
                    name: "baseCSSPath",
                    title: '<iwcm:text key="temp_edit.base_css_style"/>'
				}, {
				    data: "CSSStyle",
                    name: "CSSStyle",
                    title: '<iwcm:text key="temp_edit.CSSStyle"/>'
				}, {
				    data: "HTMLCode",
                    name: "HTMLCode",
                    title: '<iwcm:text key="components.banner.html"/>'
				}, {
				    data: "spamProtectionDisabled",
                    name: "spamProtectionDisabled",
                    title: 'Spam ochrana',
                    searchType: "boolean",
				    render: function(data, type, full, meta) {
                        var html ="";
                        if(data == true) {
                            html = "áno";
						} else if (data == false) {
                            html = "nie";
						};
                        return html;
				    }
				}, {
				    data: "installName",
                    name: "installName",
                    title: '<iwcm:text key="temp_edit.install_name"/>'
				}
            ],
            ajax: {
                type: "POST",
                url: "ajax_temps.jsp"
            },
            columnDefs: [
                {
                    targets: "_all",
                    sortable: false,
                    searchType: "input",
                    render: $.fn.dataTable.render.text()
                }
            ],
            serverSide: false,
            bStateSave: false,
            info: true,
            searching: true,
            bFilter: true,
            paging: true,
            pageLength: 25,
            pagingType: datatables_globalConfig.pagingType,
            ordering: true,
            order: [[0, 'asc']],
            orderCellsTop: false,
            responsive: true,
            scrollX: "100%",
            scrollY: "100%",
            scrollCollapse:true,
            fixedColumns:   {
                leftColumns: 2
            },
            language: datatables_globalConfig.language,
            dom: datatables_globalConfig.dom,
            lengthMenu: datatables_globalConfig.lengthMenu,
            buttons: datatables_globalConfig.buttons
    });

});

function showControllerRow(data) {
    var buttons = '<a class="btn btn-default" href="javascript:openPopupDialogFromLeftMenu(\'listtemps.do?tempid='+data.tempId+'\');"><i class="ti ti-pencil"></i> <iwcm:text key="components.tips.edit"/></a>';
    if(data.pocetPouziti < 1) {
        buttons += '<a class="btn btn-default" href="javascript:deleteTemplate(\'' + data.tempId + '\');"><i class="ti ti-trash"></i> <iwcm:text key="components.tips.delete"/></a>';
    } else {
        buttons += '<span data-bs-toggle="tooltip" data-placement="right" title="<iwcm:text key="temps.tooltip.delete-info"/>"><a class="btn btn-default disabled" href="javascript:deleteTemplate(\'' + data.tempId + '\');"><i class="ti ti-trash"></i> <iwcm:text key="components.tips.delete"/></a></span>';
    }
    return datatables_globalConfig.fn_createButtons(buttons);
}

function deleteTemplate(id) {
    if (window.confirm('<iwcm:text key="temp_edit.delete_confirm"/>')) {
        window.location.href="/admin/savetemp.do?act=delete&tempId=" + id;
    }
}
</script>

<%@ include file="/admin/layout_bottom.jsp" %>