package sk.iway.iwcm.components.quiz.jpa;

import sk.iway.iwcm.i18n.Prop;

public enum QuizType {
    RIGHT_ANSWER,
    RATED_ANSWER;

    public String toString() {
        String key = "components.quiz.type." + name().toLowerCase();
        String result = key;
        try {
            result = Prop.getInstance().getText(key);
        } catch (Exception ex) {}
        return result.equals(key) ? name() : result;
    }

    public static QuizType getQuizType(String quizType) {
        if(quizType != null && "1".equals(quizType))
            return QuizType.RATED_ANSWER;

        return QuizType.RIGHT_ANSWER;
    }
}
