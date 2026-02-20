package sk.iway.iwcm.sync.inport;

import sk.iway.iwcm.inquiry.InquiryBean;

/**
 * Zobrazenie ankety pri importe.
 * Polozky:
 * number = index do zoznamu exportovanych ankiet;
 * group = skupina ankiet;
 * question = anketova otazka;
 * questionShort = skratena otazka doplnena troma bodkami;
 * selected = ci sa odporuca importovat (ano: ak neexistuje lokalna);
 * local = ci existuje ekvivalentna lokalna anketa.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 25.6.2012 10:25:29
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ContentInquiryBean extends SimpleContentBean<InquiryBean>
{

	public ContentInquiryBean(int number, InquiryBean remoteInquiry, InquiryBean localInquiry)
	{
		super(number, remoteInquiry, localInquiry);
	}

	public String getGroup()
	{
		return remoteItem.getAnswers().get(0).getGroup();
	}

	public String getQuestion()
	{
		return remoteItem.getQuestion();
	}

	public String getQuestionShort()
	{
		String question = remoteItem.getQuestion();
		if (question.length() > 15)
		{
			question = question.substring(0, 15) + "...";
		}
		return question;
	}

}
