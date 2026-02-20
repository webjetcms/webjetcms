package sk.iway.iwcm.system.captcha;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

import sk.iway.iwcm.database.ActiveRecord;

@Entity
@Table(name="captcha_dictionary")
public class CaptchaDictionaryBean extends ActiveRecord implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(generator="WJGen_captcha_dictionary")
	@TableGenerator(name="WJGen_captcha_dictionary",pkColumnValue="captcha_dictionary")	
	private Integer id;
	
	private String word;
	
	@Override
	public int getId() 
	{
		return id;
	}

	@Override
	public void setId(int id) 
	{
		this.id=id;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}
	
	@Override
	public boolean save()
	{
		boolean status = super.save();
		CaptchaServiceSingleton.flush();
		return status;
	}
	

}
