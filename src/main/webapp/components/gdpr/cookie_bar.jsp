<%@page import="java.util.List" %>
<%@page import="sk.iway.iwcm.i18n.Prop" %>
<%@page import="java.util.Iterator" %>
<%@page import="java.util.HashSet" %>
<%@page import="java.util.HashMap" %>
<%@page import="sk.iway.iwcm.components.gdpr.CookieManagerBean" %>
<%@page import="java.util.ArrayList" %>
<%@page import="java.util.LinkedList" %>
<%@page import="sk.iway.iwcm.components.gdpr.CookieManagerDB" %>
<%@page import="sk.iway.iwcm.users.UsersDB" %>
<%
    sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ page import="sk.iway.iwcm.doc.DocDetails" %>
<%@ page import="sk.iway.iwcm.doc.GroupDetails" %>
<%@ page import="sk.iway.iwcm.doc.GroupsDB" %>
<%@ page import="sk.iway.iwcm.doc.DocDB" %>
<%@
        taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@
        taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@
        taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@
        taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld" %>
<%@
        taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%

    //    Identity user = UsersDB.getCurrentUser(request);
//    if(!Constants.getBoolean("cookieBannerAllowed") && (user == null ||  user != null && !user.isAdmin()) )
//    {
//        return;
//    }

    PageParams pageParams = new PageParams(request);
    String lng = PageLng.getUserLng(request);

    String extLng = pageParams.getValue("lng", lng);
    pageContext.setAttribute("lng", extLng);
%>

<div class="md-cookie-bar">
    <div class="cookies-bar-wrapper cookies-top">
        <div class="cookies-bar">
            <h2><iwcm:text key="cookies.bar.title"/></h2>
            <p><iwcm:text key="cookies.bar.text"/></p>
            <div class="bottom-buttons">
                <button class="btn btn-akcept btn-primary" data-acceptall="yes">
                    <iwcm:text key="cookies.bar.acceptAll"/>
                </button>
                <button class="btn btn-reject btn-primary" data-rejectall="yes">
                    <iwcm:text key="cookies.bar.rejectAll"/>
                </button>
                <button class="btn more btn-secondary showHideBlock">
                    <iwcm:text key="cookies.bar.detail"/>
                </button>
            </div>

        </div>
    </div>
    <div class="cookies-list">

    </div>
</div>
<div class="cb-overlay"></div>
<script type="text/javascript" src="/components/_common/javascript/jquery.cookie.js"></script>
<script type="text/javascript">
    function initFunctions(){
        var cookiesList = $(".cookies-list");
        var contentLoaded = false;
        $("div.md-cookie-bar .btn-akcept, div.md-cookie-bar .btn-reject").on("click", function () {
            $(".cookies-bar-wrapper, div.cookies-list").hide();

            var cookieList = document.cookie.split(/;\s*/);
            for (var J = cookieList.length - 1; J >= 0; --J) {
                var cookieName = cookieList[J].replace(/\s*(\w+)=.+$/, "$1");
                eraseCookie(cookieName);
            }

            var rels = [];

            $.cookie("cookies-gdpr-policy", "saved", {path: '/', expires: 365});

            var selector = "div.cookies-list input[type=checkbox]";
            //ak na Acku nie je data element acceptAll musime vybrat len zaskrtnute
            if ("yes" !== $(this).data("acceptall")) selector += ":checked";

            var inputCheckbox = $(selector);

            rels = inputCheckbox.map(function () {
                return $(this).attr("data-rel");
            }).toArray();

            if ("yes" === $(this).data("acceptall")) {
                var cookieClassification = "<%=Constants.getString("gdprCookieClassificationsDefault")%>";
                rels = [];

                $.each(cookieClassification.split(","), function(index, item) {
                    rels.push(item);
                });
            }
            var logAccept = true;
            if ("yes" === $(this).data("rejectall")) {
                rels = [];
                rels.push("nutne");
                logAccept = false;
            }

            var categories = rels.join("_")
            $.cookie("enableCookieCategory", categories, {path: '/', expires: 365});

            if (typeof window.dataLayer != "undefined") {
                try {
                    gtag('consent', 'update', gtagGetConsentJson(categories));
                    dataLayer.push({'event': 'consent-update'});
                } catch (e) {}
            }

            if (logAccept===false) {
                categories = "nutne";
            }
            $.ajax({
                url: "/components/gdpr/cookie_save_ajax.jsp",
                method: "post",
                data: {
                    categories: categories
                },
                success: function() {
                    <% if (pageParams.getBooleanValue("reload", true)) {%>
                    window.location.reload();
                    <% } %>
                }
            });
        });

        $("a.cookies-settings").on("click", function () {
            if (cookiesList.is(":visible") !== true) {
                cookiesList.show();
            }
            var cookies = $.cookie("enableCookieCategory");
            var cookie = cookies.split("_");
            $.each(cookie, function (i, v) {
                $("input#checkboxAccept-" + v).attr("checked", "checked");
            });
            return false;
        });
        $("a.nav-link").on("click", function(){
            $("a.nav-link").attr("aria-selected", "false");
            $(this).attr("aria-selected", "true");
        });
        $(".resp-tabs-list li").on("click", function () {
            $(".resp-tabs-container .resp-tab-content").hide();
            $(".resp-tabs-list li").removeClass("resp-tab-active");
            $(this).addClass("resp-tab-active");
            $($(this).find('a').attr("data-href")).fadeIn();
        });
        $(".cookies-list-menu a").on("click", function () {
            $(".cookies-list-content").hide();
            $(".cookies-list-menu li").removeClass("active");
            $(".cookies-list-menu li a").attr("aria-selected", "false");
            $(this).attr("aria-selected", "true").parent().addClass("active");
            $($(this).attr("data-href")).show();
        });
        $(".showHideBlock, div.cookies-list span.close").on("click", function () {
            if (cookiesList.is(":visible") === true) {
                $("div.cookies-top a.btn.more").text('<iwcm:text key="cookies.bar.btn.detail.show"/>');
                cookiesList.hide();
                if ($.cookie("cookies-gdpr-policy") != null) {
                    $("div.cookies-top").hide();
                } else {
                    $("div.cookies-top").show();
                }
            } else {
                //$("div.cookies-top a.btn.more").text('<iwcm:text key="cookies.bar.btn.detail.hide"/>');
                if(!contentLoaded){
                    $.get("/components/gdpr/cookie_bar_ajax.jsp?lng=<%=extLng %>", function(data){
                        cookiesList.append(data);
                        $(".btn-akcept").attr('data-acceptall', 'no')
                        initFunctions();
                        contentLoaded = true;
                    });
                } else {
                    setTimeout(function () {
                        cookiesList.show();
                        $("div.cookies-top").hide();
                    }, 1)
                }
                cookiesList.show();
                $("div.cookies-top").hide();
            }
        });
    }
    $(function () {
        initFunctions();
        if ($.cookie("cookies-gdpr-policy") != null) {
            $(".cookies-bar-wrapper").hide();
        } else {
            $(".cookies-bar-wrapper").show();
        }
        $(".cookies-list").hide();
    });

    function eraseCookie(cookieName) {
        var domain = document.domain;
        var domain2 = document.domain.replace(/^www\./, "");
        var domain3 = document.domain.replace(/^(\w+\.)+?(\w+\.\w+)$/, "$2");
        var pathNodes = location.pathname.split("/").map(function (pathWord) {
            return '/' + pathWord;
        });
        var cookPaths = [""].concat(pathNodes.map(function (pathNode) {
            if (this.pathStr) {
                this.pathStr += pathNode;
            }
            else {
                this.pathStr = "; path=";
                return (this.pathStr + pathNode);
            }
            return (this.pathStr);
        }));

        (eraseCookie = function (cookieName) {
            cookPaths.forEach(function (pathStr) {
                document.cookie = cookieName + "=" + pathStr + "; expires=Thu, 01-Jan-1970 00:00:01 GMT;";
                document.cookie = cookieName + "=" + pathStr + "; domain=" + domain + "; expires=Thu, 01-Jan-1970 00:00:01 GMT;";
                document.cookie = cookieName + "=" + pathStr + "; domain=" + domain2 + "; expires=Thu, 01-Jan-1970 00:00:01 GMT;";
                document.cookie = cookieName + "=" + pathStr + "; domain=" + domain3 + "; expires=Thu, 01-Jan-1970 00:00:01 GMT;";
            });
        })(cookieName);
    }
</script>
<link rel="stylesheet" href="/components/gdpr/style.css">
<% if (pageParams.getBooleanValue("showLink", false)) { %>
    <iwcm:text key="components.gdpr.cookies.cookiesSettingsLink"/>
<% } %>