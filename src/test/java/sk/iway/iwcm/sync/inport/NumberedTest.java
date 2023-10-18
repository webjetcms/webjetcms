package sk.iway.iwcm.sync.inport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 *  NumberedTest.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 29.6.2012 11:58:04
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class NumberedTest
{

	@Test
	public void testList()
	{
		List<String> result = new ArrayList<String>();
		List<String> words = Arrays.asList("zero", "one", "two");
		for (Numbered<String> word : Numbered.list(words))
		{
			result.add("" + word.number + ": " + word.item);
		}
		assertEquals("0: zero", result.get(0));
		assertEquals("1: one", result.get(1));
		assertEquals("2: two", result.get(2));
	}

	@Test
	public void testArray()
	{
		List<String> result = new ArrayList<String>();
		String[] words = { "zero", "one", "two" };
		for (Numbered<String> word : Numbered.array(words))
		{
			result.add("" + word.number + ": " + word.item);
		}
		assertEquals("0: zero", result.get(0));
		assertEquals("1: one", result.get(1));
		assertEquals("2: two", result.get(2));
	}

}
