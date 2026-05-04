package sk.iway.iwcm.sync.export;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import sk.iway.iwcm.inquiry.AnswerForm;
import sk.iway.iwcm.inquiry.InquiryBean;
import sk.iway.iwcm.inquiry.InquiryDB;

/**
 * Export udajov pre komponent "inquiry", cize ankety.
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 15.6.2012 15:21:57
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class InquiryExporter extends ComponentExporter
{

	public InquiryExporter(String params)
	{
		super(params);
	}

	@Override
	public void export(ContentBuilder callback)
	{
		String inquiryGroups = pageParams.getValue("group", null);
		HttpServletRequest request = callback.getRequest();
		boolean random = pageParams.getBooleanValue("random", true);
		int imagesLength = pageParams.getIntValue("imagesLength", 10);
		String percentageFormat = pageParams.getValue("percentageFormat", "0.0");
		String orderBy = pageParams.getValue("orderBy", "answer_id");
		String order = pageParams.getValue("order", "ascending");
		boolean orderAsc = !"descending".equalsIgnoreCase(order);

		List<Integer> inquiryIds = InquiryDB.getInquiryIds(inquiryGroups, request, random);
		for (Integer inquiryId : inquiryIds)
		{
			InquiryBean inquiry = InquiryDB.getInquiry(inquiryId.intValue(), imagesLength, percentageFormat, orderBy, orderAsc, request);
			for (AnswerForm answer : inquiry.getAnswers())
			{
				callback.addLink(answer.getImgRootDir() + answer.getImagePath());
				callback.addLink(answer.getUrl());
			}
			callback.addInquiry(inquiry);
		}
	}

}
