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
	public static boolean flashExport(String fileURL, int docId)
	{
		boolean ret = false;

		DocDetails doc = DocDB.getInstance().getDoc(docId);

		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			DOMImplementation impl = builder.getDOMImplementation();
			Document document = impl.createDocument(null, "webpage", null);
			Element root = document.getDocumentElement();
			//Document document = builder.newDocument();
			//Element root = (Element)base.appendChild(document.createElement("webpage"));
			root.appendChild(getTextNode(document, "title", doc.getTitle()));
			root.appendChild(getCDataNode(document, "perex", doc.getHtmlData()));
			root.appendChild(getTextNode(document, "perexImage", doc.getPerexImage()));

			String txtData = doc.getData(); // SearchAction.htmlToPlain(data);
			txtData = txtData.replace('\n', ' ');
			txtData = txtData.replace('\r', ' ');
			txtData = Tools.replace(txtData, "<STRONG>", "<B>");
			txtData = Tools.replace(txtData, "</STRONG>", "</B>");
			txtData = Tools.replace(txtData, "<EM>", "<I>");
			txtData = Tools.replace(txtData, "</EM>", "</I>");

			root.appendChild(getCDataNode(document, "body", txtData));

			//zapis atributy (ak nejake mame)
			List<AtrBean> atrList = AtrDB.getAtributes(docId, null, null);
			if (atrList.size()>0)
			{
				Element attributes = document.createElement("attributes");
				for (AtrBean atr : atrList)
				{
					if (Tools.isNotEmpty(atr.getValue()) && atr.getDocId()>0)
					{
						Element e = document.createElement("attribute");
						Node n = document.createCDATASection("attribute");
						n.setNodeValue(atr.getValue());
						e.appendChild(n);
						e.setAttribute("name", atr.getAtrName());
						e.setAttribute("atrId", Integer.toString(atr.getAtrId()));

						attributes.appendChild(e);
					}
				}
				root.appendChild(attributes);
			}

			Source source = new DOMSource(document);
			File f = new File(Tools.getRealPath(fileURL));
			if (f.getParentFile().exists()==false) if(f.getParentFile().mkdirs() == false) return false;

			Result result = new StreamResult(f);
         // Write the DOM document to the file
         Transformer xformer = TransformerFactory.newInstance().newTransformer();
         xformer.transform(source, result);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		return(ret);
	}

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
