package sk.iway.iwcm.components.quiz;

import sk.iway.iwcm.i18n.Prop;

public enum QuizType {
    RIGHT_ANSWER,
    RATED_ANSWER;

    public String toString() {
        String key = "components.quiz.type." + name().toLowerCase();
        String result = key;
        try
        {
            result = Prop.getInstance().getText(key);
        } catch (Exception ex) {}
        return result.equals(key) ? name() : result;
    }
}
