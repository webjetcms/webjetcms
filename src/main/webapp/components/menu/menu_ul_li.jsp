<%@page import="sk.iway.iwcm.tags.WriteTag"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.components.menu.MenuULLI" %><%
PageParams pageParams = new PageParams(request);
int rootGroupId = pageParams.getIntValue("rootGroupId", 1);

//ak je nastavene na true menu sa do stranky nevypise, ale sa do requestu iba
//nastavi polozka !UL_LI_MENU!, to je mozne neskor vypisat
//pouziva sa ak treba pred menu vypisat !ROOT_GROUP_NAME!
//najskor sa musi menu vygenerovat, potom vypisat !ROOT_GROUP_NAME! a potom az vypisat !UL_LI_MENU!
boolean onlySetVariables = pageParams.getBooleanValue("onlySetVariables", false);

//ak je nastavene na true, vsetky polozky menu budu vygenerovane
//pouziva sa napr. na horne vysuvne menu
boolean openAllItems = pageParams.getBooleanValue("openAllItems", false);
if (openAllItems)
{
   request.setAttribute("ulLiOpenAllItems", "true");
}

else
{
   request.removeAttribute("ulLiOpenAllItems");
}

//ked je nastavene, menu sa sprava ako keby bolo otvorene na pozadovanom docid
int ulLiForceDocid = pageParams.getIntValue("forceDocid", -1);
if (ulLiForceDocid != -1)
{
   request.setAttribute("ulLiForceDocid", ""+ulLiForceDocid);
}
else
{
   request.removeAttribute("ulLiForceDocid");
}

//tu je mozne zadat zoznam znakov, ktore sa budu generovat do menu do SPAN elementu
//generateEmptySpan musi byt true
//je mozne ich zadat vo formate znakLevel1,znakLevel2,znakLevel3
//cize napr. spanCharacters=>,&raquote;
//ak je level vacsi ako zadany pocet poloziek, pouzije sa posledna hodnota
String spanCharacters = pageParams.getValue("spanCharacters", "");
if (Tools.isNotEmpty(spanCharacters))
{
	request.setAttribute("ulLiSpanCharacters", Tools.replace(spanCharacters.replace('+', ','), " ", "&nbsp;"));
}
else
{
	request.removeAttribute("ulLiSpanCharacters");
}

//ak je nastavene na true, bude sa do kazdej menu polozky generovat prazdny span
//napr. <li...><span></span><a....>text</a></li>
boolean generateEmptySpan = pageParams.getBooleanValue("generateEmptySpan", false);
if (generateEmptySpan)
{
   request.setAttribute("ulLiGenerateEmptySpan", "true");
}
else
{
   request.removeAttribute("ulLiGenerateEmptySpan");
}



//spatna kompatibilita (volakedy tam davalo groupId)
int groupId = pageParams.getIntValue("groupId", -1);
if (groupId>0)
{
   rootGroupId = groupId;
}

//o kolko sa od root adresara posunieme - pre sekcne menu treba zadat 1 (level je root+1)
int startOffset = pageParams.getIntValue("startOffset", -1);

//maximalna hlbka, ktora sa bude generovat
int maxLevel = pageParams.getIntValue("maxLevel", -1);

//minimalny pocet poloziek, ktore sa vygeneruju na urovni 1 (ak to musi byt kvoli dizajnu)
int ulLiMinLiItems = pageParams.getIntValue("minLiItems", -1);
if (ulLiMinLiItems > 0)
{
   request.setAttribute("ulLiMinLiItems", ""+ulLiMinLiItems);
}
else
{
   request.removeAttribute("ulLiMinLiItems");
}

//generovane triedy = none, full
String classes = pageParams.getValue("classes", "full");

//ID root UL elementu
String rootUlId = pageParams.getValue("rootUlId", null);

String vypis = MenuULLI.doTree(rootGroupId, maxLevel, startOffset, classes, rootUlId, request);

if (onlySetVariables)
{
	request.setAttribute("!UL_LI_MENU!", vypis);
}
else
{
	if (vypis.indexOf("&amp;#47;")!=-1)
	{
		vypis = Tools.replace(vypis, "&amp;#47;", "/");
	}
	
	out.println(vypis);
}
%>
<%
WriteTag.setInlineComponentEditTextKey("components.menu.editMenu", request);
%>