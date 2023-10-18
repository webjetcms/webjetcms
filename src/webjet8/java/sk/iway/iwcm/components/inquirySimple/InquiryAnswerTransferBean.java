package sk.iway.iwcm.components.inquirySimple;

import java.util.List;

public class InquiryAnswerTransferBean {
    private String formId;
    private List<String> answers;

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
    }
}
