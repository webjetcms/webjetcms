package sk.iway.iwcm.components.inquiry.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import sk.iway.iwcm.inquiry.InquiryDB;

/**
 * Called by URL /inquiry.answer.do as original Struts URL for inquiry vote
 */
@Controller
public class InquiryController {

    @GetMapping("/inquiry.answer.struts")
    public String saveAnswer(HttpServletRequest request, HttpServletResponse response) {
        try {
            return InquiryDB.saveAnswer(request, response);
        } catch(Exception e) {
           sk.iway.iwcm.Logger.error(e);
        }

        return null;
    }
}


