<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--<iwcm:checkLogon admin="true" perms="cmp_restaurant_menu"/>--%>
<%@ include file="/admin/layout_top.jsp" %>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.components.restaurant_menu.MealBean"%>
<%@page import="sk.iway.iwcm.components.restaurant_menu.MealDB"%>
<%@page import="java.util.Calendar"%>
<%@ page import="org.apache.struts.util.ResponseUtils" %>

<link rel="stylesheet" href="css/custom-style.css">

<iwcm:menu notName="cmp_restaurant_menu"><%response.sendRedirect("/admin/403.jsp");
	if (1==1) return;%></iwcm:menu>

<script type="text/javascript">
    function setWeek() {
        window.open("/components/restaurant_menu/admin_new_menu.jsp?week="+document.getElementById('weekId').value,"_self");
    }
</script>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	Prop prop = Prop.getInstance(lng);

	request.setAttribute("cmpName", "restaurant_menu");

	if (request.getParameter("menuSave")!=null)
	{
		pageContext.include("/sk/iway/iwcm/components/restaurant_menu/Restaurant.action");
	}

	List<MealBean> mealList = MealDB.getInstance().findAll();

	String actualWeek = Integer.toString(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)) + "-"+ Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
	String week = Tools.getStringValue(request.getParameter("week"), actualWeek);


%>


<style type="text/css">
	select {
		resize: vertical;
	}

	div#ui-datepicker-div{
		width:220px;
	}
	.datepicker table tbody tr:hover {
		background: #dff0d8;
	}
</style>

<div class="padding10">
	<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.components.restaurant_menu.RestaurantAction"/>
	<iwcm:stripForm action="<%=PathFilter.getOrigPath(request)%>" beanclass="sk.iway.iwcm.components.restaurant_menu.RestaurantAction" name="menuForm" method="post">

	<div class="row title">
		<h1 class="page-title"><i class="fa icon-book-open"></i><iwcm:text key="components.restaurant_menu.title"/><i class="fa fa-angle-right"></i><iwcm:text key="components.restaurant_menu.newMenu"/></h1>
	</div>

	<div class="tabbable tabbable-custom tabbable-full-width">
		<ul class="nav nav-tabs">
			<li class="active"><a id="tabLink1" href="#tabMenu1" data-toggle="tab"><iwcm:text key="components.filter"/></a></li>
		</ul>

		<div class="tab-content">
			<div id="tabMenu1" class="tab-pane active">
				<div class="col-md-2 col-sm-6">
					<div class="form-group">
						<label for="weekId" class="form-label"><iwcm:text key="components.restaurant_menu.vyberTyzden"/></label>
						<stripes:text name="week" id="weekId" class="form-control date-picker" value="<%=week%>"/>
					</div>
				</div>
				<div class="col-md-2 col-sm-6">
					<div class="form-group">
						<label class="control-label display-block">&nbsp;</label>
						<input type="button" class="btn green" value="<iwcm:text key="components.restaurant_menu.select"/>" onclick="setWeek()" />
						<input type="submit" name="menuSave" class="btn green" value="<iwcm:text key="button.save"/>" />
					</div>
				</div>
			</div>
		</div>

		<div class="col-sm-12">
			<div class="row">
				<div class="col-xs-12">
					<div class="menu__wrapper menu-pon clearfix">
						<div class="main--title"><iwcm:text key="components.restaurant_menu.pondelok"/></div>

						<div class="tooltip-custom">
							<i class="fa fa-question-circle" aria-hidden="true"></i>
							<span class="tooltiptext-custom">
								Vyberte jedlá pre daný deň z jednotlivých kategórií kliknutím na konkrétne jedlo. Na pravej strane sa zobrazí zoznam zvolených jedál,
								medzi ktorými je následne možné meniť ich poradie. Po výbere jedál, alebo po ich usporiadaní je potrebné kliknúť na tlačidlo "uložiť".
							</span>
						</div>
						<select name="pon" class="menuMeals" id="menu-pon" onchange="getCheckedTree('menu-pon')">
							<%
								List<Integer> pon = actionBean.getPon();
								//	MenuBean mb = MenuDB.getInstance().getById(Integer.parseInt(getRequest().getParameter("menuToUpdate")));
								//actionBean.getMenuToDelete()
								for (MealBean m : mealList)
								{
									out.print("<option value=\""+m.getId()+"\" ");
									out.print("data-section=\""+ResponseUtils.filter(m.getCathegory())+"\"");
									if (pon.contains(m.getId())) out.print("data-index=\""+((pon.indexOf(m.getId()))+1)+"\" selected=\"selected\"");
									out.println(">"+m.getName()+"</option>");
								}
							%>
						</select>
						<div class="clearfix"></div>
						<input type="submit" name="menuSave" class="btn green" value="<iwcm:text key="button.save"/>" />
					</div>
				</div>
			</div>

			<div class="row">
				<div class="col-xs-12">
					<div class="menu__wrapper menu-uto clearfix">
						<div class="main--title"><iwcm:text key="components.restaurant_menu.utorok"/></div>
						<div class="tooltip-custom">
							<i class="fa fa-question-circle" aria-hidden="true"></i>
							<span class="tooltiptext-custom">
								Vyberte jedlá pre daný deň z jednotlivých kategórií kliknutím na konkrétne jedlo. Na pravej strane sa zobrazí zoznam zvolených jedál,
								medzi ktorými je následne možné meniť ich poradie. Po výbere jedál, alebo po ich usporiadaní je potrebné kliknúť na tlačidlo "uložiť".
							</span>
						</div>
						<select name="uto" class="menuMeals" id="menu-uto" onchange="getCheckedTree('menu-uto')">
							<%
								List<Integer> uto = actionBean.getUto();
								for (MealBean m : mealList)
								{
									out.print("<option value=\""+m.getId()+"\" ");
									out.print("data-section=\""+ResponseUtils.filter(m.getCathegory())+"\"");
									if (uto.contains(m.getId())) out.print("data-index=\""+((uto.indexOf(m.getId()))+1)+"\" selected=\"selected\"");
									out.println(">"+m.getName()+"</option>");
								}
							%>
						</select>
						<div class="clearfix"></div>
						<input type="submit" name="menuSave" class="btn green" value="<iwcm:text key="button.save"/>" />
					</div>
				</div>
			</div>

			<div class="row">
				<div class="col-xs-12">
					<div class="menu__wrapper menu-str clearfix">
						<div class="main--title"><iwcm:text key="components.restaurant_menu.streda"/></div>
						<div class="tooltip-custom">
							<i class="fa fa-question-circle" aria-hidden="true"></i>
							<span class="tooltiptext-custom">
								Vyberte jedlá pre daný deň z jednotlivých kategórií kliknutím na konkrétne jedlo. Na pravej strane sa zobrazí zoznam zvolených jedál,
								medzi ktorými je následne možné meniť ich poradie. Po výbere jedál, alebo po ich usporiadaní je potrebné kliknúť na tlačidlo "uložiť".
							</span>
						</div>
						<select name="str" class="menuMeals" id="menu-str" onchange="getCheckedTree('menu-str')">
							<%
								List<Integer> str = actionBean.getStr();
								for (MealBean m : mealList)
								{
									out.print("<option value=\""+m.getId()+"\" ");
									out.print("data-section=\""+ResponseUtils.filter(m.getCathegory())+"\"");
									if (str.contains(m.getId())) out.print("data-index=\""+((str.indexOf(m.getId()))+1)+"\" selected=\"selected\"");
									out.println(">"+m.getName()+"</option>");
								}
							%>
						</select>
						<div class="clearfix"></div>
						<input type="submit" name="menuSave" class="btn green" value="<iwcm:text key="button.save"/>" />
					</div>
				</div>
			</div>

			<div class="row">
				<div class="col-xs-12">
					<div class="menu__wrapper menu-stv clearfix">
						<div class="main--title"><iwcm:text key="components.restaurant_menu.stvrtok"/></div>
						<div class="tooltip-custom">
							<i class="fa fa-question-circle" aria-hidden="true"></i>
							<span class="tooltiptext-custom">
								Vyberte jedlá pre daný deň z jednotlivých kategórií kliknutím na konkrétne jedlo. Na pravej strane sa zobrazí zoznam zvolených jedál,
								medzi ktorými je následne možné meniť ich poradie. Po výbere jedál, alebo po ich usporiadaní je potrebné kliknúť na tlačidlo "uložiť".
							</span>
						</div>
						<select name="stv" class="menuMeals" id="menu-stv" onchange="getCheckedTree('menu-stv')">
							<%
								List<Integer> stv = actionBean.getStv();
								for (MealBean m : mealList)
								{
									out.print("<option value=\""+m.getId()+"\" ");
									out.print("data-section=\""+ResponseUtils.filter(m.getCathegory())+"\"");
									if (stv.contains(m.getId())) out.print("data-index=\""+((stv.indexOf(m.getId()))+1)+"\" selected=\"selected\"");
									out.println(">"+m.getName()+"</option>");
								}
							%>
						</select>
						<div class="clearfix"></div>
						<input type="submit" name="menuSave" class="btn green" value="<iwcm:text key="button.save"/>" />
					</div>
				</div>
			</div>

			<div class="row">
				<div class="col-xs-12">
					<div class="menu__wrapper menu-pia clearfix">
						<div class="main--title"><iwcm:text key="components.restaurant_menu.piatok"/></div>
						<div class="tooltip-custom">
							<i class="fa fa-question-circle" aria-hidden="true"></i>
							<span class="tooltiptext-custom">
								Vyberte jedlá pre daný deň z jednotlivých kategórií kliknutím na konkrétne jedlo. Na pravej strane sa zobrazí zoznam zvolených jedál,
								medzi ktorými je následne možné meniť ich poradie. Po výbere jedál, alebo po ich usporiadaní je potrebné kliknúť na tlačidlo "uložiť".
							</span>
						</div>
						<select name="pia" class="menuMeals" id="menu-pia" onchange="getCheckedTree('menu-pia')">
							<%
								List<Integer> pia = actionBean.getPia();
								for (MealBean m : mealList)
								{
									out.print("<option value=\""+m.getId()+"\" ");
									out.print("data-section=\""+ResponseUtils.filter(m.getCathegory())+"\"");
									if (pia.contains(m.getId())) out.print("data-index=\""+((pia.indexOf(m.getId()))+1)+"\" selected=\"selected\"");
									out.println(">"+m.getName()+"</option>");
								}
							%>
						</select>
						<div class="clearfix"></div>
						<input type="submit" name="menuSave" class="btn green" value="<iwcm:text key="button.save"/>" />
					</div>
				</div>
			</div>
		</div>

		</iwcm:stripForm>
	</div>

	<script type="text/javascript">

        $(document).ready(function(){
            Layout.init();

			$('#weekId').val('<%=week%>');

            $('.date-picker').datepicker({
                autoclose: true,
                format: "dd-mm-yyyy",
				language: "sk",
				calendarWeeks: true,
				daysOfWeekDisabled: [0,6],
				weekStart: 1

			}).on('hide', function(e) {
                var kk = $(".date-picker").val(),
					week = moment(kk, "DD/MM/YYYY").isoWeek(),
					year = moment(kk, "DD/MM/YYYY").year(),
					attachIt = week + "-" + year;

                if (moment(kk, "DD/MM/YYYY").week() == 1 && moment(kk, "DD/MM/YYYY").month() == 0) {
					$('#weekId').val(week + "-" + (year - 1));
				} else {
					$('#weekId').val(attachIt);
				}

            })
        });
        $('option').mousedown(function(e) {
            e.preventDefault();
            $(this).prop('selected', $(this).prop('selected') ? false : true);
            return false;
        });

        function newMeal()
        {
            openPopupDialogFromLeftMenu('/components/restaurant_menu/admin_new_meal.jsp');
        }
	</script>

	<script type="text/javascript">
        function sort_list(a, b){
            if($(b).data('index') == undefined || $(b).data('index') == undefined ) return -1;
              return parseInt(($(b).data('index'))) < parseInt(($(a).data('index'))) ? 1 : -1;
        }

        function reorderMeals(){
            $("select.menuMeals option").each(function () {
                if($(this).attr("data-index") == undefined){
                    $(this).attr("data-index", -1);
                }
            });
            $("select#menu-pon option").sort(sort_list).appendTo('select#menu-pon');
            $("select#menu-uto option").sort(sort_list).appendTo('select#menu-uto');
            $("select#menu-str option").sort(sort_list).appendTo('select#menu-str');
            $("select#menu-stv option").sort(sort_list).appendTo('select#menu-stv');
            $("select#menu-pia option").sort(sort_list).appendTo('select#menu-pia');
        }

        function updateMealOrder(parent){
             $(parent).find(".selected div.item").each(function (i) {
               var value = $(this).attr("data-value");
               var option =   $(parent).find("select.menuMeals option[value='"+value+"']");
                 console.log(option);
               option.attr("data-index", i+1);
            });
        }

        function sortableListener(el){
            $(el+' .selected').sortable({
                update: function(event, ui) {
                    $(el+' .selected .item').each(function(i) {
                        $(this).attr('data-key', i + 1);
                    });
                    $(el+' .item').on('click', function() {
                        //alert($(this).data("data-value"));
                    })
                    updateMealOrder($(el));
                }
            });
		}

        $(document).ready(function() {
            reorderMeals();

            $("form[name=menuForm]").submit(function (e) {
                //  e.preventDefault();
                reorderMeals();
            });

            var $selectPon = $('#menu-pon'),
                $selectUto = $('#menu-uto'),
                $selectStr = $('#menu-str'),
                $selectStv = $('#menu-stv'),
                $selectPia = $('#menu-pia');

            $selectPon.treeMultiselect({ enableSelectAll: false, sortable: true, searchable: true, startCollapse: true });
            $selectUto.treeMultiselect({ enableSelectAll: false, sortable: true, searchable: true, startCollapse: true });
            $selectStr.treeMultiselect({ enableSelectAll: false, sortable: true, searchable: true, startCollapse: true });
            $selectStv.treeMultiselect({ enableSelectAll: false, sortable: true, searchable: true, startCollapse: true });
            $selectPia.treeMultiselect({ enableSelectAll: false, sortable: true, searchable: true, startCollapse: true });

            sortableListener(".menu-pon");
            sortableListener(".menu-uto");
            sortableListener(".menu-str");
            sortableListener(".menu-stv");
            sortableListener(".menu-pia");

            $( ".search" ).after("<input type='button' class='button50' value='Pridať nové jedlo' onclick='newMeal()'>");
        });

        function getCheckedTree(menuId){
            var selectobject = document.getElementById(menuId).getElementsByTagName("option");

            var tempCtr = 0;
            var $checkedText = $("#checkedTree");

            var selectobject = $("[id*=treemultiselect-0-]:checked");
            $checkedText.val("");

            for(i=0;i<selectobject.length;i++) {
                if(tempCtr==0){
                    tempCtr=1;
                    //console.log(selectobject[i])
                    $checkedText.val($(selectobject[i]).parent().data("value"));
                } else {
                    $checkedText.val($checkedText.val() + $(selectobject[i]).parent().data("value"));

                }
            }
        }
	</script>

	<%=Tools.insertJQueryUI(request, "*")%>
	<script type="text/javascript" src="js/tree-multiselect.js"></script>
	<iwcm:combine type="js" set="adminStandardJs" />
	<script src="/admin/scripts/common.jsp" type="text/javascript"></script>
