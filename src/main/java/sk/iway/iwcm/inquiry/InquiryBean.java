package sk.iway.iwcm.inquiry;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Title: WebJET Content Management Server</p>
 * <p>Description: drzi info o ankete </p>
 * <p>Copyright: Copyright (c) 2001-2003</p>
 * <p>Company: Interway s.r.o. (www.interway.sk)</p>
 * @author not attributable
 * @version 1.0
 */

public class InquiryBean
{
   private String question="";
   private List<AnswerForm> answers=new ArrayList<>();
   private int imagesLength = 10;
   private String canAnswer = "yes";
   private boolean multiple;
   private int totalClicksMultiple;



	public int getTotalClicksMultiple()
	{
		return totalClicksMultiple;
	}
	public void setTotalClicksMultiple(int totalClicksMultiple)
	{
		this.totalClicksMultiple = totalClicksMultiple;
	}

	public boolean isMultiple()
	{
		return multiple;
	}
	public void setMultiple(boolean multiple)
	{
		this.multiple = multiple;
	}
	public String getQuestion()
   {
      return question;
   }
   public void setQuestion(String question)
   {
      this.question = question;
   }
   public List<AnswerForm> getAnswers()
   {
      return answers;
   }
   public void setAnswers(List<AnswerForm> answers)
   {
      this.answers = answers;
   }
   public int getImagesLength()
   {
      return imagesLength;
   }
   public void setImagesLength(int imagesLength)
   {
      this.imagesLength = imagesLength;
   }
   public String getCanAnswer()
   {
      return canAnswer;
   }
   public void setCanAnswer(String canAnswer)
   {
      this.canAnswer = canAnswer;
   }

   public int getTotalClicks()
   {
      int total = 0;
      for (AnswerForm aForm : answers)
      {
         total += aForm.getAnswerClicks();
      }
      return(total);
   }

   public void setImgRootDir(String imgRootDir)
   {
      AnswerForm aForm;
      int len = answers.size();
      int i;
      for (i=0; i<len; i++)
      {
         aForm = answers.get(i);
         aForm.setImgRootDir(imgRootDir);
         answers.set(i, aForm);
      }
   }
}