<%@ page pageEncoding="utf-8" contentType="text/javascript" import="sk.iway.iwcm.*,sk.iway.iwcm.stat.*" %><%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
	sk.iway.iwcm.PathFilter.setStaticContentHeaders("/cache/common.js", null, request, response);
%>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true" perms="menuWebpages"/>

/*
 * FCKeditor - The text editor for internet
 * Copyright (C) 2003-2004 Frederico Caldeira Knabben
 *
 * Licensed under the terms of the GNU Lesser General Public License:
 * 		http://www.opensource.org/licenses/lgpl-license.php
 *
 * For further information visit:
 * 		http://www.fckeditor.net/
 *
 * File Name: fck_dialog_common.js
 * 	Useful functions used by almost all dialog window pages.
 *
 * Version:  2.0 RC1
 * Modified: 2004-11-23 17:21:15
 *
 * File Authors:
 * 		Frederico Caldeira Knabben (fredck@fckeditor.net)
 */

// Gets a element by its Id. Used for shorter coding.
function GetE( elementId )
{
	return document.getElementById( elementId )  ;
}

function ShowE( element, isVisible )
{
	if ( typeof( element ) == 'string' )
   {
     //window.alert(element);
	  element = GetE( element ) ;
   }
   if (element == null)
   {
      //window.alert("element je null" + element);
      return;
   }
	element.style.display = isVisible ? '' : 'none' ;
}

//WebJET - nastavi class combo pre zadanu hodnotu - ak v optionoch nie je definovana prida ju
function SetClassCombo(elementName, value)
{
	var select = GetE(elementName);
	if (select != null && select.options)
	{
		for (i=0; i < select.options.length; i++)
		{
			if (select.options[i].value == value)
			{
				select.selectedIndex = i;
				return;
			}
		}

		var o = new Option(value, value, true, true);
		select.options[select.options.length] = o;
	}
}

function SetAttribute( element, attName, attValue )
{
   if (!element || element == undefined)
   {
      return;
   }

   //element.removeAttribute(attName, 0) ;
   //window.alert("removed: " + attName + "=" + element.getAttribute( attName ) );

   if (attName.indexOf("on")==0)
	{
		attValue = attValue.replace(/'/gi, "\"");
		var atrName = attName+"_fckprotectedatt";
		var atrValue = " "+attName+"='"+attValue+"'";

		element.setAttribute(atrName, atrValue);
		return;
	}

   //skus to nastavit ako CSS atribut
   var attCssName = getAttCssName(attName);
   //window.alert("attName="+attName+"css="+attCssName);
   if (attCssName != null)
   {
      try
      {
          if (attName=="align" && element.tagName=="IMG")
          {
             if (attValue=="left" || attValue=="right" || attValue=="")
 			 {
 			    if (navigator.userAgent.indexOf("Gecko")!=-1) attCssName = "cssFloat";
 			    else attCssName = "styleFloat";
 			 }
 			 else
 			 {
 			    attCssName = "verticalAlign";
 			 }
          }

          var cssAttValue = attValue;
          if ( attValue == null || attValue.length == 0 )
          {
             cssAttValue = "";
          }
          var evalCmd = "element.style."+attCssName+"='"+cssAttValue+"';";
		  eval(evalCmd);

		  //WebJET - uprava zarovnania CSS stylu obrazku na Ziadne (ak pred tym bolo nejake vertical)
		  if (attName=="align" && element.tagName=="IMG" && attValue=="")
		  {
			  evalCmd = "element.style.verticalAlign='';";
			  eval(evalCmd);
		  }

		  try
		  {
		  	element.removeAttribute( attName, 0 ) ;
		  }
		  catch (exc2)
		  {

		  }
		  return;
	  }
	  catch (exception)
	  {
	     //window.alert("ERROR: "+exception+" eval=["+evalCmd+"] attName="+attName+" attCssName="+attCssName+" value="+cssAttValue);
	  }
   }
   try
   {
      if ( attValue == null || attValue.length == 0 )
			element.removeAttribute( attName, 0 ) ;			// 0 : Case Insensitive
		else
	   {
	      //window.alert("set atr: " + attName + " = " + attValue);
			element.setAttribute( attName, attValue, 0) ;	// 0 : Case Insensitive
	   }
   }
   catch (ex3)
   {

   }

}

function GetAttribute( element, attName, valueIfNull )
{
   if (!valueIfNull)
   {
      valueIfNull = "";
   }

   if (attName.indexOf("on")==0)
	{
		//ziskaj protected hodnotu
		var value = element.getAttribute(attName+"_fckprotectedatt");
		if (value == null) return valueIfNull;
		value = value.substring(name.length+3, value.length-1);
		return value;
	}

   var attCssName = getAttCssName(attName);
   if (attCssName != null)
   {
      if (attName=="align" && element.tagName=="IMG")
      {
         if (navigator.userAgent.indexOf("Gecko")!=-1) attCssName = "cssFloat";
         else attCssName = "styleFloat";
      }

      var value = null;
	  //window.alert("getAttr CSS: "+attName+" css="+attCssName);
	  eval("value=element.style."+attCssName+";");
	  if (value != null && value.length>0)
	  {
	  	return(value);
	  }

	  if (attName=="align" && element.tagName=="IMG")
      {
         attCssName = "verticalAlign";
         //je mozne, ze to mame ulozene ako vertical-align
         eval("value=element.style."+attCssName+";");
		 if (value != null && value.length>0)
		 {
		    return(value);
		 }
      }
   }

   try
   {
		var oAtt = element.attributes[attName] ;
		if ( oAtt == null || !oAtt.specified ) return valueIfNull ;
		else return oAtt.value;
   } catch (ex) {}

	var oValue = element.getAttribute( attName, 2 ) ;

	return ( oValue == null ? valueIfNull : oValue ) ;
}

//jeeff: nastavenie CSS atributu, tento zoznam atributov treba mat rovnaky aj vo fckxhtml.js
var attNames = new Array(   "align",     "valign",        "valign",        "border", "width", "height", "bgcolor",         "bordercolor");
var attCssNames = new Array("textAlign", "verticalAlign", "verticalAlign", "border", "width", "height", "backgroundColor", "borderColor");
function getAttCssName(attName)
{
   attName = attName.toLowerCase();
   for (i=0; i<=attNames.length; i++)
   {
      if (attNames[i]==attName)
      {
         return(attCssNames[i]);
      }
   }
   return(null);
}

function getAttName(attCssName)
{
   attCssName = attCssName();
   for (i=0; i<=attCssNames.length; i++)
   {
      if (attCssNames[i]==attCssName)
      {
         return(attName[i]);
      }
   }
   return(null);
}


function setCssAttribute(element, cssName, attValue)
{
   if (attValue == null || attValue.length == 0 || attValue=="%" || attValue=="px")
   {
      eval("element.style."+cssName+"=null;");
   }
   else
   {
   	  eval("element.style."+cssName+"='"+attValue+"';");
   }
}

//jeeff: skopirovanie atributu (ak je definovany)
function getWjAtr(node, name)
{
	if (name.indexOf("on")==0)
	{
		//ziskaj protected hodnotu
		var value = node.getAttribute(name+"_fckprotectedatt");
		if (value == null) return "";
		value = value.substring(name.length+3, value.length-1);
		return value;
	}
	if (name=="style")
	{
		return node.style.cssText;

	}
	//Firefox/3
	if (name=="className" && navigator.userAgent.indexOf("Firefox/3")!=-1)
	{
		name = "class";
	}
	var value = node.getAttribute(name, 2);
	if (value == null) value = "";
	return value;
}

function setWjAtr(node, name, value)
{
	if (value != "")
	{
		if (name=="style")
		{
			node.style.cssText = value;
		}
		else
		{
			if (name.indexOf("on")==0)
			{
				//value = value.replace(/"/gi, "&quot;");
				value = value.replace(/'/gi, "\"");
				var atrName = name+"_fckprotectedatt";
				var atrValue = " "+name+"='"+value+"'";

				node.setAttribute(atrName, atrValue);
			}
			else
			{
				//Firefox/3
				if (name=="className" && navigator.userAgent.indexOf("Firefox")!=-1)
				{
					node.setAttribute("class", value);
				}
				else
				{
					node.setAttribute(name, value);
					node.setAttribute("class", value);
				}
			}
		}
	}
	else
	{
		node.removeAttribute(name);
	}
}

function copyWjAtr(src, dest, name)
{
	var value = getWjAtr(src, name);
	if (value != null && value != "") setWjAtr(dest, name, value);
}

//jeeff: ziskanie CSS atributu, ak nie je HTML atributu
//UZ BY SA NEMALO POUZIVAT, STACI VOLAT getAttribute
function getAttributeCss(element, attName, attCssName, valueIfNull)
{
  var value = null;
  window.alert("getCssAttr: "+attName+" css="+attCssName);
  eval("value=element.style."+attCssName+";");
  if (value == null || value.length==0)
  {
     value = GetAttribute(element, attName, valueIfNull);
  }
  return(value);
}

String.prototype.startsWith = function( value )
{
	return ( this.substr( 0, value.length ) == value ) ;
}

String.prototype.remove = function( start, length )
{
	var s = '' ;

	if ( start > 0 )
		s = this.substring( 0, start ) ;

	if ( start + length < this.length )
		s += this.substring( start + length , this.length ) ;

	return s ;
}

// Functions used by text fiels to accept numbers only.
function IsDigit( e )
{
   e = e || event ;
   var iCode = ( e.keyCode || e.charCode ) ;

   event.returnValue =
      (
         ( iCode >= 48 && iCode <= 57 )      // Numbers
         || (iCode >= 37 && iCode <= 40)     // Arrows
         || iCode == 8                 // Backspace
         || iCode == 46                // Delete
      ) ;

   return event.returnValue ;
}

String.prototype.trim = function()
{
   return this.replace( /(^\s*)|(\s*$)/g, '' ) ;
}

// ---- WebJET ----
function setDialogTitle(text)
{
   var name = "TitleArea";
   var titleElement = GetE(name);
   if (titleElement == null && window.parent)
   {
      titleElement = window.parent.document.getElementById(name);
   }
   if (titleElement != null)
   {
      titleElement.innerHTML = text;
   }
   document.title = text;
   if (window.parent)
   {
      window.parent.document.title = text;
   }
}

function CreateElement( tag, atrs, parentNode )
{
   var e = null;

   if (navigator.userAgent.toLowerCase().indexOf("msie")!=-1) e = FCK.EditorDocument.createElement("<"+tag+" "+atrs+">") ;
   else e = FCK.EditorDocument.createElement(tag) ;

   e.setAttribute( '__FCKTempLabel', '1' ) ;

   //window.alert("parentNode="+parentNode.cellIndex+" tag="+tag+" atrs="+atrs);

   if (parentNode != undefined && parentNode != null) parentNode.appendChild( e ) ;
   else FCK.InsertElement( e ) ;

   var aEls = FCK.EditorDocument.getElementsByTagName( tag ) ;

   for ( i = 0 ; i < aEls.length ; i++ )
   {
      if ( aEls[i].attributes['__FCKTempLabel'] )
      {
         aEls[i].removeAttribute( '__FCKTempLabel' ) ;
         return aEls[i] ;
      }
   }
}


//WebJET - odstranenie diakritiky
function internationalToEnglish(text)
{
  var Diacritic =   "áäčďéěíĺľňóôőöŕšťúůűüýřžÁÄČĎÉĚÍĹĽŇÓÔŐÖŔŠŤÚŮŰÜÝŘŽ ";
  var DiacRemoved = "aacdeeillnoooorstuuuuyrzAACDEEILLNOOOORSTUUUUYRZ-";

  var ptext=""; // pomocná proměnná
  for(i=0;i < text.length;i++)
  {// projít zadaný text po znaku
    if (Diacritic.indexOf(text.charAt(i))!=-1) // pokud je znak v textu obsažen v řetezci Diacritic
      ptext+=DiacRemoved.charAt(Diacritic.indexOf(text.charAt(i))); // předat do pomocného řetězce znak z pole DiacRemoved
    else
      ptext+=text.charAt(i); // jinak předat původní znak
  }
  return ptext;
}

//WebJET - ponecha len safe znaky
function removeChars(text)
{
  var safeChars =   "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_.";

  var ptext="";
  for(i=0;i < text.length;i++)
  {
    if (safeChars.indexOf(text.charAt(i))==-1)
      ptext+="-";
    else
      ptext+=text.charAt(i);
  }

  //ponahradzuj
  ptext = ptext.replace(/---/gi, "-");
  ptext = ptext.replace(/--/gi, "-");
  ptext = ptext.replace(/___/gi, "_");
  ptext = ptext.replace(/__/gi, "_");
  ptext = ptext.replace(/_-_/gi, "-");
  ptext = ptext.replace(/-_/gi, "-");
  ptext = ptext.replace(/_-/gi, "-");
  ptext = ptext.replace(/-_-/gi, "-");
  ptext = ptext.replace(/--/gi, "-");
  ptext = ptext.replace(/--/gi, "-");
  ptext = ptext.replace(/--/gi, "-");
  ptext = ptext.replace(/--/gi, "-");
  ptext = ptext.replace(/__/gi, "_");
  ptext = ptext.replace(/__/gi, "_");
  ptext = ptext.replace(/__/gi, "_");
  ptext = ptext.replace(/__/gi, "_");

  return ptext;
}

function removeSpojky(text)
{
	try
	{
	<%
	if (Constants.getBoolean("urlRemoveSpojky"))
	{
		String spojky[] = Tools.getTokens(Constants.getString("urlRemoveSpojkyList"), ",");
		for (String spojka : spojky)
		{
			spojka = Tools.replace(spojka, ".", "\\.");
			%>
			text = text.replace(/-<%=spojka%>-/gi, "-");
			text = text.replace(/-<%=spojka%>\//gi, "/");
			<%
		}
	}
	%>
	}
	catch (ex)
	{

	}
	return text;
}

function formInsertLabel(name, forField, oCurrentLabel, className, parentNode)
{
	if (oCurrentLabel == null)
	{
		oCurrentLabel = CreateElement("LABEL", "for='"+forField+"'", parentNode);
	}
	if (oCurrentLabel!=null)
	{
		oCurrentLabel.innerHTML = name;

		SetAttribute(oCurrentLabel, 'for', forField);
		oCurrentLabel.htmlFor = forField;

		if (className != undefined && className != null) SetAttribute(oCurrentLabel, 'class', className);
	}
}

function formGetLabel(forField)
{
   var oEditor = FCK.EditorDocument.body;
   var labels = oEditor.getElementsByTagName("LABEL");
   for (i=0; i < labels.length; i++)
   {
   	if (labels[i].htmlFor == forField) return labels[i];
   }
   return null;
}

function getInnerText(elem)
{
	var hasInnerText = (document.getElementsByTagName("body")[0].innerText != undefined) ? true : false;

	if(!hasInnerText)
	{
	    return elem.textContent;
	}

	return elem.innerText;
}

var oLeftTD = null;
var oRightTD = null;
var oActualTD = null;
//inicializuje odkazy na lavu a pravu bunku tabulky pre vlozenie input poli formularov
function formInitializeLeftRightTD()
{
	var oActualNode = FCK.Selection.getWJActualNode();
	var oTD = oEditor.FCKTools.GetElementAscensor( oActualNode, 'TD' ) ;
	var oTR = oEditor.FCKTools.GetElementAscensor( oActualNode, 'TR' ) ;

	if (oTD != null && oTR != null)
	{
		oActualTD = oTD;
		if (oTD.cellIndex == 0)
		{
			oLeftTD = oTD;
			oRightTD = oTR.cells[1];
		}
		else
		{
			oLeftTD = oTR.cells[oTD.cellIndex-1];
			oRightTD = oTD;
		}
	}
}

//mapovanie filedov v iframe okne dialogu na fck hodnoty
function refreshValuesFromCk()
{
	var i = 0;
	for (i=0; i < wjCkMapping.length; i++)
	{
        var value = window.parent.CKEDITOR.dialog.getCurrent().getValueOf(wjCkMapping[i].tab, wjCkMapping[i].ck);
        try
        {
            //pre linku dogenerujme aj protokol
            if ("link"==window.parent.CKEDITOR.dialog.getCurrent().getName() && "txtUrl"==wjCkMapping[i].wj && value!="")
            {
                var protocol = window.parent.CKEDITOR.dialog.getCurrent().getValueOf(wjCkMapping[i].tab, "protocol");
                //console.log("protocol: " + protocol );
                if (protocol.indexOf("http")!=-1) value = protocol + value;
            }
        }
        catch (e) { }

		GetE(wjCkMapping[i].wj).value = value;
	}
}

function updateValuesToCk()
{
	var dialog = window.parent.CKEDITOR.dialog.getCurrent();

	var i = 0;
	for (i=0; i < wjCkMapping.length; i++)
	{
		dialog.getContentElement(wjCkMapping[i].tab, wjCkMapping[i].ck).setValue(GetE(wjCkMapping[i].wj).value);
	}
}

function updateValueToCk(wj)
{
	var dialog = window.parent.CKEDITOR.dialog.getCurrent();

	var i = 0;
	for (i=0; i < wjCkMapping.length; i++)
	{
		if (wj == wjCkMapping[i].wj)
		{
			dialog.getContentElement(wjCkMapping[i].tab, wjCkMapping[i].ck).setValue(GetE(wjCkMapping[i].wj).value);

			//call onkeyup event on ck field
			if (wj == "txtWidth" || wj == "txtHeight")
			{
				var inputElement = dialog.getContentElement(wjCkMapping[i].tab, wjCkMapping[i].ck).getInputElement().$;
				dialog.getContentElement(wjCkMapping[i].tab, wjCkMapping[i].ck).fire("keyUp");

				var keyboardEvent = window.parent.document.createEvent("KeyboardEvent");
				var initMethod = typeof keyboardEvent.initKeyboardEvent !== 'undefined' ? "initKeyboardEvent" : "initKeyEvent";
				keyboardEvent[initMethod](
				                   "keyup", // event type : keydown, keyup, keypress
				                    true, // bubbles
				                    true, // cancelable
				                    window.parent, // viewArg: should be window
				                    false, // ctrlKeyArg
				                    false, // altKeyArg
				                    false, // shiftKeyArg
				                    false, // metaKeyArg
				                    40, // keyCodeArg : unsigned long the virtual key code, else 0 - 40 = key down
				                    0 // charCodeArgs : unsigned long the Unicode character associated with the depressed key, else 0
				);
				inputElement.dispatchEvent(keyboardEvent);
			}
		}
	}
	refreshValuesFromCk();
}

function setChangeHandlerToUpdateCk()
{
	var i = 0;
	for (i=0; i < wjCkMapping.length; i++)
	{
		GetE(wjCkMapping[i].wj).addEventListener("change", function(event)
		{
			updateValueToCk(event.target.getAttribute("id"));
		});

		//IE fix
        GetE(wjCkMapping[i].wj).addEventListener("keyup", function(event)
        {
            updateValueToCk(event.target.getAttribute("id"));
        });

        //IE fix pre paste
        GetE(wjCkMapping[i].wj).addEventListener("paste", function(event)
        {
            window.setTimeout( function()
            {
                updateValueToCk(event.target.getAttribute("id"));
            }, 100);
        });
	}
}
