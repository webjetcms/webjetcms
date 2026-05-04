package sk.iway.iwcm.system.captcha;

import sk.iway.iwcm.database.JpaDB;

public class CaptchaDictionaryDB extends JpaDB<CaptchaDictionaryBean>
{
	public CaptchaDictionaryDB()
	{
		super(CaptchaDictionaryBean.class);
	}
}
