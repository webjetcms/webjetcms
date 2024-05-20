package sk.iway.iwcm.components.quiz.rest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.EditorToolsForCore;
import sk.iway.iwcm.components.quiz.jpa.NameValueBean;
import sk.iway.iwcm.components.quiz.jpa.QuizAnswerEntity;
import sk.iway.iwcm.components.quiz.jpa.QuizAnswerRespository;
import sk.iway.iwcm.components.quiz.jpa.QuizEntity;
import sk.iway.iwcm.components.quiz.jpa.QuizQuestionEntity;
import sk.iway.iwcm.components.quiz.jpa.QuizQuestionRepository;
import sk.iway.iwcm.components.quiz.jpa.QuizRepository;
import sk.iway.iwcm.components.quiz.jpa.QuizStatDTO;
import sk.iway.iwcm.components.quiz.jpa.QuizType;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.ChartType;
import sk.iway.iwcm.stat.rest.StatService;
import sk.iway.iwcm.system.jpa.JpaTools;
import sk.iway.iwcm.utils.Pair;

public class QuizService {

    static final String rightKey = "components.quiz.stat.right_answers";
    static final String wrongKey = "components.quiz.stat.wrong_answers";
    static final String allKey = "components.quiz.stat.all_answers";
    
    /**************************** GET data methods ****************************/
    public static QuizEntity getById(int id) {
        if(id < 1) return new QuizEntity();
        QuizRepository qr = Tools.getSpringBean("quizRepository", QuizRepository.class);
        Optional<QuizEntity> opt =  qr.findById((long) id);
        if(opt.isPresent()) return opt.get();
        else return new QuizEntity();
    }

    public static List<QuizEntity> getAll() {
        QuizRepository qr = Tools.getSpringBean("quizRepository", QuizRepository.class);
        return qr.findAll();
    }

    public static List<QuizQuestionEntity> getQuizQuestions(int quizId) {
        QuizQuestionRepository qqr = Tools.getSpringBean("quizQuestionRepository", QuizQuestionRepository.class);
        List<QuizQuestionEntity> quizQuestions = qqr.findAllByQuizId(quizId);
        if(quizQuestions == null || quizQuestions.size() < 1) return new ArrayList<>();
        return quizQuestions;
    }

    public static Map<Integer, QuizQuestionEntity> getQuizQuestionsMap(int quizId) { 
        List<QuizQuestionEntity> quizQuestions = getQuizQuestions(quizId);
        
        Map<Integer, QuizQuestionEntity> quizQuestionsByQuestionId = new HashMap<>();
        for (QuizQuestionEntity item : quizQuestions) {
            quizQuestionsByQuestionId.put(item.getId().intValue(), item);
        }

        return quizQuestionsByQuestionId;
    }
    
    /**************************** SAVE data method ****************************/
    public static List<QuizAnswerEntity> saveAnswersAndGetResult(int quizId, String formId, List<NameValueBean> answers) {
        //Get quiz questions
        QuizAnswerRespository qar = Tools.getSpringBean("quizAnswerRespository", QuizAnswerRespository.class);
        Map<Integer, QuizQuestionEntity> quizQuestionsByQuestionId = getQuizQuestionsMap(quizId);

        QuizType quizType = QuizType.getQuizType( (new SimpleQuery()).forString("SELECT quiz_type FROM quiz WHERE id=? AND domain_id=?", quizId, CloudToolsForCore.getDomainId()) );
        for (NameValueBean answer : answers) {
            QuizAnswerEntity existingAnswer = JpaTools.findFirstByProperties(QuizAnswerEntity.class,
                    new Pair<String, Integer>("quizId", quizId), new Pair<String, String>("formId", formId),
                    new Pair<String, Integer>("quizQuestionId", answer.getName()));
            if (existingAnswer != null) {
                break;
            }

            QuizAnswerEntity newAnswer = new QuizAnswerEntity();
            newAnswer.setQuizId(quizId);
            newAnswer.setFormId(formId);
            newAnswer.setQuizQuestionId(answer.getName());
            newAnswer.setAnswer(answer.getValue());

            if(quizType == QuizType.RIGHT_ANSWER) {
                int rightAnswer = quizQuestionsByQuestionId.get(answer.getName()).getRightAnswer();
                boolean isCorrect = rightAnswer == answer.getValue();
                newAnswer.setIsCorrect(isCorrect);
                newAnswer.setRightAnswer(rightAnswer);

                //By default, if answer is correct, set rate to 1
                if(isCorrect) newAnswer.setRate(1);
                else newAnswer.setRate(0);

            } else if(quizType == QuizType.RATED_ANSWER) {
                int rate = quizQuestionsByQuestionId.get( answer.getName() ).getRate( answer.getValue() );
                newAnswer.setRate(rate);

                //If rate is greater than 0, answer is correct
                if(rate > 0) { 
                    newAnswer.setIsCorrect(true);
                    newAnswer.setRightAnswer( answer.getValue() ); //If our answer is correct, set it as right answer
                } else {
                    newAnswer.setIsCorrect(false);
                    newAnswer.setRightAnswer( -1 ); //If our answer is wrong, set -1 as right answer
                }
            }
            newAnswer.setCreated(new Date());
            qar.save(newAnswer);
        }
        return qar.findAllByFormId(formId);
    }

    /**************************** STAT methods ****************************/

    /**
     * Prepare list of QuizStatDTO entities (for chart), where every entity represents one question and has compute number of right/wrong answers for selected date range via "stringRange".
     * 
     * @param quizId - QuizEntity id
     * @param stringRange - clasic string range
     * @param chartType
     * @param quizAnswerRespository
     * @return if any error occur, return empty list
     */    
    public static List<QuizStatDTO> statTableData(Integer quizId, String stringRange, ChartType chartType, QuizAnswerRespository quizAnswerRespository) {
        if(quizId == null || quizId < 1) return new ArrayList<>();
        Date[] dateRangeArr = StatService.processDateRangeString(stringRange);

        //Sort data into map -> based on questionId
        Map<Integer, List<QuizAnswerEntity>> questionAnswers = new HashMap<>();
        for(QuizAnswerEntity answer : quizAnswerRespository.findAllByQuizIdAndCreatedBetweenOrderByQuizQuestionId(quizId, dateRangeArr[0], dateRangeArr[1])) {
            Integer answerQuestionId = answer.getQuizQuestionId();

            if(questionAnswers.get(answerQuestionId) == null) {
                questionAnswers.put(answerQuestionId, new ArrayList<>());
                questionAnswers.get(answerQuestionId).add(answer);
            } else { questionAnswers.get(answerQuestionId).add(answer); }
        }

        //Loop map and compute right/wrong answers + all answers for specific date
        List<QuizStatDTO> stats = new ArrayList<>();
        for (Map.Entry<Integer, List<QuizAnswerEntity>> entry : questionAnswers.entrySet()) {
            QuizStatDTO newStat = null;
            int rightAnswers = 0;
            int wrongAnswers = 0;
            int gainedPoints = 0;
            QuizType quizType = QuizType.RIGHT_ANSWER;

            for(QuizAnswerEntity answer : entry.getValue()) {
                if(newStat == null) {
                    newStat = new QuizStatDTO();
                    newStat.setQuestion(answer.getQuizQuestion().getQuestion());
                    newStat.setImageUrl(answer.getQuizQuestion().getImageUrl());
                    quizType = answer.getQuiz().getQuizTypeEnum();
                    if(quizType == QuizType.RATED_ANSWER) newStat.setQuestionMaxPoints( answer.getQuizQuestion().getMaxRate() );
                }

                if(quizType == QuizType.RATED_ANSWER) gainedPoints += answer.getQuizQuestion().getRate( answer.getAnswer() );

                if(answer.getIsCorrect()) rightAnswers++;
                else wrongAnswers++;
            }

            //If entry is not null, prepare it and add it to list
            if(newStat != null) {
                newStat.setNumberOfRightAnswers(rightAnswers);
                newStat.setNumberOfWrongAnswers(wrongAnswers);
                
                if(chartType == ChartType.NOT_CHART) newStat.setPercentageOfRightAnswers( (float) rightAnswers / (float) (rightAnswers + wrongAnswers) * 100 );
                else newStat.setPercentageOfRightAnswers( (float) Math.round((float) rightAnswers / (float) (rightAnswers + wrongAnswers) * 100) );

                if(quizType == QuizType.RATED_ANSWER) newStat.setAverageGainedPoints( (float) gainedPoints / (float) entry.getValue().size() );
                stats.add(newStat);
            }
        }

        //sort it based on number of right answers
        Collections.sort(stats, Comparator.comparingInt(QuizStatDTO::getNumberOfRightAnswers).reversed());

        //Prepare valid question format
        if(chartType != ChartType.NOT_CHART) {
            for(QuizStatDTO stat : stats) {
                stat.setQuestion( prepareQuestionString( stat.getQuestion() ) );
            }
        }

        return stats;
    }

    /**
     * Nulify hours, minutes, seconds and miliseconds in date. (whole time part)
     * @param date
     * @return
     */
    private static Date prepareDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Map<String, List<QuizStatDTO>> statLineData(Integer quizId, String dateRange, String quizType, QuizAnswerRespository quizAnswerRespository, Prop prop) { 
        if(quizId == null || quizId < 1) return new HashMap<>();
        Date[] dateRangeArr = StatService.processDateRangeString(dateRange);

        if(Tools.isAnyEmpty(quizType) || "0".equals(quizType)) 
            return statLineDataAllAnswers(quizId, dateRangeArr, quizType, quizAnswerRespository, prop);
        else if("1".equals(quizType)) 
            return statLineDataAnswers(quizId, dateRangeArr, quizType, quizAnswerRespository, prop);
        else 
            return new HashMap<>();
    }

    private static Map<String, List<QuizStatDTO>> statLineDataAllAnswers(Integer quizId, Date[] dateRangeArr, String quizType, QuizAnswerRespository quizAnswerRespository, Prop prop) {
        //Create combination of dates and number of right/wrong answers (separe maps)
        Map<Date, Integer> rightAnswPerDay = new HashMap<>();
        Map<Date, Integer> wrongAnswPerDay = new HashMap<>();
        for(QuizAnswerEntity answer : quizAnswerRespository.findAllByQuizIdAndCreatedBetweenOrderByQuizQuestionId(quizId, dateRangeArr[0], dateRangeArr[1])) { 
            Date created = prepareDate( answer.getCreated() );

            if(answer.getIsCorrect()) {
                if(rightAnswPerDay.get(created) == null) rightAnswPerDay.put(created, 1);
                else rightAnswPerDay.put(created, rightAnswPerDay.get(created) + 1);
            } else {
                if(wrongAnswPerDay.get(created) == null) wrongAnswPerDay.put(created, 1);
                else wrongAnswPerDay.put(created, wrongAnswPerDay.get(created) + 1);
            }
        }

        List<QuizStatDTO> rightAnsw = new ArrayList<>();
        for (Map.Entry<Date, Integer> entry : rightAnswPerDay.entrySet()) {
            QuizStatDTO newStatAnsw = new QuizStatDTO();
            newStatAnsw.setDayDate(entry.getKey());
            newStatAnsw.setChartValue(entry.getValue());
            rightAnsw.add(newStatAnsw);
        }

        List<QuizStatDTO> wrongAnsw = new ArrayList<>();
        for (Map.Entry<Date, Integer> entry : wrongAnswPerDay.entrySet()) {
            QuizStatDTO newStatAnsw = new QuizStatDTO();
            newStatAnsw.setDayDate(entry.getKey());
            newStatAnsw.setChartValue(entry.getValue());
            wrongAnsw.add(newStatAnsw);
        }

        //Combine days value (combine maps) -> put/sum right map into wrong map
        for (Map.Entry<Date, Integer> right : rightAnswPerDay.entrySet()) {

            Integer wrongValue = wrongAnswPerDay.get( right.getKey() );
            if(wrongValue == null) {
                wrongAnswPerDay.put(right.getKey(), right.getValue());
            } else {
                wrongAnswPerDay.put(right.getKey(), right.getValue() + wrongValue);
            }
        }
        //Loop combined map and create list of QuizStatDTO
        List<QuizStatDTO> allAnsw = new ArrayList<>();
        for (Map.Entry<Date, Integer> combined : wrongAnswPerDay.entrySet()) { 
            QuizStatDTO newStat = new QuizStatDTO();
            newStat.setDayDate(combined.getKey());
            newStat.setChartValue(combined.getValue());
            allAnsw.add(newStat);
        }

        Map<String, List<QuizStatDTO>> lineChartData = new HashMap<>();
        lineChartData.put(prop.getText(rightKey), rightAnsw);
        lineChartData.put(prop.getText(wrongKey), wrongAnsw);
        lineChartData.put(prop.getText(allKey), allAnsw);

        return lineChartData;
    }

    private static Map<String, List<QuizStatDTO>> statLineDataAnswers(Integer quizId, Date[] dateRangeArr, String quizType, QuizAnswerRespository quizAnswerRespository, Prop prop) {
        //Compute points sum for every question for every day (time part of date is nulified)
        Map<String, QuizStatDTO> valueByDateAndQuestion = new HashMap<>();
        for(QuizAnswerEntity answer : quizAnswerRespository.findAllByQuizIdAndCreatedBetweenOrderByQuizQuestionId(quizId, dateRangeArr[0], dateRangeArr[1])) { 
            Date created = prepareDate( answer.getCreated() );
            String mapKey = "" + created.getTime() + "_" + prepareQuestionString( answer.getQuizQuestion().getQuestion() );
            
            if(valueByDateAndQuestion.get(mapKey) == null) {
                QuizStatDTO newStat = new QuizStatDTO();
                newStat.setDayDate(created);
                newStat.setChartValue(answer.getRate());
                
                if(answer.getIsCorrect()) {
                    newStat.setNumberOfRightAnswers(1);
                    newStat.setNumberOfWrongAnswers(0);
                } else {
                    newStat.setNumberOfRightAnswers(0);
                    newStat.setNumberOfWrongAnswers(1);
                }
                valueByDateAndQuestion.put(mapKey, newStat);
            } else {
                valueByDateAndQuestion.get(mapKey).setChartValue( valueByDateAndQuestion.get(mapKey).getChartValue() + answer.getRate() );
                if(answer.getIsCorrect()) 
                    valueByDateAndQuestion.get(mapKey).setNumberOfRightAnswers( valueByDateAndQuestion.get(mapKey).getNumberOfRightAnswers() + 1 );
                else
                    valueByDateAndQuestion.get(mapKey).setNumberOfWrongAnswers( valueByDateAndQuestion.get(mapKey).getNumberOfWrongAnswers() + 1 );
            }
        }

        //Tranform data to line chart format
        Map<String, List<QuizStatDTO>> lineChartData = new HashMap<>();
        for (Map.Entry<String, QuizStatDTO> entry : valueByDateAndQuestion.entrySet()) { 
            String[] keyParts = entry.getKey().split("_");

            if(lineChartData.get(keyParts[1]) == null) lineChartData.put(keyParts[1], new ArrayList<>());

            lineChartData.get(keyParts[1]).add(entry.getValue());
        }

        return lineChartData;
    }

    /**
     * Remove Html tags from string.
     * If string is longer than 64 characters, cut it and add "..." at the end.
     * else return string.
     * 
     * If string is null, return empty string.
     * 
     * @param question
     * @return
     */
    private static String prepareQuestionString (String question) {
        if(question == null)  return "";

        String formatted = EditorToolsForCore.removeHtmlTagsKeepLength( question );
        if(formatted.length() < 64) return formatted;
        else return formatted.substring(0, 64) + "...";
    }
}