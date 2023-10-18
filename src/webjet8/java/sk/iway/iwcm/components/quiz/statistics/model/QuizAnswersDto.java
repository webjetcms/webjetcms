package sk.iway.iwcm.components.quiz.statistics.model;

public class QuizAnswersDto {
   private String questionTitle;
   private Integer rightAnswerCount;
   private Integer wrongAnswerCount;

   public QuizAnswersDto() {
   }

   public QuizAnswersDto(String questionTitle, Integer rightAnswerCount, Integer wrongAnswerCount) {
      this.questionTitle = questionTitle;
      this.rightAnswerCount = rightAnswerCount;
      this.wrongAnswerCount = wrongAnswerCount;
   }

   public String getQuestionTitle() {
      return questionTitle;
   }

   public void setQuestionTitle(String questionTitle) {
      this.questionTitle = questionTitle;
   }

   public Integer getRightAnswerCount() {
      return rightAnswerCount;
   }

   public void setRightAnswerCount(Integer rightAnswerCount) {
      this.rightAnswerCount = rightAnswerCount;
   }

   public Integer getWrongAnswerCount() {
      return wrongAnswerCount;
   }

   public void setWrongAnswerCount(Integer wrongAnswerCount) {
      this.wrongAnswerCount = wrongAnswerCount;
   }

}
