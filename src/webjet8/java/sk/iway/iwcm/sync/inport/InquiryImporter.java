package sk.iway.iwcm.sync.inport;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.inquiry.AnswerForm;
import sk.iway.iwcm.inquiry.InquiryBean;
import sk.iway.iwcm.inquiry.InquiryDB;
import sk.iway.iwcm.sync.export.Content;

/**
 * Import ankiet.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 25.6.2012 10:22:28
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class InquiryImporter
{

	public static List<ContentInquiryBean> getInquiries(Content content, HttpServletRequest request)
	{
		List<ContentInquiryBean> inquiryBeans = new ArrayList<ContentInquiryBean>();
		if (null == content) return inquiryBeans;

		for (Numbered<InquiryBean> remoteInquiry : Numbered.list(content.getInquiries()))
		{
			InquiryBean localInquiry = getLocalInquiry(remoteInquiry.item, request);
			inquiryBeans.add(new ContentInquiryBean(remoteInquiry.number, remoteInquiry.item, localInquiry));
		}
		return inquiryBeans;
	}

	public static void importInquiries(HttpServletRequest request, Content content)
	{
		if (null == content) return;
		for (Numbered<InquiryBean> inquiry : Numbered.list(content.getInquiries()))
		{
			if (null != request.getParameter("inquiry_" + inquiry.number) || request.getAttribute("syncAll")!=null)
			{
				createLocalContentInquiry(inquiry.item, request);
			}
		}

	}

	/**
	 * Vrati anketu zodpovedajucu importovanej (rovnaka skupina a otazka), alebo null.
	 * 
	 * @param remoteInquiry
	 * @return
	 */
	private static InquiryBean getLocalInquiry(InquiryBean remoteInquiry, HttpServletRequest request)
	{
		List<Integer> localIds = InquiryDB.getInquiryIds(remoteInquiry.getAnswers().get(0).getGroup(), request, true);
		for (Integer localId : localIds)
		{
			InquiryBean localInquiry = InquiryDB.getInquiry(localId.intValue(), 0, null, "answer_id", true, request);
			if (Tools.areSame(remoteInquiry.getQuestion(), localInquiry.getQuestion()))
			{
				return localInquiry;
			}
		}
		return null;
	}

	private static boolean createLocalContentInquiry(InquiryBean remoteInquiry, HttpServletRequest request)
	{
		InquiryBean localInquiry = getLocalInquiry(remoteInquiry, request);
		if (null != localInquiry)
		{
			InquiryDB.deleteInquiry(localInquiry.getAnswers().get(0).getQuestionID(), request);
		}
		AnswerForm a = remoteInquiry.getAnswers().get(0);
		InquiryDB.createNewQuestion(a, request);
		int inquiryId = a.getQuestionID();
		for (AnswerForm remoteAnswer : remoteInquiry.getAnswers())
		{
			InquiryDB.addNewAnswer(inquiryId, remoteAnswer, request);
		}
		return true;
	}

}
