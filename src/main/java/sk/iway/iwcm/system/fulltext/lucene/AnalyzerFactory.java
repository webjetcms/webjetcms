package sk.iway.iwcm.system.fulltext.lucene;

import org.apache.lucene.util.Version;

import sk.iway.iwcm.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

public class AnalyzerFactory {


   private AnalyzerFactory() {
      //private konstruktor, je to len factory trieda so statickymi metodami
   }

   /**
    * Vrati analyzer pre zadany jazyk
    * @param matchVersion
    * @param language
    * @return
    */
   public static Analyzer getAnalyzer(Version matchVersion, String language) {
      Logger.debug(AnalyzerFactory.class, "getAnalyzer("+language+")");

      if ("en".equals(language)) return new EnglishAnalyzer(matchVersion);
      else if ("cz".equals(language)) return new CzechAnalyzer(matchVersion);
      else if ("de".equals(language)) return new GermanAnalyzer(matchVersion);

      //defaultne vrat SK analyzer
      return new CustomAnalyzer(matchVersion, language);
   }

}
