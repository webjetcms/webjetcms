package sk.iway.iwcm.components.qa.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import sk.iway.iwcm.qa.QABean;
import sk.iway.iwcm.qa.QADB;

/**
 * Called by URL /qa.add.do as original Struts URL for adding qa question
 */
@Controller
public class QuestionsAnswersController {
    

    @Autowired
	private HttpServletRequest request;

    @Autowired
    HttpServletResponse response;

    @RequestMapping("/qa.add.struts")
    public String addQuestion(QABean bean) {
        try {
            return QADB.addQuestion(bean, request, response);
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }
}