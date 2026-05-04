package sk.iway.iwcm.system.captcha;

import com.octo.captcha.component.word.wordgenerator.DictionaryWordGenerator;
import com.octo.captcha.component.word.wordgenerator.RandomWordGenerator;
import com.octo.captcha.component.word.wordgenerator.WordGenerator;

import sk.iway.iwcm.Constants;

public class CaptchaWordGeneratorFactory 
{
	public static WordGenerator getWordGenerator()
	{
		boolean definedWordGeneratorEnabled = Constants.getBoolean("captchaDictionaryEnabled");
		if (definedWordGeneratorEnabled)
		{
			return new DictionaryWordGenerator(new DictionaryReaderImpl("en"));
		}
		return new RandomWordGenerator("ABCDEFGHJKLMNPRSTUVXYZ");
	}
}
