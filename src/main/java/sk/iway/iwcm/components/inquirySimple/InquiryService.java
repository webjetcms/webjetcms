package sk.iway.iwcm.components.inquirySimple;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.JpaDB;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.utils.Pair;

@SuppressWarnings("rawtypes")
@RestController
public class InquiryService {

    @RequestMapping(path="/rest/inquirySimple/saveAnswer/", method={RequestMethod.POST})
    public Object saveAnswer(@RequestBody InquiryAnswerTransferBean answer, HttpServletRequest request)
    {
        String userId = getUserId(request);

        Object result = saveAnswer(answer.getFormId(), userId, answer.getAnswers());
        if(result == null)
        {
            Map<String, String> result2 = new HashMap<>();
            result2.put("error", "V tejto ankete si uz hlasoval");
            return result2;
        }
        return result;
    }

    public Map<String, InquiryResultBean> saveAnswer(String formId, String userId, List<String> answers)
    {
        if(canSaveAnswer(formId, userId))
        {
            for(String item : answers)
            {
                InquiryAnswerBean newItem = new InquiryAnswerBean();
                newItem.setFormId(formId);
                newItem.setUserId(userId);
                newItem.setQuestionId(item);
                newItem.save();
            }
            return getResultsForForm(formId);
        }
        return null;
    }

    protected List getResults(EntityManager em, String formId)
    {
        Query q = em.createQuery("select i.questionId,count(i.id) as cnt from InquiryAnswerBean i where i.formId = :formId group by i.questionId");
        q.setParameter("formId", formId);
        return q.getResultList();
    }

    protected Long getTotalCount(EntityManager em, String formId)
    {
        Query q = em.createQuery("select count(i.id) as cnt from InquiryAnswerBean i where i.formId = :formId group by i.formId");
        q.setParameter("formId", formId);
        List result = q.getResultList();
        if(result != null && result.size() > 0)
        {
            return (Long)result.get(0);
        }
        return 0L;
    }

    public Map<String, InquiryResultBean> getResultsForForm(String formId)
    {
        Map<String, InquiryResultBean> valueToReturn = new HashMap<>();

        EntityManager em = JpaTools.getEclipseLinkEntityManager();
        List result = getResults(em, formId);
        Long count = getTotalCount(em, formId);
        if(result != null)
        {
            for(int i = 0; i < result.size(); i++)
            {
                Object[] o = (Object[])result.get(i);
                InquiryResultBean ir = new InquiryResultBean();
                ir.setCount((Long)o[1]);
                double p = Math.round((count > 0 ? (Long)o[1] / (double)count * 100 : 0) * 100) / 100;
                ir.setPercent(p);

                valueToReturn.put((String)o[0], ir);
            }
        }
        return valueToReturn;
    }

    public String getUserId(HttpServletRequest request)
    {
        Cookie[] cookies = request.getCookies();
        try
        {
            return Tools.getCookieValue(cookies, "JSESSIONID", null);
        }
        catch(Exception e)
        {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public boolean canSaveAnswer(String formId, String userId)
    {
        if(userId == null)
        {
            return false;
        }

        InquiryAnswerBean item = new JpaDB<InquiryAnswerBean>(InquiryAnswerBean.class).findFirstByProperties(new Pair("formId", formId), new Pair("userId", userId));
        return item == null;
    }
}
