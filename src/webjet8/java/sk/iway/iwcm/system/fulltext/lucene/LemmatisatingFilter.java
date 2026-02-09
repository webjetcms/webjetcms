package sk.iway.iwcm.system.fulltext.lucene;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.util.Version;

import sk.iway.iwcm.Constants;

/**
 * LematisatiingFilter.java
 * 
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2011
 * @author $Author: jeeff thaber $
 * @version $Revision: 1.3 $
 * @created Date: 15.4.2011 13:45:54
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class LemmatisatingFilter extends TokenFilter
{
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final KeywordAttribute keywordAtt = addAttribute(KeywordAttribute.class);
	private String language;

	public LemmatisatingFilter(Version matchVersion, TokenStream input, String language)
	{
		super(input);
		this.language = language;
	}

	@Override
	public final boolean incrementToken() throws IOException
	{
		if (input.incrementToken())
		{
			if (!keywordAtt.isKeyword())
			{ // don't muck with already-keyworded terms
				char[] buffer = termAtt.buffer();
				char[] stem = Lemmas.get(language, buffer, 0, termAtt.length());
				
				if (stem != null)
				{
					if (Constants.getBoolean("luceneIndexingSkSynonymExpansion"))
					{
						char[] synonym = Synonyms.get(language, buffer, 0, termAtt.length());
						if (synonym != null)
						{
							stem = synonym;
						}
					}
				}
				
				if (stem != null)
				{
					termAtt.setEmpty();
					for (char c : stem)
						termAtt.append(c);
				}
			}
			return true;
		}
		else
		{
			return false;
		}
	}
}
