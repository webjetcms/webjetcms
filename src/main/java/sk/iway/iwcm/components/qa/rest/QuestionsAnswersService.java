package sk.iway.iwcm.components.qa.rest;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.qa.jpa.QuestionsAnswersEntity;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;

@Service
public class QuestionsAnswersService {

	/**
	 * Odosle odpoved navstevnikovi stranky
	 * @param qa
	 * @param request
	 * @return
	 */
    public boolean sendAnswerEmail(QuestionsAnswersEntity qa, HttpServletRequest request) {

        String lng =  (String)request.getSession().getAttribute(Prop.SESSION_I18N_PROP_LNG);
        Prop prop = Prop.getInstance(Constants.getServletContext(), lng, false);

        try {
			String message = "<p>"+prop.getText("components.qa.add_action.answer_to_your_question") + ":<br/></p>";

			String question = qa.getQuestion().trim();
			String answer = qa.getAnswerToEmail().trim();
			String sender = qa.getToName().trim();
			String recipient = qa.getFromName().trim();

			if (Tools.isEmail(qa.getToEmail()) && Tools.isEmail(qa.getFromEmail()) && Tools.isNotEmpty(answer) && answer.length()>10) {

				message += "<p>" + prop.getText("components.qa.add_action.question") + ":<br/></p><p>"+question+"<br/><br/></p>";
				message += "<p>" + prop.getText("components.qa.add_action.answer") + ":<br/></p><p>"+answer+"<br/><br/></p>";

				message += "<p><br/><br/>" + prop.getText("components.qa.add_action.footer") +  " " + sender + "</p>";

				//bacha, toto je odpoved, takze toName je vlastne sender
				//a fromName je recipient

				String toName = qa.getToName();
				if (toName == null || toName.length()<2)
				{
					toName = qa.getToEmail();
				}
				String subject = prop.getText("components.qa.add_action.answer_to_your_question");

				//tu si treba uvedomit, ze from je navstevnik webu a to je admin, ktory odpoveda na otazku
				SendMail.send(toName, qa.getToEmail(), qa.getFromEmail(), subject, message);

				//daj do requestu answer
				String email = "<table border=0><tr><td>"+prop.getText("components.qa.add_action.sender")+": </td><td>"+sender+" &lt;"+qa.getToEmail()+"&gt;</td></tr>";
				email += "<tr><td>"+prop.getText("components.qa.add_action.recipient")+": </td><td>"+recipient+" &lt;"+qa.getFromEmail()+"&gt;</td></tr>";
				email += "<tr><td>"+prop.getText("components.qa.add_action.subject")+": </td><td>"+subject+"</td></tr>";
				email += "<tr><td>"+prop.getText("components.qa.add_action.email_body")+": </td><td>&nbsp;</td></tr>";
				email += "<tr><td colspan=2>"+Tools.replace(message, "\n", "<br>")+"</td></tr>";
				email += "</table>";

				request.setAttribute("answer", email);
			}

			return true;
		}
		catch (Exception ex)
		{
			Logger.error(QuestionsAnswersService.class, ex);
			return false;
		}
    }

	/**
	 * Ziska hodnotu poradia usporiadania pre novy zaznam
	 * @param groupName
	 * @return
	 */
	public int getNewPriority(String groupName){
		return new SimpleQuery().forInt("select max(sort_priority) from questions_answers where group_name=?"+CloudToolsForCore.getDomainIdSqlWhere(true), groupName)+10;
	}
}
