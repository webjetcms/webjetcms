//Pomocne funkcie pre PB
//autogenerovanie tabov a accordionov

$(document).ready(function() {
   pbAutotabs();
   pbAutoAcoordion();
   window.setInterval(function() {pbAutotabs();}, 5000);

   window.addEventListener("WJ.PageBuilder.loaded", function(e) {
       pbAutotabs();
       pbAutoAcoordion();
   });
   window.addEventListener("WJ.PageBuilder.gridChanged", function(e) {
       pbAutotabs();
       pbAutoAcoordion();
   });
   window.addEventListener("WJ.PageBuilder.styleChange", function(e) {
       pbAutotabs();
       pbAutoAcoordion();
   });
   window.addEventListener("WJ.PageBuilder.elementRemoved", function(e) {
       pbAutotabs();
       pbSetActiveOnRemove(e.detail);
   });
});

function pbAutotabs() {
   if ($("body").hasClass("is-view-mode")) return;
   $("ul.pb-autotabs").each(function(index) {
       var id = $(this).attr("id");
       //if (typeof id == "undefined" || id.indexOf("autotabs-")!=-1) {
           id = "autotabs-"+index;
           $(this).attr("id", id);
       //}
       pbGenerateTabs(id);
   });
}

function pbGenerateTabs(tabNavId) {
   var tabNav = $("#"+tabNavId);
   //console.log("tabNav=", tabNav);
   tabNav.html("");
   var tabPanes = tabNav.parent().find(".tab-pane");
   //console.log("tabPanes=", tabPanes);
   if (tabPanes.length==0) tabPanes = tabNav.parent().parent().find(".tab-pane");
   var isOneActive = false;
   tabPanes.each(function(index) {
       var title = $(this).attr("title");
       if (typeof title == "undefined" || title == "" || title == null) {
           title = $(this).find("div.pb-tab-title h3,div.pb-tab-title p").text();
           //console.log("title 1=", title);
       }
       var id = $(this).attr("id");

       //if (typeof id == "undefined" || id == "" || id == null || id.indexOf(tabNavId)==0) {
           id = tabNavId + "-" + index;
           $(this).attr("id", id);
       //}

       if (typeof title == "undefined" || title == "" || title == null) title = "Tab "+index;

       var tabHtmlCode = "<li class=\"nav-item";
       if ($(this).hasClass("active") && isOneActive==false) tabHtmlCode += " active";
       tabHtmlCode += "\"><a class=\"nav-link";
       if ($(this).hasClass("active") && isOneActive==false) tabHtmlCode += " active show";
       tabHtmlCode += "\" data-toggle=\"tab\" role=\"tab\" href=\"#"+id+"\">"+title+"</a></li>";

       if ($(this).hasClass("active")) {
           if (isOneActive==false) isOneActive = true;
           else {
               $(this).removeClass("active");
               $(this).removeClass("show");
           }
       }

       tabNav.append(tabHtmlCode);

   });
}

function pbAutoAcoordion() {
   $("div.pb-autoaccordion").each(function(index) {
       var id = $(this).attr("id");
       if (typeof id == "undefined" || id.indexOf("autoaccordion-")!=-1) {
           id = "autoaccordion-"+index;
           $(this).attr("id", id);
       }
       pbGenerateAccordion(id);
   });
}

function pbGenerateAccordion(accordionId) {
   var accordion = $("#"+accordionId);
   //console.log("accordion=", accordion);

   var cards = accordion.find("div.card, div.md-accordion-item, div.accordion-item");
   //console.log("cards=", cards);
   cards.each(function(index) {
       var card = $(this);
       //console.log("card=", card);
       var header = card.find("div.card-header, div.md-accordion-header, div.accordion-header");
       var id = accordionId+"-"+index;
       var collapseId = "collapse-"+id;

       header.attr("id", id);
       header.find(".accordionLink").attr("data-target", "#"+collapseId);

       var collapse = card.find("div.collapse");
       collapse.attr("id", collapseId);
       collapse.attr("data-parent", "#"+accordionId);

   });
}

/**
 * Niektore bloky musia mat nastaveny active, ak sa zmaze, pokazi sa vizual
 */
function pbSetActiveOnRemove(detail) {
    //console.log("pbSetActiveOnRemove, detail=", detail);
    $("div.carousel div.carousel-inner").each(function(index) {
        var carrousel = $(this);
        var items = carrousel.find("div.carousel-item.active");
        //console.log("items=", items);
        if (items.length == 0) {
            items = carrousel.find("div.carousel-item");
            //console.log("items2=", items);
            if (items.length>0) {
                $(items[0]).addClass("active");
            }
        }
    });
    $("ul.pb-autotabs").each(function(index) {
        var tabs = $(this);
        var items = tabs.find(".nav-item.active");
        if (items.length == 0) {
            items = tabs.find(".nav-item");
            if (items.length>0) {
                $("#"+tabs.attr("id")+"-0").addClass("active").addClass("show");
            } else if (items.length==0) {
                //pridaj tlacidlo na pridanie tabu
                var pb = window.pageBuilder;
                var tabContent = tabs.parents(".container").find(".tab-content");
                if (tabContent.find("."+pb.tag.empty_placeholder).length==0) {
                    tabContent.append(pb.build_aside(pb.tag.empty_placeholder, pb.build_button(pb.tag.empty_placeholder_button)));
                }
            }
        }
    });
    $("div.pb-autoaccordion").each(function(index) {
        var accordion = $(this);
        var items = accordion.find("div.card, div.md-accordion-item, div.accordion-item");
        if (items.length == 0) {
            //pridaj tlacidlo na pridanie tabu
            var pb = window.pageBuilder;
            if (accordion.find("."+pb.tag.empty_placeholder).length==0) {
                accordion.append(pb.build_aside(pb.tag.empty_placeholder, pb.build_button(pb.tag.empty_placeholder_button)));
            }
        }
    });
}