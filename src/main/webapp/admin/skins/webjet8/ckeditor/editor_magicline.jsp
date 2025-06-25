<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="menuWebpages"/>

<style type="text/css">

	#webjetMagicLine
	{
		position: absolute; top: 0px; left: 3px; height: 11px !important; overflow: hidden; width: 98%; margin:0xp; padding: 0px; z-index:9999;
		cursor: pointer;
		background-position: center 4px;
		background-repeat: no-repeat;
		text-decoration: none;
		background-image: url(/admin/skins/webjet8/assets/global/img/magicline.png);
		background-position: right 0px;
		height: 20px !important;
	}
	a.magicLineHover
	{
		text-decoration: none;
	}

	#webjetComponentsToolbar, #webjetMagicLineToolbar
	{
		position: absolute; top: 0px; left: 15px; height: 21px; _height: 23px; overflow: hidden; margin:0xp; z-index:9999;
		background-image: url(/components/_common/admin/inline/toolbarbg.png);
		background-position: left top;
		background-repeat: repeat-x;
		text-decoration: none;
		border: 1px solid #e86678;
		border-radius: 3px;
		box-shadow: 6px 8px 5px 1px #888888;
	}
	#webjetMagicLineToolbar
	{
		height: 32px;
		left: auto;
		right: 18px;
		background-color: #e1e1e1;
		border: 0px;
		box-shadow: none;
	}
	#webjetComponentsToolbar span
	{
		font-size: 11px;
	   font-family: Tahoma, Arial, Verdana, Sans-Serif;
	   color: black;

		background:url(/admin/FCKeditor/editor/skins/webjet/images/components_toolbar_sprite.png) no-repeat top left;

		cursor: pointer;
		margin-left: 4px;
		margin-right: 4px;
		padding-top: 3px;
		padding-left: 18px;

		display: block;
		width: auto;
		height: 21px;
		float: left;
	}

	#webjetMagicLineToolbar span.mlBtn
	{
		display: block;
		height: 32px;
		float: left;
		text-align: center;
		font-size: 12px;
	   font-family: Tahoma, Arial, Verdana, Sans-Serif;
	   color: #e86678;
	   margin: 0px;
	   padding-left: 10px;
	   padding-right: 10px;
	   padding-top: 9px;
	   border-right: 1px solid #bababa;
	   font-weight: bold;
	}
	#webjetMagicLineToolbar span.mlBtn:hover
	{
		background-color: #dff1ff;
		color: 316ac5;
		cursor: pointer;
	}
	#webjetMagicLineToolbar span.mlBtn.disabled, #webjetMagicLineToolbar span.mlBtn.disabled:hover
	{
		color: black;
		background-color: transparent;
		cursor: default;
		font-weight: normal;
	}

	#webjetSectionToolbar
	{
		position: absolute; top: 0px; left: 15px; height: 21px; _height: 23px; overflow: hidden; margin:0xp; z-index:9999;
		background-image: url(/components/_common/admin/inline/toolbarbg.png);
		background-position: left top;
		background-repeat: repeat-x;
		text-decoration: none;
		border: 1px solid #e86678;
		border-radius: 3px;
		box-shadow: 6px 8px 5px 1px #888888;
	}
	#webjetSectionToolbar span
	{
		font-size: 11px;
	   font-family: Tahoma, Arial, Verdana, Sans-Serif;
	   color: black;

		background:url(/admin/FCKeditor/editor/skins/webjet/images/components_toolbar_sprite.png) no-repeat top left;

		cursor: pointer;
		margin-left: 4px;
		margin-right: 4px;
		padding-top: 3px;
		padding-left: 18px;

		display: block;
		width: auto;
		height: 21px;
		float: left;

	}

</style>

<a id="webjetMagicLine" title="<iwcm:text key="editor.magicline.clickToAddParagraph"/>" class="magicLine" onmouseup="webjetMagicLineClick()" style="display: none;" onmouseover="this.className='magicLineHover';" onmouseout="this.className='magicLine'"></a>
<a id="webjetSectionToolbar" class="componentsToolbar"  style="display: none;"><span onclick="webjetSectionToolbarClick('edit')" style="background-position: 0 -132px;"><iwcm:text key="editor.div.editSection"/></span></a>
<div id="webjetMagicLineToolbar" class="componentsToolbar" style="display: none;">
	<span class="mlBtn disabled"><iwcm:text key="button.add"/></span>
	<span class="mlBtn" onclick="webjetMagicLineToolbarClick('text')"><iwcm:text key="calendar_edit.text"/></span>
	<span class="mlBtn" onclick="webjetMagicLineToolbarClick('image')"><iwcm:text key="editor.perex.image"/></span>
	<span class="mlBtn" onclick="webjetMagicLineToolbarClick('app')"><iwcm:text key="editor.magicline.app"/></span>
	<span class="mlBtn" onclick="webjetMagicLineToolbarClick('template')"><iwcm:text key="editor.magicline.template"/></span>
</div>
