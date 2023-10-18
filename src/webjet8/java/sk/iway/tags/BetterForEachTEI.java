package sk.iway.tags;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.VariableInfo;

import org.apache.taglibs.standard.tei.ForEachTEI;


/**
 * 
 *  BetterForEachTagExtraInfo.java
 *
 *	TEI - Tag extra info trieda k vylepsenemu c:forEach tagu, umoznuje exponovat premennu pod 
 *		jej nazvom (atribut var) a typom (atribut type, alebo ako java.lang.Object)
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author 		mbocko
 *@version      7
 *@created      1.3.2015 19:57:29
 */
public class BetterForEachTEI extends ForEachTEI
{
	@Override
	public VariableInfo[] getVariableInfo(TagData data)
	{
		VariableInfo[] variables = new VariableInfo[2];

		int counter = 0;

		String var = data.getAttributeString("var");
		String type = data.getAttributeString("type");

		if (var != null) 
		{
			if (type == null) 
			{
				type = "java.lang.Object";
			}

			variables[(counter++)] = new VariableInfo( data.getAttributeString("var"), type, true, 0);
		}

		String varStatus = data.getAttributeString("varStatus");

		if (varStatus != null)
			variables[(counter++)] = new VariableInfo(varStatus, "javax.servlet.jsp.jstl.core.LoopTagStatus", true, 0);
		VariableInfo[] result;
		if (counter > 0) 
		{
			result = new VariableInfo[counter];
			System.arraycopy(variables, 0, result, 0, counter);
		} 
		else 
		{
			result = new VariableInfo[0];
		}

		return result;
	}
}
