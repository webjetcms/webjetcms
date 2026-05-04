package sk.iway.iwcm.system.captcha;

import java.util.Locale;

import com.octo.captcha.component.word.DefaultSizeSortedWordList;
import com.octo.captcha.component.word.DictionaryReader;
import com.octo.captcha.component.word.SizeSortedWordList;

public class DictionaryReaderImpl implements DictionaryReader {

	public DictionaryReaderImpl(String locale)
	{
		list = new DefaultSizeSortedWordList(new Locale(locale));
		CaptchaDictionaryDB cddb = new CaptchaDictionaryDB();
		for (CaptchaDictionaryBean bean : cddb.getAll())
		{
			list.addWord(bean.getWord().toUpperCase());
		}
	}
	
	private SizeSortedWordList list;
	
	@Override
	public SizeSortedWordList getWordList() 
	{
		return list;
	}

	@Override
	public SizeSortedWordList getWordList(Locale arg0) 
	{
		return list;
	}

}
