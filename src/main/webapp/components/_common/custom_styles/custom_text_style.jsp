<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(lng);
%>
// Plugin na zobrazovanie rozsirenych nastaveni textu
// 17.9.2015 LZL

function submitCustomStyle(pole){

    	var style = $("#styleFrom").val();
    	$("#"+pole+"").val(style);
    	$(".rozsirene").html("").css("display","none");
    }

    function closeCustomStyle(){
    	$(".rozsirene").html("").css("display","none");
    }

    function changeCustomStyle(){
    var fontWeight;
    var fontStyle;

    if($('#customFontWeight').is(':checked')){
   		 fontWeight="700";
    	$("label[for='customFontWeight']").css("background-image","url(/components/_common/custom_styles/images/icon_bold_selected.png)");}
    else{
    	fontWeight="normal"; $("label[for='customFontWeight']").css("background-image","url(/components/_common/custom_styles/images/icon_bold.png)");
    }



    if($('#customFontItalic').is(':checked')){
   		 fontStyle="italic";
   		 $("label[for='customFontItalic']").css("background-image","url(/components/_common/custom_styles/images/icon_italic_selected.png)");}

   		 else{
   		 fontStyle="normal";
   		 $("label[for='customFontItalic']").css("background-image","url(/components/_common/custom_styles/images/icon_italic.png)");}


    //--------------------------- align

    $('label[for="textAlignRight"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_right.png)");
    $('label[for="textAlignLeft"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_left.png)");
    $('label[for="textAlignCenter"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_center.png)");
    $('label[for="textAlignJustify"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_justify.png)");


    switch($('input[name="customTextAlign"]:checked').val()){
    	case "left":
    		$('label[for="textAlignLeft"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_left_selected.png)");
    		break;

    case "right":
    		$('label[for="textAlignRight"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_right_selected.png)");
    		break;

    case "center":
    		$('label[for="textAlignCenter"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_center_selected.png)");
    		break;

    case "justify":
    		$('label[for="textAlignJustify"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_justify_selected.png)");
    		break;
    }
    // -------------------------END align

        var outputValue ="text-align: "+$('input[name="customTextAlign"]:checked').val()+";font-style:"+fontStyle+";font-weight:"+fontWeight+";font-size:"+$('#cutomFontSize').val()+"px; margin-top: "+$('#cutomMarginTop').val()+"px; font-family:"+$('#fontFamilyForm').val()+"; margin-left:"+$('#cutomMarginLeft').val()+"px;";
    	$('#styleFrom').val(outputValue);
    }

  // presunutie boxu na spravnu poziciu

  $(document).ready(function(){
		addClickEvent();
  });

  function addClickEvent(){
  		 $(".rozsireneLink").click(function( event ) {
 		// posunutie okienka na spravne miesto kliku
 		 var offset = $( this ).offset();
 		 event.stopPropagation();
  		$(".rozsirene").offset({left:offset.left-$(".rozsirene").width(),top:offset.top});
 		 $(".rozsirene.r").offset({left:offset.left,top:offset.top});
});
  }


  // -------------------------END presunutie boxu na spravnu poziciu

    function openCustomStyle(pole){

   $(".rozsirene").html("").css("display","block");

  		//---- skryvanie okna----
  		$(document).mouseup(function (e){
     			 if (!$(".rozsirene").is(e.target)  && $(".rozsirene").has(e.target).length === 0){
       			closeCustomStyle();
  				  }
    		});

    	//---END--- skryvanie okna----

    	var oldStyle = $(pole).val();
        var s = "<div style=\""+oldStyle+"\">";
        // nacitanie inline stylu z oldStyle

        var fontSize = $(s).css("font-size");
        var textAlign = $(s).css("text-align");
        var fontWeight =  $(s).css("font-weight");
        var fontStyle =  $(s).css("font-style");

        var fontFamily =  $(s).css("font-family");
        var marginTop =  $(s).css("margin-top");
        var marginLeft =  $(s).css("margin-left");


		var outputHtml = "";

         var fontSizeForm = "<td><%=prop.getText("components.custom_style.fontSize") %>: </td><td><input style=\"width:50px;\" onkeyup=\"changeCustomStyle()\" type=\"text\" id=\"cutomFontSize\" value=\""+fontSize.slice(0, -2)+"\"></td>";
   		 var marginTopForm = "<td> <%=prop.getText("components.custom_style.marginTop") %>:</td> <td><input style=\"width:50px;\" onkeyup=\"changeCustomStyle()\" type=\"text\" id=\"cutomMarginTop\" value=\""+marginTop.slice(0, -2)+"\"></td>";
   		 var marginLeftForm = "<td><%=prop.getText("components.custom_style.marginLeft") %>:</td> <td><input style=\"width:50px;\" onkeyup=\"changeCustomStyle()\" type=\"text\" id=\"cutomMarginLeft\" value=\""+marginLeft.slice(0, -2)+"\"></td>";

   		var fontWeightForm = "<label for=\"customFontWeight\" style=\"display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_bold.png)\"></label> <input style=\"display:none\" id=\"customFontWeight\" name=\"customFontWeight\" onclick=\"changeCustomStyle()\" type=\"checkbox\" value=\"700\">";
   		var fontItalicForm = "<label for=\"customFontItalic\" style=\"display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_italic.png)\"></label><input style=\"display:none\" id=\"customFontItalic\" name=\"customFontItalic\" onclick=\"changeCustomStyle()\" type=\"checkbox\" value=\"italic\">";

   		//----------- align---

   		var alignLeft = "<label style=\"display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_align_left.png)\" for=\"textAlignLeft\"></label><input style=\"display:none\" type=\"radio\" onclick=\"changeCustomStyle()\" id=\"textAlignLeft\" name=\"customTextAlign\" value=\"left\">";
   		var alignCenter = "<label style=\"display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_align_center.png)\" for=\"textAlignCenter\"></label><input style=\"display:none\" type=\"radio\" onclick=\"changeCustomStyle()\" id=\"textAlignCenter\" name=\"customTextAlign\" value=\"center\">";
   		var alignRight = "<label style=\"display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_align_right.png)\" for=\"textAlignRight\"></label><input style=\"display:none\" type=\"radio\" onclick=\"changeCustomStyle()\" id=\"textAlignRight\" name=\"customTextAlign\" value=\"right\">";
   		var alignJustify = "<label style=\"display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_align_justify.png)\" for=\"textAlignJustify\"></label><input style=\"display:none\" type=\"radio\" onclick=\"changeCustomStyle()\" id=\"textAlignJustify\" name=\"customTextAlign\" value=\"justify\">";
   		var alignForm = alignLeft+alignCenter+alignRight+alignJustify;
		// -----------------

   		 var googleFonts = "";
   		 var fontFamilyForm="<%=prop.getText("components.custom_style.font") %><br><select onchange=\"changeCustomStyle()\" style=\"width:200px\" class=\"fontFamilyForm\" id=\"fontFamilyForm\" name=\"fontFamily\"><option style=\"font-family: Arial, Helvetica, sans-serif;\">Arial, Helvetica,sans-serif</option><option style=\"font-family: Georgia,serif;\" >Georgia,serif</option><option style=\"font-family: \'Palatino Linotype\', \'Book Antiqua\', Palatino, serif;\">\'Palatino Linotype\',\'Book Antiqua\', Palatino, serif</option><option style=\"font-family: \'Times New Roman\', Times, serif;\" >\'Times New Roman\',Times, serif</option><option style=\"font-family: \'Arial Black\', Gadget, sans-serif;\" >\'Arial Black\',Gadget, sans-serif</option><option style=\"font-family: \'Comic Sans MS\', cursive, sans-serif;\" >\'Comic Sans MS\',cursive,sans-serif</option><option style=\"font-family: Impact, Charcoal, sans-serif;\" >Impact,Charcoal,sans-serif</option><option style=\"font-family: \'Lucida Sans Unicode\', \'Lucida Grande\', sans-serif;\">\'Lucida Sans Unicode\',\Lucida Grande\',sans-serif</option><option style=\"font-family: Tahoma, Geneva, sans-serif;\">Tahoma,Geneva,sans-serif</option><option style=\"font-family: \'Trebuchet MS\', Helvetica, sans-serif;\">\'Trebuchet MS\',Helvetica,sans-serif</option><option style=\"font-family: Verdana, Geneva, sans-serif;\">Verdana,Geneva,sans-serif</option><option style=\"font-family: \'Courier New\', Courier, monospace;\">\'Courier New\',Courier,monospace</option><option style=\"font-family:\'Lucida Console\', Monaco, monospace;\">\'Lucida Console\',Monaco,monospace</option>"+googleFonts+"</select>";

         var styleForm = "<textarea style=\"width:100%;display:none; \" type=\"text\" id=\"styleFrom\" value=\""+oldStyle+"\"> </textarea>";
		var submitButton = "<br><br><a style=\"cursor:pointer; padding:5px 10px; color:#fff; background-color:#29c01a; border-radius:3px;\" onclick=\"submitCustomStyle('"+$(pole).attr("id")+"')\">OK</a>";
		var canclelButton = "<a style=\"cursor:pointer; padding:5px 10px; color:#fff; margin-left:7px; background-color:#9b9b9b; border-radius:3px;\" onclick=\"closeCustomStyle()\">Zrušiť</a><br><br>";

        outputHtml+="<strong><%=prop.getText("components.custom_style.heading") %></strong><br><br>"+fontWeightForm+fontItalicForm+alignForm+"<br><table><tr>"+fontSizeForm+"</tr><tr>"+marginTopForm+"</tr><tr>"+marginLeftForm+"</tr></table>"+fontFamilyForm+"<br>"+styleForm+submitButton+canclelButton;

        $(".rozsirene").attr("style","z-index:100; border: 1px solid #afafaf; padding: 10px; position: absolute; background-color:#fff");

        $(".rozsirene").html(outputHtml);


        $('.rozsirene #fontFamilyForm option').filter(function() {
            return ($(this).text().replace(/\'/g,"\"").replace(/\, /g,",") == fontFamily);
        }).prop('selected', true);


        $('.rozsirene #customAlign option').filter(function() {
            return ($(this).val() == textAlign);
        }).prop('selected', true);

        if(fontWeight=="700"){$('#customFontWeight').prop('checked', true); $("label[for='customFontWeight']").css("background-image","url(/components/_common/custom_styles/images/icon_bold_selected.png)");}
        else{$('#customFontWeight').prop('checked', false);$("label[for='customFontWeight']").css("background-image","url(/components/_common/custom_styles/images/icon_bold.png)");}

        if(fontStyle=="italic"){$('#customFontItalic').prop('checked', true); $("label[for='customFontItalic']").css("background-image","url(/components/_common/custom_styles/images/icon_italic_selected.png)");}else{
        $('#customFontStyle').prop('checked', false);
        $("label[for='customFontItalic']").css("background-image","url(/components/_common/custom_styles/images/icon_italic.png)")
        }

        //----------------align

            switch(textAlign){
    	case "left":
    		$('label[for="textAlignLeft"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_left_selected.png)");
    		break;

    case "right":
    		$('label[for="textAlignRight"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_right_selected.png)");
    		break;

    case "center":
    		$('label[for="textAlignCenter"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_center_selected.png)");
    		break;

    case "justify":
    		$('label[for="textAlignJustify"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_justify_selected.png)");
    		break;
        }
        //------------END align

        var outputValue ="font-style:"+$('#customFontItalic').val()+"; font-size:"+$('#cutomFontSize').val()+"px; margin-top: "+$('#cutomMarginTop').val()+"px; font-family:"+$('#fontFamilyForm').val()+"; margin-left:"+$('#cutomMarginLeft').val()+"px; text-align:"+$('#customAlign').val();
    	$('#styleFrom').val(outputValue);
    }
