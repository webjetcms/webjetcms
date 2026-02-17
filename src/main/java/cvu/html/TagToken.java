/*
 * HTML Parser
 * Copyright (C) 1997 David McNicol
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * file COPYING for more details.
 */

package cvu.html;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/**
 * This represents a single HTML tag. Each TagToken has a name and a
 * list of attributes and values.
 * @see HTMLTokenizer
 * @author <a href="http://www.strath.ac.uk/~ras97108/">David McNicol</a>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TagToken {

	/** Identifies the escape character. */
	public static final char ESCAPE = '\\';

	/** Identifies the quotation character. */
	public static final char QUOTE = '\'';
   public static final char QUOTE2 = '"';

	/** Stores the name of the TagToken. */
	private String name;

	/** Indicates whether the TagToken is an end-token. */
	private boolean end = false;

	/** Stores a list of attributes and their values. */
	private AttributeList attr;
   private String line;

   protected static final Map<String, String> skipTag = new Hashtable<>();
   protected static final Map<String, String> skipStartTag = new Hashtable<>();
   protected static final Map<String, String> skipEndTag = new Hashtable<>();
   protected static final Map<String, String> formTag = new Hashtable<>();

   static
   {
      //tagy, ktore sa v HTML vystupe preskakuju (ich obsah je ignorovany)
      skipTag.put("option", "");
      //skipTag.put("a", "");
      skipTag.put("textarea", "always");
      skipTag.put("button", "");

      //otvaracie tagy, ktore preskakujeme, ako hodnota moze byt podmienka na atribut
      skipStartTag.put("input", "type=hidden,type=button,type=submit,type=reset");
      skipStartTag.put("option", "");
      //skipStartTag.put("a", "");

      //uzatvaracie tagy, ktore preskakujeme
      skipEndTag.put("input", "");
      skipEndTag.put("option", "");
      skipEndTag.put("textarea", "");
      skipEndTag.put("select", "");
      skipEndTag.put("radio", "");
      skipEndTag.put("checkbox", "");
      //skipEndTag.put("button", "");
      //skipEndTag.put("a", "");

      //tagy, ktore sa pridavaju do databazy a su povazovane za formular
      formTag.put("input", "");
      formTag.put("textarea", "");
      formTag.put("select", "");
      formTag.put("checkbox", "");
      formTag.put("radio", "");
   }

	/**
	 * Constructs a new TagToken converting the specified string
	 * into a token name and a list of attributes with values.
	 * @param line the raw data.
	 */
	public TagToken (String line) {
		name = null;
      this.line = Tools.replace(line, "/>", ">");
		attr = new AttributeList();
		tokenizeAttributes(this.line);
	}

	/**
	 * Returns the name of the TagToken.
	 */
	public String getName () {
		return name;
	}

	/**
	 * Returns the attribute list of the TagToken.
	 */
	public AttributeList getAttributes () {
		return attr;
	}

	/**
	 * Indicates whether this token is an end tag.
	 */
	public boolean isEndTag () {
		return end;
	}

	/**
	 * Returns true if the given attribute exists.
	 * @param name the name of the attribute.
	 */
	public boolean isAttribute (String name) {
		return attr.exists(name);
	}

	/**
	 * Returns the value of the specified attribute or null if the
	 * attribute does not exist.
	 * @param name the name of the attribute.
	 */
	public String getAttribute (String name) {
		return attr.get(name);
	}

	/**
	 * Returns an attribute with all double quote characters
	 * escaped with a backslash.
	 * @param name the name of the attribute.
	 */
	public String getQuotedAttribute (String name) {

		// Check that the attribute list is there.
		if (attr == null) return null;

		// Return the quoted version.
		return attr.getQuoted(name);
	}

	/**
	 * Returns a string version of the attribute and its value.
	 * @param name the name of the attribute.
	 */
	public String getAttributeToString (String name) {

		// Check that the attribute list is there.
		if (attr == null) return null;

		// Return the string version.
		return attr.toString(name);
	}

	/**
	 * Returns a string version of the TagToken.
	 */
	@Override
	public String toString () {

		StringBuffer sb;  // Stores the string to be returned.
		//Enumeration list; // List of node's arguments or children.

		// Get a new StringBuffer.
		sb = new StringBuffer();

		// Write the opening of the tag.
		if (end)
			sb.append("</" + name);
		else
			sb.append('<' + name);

		// Check if there are any attributes.
		if (attr != null && attr.size() > 0) {

			// Print string version of the attributes.
			sb.append(' ').append(attr.toString());
		}

		// Finish off the tag.
		sb.append('>');

		// Return the string version.
		return sb.toString();
	}

	/**
	 * Sets the name of the token and also whether it is a begin
	 * or an end token.
	 * @param name the name of the token.
	 */
	private void setName (String name) {

		if (name == null) {
			this.name = null;
			return;
		}

		String lcname = name.toLowerCase();

		try
		{
			if (lcname.length()>0 && lcname.charAt(0) == '/') {
				this.name = lcname.substring(1);
				end = true;
			} else {
				this.name = lcname;
			}
		}
		catch (RuntimeException e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Adds a attribute and value to the list.
	 * @param name the name of the attribute.
	 * @param value the value of the attribute.
	 */
	private void setAttribute (String name, String value) {
		attr.set(name, value);
	}

	/**
	 * Adds a attribute to the list using the given string. The string
	 * may either be in the form 'attribute' or 'attribute=value'.
	 * @param s contains the attribute information.
 	 */
	private void setAttribute (String s)
   {
		int idx;	// The index of the = sign in the string.
		String attrName;	// Stores the name of the attribute.
		String value;	// Stores the value of the attribute.

		// Check if the string is null.
		if (s == null) return;

		// Get the index of = within the string.
		idx = s.indexOf('=');

		// Check if there was '=' character present.
		if (idx < 0) {

			// If not, add the whole string as the attribute
			// name with a null value.
			setAttribute(s, "");
		} else {

			// If so, split the string into a name and value.



			attrName = s.substring(0, idx);
			value = s.substring(idx + 1);

         try
         {
            //odstran apostrofy z value
            value = value.replace('\'', ' ').trim();
         }
         catch (Exception ex)
         {
            sk.iway.iwcm.Logger.error(ex);
         }

			// Add the name and value to the attribute list.
			setAttribute(attrName, value);
		}
	}

	/**
	 * Tokenizes the given string and uses the resulting vector
	 * to to build up the TagToken's attribute list.
	 * @param args the string to tokenize.
	 */
	private void tokenizeAttributes (String args) {

		Vector v;		// Vector of tokens from the string.
		Enumeration e;		// Enumeration of vector elements.
		String[] tokens = null;	// Array of tokens from vector.
		int length;		// Size of the vector.
		int i;			// Loop variable.

		// Get the vector of tokens.
		v = tokenizeString(args);

		// Check it is not null.
		if (v == null) return;

		// Create a new String array.
		length = v.size() - 1;
		if (length > 0) tokens = new String[length];
		else tokens = new String[0];

		// Get an enumeration of the vector's elements.
		e = v.elements();

		// Store the first element as the TagToken's name.
		setName((String) e.nextElement());

		// Stop processing now if there are no more elements.
		if (! e.hasMoreElements()) return;

		// Put the rest of the elements into the string array.
		i = 0;
		while (e.hasMoreElements())
			tokens[i++] = (String) e.nextElement();

		// Deal with the name/value pairs with separate = signs.
		for (i = 1; i < (length - 1); i++) {

			if (tokens[i] == null) continue;

			if (tokens[i].equals("=")) {
				setAttribute(tokens[i - 1], tokens[i + 1]);
				tokens[i] = null;
				tokens[i - 1] = null;
				tokens[i + 1] = null;
			}
		}

		// Deal with lone attributes and joined name/value pairs.
		for (i = 0; i < length; i++)
			if (tokens[i] != null) setAttribute(tokens[i]);
	}

	/**
	 * This method tokenizes the given string and returns a vector
	 * of its constituent tokens. It understands quoting and character
	 * escapes.
	 * @param s the string to tokenize.
	 */
	private Vector tokenizeString (String s) {

		// First check that the args are not null or zero-length.
		if (s == null || s.length() == 0) return null;

		boolean whitespace = false; // True if we are reading w/space.
		boolean escaped = false;    // True if next char is escaped.
		boolean quoted = false;	    // True if we are in quotes.
		int length;		    // Length of attribute string.
		int i = 0;		    // Loop variable.

		// Create a vector to store the complete tokens.
		Vector tokens = new Vector();

		// Create a buffer to store an individual token.
		StringBuffer buffer = new StringBuffer(80);

		// Convert the String to a character array;
		char[] array = s.toCharArray();

		length = array.length;

		// Loop over the character array.
		while (i < length) {

			// Check if we are currently removing whitespace.
			if (whitespace) {
				if (isWhitespace(array[i])) {
					i++;
					continue;
				} else {
					whitespace = false;
				}
			}

			// Check if we are currently escaped.
			if (escaped) {

				// Add the next character to the array.
				buffer.append(array[i++]);

				// Turn off the character escape.
				escaped = false;
			} else {

				// Check for the escape character.
				if (array[i] == ESCAPE) {
					escaped = true;
					i++;
					continue;
				}

				// Check for the quotation character.
				if (array[i] == QUOTE || array[i] == QUOTE2)
            	{
					quoted = !quoted;
					i++;
					continue;
				}

				// Check for the end of the token.
				if (!quoted && isWhitespace(array[i])) {

					// Add the token and refresh the buffer.
					tokens.addElement(buffer.toString());
					buffer = new StringBuffer(80);

					// Stop reading the token.
					whitespace = true;

					continue;
				}

				// Otherwise add the character to the buffer.
				buffer.append(array[i++]);
			}
		}

		// Add the last token to the vector if there is one.
		if (! whitespace) tokens.addElement(buffer.toString());

		return tokens;
	}

	/**
	 * Returns true if the given character is considered to be
	 * whitespace.
	 * @param c the character to test.
	 */
	private boolean isWhitespace (char c) {
		return (c == ' ' || c == '\t' || c == '\n');
	}

   private boolean isSkip()
   {
      String data;
      if (end)
      {
         data = skipEndTag.get(name.toLowerCase());
      }
      else
      {
         data = skipStartTag.get(name.toLowerCase());
      }
      if (data==null) return(false);

      if (data.length()==0) return(true);

      //inak je v data podmienka na field

      String attribute;
	  String value;

      StringTokenizer stMain = new StringTokenizer(data, ",");
      String condition;
      StringTokenizer st;
      while (stMain.hasMoreTokens())
      {
         condition = stMain.nextToken();
         st = new StringTokenizer(condition, "=");
         if (st.countTokens()==2)
         {
            attribute = st.nextToken();
            value = st.nextToken();
            if (value.equalsIgnoreCase(getAttribute(attribute)))
            {
               return(true);
            }
         }
      }
      return(false);
   }

   public void setLine(String line)
   {
      this.line = line;
   }
   public String getLine()
   {
      return line;
   }
   public String getLineForm(HttpServletRequest request)
   {
   	try
		{

	      if (isSkip()) return("");
	      String type = getAttribute("type");
	      if ("checkbox".equalsIgnoreCase(type))
	      {
	          String attrName = getAttribute("name");
	          if (Tools.isEmpty(attrName)) attrName = getAttribute("id");

	          StringBuffer ret = new StringBuffer("<input type='checkbox' name='").append(attrName).append('\'');
	          String[] params = request.getParameterValues(attrName);
	          String value = normalizeValue(getAttribute("value"));

	          int size = 0;
	          if (params!=null) {
				size = params.length;

				String param = null;
				int i;
				for (i=0; i<size; i++)
				{
					param = normalizeValue(params[i]);
					if (param!=null && param.equals(value))
					{
						//povodne tu bol test na size==1 || value, to ale nefunguje pri multicheckboxe ak sa zvoli len jedna hodnota (a cb maju rovnaky nazov)
						ret.append(" checked");
					}
				}
			  }
	          ret.append('>');

	          if (Constants.getBoolean("formMailRenderRadioCheckboxText"))
	          {
	         	 if (ret.indexOf("checked")!=-1) ret = new StringBuffer("<span class='inputcheckbox emailinput-cb input-checked'>[X]</span>");
	         	 else ret = new StringBuffer("<span class='inputcheckbox emailinput-cb input-unchecked'>[ ]</span>");
	          }

	          return(ret.toString());
	      }
	      if ("radio".equalsIgnoreCase(type))
	      {
	          String attrName = getAttribute("name");
	          if (Tools.isEmpty(attrName)) attrName = getAttribute("id");

	          String ret = "<input type='radio' name='"+attrName+"'";
	          String param = normalizeValue(request.getParameter(attrName));
	          String value = normalizeValue(getAttribute("value"));
	          if (param!=null && param.equals(value))
	          {
	             ret+=" checked";
	          }
	          ret +=">";

	          if (Constants.getBoolean("formMailRenderRadioCheckboxText"))
	          {
	         	 if (ret.indexOf("checked")!=-1) ret = "<span class='inputradio emailinput-radio input-checked'>[X]</span>";
	         	 else ret = "<span class='inputradio emailinput-radio input-unchecked'>[ ]</span>";
	          }

	          return(ret);
	      }

	      String ret = formTag.get(name);
	      if (ret!=null)
	      {
	         if (name.equalsIgnoreCase("input"))
	         {
	            type = "text";
	         }

	         ret = "";

	         String cssType = type;
	         if (cssType==null) cssType = name;
	         if (cssType!=null)
	         {
					String htmlClasses = getAttribute("class");

					ret = "<span class='form-control emailInput-"+cssType.toLowerCase();
					if (htmlClasses!=null && htmlClasses.contains("formsimple-wysiwyg")) ret += " formsimple-wysiwyg";
					ret += "'";
					if ("textarea".equalsIgnoreCase(cssType)) ret += " style='height: auto;'";
					ret += ">";
	         }

	         String myName = getAttribute("name");
	         if (Tools.isEmpty(myName)) myName = getAttribute("id");

	         ret += "!" + myName + "!";
	         if (cssType!=null)
	         {
	            ret += "</span>";
	         }
	         return(ret);
	      }

	      ret = "<" + line + ">";
	      return(ret);
		}
   	catch (Exception ex)
		{
   		sk.iway.iwcm.Logger.error(ex);
   		return("ERROR: " + ex.getMessage());
		}
   }

   /**
    * Upravi hodnoty z requestu / atributu na normalizovany tvar (trim, tvrde medzery, &nbsp;)
    * @param value
    * @return
    */
   private static String normalizeValue(String value)
   {
   	if (value == null) return value;
   	//nbsp tam pridava uprava nerozdelovania spojok
   	value = Tools.replace(value, "&nbsp;", " ");
  	 	value = Tools.replace(value, Constants.NON_BREAKING_SPACE, " ");
  	 	value = value.trim();
   	return value;
   }

   public String getFormField()
   {
      if (isSkip()) return(null);

      String ret = formTag.get(name);
      if (ret!=null)
      {
      	String myName = getAttribute("name");
         if (Tools.isEmpty(myName)) myName = getAttribute("id");
         return(myName);
      }

      return(null);
   }
   /**
    * vrati meno tagu, na ktory cakame, alebo null, ak sme ho prave nasli
    * @param skipToTag - aktualne meno
    * @return
    */
   public String getSkipToTag(String skipToTag)
   {
      //ak uz na nieco cakame
      if (skipToTag!=null)
      {
         if (end && name.equals(skipToTag))
         {
            //ak sme cakali na tento tag, tak uz cakat netreba
            return(null);
         }
         else
         {
            return(skipToTag);
         }
      }

      String ret = skipTag.get(name.toLowerCase());
      if (ret!=null)
      {
         if ("always".equals(ret) || isSkip()) return(name);
      }
      return(null);
   }
}
