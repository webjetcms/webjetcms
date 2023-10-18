package sk.iway.iwcm.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.test.Assertions;
import sk.iway.iwcm.test.BaseWebjetTest;

/**
 *  FormResultsParserJUnit.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.1 $
 *@created      Date: 16.06.2010 15:48:45
 *@modified     $Date: 2009/12/11 15:42:33 $
 */
public class FormResultsParserJUnit extends BaseWebjetTest
{

	String config = "Krajina~Krajinka~300|Destinacia~dest~*";

	@Test
	public void labelParsing()
	{
		try
		{
			FormResultsParser parser = new FormResultsParser(config);
			Assertions.collectionsEqual(Arrays.asList("Krajina", "Destinacia"), parser.getColumnNames());
			assertEquals(parser.getLabelFor("Krajina"), "Krajinka");
			assertEquals(parser.getLabelFor("Destinacia"), "dest");
		}
		catch (ParseException e){e.printStackTrace(); assertTrue(false);}
	}

	@Test
	public void widthParsing()
	{
		try
		{
			FormResultsParser parser = new FormResultsParser(config);
			assertEquals(parser.getCssWidthFor("Krajina"), "300px");
			assertEquals(parser.getCssWidthFor("Destinacia"), "auto");
		}
		catch (ParseException e){e.printStackTrace(); assertTrue(false);}
	}
}