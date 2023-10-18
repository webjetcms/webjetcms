package sk.iway.iwcm;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmInputStream;
import sk.iway.iwcm.io.IwcmOutputStream;

/**
 *  XmlUtils.java - pomocne metody pre pracu s XML
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: kmarton $
 *@version      $Revision: 1.6 $
 *@created      Date: 27.2.2008 15:45:28
 *@modified     $Date: 2010/02/11 13:55:35 $
 */
public class XmlUtils {

	protected XmlUtils() {
		//utility class
	}

	public static String getAttribute(Node node, String attrname) {
		if( node == null || attrname == null ) return null;
		NamedNodeMap ats = node.getAttributes();
		if( ats == null ) return null;
		for( int i = 0; i< ats.getLength(); i++ ) {
			Node n = ats.item(i);
			if( attrname.equalsIgnoreCase(n.getNodeName()) ) return n.getNodeValue();
		}
		return null;
	}



	public static String getText(Node node, boolean concatenate)
	{
		if (node == null)
			return null;
		Vector<Node> nl = getChildNodes(node,Node.TEXT_NODE);
		if (nl.size()==0)
			return null;

		StringBuilder result = new StringBuilder();
		for(int i=0;i<nl.size()-1;i++)
			result.append(((Text)nl.get(i)).getNodeValue()).append(concatenate ? ' ' : '\n');
		result.append(((Text)nl.get(nl.size()-1)).getNodeValue());
		return result.toString();
	}

	/*
		Gets a node's text or returns the defaultValue if the node doesn't have any text child
	*/
	  public static String getText(Node node) {
		  return getText(node, null, true);
	  }


	public static String getText(Node node, String defaultValue) {
		String retVal = getText(node,null,true);
		return retVal == null ? defaultValue : retVal;
	}

	public static String getText(Node node, String defaultValue, boolean concatenate) {
		String retVal = getText(node, concatenate);
		return retVal == null ? defaultValue : retVal;
	}

	/*
		Gets a node's CDATA child nodes (concatenated)
	*/
	public static String getCDATA(Node node)
	{
		if (node == null)
			return null;
		Vector<Node> nl = getChildNodes(node,Node.CDATA_SECTION_NODE);
		if (nl.size()==0)
			return null;

		StringBuilder result = new StringBuilder();
		for(int i=0;i<nl.size();i++)
		{
			result.append(((CDATASection)nl.get(i)).getNodeValue());
		}
		return result.toString();
	}
	/*
		Appends a Text to a Node
	*/
	public static Text addText(Node node,String text)
	{
		Document doc = node.getOwnerDocument();
		Text tnode = doc.createTextNode(text);
		node.appendChild(tnode);
		return tnode;
	}
	/*
		Appends a CDATA to a Node
	*/
	public static void addCDATA(Node node,String text)
	{
		Document doc = node.getOwnerDocument();
		CDATASection cdata = doc.createCDATASection(text);
		node.appendChild(cdata);
	}
	/*
		Removes the node's Text children and sets a new one
	*/
	public static Text setText(Node node,String text)
	{
		if (node==null) {
			return null;
		} else if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
			node.setNodeValue(text);
		}
		Text tnode = addText(node,text);
		return tnode;
	}
	/*
		Removes the node's CDATA children and sets a new one
	*/
	public static void setCDATA(Node node,String text)
	{
		removeChildNodes(node,Node.CDATA_SECTION_NODE);
		addCDATA(node,text);
	}

	public static double getDouble(Node node, double defaultValue) {
		String text = getText(node);
		if(text == null) return defaultValue;
		return Double.parseDouble(text);
	}

	public static int getInt(Node node, int defaultValue) {
		String text = getText(node);
		if(text == null) return defaultValue;
		return Integer.parseInt(text);
	}

	/*
		Get the node's childnodes with the specified type
		// Result can be casted to a class implementing the type without ClassCastException
	*/
	public static Vector<Node> getChildNodes( Node parent, short type )
	{
		Node node;
		Vector<Node> result  = new Vector<>();
		for( node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if( node.getNodeType() == type ) {
				result.add( node );
			}
		}
		return( result );
	}
	/*
		Removes the node's childnodes with the specified nodeType
	*/
	public static void removeChildNodes(Node node,short type)
	{
		if (node==null)
			return;
		NodeList nl = node.getChildNodes();
		if ( nl == null )
			return;
		int i=0;
		while (i<(nl=node.getChildNodes()).getLength())
		{
			Node n = nl.item(i);
			if (n.getNodeType()==type)
			{
				node.removeChild(n);
			}
			else
				i++;
		}
	}

	/*
		Removes all the node's childnodes
		Even the text nodes
	*/
	public static void removeAllChildren(Node node)
	{
		NodeList nl;
		while ((nl=node.getChildNodes()).getLength()>0)
			node.removeChild(nl.item(0));
	}

	/*
		Gets the node's childnodes with the specified nodeName
	*/
	public static Vector<Node> getChildNodes(Node parent, String tagName) {
		Vector<Node> result  = new Vector<>();
		if (parent == null) return result;
		NodeList nl = parent.getChildNodes();
		int length = nl.getLength();
		for (int i=0; i<length; i++) {
			Node n = nl.item(i);
			if (tagName.equals(n.getNodeName())) {
				result.add(n);
			}
		}
		return result;
	}
	/*
		Removes the node's childnodes with the specified nodeName
	*/
	public static Vector<Node> removeChildNodes(Node node,String name)
	{
		Vector<Node> nl = getChildNodes(node,name);
		for(int i=0;i<nl.size();i++)
		{
			node.removeChild(nl.get(i));
		}
		return nl;
	}

	/*
		Selects the nodes from the NodeList which has attribute 'attributeName' with 'attributeValue'
	*/
	public static Vector<Element> getNodesWithAttribute(Node node,String attributeName,String attributeValue)
	{
		if ( node == null )
			return new Vector<>();
		else
			return getNodesWithAttribute(node.getChildNodes(),attributeName,attributeValue);
	}
	/*
		Selects the nodes from the NodeList which has attribute 'attributeName' with 'attributeValue'
	*/
	public static Vector<Element> getNodesWithAttribute(NodeList nl,String attributeName,String attributeValue)
	{
		Vector<Element> result  = new Vector<>();
		Element child;
		for(int i=0;i<nl.getLength();i++)
		{
			if (nl.item(i).getNodeType()==Node.ELEMENT_NODE)
			{
				child=(Element)nl.item(i);
				if (attributeValue.equals(child.getAttribute(attributeName)))
					result.add(child);
			}
		}
		return result;
	}


	/*
		Swaps two nodes
	*/
	public static void swapNodes(Node aNode,Node bNode)
	{
		Node aParent = aNode.getParentNode();
		Node bParent = bNode.getParentNode();
		aParent.replaceChild(aNode,bNode);
		Node bNextSibling = bNode.getNextSibling();
		if (bNextSibling!=null)
			bParent.insertBefore(bNextSibling,aNode);
		else
			bParent.appendChild(aNode);
	}
	/*
		Gets the node's siblings with the same name
	*/
	public static Vector<Node> getSameNameSiblings(Node node)
	{
		if (node.getParentNode()!=null)
		{
			return getChildNodes(node.getParentNode(),node.getNodeName());
		}
		return new Vector<>();
	}

	/*
		Adds a new node with the specified tagName to the parent
	*/
	public static Element addNode(Node parent,String tagName)
	{
		Document doc = parent.getOwnerDocument();
		Element newNode = doc.createElement(tagName);
		parent.appendChild(newNode);
		return newNode;
	}
	/*
		Adds a new node with the specified tagName and value to the parent
	*/
	public static Element addNode(Node parent,String tagName,String value)
	{
		Element newNode = addNode(parent,tagName);
		setText(newNode,value);
		return newNode;
	}

	/*
		Searches for the node's childNodes with the specified name
		and returns the first one if exists or null
	*/
	public static Node getFirstChild( Node parent, String tagName )
	{
		Node node;
		Node result = null;

		for( node = parent.getFirstChild(); ( node != null ) && ( result == null ); node = node.getNextSibling())
		{
			if(( node.getNodeType() == Node.ELEMENT_NODE ) && ( tagName.equals( node.getNodeName())))
			{
				result = node;
			}
		}

		return( result );
	}

	/*
		Searches for the node's childNodes with the specified name
		and returns the textValue if a node was found
		if no node found or no text value exists returns an empty String
	*/
	public static String getFirstChildValue(Node node,String tagName)
	{
		String result = "";
		Node childnode = getFirstChild(node,tagName);
		if (childnode!=null)
			result = XmlUtils.getText(childnode);

		return result;
	}


	/*
		Returns whether the node has child nodes with the type Node.TEXT_NODE or not
	*/
	public static boolean hasNonTextChild(Node node)
	{
		int childcount = node.getChildNodes().getLength();
		int textchildcount = XmlUtils.getChildNodes(node,Node.TEXT_NODE).size();
		return (childcount!=textchildcount);
	}


	public static Node createSimpleElement(Document doc, String name, boolean value) {
		return createSimpleElement(doc, name, String.valueOf(value));
	}
	public static Node createSimpleElement(Document doc, String name, int value) {
		return createSimpleElement(doc, name, String.valueOf(value));
	}
	public static Node createSimpleElement(Document doc, String name, double value) {
		return createSimpleElement(doc, name, String.valueOf(value));
	}
	public static Node createSimpleElement(Document doc, String name, String value) {
		Element elem = doc.createElement(name);
		if(value != null)
			elem.appendChild(doc.createTextNode(value));
		return elem;
	}

	/**
	 * Returns the value of the given node as a boolean
	 */
	public static boolean nodeToBoolean(Node node) {
		if(node == null) {
			return false;
			//throw new IllegalArgumentException("null node");
		}
		String value;
		if(node instanceof Attr) {
			Attr attr = (Attr)node;
			value = attr.getValue();
		} else {
			value = getText(node);
		}

		if(value == null) throw new IllegalArgumentException("could not convert " + value + " to boolean");

		if(value.equals("true")) {
			return true;
		} else if(value.equals("false")) {
			return false;
		} else {
			throw new IllegalArgumentException("could not convert " + value + " to boolean");
		}
	}

	/** Adds an attribute with the specified name to the given element only if
	 *  the <code>value</code> parameter is not <code>null</code>. */
	public static void addAttr(Element parent, String name, String value) {
		if (value != null) {
			parent.setAttribute(name,value);
		}
	}

	/*
		@return the attribute value, or the specified defaultValue, if the node is not an instance
		of Element, or the specified attribute doesn't exist
	*/
	public static String getAttribute(Node node,String attribute, String defaultValue) {
		if (!(node instanceof Element))
			return defaultValue;
		String attrValue = ((Element)node).getAttribute(attribute);
		return attrValue == null || "".equals(attrValue) ? defaultValue : attrValue;
	}

	/*
		Returns the attribute value
		searches the parents if no attribute found in the node
	*/
	public static String getAttributeUp(Node node,String attribute)
	{
		if (!(node instanceof Element))
			return "";
		Element element = (Element)node;
		String result = "";
		do
		{
			result = element.getAttribute(attribute);
			if (result!=null)
				if (result.length()>0)
					return result;

			element=(Element)element.getParentNode();
		} while ((element!=null) && (element.getNodeType()!=Node.DOCUMENT_NODE)) ;

		return "";
	}

	public static boolean hasElementWithValue(Node parent,String tagName,String value)
	{
		return getElementsWithValue(parent,tagName,value).size()>0;
	}

	public static Vector<Node> getElementsWithValue(Node parent,String tagName,String value)
	{
		Vector<Node> nl = new Vector<>();
		if ((parent instanceof Element) && (value!=null))
		{
			NodeList elements = ((Element)parent).getElementsByTagName(tagName);
			for (int i=0;i<elements.getLength();i++)
			{
				if (value.equals(XmlUtils.getText(elements.item(i))))
				{
					nl.add(elements.item(i));
				}
			}
		}
		return nl;
	}

	protected static int getIntAttributeFromValue(Node node, String value,
		int defaultValue) {
		int res = defaultValue;
		if (value != null) {
			try {
				res = Integer.parseInt(value);
			}
			catch (NumberFormatException ne) {
			}
		}
		return res;
	}

	public static int getIntAttributeUp(Node node, String attribname,
							int defaultValue) {
		return getIntAttributeFromValue(node,
							  XmlUtils.getAttributeUp(node, attribname),
							  defaultValue);
	}

	public static int getIntAttribute(Node node, String attribname,
							 int defaultValue) {
		return getIntAttributeFromValue(node,
							  XmlUtils.getAttribute(node, attribname,
			Integer.toString(defaultValue)), defaultValue);
	}

	protected static boolean getBooleanAttributeFromValue(Node node,
		String value, boolean defaultValue) {
		boolean res = defaultValue;
		if (value != null) {
			res = Boolean.parseBoolean(value);
		}
		return res;
	}

	public static boolean getBooleanAttributeUp(Node node, String attribname,
								  boolean defaultValue) {
		return getBooleanAttributeFromValue(node,
								XmlUtils.getAttributeUp(node,
			attribname), defaultValue);
	}

	public static boolean getBooleanAttribute(Node node, String attribname,
								boolean defaultValue) {
		return getBooleanAttributeFromValue(node,
								XmlUtils.getAttribute(node,
			attribname, Boolean.toString(defaultValue)), defaultValue);
	}

	public static int getChildPos(Node parent, Node child) {
		if (parent == null || child == null)
			return -1;
		NodeList nl = parent.getChildNodes();
		if (nl != null)
			for (int i = 0; i < nl.getLength(); i++)
				if (nl.item(i) == child)
					return i;
		return -1;
	}


	/**
	 * recursively copies the source node to the destination
	 */
	public static void copyNode(final Node source, final Node dest) {
		copyNode(source, dest, true);
	}

	/**
	 * recursively copies the source node to the destination
	 */
	public static void copyNode(final Node source, final Node dest, boolean copyAttributes) {
		if(source instanceof Text) {
			dest.setNodeValue(source.getNodeValue());
			return;
		}

		final NamedNodeMap attrs = source.getAttributes();
		final NodeList children = source.getChildNodes();
		Node attr;
		Node child;
		Node destChild;

		if(dest instanceof Element && copyAttributes)
			for(int i=0, c=attrs.getLength(); i<c; i++) {
				attr = attrs.item(i);
				addAttr((Element)dest, attr.getNodeName(), attr.getNodeValue());
			}

		for(int i=0, c=children.getLength(); i<c; i++) {
			child = children.item(i);
			if(child instanceof Text)
				addText(dest, child.getNodeValue());
			else {
				destChild = addNode(dest, child.getNodeName());
				copyNode(child, destChild, copyAttributes);
			}
		}
	}

	public static Node getChildNodeByPath( final Node source, String path ) {
		if( Tools.isEmpty(path)) return null;
		if( path.charAt(0) == ('/') ) path = path.substring(1);
		int ps = path.indexOf('/');
		if( ps != -1 ) {
			String chstr = path.substring(0,ps);
			Node tch = getFirstChild( source, chstr);
			if( tch != null ) {
				return getChildNodeByPath(tch,path.substring(ps+1));
			}
		} else {
			return getFirstChild( source,path);
		}
		return null;
	}

	public static Vector<Node> getChildNodesByPath( final Node source, String path ) {
		if( path == null ) return null;
		if( path.charAt(0) == ('/') ) path = path.substring(1);
		int ps = path.indexOf('/');
		if( ps != -1 ) {
			String chstr = path.substring(0,ps);
			Node tch = getFirstChild( source, chstr);
			if( tch != null ) {
				return getChildNodesByPath(tch,path.substring(ps+1));
			}
		} else {
			return getChildNodes( source,path);
		}
		return null;
	}

	public static String normalizeAndPrint(String s) {
		if (s==null) return "";
		int len = s.length();
		StringWriter sout = new StringWriter();
		PrintWriter pw = new PrintWriter(sout);
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			normalizeAndPrint(pw,c,true);
		}
		pw.flush();
		return sout.toString();
	} // normalizeAndPrint(String)

	/** Normalizes and print the given character. */
	protected static void normalizeAndPrint(PrintWriter fOut, char c, boolean fCanonical ) {
		switch (c) {
			case '<': {
				fOut.print("&lt;");
				break;
			}
			case '>': {
				fOut.print("&gt;");
				break;
			}
			case '&': {
				fOut.print("&amp;");
				break;
			}
			case '"': {
				fOut.print("&quot;");
				break;
			}
			case '\r':
			case '\n': { //NOSONAR
				if (fCanonical) {
					fOut.print("&#");
					fOut.print(Integer.toString(c));
					fOut.print(';');
					break;
				}
				// else, default print char
			}
			default: {
				fOut.print(c);
			}
		}

	} // normalizeAndPrint(char)


	/**
	 * Vrati objekt typu Document zo zadaneho URL (lokalne)
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public static Document readDocument(String url) throws Exception
	{
		IwcmFile file = new IwcmFile(Tools.getRealPath(url));
		if (file.exists())
		{
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			// to be compliant, completely disable DOCTYPE declaration:
			docBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

			IwcmInputStream in = new IwcmInputStream(file);
			InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
			InputSource is = new InputSource(reader);
			is.setEncoding("utf-8");

			Document doc = docBuilder.parse(is);

			reader.close();
			in.close();

			return doc;
		}
		return null;
	}

	/**
	 * Ulozi objekt typu Document na zadane URL (lokalne)
	 * @param doc
	 * @param url
	 * @param encoding
	 * @return
	 * @throws Exception
	 */
	public static boolean saveDocument(Document doc, String url, String encoding) throws Exception
	{
		IwcmFile file = new IwcmFile(Tools.getRealPath(url));
		if (file.exists())
		{
			TransformerFactory xf = TransformerFactory.newInstance();
			// to be compliant, prohibit the use of all protocols by external entities:
			xf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			xf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

			xf.setAttribute("indent-number", Integer.valueOf(2));

			Transformer xformer = xf.newTransformer();
			xformer.setOutputProperty(OutputKeys.METHOD, "xml");
			xformer.setOutputProperty(OutputKeys.INDENT, "yes");
			xformer.setOutputProperty(OutputKeys.ENCODING, encoding);

			Result result = new StreamResult(new OutputStreamWriter(new IwcmOutputStream(file), encoding));

			xformer.transform(new DOMSource(doc), result);
			return true;
		}
		return false;
	}

	/**
	 * Funkcia, ktora ziska text z xml tagu tagName v elemente el.
	 *
	 * @param tagName	Nazov tagu, z ktoreho chceme vytiahnut text
	 * @param el		Element, v ktorom sa nachadza dany tag
	 *
	 * @return	Vrati text, ktory sa nachadza v danom retazci. Ak nastane hocijaka chyba, vrati prazdny retazec.
	 *
	 * @author kmarton
	 */
	public static String getTextFromNode(String tagName, Element el)
	{
		NodeList list = null;
		try
		{
   		NodeList item = el.getElementsByTagName(tagName);
   		Element element = (Element)item.item(0);
   		list = element.getChildNodes();
		}
		catch (NullPointerException e)
		{
			return "";
		}

		if (list.item(0) != null)
   		return (list.item(0)).getNodeValue().trim();
   	else
   		return "";
	}
}
