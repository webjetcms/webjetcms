package sk.iway.iwcm.doc;

import java.io.File;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import sk.iway.iwcm.Tools;

/**
 *  XmlExport.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 12.2.2006 21:54:05
 *@modified     $Date: 2007/09/07 13:39:29 $
 */
public class XmlExport
{

	private static Element getTextNode(Document document, String name, String value)
	{
		Element e = document.createElement(name);
		Node n = document.createTextNode(name);
		n.setNodeValue(value);
		e.appendChild(n);
		return(e);
	}

	private static Element getCDataNode(Document document, String name, String value)
	{
		Element e = document.createElement(name);
		Node n = document.createCDATASection(name);
		n.setNodeValue(value);
		e.appendChild(n);
		return(e);
	}
}
