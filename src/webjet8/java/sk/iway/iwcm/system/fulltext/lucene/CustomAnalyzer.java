package sk.iway.iwcm.system.fulltext.lucene;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cz.CzechStemFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

import sk.iway.iwcm.system.fulltext.FulltextSearch;

/**
 *  CustomAnalyzer
 *  Applies Lemmatising ONLY on fields DATA and TITLE
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: jeeff thaber $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.4.2011 11:27:45
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class CustomAnalyzer extends StopwordAnalyzerBase
{
	/** Default maximum allowed token length */
	  public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;

	  private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;

	  /**
	   * Specifies whether deprecated acronyms should be replaced with HOST type.
	   * See {@linkplain "https://issues.apache.org/jira/browse/LUCENE-1068"}
	   */
	  //private final boolean replaceInvalidAcronym;

	private String language;

	  /** An unmodifiable set containing some common English words that are usually not
	  useful for searching. */

	  /** Builds an analyzer with the given stop words.
	   * @param matchVersion Lucene version to match See {@link
	   * <a href="#version">above</a>}
	   * @param stopWords stop words */
	  private  CustomAnalyzer(Version matchVersion, Set<?> stopWords) {
	    super(matchVersion, stopWords);
	    //replaceInvalidAcronym = matchVersion.onOrAfter(Version.LUCENE_31);
	  }

	  /** Builds an analyzer with the default stop words ({@link
	   * #STOP_WORDS_SET}).
	   * @param matchVersion Lucene version to match See {@link
	   * <a href="#version">above</a>}
	   */
	  public CustomAnalyzer(Version matchVersion,String language) {
	    this(matchVersion, FulltextSearch.stopwords(language));
	    this.language = language;
	  }

	  /**
	   * Set maximum allowed token length.  If a token is seen
	   * that exceeds this length then it is discarded.  This
	   * setting only takes effect the next time tokenStream or
	   * reusableTokenStream is called.
	   */
	  public void setMaxTokenLength(int length) {
	    maxTokenLength = length;
	  }

	  /**
	   * @see #setMaxTokenLength
	   */
	  public int getMaxTokenLength() {
	    return maxTokenLength;
	  }


	  @Override
	  protected TokenStreamComponents createComponents(final String fieldName, final Reader reader)
	  {
		  final StandardTokenizer src = new StandardTokenizer(matchVersion, reader);
		  src.setMaxTokenLength(maxTokenLength);
	    //src.setReplaceInvalidAcronym(replaceInvalidAcronym);
	    TokenStream tok = new StandardFilter(matchVersion, src);
	    tok = new LowerCaseFilter(matchVersion, tok);
	    /* use algorithmic stemmers for Enlish and Czech language */
	    if (language.equals("en"))
	    {
	   	 tok = new PorterStemFilter(tok);
	   	 tok = new StopFilter(matchVersion, tok, stopwords);
	    }else if (language.equals("cz"))
	    {
	   	 tok = new CzechStemFilter(tok);
	   	 tok = new ASCIIFoldingFilter(tok);
	   	 tok = new StopFilter(matchVersion, tok, stopwords);
	    }
	    else{
	   	 tok = new ASCIIFoldingFilter(tok);
	   	 tok = new StopFilter(matchVersion, tok, stopwords);
	   	 tok = new LemmatisatingFilter(Version.LUCENE_31, tok, language);
	    }


	    return new TokenStreamComponents(src, tok)
	    {
	      @Override
	      protected boolean reset(final Reader reader) throws IOException
	      {
	        src.setMaxTokenLength(maxTokenLength);
	        return super.reset(reader);
	      }
	    };
	  }
}
