<%@page import="sk.iway.iwcm.Identity"%>
<%@page import="sk.iway.iwcm.PathFilter"%>
<%@page import="sk.iway.iwcm.Tools"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.users.SettingsAdminBean"%><%@ page import="sk.iway.iwcm.users.SettingsAdminDB" %><%@ page import="sk.iway.iwcm.users.UsersDB" %><%@ page import="java.util.Map" %>
<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="windows-1250" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>
<%
    Identity currUser = UsersDB.getCurrentUser(request);
    if(currUser==null)
        return;
    Map<String,SettingsAdminBean> userSettings = SettingsAdminDB.getSettings(currUser.getUserId());
%>
<script>

//console.log("Nastavujem config");

var datatables_globalConfig = {
    lengthMenu: [
        [ 10, 25, 50, 100, -1 ],
        [ '10 <iwcm:text key="datatables.paging.rows"/>', '25 <iwcm:text key="datatables.paging.rows"/>', '50 <iwcm:text key="datatables.paging.rows"/>', '100 <iwcm:text key="datatables.paging.rows"/>', '<iwcm:text key="datatables.paging.allRows"/>' ]
    ], //lengthMenu
    language: {
        "url": "/admin/skins/webjet8/assets/global/plugins/datatables/i18n/<%=sk.iway.iwcm.i18n.Prop.getLngForJavascript(request)%>.json",
    },
    pagingType: "simple_numbers",
    dom: "t<'table-control-panel'<'table-control-panel-info'ipf><'table-control-panel-buttons'B>>",
    renderText: function(data){
        var render = $.fn.dataTable.render.text().display;
        return render(data);
    },
    renderDateTime: function ( columnName ) {
        return function ( data, type, row ) {

            if (typeof data == 'undefined' && typeof columnName !== 'undefined')
            {
                //toto je podmienene hladanie stlpca v datach, nie vzdy nam vsetko pride
                data = row[columnName];
            }
            //stlpec nebol podla mena najdeny, takze nie je v datach, vratime prazdny string
            if (typeof data == 'undefined') return "";

            //console.log("columnName", columnName, "data", data, "type", type, "row", row);

            if (type === "display") {
                if (data == "") return "";
                return moment(data).format("<%=sk.iway.iwcm.Constants.getString("dateTimeFormat").replace('d', 'D').replace('y', 'Y')%>");
            }
            return data;
        };
    },
    renderDate: function ( columnName ) {
        return function ( data, type, row )
        {
            if (typeof data == 'undefined' && typeof columnName !== 'undefined')
            {
                //toto je podmienene hladanie stlpca v datach, nie vzdy nam vsetko pride
                data = row[columnName];
            }
            //stlpec nebol podla mena najdeny, takze nie je v datach, vratime prazdny string
            if (typeof data == 'undefined') return "";

            if (type === "display") {
                if (data == "") return "";
                return moment(data).format("<%=sk.iway.iwcm.Constants.getString("dateFormat").replace('d', 'D').replace('y', 'Y')%>");
            }
            return data;
        };
    },
    buttons: [
        {
            extend: 'colvis',
            text: '<i class="fa fa-columns" aria-hidden="true"></i> <iwcm:text key="datatables.columns"/>',
            collectionLayout: 'fixed  two-column table-buttons-collections-style',
            columnText: function ( dt, idx, title ) {
                return title;
            },
            columns: ":not(.not-allow-hide)"
        },
        {
            extend: 'pageLength',
            text: '<i class="fa fa-bars" aria-hidden="true"></i> <iwcm:text key="datatables.paging"/>',
            collectionLayout: 'fixed table-buttons-collections-style'
        },
        {
            extend: 'collection',
            text: '<i class="fa fa-share-square-o" aria-hidden="true"></i> <iwcm:text key="datatables.export"/>',
            collectionLayout: 'fixed table-buttons-collections-style',
            buttons: [
                {
                    extend: 'excelHtml5',
                    text: 'EXCEL',
                    title: 'Data export',
                    exportOptions: {
                        columns: ':visible:not(.not-export)'
                    }
                },
                {
                    extend: 'csvHtml5',
                    text: 'CSV',
                    title: 'Data export',
                    exportOptions: {
                        columns: ':visible:not(.not-export)'
                    }
                },
                {
                    extend: 'pdfHtml5',
                    text: 'PDF',
                    title: 'Data export',
                    download: 'download',
                    exportOptions: {
                        columns: ':visible:not(.not-export)'
                    }
                }
            ]
        }
    ], //buttons

    fn_setColumnsVisibility: function(table) {
        <%!
        String getUserSettings(Map<String,SettingsAdminBean> userSettings, HttpServletRequest request)
        {
            String prefix = PathFilter.getOrigPath(request)+"__";
            StringBuilder sb = new StringBuilder();
            if(userSettings!=null)
            {
                for(Map.Entry<String, SettingsAdminBean> entry : userSettings.entrySet())
                {
                    if(entry.getKey().startsWith(prefix))
                    {
                        String key = entry.getKey().replaceFirst(prefix, "");
                        String row = "try { table.column('"+key+":name').visible(false); } catch(err) {  console.log('- column name not exist - "+key+"'); console.log(err); }";
                        sb.append(row);
                    }
                }
                return sb.toString();
            }
            return "";
        }
        %>

        <%=getUserSettings(userSettings,request)%>
    }, //fn_setColumnsVisibility

    fn_setScrollHeight: function(){
        var newHeight = $(".full-height-content-body").height() - 76 - 50;
        if (newHeight < 100) newHeight = 300;
        $(".dataTables_scrollBody").height(newHeight);
        $(".DTFC_ScrollWrapper").height(newHeight + 76);
        $(".DTFC_LeftBodyWrapper").height(newHeight-20).css("max-height",newHeight-20);
        $(".DTFC_LeftBodyLiner").height(newHeight-20).css("max-height",newHeight-20);
    }, //fn_setScrollHeight

    fn_createButtons: function(buttons) {
        var btnGroup = '<div class="btn-group" role="group">'+buttons+'</div>';
        return btnGroup;
    }, //fn_createButtons

    fn_createRowController: function(id){
        var tools = '<div class="row-two-block not-checkbox"><div class="row-two-block-numbers">'+id+'</div><div class="row-two-block-tools" style="width:14px;">';
        tools += '<div class="btn-group"><a class="show-doc-controller" href="javascript:;" ><i class="fa fa-cog"></i></a>';
        tools += '</div></div></div>';
        return tools;
    }, //fn_createRowController

    fn_initComplete: function(table) {

        this.fn_setColumnsVisibility(table);

        table.columns().iterator('column', function (settings, column) {

            var sortingClass = "sortable";
            if (settings.aoColumns[column].disableSorting === true) {
                sortingClass = "";
            }

            var emptyClass = "";
            if (settings.aoColumns[column].title == "") {
                emptyClass = "title-empty";
            }

            var html = '<div class="table-title '+sortingClass + emptyClass +'">' + settings.aoColumns[column].title + '</div><div class="table-input">';

            html += '<a href="javascript:;" style="display:none;" class="table-clear-search-input"><i class="fa fa-times" aria-hidden="true"></i></a>';

            if (settings.aoColumns[column].searchType == "input") {
                html += '<input type="text" class="form-control form-filter">';
            } else if (settings.aoColumns[column].searchType == "datePublishStart") {
                html += '<input class="date-picker form-control form-filter" type="text" id="dtFilterPublishStartMin" placeholder="<iwcm:text key="datatables.filter.from"/>" />';
                html += '<input class="date-picker form-control form-filter" type="text" id="dtFilterPublishStartMax" placeholder="<iwcm:text key="datatables.filter.to"/>" />';
            } else if (settings.aoColumns[column].searchType == "datePublishEnd") {
                html += '<input class="date-picker form-control form-filter" type="text" id="dtFilterPublishEndMin" placeholder="<iwcm:text key="datatables.filter.from"/>" />';
                html += '<input class="date-picker form-control form-filter" type="text" id="dtFilterPublishEndMax" placeholder="<iwcm:text key="datatables.filter.to"/>" />';
            } else if (settings.aoColumns[column].searchType == "boolean") {
                html += '<select class="form-control boolean-filter">';
                html += '<option value=""><iwcm:text key="datatables.filter.all"/></option>';
                html += '<option value="<iwcm:text key="webstranky.nadpis.true" />"><iwcm:text key="webstranky.nadpis.true" /></option>';
                html += '<option value="<iwcm:text key="webstranky.nadpis.false" />"><iwcm:text key="webstranky.nadpis.false" /></option>';
                html += '</select>';
            } else if (settings.aoColumns[column].searchType == "checkAll") {
                html += '<label><input type="checkbox" name="checkAllId" id="checkAllId" class="group-checkable" value="true" onclick="checkAll();"> <iwcm:text key="datatables.allItems"/></label>';
            }

            html += '</div>';

            $(table.column(column).header()).html(html);
        });

        function searchClearToggler(el) {
            if(el.siblings("input").length > 0) {
                if(el.val() == "" && el.siblings("input").val() == "" ) {
                    el.parents('th').find("a.table-clear-search-input").hide();
                } else {
                    el.parents('th').find("a.table-clear-search-input").show();
                }
            } else {
                if(el.val() == "") {
                    el.parents('th').find("a.table-clear-search-input").hide();
                } else {
                    el.parents('th').find("a.table-clear-search-input").show();
                }
            }
        }

        $(".table-ninja-style").on( 'keyup change', 'input.form-filter', function () {
            if(!$(this).hasClass("date-picker")){
                var thisColumnIndex = table.column( $(this).parents('th').index()+':visIdx' ).index();
                table.column(thisColumnIndex).search( $(this).val() ).draw();
            }
            searchClearToggler($(this));
        });

        $(".table-ninja-style").on( 'change', 'select.boolean-filter', function () {
            var thisColumnIndex = table.column( $(this).parents('th').index()+':visIdx' ).index();
            table.column(thisColumnIndex).search( $(this).val() ).draw();
            searchClearToggler($(this));
        });

        $(".table-ninja-style").on( 'click', '.table-title.sortable', function () {

            var th = $(this).parents('th'),
                thisColumnIndex = table.column( th.index()+':visIdx' ).index(),
                sortingType = "desc";

            if(th.hasClass("sorting_desc")) {
                sortingType = "asc";
            }

            table.order([thisColumnIndex, sortingType]).draw();
        });

        $(".table-ninja-style").on( 'click', 'a.table-clear-search-input', function () {
            var select = $(this).parents('th').find("select"),
                thisColumnIndex = table.column( $(this).parents('th').index()+':visIdx' ).index();

                $(this).parents('th').find("input").each(function () {
                    $(this).val("").trigger("change");
                });

                select.val("");
                table.column(thisColumnIndex).search("").draw();
                $(this).hide();
        });

        $(".dataTables_scrollBody").scroll(function() {
            var el = $(this),
                elWidth = el.width(),
                howFar = 300;

            if(el.scrollLeft() > howFar) {
                $(".DTFC_ScrollWrapper").addClass("is-left-sidebar-fixed");
            } else {
                $(".DTFC_ScrollWrapper").removeClass("is-left-sidebar-fixed");
            }
        });

        $('[data-toggle="tooltip"]').tooltip();

        $('.date-picker').datepicker({
            rtl: Metronic.isRTL(),
            orientation: "left",
            autoclose: true,
            format: "dd.mm.yyyy",
            language: "<%=Prop.getLngForJavascript(request)%>",
            keyboardNavigation: false,
            todayHighlight: true,
            forceParse: false,
            weekStart: 1
        });

        setTimeout(function () {
            datatables_globalConfig.fn_setScrollHeight();
        });
        setTimeout(function () {
            datatables_globalConfig.fn_setScrollHeight();
        },1000);

        $(".table-ninja-style").on('click', '.show-doc-controller', function () {

            var tr = $(this).closest('tr'),
                row = table.row( tr );

            if ( row.child.isShown() ) {
                row.child.hide();
                tr.removeClass('doc-controller-shown');
            } else {
                if (typeof showControllerRow == 'function') {

                    $('.doc-controller-shown').removeClass('doc-controller-shown');
                    $('.tr-controller').remove();

                    row.child( showControllerRow(row.data() ) ).show();
                    row.child().addClass("tr-controller");
                    tr.addClass('doc-controller-shown');
                    $('.tr-controller [data-toggle="tooltip"]').tooltip();
                }
                else {
                    console.log('showControlerRow() not exist');
                }
            }

        });


        $(".table-ninja-style").on( 'column-visibility.dt', function ( e, settings, column, state ) {

            var columns = table.settings().init().columns,
                columnName = columns[column].name,
                prefix = window.location.pathname,
                params = {key:prefix +"__"+ columnName, value:state};

            if(typeof columns[column].name != "undefined") {

                $.ajax({
                    type: "POST",
                    url: "/admin/datatables_set_user_settings_ajax.jsp",
                    data: $.param(params)
                });

            }

        });

        $(".table-ninja-style").on('click', 'tr.tr-controller a', function () {
            $(".doc-controller-shown .show-doc-controller").click();
        });

        setTimeout(function () {
            table.search("").draw();
        });

        $("body").addClass("is-table-ninja-style-init");

    }, //fn_initComplete

    fn_drawCallback: function(table){
        $('[data-toggle="tooltip"]').tooltip();
    } //fn_drawCallback
};

$(document).ready(function() {
    Metronic.addResizeHandler(function(){
        setTimeout(function () {
            datatables_globalConfig.fn_setScrollHeight();
        },1000);
    });
});

</script>