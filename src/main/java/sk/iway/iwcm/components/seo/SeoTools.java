package sk.iway.iwcm.components.seo;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import sk.iway.Html2Text;

@SuppressWarnings("java:S1659")
public class SeoTools
{
	private static List<String> middleSentences, maxSentences, middleWords, maxWords;
	private static int sentencesCount, wordsCount, complexWordsCount;

	protected SeoTools() {
		//utility class
	}

	/**
	 * Metoda vracia zoznam objektov typu Density, ktory je vhodny na zobrazenie v tabulke.
	 *
	 * @param html			zdrojovy kod stranky
	 * @param keywords	klucove slova oddelene bodkociarkou ;
	 * @return
	 */
	public static List<Density> getKeywordDensityTable(String html, String [] keywords){

		Html2Text html2Text = new Html2Text(html);

		List<String> h1Elements = html2Text.getTextByElement("h1");
		List<String> h2Elements = html2Text.getTextByElement("h2");
		List<String> h3Elements = html2Text.getTextByElement("h3");
		List<String> strongElements = html2Text.getTextByElement("strong,b");
		List<String> italicElements = html2Text.getTextByElement("i,em");
		List<String> linkElements = html2Text.getTextByElement("a");

		List<Density> keywordDensityList = new ArrayList<>();
		for(String keyword : keywords){
			keyword = keyword.toLowerCase();
			Density keywordDensity = new Density(keyword);

			for(String e : h1Elements){
				keywordDensity.incrementH1(countOccurences(e.toLowerCase(), keyword));
			}

			for(String e : h2Elements){
				keywordDensity.incrementH2(countOccurences(e.toLowerCase(), keyword));
			}

			for(String e : h3Elements){
				keywordDensity.incrementH3(countOccurences(e.toLowerCase(), keyword));
			}

			for(String e : strongElements){
				keywordDensity.incrementStrong(countOccurences(e.toLowerCase(), keyword));
			}

			for(String e : italicElements){
				keywordDensity.incrementItalics(countOccurences(e.toLowerCase(), keyword));
			}

			for(String e : linkElements){
				keywordDensity.incrementLink(countOccurences(e.toLowerCase(), keyword));
			}


			keywordDensity.incrementAlltogether(countOccurences(html.toLowerCase(), keyword));

			keywordDensityList.add(keywordDensity);

		}

		return keywordDensityList;
	}

	/**
	 * Pomocna metoda pre metodu getKeywordDensityTable(String, String[])
	 * @param text 		obsahuje usek prehladavaneho zdrojoveho kodu
	 * @param keyword		klucove slovo
	 * @return
	 */
	private static int countOccurences(String text, String keyword){
		int count=0;
		while(text.contains(keyword)){
			count++;
			text = text.replaceFirst(keyword, "");
		}
		return count;
	}

	public static void countSentences(String html, int middle, int max){
		middleSentences = new ArrayList<>();
		maxSentences = new ArrayList<>();
		html = html.replaceAll("\\<[^>]*>","");
		html = html.replaceAll("\\![^!]*!","");
		if(html != null && html.contains(".")){
			String[] sentences = html.split("[.!?]");
			sentencesCount = sentences.length;
			for(int i=0; i < sentencesCount; i++){
				String[] words = sentences[i].split(" ");
				if(words.length >= middle && words.length < max){
					middleSentences.add(sentences[i]+".");
				}
				if(words.length >= max){
					maxSentences.add(sentences[i]+".");
				}
			}
		}
	}

	public static void countWords(String html, int middle, int max){
		middleWords = new ArrayList<>();
		maxWords = new ArrayList<>();
		complexWordsCount = 0;
		html = html.replaceAll("\\<[^>]*>","");
		html = html.replaceAll("\\![^!]*!","");
		html = html.replaceAll("[.!?]", " ");
		if(html != null && html.contains(" ")){
			String[] words = html.split(" ");
			wordsCount = words.length;
			for(int i=0; i< wordsCount; i++){	//prejde cez kazde slovo
				if(getSyllableCount(words[i]) > 2)
					complexWordsCount++;
				if(words[i].length() >= middle && words[i].length() < max){
					middleWords.add(words[i]);
				}
				if(words[i].length() >= max){
					maxWords.add(words[i]);
				}
			}
		}
	}

	/**
	 * Analyzuje čitateľnosť textu
	 * Používa techniku analyzy Gunning fog index - pre angličtinu -> možná nepresnosť
	 * a mnou navrhnutu funkciu pre počítanie slabík -> =dalšia možná nepresnosť
	 * Odhaduje počet rokov vzdelávania potrebný na porozumenie textu.
	 * Z praxe: 6 znamená vynikajúcu čitateľnosť. 8 až 10 sú časopisové a novinové články, poviedky, ľahko čitateľné a pochopiteľné.
	 * Index 11 až 14 už dosahujú odbornejšie články. Vedecké práce sa zvyčajne dostávajú na úroveň 15 až 20 a vyžadujú už od čitateľa plnú sústredenosť.
	 * Index nad 20 majú len texty, pri ktorých pisateľ celkom ignoruje čitateľa.
	 */
	public static double textReadibility(){
		return (0.4*(wordsCount/(double)sentencesCount)+100*(complexWordsCount/(double)wordsCount));
	}

	public static List<String> getMiddleSentences(){
		return middleSentences;
	}

	public static List<String> getMaxSentences(){
		return maxSentences;
	}

	public static List<String> getMiddleWords(){
		return middleWords;
	}

	public static List<String> getMaxWords(){
		return maxWords;
	}

	private static int getSyllableCount(String word)
	{
		int syllableCount = 0;
		Scanner sc = new Scanner(word);
		sc.useDelimiter("[aeiouyáéíúóýô]+");

		while(sc.hasNext())
		{
			syllableCount++;
			sc.next();
		}

		sc.close();

		if(word.startsWith("a") || word.startsWith("e") || word.startsWith("i") || word.startsWith("o") || word.startsWith("u") || word.startsWith("á")
					|| word.startsWith("é") || word.startsWith("í") || word.startsWith("ó") || word.startsWith("ú"))
		{
			syllableCount++;
		}

		return syllableCount;
	}
}
